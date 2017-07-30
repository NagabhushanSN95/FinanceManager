// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.financemanager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.DatabaseManager;

public class SummaryActivity extends Activity
{
	private static final String SHARED_PREFERENCES_VERSION = "app_version";
	private static final String KEY_VERSION = "version";
	private static int VERSION_NO;
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_CURRENCY_SYMBOL = "currency_symbols";
	private String currencySymbol = " ";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "transactions_display_interval";
	private String transactionsDisplayInterval = "Month";
	
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_AMOUNT_VIEWS;
	private int MARGIN_LEFT_NAME_VIEWS;
	
	private static LinearLayout parentLayout;
	private static LayoutParams parentLayoutParams;
	private static ArrayList<LinearLayout> layouts;
	private static ArrayList<TextView> nameViews;
	private static ArrayList<TextView> amountViews;
	
	private Intent transactionsIntent;
	private Intent editBanksIntent;
	private Intent editExpenditureTypesIntent;
	private Intent statisticsIntent;
	private Intent settingsIntent;
	private Intent helpIntent;
	private Intent extrasIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_summary);
		}
		else
		{
			setContentView(R.layout.activity_summary);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=(screenHeight-(DatabaseManager.getNumBanks()*100))/6;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*5/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*5/100;
		WIDTH_NAME_VIEWS=screenWidth*55/100;
		WIDTH_AMOUNT_VIEWS = screenWidth*35/100;
		MARGIN_LEFT_NAME_VIEWS = 5;
		
		try
		{
			VERSION_NO = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			Toast.makeText(getApplicationContext(), "Error In Retrieving Version No In " + 
					"SummaryActivity\\onCreate\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		SharedPreferences versionPreferences = getSharedPreferences(SHARED_PREFERENCES_VERSION, 0);
		SharedPreferences.Editor versionEditor = versionPreferences.edit();
		if(versionPreferences.contains(KEY_VERSION))
		{
			int versionNo = versionPreferences.getInt(KEY_VERSION, 0);
			if(versionNo != VERSION_NO)
			{
				runUpdateClasses(versionNo);
				versionEditor.putInt(KEY_VERSION, VERSION_NO);
			}
		}
		else
		{
			runUpdateClasses(0);
			versionEditor.putInt(KEY_VERSION, VERSION_NO);
		}
		versionEditor.commit();
		
		buildLayout();
		setData();
		
		transactionsIntent=new Intent(this, TransactionsActivity.class);
		editBanksIntent=new Intent(this, EditBanksActivity.class);
		editExpenditureTypesIntent = new Intent(this, EditExpenditureTypesActivity.class);
		statisticsIntent=new Intent(this, StatisticsActivity.class);
		settingsIntent=new Intent(this, SettingsActivity.class);
		helpIntent = new Intent(this, HelpActivity.class);
		extrasIntent = new Intent(this, ExtrasActivity.class);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		try
		{
			if(DatabaseManager.getNumTransactions()==0)
			{
				DatabaseManager.setContext(SummaryActivity.this);
				DatabaseManager.readDatabase();
				buildLayout();
				setData();
				Toast.makeText(getApplicationContext(), "Data Recovered And Restored In If Block", Toast.LENGTH_SHORT).show();
			}
		}
		catch(Exception e)
		{
			new DatabaseManager(SummaryActivity.this);
			DatabaseManager.readDatabase();
			buildLayout();
			setData();
			Toast.makeText(getApplicationContext(), "Data Recovered And Restored In Catch Block", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_summary, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.action_details:
				startActivityForResult(transactionsIntent, 0);
				return true;
				
			case R.id.action_edit_banks:
				startActivityForResult(editBanksIntent, 0);
				return true;
				
			case R.id.action_edit_expenditure_types:
				startActivity(editExpenditureTypesIntent);
				return true;
				
			case R.id.action_statistics:
				startActivity(statisticsIntent);
				return true;
				
			case R.id.action_settings:
				startActivityForResult(settingsIntent, 0);
				return true;
				
			case R.id.action_help:
				startHelpActivity();
				return true;
				
			case R.id.action_extras:
				startActivityForResult(extrasIntent, 0);
				return true;
		}
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		buildLayout();
		setData();
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(KEY_CURRENCY_SYMBOL, " ");
		}
		
		int numBanks = DatabaseManager.getNumBanks();
		
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT, 0);
		parentLayout.setLayoutParams(parentLayoutParams);
		parentLayout.removeAllViews();
		
		View line = new View(this);
		LayoutParams lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		line.setLayoutParams(lineParams);
		line.setBackgroundColor(Color.parseColor("#FFFFFF"));
		
		layouts=new ArrayList<LinearLayout>(numBanks+3);
		nameViews=new ArrayList<TextView>(numBanks+3);
		amountViews=new ArrayList<TextView>(numBanks+3);
		for(int i=0; i<numBanks+3; i++)
		{
			LayoutInflater layoutInflater = LayoutInflater.from(this);
			LinearLayout summaryLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_summary, null);
			if(i%2==0)
				summaryLayout.setBackgroundColor(Color.parseColor("#88CC00CC"));
			else
				summaryLayout.setBackgroundColor(Color.parseColor("#880044FF"));
			
			TextView nameView = (TextView)summaryLayout.findViewById(R.id.name);
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams.WRAP_CONTENT);
			nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			nameView.setLayoutParams(nameViewParams);
			
			TextView currencySymbolView = (TextView)summaryLayout.findViewById(R.id.currencySymbol);
			currencySymbolView.setText(currencySymbol);
			
			TextView amountView = (TextView)summaryLayout.findViewById(R.id.amount);
			LayoutParams amountViewParams = new LayoutParams(WIDTH_AMOUNT_VIEWS, LayoutParams.WRAP_CONTENT);
			amountView.setLayoutParams(amountViewParams);
			
			layouts.add(summaryLayout);
			nameViews.add(nameView);
			amountViews.add(amountView);
			parentLayout.addView(summaryLayout);
		}
		
		line = new View(this);
		lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		line.setLayoutParams(lineParams);
		line.setBackgroundColor(Color.parseColor("#FFFFFF"));
		parentLayout.addView(line);
		
	}
	
	private void setData()
	{
		int numBanks = DatabaseManager.getNumBanks();
		ArrayList<Bank> banks = DatabaseManager.getAllBanks();
		DecimalFormat formatter = new DecimalFormat("###,##0.##");
		try
		{
			// Set The Data
			for(int i=0; i<numBanks; i++)
			{
				nameViews.get(i).setText(banks.get(i).getName());
				amountViews.get(i).setText(""+formatter.format(banks.get(i).getBalance()));
			}
			nameViews.get(numBanks).setText("Wallet");
			nameViews.get(numBanks+1).setText("Amount Spent");
			nameViews.get(numBanks+2).setText("Income");
			amountViews.get(numBanks).setText(""+formatter.format(DatabaseManager.getWalletBalance()));
			
			SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
			
			if(preferences.contains(KEY_TRANSACTIONS_DISPLAY_INTERVAL))
			{
				transactionsDisplayInterval=preferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
			}
			
			if(transactionsDisplayInterval.equals("Month"))
			{
				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				long currentMonth = year*100+month;
				amountViews.get(numBanks+1).setText(""+formatter.format(DatabaseManager.getMonthlyAmountSpent(currentMonth)));
				amountViews.get(numBanks+2).setText(""+formatter.format(DatabaseManager.getMonthlyIncome(currentMonth)));
			}
			else if(transactionsDisplayInterval.equals("Year"))
			{
				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				//amountViews.get(numBanks+1).setText(""+formatter.format(DatabaseManager.getYearlyAmountSpent(year)));
				//amountViews.get(numBanks+2).setText(""+formatter.format(DatabaseManager.getYearlyIncome(year)));
			}
			else
			{
				amountViews.get(numBanks+1).setText(""+formatter.format(DatabaseManager.getTotalAmountSpent()));
				amountViews.get(numBanks+2).setText(""+formatter.format(DatabaseManager.getTotalIncome()));
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), "Error In SummaryActivity.setData()\n"+e.getMessage(), 
					Toast.LENGTH_LONG).show();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void startHelpActivity()
	{
		int androidVersionNo = android.os.Build.VERSION.SDK_INT;
		if(androidVersionNo < Build.VERSION_CODES.JELLY_BEAN)
		{
			startActivity(helpIntent);
			overridePendingTransition(R.anim.new_activity_enter, R.anim.old_activity_leave);
		}
		else
		{
			Bundle animationBundle = ActivityOptions.makeCustomAnimation(getApplicationContext(), 
					R.anim.new_activity_enter, R.anim.old_activity_leave).toBundle();
			startActivity(helpIntent, animationBundle);
		}
	}
	
	private void runUpdateClasses(int oldVersionNo)
	{
		
	}
}