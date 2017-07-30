package com.chaturvedi.financemanager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	
	private final int SPLASH_DURATION_0     = 0;
	private final int SPLASH_DURATION_5000  = 1;
	private final int SPLASH_DURATION_10000 = 2;
	private final int SPLASH_DURATION_15000 = 3;
	private final int BANK_SMS_NO_RESPONSE  = 0;
	private final int BANK_SMS_POPUP        = 1;
	private final int BANK_SMS_AUTOMATIC    = 2;
	private final int TRANSACTIONS_MONTHLY  = 0;
	private final int TRANSACTIONS_YEARLY   = 1;
	private final int TRANSACTIONS_ALL      = 2;
	
	private Spinner splashDurationSpinner;
	private int splashDuration = SPLASH_DURATION_5000;
	//private CheckBox splashCheckBox;
	//private static boolean enableSplash=true;
	private Spinner bankSmsSpinner;
	private int bankSmsResponse = BANK_SMS_POPUP;
	//private CheckBox bankSmsCheckBox;
	//private static boolean respondBankMessages = true;
	private Spinner currencySymbolsSpinner;
	private ArrayList<String> currencySymbols;
	private String currencySymbolSelected = "Rs ";
	private final int NUM_CURRENCY_SYMBOLS=3;
	private Spinner transactionsDisplayIntervalSpinner;
	private int transactionsDisplayInterval = TRANSACTIONS_MONTHLY;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
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
		
		splashDurationSpinner=(Spinner)findViewById(R.id.spinner_splashDuration);
		Integer[] splashDurationsList = {0,5000,10000,15000};
		splashDurationSpinner.setAdapter(new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item,
				splashDurationsList));
		splashDurationSpinner.setSelection(splashDuration);
		
		bankSmsSpinner = (Spinner)findViewById(R.id.spinner_bankSms);
		String[] bankSmsOptionsList = {"Don't Respond", "Show Pop-up", "Automatic Transaction"};
		bankSmsSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				bankSmsOptionsList));
		bankSmsSpinner.setSelection(bankSmsResponse);
		
		currencySymbolsSpinner = (Spinner)findViewById(R.id.spinner_currencySymbols);
		currencySymbolsSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currencySymbols));
		currencySymbolsSpinner.setSelection(getCurrencySymbolPosition(currencySymbolSelected));
		
		transactionsDisplayIntervalSpinner = (Spinner)findViewById(R.id.spinner_transactionsDisplayInterval);
		String[] transactionsDisplayOptions = { "Month", "Year", "All" };
		//String[] transactionsDisplayOptions = { "Month", "All" };
		transactionsDisplayIntervalSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, transactionsDisplayOptions));
		transactionsDisplayIntervalSpinner.setSelection(transactionsDisplayInterval);
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		int splashDurationMillis;
		if(preferences.contains(KEY_SPLASH_DURATION))
		{
			splashDurationMillis = preferences.getInt(KEY_SPLASH_DURATION, 5000);
			switch(splashDurationMillis)
			{
				case 0:
					splashDuration = SPLASH_DURATION_0;
					break;
				
				case 5000:
					splashDuration = SPLASH_DURATION_5000;
					break;
				
				case 10000:
					splashDuration = SPLASH_DURATION_10000;
					break;
				
				case 15000:
					splashDuration = SPLASH_DURATION_15000;
					break;
				
				default:
					splashDuration = SPLASH_DURATION_5000;
					break;
			}
		}
		if(preferences.contains(KEY_RESPOND_BANK_SMS))
		{
			String bankSmsResponse1 = preferences.getString(KEY_RESPOND_BANK_SMS, "Popup");
			if(bankSmsResponse1.equals("Popup"))
				bankSmsResponse = BANK_SMS_POPUP;
			else if(bankSmsResponse1.equals("NoResponse"))
				bankSmsResponse = BANK_SMS_NO_RESPONSE;
			else if(bankSmsResponse1.equals("Automatic"))
				bankSmsResponse = BANK_SMS_AUTOMATIC;
			else
				bankSmsResponse = BANK_SMS_POPUP;
		}
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbolSelected=preferences.getString(KEY_CURRENCY_SYMBOL, "Rs ");
		}
		if(preferences.contains(KEY_TRANSACTIONS_DISPLAY_INTERVAL))
		{
			String interval=preferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
			if(interval.equals("Month"))
				transactionsDisplayInterval = TRANSACTIONS_MONTHLY;
			else if(interval.equals("Year"))
				transactionsDisplayInterval = TRANSACTIONS_YEARLY;
			else if(interval.equals("All"))
				transactionsDisplayInterval = TRANSACTIONS_ALL;
			else
				transactionsDisplayInterval = TRANSACTIONS_MONTHLY;
		}
	}
	
	private void savePreferences()
	{
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, 0);
		SharedPreferences.Editor editor = preferences.edit();
		
		splashDuration = splashDurationSpinner.getSelectedItemPosition();
		switch(splashDuration)
		{
			case 0:
				editor.putInt(KEY_SPLASH_DURATION, 0);
				break;
				
			case 1:
				editor.putInt(KEY_SPLASH_DURATION, 5000);
				break;
				
			case 2:
				editor.putInt(KEY_SPLASH_DURATION, 10000);
				break;
				
			case 3:
				editor.putInt(KEY_SPLASH_DURATION, 15000);
				break;
				
			default:
				editor.putInt(KEY_SPLASH_DURATION, 5000);
				break;
		}
		
		bankSmsResponse = bankSmsSpinner.getSelectedItemPosition();
		switch(bankSmsResponse)
		{
			case 0:
				editor.putString(KEY_RESPOND_BANK_SMS, "NoResponse");
				break;
				
			case 1:
				editor.putString(KEY_RESPOND_BANK_SMS, "Popup");
				break;
				
			case 2:
				editor.putString(KEY_RESPOND_BANK_SMS, "Automatic");
				break;
				
			default:
				editor.putString(KEY_RESPOND_BANK_SMS, "Popup");
				break;
		}
		currencySymbolSelected = (String) currencySymbolsSpinner.getSelectedItem();
		if(currencySymbolSelected.equalsIgnoreCase("None"))
			currencySymbolSelected = " ";
		editor.putString(KEY_CURRENCY_SYMBOL, currencySymbolSelected);
		
		transactionsDisplayInterval = transactionsDisplayIntervalSpinner.getSelectedItemPosition();
		switch(transactionsDisplayInterval)
		{
			case 0:
				editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
				break;
				
			case 1:
				editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Year");
				break;
				
			case 2:
				editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "All");
				break;
				
			default:
				editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
				break;
		}
		
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
