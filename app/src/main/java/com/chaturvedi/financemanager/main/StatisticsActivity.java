package com.chaturvedi.financemanager.main;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Date;

public class StatisticsActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	//private static final String KEY_NUM_EXP_TYPES = "NumExpTypes";
	//private int numExpTypes = 5;
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private String currencySymbol = " ";
	
	/*private int screenWidth;
	private int screenHeight;
	private int WIDTH_NAMES;
	private int WIDTH_AMOUNTS;
	private int MARGIN_LEFT_PARENT_LAYOUT;*/

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
		
		/*DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		WIDTH_NAMES=screenWidth*50/100;
		WIDTH_AMOUNTS=screenWidth*30/100;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*10/100;*/
		
		buildLayout();
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(StatisticsActivity.this);
				return true;
		}
		return true;
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
		
		ArrayList<String> months = DatabaseManager.getExportableMonths(StatisticsActivity.this);
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
}
