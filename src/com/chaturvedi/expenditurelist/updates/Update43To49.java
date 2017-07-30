package com.chaturvedi.expenditurelist.updates;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.Counters;
import com.chaturvedi.expenditurelist.database.Date;
import com.chaturvedi.expenditurelist.database.Time;

public class Update43To49 extends SQLiteOpenHelper
{
	private static Context context;
	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "expenditureManager";
	// Table names
	private static final String TABLE_TRANSACTIONS = "transactions";
	private static final String TABLE_BANKS = "banks";
	private static final String TABLE_WALLET = "wallet";
	private static final String TABLE_EXPENDITURE_TYPES = "expenditure_types";
	private static final String TABLE_COUNTERS = "counters";
	
	// Common Column Names
	private static final String KEY_ID = "id";
	
	// Transaction Table Columns names
	private static final String KEY_CREATED_TIME = "created_time";
	private static final String KEY_MODIFIED_TIME = "modified_time";
	private static final String KEY_DATE = "date";
	private static final String KEY_TYPE = "type";
	private static final String KEY_PARTICULARS = "particulars";
	private static final String KEY_RATE = "rate";
	private static final String KEY_QUANTITY = "quantity";
	private static final String KEY_AMOUNT = "amount";
	
	// Banks Table Column Names
	private static final String KEY_NAME="name";
	private static final String KEY_ACC_NO="account_number";
	private static final String KEY_BALANCE="balance";
	private static final String KEY_SMS_NAME="sms_name";
	
	// Expenditure Types Table Column Names
	private static final String KEY_EXPENDITURE_TYPE_NAME="expenditure_type_name";
	
	// Counters Table Column Names
	private static final String KEY_EXP01 = "expenditure_01";
	private static final String KEY_EXP02 = "expenditure_02";
	private static final String KEY_EXP03 = "expenditure_03";
	private static final String KEY_EXP04 = "expenditure_04";
	private static final String KEY_EXP05 = "expenditure_05";
	
