package com.chaturvedi.financemanager.updates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.widget.Toast;

public class Update68To75
{
	private Context context;
	private final int CURRENT_APP_VERSION_NO = 75;
	
	public Update68To75(Context cxt)
	{
		context = cxt;
		
		Toast.makeText(context, "Updating The App", Toast.LENGTH_LONG).show();
		updateSharedPreferences();
		updateBackups();
	}
	
	private void updateSharedPreferences()
	{
		// Older Preferences
		final String SHARED_PREFERENCES_SETTINGS = "Settings";
		final String KEY_ENABLE_SPLASH = "enable_splash";
		boolean showSplash = true;
		final String SHARED_PREFERENCES_DATABASE = "Database";
		final String SHARED_PREFERENCES_DATABASE_INITIALIZED = "DatabaseInitialized";
		final String KEY_DATABASE_INITIALIZED_OLD = "database_initialized";
		final String SHARED_PREFERENCES_APP = "Other App Preferences";
		final String KEY_QUOTE_NUMREADS = "quote_numReads";
		int quotesNumReads = 0;
		final String SHARED_PREFERENCES_VERSION = "app_version";
		final String KEY_VERSION = "version";
		int versionNo;
		final String KEY_CURRENCY_SYMBOL_OLD = "currency_symbols";
		String currencySymbol = " ";
		final String KEY_TRANSACTIONS_DISPLAY_INTERVAL_OLD = "transactions_display_interval";
		String transactionsDisplayInterval = "Month";
		final String SHARED_PREFERENCES_SMS = "Bank_SMS";
		final String KEY_BANK_SMS_ARRIVED_OLD = "sms_arrived";
		boolean newBankSmsArrived = false;
		final String KEY_BANK_SMS_OLD = "respond_bank_messages";
		boolean respondToBankSms = true;
		
		
		// New Preferences
		final String ALL_PREFERENCES = "AllPreferences";
		final String KEY_APP_VERSION = "AppVersionNo";
		final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
		boolean databaseInitialized = true;
		final String KEY_SPLASH_DURATION = "SplashDuration";
		int splashDuration = 5000;
		final String KEY_QUOTE_NO = "QuoteNo";
		final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
		final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
		final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
		final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";

		SharedPreferences oldPreferences;
		SharedPreferences newPreferences = context.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = newPreferences.edit();
		
		//------------------------------------------------------------------------------------------
		// Update Preferences Stored in "Settings" SharedPreferences
		oldPreferences = context.getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		// Change Whether or not to display Splash Screen to Splash Screen Duration
		if(oldPreferences.contains(KEY_ENABLE_SPLASH))
		{
			showSplash=newPreferences.getBoolean(KEY_ENABLE_SPLASH, true);
		}
		else
		{
			showSplash = true;
		}
		splashDuration = showSplash ? 5000 : 0;
		editor.putInt(KEY_SPLASH_DURATION, splashDuration);
		
		if(oldPreferences.contains(KEY_TRANSACTIONS_DISPLAY_INTERVAL_OLD))
		{
			transactionsDisplayInterval=oldPreferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL_OLD, "Month");
		}
		else
		{
			transactionsDisplayInterval = "Month";
		}
		editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, transactionsDisplayInterval);
		
		if(oldPreferences.contains(KEY_CURRENCY_SYMBOL_OLD))
		{
			currencySymbol = oldPreferences.getString(KEY_CURRENCY_SYMBOL_OLD, " ");
		}
		else
		{
			currencySymbol = " ";
		}
		editor.putString(KEY_CURRENCY_SYMBOL, currencySymbol);
		
		if(oldPreferences.contains(KEY_BANK_SMS_OLD))
		{
			respondToBankSms=oldPreferences.getBoolean(KEY_BANK_SMS_OLD, true);
		}
		else
		{
			respondToBankSms = true;
		}
		editor.putBoolean(KEY_RESPOND_BANK_SMS, respondToBankSms);
		
		oldPreferences.edit().clear();							// Delete All Old Preferences
		//-----------------------------------------------------------------------------------------------
		
		// Update Preferences Stored in "Database" SharedPreferences
		oldPreferences = context.getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0);
		if(oldPreferences.contains(KEY_DATABASE_INITIALIZED_OLD))
		{
			databaseInitialized = true;
		}
		else
		{
			// Here oldPreferences will be changed. So, clear all oldPreferences
			oldPreferences.edit().clear();							// Delete All Old Preferences
			// In the update 3.1.0, the name of the Shared Preferences Changed 
			// From DatabaseInitialized to Database.
			// So, check if that preferences file is there 
			oldPreferences = context.getSharedPreferences(SHARED_PREFERENCES_DATABASE_INITIALIZED, 0);
			if(oldPreferences.contains(KEY_DATABASE_INITIALIZED_OLD))
			{
				databaseInitialized = true;
			}
			else
			{
				databaseInitialized = false;
			}
		}
		editor.putBoolean(KEY_DATABASE_INITIALIZED, databaseInitialized);
		oldPreferences.edit().clear();							// Delete All Old Preferences
		//-------------------------------------------------------------------------------------------------
		
		// Update Preferences Stored in "Other App Preferences" SharedPreferences
		oldPreferences = context.getSharedPreferences(SHARED_PREFERENCES_APP, 0);
		if(oldPreferences.contains(KEY_QUOTE_NUMREADS))
		{
			quotesNumReads=oldPreferences.getInt(KEY_QUOTE_NUMREADS, 0);
		}
		else
		{
			quotesNumReads = 0;
		}
		editor.putInt(KEY_QUOTE_NO, quotesNumReads);
		oldPreferences.edit().clear();							// Delete All Old Preferences
		//----------------------------------------------------------------------------------------------------
		
		// Update Preferences Stored in "app_version" SharedPreferences
		oldPreferences = context.getSharedPreferences(SHARED_PREFERENCES_VERSION, 0);
		if(oldPreferences.contains(KEY_VERSION))
		{
			versionNo = oldPreferences.getInt(KEY_VERSION, CURRENT_APP_VERSION_NO);
		}
		else
		{
			versionNo = CURRENT_APP_VERSION_NO;
		}
		try
		{
			versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), CURRENT_APP_VERSION_NO)
					.versionCode;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		editor.putInt(KEY_APP_VERSION, versionNo);
		oldPreferences.edit().clear();							// Delete All Old Preferences
		//----------------------------------------------------------------------------------------------------
		
		// Update Preferences Stored in "Bank_Sms" SharedPreferences
		oldPreferences = context.getSharedPreferences(SHARED_PREFERENCES_SMS, 0);
		if(oldPreferences.contains(KEY_BANK_SMS_ARRIVED_OLD))
		{
			newBankSmsArrived = oldPreferences.getBoolean(KEY_BANK_SMS_ARRIVED_OLD, false);
		}
		else
		{
			newBankSmsArrived = false;
		}
		editor.putBoolean(KEY_BANK_SMS_ARRIVED, newBankSmsArrived);
		oldPreferences.edit().clear();							// Delete All Old Preferences
		//----------------------------------------------------------------------------------------------------
		
		editor.commit();
	}

	private void updateBackups()
	{
		int backupVersionNo = 0;
		int numTransactions = 0;
		int numBanks 		= 0;
		int numCountersRows = 0;
		
		String backupFolderName = "Finance Manager/BackupTrial";
		String extension = ".snb";
		File backupFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!backupFolder.exists())
			return;
		
		String keyDataFileName = "Key Data";
		File keyDataFile = new File(backupFolder, keyDataFileName+extension);
		if(!keyDataFile.exists())
			return;
		
		// Append NumExpTypes and NumTemplates to Key Data File
		try
		{
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			ArrayList<String> lines = new ArrayList<String>();
			for(int i=0; i<4; i++)
			{
				lines.add(keyDataReader.readLine().trim());
			}
			backupVersionNo = Integer.parseInt(lines.get(0));
			numTransactions = Integer.parseInt(lines.get(1));
			numBanks 		= Integer.parseInt(lines.get(2));
			numCountersRows = Integer.parseInt(lines.get(3));
			keyDataReader.close();
			
			lines.add(5 + "");	// NumExpTypes
			lines.add(0 + "");	// NumTemplates
			keyDataFile = new File(backupFolder, keyDataFileName+extension);
			BufferedWriter keyDataWriter = new BufferedWriter(new FileWriter(keyDataFile));
			for(int i=0; i<lines.size(); i++)
			{
				keyDataWriter.write(lines.get(i).trim() + "\n");
			}
			keyDataWriter.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Error Found in Update68\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Error Found in Update68\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		
		// In Transactions File, add ID before each transaction
		String transactionsFileName = "Key Data";
		File transactionsFile = new File(backupFolder, transactionsFileName+extension);
		try
		{
			BufferedReader transactionsReader = new BufferedReader(new FileReader(transactionsFile));
			ArrayList<String> lines = new ArrayList<String>();
			for(int i=0; i<numTransactions*9; i++)
			{
				lines.add(transactionsReader.readLine());
			}
			transactionsReader.close();
			
			BufferedWriter transactionsWriter = new BufferedWriter(new FileWriter(transactionsFile));
			for(int i=0; i<numTransactions*9; i++)
			{
				if(i%9==0)
					transactionsWriter.write((i/9)+1);
				transactionsWriter.write(lines.get(i) + "\n");
			}
			transactionsWriter.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Error Found in Update68\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
}
