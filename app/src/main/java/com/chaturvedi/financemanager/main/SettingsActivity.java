package com.chaturvedi.financemanager.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NavUtils;

import com.chaturvedi.customviews.SettingsLayout;
import com.chaturvedi.customviews.SettingsLayout.OnSettingChangedListener;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.functions.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SettingsActivity extends Activity
{
	
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

	private SettingsLayout splashDurationSetting;
	private int splashDuration = SPLASH_DURATION_5000;
	private SettingsLayout bankSmsSetting;
	private int bankSmsResponse = BANK_SMS_POPUP;
	private SettingsLayout currencySymbolSetting;
	private ArrayList<String> currencySymbols;
	private String currencySymbolSelected = "Rs ";
	private final int NUM_CURRENCY_SYMBOLS=3;
	private SettingsLayout transactionsDisplayIntervalSetting;
	private int transactionsDisplayInterval = TRANSACTIONS_MONTHLY;
	private SettingsLayout autoBackupRestoreSetting;
	private int autoBackupRestoreValue;
	private SettingsLayout dailyBackupSetting;
	private boolean dailyBackupSelected;
    private SettingsLayout sortTransactionsSetting;
    private String sortTransactionsSelected = Constants.VALUE_SORT_TRANSACTIONS_CREATED;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		
		readPreferences();
		readCurrencySymbolsFile();
		buildLayout();
		
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(SettingsActivity.this);
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
		
		splashDurationSetting = (SettingsLayout) findViewById(R.id.splashDuration);
		splashDurationSetting.setSettingName("Splash Duration");
		String[] splashDurationOptions = {"Minimum", "5 seconds", "10 seconds", "15 seconds"};
		splashDurationSetting.setOptions(splashDurationOptions);
		splashDurationSetting.setSelection(splashDuration);
		splashDurationSetting.setSettingChangedListener(new OnSettingChangedListener()
		{
			@Override
			public void onSettingChanged()
			{
				SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				splashDuration = splashDurationSetting.getSelectedOptionNo()*5000;
				editor.putInt(Constants.KEY_SPLASH_DURATION, splashDuration);
				editor.commit();
			}
		});
		
		/*bankSmsSetting = (SettingsLayout) findViewById(R.id.bankSms);
		bankSmsSetting.setSettingName("Action For Bank Messages");
		String[] bankSmsOptions = {"Don't Respond", "Show Pop-up", "Automatic Transaction"};
		bankSmsSetting.setOptions(bankSmsOptions);
		bankSmsSetting.setSelection(bankSmsResponse);
		bankSmsSetting.setSettingChangedListener(new OnSettingChangedListener()
		{
			@Override
			public void onSettingChanged()
			{
				SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				
				bankSmsResponse = bankSmsSetting.getSelectedOptionNo();
				switch(bankSmsResponse)
				{
					case 0:
						editor.putString(Constants.KEY_RESPOND_BANK_SMS, "NoResponse");
						break;
						
					case 1:
						editor.putString(Constants.KEY_RESPOND_BANK_SMS, "Popup");
						break;
						
					case 2:
						editor.putString(Constants.KEY_RESPOND_BANK_SMS, "Automatic");
						break;
						
					default:
						editor.putString(Constants.KEY_RESPOND_BANK_SMS, "Popup");
						break;
				}
				editor.commit();
			}
		});*/
		
		currencySymbolSetting = (SettingsLayout) findViewById(R.id.currencySymbols);
		currencySymbolSetting.setSettingName("Currency Symbol");
		currencySymbolSetting.setOptions(currencySymbols);
		currencySymbolSetting.setSelection(getCurrencySymbolPosition(currencySymbolSelected));
		currencySymbolSetting.setSettingChangedListener(new OnSettingChangedListener()
		{
			@Override
			public void onSettingChanged()
			{
				SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				currencySymbolSelected = (String) currencySymbolSetting.getSelectedOption();
				if(currencySymbolSelected.equalsIgnoreCase("None"))
					currencySymbolSelected = " ";
				editor.putString(Constants.KEY_CURRENCY_SYMBOL, currencySymbolSelected);
				editor.commit();
			}
		});
		
		transactionsDisplayIntervalSetting = (SettingsLayout) findViewById(R.id.transactionsDisplayInterval);
		transactionsDisplayIntervalSetting.setSettingName("Transactions Display Interval");
		String[] transactionsDisplayIntervalOptions = {"Month", "Year", "All"};
		transactionsDisplayIntervalSetting.setOptions(transactionsDisplayIntervalOptions);
		transactionsDisplayIntervalSetting.setSelection(transactionsDisplayInterval);
		transactionsDisplayIntervalSetting.setSettingChangedListener(new OnSettingChangedListener()
		{
			@Override
			public void onSettingChanged()
			{
				SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				transactionsDisplayInterval = transactionsDisplayIntervalSetting.getSelectedOptionNo();
				switch(transactionsDisplayInterval)
				{
					case 0:
						editor.putString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
						break;
						
					case 1:
						editor.putString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Year");
						break;
						
					case 2:
						editor.putString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "All");
						break;
						
					default:
						editor.putString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "All");
						break;
				}
				editor.commit();
			}
		});
		
		autoBackupRestoreSetting = (SettingsLayout) findViewById(R.id.autoBackupRestore);
		autoBackupRestoreSetting.setSettingName("Auto Backup & Restore");
		String[] autoBackupRestoreOptions = { "No Auto Backup", "Auto Backup Only", "Auto Backup And Restore" };
		autoBackupRestoreSetting.setOptions(autoBackupRestoreOptions);
		autoBackupRestoreSetting.setSelection(autoBackupRestoreValue);
		autoBackupRestoreSetting.setSettingChangedListener(new OnSettingChangedListener()
		{
			@Override
			public void onSettingChanged()
			{
				SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				
				autoBackupRestoreValue = autoBackupRestoreSetting.getSelectedOptionNo();
				switch(autoBackupRestoreValue)
				{
					case 0:
						autoBackupRestoreValue = 0;
						break;
						
					case 1:
						autoBackupRestoreValue = 1;
						break;
						
					case 2:
						autoBackupRestoreValue = 4;
						break;
				}
				editor.putInt(Constants.KEY_AUTOMATIC_BACKUP_RESTORE, autoBackupRestoreValue);
				editor.commit();
			}
		});

		dailyBackupSetting = (SettingsLayout) findViewById(R.id.dailyBackup);
		dailyBackupSetting.setSettingName("Daily Backup");
		String[] dailyBackupSettingOptions = new String[]{Constants.VALUE_DAILY_BACKUP_ENABLED,
				Constants.VALUE_DAILY_BACKUP_DISABLED};
		dailyBackupSetting.setOptions(dailyBackupSettingOptions);
		if (dailyBackupSelected)
		{
			dailyBackupSetting.setSelection(0);
		}
		else
		{
			dailyBackupSetting.setSelection(1);
		}
		dailyBackupSetting.setSettingChangedListener(new OnSettingChangedListener()
		{
			@Override
			public void onSettingChanged()
			{
				SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				String dailyBackupSelectedString = dailyBackupSetting.getSelectedOption();
				dailyBackupSelected = dailyBackupSelectedString.equals(Constants.VALUE_DAILY_BACKUP_ENABLED);
				editor.putBoolean(Constants.KEY_DAILY_BACKUP, dailyBackupSelected);
				editor.commit();
				Utilities.setDailyBackupService(SettingsActivity.this, dailyBackupSelected);
			}
		});

        sortTransactionsSetting = findViewById(R.id.sortTransactions);
        sortTransactionsSetting.setSettingName("Sort Transactions By");
        String[] sortTransactionsSettingOptions = new String[]{
                Constants.VALUE_SORT_TRANSACTIONS_DATE,
                Constants.VALUE_SORT_TRANSACTIONS_CREATED,
                Constants.VALUE_SORT_TRANSACTIONS_MODIFIED
        };
        sortTransactionsSetting.setOptions(sortTransactionsSettingOptions);
        sortTransactionsSetting.setSelection(sortTransactionsSelected);
        sortTransactionsSetting.setSettingChangedListener(new OnSettingChangedListener() {
            @Override
            public void onSettingChanged() {
                SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                String sortTransactionsSelectedString = sortTransactionsSetting.getSelectedOption();
                editor.putString(Constants.KEY_SORT_TRANSACTIONS, sortTransactionsSelectedString);
                editor.apply();
            }
        });
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
		int splashDurationMillis;
		if (preferences.contains(Constants.KEY_SPLASH_DURATION))
		{
			splashDurationMillis = preferences.getInt(Constants.KEY_SPLASH_DURATION, 5000);
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
		if (preferences.contains(Constants.KEY_RESPOND_BANK_SMS))
		{
			String bankSmsResponse1 = preferences.getString(Constants.KEY_RESPOND_BANK_SMS, "Popup");
			if(bankSmsResponse1.equals("Popup"))
				bankSmsResponse = BANK_SMS_POPUP;
			else if(bankSmsResponse1.equals("NoResponse"))
				bankSmsResponse = BANK_SMS_NO_RESPONSE;
			else if(bankSmsResponse1.equals("Automatic"))
				bankSmsResponse = BANK_SMS_AUTOMATIC;
			else
				bankSmsResponse = BANK_SMS_POPUP;
		}
		if (preferences.contains(Constants.KEY_CURRENCY_SYMBOL))
		{
			currencySymbolSelected = preferences.getString(Constants.KEY_CURRENCY_SYMBOL, "Rs ");
		}
		if (preferences.contains(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL))
		{
			String interval = preferences.getString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
			if(interval.equals("Month"))
				transactionsDisplayInterval = TRANSACTIONS_MONTHLY;
			else if(interval.equals("Year"))
				transactionsDisplayInterval = TRANSACTIONS_YEARLY;
			else if(interval.equals("All"))
				transactionsDisplayInterval = TRANSACTIONS_ALL;
			else
				transactionsDisplayInterval = TRANSACTIONS_MONTHLY;
		}
		
		// Retrieve Automatic Backup And Restore Status
		if (preferences.contains(Constants.KEY_AUTOMATIC_BACKUP_RESTORE)) {
			autoBackupRestoreValue = preferences.getInt(Constants.KEY_AUTOMATIC_BACKUP_RESTORE, 1);
		}
		switch(autoBackupRestoreValue)
		{
		case 0:
			autoBackupRestoreValue = 0;
			break;
			
		case 1:
			autoBackupRestoreValue = 1;
			break;
			
		case 2:
		case 3:
		case 4:
			autoBackupRestoreValue = 2;
			break;
		}
		
		if (preferences.contains(Constants.KEY_DAILY_BACKUP))
		{
			dailyBackupSelected = preferences.getBoolean(Constants.KEY_DAILY_BACKUP, false);
		}
        if (preferences.contains(Constants.KEY_SORT_TRANSACTIONS)) {
            sortTransactionsSelected = preferences.getString(Constants.KEY_SORT_TRANSACTIONS, Constants.VALUE_SORT_TRANSACTIONS_DATE);
        }
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
	
	private int getCurrencySymbolPosition(String currencySymbolSelected)
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
