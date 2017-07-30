package com.chaturvedi.financemanager;

import java.util.ArrayList;

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
	private final int NUM_EXP_TYPES=5;
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_ENABLE_SPLASH = "enable_splash";
	private static final String KEY_BANK_SMS = "respond_bank_messages";
	private static final String SHARED_PREFERENCES_VERSION = "app_version";
	private static final String KEY_VERSION = "version";
	private static int VERSION_NO;
	private static final String SHARED_PREFERENCES_DATABASE = "DatabaseInitialized";
	private static final String KEY_DATABASE_INITIALIZED = "database_initialized";
	
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
		SharedPreferences databasePreferences = getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0);
		if(databasePreferences.contains(KEY_DATABASE_INITIALIZED))
		{
			if(databasePreferences.getBoolean(KEY_DATABASE_INITIALIZED, false))
			{
				// Finish This Activity
				super.onBackPressed();
			}
		}
	}
	
	private void restoreData()
	{
		RestoreManager restore = new RestoreManager(StartupActivity.this);
		int result = restore.restore();
		if(result==1)
		{
			startActivity(summaryIntent);
			super.onBackPressed();
		}
		/*else if(result==0)
		{
			
		}*/
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
		
		SharedPreferences settingsPreferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		SharedPreferences.Editor settingsEditor = settingsPreferences.edit();
		settingsEditor.putBoolean(KEY_ENABLE_SPLASH, true);
		settingsEditor.putBoolean(KEY_BANK_SMS, true);
		settingsEditor.commit();
		

		SharedPreferences versionPreferences = getSharedPreferences(SHARED_PREFERENCES_VERSION, 0);
		SharedPreferences.Editor versionEditor = versionPreferences.edit();
		try
		{
			VERSION_NO = this.getPackageManager().getPackageInfo(this.getLocalClassName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			Toast.makeText(getApplicationContext(), "Error In Retrieving Version No In " + 
					"ExpenditureSetupActivity\\saveToDatabase\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		versionEditor.putInt(KEY_VERSION, VERSION_NO);
		versionEditor.commit();
		
		SharedPreferences databasePreferences = getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0);
		SharedPreferences.Editor databaseEditor = databasePreferences.edit();
		databaseEditor.putBoolean(KEY_DATABASE_INITIALIZED, true);
		databaseEditor.commit();
		
		startActivityForResult(summaryIntent, 0);
		super.onBackPressed();
	}
}
