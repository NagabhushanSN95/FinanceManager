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
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

public class PdfExportManager extends ExportManager
{
	PdfExportManager(Context context, ExportMetaData exportMetaData, Handler exportHandler)
	{
		super(context, exportMetaData, exportHandler);
	}
	
	public File export() throws Exception
	{
		File exportFile = getExportFile();
		String title = getTitle();
		
		Document document = new Document(PageSize.A4);
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(exportFile));
		writer.setPageEvent(new Footer());
		
		document.open();
		document.addTitle(title);
		document.addCreator("Finance Manager");
		document.addCreationDate();
		
		document.add(getTitleParagraph(title));
		document.add(Chunk.NEWLINE);
		document.add(getTransactionsTable());
		document.add(Chunk.NEWLINE);
		document.add(getSummaryTable());
		
		document.close();
		return exportFile;
	}
	
	private Paragraph getTitleParagraph(String title)
	{
		Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 15, Font.BOLDITALIC, BaseColor
				.BLACK);
		Paragraph titleParagraph = new Paragraph(title, titleFont);
		titleParagraph.setAlignment(Element.ALIGN_CENTER);
		return titleParagraph;
	}
	
	private PdfPTable getTransactionsTable() throws DocumentException
	{
		List<Transaction> transactions = getTransactions();
		int numColumns = 4;
		numColumns = exportMetaData.isIncludeTransactionType() ? numColumns + 1 : numColumns;
		numColumns = exportMetaData.isIncludeRateQuantity() ? numColumns + 2 : numColumns;
		PdfPTable transactionsTable = new PdfPTable(numColumns);
		transactionsTable.setWidths(getColumnWidths(numColumns));
		transactionsTable.setHeaderRows(1);
		addTableHeader(transactionsTable);
		
		int exportProgress = 0;
		for (int i = 0; i < transactions.size(); i++)
		{
			addTransaction(i, transactions.get(i), transactionsTable);
			
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
		return transactionsTable;
	}
	
	private int[] getColumnWidths(int numColumns)
	{
		switch (numColumns)
		{
			case 4:
				// Sl No, Date, Particulars, Amount
				return new int[]{7, 13, 65, 15};
			
			case 5:
				// Sl No, Date, Type, Particulars, Amount
				return new int[]{7, 13, 10, 40, 15};
			
			case 6:
				// Sl No, Date, Particulars, Rate, Quantity, Amount
				return new int[]{7, 13, 45, 10, 10, 15};
			
			case 7:
				// Sl No, Date, Type, Particulars, Rate, Quantity, Amount
				return new int[]{7, 13, 10, 35, 10, 10, 15};
			
			default:
				throw new RuntimeException("Invalid Number of Columns: " + numColumns);
		}
	}
	
	private void addTableHeader(PdfPTable transactionsTable)
	{
		Font headerFont = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.BOLD, BaseColor.BLACK);
		transactionsTable.addCell(new PdfPCell(new Phrase("Sl No", headerFont)));
		transactionsTable.addCell(new PdfPCell(new Phrase("Date", headerFont)));
		if (exportMetaData.isIncludeTransactionType())
		{
			transactionsTable.addCell(new PdfPCell(new Phrase("Type", headerFont)));
		}
		transactionsTable.addCell(new PdfPCell(new Phrase("Particulars", headerFont)));
		if (exportMetaData.isIncludeRateQuantity())
		{
			transactionsTable.addCell(new PdfPCell(new Phrase("Rate", headerFont)));
			transactionsTable.addCell(new PdfPCell(new Phrase("Quantity", headerFont)));
		}
		transactionsTable.addCell(new PdfPCell(new Phrase("Amount", headerFont)));
	}
	
	private void addTransaction(int i, Transaction transaction, PdfPTable transactionsTable)
	{
		BaseColor color;
		if (transaction.getType().contains("Debit"))
		{
			color = BaseColor.RED;
		}
		else if (transaction.getType().contains("Credit"))
		{
			color = new BaseColor(0, 204, 0);
		}
		else if (transaction.getType().contains("Transfer"))
		{
			color = BaseColor.BLUE;
		}
		else
		{
			color = BaseColor.BLACK;
		}
		Font transactionFont = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, color);
		
		transactionsTable.addCell(new Phrase(Integer.toString(i + 1), transactionFont));
		transactionsTable.addCell(new Phrase(transaction.getDate().getDisplayDate("/"),
				transactionFont));
		if (exportMetaData.isIncludeTransactionType())
		{
			TransactionTypeParser parser = new TransactionTypeParser(context, transaction
					.getType());
			transactionsTable.addCell(new Phrase(parser.getTransactionTypeForDisplay(),
					transactionFont));
		}
		transactionsTable.addCell(new Phrase(transaction.getDisplayParticular(context),
				transactionFont));
		if (exportMetaData.isIncludeRateQuantity())
		{
			transactionsTable.addCell(new Phrase(formatter.format(transaction.getRate()),
					transactionFont));
			transactionsTable.addCell(new Phrase(formatter.format(transaction.getQuantity()),
					transactionFont));
		}
		transactionsTable.addCell(new Phrase(formatter.format(transaction.getAmount()),
				transactionFont));
	}
	
	private PdfPTable getSummaryTable()
	{
		Font summaryFont = new Font(Font.FontFamily.TIMES_ROMAN, 10, Font.NORMAL, BaseColor.BLACK);
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		PdfPTable summaryTable = new PdfPTable(2);
		summaryTable.setHeaderRows(0);
		summaryTable.addCell(new Phrase("Total Income", summaryFont));
		summaryTable.addCell(new Phrase(currencySymbol + formatter.format(databaseAdapter
				.getIncome(exportMetaData.getStartDate(), exportMetaData.getEndDate())),
				summaryFont));
		summaryTable.addCell(new Phrase("Total Expenditure", summaryFont));
		summaryTable.addCell(new Phrase(currencySymbol + formatter.format(databaseAdapter
				.getAmountSpent(exportMetaData.getStartDate(), exportMetaData.getEndDate())),
				summaryFont));
		
		if (exportMetaData.isIncludeCurrentWalletBankBalances())
		{
			for (Wallet wallet : databaseAdapter.getAllVisibleWallets())
			{
				summaryTable.addCell(new Phrase("Amount In " + wallet.getName(), summaryFont));
				summaryTable.addCell(new Phrase(currencySymbol + formatter.format(wallet
						.getBalance()), summaryFont));
			}
			for (Bank bank : databaseAdapter.getAllVisibleBanks())
			{
				summaryTable.addCell(new Phrase("Amount In " + bank.getName(), summaryFont));
				summaryTable.addCell(new Phrase(currencySymbol + formatter.format(bank.getBalance
						()), summaryFont));
			}
		}
		return summaryTable;
	}
	
	private class Footer extends PdfPageEventHelper
	{
		Font footerFont = new Font(Font.FontFamily.TIMES_ROMAN, 5, Font.ITALIC);
		
		public void onEndPage(PdfWriter writer, Document document)
		{
			PdfContentByte cb = writer.getDirectContent();
			Phrase footer = new Phrase("Exported by Finance Manager on " + new Date(Calendar
					.getInstance()).getDisplayDate("/"), footerFont);
			ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
					footer,
					(document.right() - document.left()) / 2 + document.leftMargin(),
					document.bottom() - 10, 0);
		}
	}
}
