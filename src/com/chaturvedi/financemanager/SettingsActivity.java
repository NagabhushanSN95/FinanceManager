package com.chaturvedi.financemanager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private int splashDuration = 5000;
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	
	private CheckBox splashCheckBox;
	private static boolean enableSplash=true;
	private CheckBox bankSmsCheckBox;
	private static boolean respondBankMessages = true;
	private Spinner currencySymbolsList;
	private ArrayList<String> currencySymbols;
	private String currencySymbolSelected = "Rs ";
	private static final int NUM_CURRENCY_SYMBOLS=3;
	Spinner transactionsDisplayOptionsList;
	private static String transactionsDisplayInterval = "Month";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_settings);
		}
		else
		{
			setContentView(R.layout.activity_settings);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		readPreferences();
		readCurrencySymbolsFile();
		buildLayout();
		
	}

	@Override
	public void onPause()
	{
		super.onPause();
		savePreferences();
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		splashCheckBox=(CheckBox)findViewById(R.id.checkBox_splash);
		splashCheckBox.setChecked(enableSplash);
		bankSmsCheckBox = (CheckBox)findViewById(R.id.checkBox_bank_sms);
		bankSmsCheckBox.setChecked(respondBankMessages);
		currencySymbolsList = (Spinner)findViewById(R.id.list_currencySymbols);
		currencySymbolsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currencySymbols));
		currencySymbolsList.setSelection(getCurrencySymbolPosition(currencySymbolSelected));
		
		transactionsDisplayOptionsList = (Spinner)findViewById(R.id.list_transactionsDisplayOptions);
		//String[] transactionsDisplayOptions = { "Month", "Year", "All" };
		String[] transactionsDisplayOptions = { "Month", "All" };
		transactionsDisplayOptionsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, transactionsDisplayOptions));
		if(transactionsDisplayInterval.equals("Month"))
		{
			transactionsDisplayOptionsList.setSelection(0);
		}
		else if(transactionsDisplayInterval.equals("Year"))
		{
			transactionsDisplayOptionsList.setSelection(1);
		}
		else
		{
			//transactionsDisplayOptionsList.setSelection(2);
			transactionsDisplayOptionsList.setSelection(1);
		}
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		if(preferences.contains(KEY_SPLASH_DURATION))
		{
			enableSplash = preferences.getInt(KEY_SPLASH_DURATION, splashDuration) == splashDuration;
		}
		if(preferences.contains(KEY_RESPOND_BANK_SMS))
		{
			respondBankMessages=preferences.getBoolean(KEY_RESPOND_BANK_SMS, true);
		}
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbolSelected=preferences.getString(KEY_CURRENCY_SYMBOL, "Rs ");
		}
		if(preferences.contains(KEY_TRANSACTIONS_DISPLAY_INTERVAL))
		{
			transactionsDisplayInterval=preferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
		}
	}
	
	private void savePreferences()
	{
		enableSplash=splashCheckBox.isChecked();
		respondBankMessages = bankSmsCheckBox.isChecked();
		currencySymbolSelected = (String) currencySymbolsList.getSelectedItem();
		if(currencySymbolSelected.equalsIgnoreCase("None"))
			currencySymbolSelected = " ";
		transactionsDisplayInterval = (String) transactionsDisplayOptionsList.getSelectedItem();
		
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(KEY_SPLASH_DURATION, enableSplash ? splashDuration : 0);
		editor.putBoolean(KEY_RESPOND_BANK_SMS, respondBankMessages);
		editor.putString(KEY_CURRENCY_SYMBOL, currencySymbolSelected);
		editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, transactionsDisplayInterval);
		editor.commit();
	}
	
	private void readCurrencySymbolsFile()
	{
		currencySymbols=new ArrayList<String>();
		InputStream textStream = getResources().openRawResource(R.raw.currency_symbols);
		BufferedReader symbolsReader = new BufferedReader(new InputStreamReader(textStream));
		try
		{
			String line=symbolsReader.readLine();
			while(line!=null)
			{
				currencySymbols.add(line);
				line=symbolsReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private int getCurrencySymbolPosition(String currencySymbolSelected2)
	{
		int position = 0;
		if(currencySymbolSelected.equalsIgnoreCase(" "))
		{
			position = 0;
		}
		else
		{
			for(int i=0; i<NUM_CURRENCY_SYMBOLS; i++)
			{
				if(currencySymbolSelected.equalsIgnoreCase(currencySymbols.get(i)))
					position = i;
			}
		}
		
		return position;
	}
}
