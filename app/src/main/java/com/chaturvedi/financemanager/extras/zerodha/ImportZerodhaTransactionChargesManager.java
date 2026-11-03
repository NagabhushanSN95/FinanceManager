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
import java.util.Locale;
import java.util.Map;

public class ImportZerodhaTransactionChargesManager {
    private Context context;

    private int result;

    private int numTransactions;

    private ArrayList<Transaction> transactions;

    /**
     * @param cxt     Context Eg: ZerodhaImportActivity.this
     * @param fileUri The URI of the Zerodha Transaction Charges CSV file
     */
    public ImportZerodhaTransactionChargesManager(Context cxt, Uri fileUri) {
        context = cxt;
        result = parseZerodhaTransactionCharges(fileUri);
    }

    /**
     * Reads the Zerodha Transaction Charges CSV and parses into expense transactions.
     *
     * @param fileUri:
     * @return 0 if read properly
     * 1 if no file exists
     * 2 error occurs
     */
    private int parseZerodhaTransactionCharges(Uri fileUri) {
        String zerodha_demat_bank_id = getZerodhaDematBankId();
        String tradingExpTypeId = getTradingExpTypeId();
        String expType = "Debit " + zerodha_demat_bank_id + " " + tradingExpTypeId;

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

            SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
            SimpleDateFormat dbDateTimeFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss/S", Locale.ENGLISH);
            String importTimeStr = dbDateTimeFormat.format(new java.util.Date());

            java.util.Date minDate = null;
            java.util.Date maxDate = null;

            while ((line1 = reader.readLine()) != null) {
                if (isHeader) {
                    headers = line1.split(",");
                    isHeader = false;
                    continue;
                }

                String[] values = line1.split(",", -1);
                if (values.length < 2) {
                    continue; // Skip malformed rows
                }

                String dateStr = getColumnValue(headers, values, "date");
                String amountStr = getColumnValue(headers, values, "amount");

                if (dateStr.isEmpty() || amountStr.isEmpty()) {
                    continue;
                }

                java.util.Date parsedDate;
                try {
                    parsedDate = csvDateFormat.parse(dateStr);
                } catch (ParseException e) {
                    Log.e("Import Zerodha Transaction Charges", "Error parsing date: " + e.getMessage());
                    continue;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                    amount = Math.round(amount * 10000.0) / 10000.0;
                } catch (NumberFormatException e) {
                    Log.e("Import Zerodha Transaction Charges", "Error parsing amount: " + e.getMessage());
                    continue;
                }

                String formattedDate = dbDateFormat.format(parsedDate);
                Time createdTime = new Time(importTimeStr);
                Time modifiedTime = createdTime;
                Date tradeDate = new Date(formattedDate);

                Transaction transaction = new Transaction(
                        0, // Temporary ID
                        createdTime,
                        modifiedTime,
                        tradeDate,
                        expType,
                        "Zerodha Transaction Charges",
                        amount, // rate
                        1.0,    // quantity
                        amount, // amount
                        false,  // hidden
                        true    // includeInCounters
                );
                transactions.add(transaction);

                if (minDate == null || parsedDate.before(minDate)) {
                    minDate = parsedDate;
                }
                if (maxDate == null || parsedDate.after(maxDate)) {
                    maxDate = parsedDate;
                }
            }
            reader.close();

            if (transactions.isEmpty()) {
                numTransactions = 0;
                return 0;
            }

            // Sort transactions by date then createdTime
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

            // Filter out transactions that already exist in the database
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

            // Assign unique IDs and increment milliseconds for ordering
            int idCounter = databaseAdapter.getIDforNextTransaction();
            int lastMilliseconds = 0;
            for (Transaction transaction : transactions) {
                transaction.setID(idCounter++);
                transaction.getCreatedTime().setMillis(++lastMilliseconds);
            }

            numTransactions = transactions.size();
            return 0;
        } catch (IOException | NullPointerException |
                 StringIndexOutOfBoundsException e) {
            return 2;
        }
    }

    private String generateTransactionKey(Transaction transaction) {
        double rate = Math.round(transaction.getRate() * 10000.0) / 10000.0;
        return transaction.getDate().getSavableDate() + "|" +
                transaction.getType() + "|" +
                transaction.getParticular() + "|" +
                rate + "|" +
                transaction.getQuantity();
    }

    private String getColumnValue(String[] headers, String[] values, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return values[i].trim();
            }
        }
        return "";
    }

    private String getZerodhaDematBankId() {
        String bankId = null;
        DecimalFormat formatter = new DecimalFormat("00");
        DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
        ArrayList<Bank> banks = databaseAdapter.getAllBanks();
        for (Bank bank : banks) {
            if (bank.getName().equalsIgnoreCase("Zerodha Demat")) {
                bankId = "Bank" + formatter.format(bank.getID());
            }
        }
        return bankId;
    }

    private String getTradingExpTypeId() {
        String expTypeId = null;
        DecimalFormat formatter = new DecimalFormat("00");
        DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
        ArrayList<ExpenditureType> expTypes = databaseAdapter.getAllExpenditureTypes();
        for (ExpenditureType expType : expTypes) {
            if (expType.getName().equalsIgnoreCase("Trading")) {
                expTypeId = "Exp" + formatter.format(expType.getId());
            }
        }
        return expTypeId;
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
