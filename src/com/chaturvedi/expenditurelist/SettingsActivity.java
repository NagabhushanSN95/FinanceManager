package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsActivity extends Activity
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_ENABLE_SPLASH = "enable_splash";
	private static final String KEY_BANK_SMS = "respond_bank_messages";
	private static final String KEY_CURRENCY_SYMBOL = "currency_symbols";
	private static final String KEY_TRANSACTIONS_DISPLAY_OPTIONS = "transactions_display_options";
	
	private CheckBox splashCheckBox;
	private static boolean enableSplash=true;
	private CheckBox bankSmsCheckBox;
	private static boolean respondBankMessages = true;
	private Spinner currencySymbolsList;
	private ArrayList<String> currencySymbols;
	private String currencySymbolSelected = "Rs ";
	private static final int NUM_CURRENCY_SYMBOLS=3;
	Spinner transactionsDisplayOptionsList;
	private static String transactionsDisplayOption = "Month";
	
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
		if(transactionsDisplayOption.equals("Month"))
		{
			transactionsDisplayOptionsList.setSelection(0);
		}
		else if(transactionsDisplayOption.equals("Year"))
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
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_ENABLE_SPLASH))
		{
			enableSplash=preferences.getBoolean(KEY_ENABLE_SPLASH, true);
		}
		if(preferences.contains(KEY_BANK_SMS))
		{
			respondBankMessages=preferences.getBoolean(KEY_BANK_SMS, true);
		}
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbolSelected=preferences.getString(KEY_CURRENCY_SYMBOL, "Rs ");
		}
		if(preferences.contains(KEY_TRANSACTIONS_DISPLAY_OPTIONS))
		{
			transactionsDisplayOption=preferences.getString(KEY_TRANSACTIONS_DISPLAY_OPTIONS, "Month");
		}
	}
	
	private void savePreferences()
	{
		enableSplash=splashCheckBox.isChecked();
		respondBankMessages = bankSmsCheckBox.isChecked();
		currencySymbolSelected = (String) currencySymbolsList.getSelectedItem();
		if(currencySymbolSelected.equalsIgnoreCase("None"))
			currencySymbolSelected = " ";
		transactionsDisplayOption = (String) transactionsDisplayOptionsList.getSelectedItem();
		
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(KEY_ENABLE_SPLASH, enableSplash);
		editor.putBoolean(KEY_BANK_SMS, respondBankMessages);
		editor.putString(KEY_CURRENCY_SYMBOL, currencySymbolSelected);
		editor.putString(KEY_TRANSACTIONS_DISPLAY_OPTIONS, transactionsDisplayOption);
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
