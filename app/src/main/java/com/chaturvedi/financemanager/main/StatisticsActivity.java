package com.chaturvedi.financemanager.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.functions.Utilities;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class StatisticsActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private String currencySymbol = " ";
	
	// Filters
	private String intervalType = Constants.VALUE_ALL;
	private String intervalMonthYear = null;
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB) //Up Navigation Button is available only after Honeycomb. So, this is required
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_statistics);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		
		// Set initial filter options
		intervalType = Constants.VALUE_YEAR;
		intervalMonthYear = Calendar.getInstance().get(Calendar.YEAR) + "";
		
		buildLayout();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds childItems to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_statistics, menu);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(StatisticsActivity.this);
				return true;
			
			case R.id.action_filterStatistics:
				displayFilterOptions();
				return true;
		}
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		switch (requestCode)
		{
			case Constants.REQUEST_CODE_FILTERS:
				if (resultCode == RESULT_OK)
				{
					applyFilters(intent);
				}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}

		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(StatisticsActivity.this);
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(KEY_CURRENCY_SYMBOL, " ");
		}
		/*if(preferences.contains(KEY_NUM_EXP_TYPES))
		{
			numExpTypes = preferences.getInt(KEY_NUM_EXP_TYPES, 5);
		}*/
		
		DecimalFormat formatter1 = new DecimalFormat("#,##0.##");
		DecimalFormat formatter2 = new DecimalFormat("00");
		
		TableLayout statLayout = (TableLayout) findViewById(R.id.layout_statistics);
		statLayout.removeAllViews();
		
		TableRow titleRow = new TableRow(this);
		TableRow.LayoutParams titleRowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 
				TableRow.LayoutParams.WRAP_CONTENT);
		titleRow.setLayoutParams(titleRowParams);
		
		TextView monthsTitleView = new TextView(this);
		monthsTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		monthsTitleView.setText("Months");
		monthsTitleView.setBackgroundResource(R.drawable.border_black_2dp);
		titleRow.addView(monthsTitleView);
		for(int i=0; i<databaseAdapter.getNumVisibleExpenditureTypes(); i++)
		{
			TextView expTypeTitleView = new TextView(this);
			expTypeTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			expTypeTitleView.setText(databaseAdapter.getAllVisibleExpenditureTypes().get(i).getName());
			expTypeTitleView.setBackgroundResource(R.drawable.border_black_2dp);
			titleRow.addView(expTypeTitleView);
		}
		TextView totalExpTitleView = new TextView(this);
		totalExpTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		totalExpTitleView.setText("Total Expenses");
		totalExpTitleView.setBackgroundResource(R.drawable.border_black_2dp);
		titleRow.addView(totalExpTitleView);
		TextView incomeTitleView = new TextView(this);
		incomeTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		incomeTitleView.setText("Total Income");
		incomeTitleView.setBackgroundResource(R.drawable.border_black_2dp);
		titleRow.addView(incomeTitleView);
		TextView savingsTitleView = new TextView(this);
		savingsTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		savingsTitleView.setText("Savings");
		savingsTitleView.setBackgroundResource(R.drawable.border_black_2dp);
		titleRow.addView(savingsTitleView);
		TextView withdrawTitleView = new TextView(this);
		withdrawTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		withdrawTitleView.setText("Withdrawals");
		withdrawTitleView.setBackgroundResource(R.drawable.border_black_2dp);
		titleRow.addView(withdrawTitleView);
		statLayout.addView(titleRow);
		
		ArrayList<String> months;
		if (intervalType.equals(Constants.VALUE_ALL))
		{
			months = DatabaseManager.getExportableMonths(StatisticsActivity.this);
		}
		else if (intervalType.equals(Constants.VALUE_YEAR))
		{
			months = Date.getMonthsList(intervalMonthYear);
		}
		else
		{
			months = new ArrayList<String>();
			Utilities.logDebugMode("Statistics Filters", "Unknown Interval Type: " + intervalType);
		}
		
		for(int i=0; i<months.size(); i++)
		{
			TableRow monthRow = new TableRow(this);
			TableRow.LayoutParams monthRowParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 
					TableRow.LayoutParams.WRAP_CONTENT);
			monthRow.setLayoutParams(monthRowParams);
			
			TextView monthView = new TextView(this);
			monthView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			monthView.setText(months.get(i));
			monthView.setBackgroundResource(R.drawable.border_black_1dp);
			monthRow.addView(monthView);
			long longMonth = Date.getLongMonth(months.get(i));
			String month = (longMonth/100) + "/" + formatter2.format(longMonth%100);
			double[] monthlyCounters = databaseAdapter.getMonthlyCounters(month);
			
			for(int j=0; j<databaseAdapter.getNumVisibleExpenditureTypes()+4; j++)//+4 for Total Expenses,Incomes,Savings and Withdrawals
			{
				TextView expValueView = new TextView(this);
				expValueView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
				expValueView.setText(currencySymbol + formatter1.format(monthlyCounters[j]));
				expValueView.setBackgroundResource(R.drawable.border_black_1dp);
				monthRow.addView(expValueView);
			}
			statLayout.addView(monthRow);
		}
		
		// Totals Row
		TableRow totalsRow = new TableRow(this);
		totalsRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		
		TextView totalView = new TextView(this);
		totalView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		totalView.setText("Total");
		totalView.setBackgroundResource(R.drawable.border_black_1dp);
		totalsRow.addView(totalView);

		double[] totalCounters = databaseAdapter.getTotalCounters();
		for(int j=0; j<databaseAdapter.getNumVisibleExpenditureTypes()+4; j++)//+4 for Total Expenses,Incomes,Savings and Withdrawals
		{
			TextView expValueView = new TextView(this);
			expValueView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			expValueView.setText(currencySymbol + formatter1.format(totalCounters[j]));
			expValueView.setBackgroundResource(R.drawable.border_black_1dp);
			totalsRow.addView(expValueView);
		}
		statLayout.addView(totalsRow);
	}
	
	private void displayFilterOptions()
	{
		Intent filterIntent = new Intent(StatisticsActivity.this, StatisticsFilterActivity.class);
		
		// Send current filter state
		/*filterIntent.putExtra(Constants.KEY_INTERVAL_TYPE, transactionsDisplayIntervalType);
		if (transactionsDisplayIntervalType.equals(Constants.VALUE_MONTH))
		{
			filterIntent.putExtra(Constants.KEY_INTERVAL_TYPE_MONTH, intervalMonthYear);
		}
		else if (transactionsDisplayIntervalType.equals(Constants.VALUE_YEAR))
		{
			filterIntent.putExtra(Constants.KEY_INTERVAL_TYPE_YEAR, intervalMonthYear);
		}
		else if (transactionsDisplayIntervalType.equals(Constants.VALUE_CUSTOM))
		{
			filterIntent.putExtra(Constants.KEY_START_DATE, transactionsDisplayIntervalStartDate);
			filterIntent.putExtra(Constants.KEY_END_DATE, transactionsDisplayIntervalEndDate);
		}
		filterIntent.putStringArrayListExtra(Constants.KEY_ALLOWED_TRANSACTION_TYPES, allowedTransactionTypes);
		filterIntent.putExtra(Constants.KEY_SEARCH_KEYWORD, searchKeyword);*/
		
		startActivityForResult(filterIntent, Constants.REQUEST_CODE_FILTERS);
	}
	
	private void applyFilters(Intent intent)
	{
		intervalType = intent.getStringExtra(Constants.KEY_INTERVAL_TYPE);
		if (intervalType.equals(Constants.VALUE_ALL))
		{
			intervalMonthYear = null;
			/*transactionsDisplayIntervalStartDate = null;
			transactionsDisplayIntervalEndDate = null;*/
		}
		else if (intervalType.equals(Constants.VALUE_YEAR))
		{
			intervalMonthYear = intent.getStringExtra(Constants.KEY_INTERVAL_TYPE_YEAR);
			/*transactionsDisplayIntervalStartDate = null;
			transactionsDisplayIntervalEndDate = null;*/
		}
		/*else if (intervalType.equals(Constants.VALUE_MONTH))
		{
			intervalMonthYear = intent.getStringExtra(Constants.KEY_INTERVAL_TYPE_MONTH);
			transactionsDisplayIntervalStartDate = null;
			transactionsDisplayIntervalEndDate = null;
		}
		else if (intervalType.equals(Constants.VALUE_CUSTOM))
		{
			intervalMonthYear = null;
			transactionsDisplayIntervalStartDate = intent.getStringExtra(Constants.KEY_START_DATE);
			transactionsDisplayIntervalEndDate = intent.getStringExtra(Constants.KEY_END_DATE);
		}*/
		else
		{
			Toast.makeText(StatisticsActivity.this, "Unknown Interval Type. Can't filter.", Toast.LENGTH_LONG).show();
			intervalMonthYear = null;
			/*transactionsDisplayIntervalStartDate = null;
			transactionsDisplayIntervalEndDate = null;*/
		}
		
		/*allowedTransactionTypes = intent.getStringArrayListExtra(Constants.KEY_ALLOWED_TRANSACTION_TYPES);
		searchKeyword = intent.getStringExtra(Constants.KEY_SEARCH_KEYWORD);*/

//		refreshBodyLayout();
		buildLayout();
	}
}
