// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.extras.export;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Bank;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.datastructures.Wallet;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.List;

public class HtmlExportManager extends ExportManager
{
	HtmlExportManager(Context context, ExportMetaData exportMetaData, Handler exportHandler)
	{
		super(context, exportMetaData, exportHandler);
	}
	
	public File export() throws Exception
	{
		File exportFile = getExportFile();
		String title = getTitle();
		List<Transaction> transactions = getTransactions();
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		BufferedWriter exportWriter = new BufferedWriter(new FileWriter(exportFile));
		
		exportWriter.write("<html>\n<body>");
		exportWriter.write("<h1>" + title + "</h1>\n");
		
		exportWriter.write("<table border=\"1\" style=\"width:600px\">");
		exportWriter.write("<tr>\n");
		exportWriter.write("\t<td>" + "Sl No" + "</td>");
		exportWriter.write("\t<td>" + "Date" + "</td>");
		if (exportMetaData.isIncludeTransactionType())
		{
			exportWriter.write("\t<td>" + "Type" + "</td>");
		}
		exportWriter.write("\t<td>" + "Particulars" + "</td>");
		if (exportMetaData.isIncludeRateQuantity())
		{
			exportWriter.write("\t<td>" + "Rate" + "</td>");
			exportWriter.write("\t<td>" + "Quantity" + "</td>");
		}
		exportWriter.write("\t<td>" + "Amount" + "</td>");
		exportWriter.write("</tr>\n");
		
		int exportProgress = 0;
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
			exportWriter.write("\t<td>" + transaction.getDate().getDisplayDate("/") + "</td>");
			if (exportMetaData.isIncludeTransactionType())
			{
				TransactionTypeParser parser = new TransactionTypeParser(context, transaction
						.getType());
				exportWriter.write("\t<td>" + parser.getTransactionTypeForDisplay() + "</td>");
			}
			exportWriter.write("\t<td>" + transaction.getDisplayParticular(context) + "</td>");
			if (exportMetaData.isIncludeRateQuantity())
			{
				exportWriter.write("\t<td>" + transaction.getRate() + "</td>");
				exportWriter.write("\t<td>" + transaction.getQuantity() + "</td>");
			}
			exportWriter.write("\t<td>" + formatter.format(transaction.getAmount()) + "</td>");
			exportWriter.write("</font>");
			exportWriter.write("</tr>\n");
			
			if (100 * (i + 1) / transactions.size() > exportProgress)
			{
				// Send ExportProgressMessage
				exportProgress = 100 * (i + 1) / transactions.size();
				Message exportProgressMessage = exportHandler.obtainMessage(ExportManager
						.ACTION_EXPORT_PROGRESS);
				exportProgressMessage.arg1 = exportProgress;
				exportProgressMessage.sendToTarget();
			}
		}
		exportWriter.write("</table>");
		
		exportWriter.write("<table border=\"1\" style=\"width:600px\">");
		exportWriter.write("<tr>\n");
		exportWriter.write("\t<td>" + "Total Income" + "</td>");
		exportWriter.write("\t<td>" + currencySymbol + formatter.format(databaseAdapter
				.getIncome(exportMetaData.getStartDate(), exportMetaData.getEndDate())) + "</td>");
		exportWriter.write("</tr>\n");
		exportWriter.write("<tr>\n");
		exportWriter.write("\t<td>" + "Total Expenditure" + "</td>");
		exportWriter.write("\t<td>" + currencySymbol + formatter.format(databaseAdapter
				.getAmountSpent(exportMetaData.getStartDate(), exportMetaData.getEndDate())) +
				"</td>");
		exportWriter.write("</tr>\n");
		if (exportMetaData.isIncludeCurrentWalletBankBalances())
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
		exportWriter.write("<footer><p>Exported by Finance Manager on " + new Date(Calendar
				.getInstance()
		).getDisplayDate("/") + "</p></footer>");
		
		exportWriter.write("</html>\n</body>");
		exportWriter.close();
		return exportFile;
	}
}
