package com.chaturvedi.financemanager.extras.zerodha;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Time;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Bank;
import com.chaturvedi.financemanager.datastructures.ExpenditureType;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.functions.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ImportZerodhaKiteTradebookManager {
    private Context context;

    private int result;

    private int numTransactions;

    private ArrayList<Transaction> transactions;

    /**
     * @param cxt     Context Eg: ExtrasActivity.this
     * @param fileUri The URI of the Zerodha Coin CSV file
     */
    public ImportZerodhaKiteTradebookManager(Context cxt, Uri fileUri) {
        context = cxt;
        result = parseZerodhaKiteTradebook(fileUri);

    }

    /**
     * Reads the Zerodha Coin Statement and parses into appropriate transactions
     *
     * @param fileUri:
     * @return 0 if read properly
     * 1 if no file exists
     * 2 error occurs
     */
    private int parseZerodhaKiteTradebook(Uri fileUri) {
        String investments_bank_id = get_investments_bank_id();
        String host_bank_id = get_host_bank_id();
        String dpChargesExpType = getDpChargesExpType();
        transactions = new ArrayList<>();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                return 1;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line1;
            String[] headers = null;
            boolean isHeader = true;

            SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat csvDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.ENGLISH);
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
            SimpleDateFormat dbDateTimeFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss/S", Locale.ENGLISH);

            java.util.Date minDate = null;
            java.util.Date maxDate = null;

            ArrayList<String> lines = new ArrayList<>();
            while ((line1 = reader.readLine()) != null) {
                if (isHeader) {
                    headers = line1.split(",");
                    isHeader = false;
                    continue;
                }
                lines.add(line1);
            }

            // Keep a set of DP charges per stock per day. Multiple stock sell options in the same day does not incur multiple DP charges
            Set<String> dpChargesSet = new HashSet<>();

            for (String line : lines) {
                String[] values = line.split(",", -1);
                if (values.length < headers.length) {
                    continue; // Skip malformed rows
                }

                // Extract columns
                String ticker = getColumnValue(headers, values, "symbol");
                String trade_type = getColumnValue(headers, values, "trade_type");
                String tradeDateStr = getColumnValue(headers, values, "trade_date");
                String orderExecutionTime = getColumnValue(headers, values, "order_execution_time");
                String rateStr = getColumnValue(headers, values, "price");
                String quantityStr = getColumnValue(headers, values, "quantity");

                double rate = Double.parseDouble(rateStr);
                double quantity = Double.parseDouble(quantityStr);

                Time createdTime;
                java.util.Date parsedDateTime;
                try {
                    parsedDateTime = csvDateTimeFormat.parse(orderExecutionTime);
//                    createdTime = dbDateTimeFormat.format(parsedDateTime) + "/" + (++lastMilliseconds);
                    createdTime = new Time(dbDateTimeFormat.format(parsedDateTime));
                } catch (ParseException e) {
                    Log.e("Import Zerodha Kite Tradebook", "Error parsing order execution time: " + e.getMessage());
                    continue;
                }

                Time modifiedTime = createdTime;
                Date tradeDate;
                try {
                    tradeDate = new Date(dbDateFormat.format(csvDateFormat.parse(tradeDateStr)));
                } catch (ParseException e) {
                    Log.e("Import Zerodha Kite Tradebook", "Error parsing trade date: " + e.getMessage());
                    continue;
                }

                String expType, particulars;
                if (trade_type.equalsIgnoreCase("BUY")) {
                    expType = "Transfer " + host_bank_id + " " + investments_bank_id;
                    particulars = "Bought Stocks - " + ticker;
                } else {
                    expType = "Transfer " + investments_bank_id + " " + host_bank_id;
                    particulars = "Sold Stocks - " + ticker;
                }

                Transaction transaction = new Transaction(
                        0, // Temporary ID
                        createdTime,
                        modifiedTime,
                        tradeDate,
                        expType,
                        particulars,
                        rate, // rate
                        quantity, // quantity
                        rate * quantity, // rate
                        false, // hidden
                        true // includeInCounters
                );
                transactions.add(transaction);

                // Add DP Charges for selling stocks
                if (trade_type.equalsIgnoreCase("SELL")) {
                    String dpChargesKey = tradeDateStr + "|" + ticker;
                    if (!dpChargesSet.contains(dpChargesKey)) {
                        double dpCharges = 15.34;
                        String expTypeDp = "Debit " + host_bank_id + " " + dpChargesExpType;
                        String particularsDp = "Depository Participant Charges For Selling Stocks - " + ticker;
                        Transaction dpChargesTransaction = new Transaction(
                                0, // Temporary ID
                                createdTime,
                                modifiedTime,
                                tradeDate,
                                expTypeDp,
                                particularsDp,
                                dpCharges, // rate
                                1, // quantity
                                dpCharges, // rate
                                false, // hidden
                                true // includeInCounters
                        );
                        transactions.add(dpChargesTransaction);
                        dpChargesSet.add(dpChargesKey);
                    }
                }

                // Update min and max date
                if (minDate == null || csvDateFormat.parse(tradeDateStr).before(minDate)) {
                    minDate = csvDateFormat.parse(tradeDateStr);
                }
                if (maxDate == null || csvDateFormat.parse(tradeDateStr).after(maxDate)) {
                    maxDate = csvDateFormat.parse(tradeDateStr);
                }
            }
            reader.close();

            // Sort transactions
            Collections.sort(transactions, new Comparator<Transaction>() {
                @Override
                public int compare(Transaction t1, Transaction t2) {
                    int dateCompare = t1.getDate().getSavableDate().compareTo(t2.getDate().getSavableDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return t1.getCreatedTime().toString().compareTo(t2.getCreatedTime().toString());
                }
            });

            // Filter out transactions if they already exist in the database - including repetitions
            DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
            String startDate = dbDateFormat.format(minDate);
            String endDate = dbDateFormat.format(maxDate);
            ArrayList<Transaction> existingTransactions = databaseAdapter.getTransactions(null, new Date(startDate), new Date(endDate), null, null, true, Constants.VALUE_SORT_TRANSACTIONS_DATE, 0, -1);

            Map<String, Integer> existingTransactionCounts = new HashMap<>();
            for (Transaction existingTransaction : existingTransactions) {
                String key = generateTransactionKey(existingTransaction);
                existingTransactionCounts.put(key, existingTransactionCounts.getOrDefault(key, 0) + 1);
            }

            ArrayList<Transaction> filteredTransactions = new ArrayList<>();
            Map<String, Integer> csvTransactionCounts = new HashMap<>();

            for (Transaction transaction : transactions) {
                String transactionKey = generateTransactionKey(transaction);
                csvTransactionCounts.put(transactionKey, csvTransactionCounts.getOrDefault(transactionKey, 0) + 1);

                int existingCount = existingTransactionCounts.getOrDefault(transactionKey, 0);
                int csvCount = csvTransactionCounts.get(transactionKey);

                if (csvCount > existingCount) {
                    filteredTransactions.add(transaction);
                }
            }
            transactions = filteredTransactions;

            // Assign unique IDs
            int idCounter = databaseAdapter.getIDforNextTransaction();
            int lastMilliseconds = 0;
            for (Transaction transaction : transactions) {
                transaction.setID(idCounter++);
                transaction.getCreatedTime().setMillis(++lastMilliseconds);
            }

            numTransactions = transactions.size();
            return 0;
        } catch (IOException | ParseException | NullPointerException |
                 StringIndexOutOfBoundsException e) {
//            Toast.makeText(context, "Error in parsing Zerodha Coin Statement\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return 2;
        }
    }

    /**
     * Generates a unique key for a transaction based on its attributes.
     */
    private String generateTransactionKey(Transaction transaction) {
        double rate = Math.round(transaction.getRate() * 10000.0) / 10000.0;  // round rate to 4 decimal places
        return transaction.getDate().getSavableDate() + "|" +
                transaction.getType() + "|" +
                transaction.getParticular() + "|" +
                rate + "|" +
                transaction.getQuantity();
    }

    /**
     * Gets the value of a column by name from the CSV row.
     */
    private String getColumnValue(String[] headers, String[] values, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return values[i].trim();
            }
        }
        return "";
    }

    private String get_investments_bank_id() {
        String investments_bank_id = null;
        DecimalFormat formatter = new DecimalFormat("00");
        DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
        ArrayList<Bank> banks = databaseAdapter.getAllBanks();
        // Iterate through all banks and get the id of the bank who name == "Investments". Return bank{id}
        for (Bank bank : banks) {
            if (bank.getName().equalsIgnoreCase("Investments")) {
                investments_bank_id = "Bank" + formatter.format(bank.getID());
            }
        }
        return investments_bank_id;
    }

    private String get_host_bank_id() {
        String investments_bank_id = null;
        DecimalFormat formatter = new DecimalFormat("00");
        DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
        ArrayList<Bank> banks = databaseAdapter.getAllBanks();
        // Iterate through all banks and get the id of the bank who name == "Investments". Return bank{id}
        for (Bank bank : banks) {
            if (bank.getName().equalsIgnoreCase("Zerodha Demat")) {
                investments_bank_id = "Bank" + formatter.format(bank.getID());
            }
        }
        return investments_bank_id;
    }

    private String getDpChargesExpType() {
        String dpChargesExpType = null;
        DecimalFormat formatter = new DecimalFormat("00");
        DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
        ArrayList<ExpenditureType> expTypes = databaseAdapter.getAllExpenditureTypes();
        // Iterate through all expenditure types and get the id of the expType whose name == "Trading". Return expType{id}
        for (ExpenditureType expType : expTypes) {
            if (expType.getName().equalsIgnoreCase("Trading")) {
                dpChargesExpType = "Exp" + formatter.format(expType.getId());
            }
        }
        return dpChargesExpType;
    }

    public int getResult() {
        return result;
    }

    public int getNumTransactions() {
        return numTransactions;
    }

    public ArrayList<Transaction> getAllTransactions() {
        return transactions;
    }
}
