package com.chaturvedi.financemanager.updates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.chaturvedi.financemanager.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

public class Update68To88 extends SQLiteOpenHelper		// To create Templates Table
{
	private Context context;
	private int CURRENT_APP_VERSION_NO;

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "expenditureManager";
	public Update68To88(Context cxt)
	{
		super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
		context = cxt;
		CURRENT_APP_VERSION_NO = Integer.parseInt(context.getResources().getString(R.string.currentAppVersion));
		
		Toast.makeText(context, "Updating The App", Toast.LENGTH_LONG).show();
		updateDatabase();
		updateSharedPreferences();
		updateBackups();
	}
	
	private void updateDatabase()
	{
		final String TABLE_TEMPLATES = "templates";
		final String KEY_ID = "id";
		final String KEY_TYPE = "type";
		final String KEY_PARTICULARS = "particulars";
		final String KEY_AMOUNT = "amount";
		
		String CREATE_TEMPLATES_TABLE = "CREATE TABLE " + TABLE_TEMPLATES + "(" + 
				KEY_ID + " INTEGER PRIMARY KEY," + 
				KEY_PARTICULARS + " TEXT,"+ 
				KEY_TYPE + " STRING," +
				KEY_AMOUNT + " DOUBLE" + ")";
		
		// Create The Table
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEMPLATES);		// Table will not exist. To be on safer side
		db.execSQL(CREATE_TEMPLATES_TABLE);
		db.close();
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
		String bankSmsResponse = "Popup";
		
		
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
			bankSmsResponse=oldPreferences.getBoolean(KEY_BANK_SMS_OLD, true)? "Popup" : "NoResponse";
		}
		else
		{
			bankSmsResponse = "Popup";
		}
		editor.putString(KEY_RESPOND_BANK_SMS, bankSmsResponse);
		
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
		
		String backupFolderName = "Finance Manager/Backups";
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
		String transactionsFileName = "Transactions";
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
					transactionsWriter.write(((i/9)+1) + "\n");
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
		
		// In Banks File, add ID before each transaction
		String banksFileName = "Banks";
		File banksFile = new File(backupFolder, banksFileName+extension);
		try
		{
			BufferedReader banksReader = new BufferedReader(new FileReader(banksFile));
			ArrayList<String> lines = new ArrayList<String>();
			for(int i=0; i<numBanks*5; i++)
			{
				lines.add(banksReader.readLine());
			}
			banksReader.close();
			
			BufferedWriter banksWriter = new BufferedWriter(new FileWriter(banksFile));
			for(int i=0; i<numBanks*5; i++)
			{
				if(i%5==0)
					banksWriter.write(((i/5)+1) + "\n");
				banksWriter.write(lines.get(i) + "\n");
			}
			banksWriter.close();
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
		
		// In Counters File, add ID before each transaction
		String countersFileName = "Counters";
		File countersFile = new File(backupFolder, countersFileName+extension);
		try
		{
			BufferedReader countersReader = new BufferedReader(new FileReader(countersFile));
			ArrayList<String> lines = new ArrayList<String>();
			for(int i=0; i<numCountersRows*11; i++)
			{
				lines.add(countersReader.readLine());
			}
			countersReader.close();
			
			BufferedWriter countersWriter = new BufferedWriter(new FileWriter(countersFile));
			for(int i=0; i<numCountersRows*11; i++)
			{
				if(i%11==0)
					countersWriter.write(((i/11)+1) + "\n");
				countersWriter.write(lines.get(i) + "\n");
			}
			countersWriter.close();
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
		
		// For older version, if wallet balance is not backed up
		if(backupVersionNo == 56)
		{
			String walletFileName = "Wallet";
			File walletFile = new File(backupFolder, walletFileName + extension);
			try
			{
				BufferedWriter walletWriter = new BufferedWriter(new FileWriter(walletFile));
				walletWriter.write("0\n");
				walletWriter.close();
			}
			catch(IOException e)
			{
				Toast.makeText(context, "Error Found in Update68/Wallet\nPlease Report To The Developer", 
						Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
		
		// Create Templates File
		String templatesFileName = "Templates";
		File templatesFile = new File(backupFolder, templatesFileName+extension);
		try
		{
			BufferedWriter templatesWriter = new BufferedWriter(new FileWriter(templatesFile));
			templatesWriter.write("\n");
			templatesWriter.close();
		}
		catch(IOException e)
		{
			Toast.makeText(context, "Error Found in Update68/Templates\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	// These methods are related to Database. Since this class extends, SQLiteOpenHelper class, these methods 
	// have to be impemented, but will never be called. So. don't worry
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// This method will be called only when the database is created for the first time.
		// Since the database is already created, this method will not be called. So, nothing is required here
		Toast.makeText(context, "Database onCreate called in Updata68 Class\nPlease Contact Developer ASAP", 
				Toast.LENGTH_LONG).show();
	}
	
	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Called when database is upgraded. Not called here
		Toast.makeText(context, "Database onUpgrade called in Updata68 Class\nPlease Contact Developer ASAP", 
				Toast.LENGTH_LONG).show();
	}
}
