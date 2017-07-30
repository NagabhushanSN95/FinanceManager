package com.chaturvedi.financemanager;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;

public class ExpenditureSetupActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_APP_VERSION = "AppVersionNo";
	private final int CURRENT_APP_VERSION_NO = 75;
	private static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private int splashDuration = 5000;
	private static final String KEY_QUOTE_NO = "QuoteNo";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
	
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_TEXT_FIELDS;
	private int MARGIN_TOP_TEXT_FIELDS;
	private int MARGIN_LEFT_TEXT_FIELDS;

	private final int NUM_EXPENDITURE_TYPES=5;
	
	private ArrayList<EditText> typeTextFields;
	private ArrayList<LayoutParams> typeFieldParams;
	private ArrayList<String> expenditureTypes;
	
	private Intent summaryIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_setup_expenditure);
		}
		else
		{
			setContentView(R.layout.activity_setup_expenditure);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		WIDTH_TEXT_FIELDS = screenWidth*60/100;
		MARGIN_TOP_TEXT_FIELDS = screenHeight*5/100;
		MARGIN_LEFT_TEXT_FIELDS = screenWidth*20/100;
		
		buildLayout();
		summaryIntent = new Intent(this, SummaryActivity.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		if(VERSION.SDK_INT>10)
		{
			getMenuInflater().inflate(R.menu.activity_startup_banks, menu);
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.next:
				saveToDatabase();
				startActivity(summaryIntent);
				finish();
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
		
		typeTextFields = new ArrayList<EditText>();
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type1));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type2));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type3));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type4));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type5));
		
		typeFieldParams = new ArrayList<LayoutParams>();
		for(int i=0; i<NUM_EXPENDITURE_TYPES; i++)
		{
			LayoutParams params = new LayoutParams(WIDTH_TEXT_FIELDS, LayoutParams.WRAP_CONTENT);
			params.setMargins(MARGIN_LEFT_TEXT_FIELDS, MARGIN_TOP_TEXT_FIELDS, 0, 0);
			typeTextFields.get(i).setLayoutParams(params);
			typeFieldParams.add(params);
		}
		if(VERSION.SDK_INT<=10)
		{
			Button nextButton = (Button)findViewById(R.id.button_finish);
			nextButton.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v)
				{
					saveToDatabase();
					startActivity(summaryIntent);
					finish();
				}
			});
		}
	}

	private void saveToDatabase()
	{
		String[] hints = new String[5];
		hints[0] = getResources().getString(R.string.hint_exp01);
		hints[1] = getResources().getString(R.string.hint_exp02);
		hints[2] = getResources().getString(R.string.hint_exp03);
		hints[3] = getResources().getString(R.string.hint_exp04);
		hints[4] = getResources().getString(R.string.hint_exp05);
		//String[] hints = {"Studies", "Food", "Travels", "Entertainment", "Others"};
		
		expenditureTypes = new ArrayList<String>();
		for(int i=0; i<NUM_EXPENDITURE_TYPES; i++)
		{
			String type = typeTextFields.get(i).getText().toString();
			if(type.length()!=0)
			{
				expenditureTypes.add(type);
			}
			else
			{
				expenditureTypes.add(hints[i]);
			}
		}
		
		DatabaseManager.setAllExpenditureTypes(expenditureTypes);
		
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		
		int appVersionNo;
		try
		{
			appVersionNo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			appVersionNo = CURRENT_APP_VERSION_NO;
			Toast.makeText(getApplicationContext(), "Error In Retrieving Version No In " + 
					"ExpenditureSetupActivity\\saveToDatabase\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		editor.putInt(KEY_APP_VERSION, appVersionNo);
		editor.putBoolean(KEY_DATABASE_INITIALIZED, true);
		editor.putInt(KEY_SPLASH_DURATION, splashDuration);
		editor.putInt(KEY_QUOTE_NO, 0);
		editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
		editor.putString(KEY_CURRENCY_SYMBOL, " ");
		editor.putBoolean(KEY_RESPOND_BANK_SMS, true);
		editor.putBoolean(KEY_BANK_SMS_ARRIVED, false);
		editor.commit();
	}
}
