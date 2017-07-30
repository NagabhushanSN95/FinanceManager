// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

public class Update107To110 extends SQLiteOpenHelper
{
	private Context context;

	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "expenditureManager";
	// Table names
	private static final String TABLE_TRANSACTIONS = "transactions";
	private static final String TABLE_BANKS = "banks";
	private static final String TABLE_WALLET = "wallet";
	private static final String TABLE_EXPENDITURE_TYPES = "expenditure_types";
	private static final String TABLE_TEMPLATES = "templates";

	// Common Column Names
	private static final String KEY_DELETED = "deleted";
	private static final String KEY_HIDDEN = "hidden";

	// Transaction Table Columns names
	private static final String KEY_CREATED_TIME = "created_time";
	private static final String KEY_MODIFIED_TIME = "modified_time";
	private static final String KEY_DATE = "date";
	private static final String KEY_TYPE = "type";
	private static final String KEY_PARTICULARS = "particulars";

	// Banks Table Column Names
	private static final String KEY_NAME = "name";

	public Update107To110(Context cxt)
	{
		super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
		context = cxt;
		Toast.makeText(context, "Updating...", Toast.LENGTH_LONG).show();

		SQLiteDatabase db = this.getWritableDatabase();
		updateTransactionsTable(db);
		updateWalletsTable(db);
		updateBanksTable(db);
		updateExpenditureTypesTable(db);
		updateTemplatesTable(db);
		// Updating Backups doesn't work. To update Backups, transactionType should be updated in transactions and templates
		// which is not done.
		//updateBackups();

		this.close();
	}

