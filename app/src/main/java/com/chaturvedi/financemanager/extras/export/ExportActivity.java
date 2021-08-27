// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.extras.export;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.chaturvedi.customviews.IntervalSelector;
import com.chaturvedi.customviews.ProgressiveWaitDialogBuilder;
import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Month;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.functions.Constants;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Calendar;

public class ExportActivity extends Activity
{
	private IntervalSelector intervalSelector;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_export);
		buildLayout();
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if ((this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		ExportMetaData exportPreferences = readPreferences();
		intervalSelector = (IntervalSelector) findViewById(R.id.intervalSelector);
		intervalSelector.setTitle("Select Interval to export");
		intervalSelector.setOnIntervalChangeListener(new IntervalSelector
				.OnIntervalChangeListener()
		{
			@Override
			public void onIntervalChange()
			{
				String filename = getDefaultExportFileName(intervalSelector);
				EditText exportFileNameField = (EditText) findViewById(R.id
						.editText_exportFileName);
				exportFileNameField.setText(filename);
			}
		});
		intervalSelector.setIntervalType(exportPreferences.getIntervalType());
		
		((CheckBox) findViewById(R.id.checkBox_includeTransactionType)).setChecked
				(exportPreferences.isIncludeTransactionType());
		((CheckBox) findViewById(R.id.checkBox_includeRateQuantity)).setChecked(exportPreferences
				.isIncludeRateQuantity());
		((CheckBox) findViewById(R.id.checkBox_includeWalletBankBalances)).setChecked
				(exportPreferences.isIncludeCurrentWalletBankBalances());
		
		Spinner exportFileFormatSpinner = (Spinner) findViewById(R.id.spinner_exportFileFormat);
		ArrayAdapter<ExportFileFormat> exportFileFormatAdapter = new ArrayAdapter<>(ExportActivity
				.this, android.R.layout.simple_spinner_item, ExportFileFormat.values());
		exportFileFormatSpinner.setAdapter(exportFileFormatAdapter);
		exportFileFormatSpinner.setSelection(exportPreferences.getExportFileFormat().ordinal());
		
		Button exportButton = (Button) findViewById(R.id.button_export);
		exportButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				ExportMetaData exportMetaData = readExportData();
				exportDataInSeparateThread(exportMetaData);
				savePreferences(exportMetaData);
			}
		});
	}
	
	private ExportMetaData readExportData()
	{
		Date startDate, endDate;
		switch (intervalSelector.getSelectedIntervalType())
		{
			case INTERVAL_MONTH:
				Month selectedMonth = intervalSelector.getSelectedMonth();
				int selectedYear = intervalSelector.getSelectedYear();
				startDate = new Date(selectedYear, selectedMonth.getMonthNo(), 1);
				endDate = new Date(selectedYear, selectedMonth.getMonthNo(), selectedMonth
						.getLastDate(selectedYear));
				break;
			
			case INTERVAL_YEAR:
				selectedYear = intervalSelector.getSelectedYear();
				startDate = new Date(selectedYear, 1, 1);
				endDate = new Date(selectedYear, 12, 31);
				break;
			
			case INTERVAL_ALL:
				startDate = new Date(Constants.YEAR_MINIMUM_VALUE, 1, 1);
				endDate = new Date(Calendar.getInstance());
				break;
			
			case INTERVAL_CUSTOM:
				startDate = intervalSelector.getFromDate();
				endDate = intervalSelector.getToDate();
				break;
			
			default:
				throw new RuntimeException("Invalid Interval Type");
		}
		
		EditText exportFileNameField = (EditText) findViewById(R.id.editText_exportFileName);
		String exportFileName = exportFileNameField.getText().toString();
		Spinner exportFileFormatSpinner = (Spinner) findViewById(R.id
				.spinner_exportFileFormat);
		
		boolean includeTransactionType = ((CheckBox) findViewById(R.id
				.checkBox_includeTransactionType)).isChecked();
		boolean includeRateQuantity = ((CheckBox) findViewById(R.id
				.checkBox_includeRateQuantity))
				.isChecked();
		boolean includeWalletBankBalances = ((CheckBox) findViewById(R.id
				.checkBox_includeWalletBankBalances)).isChecked();
		
		return new ExportMetaData(exportFileName, (ExportFileFormat)
				exportFileFormatSpinner.getSelectedItem(), intervalSelector
				.getSelectedIntervalType(), startDate, endDate, includeTransactionType,
				includeRateQuantity, includeWalletBankBalances);
	}
	
	private String getDefaultExportFileName(IntervalSelector intervalSelector)
	{
		String filename;
		switch (intervalSelector.getSelectedIntervalType()) {
			case INTERVAL_MONTH:
				DecimalFormat formatter = new DecimalFormat("00");
				filename = intervalSelector.getSelectedYear() + formatter.format(intervalSelector.getSelectedMonth().getMonthNo()) + " Finance Manager Statement for " + intervalSelector.getSelectedMonth()
						.toString() + " - " + intervalSelector.getSelectedYear();
				break;

			case INTERVAL_YEAR:
				filename = "Finance Manager Statement for " + intervalSelector.getSelectedYear();
				break;

			case INTERVAL_ALL:
				DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(ExportActivity.this);
				filename = "Finance Manager Statement from " + databaseAdapter
						.getFirstTransactionDate().getDisplayDate("-") + " to " +
						databaseAdapter.getLastTransactionDate().getDisplayDate("-");
				break;
			
			case INTERVAL_CUSTOM:
				filename = "Finance Manager Statement from " + intervalSelector.getFromDate()
						.getDisplayDate("-") + " to " + intervalSelector.getToDate()
						.getDisplayDate("-");
				break;
			
			default:
				throw new RuntimeException("Invalid Interval Type: " + intervalSelector
						.getSelectedIntervalType().toString());
		}
		return filename;
	}
	
	private void exportDataInSeparateThread(final ExportMetaData exportMetaData)
	{
		ProgressiveWaitDialogBuilder waitDialogBuilder = new ProgressiveWaitDialogBuilder
				(ExportActivity.this);
		waitDialogBuilder.setTitle("Exporting Data");
		waitDialogBuilder.setWaitText("Please wait while your data is exported...");
		waitDialogBuilder.setProgress(0);
		waitDialogBuilder.setCancelable(false);
		AlertDialog waitDialog = waitDialogBuilder.show();
		
		final Handler exportHandler = getExportHandler(waitDialogBuilder, waitDialog);
		Thread exportThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				exportData(exportMetaData, exportHandler);
			}
		});
		exportThread.start();
	}
	
	private Handler getExportHandler(final ProgressiveWaitDialogBuilder waitDialogBuilder, final
	AlertDialog waitDialog)
	{
		return new Handler(Looper.getMainLooper())
		{
			@Override
			public void handleMessage(Message exportMessage)
			{
				switch (exportMessage.what)
				{
					case ExportManager.ACTION_EXPORT_PROGRESS:
						int exportProgress = exportMessage.arg1;
						waitDialogBuilder.setProgress(exportProgress);
						break;
					
					case ExportManager.ACTION_EXPORT_RESULT:
						String title, message;
						switch (exportMessage.arg1)
						{
							case ExportManager.CODE_EXPORT_SUCCESSFUL:
								title = "Export Successful";
								message = (String) exportMessage.obj;
								break;
							
							case ExportManager.CODE_EXPORT_FAILED:
								title = "Export Failed";
								message = "Please try again. Please contact developer if the " +
										"problem persists.\nException:" + exportMessage.obj;
								break;
							
							default:
								title = "Unknown result";
								message = "Please try again. Please contact developer if the " +
										"problem persists";
						}
						AlertDialog.Builder exportDialog = new AlertDialog.Builder(ExportActivity
								.this);
						exportDialog.setTitle(title);
						exportDialog.setMessage(message);
						exportDialog.setPositiveButton("Ok", null);
						exportDialog.show();
						waitDialog.dismiss();
					
					default:
						super.handleMessage(exportMessage);
				}
				
			}
		};
	}
	
	private void exportData(ExportMetaData exportMetaData, Handler exportHandler)
	{
		try
		{
			ExportManager exportManager;
			switch (exportMetaData.getExportFileFormat())
			{
				case PDF:
					exportManager = new PdfExportManager(getApplicationContext(), exportMetaData,
							exportHandler);
					break;
				
				case DOC:
				case HTML:
					exportManager = new HtmlExportManager(getApplicationContext(), exportMetaData,
							exportHandler);
					break;
				
				default:
					throw new Exception("Invalid File Format: " + exportMetaData
							.getExportFileFormat().toString());
			}
			File exportFile = exportManager.export();
			Message exportResult = exportHandler.obtainMessage(ExportManager.ACTION_EXPORT_RESULT,
					"Folder: " +
					exportFile.getParent() + "\nFile Name: " + exportFile.getName());
			exportResult.arg1 = ExportManager.CODE_EXPORT_SUCCESSFUL;
			exportResult.sendToTarget();
		}
		catch (Exception e)
		{
			Log.e("Export", e.getLocalizedMessage(), e);
			Message exportResult = exportHandler.obtainMessage(ExportManager.ACTION_EXPORT_RESULT,
					e.getLocalizedMessage());
			exportResult.arg1 = ExportManager.CODE_EXPORT_FAILED;
			exportResult.sendToTarget();
		}
	}
	
	private ExportMetaData readPreferences()
	{
		ExportMetaData exportPreferences = new ExportMetaData();
		SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES,
				MODE_PRIVATE);
		exportPreferences.setIntervalType(IntervalSelector.IntervalType.fromString(preferences
				.getString(Constants.KEY_EXPORT_INTERVAL_TYPE, "Month")));
		exportPreferences.setIncludeTransactionType(preferences.getBoolean(Constants
				.KEY_EXPORT_INCLUDE_TRANSACTION_TYPE, true));
		exportPreferences.setIncludeRateQuantity(preferences.getBoolean(Constants
				.KEY_EXPORT_INCLUDE_RATE_QUANTITY, false));
		exportPreferences.setIncludeCurrentWalletBankBalances(preferences.getBoolean(Constants
				.KEY_EXPORT_INCLUDE_CURRENT_BALANCES, false));
		exportPreferences.setExportFileFormat(ExportFileFormat.fromString(preferences.getString
				(Constants.KEY_EXPORT_FILE_FORMAT, "pdf")));
		return exportPreferences;
	}
	
	private void savePreferences(ExportMetaData exportMetaData)
	{
		SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(Constants.KEY_EXPORT_INTERVAL_TYPE, exportMetaData.getIntervalType()
				.toString());
		editor.putBoolean(Constants.KEY_EXPORT_INCLUDE_TRANSACTION_TYPE, exportMetaData
				.isIncludeTransactionType());
		editor.putBoolean(Constants.KEY_EXPORT_INCLUDE_RATE_QUANTITY, exportMetaData
				.isIncludeRateQuantity());
		editor.putBoolean(Constants.KEY_EXPORT_INCLUDE_CURRENT_BALANCES, exportMetaData
				.isIncludeCurrentWalletBankBalances());
		editor.putString(Constants.KEY_EXPORT_FILE_FORMAT, exportMetaData.getExportFileFormat()
				.toString());
		editor.apply();
	}
}
