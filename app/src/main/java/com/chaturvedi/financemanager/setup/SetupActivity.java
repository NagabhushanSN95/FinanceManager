package com.chaturvedi.financemanager.setup;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.ExpenditureType;
import com.chaturvedi.financemanager.datastructures.Wallet;
import com.chaturvedi.financemanager.main.SummaryActivity;
import com.chaturvedi.financemanager.datastructures.Bank;

public class SetupActivity extends FragmentActivity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private SharedPreferences preferences;
	private static final String KEY_APP_VERSION = "AppVersionNo";
	private int CURRENT_APP_VERSION_NO;
	private static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private int splashDuration = 5000;
	private static final String KEY_QUOTE_NO = "QuoteNo";
	private static final String KEY_NUM_EXP_TYPES = "NumExpTypes";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
	private static final String KEY_AUTOMATIC_BACKUP_RESTORE = "AutomaticBackupAndRestore";
	
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_NEXT_BUTTON;
	private int HEIGHT_NEXT_BUTTON;
	
	private ViewPager setupPager;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);

		calculateDimensions();
		buildLayout();
		
	}
	
	@Override
	public void onBackPressed()
	{
		int pageNo = setupPager.getCurrentItem();
		if(pageNo == 0)								// 1st Page, so go back to previous activity
		{
			super.onBackPressed();
		}
		else										// Rewind ViewPager to Previous Page
		{
			setupPager.setCurrentItem(pageNo-1);
		}
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

		WIDTH_NEXT_BUTTON=screenWidth*60/100;
		HEIGHT_NEXT_BUTTON=screenHeight*10/100;
	}
	
	private void buildLayout()
	{
		setupPager = (ViewPager) findViewById(R.id.viewPager);
		setupPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
		
		final Button nextButton = (Button) findViewById(R.id.button_next);
		RelativeLayout.LayoutParams nextButtonParams = (LayoutParams) nextButton.getLayoutParams();
		nextButtonParams.width = WIDTH_NEXT_BUTTON;
		nextButtonParams.height = HEIGHT_NEXT_BUTTON;
		//nextButton.setLayoutParams(nextButtonParams);
		nextButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Slide to NextPage
				int pageNo = setupPager.getCurrentItem();	// Gives the Page No
				if(pageNo == 2)								// Current Page is ExpTypesSetup (Last Page). 
				{
					finishSetup();
				}
				else
				{
					setupPager.setCurrentItem(setupPager.getCurrentItem() + 1);
				}
			}
		});
		
		setupPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
		{
			@Override
			public void onPageSelected(int arg0)
			{
				int pageNo = setupPager.getCurrentItem();	// Gives the Page No
				if(pageNo == 2)								// Current Page is ExpTypesSetup (Last Page). 
				{
					nextButton.setText("Finish");			// So, Change NextButton to FinishButton
				}
				else
				{
					nextButton.setText("Next");
				}
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0)
			{
				
			}
		});
	}
	
	private void finishSetup()
	{
		ArrayList<Wallet> wallets = WalletsSetupFragment.getAllWallets();
		ArrayList<Bank> banks = BanksSetupFragment.getAllBanks();
		ArrayList<ExpenditureType> expTypes = ExpTypesSetupFragment.getAllExpTypes();

		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(SetupActivity.this);
		databaseAdapter.addAllWallets(wallets);
		databaseAdapter.addAllBanks(banks);
		databaseAdapter.addAllExpenditureTypes(expTypes);
		databaseAdapter.readjustCountersTable();
		
		// Store Default Preferences
		CURRENT_APP_VERSION_NO = Integer.parseInt(getResources().getString(R.string.currentAppVersion));
		preferences = this.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
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
		editor.putInt(KEY_NUM_EXP_TYPES, expTypes.size());
		editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
		editor.putString(KEY_CURRENCY_SYMBOL, " ");
		editor.putString(KEY_RESPOND_BANK_SMS, "Popup");
		editor.putBoolean(KEY_BANK_SMS_ARRIVED, false);
		editor.putInt(KEY_AUTOMATIC_BACKUP_RESTORE, 4);
		editor.commit();
		
		Intent summaryIntent = new Intent(this, SummaryActivity.class);
		startActivity(summaryIntent);
		finish();
	}
	
	private class MyPagerAdapter extends FragmentPagerAdapter
	{
		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}
		
		@Override
		public Fragment getItem(int pos)
		{
			switch(pos)
			{
				case 0: return WalletsSetupFragment.newInstance();
				case 1: return BanksSetupFragment.newInstance();
				case 2: return ExpTypesSetupFragment.newInstance("Rama");
				default: return WalletsSetupFragment.newInstance();
			}
		}
		
		@Override
		public int getCount()
		{
			return 3;
		}
	}
}
