// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.extras;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.chaturvedi.customviews.IntervalSelector;
import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Month;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.functions.Constants;

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
		
		Button exportButton = (Button) findViewById(R.id.button_export);
		exportButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				exportData();
			}
		});
	}
	
	private String getDefaultExportFileName(IntervalSelector intervalSelector)
	{
		String filename;
		switch (intervalSelector.getSelectedIntervalType())
		{
			case INTERVAL_MONTH:
				filename = "Finance Manager Statement for " + intervalSelector.getSelectedMonth()
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
		return filename + ".doc";
	}
	
	private void exportData()
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
		boolean includeTransactionType = ((CheckBox) findViewById(R.id
				.checkBox_includeTransactionType)).isChecked();
		boolean includeRateQuantity = ((CheckBox) findViewById(R.id.checkBox_includeRateQuantity))
				.isChecked();
		boolean includeWalletBankBalances = ((CheckBox) findViewById(R.id
				.checkBox_includeWalletBankBalances)).isChecked();
		int result = new ExportManager(getApplicationContext(), exportFileName, intervalSelector
				.getSelectedIntervalType(), startDate, endDate, includeTransactionType,
				includeRateQuantity, includeWalletBankBalances).export();
		
		String title, message;
		switch (result)
		{
			case 0:
				title = "Export Successful";
				message = "Folder: Internal Storage/Android/Chaturvedi/Finance Manager\n" +
						"File Name: " + exportFileName;
				break;
			
			case 1:
				title = "Export Failed";
				message = "Failed to export data. Unable to create file. Please contact developer " +
						"if problem persists.";
				break;
			
			case 2:
				title = "Export Failed";
				message = "Failed to export data. Exception while exporting. Please contact " +
						"developer if problem persists.";
				break;
			
			default:
				title = "Export Failed";
				message = "Failed to export data. Unknown error. Please contact developer if " +
						"problem persists.";
				break;
		}
		AlertDialog.Builder exportDialog = new AlertDialog.Builder(ExportActivity.this);
		exportDialog.setTitle(title);
		exportDialog.setMessage(message);
		exportDialog.setPositiveButton("Ok", null);
		exportDialog.show();
	}
}
