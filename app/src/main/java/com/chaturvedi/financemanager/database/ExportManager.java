package com.chaturvedi.financemanager.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class ExportManager
{
	private Context context;
	
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_CURRENCY_SYMBOL = "currency_symbols";
	private String currencySymbol = " ";

	private String exportFileName;
	private long exportLongMonth;
	private String exportMonthName;
	private int exportYear;
	//ArrayList<String> months;
	
	public void export(Context cxt, String fileName, long longMonth)
	{
		context = cxt;
		readPreferences();
		
		exportFileName = fileName;
		exportLongMonth = longMonth;
		exportMonthName = Date.getMonthName((int) (longMonth%100));
		exportYear = (int) (longMonth/100);
		
		writeToSDCard();
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(KEY_CURRENCY_SYMBOL, " ");
		}
		else
		{
			currencySymbol = " ";
		}
	}
	
	private void writeToSDCard()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		try
		{
			String expenditureFolderName = "Finance Manager";
			
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			File exportFile = new File(expenditureFolder, exportFileName);
			BufferedWriter exportWriter = new BufferedWriter(new FileWriter(exportFile));
			
			exportWriter.write("<html>\n<body>");
			exportWriter.write("<h1>" + exportMonthName + "-" + exportYear + "</h1>\n");

			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Sl No"+"</td>");
			exportWriter.write("\t<td>"+"Date"+"</td>");
			exportWriter.write("\t<td>"+"Type"+"</td>");
			exportWriter.write("\t<td>"+"Particulars"+"</td>");
			exportWriter.write("\t<td>"+"Rate"+"</td>");
			exportWriter.write("\t<td>"+"Quantity"+"</td>");
			exportWriter.write("\t<td>"+"Amount"+"</td>");
			exportWriter.write("</tr>\n");

			String month = (exportLongMonth/100) + "/" + (exportLongMonth%100);
			ArrayList<Transaction> transactions = databaseAdapter.getMonthlyVisibleTransactions(month);
			for(int i=0; i<transactions.size(); i++)
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
				exportWriter.write("\t<td>"+(i+1)+"</td>");
				exportWriter.write("\t<td>"+transaction.getDate().getDisplayDate()+"</td>");
				//exportWriter.write("\t<td>"+DatabaseManager.getExactExpType(transactionNo)+"</td>");
				exportWriter.write("\t<td>"+transaction.getType()+"</td>");
				exportWriter.write("\t<td>"+transaction.getParticular()+"</td>");
				exportWriter.write("\t<td>"+transaction.getRate()+"</td>");
				exportWriter.write("\t<td>"+transaction.getQuantity()+"</td>");
				exportWriter.write("\t<td>"+transaction.getAmount()+"</td>");
				exportWriter.write("</font>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Income In This Month"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+databaseAdapter.getMonthlyIncome(month)+"</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Amount Spent In This Month"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+databaseAdapter.getMonthlyAmountSpent(month)+"</td>");
			exportWriter.write("</tr>\n");
			/*exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Amount In wallet"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+databaseAdapter.getWalletBalance()+"</td>");
			exportWriter.write("</tr>\n");
			ArrayList<Bank> banks = DatabaseManager.getAllBanks();
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+"Amount In "+banks.get(i).getName()+"</td>");
				exportWriter.write("\t<td>"+currencySymbol+banks.get(i).getBalance()+"</td>");
				exportWriter.write("</tr>\n");
			}*/
			exportWriter.write("</table>");
			
			exportWriter.write("</html>\n</body>");
			exportWriter.close();
			
			Toast.makeText(context, "Data Has Been Exported Successfully", Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			Toast.makeText(context, "Error In Saving To File\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
