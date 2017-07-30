package com.chaturvedi.financemanager;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.RestoreManager;

public class StartupActivity extends FragmentActivity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private SharedPreferences preferences;
	private static final String KEY_APP_VERSION = "AppVersionNo";
	private final int CURRENT_APP_VERSION_NO = 76;
	private static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private int splashDuration = 5000;
	private static final String KEY_QUOTE_NO = "QuoteNo";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";

	private final int NUM_EXP_TYPES=5;
	
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_BUTTON;
	private int HEIGHT_BUTTON;
	
	private Intent setupIntent;
	private Intent summaryIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_startup);
		
		preferences = this.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		calculateDimensions();
		buildLayout();
		
	}
	
	/**
	 * Calculate the values of various Dimension Fields
	 */
	private void calculateDimensions()
	{
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		
		WIDTH_BUTTON = screenWidth*60/100;
		HEIGHT_BUTTON = screenHeight*10/100;
		
		setupIntent = new Intent(StartupActivity.this, BanksSetupActivity.class);
		summaryIntent = new Intent(StartupActivity.this, SummaryActivity.class);
		
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if((this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		TextView financeManager = (TextView) findViewById(R.id.textView_FinanceManager);
		RelativeLayout.LayoutParams financeManagerTextParams = (LayoutParams) financeManager.getLayoutParams();
		financeManagerTextParams.topMargin = screenHeight * 10/100;
		
		TextView welcomeText = (TextView) findViewById(R.id.textView_welcome);
		RelativeLayout.LayoutParams welcomeTextParams = (LayoutParams) welcomeText.getLayoutParams();
		welcomeTextParams.topMargin = screenHeight * 3/100;
		
		Button setupButton = (Button) findViewById(R.id.button_setup);
		RelativeLayout.LayoutParams setupButtonParams = (LayoutParams) setupButton.getLayoutParams();
		setupButtonParams.width = WIDTH_BUTTON;
		setupButtonParams.height = HEIGHT_BUTTON;
		setupButtonParams.topMargin = screenHeight*10/100;
		//setupButton.setLayoutParams(setupButtonParams);
		setupButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivityForResult(setupIntent, 0);
			}
		});
		
		Button restoreButton = (Button) findViewById(R.id.button_restore);
		RelativeLayout.LayoutParams restoreButtonParams = (LayoutParams) restoreButton.getLayoutParams();
		restoreButtonParams.width = WIDTH_BUTTON;
		restoreButtonParams.height = HEIGHT_BUTTON;
		restoreButtonParams.topMargin = screenHeight*3/100;
		restoreButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				restoreData();
			}
		});
		
		Button skipButton = (Button) findViewById(R.id.button_skip);
		RelativeLayout.LayoutParams skipButtonParams = (LayoutParams) skipButton.getLayoutParams();
		skipButtonParams.width = WIDTH_BUTTON;
		skipButtonParams.height = HEIGHT_BUTTON;
		skipButtonParams.topMargin = screenHeight*3/100;
		skipButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				skipSetup();
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(preferences.contains(KEY_DATABASE_INITIALIZED))
		{
			if(preferences.getBoolean(KEY_DATABASE_INITIALIZED, false))
			{
				// Finish This Activity
				//super.onBackPressed();
				finish();
			}
		}
	}
	
	private void restoreData()
	{
		RestoreManager restoreManager = new RestoreManager(StartupActivity.this);
		int result = restoreManager.readBackups("Finance Manager/Backups");
		if(result == 0)
		{
			DatabaseManager.setWalletBalance(restoreManager.getWalletBalance());
			DatabaseManager.setAllTransactions(restoreManager.getAllTransactions());
			DatabaseManager.setAllBanks(restoreManager.getAllBanks());
			DatabaseManager.setAllCounters(restoreManager.getAllCounters());
			DatabaseManager.setAllExpenditureTypes(restoreManager.getAllExpTypes());
			DatabaseManager.setAllTemplates(restoreManager.getAllTemplates());
			
			// Store Default Preferences
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
						"StartupActivity/restoreData\n" + e.getMessage(), Toast.LENGTH_LONG).show();
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
			
			startActivity(summaryIntent);
			super.onBackPressed();
		}
		else if(result == 1)
		{
			Toast.makeText(this, "No Backups Were Found.\nMake sure the Backup Files are located in\n" + 
					"Chaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();
		}
		else if(result == 2)
		{
			Toast.makeText(this, "Old Data. Cannot be Restored. Sorry!", Toast.LENGTH_LONG).show();
		}
		else if(result == 3)
		{
			Toast.makeText(this, "Error in Restoring Data\nControl Entered Catch Block", Toast.LENGTH_LONG).show();
		}
	}
	
	private void skipSetup()
	{
		double walletBalance = 0;
		int numBanks = 0;
		ArrayList<Bank> banks = new ArrayList<Bank>();
		DatabaseManager.initialize(walletBalance);
		DatabaseManager.setNumBanks(numBanks);
		DatabaseManager.setAllBanks(banks);
		
		ArrayList<String> expTypes = new ArrayList<String>(NUM_EXP_TYPES);
		expTypes.add(getResources().getString(R.string.hint_exp01));
		expTypes.add(getResources().getString(R.string.hint_exp02));
		expTypes.add(getResources().getString(R.string.hint_exp03));
		expTypes.add(getResources().getString(R.string.hint_exp04));
		expTypes.add(getResources().getString(R.string.hint_exp05));
		DatabaseManager.setAllExpenditureTypes(expTypes);
		
		// Store Default Preferences
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
					"StartupActivity/skipSetup\n" + e.getMessage(), Toast.LENGTH_LONG).show();
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
		
		startActivityForResult(summaryIntent, 0);
		super.onBackPressed();
	}
}
