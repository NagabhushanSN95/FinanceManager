// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build.VERSION;
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

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class SummaryActivity extends Activity
{
	private static final String SHARED_PREFERENCES_DATABASE = "DatabaseInitialized";
	private static final String KEY_DATABASE_INITIALIZED = "database_initialized";
	
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
	
	private Intent detailsIntent;
	private Intent editBanksIntent;
	private Intent editExpenditureTypesIntent;
	private Intent statisticsIntent;
	private Intent settingsIntent;
	private Intent exportIntent;
	
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
		
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0);
		if(preferences.contains(KEY_DATABASE_INITIALIZED))
		{
			new DatabaseManager(this);
			DatabaseManager.readDatabase();
		}
		else
		{
			DatabaseManager.setContext(this);
			DatabaseManager.initializeDatabase();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(KEY_DATABASE_INITIALIZED, true);
			editor.commit();
		}
		
		buildLayout();
		setData();
		
		detailsIntent=new Intent(this, DetailsActivity.class);
		editBanksIntent=new Intent(this, EditBanksActivity.class);
		editExpenditureTypesIntent = new Intent(this, EditExpenditureTypesActivity.class);
		statisticsIntent=new Intent(this, StatisticsActivity.class);
		settingsIntent=new Intent(this, SettingsActivity.class);
		exportIntent=new Intent(this, ExportActivity.class);
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
				startActivityForResult(detailsIntent, 0);
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
				startActivity(settingsIntent);
				return true;
				
			case R.id.action_export:
				startActivityForResult(exportIntent, 0);
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
	
	/*@Override
	public void onBackPressed()
	{
		DatabaseManager.saveDatabase();
		super.onBackPressed();
	}*/
	
	@Override
	public void onPause()
	{
		super.onPause();
		DatabaseManager.saveDatabase();
	}
	
	/*@Override
	public void onStart()
	{
		super.onPause();
		DatabaseManager.readDatabase();
	}*/
	
	private void buildLayout()
	{
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
		//parentLayout.addView(line);
		
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
		//Toast.makeText(getApplicationContext(), "Check-Point 01 "+numBanks, Toast.LENGTH_SHORT).show();
		ArrayList<String> bankNames = DatabaseManager.getBankNames();
		//Toast.makeText(getApplicationContext(), "Check-Point 02 "+bankNames, Toast.LENGTH_SHORT).show();
		ArrayList<Double> bankBalances = DatabaseManager.getBankBalances();
		//Toast.makeText(getApplicationContext(), "Check-Point 03 "+bankBalances, Toast.LENGTH_SHORT).show();
		DecimalFormat formatter = new DecimalFormat("###,##0");
		try
		{
			// Set The Data
			for(int i=0; i<numBanks; i++)
			{
				nameViews.get(i).setText(bankNames.get(i));
				amountViews.get(i).setText(""+formatter.format(bankBalances.get(i)));
			}
			//Toast.makeText(getApplicationContext(), "Check-Point 04", Toast.LENGTH_SHORT).show();
			nameViews.get(numBanks).setText("Wallet");
			nameViews.get(numBanks+1).setText("Amount Spent");
			nameViews.get(numBanks+2).setText("Income");
			amountViews.get(numBanks).setText(""+formatter.format(DatabaseManager.getWalletBalance()));
			amountViews.get(numBanks+1).setText(""+formatter.format(DatabaseManager.getAmountSpent()));
			amountViews.get(numBanks+2).setText(""+formatter.format(DatabaseManager.getIncome()));
			//Toast.makeText(getApplicationContext(), "Check-Point 05", Toast.LENGTH_SHORT).show();
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), "Error In SummaryActivity.setData()\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}