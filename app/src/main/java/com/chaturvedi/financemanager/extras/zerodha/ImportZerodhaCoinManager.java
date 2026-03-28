package com.chaturvedi.financemanager.extras.zerodha;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Time;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Bank;
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
import java.util.Locale;
import java.util.Map;

public class ImportZerodhaCoinManager {
    private Context context;

    private int result;

    private int numTransactions;

    private ArrayList<Transaction> transactions;

    /**
     * @param cxt     Context Eg: ExtrasActivity.this
     * @param fileUri The URI of the Zerodha Coin CSV file
     */
    public ImportZerodhaCoinManager(Context cxt, Uri fileUri) {
        context = cxt;
        result = parseZerodhaCoinStatement(fileUri);

    }

    /**
     * Reads the Zerodha Coin Statement and parses into appropriate transactions
     *
     * @param fileUri:
     * @return 0 if read properly
     * 1 if no file exists
     * 2 error occurs
     */
    private int parseZerodhaCoinStatement(Uri fileUri) {
        String investments_bank_id = get_investments_bank_id();
        String host_bank_id = get_host_bank_id();
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

            SimpleDateFormat csvDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
            SimpleDateFormat csvDateTimeFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
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
            // Reverse all the lines
            Collections.reverse(lines);

            for (String line : lines) {
                String[] values = line.split(",", -1);
                if (values.length < headers.length) {
                    continue; // Skip malformed rows
                }

                // Extract columns
                String schemeName = getColumnValue(headers, values, "scheme_name");
                schemeName = processSchemeName(schemeName);

                String transactionMode = getColumnValue(headers, values, "transaction_mode");
                String tradeDateStr = getColumnValue(headers, values, "trade_date");
                String orderedAt = getColumnValue(headers, values, "ordered_at");
                String navStr = getColumnValue(headers, values, "nav");
                String unitsStr = getColumnValue(headers, values, "units");
                String amountStr = getColumnValue(headers, values, "amount");
                String status = getColumnValue(headers, values, "status");
                String remarks = getColumnValue(headers, values, "remarks");
                String tag = getColumnValue(headers, values, "tag");

                if (!"COMPLETE".equalsIgnoreCase(status)) {
                    continue; // Filter rows by status
                }

                if ("SELL".equalsIgnoreCase(transactionMode) && remarks.contains("on")) {
                    String[] remarkParts = remarks.split("on");
                    if (remarkParts.length > 1) {
                        tradeDateStr = remarkParts[1].trim().split(" ")[0];
                    }
                }

                double nav = Double.parseDouble(navStr);
                double units = Double.parseDouble(unitsStr);
                double amount = Double.parseDouble(amountStr);

                Time createdTime;
                java.util.Date parsedDateTime;
                try {
                    parsedDateTime = csvDateTimeFormat.parse(tradeDateStr + " " + orderedAt);
//                    createdTime = dbDateTimeFormat.format(parsedDateTime) + "/" + (++lastMilliseconds);
                    createdTime = new Time(dbDateTimeFormat.format(parsedDateTime));
                } catch (ParseException e) {
                    Log.e("ImportZerodhaCoin", "Error parsing date/time: " + e.getMessage());
                    continue;
                }

                Time modifiedTime = createdTime;
                Date tradeDate;
                try {
                    tradeDate = new Date(dbDateFormat.format(csvDateFormat.parse(tradeDateStr)));
                } catch (ParseException e) {
                    Log.e("ImportZerodhaCoin", "Error parsing trade date: " + e.getMessage());
                    continue;
                }

                String expType, particulars;
                if (transactionMode.equalsIgnoreCase("BUY")) {
                    expType = "Transfer " + host_bank_id + " " + investments_bank_id;
                    if (remarks.toLowerCase().contains("sip") || tag.toLowerCase().contains("sip")) {
                        particulars = "Mutual Funds SIP Installment - " + schemeName;
                    } else {
                        particulars = "Invested In Mutual Funds - " + schemeName;
                    }
                } else {
                    expType = "Transfer " + investments_bank_id + " " + host_bank_id;
                    particulars = "Redeemed Mutual Funds - " + schemeName;
                }

                Transaction transaction = new Transaction(
                        0, // Temporary ID
                        createdTime,
                        modifiedTime,
                        tradeDate,
                        expType,
                        particulars,
                        nav, // rate
                        units, // quantity
                        amount, // amount
                        false, // hidden
                        true // includeInCounters
                );
                transactions.add(transaction);

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

            // Log all the filtered transactions
            Log.d("ImportZerodhaCoin", "Filtered Transactions:");  // TODO: Remove this
            for (Transaction transaction : transactions) {
                Log.d("ImportZerodhaCoin", transaction.toString());
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
        return transaction.getDate().getSavableDate() + "|" +
                transaction.getType() + "|" +
                transaction.getParticular() + "|" +
                transaction.getQuantity() + "|" +
                transaction.getAmount();
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

    private String processSchemeName(String schemeName) {
        // Replace & with 'And' to avoid issues with SQL
        schemeName = schemeName.replace("&", "And");
        // Ensure that each word starts with capital letter. Rest of the letters are retained as is
        String[] schemeNameParts = schemeName.split(" ");
        StringBuilder schemeNameBuilder = new StringBuilder();
        for (String schemeNamePart : schemeNameParts) {
            schemeNameBuilder.append(schemeNamePart.substring(0, 1).toUpperCase());
            schemeNameBuilder.append(schemeNamePart.substring(1));
            schemeNameBuilder.append(" ");
        }
        schemeName = schemeNameBuilder.toString().trim();
        return schemeName;
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
        return "Bank03";  // TODO: Do not hard-code
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