	private void updateTransactionsTable(SQLiteDatabase db)
	{
		DecimalFormat formatter = new DecimalFormat("00");
		String updateQuery;


		// Update "Wallet Credit" to "Credit Wallet01"
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Credit Wallet01\" WHERE " +
				KEY_TYPE + " = \"Wallet Credit\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Update "Wallet Debit Exp00" to "Debit Wallet01 Exp01"
		for(int i=0; i<getNumExpTypes(); i++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Debit Wallet01 Exp" +
					formatter.format(i+1) + "\" WHERE " + KEY_TYPE + " = \"Wallet Debit Exp" + formatter.format(i) + "\"";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Income" to "Credit Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Credit Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + " = \"Bank Credit " + formatter.format(j) + " Income\"";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Savings" to "Transfer Wallet01 Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Transfer Wallet01 Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + " = \"Bank Credit " + formatter.format(j) + " Savings\"";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Withdraw" to "Transfer Bank01 Wallet01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Transfer Bank" +
					formatter.format(j+1) + " Wallet01\" WHERE " + KEY_TYPE + " = \"Bank Debit " + formatter.format(j) + " Withdraw\"";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Exp00" to "Debit Bank01 Exp01"
		for(int j=0; j<getNumBanks(); j++)
		{
			for(int i=0; i<getNumExpTypes(); i++)
			{
				updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Debit Bank" +
						formatter.format(j+1) + " Exp" + formatter.format(i+1) + "\" WHERE " + KEY_TYPE + " = \"Bank Debit " +
						formatter.format(j) + " Exp" + formatter.format(i) + "\"";
				Log.d("Updating Transactions", updateQuery);
				if(!db.isOpen())
				{
					db = this.getWritableDatabase();
				}
				db.execSQL(updateQuery);
			}
		}

		// Update Particulars
		// Remove "Union Bank Of India Credit: "
		ArrayList<String> bankNames = getAllBanksNames();
		for(String bankName : bankNames)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
					", \"" + bankName + " Credit: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
					"\"" + bankName + " Credit: %\"";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Remove "Union Bank Of India Withdrawal: "
		for(String bankName : bankNames)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
					", \"" + bankName + " Withdrawal: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
					"\"" + bankName + " Withdrawal: %\"";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Remove "Account Transfer: "
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"Account Transfer: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"Account Transfer: %\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "Account Transfer:"
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"Account Transfer:\", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"Account Transfer:%\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "From Wallet: "
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"From Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"From Wallet: %\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "From Wallet:"
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"From Wallet:\", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"From Wallet:%\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "To Wallet: "
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"To Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"To Wallet: %\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "To Wallet:"
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"To Wallet:\", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"To Wallet:%\"";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Change CreatedTime, ModifiedTime and Date as follows
		// Time: 2016/8/3/9/7/3/9 to 2016/08/03/09/07/03/9
		// Date: 2016/8/3 to 2016/08/03
		for (int i=0; i<10; i++)
		{
			//UPDATE transactions SET created_time = replace(created_time, '/1/', '/01/')
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_CREATED_TIME + " = replace(" + KEY_CREATED_TIME +
					", '/" + i + "/', '/" + formatter.format(i) + "/')";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);

			//UPDATE transactions SET modified_time = replace(modified_time, '/1/', '/01/')
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_MODIFIED_TIME + " = replace(" + KEY_MODIFIED_TIME +
					", '/" + i + "/', '/" + formatter.format(i) + "/')";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);

			// UPDATE transactions SET date = replace(date, '/1/', '/01/')
			// UPDATE transactions SET date = replace(date, '/1', '/01') where date LIKE '%/%/1'
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_DATE + " = replace(" + KEY_DATE +
					", '/" + i + "/', '/" + formatter.format(i) + "/')";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_DATE + " = replace(" + KEY_DATE +
					", '/" + i + "', '/" + formatter.format(i) + "') WHERE " + KEY_DATE + " LIKE " + "'%/%/" + i + "'";
			Log.d("Updating Transactions", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Add Hidden Column
		updateQuery = "ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + KEY_HIDDEN + " boolean not null default 0";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);
	}

	private void updateBanksTable(SQLiteDatabase db)
	{
		// Add Hidden Column
		String updateQuery = "ALTER TABLE " + TABLE_BANKS + " ADD COLUMN " + KEY_DELETED + " boolean not null default 0";
		Log.d("Updating Banks", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);
	}

	private void updateExpenditureTypesTable(SQLiteDatabase db)
	{
		// Add Hidden Column
		String updateQuery = "ALTER TABLE " + TABLE_EXPENDITURE_TYPES + " ADD COLUMN " + KEY_DELETED + " boolean not null default 0";
		Log.d("Updating ExpTypes", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);
	}

	private void updateWalletsTable(SQLiteDatabase db)
	{
		//Change name of 1st row to Wallet01
		String updateQuery = "UPDATE " + TABLE_WALLET + " SET " + KEY_NAME + " = \"Wallet01\"";
		Log.d("Updating Wallet", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Add Hidden Column
		updateQuery = "ALTER TABLE " + TABLE_WALLET + " ADD COLUMN " + KEY_DELETED + " boolean not null default 0";
		Log.d("Updating Wallet", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);
	}

	private void updateTemplatesTable(SQLiteDatabase db)
	{
		DecimalFormat formatter = new DecimalFormat("00");
		String updateQuery;


		// Update "Wallet Credit" to "Credit Wallet01"
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Credit Wallet01\" WHERE " +
				KEY_TYPE + " = \"Wallet Credit\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Update "Wallet Debit Exp00" to "Debit Wallet01 Exp01"
		for(int i=0; i<getNumExpTypes(); i++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Debit Wallet01 Exp" +
					formatter.format(i+1) + "\" WHERE " + KEY_TYPE + " = \"Wallet Debit Exp" + formatter.format(i) + "\"";
			Log.d("Updating Templates", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Income" to "Credit Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Credit Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + " = \"Bank Credit " + formatter.format(j) + " Income\"";
			Log.d("Updating Templates", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Savings" to "Transfer Wallet01 Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Transfer Wallet01 Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + " = \"Bank Credit " + formatter.format(j) + " Savings\"";
			Log.d("Updating Templates", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Withdraw" to "Transfer Bank01 Wallet01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Transfer Bank" +
					formatter.format(j+1) + " Wallet01\" WHERE " + KEY_TYPE + " = \"Bank Debit " + formatter.format(j) + " Withdraw\"";
			Log.d("Updating Templates", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Exp00" to "Debit Bank01 Exp01"
		for(int j=0; j<getNumBanks(); j++)
		{
			for(int i=0; i<getNumExpTypes(); i++)
			{
				updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Debit Bank" +
						formatter.format(j+1) + " Exp" + formatter.format(i+1) + "\" WHERE " + KEY_TYPE + " = \"Bank Debit " +
						formatter.format(j) + " Exp" + formatter.format(i) + "\"";
				Log.d("Updating Templates", updateQuery);
				if(!db.isOpen())
				{
					db = this.getWritableDatabase();
				}
				db.execSQL(updateQuery);
			}
		}

		// Update Particulars
		// Remove "Union Bank Of India Credit: "
		ArrayList<String> bankNames = getAllBanksNames();
		for(String bankName : bankNames)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
					", \"" + bankName + " Credit: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
					"\"" + bankName + " Credit: %\"";
			Log.d("Updating Templates", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Remove "Union Bank Of India Withdrawal: "
		for(String bankName : bankNames)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
					", \"" + bankName + " Withdrawal: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
					"\"" + bankName + " Withdrawal: %\"";
			Log.d("Updating Templates", updateQuery);
			if(!db.isOpen())
			{
				db = this.getWritableDatabase();
			}
			db.execSQL(updateQuery);
		}

		// Remove "Account Transfer: "
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"Account Transfer: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"Account Transfer: %\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "Account Transfer:"
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"Account Transfer:\", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"Account Transfer:%\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "From Wallet: "
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"From Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"From Wallet: %\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "From Wallet:"
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"From Wallet:\", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"From Wallet:%\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "To Wallet: "
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"To Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"To Wallet: %\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Remove "To Wallet:"
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"To Wallet:\", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				"\"To Wallet:%\"";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		// Add Hidden Column
		updateQuery = "ALTER TABLE " + TABLE_TEMPLATES + " ADD COLUMN " + KEY_HIDDEN + " boolean not null default 0";
		Log.d("Updating Templates", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);
	}

	//-------------------------------------------------------------------------------------------//
	// These methods are copied from older DatabaseAdapter class

	private int getNumExpTypes()
	{
		String countQuery = "SELECT * FROM " + TABLE_EXPENDITURE_TYPES;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int numExpTypes = cursor.getCount();
		cursor.close();
		db.close();
		return numExpTypes;
	}

	private int getNumBanks()
	{
		String countQuery = "SELECT * FROM " + TABLE_BANKS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int numBanks = cursor.getCount();
		cursor.close();
		db.close();
		return numBanks;
	}

	private ArrayList<String> getAllBanksNames()
	{
		ArrayList<String> banksNamesList = new ArrayList<String>();
		String selectQuery = "SELECT " + KEY_NAME + " FROM " + TABLE_BANKS;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst())
		{
			do
			{
				banksNamesList.add(cursor.getString(0).trim());
			}
			while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return banksNamesList;
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

	private void updateBackups()
	{
		// Update Manual Backups
		String backupFolderName = "Chaturvedi/Finance Manager/Backups";
		File backupFolder = new File(Environment.getExternalStoragePublicDirectory("Android"), backupFolderName);
		if(backupFolder.exists())
		{
			for(File file : backupFolder.listFiles())
			{
				if(file.isFile() && file.getName().contains("Data Backup"))
				{
					new BackupsUpdater(file.getPath());
				}
			}
		}

		// Update Auto Backups
		backupFolderName = "Chaturvedi/Finance Manager/Auto Backups";
		backupFolder = new File(Environment.getExternalStoragePublicDirectory("Android"), backupFolderName);
		if(backupFolder.exists())
		{
			for(File file : backupFolder.listFiles())
			{
				if(file.isFile() && file.getName().contains("Data Backup"))
				{
					new BackupsUpdater(file.getPath());
				}
			}
		}
	}

	private class BackupsUpdater
	{

		private static final int APP_VERSION_107 = 107;

		private int appVersionNo;
		private int numTransactions;
		private int numBanks;
		private int numCountersRows;
		private int numExpTypes;
		private int numTemplates;

		private ArrayList<Bank> banks;
		private ArrayList<Transaction> transactions;
		private ArrayList<Counters> counters;
		private ArrayList<String> expTypes;
		private double walletBalance;
		private ArrayList<Template> templates;

		private  BackupsUpdater(String path)
		{
			int result = readDataBackup(path);
			if(result == 0)
			{
				backupData(path);
			}
		}

		/**
		 * Reads the backed up data
		 * @param path:
		 * @return
		 * 		0 If Read Properly
		 * 		1 If No Backup Exists
		 * 		2 Old Data
		 * 		3 Error in Catch Block
		 */
		private int readDataBackup(String path)
		{
			File backupFile = new File(path);

			if(!backupFile.exists())
			{
				return 1;
			}

			try
			{
				BufferedReader backupReader = new BufferedReader(new FileReader(backupFile));

				// Read The KEY DATA
				backupReader.readLine();
				appVersionNo = Integer.parseInt(backupReader.readLine().trim());

				if(appVersionNo < APP_VERSION_107)
				{
					return 2;
				}
				numBanks = Integer.parseInt(backupReader.readLine().trim());
				numTransactions = Integer.parseInt(backupReader.readLine().trim());
				numCountersRows = Integer.parseInt(backupReader.readLine().trim());
				numExpTypes = Integer.parseInt(backupReader.readLine().trim());
				numTemplates = Integer.parseInt(backupReader.readLine().trim());
				backupReader.readLine();

				// Read Wallet Balance
				backupReader.readLine();
				walletBalance = Double.parseDouble(backupReader.readLine());
				backupReader.readLine();

				// Read Bank Details
				backupReader.readLine();
				banks = new ArrayList<Bank>(numBanks);
				for(int i=0; i<numBanks; i++)
				{
					Bank bank = new Bank(backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),
							backupReader.readLine(),backupReader.readLine(), String.valueOf(false));
					banks.add(bank);
					backupReader.readLine();
				}

				// Read Transactions
				backupReader.readLine();
				transactions = new ArrayList<Transaction>(numTransactions);
				for(int i=0; i<numTransactions; i++)
				{
					Transaction transaction = new Transaction(backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),
							backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),backupReader.readLine()
							,backupReader.readLine(),backupReader.readLine(), String.valueOf(false));
					transactions.add(transaction);
					backupReader.readLine();
				}

				// Read Counters
				backupReader.readLine();
				counters = new ArrayList<Counters>(numCountersRows);
				for(int i=0; i<numCountersRows; i++)
				{
					int ID = Integer.parseInt(backupReader.readLine().trim());
					Date date = new Date(backupReader.readLine().trim());
					double[] counters1 = new double[numExpTypes+4];
					for(int j=0; j<numExpTypes+4; j++)
					{
						counters1[j] = Double.parseDouble(backupReader.readLine().trim());
					}
					Counters counter = new Counters(ID, date, counters1);
					counters.add(counter);
					backupReader.readLine();
				}

				// Read Expenditure Types
				backupReader.readLine();
				expTypes = new ArrayList<String>(numExpTypes);
				for(int i=0; i<numExpTypes ; i++)
				{
					expTypes.add(backupReader.readLine().trim());
				}
				backupReader.readLine();

				// Read templates
				backupReader.readLine();
				templates = new ArrayList<Template>();
				for(int i=0; i<numTemplates; i++)
				{
					String ID = backupReader.readLine().trim();
					String particulars = backupReader.readLine().trim();
					String type = backupReader.readLine().trim();
					String amount = backupReader.readLine().trim();
					backupReader.readLine();
					Template template = new Template(ID, particulars, type, amount, String.valueOf(false));
					templates.add(template);
				}
				backupReader.close();
				return 0;
			}
			catch(IOException e)
			{
				Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
				return 3;
			}
		}

		private void backupData(String path)
		{
			File backupFile = new File(path);
			try
			{
				BufferedWriter backupWriter = new BufferedWriter(new FileWriter(backupFile));

				// Store The KEY DATA
				backupWriter.write("---------------Key Data---------------\n");
				int versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
				backupWriter.write(versionNo + "\n");
				backupWriter.write(1 + "\n");
				backupWriter.write(banks.size() + "\n");
				backupWriter.write(transactions.size() + "\n");
				backupWriter.write(counters.size() + "\n");
				backupWriter.write(expTypes.size() + "\n");
				backupWriter.write(templates.size() + "\n");
				backupWriter.write("\n");

				// Write Wallets Data
				backupWriter.write("---------------Wallets---------------\n");
				backupWriter.write(1 + "\n");
				backupWriter.write("Wallet01" + "\n");
				backupWriter.write(walletBalance + "\n");
				backupWriter.write(false + "\n");
				backupWriter.write("\n");

				// Backup Banks Data
				backupWriter.write("---------------Banks---------------\n");
				for(Bank bank : banks)
				{
					backupWriter.write(bank.getID() + "\n");
					backupWriter.write(bank.getName() + "\n");
					backupWriter.write(bank.getAccNo() + "\n");
					backupWriter.write(bank.getBalance() + "\n");
					backupWriter.write(bank.getSmsName() + "\n");
					backupWriter.write(bank.isDeleted() + "\n");
					backupWriter.write("\n");
				}

				// Backup The Transactions
				backupWriter.write("---------------Transactions---------------\n");
				for(Transaction transaction : transactions)
				{
					backupWriter.write(transaction.getID() + "\n");
					backupWriter.write(transaction.getCreatedTime().toString() + "\n");
					backupWriter.write(transaction.getModifiedTime().toString() + "\n");
					backupWriter.write(transaction.getDate().getSavableDate() + "\n");
					backupWriter.write(transaction.getType() + "\n");
					backupWriter.write(transaction.getParticular() + "\n");
					backupWriter.write(transaction.getRate() + "\n");
					backupWriter.write(transaction.getQuantity() + "\n");
					backupWriter.write(transaction.getAmount() + "\n");
					backupWriter.write(transaction.isHidden() + "\n");
					backupWriter.write("\n");
				}

				// Backup Counters Data
				backupWriter.write("---------------Counters---------------\n");
				for(Counters counter : counters)
				{
					backupWriter.write(counter.getID() + "\n");
					backupWriter.write(counter.getDate().getSavableDate() + "\n");
					double[] expenditures = counter.getAllExpenditures();
					for (double expenditure : expenditures)
					{
						backupWriter.write(expenditure + "\n");
					}
					backupWriter.write(counter.getAmountSpent() + "\n");
					backupWriter.write(counter.getIncome() + "\n");
					backupWriter.write(counter.getSavings() + "\n");
					backupWriter.write(counter.getWithdrawal() + "\n");
					backupWriter.write("\n");
				}

				// Backup Expenditure Types
				backupWriter.write("---------------Expenditure Types---------------\n");
				for(int i=0; i<expTypes.size(); i++)
				{
					backupWriter.write((i+1) + "\n");
					backupWriter.write(expTypes.get(i) + "\n");
					backupWriter.write(false + "\n");
					backupWriter.write("\n");
				}

				// Backup The Templates
				backupWriter.write("---------------Templates---------------\n");
				for(Template template : templates)
				{
					backupWriter.write(template.getID() + "\n");
					backupWriter.write(template.getParticular() + "\n");
					backupWriter.write(template.getType() + "\n");
					backupWriter.write(template.getAmount() + "\n");
					backupWriter.write(template.isHidden() + "\n");
					backupWriter.write("\n");
				}

				backupWriter.close();

				Toast.makeText(context, "Data Has Been Backed-Up Successfully", Toast.LENGTH_LONG).show();
			}
			catch(IOException e)
			{
				Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			}
			catch (PackageManager.NameNotFoundException e)
			{
				Toast.makeText(context, "Error in retrieving Version No\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private class Date
	{
		private int year;
		private int month;
		private int date;

		public Date(String date)
		{
			StringTokenizer tokens = new StringTokenizer(date,"/");
			this.year = Integer.parseInt(tokens.nextToken());
			this.month = Integer.parseInt(tokens.nextToken());
			this.date = Integer.parseInt(tokens.nextToken());

			if(this.year<this.date)
			{
				int temp = this.date;
				this.date = this.year;
				this.year = temp;
			}
		}

		public Date(Date date1)
		{
			this.year = date1.getYear();
			this.month = date1.getMonth();
			this.date = date1.getDate();
		}

		String getSavableDate()
		{
			DecimalFormat formatter = new DecimalFormat("00");
			return (year + "/" + formatter.format(month) + "/" + formatter.format(this.date));
		}

		public int getYear() {
			return year;
		}

		public void setYear(int year) {
			this.year = year;
		}

		public int getMonth() {
			return month;
		}

		public void setMonth(int month) {
			this.month = month;
		}

		public int getDate() {
			return date;
		}

		public void setDate(int date) {
			this.date = date;
		}
	}

	private class Bank
	{
		private int id;
		private String name;
		private String accNo;
		private double balance;
		private String smsName;
		private boolean deleted;

		public Bank(String id, String name, String accNo, String balance, String smsName, String deleted)
		{
			this.id=Integer.parseInt(id);
			this.name=name;
			this.accNo=accNo;
			this.balance=Double.parseDouble(balance);
			this.smsName=smsName;
			this.deleted = Boolean.parseBoolean(deleted);
		}

		/**
		 * @param id the id to set
		 */
		public void setID(int id)
		{
			this.id = id;
		}

		/**
		 * @return the id
		 */
		public int getID()
		{
			return id;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name)
		{
			this.name = name;
		}

		/**
		 * @return the name
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * @return the accNo
		 */
		String getAccNo()
		{
			return accNo;
		}

		/**
		 * @param balance the balance to set
		 */
		public void setBalance(double balance)
		{
			this.balance = balance;
		}

		/**
		 * @return the balance
		 */
		public double getBalance()
		{
			return balance;
		}

		/**
		 * @return the smsName
		 */
		String getSmsName()
		{
			return smsName;
		}

		boolean isDeleted()
		{
			return deleted;
		}
	}

	private class Transaction
	{
		private int id;
		private Time createdTime; // The Time At Which The Transaction Is Created
		private Time modifiedTime; // The Time At Which The Transaction Was Last Modified
		private Date date; // The Date As Entered By The User
		private String type;
		private String particular;
		private double rate;
		private double quantity;
		private double amount;
		private boolean hidden;

		// Constructor
		public Transaction(String id, String createdTime, String modifiedTime, String date, String type, String particular,
						   String rate, String quantity, String amount, String hidden)
		{
			this.setID(Integer.parseInt(id));
			this.setCreatedTime(new Time(createdTime));
			this.setModifiedTime(new Time(modifiedTime));
			this.setDate(new Date(date));
			this.setType(type);
			this.setParticular(particular);
			this.setRate(Double.parseDouble(rate));
			this.setQuantity(Double.parseDouble(quantity));
			this.setAmount(Double.parseDouble(amount));
			this.hidden = Boolean.parseBoolean(hidden);
		}

		/**
		 * @param id the id to set
		 */
		public void setID(int id)
		{
			this.id = id;
		}

		/**
		 * @return the id
		 */
		public int getID()
		{
			return id;
		}

		/**
		 * @param createdTime The Time at which the transaction is created
		 */
		void setCreatedTime(Time createdTime)
		{
			this.createdTime = createdTime;
		}

		/**
		 * @return the time at which the transaction is created
		 */
		Time getCreatedTime()
		{
			return createdTime;
		}

		/**
		 * @param modifiedTime The Time at which the transaction is modified
		 */
		void setModifiedTime(Time modifiedTime)
		{
			this.modifiedTime = modifiedTime;
		}

		/**
		 * @return the time at which the transaction is created
		 */
		Time getModifiedTime()
		{
			return modifiedTime;
		}

		/**
		 * @param date the date to set
		 */
		public void setDate(Date date)
		{
			this.date = date;
		}

		/**
		 * @return the date
		 */
		public Date getDate()
		{
			return date;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(String type)
		{
			this.type = type;
		}

		/**
		 * @return the type
		 */
		public String getType()
		{
			return type;
		}

		/**
		 * @param particular the particular to set
		 */
		void setParticular(String particular)
		{
			this.particular = particular;
		}

		/**
		 * @return the particular
		 */
		public String getParticular()
		{
			return particular;
		}

		/**
		 * @param rate the rate to set
		 */
		public void setRate(double rate)
		{
			this.rate = rate;
		}

		/**
		 * @return the rate
		 */
		public double getRate()
		{
			return rate;
		}

		/**
		 * @param quantity the quantity to set
		 */
		public void setQuantity(double quantity)
		{
			this.quantity = quantity;
		}

		/**
		 * @return the quantity
		 */
		public double getQuantity()
		{
			return quantity;
		}

		/**
		 * @param amount the amount to set
		 */
		public void setAmount(double amount)
		{
			this.amount = amount;
		}

		/**
		 * @return the amount
		 */
		public double getAmount()
		{
			return amount;
		}

		boolean isHidden()
		{
			return hidden;
		}

	}

	private class Counters
	{
		private int id;
		private Date date;
		private double[] exp;
		private double amountSpent;
		private double income;
		private double savings;
		private double withdrawal;

		// Constructor
		public Counters(int id, Date date, double[] exp)
		{
			this.id = id;
			this.date = date;
			int numExpTypes = exp.length-4;
			this.exp = new double[numExpTypes];
			for(int i=0; i<numExpTypes; i++)
			{
				this.exp[i] = exp[i];
			}
			this.amountSpent = exp[numExpTypes];
			this.income = exp[numExpTypes+1];
			this.savings = exp[numExpTypes+2];
			this.withdrawal = exp[numExpTypes+3];
		}

		/**
		 * @param id the id to set
		 */
		public void setID(int id)
		{
			this.id = id;
		}

		/**
		 * @return the id
		 */
		public int getID()
		{
			return id;
		}

		/**
		 * @param date the date to set
		 */
		public void setDate(Date date)
		{
			this.date = date;
		}

		/**
		 * @return the date
		 */
		public Date getDate()
		{
			return date;
		}

		public void setExp(double[] exp)
		{
			int numExpTypes = exp.length-4;
			for(int i=0; i<numExpTypes; i++)
			{
				this.exp[i] = exp[i];
			}
		}

		double[] getAllExpenditures()
		{
			int numExpTypes = exp.length;
			double[] exp1 = new double[numExpTypes];
			for(int i=0; i<numExpTypes; i++)
			{
				exp1[i] = exp[i];
			}
			return exp1;
		}

		/**
		 * @return the amountSpent
		 */
		double getAmountSpent() {
			return amountSpent;
		}

		/**
		 * @return the income
		 */
		public double getIncome() {
			return income;
		}

		/**
		 * @param income the income to set
		 */
		public void setIncome(double income) {
			this.income = income;
		}

		/**
		 * @return the savings
		 */
		double getSavings() {
			return savings;
		}

		/**
		 * @return the withdrawal
		 */
		double getWithdrawal() {
			return withdrawal;
		}
	}

	private class Template
	{
		private int id;
		private String particular;
		private String type;
		private double amount;
		private boolean hidden;

		public Template(String id, String particular, String type, String amount, String hidden)
		{
			this.id = Integer.parseInt(id);
			this.particular = particular;
			this.type = type;
			this.amount = Double.parseDouble(amount);
			this.hidden = Boolean.parseBoolean(hidden);
		}

		/**
		 * @param id the id to set
		 */
		public void setID(int id)
		{
			this.id = id;
		}

		/**
		 * @return the id
		 */
		public int getID()
		{
			return id;
		}

		/**
		 * @return the particular
		 */
		public String getParticular()
		{
			return particular;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(String type)
		{
			this.type = type;
		}

		/**
		 * @return the type
		 */
		public String getType()
		{
			return type;
		}

		/**
		 * @param amount the amount to set
		 */
		public void setAmount(double amount)
		{
			this.amount = amount;
		}

		/**
		 * @return the amount
		 */
		public double getAmount()
		{
			return amount;
		}

		boolean isHidden()
		{
			return hidden;
		}
	}

	private class Time
	{
		private int year;
		private int month;
		private int date;
		private int hour;
		private int minute;
		private int second;
		private int millis;

		public Time(int year, int month, int date, int hour, int minute, int second, int millis)
		{
			this.setYear(year);
			this.setMonth(month);
			this.setDate(date);
			this.setHour(hour);
			this.setMinute(minute);
			this.setSecond(second);
			this.setMillis(millis);
		}

		public Time(String time)
		{
			StringTokenizer tokens = new StringTokenizer(time,"/");
			this.year = Integer.parseInt(tokens.nextToken());
			this.month = Integer.parseInt(tokens.nextToken());
			this.date = Integer.parseInt(tokens.nextToken());
			this.hour = Integer.parseInt(tokens.nextToken());
			this.minute = Integer.parseInt(tokens.nextToken());
			this.second = Integer.parseInt(tokens.nextToken());
			this.millis = Integer.parseInt(tokens.nextToken());
		}

		public Time(Calendar calendar)
		{
			this.year = calendar.get(Calendar.YEAR);
			this.month = calendar.get(Calendar.MONTH) + 1;
			this.date = calendar.get(Calendar.DATE);
			this.hour = calendar.get(Calendar.HOUR);
			this.minute = calendar.get(Calendar.MINUTE);
			this.second = calendar.get(Calendar.SECOND);
			this.millis = calendar.get(Calendar.MILLISECOND);
		}

		public String toString()
		{
			DecimalFormat formatter = new DecimalFormat("00");
			return  (year + "/" + formatter.format(month) + "/" + formatter.format(date) + "/" + formatter.format(hour) + "/" +
					formatter.format(minute) + "/" + formatter.format(second) + "/" + millis);
		}

		public int getYear() {
			return year;
		}

		public void setYear(int year) {
			this.year = year;
		}

		public int getMonth() {
			return month;
		}

		public void setMonth(int month) {
			this.month = month;
		}

		public int getDate() {
			return date;
		}

		public void setDate(int date) {
			this.date = date;
		}

		void setHour(int hour) {
			this.hour = hour;
		}

		void setMinute(int minute) {
			this.minute = minute;
		}

		void setSecond(int second) {
			this.second = second;
		}

		void setMillis(int millis) {
			this.millis = millis;
		}
	}

}
