package com.chaturvedi.financemanager.extras;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.chaturvedi.customviews.IntervalSelector;
import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Month;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Bank;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.datastructures.Wallet;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

class ExportManager
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_CURRENCY_SYMBOL = "currency_symbols";
	private Context context;
	private String currencySymbol = " ";
	
	private String exportFileName;
	private IntervalSelector.IntervalType intervalType;
	private Date startDate;
	private Date endDate;
	private boolean includeTransactionType;
	private boolean includeRateQuantity;
	private boolean includeCurrentWalletBankBalances;
	
	ExportManager(Context context, String exportFileName, IntervalSelector.IntervalType
			intervalType, Date startDate, Date endDate, boolean includeTransactionType, boolean
						  includeRateQuantity, boolean includeCurrentWalletBankBalances)
	{
		this.context = context;
		this.exportFileName = exportFileName;
		this.intervalType = intervalType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.includeTransactionType = includeTransactionType;
		this.includeRateQuantity = includeRateQuantity;
		this.includeCurrentWalletBankBalances = includeCurrentWalletBankBalances;
		readPreferences();
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_SETTINGS,
				0);
		if (preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(KEY_CURRENCY_SYMBOL, " ");
		}
		else
		{
			currencySymbol = " ";
		}
	}
	
	private String getTitle()
	{
		String title;
		switch (intervalType)
		{
			case INTERVAL_MONTH:
				title = "Statement for " + Month.fromMonthNo(startDate.getMonth()).toString() +
						" - " + startDate.getYear();
				break;
			
			case INTERVAL_YEAR:
				title = "Statement for " + startDate.getYear();
				break;
			
			case INTERVAL_ALL:
			case INTERVAL_CUSTOM:
				title = "Statement from " + startDate.getDisplayDate("/") + " to " + endDate
						.getDisplayDate("/");
				break;
			
			default:
				throw new RuntimeException("Invalid Interval Type: " + intervalType.toString());
		}
		return title;
	}
	
	/**
	 * @return 0 if export is successful
	 * 1 if not able to create Directory Structure
	 * 2 Exception Occurred
	 */
	int export()
	{
		String title = getTitle();
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		List<Transaction> transactions = databaseAdapter.getTransactions(null, startDate, endDate,
				null, null, false, 0, -1);
		DecimalFormat formatter = new DecimalFormat("0.00");
		try
		{
			String exportFolderPath = "Chaturvedi/Finance Manager";
			
			File exportFolder = new File(Environment.getExternalStoragePublicDirectory("Android"),
					exportFolderPath);
			if (!exportFolder.exists())
			{
				if (!exportFolder.mkdirs())
				{
					return 1;
				}
			}
			
			File exportFile = new File(exportFolder, exportFileName);
			BufferedWriter exportWriter = new BufferedWriter(new FileWriter(exportFile));
			
			exportWriter.write("<html>\n<body>");
			exportWriter.write("<h1>" + title + "</h1>\n");
			
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>" + "Sl No" + "</td>");
			exportWriter.write("\t<td>" + "Date" + "</td>");
			if (includeTransactionType)
			{
				exportWriter.write("\t<td>" + "Transaction Type" + "</td>");
			}
			exportWriter.write("\t<td>" + "Particulars" + "</td>");
			if (includeRateQuantity)
			{
				exportWriter.write("\t<td>" + "Rate" + "</td>");
				exportWriter.write("\t<td>" + "Quantity" + "</td>");
			}
			exportWriter.write("\t<td>" + "Amount" + "</td>");
			exportWriter.write("</tr>\n");
			
			for (int i = 0; i < transactions.size(); i++)
			{
				Transaction transaction = transactions.get(i);
				
				// Set the colour
				String colour;
				if (transaction.getType().contains("Debit"))
				{
					colour = "red";
				}
				else if (transaction.getType().contains("Credit"))
				{
					colour = "green";
				}
				else if (transaction.getType().contains("Transfer"))
				{
					colour = "blue";
				}
				else
				{
					colour = "black";
				}
				
				exportWriter.write("<tr>\n");
				exportWriter.write("<font color=\"" + colour + "\">");
				exportWriter.write("\t<td>" + (i + 1) + "</td>");
				exportWriter.write("\t<td>" + transaction.getDate().getDisplayDate() + "</td>");
				if (includeTransactionType)
				{
					TransactionTypeParser parser = new TransactionTypeParser(context, transaction
							.getType());
					exportWriter.write("\t<td>" + parser.getTransactionTypeForDisplay() + "</td>");
				}
				exportWriter.write("\t<td>" + transaction.getDisplayParticular(context) + "</td>");
				if (includeRateQuantity)
				{
					exportWriter.write("\t<td>" + transaction.getRate() + "</td>");
					exportWriter.write("\t<td>" + transaction.getQuantity() + "</td>");
				}
				exportWriter.write("\t<td>" + formatter.format(transaction.getAmount()) + "</td>");
				exportWriter.write("</font>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>" + "Total Income" + "</td>");
			exportWriter.write("\t<td>" + currencySymbol + formatter.format(databaseAdapter
					.getIncome(startDate,
							endDate)) + "</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>" + "Total Expenditure" + "</td>");
			exportWriter.write("\t<td>" + currencySymbol + formatter.format(databaseAdapter
					.getAmountSpent
							(startDate, endDate)) + "</td>");
			exportWriter.write("</tr>\n");
			if (includeCurrentWalletBankBalances)
			{
				for (Wallet wallet : databaseAdapter.getAllVisibleWallets())
				{
					exportWriter.write("<tr>\n");
					exportWriter.write("\t<td>" + "Amount In " + wallet.getName() + "</td>");
					exportWriter.write("\t<td>" + currencySymbol + formatter.format(wallet
							.getBalance()) + "</td>");
					exportWriter.write("</tr>\n");
				}
				for (Bank bank : databaseAdapter.getAllVisibleBanks())
				{
					exportWriter.write("<tr>\n");
					exportWriter.write("\t<td>" + "Balance In " + bank.getName() + "</td>");
					exportWriter.write("\t<td>" + currencySymbol + formatter.format(bank
							.getBalance()) + "</td>");
					exportWriter.write("</tr>\n");
				}
			}
			exportWriter.write("</table>");
			exportWriter.write("Exported by Finance Manager on " + new Date(Calendar.getInstance()
			).getDisplayDate());
			
			exportWriter.write("</html>\n</body>");
			exportWriter.close();
			
			return 0;
		}
		catch (Exception e)
		{
			return 2;
		}
	}
}
