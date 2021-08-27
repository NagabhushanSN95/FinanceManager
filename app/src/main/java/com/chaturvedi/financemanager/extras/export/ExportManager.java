package com.chaturvedi.financemanager.extras.export;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Month;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.functions.Constants;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

abstract class ExportManager
{
	static final int ACTION_EXPORT_PROGRESS = 1001;
	static final int ACTION_EXPORT_RESULT = 1002;
	static final int CODE_EXPORT_SUCCESSFUL = 1101;
	static final int CODE_EXPORT_FAILED = 1102;
	protected String currencySymbol = " ";
    private String sortBy;
	protected DecimalFormat formatter;

	protected Context context;
	ExportMetaData exportMetaData;
	Handler exportHandler;
	
	ExportManager(Context context, ExportMetaData exportMetaData, Handler exportHandler)
	{
		this.context = context;
		this.exportMetaData = exportMetaData;
		this.exportHandler = exportHandler;
		readPreferences();
		formatter = new DecimalFormat("0.00");
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = context.getSharedPreferences(Constants.ALL_PREFERENCES, 0);
		if (preferences.contains(Constants.KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(Constants.KEY_CURRENCY_SYMBOL, " ");
		}
		else
		{
			currencySymbol = " ";
		}
        sortBy = preferences.getString(Constants.KEY_SORT_TRANSACTIONS, Constants.VALUE_SORT_TRANSACTIONS_CREATED);
	}
	
	String getTitle()
	{
		String title;
		Date startDate = exportMetaData.getStartDate();
		Date endDate = exportMetaData.getEndDate();
		switch (exportMetaData.getIntervalType())
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
				throw new RuntimeException("Invalid Interval Type: " + exportMetaData
						.getIntervalType().toString());
		}
		return title;
	}
	
	protected List<Transaction> getTransactions()
	{
		return DatabaseAdapter.getInstance(context).getTransactions(null, exportMetaData
                .getStartDate(), exportMetaData.getEndDate(), null, null, false, sortBy, 0, -1);
	}
	
	File getExportFile() throws Exception
	{
		String exportFolderPath = "Chaturvedi/Finance Manager";
		File exportFolder = new File(Environment.getExternalStoragePublicDirectory("Android"),
				exportFolderPath);
		if (!exportFolder.exists())
		{
			if (!exportFolder.mkdirs())
			{
				throw new Exception("Failed to create File");
			}
		}
		return new File(exportFolder, exportMetaData.getExportFileName() + "." + exportMetaData
				.getExportFileFormat().toString());
	}
	
	abstract File export() throws Exception;
}
