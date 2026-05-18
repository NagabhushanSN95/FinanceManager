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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImportZerodhaKiteDividendsManager {
    private static final List<String> TARGET_COLUMNS = Arrays.asList("Symbol", "ISIN", "Date", "Quantity", "Dividend Per Share", "Net Dividend Amount");
    private Context context;
    private int result;
    private int numTransactions;
    private ArrayList<Transaction> transactions;

    /**
     * @param cxt     Context Eg: ExtrasActivity.this
     * @param fileUri The URI of the Zerodha Coin CSV file
     */
    public ImportZerodhaKiteDividendsManager(Context cxt, Uri fileUri) {
        context = cxt;
        result = parseZerodhaKiteDividendsStatement(fileUri);

    }

    /**
     * Reads the Zerodha Coin Statement and parses into appropriate transactions
     *
     * @param fileUri:
     * @return 0 if read properly
     * 1 if no file exists
     * 2 error occurs
     */
    private int parseZerodhaKiteDividendsStatement(Uri fileUri) {
        String host_bank_id = get_host_bank_id();
        transactions = new ArrayList<>();
        try {
            List<List<String>> tableData = readExcelFile(context, fileUri);

            if (tableData == null || tableData.isEmpty()) {
                return 1;
            }

            SimpleDateFormat csvDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
            SimpleDateFormat dbDateTimeFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss/S", Locale.ENGLISH);
            SimpleDateFormat exDividendDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
            String importTimeStr = dbDateTimeFormat.format(new java.util.Date());

            List<String> headers = tableData.get(0);
            tableData.remove(0);
            java.util.Date minDate = null;
            java.util.Date maxDate = null;

            ArrayList<String> lines = new ArrayList<>();
            for (List<String> rowData : tableData) {
                // Extract columns
                String ticker = getColumnValue(headers, rowData, "Symbol");
                String dateStr = getColumnValue(headers, rowData, "Date");
                String rateStr = getColumnValue(headers, rowData, "Dividend Per Share");
                String quantityStr = getColumnValue(headers, rowData, "Quantity");
//                Log.d("Import Zerodha Kite Dividends", "Ticker: " + ticker + ", Date: " + dateStr + ", Rate: " + rateStr + ", Quantity: " + quantityStr);

                double rate = Double.parseDouble(rateStr);
                double quantity = Double.parseDouble(quantityStr);

                Time createdTime = new Time(importTimeStr);

                Time modifiedTime = createdTime;
                Date creditDate;
                try {
                    creditDate = new Date(dbDateFormat.format(csvDateFormat.parse(dateStr)));
                } catch (ParseException e) {
                    Log.e("Import Zerodha Kite Dividends Manager", "Error parsing trade date: " + e.getMessage());
                    continue;
                }

                String expType, particulars;
                expType = "Credit " + host_bank_id;
                particulars = "Dividend From Stocks - " + ticker + " (Ex-Dividend Date: " + exDividendDateFormat.format(csvDateFormat.parse(dateStr)) + ")";

                Transaction transaction = new Transaction(
                        0, // Temporary ID
                        createdTime,
                        modifiedTime,
                        creditDate,
                        expType,
                        particulars,
                        rate, // rate
                        quantity, // quantity
                        rate * quantity, // rate
                        false, // hidden
                        true // includeInCounters
                );
                transactions.add(transaction);

                // Update min and max date
                if (minDate == null || csvDateFormat.parse(dateStr).before(minDate)) {
                    minDate = csvDateFormat.parse(dateStr);
                }
                if (maxDate == null || csvDateFormat.parse(dateStr).after(maxDate)) {
                    maxDate = csvDateFormat.parse(dateStr);
                }
            }

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
            Calendar cal = Calendar.getInstance();
            cal.setTime(maxDate);
            cal.add(Calendar.MONTH, 1); // Add one month
            maxDate = cal.getTime();
            String endDate = dbDateFormat.format(maxDate);
            ArrayList<String> allowedTransactionTypes = new ArrayList<>();
            allowedTransactionTypes.add("Credit " + host_bank_id);
            ArrayList<Transaction> existingTransactions = databaseAdapter.getTransactions(null, new Date(startDate), new Date(endDate), allowedTransactionTypes, "Dividend From Stocks", true, Constants.VALUE_SORT_TRANSACTIONS_DATE, 0, -1);

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
        } catch (ParseException | NullPointerException |
                 StringIndexOutOfBoundsException e) {
//            Toast.makeText(context, "Error in parsing Zerodha Coin Statement\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return 2;
        }
    }

    public List<List<String>> readExcelFile(Context context, Uri fileUri) {
        List<List<String>> tableData = new ArrayList<>();

        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // Read the first sheet
            boolean tableStarted = false;
            List<Integer> targetColumnIndexes = new ArrayList<>();
            List<String> columnHeaderNames = new ArrayList<>();

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();

                if (!tableStarted) {
                    // Check if this row contains the target headers
                    for (Cell cell : row) {
                        if (TARGET_COLUMNS.contains(cell.getStringCellValue().trim())) {
                            tableStarted = true;
                        }
                    }

                    if (tableStarted) {
                        // Find column indexes of target headers
                        for (Cell cell : row) {
                            if (TARGET_COLUMNS.contains(cell.getStringCellValue().trim())) {
                                targetColumnIndexes.add(cell.getColumnIndex());
                                columnHeaderNames.add(cell.getStringCellValue().trim());
                            }
                        }
                        tableData.add(columnHeaderNames);
                    }
                    continue;
                }

                // If table started, read only the target columns
                for (Integer colIndex : targetColumnIndexes) {
                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rowData.add(cell != null ? cell.toString() : "");
                }

                // Stop reading if an empty row is encountered (assuming table ends)
                if (rowData.stream().allMatch(String::isEmpty)) {
                    break;
                }

                // Only add rowData if there are no empty values in the row
                if (rowData.stream().noneMatch(String::isEmpty)) {
                    tableData.add(rowData);
                }
            }
        } catch (Exception e) {
            Log.e("Zerodha Kite Dividends Manager", "Error reading Excel file", e);
        }

//        Log.d("Zerodha Kite Dividends Manager", "Table Data: " + tableData);

        return tableData;
    }

    /**
     * Generates a unique key for a transaction based on its attributes.
     */
    private String generateTransactionKey(Transaction transaction) {
        return transaction.getParticular();
    }

    /**
     * Gets the value of a column by name from the CSV row.
     */
    private String getColumnValue(List<String> headers, List<String> rowData, String columnName) {
        int index = headers.indexOf(columnName);
        return (index >= 0 && index < rowData.size()) ? rowData.get(index).trim() : "";
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