	// Table Create Statements 
	private static String CREATE_TRANSACTIONS_OLD_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("+ 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_DATE + " STRING," +
			KEY_TYPE + " STRING," +
			KEY_PARTICULARS + " TEXT,"+ 
			KEY_RATE + " DOUBLE," +
			KEY_QUANTITY + " INTEGER," +
			KEY_AMOUNT + " TEXT" + ")";

	private static String CREATE_TRANSACTIONS_NEW_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("+ 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_CREATED_TIME + " STRING," +
			KEY_MODIFIED_TIME + " STRING," +
			KEY_DATE + " STRING," +
			KEY_TYPE + " STRING," +
			KEY_PARTICULARS + " TEXT,"+ 
			KEY_RATE + " DOUBLE," +
			KEY_QUANTITY + " DOUBLE," +
			KEY_AMOUNT + " TEXT" + ")";
	
	private static String CREATE_BANKS_TABLE = "CREATE TABLE " + TABLE_BANKS + "("+ 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_NAME + " TEXT," + 
			KEY_ACC_NO + " TEXT," + 
			KEY_BALANCE + " DOUBLE," +
			KEY_SMS_NAME + " TEXT" + ")";
	
	private static String CREATE_WALLET_TABLE = "CREATE TABLE " + TABLE_WALLET + "("+ 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_NAME + " TEXT,"+ 
			KEY_AMOUNT + " DOUBLE" + ")";
	
	private static String CREATE_EXPENDITURE_TYPES_TABLE = "CREATE TABLE " + TABLE_EXPENDITURE_TYPES + "(" + 
			KEY_ID + " INTEGER PRIMARY KEY," +
			KEY_EXPENDITURE_TYPE_NAME + " TEXT" + ")";
	
	private static String CREATE_COUNTERS_OLD_TABLE = "CREATE TABLE " + TABLE_COUNTERS + "(" + 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_NAME + " TEXT," + 
			KEY_AMOUNT + " DOUBLE" + ")";
	
	private static String CREATE_COUNTERS_NEW_TABLE = "CREATE TABLE " + TABLE_COUNTERS + "(" + 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_DATE + " TEXT," + 
			KEY_EXP01 + " DOUBLE," + 
			KEY_EXP02 + " DOUBLE," + 
			KEY_EXP03 + " DOUBLE," + 
			KEY_EXP04 + " DOUBLE," + 
			KEY_EXP05 + " DOUBLE" + ")";
	
	private int numTransactions;
	private ArrayList<Time> createdTimes;
	private ArrayList<Time> modifiedTimes;
	private ArrayList<String> oldDates;
	private ArrayList<Date> newDates;
	private ArrayList<String> oldTypes;
	private ArrayList<String> newTypes;
	private ArrayList<String> oldParticulars;
	private ArrayList<String> newParticulars;
	private ArrayList<Double> rates;
	private ArrayList<Double> quantities;
	private ArrayList<Double> amounts;
	
	private int numBanks;
	private ArrayList<String> bankNames;
	
	private ArrayList<String> expenditureTypes;
	
	private int numCountersRows;
	private ArrayList<Counters> countersRows;
	
	public Update43To49(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Toast.makeText(context, "Check-Point 07", Toast.LENGTH_SHORT).show();
		Update43To49.context=context;
		readOldDatabase();
		rectifyTransactionsTable();
		buildCountersTable();
		saveNewDatabase();
	}
	
	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// Create The Tables
		db.execSQL(CREATE_TRANSACTIONS_NEW_TABLE);
		db.execSQL(CREATE_BANKS_TABLE);
		db.execSQL(CREATE_WALLET_TABLE);
		db.execSQL(CREATE_EXPENDITURE_TYPES_TABLE);
		db.execSQL(CREATE_COUNTERS_OLD_TABLE);
	}
	
	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_BANKS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WALLET);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENDITURE_TYPES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTERS);
		// Create tables again
		onCreate(db);
	}
	
	private void readOldDatabase()
	{
		Toast.makeText(context, "Check-Point 08", Toast.LENGTH_SHORT).show();
		createdTimes = new ArrayList<Time>();
		modifiedTimes = new ArrayList<Time>();
		oldDates = new ArrayList<String>();
		newDates = new ArrayList<Date>();
		oldTypes = new ArrayList<String>();
		newTypes = new ArrayList<String>();
		oldParticulars = new ArrayList<String>();
		newParticulars = new ArrayList<String>();
		rates = new ArrayList<Double>();
		quantities = new ArrayList<Double>();
		amounts = new ArrayList<Double>();
		
		bankNames = new ArrayList<String>();
		expenditureTypes = new ArrayList<String>();
		countersRows = new ArrayList<Counters>();
		
		// Select All Query From Transactions Table
		String transactionsQuery = "SELECT * FROM " + TABLE_TRANSACTIONS;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(transactionsQuery, null);
		numTransactions = cursor.getCount();
		if (cursor.moveToFirst())
		{
			do
			{
				oldDates.add(cursor.getString(1));
				oldTypes.add(cursor.getString(2));
				Toast.makeText(context, "Check-Point 09: "+oldTypes.get(0), Toast.LENGTH_SHORT).show();
				oldParticulars.add(cursor.getString(3));
				rates.add(Double.parseDouble(cursor.getString(4)));
				quantities.add(Double.parseDouble(cursor.getString(5)));
				amounts.add(Double.parseDouble(cursor.getString(6)));
			}
			while (cursor.moveToNext());
		}
		
		// Select All Query From Banks Table
		String bankQuery = "SELECT * FROM " + TABLE_BANKS;
		cursor = db.rawQuery(bankQuery, null);
		numBanks = cursor.getCount();
		if (cursor.moveToFirst())
		{
			do
			{
				bankNames.add(cursor.getString(1));
			}
			while (cursor.moveToNext());
		}
		
		// Select All Query From Banks Table
		String expQuery = "SELECT * FROM " + TABLE_EXPENDITURE_TYPES;
		cursor = db.rawQuery(expQuery, null);
		if (cursor.moveToFirst())
		{
			do
			{
				expenditureTypes.add(cursor.getString(1));
			}
			while (cursor.moveToNext());
		}
		
		db.close();
	}
	
	private void rectifyTransactionsTable()
	{
		Toast.makeText(context, "Check-Point 10", Toast.LENGTH_SHORT).show();
		for(int i=0; i<numTransactions; i++)
		{
			newDates.add(new Date(oldDates.get(i)));
			createdTimes.add(convertDateToTime(i, newDates.get(i)));
			modifiedTimes.add(convertDateToTime(i, newDates.get(i)));
			String newType = getNewType(oldTypes.get(i), oldParticulars.get(i));
			newTypes.add(newType);
			String newParticular = getNewParticular(newType, oldParticulars.get(i));
			newParticulars.add(newParticular);
			Toast.makeText(context, "Check-Point 12: "+newType, Toast.LENGTH_SHORT).show();
		}
	}
	
	
	private Time convertDateToTime(int i, Date date)
	{
		Time time = new Time(date.getYear(), date.getMonth(), date.getDate(), 0, 0, 0, i);
		return time;
	}
	
	private String getNewType(String oldType, String particulars)
	{
		String newType = null;
		
		if(oldType.equals("Income"))
		{
			Toast.makeText(context, "Check-Point 11", Toast.LENGTH_SHORT).show();
			newType = "Wallet Credit";
			return newType;
		}
		
		for(int i=0; i<5; i++)
		{
			if(oldType.equals(expenditureTypes.get(i)))
			{
				DecimalFormat formatter = new DecimalFormat("00");
				newType = "Wallet Debit Exp" + formatter.format(i);
				return newType;
			}
		}
		
		if(oldType.equals("Income Bank"))
		{
			// Determine Which Bank
			int bankNo=0;
			for(int i=0; i<numBanks; i++)
			{
				if(particulars.contains(bankNames.get(i)))
				{
					bankNo = i;
					break;
				}
			}
			DecimalFormat formatter = new DecimalFormat("00");
			newType = "Bank Credit " + formatter.format(bankNo) + " Income";
			return newType;
		}
		
		if(oldType.equals("Bank Savings"))
		{
			// Determine Which Bank
			int bankNo=0;
			for(int i=0; i<numBanks; i++)
			{
				if(particulars.contains(bankNames.get(i)))
				{
					bankNo = i;
					break;
				}
			}
			DecimalFormat formatter = new DecimalFormat("00");
			newType = "Bank Credit " + formatter.format(bankNo) + " Savings";
			return newType;
		}
		
		if(oldType.equals("Bank Withdraw"))
		{
			// Determine Which Bank
			int bankNo=0;
			for(int i=0; i<numBanks; i++)
			{
				if(particulars.contains(bankNames.get(i)))
				{
					bankNo = i;
					break;
				}
			}
			DecimalFormat formatter = new DecimalFormat("00");
			newType = "Bank Debit " + formatter.format(bankNo) + " Withdraw";
			return newType;
		}
		
		for(int i=0; i<5; i++)
		{
			int bankNo = 0;
			int expTypeNum = 0;
			if(oldType.equals(expenditureTypes.get(i) + " Bank"))
			{
				expTypeNum = i;
				// Determine Which Bank
				for(int j=0; j<numBanks; j++)
				{
					if(particulars.contains(bankNames.get(j)))
					{
						bankNo = j;
						break;
					}
				}
				DecimalFormat formatter = new DecimalFormat("00");
				newType = "Bank Debit " + formatter.format(bankNo) + " Exp" + formatter.format(expTypeNum);
				return newType;
			}
		}
		
		if(newType == null)
		{
			if(oldType.contains("Bank"))
			{
				int bankNo = 0;
				for(int i=0; i<numBanks; i++)
				{
					if(particulars.contains(bankNames.get(i)))
					{
						bankNo = i;
						break;
					}
				}
				DecimalFormat formatter = new DecimalFormat("00");
				newType = "Bank Debit " + formatter.format(bankNo) + " Exp05";
				Toast.makeText(context, "Check-point 18", Toast.LENGTH_SHORT).show();
				return newType;
			}
			else
			{
				newType = "Wallet Debit Exp05";
				return newType;
			}
		}
		return newType;
	}
	
	private String getNewParticular(String type, String oldParticular)
	{
		String newParticular = null;
		
		if(type.contains("Bank Credit") && type.contains("Income"))
		{
			int bankNo = Integer.parseInt(type.substring(12, 14));    // Bank Credit 01 Income;
			int start = bankNames.get(bankNo).length() + 8;
			String netParticular = oldParticular.substring(start);
			newParticular = bankNames.get(bankNo) + " Credit: Account Transfer: " + netParticular;
		}
		else if(type.contains("Bank Credit") && type.contains("Savings"))
		{
			int bankNo = Integer.parseInt(type.substring(12, 14));    // Bank Credit 01 Savings;
			int start = bankNames.get(bankNo).length() + 8;
			String netParticular = oldParticular.substring(start);
			newParticular = bankNames.get(bankNo) + " Credit: From Wallet: " + netParticular;
		}
		else if(type.contains("Bank Debit") && type.contains("Withdraw"))
		{
			int bankNo = Integer.parseInt(type.substring(11, 13));  // Bank Debit 01 Withdraw
			int start = bankNames.get(bankNo).length() + 12;
			String netParticular = oldParticular.substring(start);
			newParticular = bankNames.get(bankNo) + " Withdrawal: To Wallet: " + netParticular;
		}
		else if(type.contains("Bank Debit") && type.contains("Exp"))
		{
			int bankNo = Integer.parseInt(type.substring(11, 13));  // Bank Debit 01 Exp01
			int start = bankNames.get(bankNo).length() + 12;
			String netParticular = oldParticular.substring(start);
			newParticular = bankNames.get(bankNo) + " Withdrawal: Account Transfer: " + netParticular;
		}
		else
		{
			newParticular = oldParticular;
		}
		return newParticular;
	}
	
	private void buildCountersTable()
	{
		Toast.makeText(context, "Check-Point 13", Toast.LENGTH_SHORT).show();
		countersRows = new ArrayList<Counters>();
		numCountersRows = 0;
		
		for(int i=0; i<numTransactions; i++)
		{
			Toast.makeText(context, "Check-Point 14", Toast.LENGTH_SHORT).show();
			String newType = newTypes.get(i);
			if(newType.contains("Wallet Debit"))
			{
				Date date = newDates.get(i);
				int expTypeNo = Integer.parseInt(newType.substring(16, 18));   // Wallet Debit Exp01
				double amount = amounts.get(i);
				increamentCounters(date, expTypeNo, amount);
			}
			else if(newType.contains("Bank Debit") && newType.contains("Exp"))
			{
				Date date = newDates.get(i);
				int expTypeNo = Integer.parseInt(newType.substring(17, 19));//Bank Debit 01 Exp01
				double amount = amounts.get(i);
				increamentCounters(date, expTypeNo, amount);
			}
		}
	}
	
	private void increamentCounters(Date date, int expTypeNo, double amount)
	{
		Toast.makeText(context, "Check-Point 15", Toast.LENGTH_SHORT).show();
		double[] exp = {0.0, 0.0, 0.0, 0.0, 0.0};
		exp[expTypeNo] = amount;
		if(numCountersRows == 0)
		{
			countersRows = new ArrayList<Counters>();
			countersRows.add(new Counters(date, exp));
			numCountersRows++;
		}
		else if(date.getLongDate()<countersRows.get(0).getDate().getLongDate())
		{
			countersRows.add(0, new Counters(date, exp));
			numCountersRows++;
		}
		else if(date.getLongDate()>countersRows.get(numCountersRows-1).getDate().getLongDate())
		{
			countersRows.add(new Counters(date, exp));
			numCountersRows++;
		}
		else
		{
			// Search For The Date Within
			int first = 0;
			int last = numCountersRows-1;
			int middle = (first+last)/2;
			while(first<=last)
			{
				if(countersRows.get(middle).getDate().getLongDate()<date.getLongDate())
				{
					first = middle+1;
				}
				else if(countersRows.get(middle).getDate().getLongDate()>date.getLongDate())
				{
					last = middle-1;
				}
				else
				{
					countersRows.get(middle).increamentCounters(exp);
					break;
				}
				middle = (first + last)/2;
			}
			if(first>last)
			{
				countersRows.add(middle+1, new Counters(date, exp));   // Insert The New Counters Row.
			}
		}
	}
	
	private void saveNewDatabase()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		Toast.makeText(context, "Check-Point 16", Toast.LENGTH_SHORT).show();
		
		// Delete Old Table and create new Table
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
		db.execSQL(CREATE_TRANSACTIONS_NEW_TABLE);
				
		// Save Transactions Table
		for(int i=0; i<numTransactions; i++)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_CREATED_TIME, createdTimes.get(i).toString());
			values.put(KEY_MODIFIED_TIME, modifiedTimes.get(i).toString());
			values.put(KEY_DATE, newDates.get(i).getSavableDate());
			values.put(KEY_TYPE, newTypes.get(i));
			values.put(KEY_PARTICULARS, newParticulars.get(i));
			values.put(KEY_RATE, rates.get(i));
			values.put(KEY_QUANTITY, quantities.get(i));
			values.put(KEY_AMOUNT, amounts.get(i));
			
			db.insert(TABLE_TRANSACTIONS, null, values);
		}
		
		// Delete Old Table and create new Table
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTERS);
		db.execSQL(CREATE_COUNTERS_NEW_TABLE);
		for(Counters counter: countersRows)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_DATE, counter.getDate().getSavableDate());
			values.put(KEY_EXP01, counter.getExp01());
			values.put(KEY_EXP02, counter.getExp02());
			values.put(KEY_EXP03, counter.getExp03());
			values.put(KEY_EXP04, counter.getExp04());
			values.put(KEY_EXP05, counter.getExp05());
			
			db.insert(TABLE_COUNTERS, null, values);
		}
		db.close();
	}
	
	/**
	* Adds A New Transaction
	* 
	* /
	public void addTransaction(Transaction transaction)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_DATE, transaction.getDate());
		values.put(KEY_TYPE, transaction.getType());
		values.put(KEY_PARTICULARS, transaction.getParticular());
		values.put(KEY_RATE, transaction.getRate());
		values.put(KEY_QUANTITY, transaction.getQuantity());
		values.put(KEY_AMOUNT, transaction.getAmount());
		
		// Inserting Row
		db.insert(TABLE_TRANSACTIONS, null, values);
		db.close(); // Closing database connection
	}
	
	public void addAllTransactions(ArrayList<Transaction> transactions)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		for(Transaction transaction: transactions)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_DATE, transaction.getDate());
			values.put(KEY_TYPE, transaction.getType());
			values.put(KEY_PARTICULARS, transaction.getParticular());
			values.put(KEY_RATE, transaction.getRate());
			values.put(KEY_QUANTITY, transaction.getQuantity());
			values.put(KEY_AMOUNT, transaction.getAmount());
			
			db.insert(TABLE_TRANSACTIONS, null, values);
		}
		db.close();
	}
	
	// Getting single Transaction
	public Transaction getTransaction(int id)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_TRANSACTIONS, new String[] { KEY_ID, KEY_DATE, KEY_TYPE, 
				KEY_PARTICULARS, KEY_RATE, KEY_QUANTITY, KEY_AMOUNT }, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		Transaction transaction = new Transaction(cursor.getString(0), cursor.getString(1), cursor.getString(2),
				cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6));
		db.close();
		return transaction;
	}
	
	// Getting All Transactions
	public ArrayList<Transaction> getAllTransactions()
	{
		ArrayList<Transaction> transactionList = new ArrayList<Transaction>();
		// Select All Query
		String selectQuery = "SELECT * FROM " + TABLE_TRANSACTIONS;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		// looping through all rows and adding to list
		if (cursor.moveToFirst())
		{
			do
			{
				Transaction transaction = new Transaction(cursor.getString(0), cursor.getString(1), cursor.getString(2),
						cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6));
				
				transactionList.add(transaction);
			}
			while (cursor.moveToNext());
		}
		db.close();
		return transactionList;
	}
	
	// Updating single Transaction
	public void updateTransaction(Transaction transaction)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_DATE, transaction.getDate());
		values.put(KEY_PARTICULARS, transaction.getParticular());
		values.put(KEY_RATE, transaction.getRate());
		values.put(KEY_QUANTITY, transaction.getQuantity());
		values.put(KEY_AMOUNT, transaction.getAmount());
		// updating row
		db.update(TABLE_TRANSACTIONS, values, KEY_ID + " = ?", 
				new String[] { String.valueOf(transaction.getID()) });
		db.close();
	}
	
	// Deleting single contact
	public void deleteTransaction(Transaction transaction)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_TRANSACTIONS, KEY_ID + " = ?", new String[] { String.valueOf(transaction.getID()) });
		db.close();
	}
	
	/**
	 * Deletes All The Transactions Along With The Table
	 * /
	public void deleteAllTransactions()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
		db.execSQL(CREATE_TRANSACTIONS_TABLE);
		db.close();
	}
	
	// Getting contacts Count
	public int getNumTransactions()
	{
		String countQuery = "SELECT * FROM " + TABLE_TRANSACTIONS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int numTransactions = cursor.getCount();
		cursor.close();
		db.close();
		return numTransactions;
	}
	
	public void addBank(Bank bank)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, bank.getName());
		values.put(KEY_ACC_NO, bank.getAccNo());
		values.put(KEY_BALANCE, bank.getBalance());
		values.put(KEY_SMS_NAME, bank.getSmsName());
		
		db.insert(TABLE_BANKS, null, values);
		db.close();
	}
	
	public void addAllBanks(ArrayList<Bank> banks)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		int i=0;
		for(Bank bank:banks)
		{
			i++;
			ContentValues values = new ContentValues();
			values.put(KEY_ID, i);
			values.put(KEY_NAME, bank.getName());
			values.put(KEY_ACC_NO, bank.getAccNo());
			values.put(KEY_BALANCE, bank.getBalance());
			values.put(KEY_SMS_NAME, bank.getSmsName());
			
			db.insert(TABLE_BANKS, null, values);
		}
		db.close();
	}
	
	public Bank getBank(int id)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_BANKS, new String[] { KEY_ID, KEY_NAME, KEY_ACC_NO, KEY_BALANCE, KEY_SMS_NAME }
				, KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		Bank bank = new Bank(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
		Toast.makeText(context, bank.toString(), Toast.LENGTH_LONG).show();
		db.close();
		return bank;
	}
	
	public ArrayList<Bank> getAllBanks()
	{
		ArrayList<Bank> bankList = new ArrayList<Bank>();
		String selectQuery = "SELECT * FROM " + TABLE_BANKS;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst())
		{
			do
			{
				Bank bank;
				if(cursor.getColumnCount()==5)
				{

					bank = new Bank(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
				}
				else if(cursor.getColumnCount()==4)
				{

					bank = new Bank(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
				}
				else
				{
					bank = new Bank(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
					Toast.makeText(context, "Error In Reading Bank Details\nDatabaseAdapter\\getAllBanks\nContact Developer For Assistance\n", Toast.LENGTH_LONG).show();
				}
				bankList.add(bank);
			}
			while (cursor.moveToNext());
		}
		db.close();
		return bankList;
	}
	
	public void updateBank(Bank bank)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, bank.getName());
		values.put(KEY_ACC_NO, bank.getAccNo());
		values.put(KEY_BALANCE, bank.getBalance());
		values.put(KEY_SMS_NAME, bank.getSmsName());
		db.update(TABLE_BANKS, values, KEY_ID + " = ?", new String[] { String.valueOf(bank.getID()) });
		db.close();
	}
	
	public void deleteBank(Bank bank)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_BANKS, KEY_ID + " = ?", new String[] { String.valueOf(bank.getID()) });
		db.close();
	}
	
	public void deleteAllBanks()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_BANKS);
		db.execSQL(CREATE_BANKS_TABLE);
		db.close();
	}
	
	public int getNumBanks()
	{
		String countQuery = "SELECT * FROM " + TABLE_BANKS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int numBanks = cursor.getCount();
		cursor.close();
		db.close();
		return numBanks;
	}
	
	public void initializeWalletTable(double walletBalance, double amountSpent, double income)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_ID, 1);
		values.put(KEY_NAME, "wallet_balance");
		values.put(KEY_AMOUNT, walletBalance);
		db.insert(TABLE_WALLET, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 2);
		values.put(KEY_NAME, "amount_spent");
		values.put(KEY_AMOUNT, amountSpent);
		db.insert(TABLE_WALLET, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 3);
		values.put(KEY_NAME, "income");
		values.put(KEY_AMOUNT, income);
		db.insert(TABLE_WALLET, null, values);
		
		db.close();
	}
	
	public void setWalletBalance(double amount)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, "wallet_balance");
		values.put(KEY_AMOUNT, amount);
		db.update(TABLE_WALLET, values, KEY_ID + " = ?", new String[] { String.valueOf(1) });
		db.close();
	}
	
	public double getWalletBalance()
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_WALLET, new String[] { KEY_ID, KEY_NAME, KEY_AMOUNT }
				, KEY_ID + "=?", new String[] { String.valueOf(1) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		double walletBalance=Double.parseDouble(cursor.getString(2));
		db.close();
		return walletBalance;
	}
	
	public void setAmountSpent(double amount)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, "amount_spent");
		values.put(KEY_AMOUNT, amount);
		db.update(TABLE_WALLET, values, KEY_ID + " = ?", new String[] { String.valueOf(2) });
		db.close();
	}
	
	public double getAmountSpent()
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_WALLET, new String[] { KEY_ID, KEY_NAME, KEY_AMOUNT }
				, KEY_ID + "=?", new String[] { String.valueOf(2) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		double amountSpent=Double.parseDouble(cursor.getString(2));
		db.close();
		return amountSpent;
	}
	
	public void setIncome(double amount)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, "income");
		values.put(KEY_AMOUNT, amount);
		db.update(TABLE_WALLET, values, KEY_ID + " = ?", new String[] { String.valueOf(3) });
		db.close();
	}
	
	public double getIncome()
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_WALLET, new String[] { KEY_ID, KEY_NAME, KEY_AMOUNT }
				, KEY_ID + "=?", new String[] { String.valueOf(3) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		double income=Double.parseDouble(cursor.getString(2));
		db.close();
		return income;
	}

	
	public void addExpenditureType(ExpenditureTypes expenditureType)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_EXPENDITURE_TYPE_NAME, expenditureType.getExpenditureTypeName());
		db.insert(TABLE_EXPENDITURE_TYPES, null, values);
		db.close();
	}
	
	public void addAllExpenditureTypes(ArrayList<ExpenditureTypes> expenditureTypes)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		for(ExpenditureTypes expenditureType: expenditureTypes)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_EXPENDITURE_TYPE_NAME, expenditureType.getExpenditureTypeName());
			db.insert(TABLE_EXPENDITURE_TYPES, null, values);
		}
		db.close();
	}
	
	public ExpenditureTypes getExpenditureType(int id)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_EXPENDITURE_TYPES, new String[] { KEY_ID, KEY_EXPENDITURE_TYPE_NAME, }, 
				KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		ExpenditureTypes expenditureType = new ExpenditureTypes(cursor.getString(0), cursor.getString(1));
		db.close();
		return expenditureType;
	}
	
	public ArrayList<ExpenditureTypes> getAllExpenditureTypes()
	{
		ArrayList<ExpenditureTypes> expenditureTypes = new ArrayList<ExpenditureTypes>();
		String selectQuery = "SELECT * FROM " + TABLE_EXPENDITURE_TYPES;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		// looping through all rows and adding to list
		if (cursor.moveToFirst())
		{
			do
			{
				ExpenditureTypes expenditureType = new ExpenditureTypes(cursor.getString(0), cursor.getString(1));
				expenditureTypes.add(expenditureType);
			}
			while (cursor.moveToNext());
		}
		db.close();
		return expenditureTypes;
	}
	
	public void updateExpenditureType(ExpenditureTypes expenditureType)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_EXPENDITURE_TYPE_NAME, expenditureType.getExpenditureTypeName());
		db.update(TABLE_EXPENDITURE_TYPES, values, KEY_ID + " = ?",
				new String[] { String.valueOf(expenditureType.getID()) });
		db.close();
	}
	
	public void deleteExpenditureType(ExpenditureTypes expenditureType)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_EXPENDITURE_TYPES, KEY_ID + " = ?", new String[] { String.valueOf(expenditureType.getID()) });
		db.close();
	}
	
	public void deleteAllExpenditureTypes()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENDITURE_TYPES);
		db.execSQL(CREATE_EXPENDITURE_TYPES_TABLE);
		db.close();
	}
	
	public int getNumExpenditureTypes()
	{
		String countQuery = "SELECT * FROM " + TABLE_EXPENDITURE_TYPES;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.close();
		db.close();
		return cursor.getCount();
	}
	
	public void initializeCountersTable()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_ID, 1);
		values.put(KEY_NAME, "current_type1");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 2);
		values.put(KEY_NAME, "current_type2");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 3);
		values.put(KEY_NAME, "current_type3");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 4);
		values.put(KEY_NAME, "current_type4");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 5);
		values.put(KEY_NAME, "current_type5");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 6);
		values.put(KEY_NAME, "total_type1");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 7);
		values.put(KEY_NAME, "total_type2");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 8);
		values.put(KEY_NAME, "total_type3");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 9);
		values.put(KEY_NAME, "total_type4");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		values = new ContentValues();
		values.put(KEY_ID, 10);
		values.put(KEY_NAME, "total_type5");
		values.put(KEY_AMOUNT, 0);
		db.insert(TABLE_COUNTERS, null, values);
		
		db.close();
	}
	
	public void setCounters(ArrayList<Double> amounts)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		/*ContentValues values = new ContentValues();
		values.put(KEY_NAME, "current_type1");
		values.put(KEY_AMOUNT, amounts.get(0));
		db.update(TABLE_COUNTERS, values, KEY_ID + " = ?", new String[] { String.valueOf(1) });* /
		
		ContentValues values;
		for(int i=0; i<10; i++)
		{
			values = new ContentValues();
			values.put(KEY_AMOUNT, amounts.get(i));
			db.update(TABLE_COUNTERS, values, KEY_ID + " = ?", new String[] { String.valueOf(i+1) });
		}
		db.close();
	}
	
	public ArrayList<Double> getCounters()
	{
		SQLiteDatabase db = this.getReadableDatabase();
		ArrayList<Double> amounts = new ArrayList<Double>(10);
		String query = "SELECT * FROM " + TABLE_COUNTERS;
		Cursor cursor = db.rawQuery(query, null);
		if(cursor.moveToFirst())
		{
			do
			{
				amounts.add(Double.parseDouble(cursor.getString(2)));
			}
			while(cursor.moveToNext());
		}
		db.close();
		return amounts;
	}*/
}
