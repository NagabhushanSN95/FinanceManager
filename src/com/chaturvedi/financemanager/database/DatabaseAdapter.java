package com.chaturvedi.financemanager.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DatabaseAdapter extends SQLiteOpenHelper
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
	private static final String TABLE_TEMPLATES = "templates";
	
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
	private static final String KEY_AMOUNT_SPENT = "amount_spent";
	private static final String KEY_INCOME = "income";
	private static final String KEY_SAVINGS = "savings";
	private static final String KEY_WITHDRAWAL = "withdrawal";
	
	// Table Create Statements (Change Type Of Date From String To Date/DateTime whichever is available
	private static String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("+ 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_CREATED_TIME + " TEXT," +
			KEY_MODIFIED_TIME + " TEXT," +
			KEY_DATE + " TEXT," +
			KEY_TYPE + " TEXT," +
			KEY_PARTICULARS + " TEXT,"+ 
			KEY_RATE + " DOUBLE," +
			KEY_QUANTITY + " DOUBLE," +
			KEY_AMOUNT + " DOUBLE" + ")";
	
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
	
	private static String CREATE_EXPENDITURE_TYPES_TABLE = "CREATE TABLE " + TABLE_EXPENDITURE_TYPES + 
			"(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_EXPENDITURE_TYPE_NAME + " TEXT" + ")";
	
	private static String CREATE_COUNTERS_TABLE = "CREATE TABLE " + TABLE_COUNTERS + "(" + 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_DATE + " TEXT," + 
			KEY_EXP01 + " DOUBLE," + 
			KEY_EXP02 + " DOUBLE," + 
			KEY_EXP03 + " DOUBLE," + 
			KEY_EXP04 + " DOUBLE," + 
			KEY_EXP05 + " DOUBLE," + 
			KEY_AMOUNT_SPENT + " DOUBLE," + 
			KEY_INCOME + " DOUBLE," + 
			KEY_SAVINGS + " DOUBLE," + 
			KEY_WITHDRAWAL + " DOUBLE" + ")";
	
	private static String CREATE_TEMPLATES_TABLE = "CREATE TABLE " + TABLE_TEMPLATES + "(" + 
			KEY_ID + " INTEGER PRIMARY KEY," + 
			KEY_PARTICULARS + " TEXT,"+ 
			KEY_TYPE + " STRING," +
			KEY_AMOUNT + " DOUBLE" + ")";
	
	public DatabaseAdapter(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		DatabaseAdapter.context=context;
	}
			// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// Create The Tables
		db.execSQL(CREATE_TRANSACTIONS_TABLE);
		db.execSQL(CREATE_BANKS_TABLE);
		db.execSQL(CREATE_WALLET_TABLE);
		db.execSQL(CREATE_EXPENDITURE_TYPES_TABLE);
		db.execSQL(CREATE_COUNTERS_TABLE);
		db.execSQL(CREATE_TEMPLATES_TABLE);
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
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEMPLATES);
		// Create tables again
		onCreate(db);
	}
	
	/**
	* Adds A New Transaction
	* 
	*/
	public void addTransaction(Transaction transaction)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_ID, transaction.getID());
		values.put(KEY_CREATED_TIME, transaction.getCreatedTime().toString());
		values.put(KEY_MODIFIED_TIME, transaction.getModifiedTime().toString());
		values.put(KEY_DATE, transaction.getDate().getSavableDate());
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
			values.put(KEY_CREATED_TIME, transaction.getCreatedTime().toString());
			values.put(KEY_MODIFIED_TIME, transaction.getModifiedTime().toString());
			values.put(KEY_DATE, transaction.getDate().getSavableDate());
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
		Cursor cursor = db.query(TABLE_TRANSACTIONS, new String[] { KEY_ID, KEY_CREATED_TIME, 
				KEY_MODIFIED_TIME, KEY_DATE, KEY_TYPE, KEY_PARTICULARS, KEY_RATE, KEY_QUANTITY, 
				KEY_AMOUNT }, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		
		Transaction transaction = new Transaction(cursor.getInt(0), new Time(cursor.getString(1)), 
				new Time(cursor.getString(2)), new Date(cursor.getString(3)), cursor.getString(4), 
				cursor.getString(5), cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8));
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
				Transaction transaction = new Transaction(cursor.getInt(0), new Time(cursor.getString(1)), 
						new Time(cursor.getString(2)), new Date(cursor.getString(3)), cursor.getString(4), 
						cursor.getString(5), cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8));
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
		values.put(KEY_DATE, transaction.getDate().getSavableDate());
		values.put(KEY_CREATED_TIME, transaction.getCreatedTime().toString());
		values.put(KEY_MODIFIED_TIME, transaction.getModifiedTime().toString());
		values.put(KEY_PARTICULARS, transaction.getParticular());
		values.put(KEY_RATE, transaction.getRate());
		values.put(KEY_QUANTITY, transaction.getQuantity());
		values.put(KEY_AMOUNT, transaction.getAmount());
		// updating row
		db.update(TABLE_TRANSACTIONS, values, KEY_ID + " = ?", 
				new String[] { String.valueOf(transaction.getID()) });
		db.close();
	}
	
	// Deleting single Transaction
	public void deleteTransaction(Transaction transaction)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_TRANSACTIONS, KEY_ID + " = ?", new String[] { String.valueOf(transaction.getID()) });
		//Update IDs of next Transactions
		for(int i = transaction.getID(); i<=getNumTransactions();i++)
		{
			ContentValues values = new ContentValues();
			values.put(KEY_ID, i);
			// updating row
			if(db.isOpen())
			{
				db.update(TABLE_TRANSACTIONS, values, KEY_ID + " = ?", 
						new String[] { String.valueOf(i) });
			}
			else
			{
				db = this.getWritableDatabase();
				db.update(TABLE_TRANSACTIONS, values, KEY_ID + " = ?", 
						new String[] { String.valueOf(i+1) });
			}
		}
		db.close();
	}
	
	/**
	 * Deletes All The Transactions Along With The Table
	 */
	public void deleteAllTransactions()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
		db.execSQL(CREATE_TRANSACTIONS_TABLE);
		db.close();
	}
	
	// Getting Number Of Transaction
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
			
			//int i=0;
			for(Bank bank:banks)
			{
				//i++;
				ContentValues values = new ContentValues();
				//values.put(KEY_ID, i);
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
			Cursor cursor = db.query(TABLE_BANKS, new String[] { KEY_ID, KEY_NAME, KEY_ACC_NO, KEY_BALANCE, 
					KEY_SMS_NAME } , KEY_ID + "=?", new String[] { String.valueOf(id) }, 
					null, null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			
			Bank bank = new Bank(cursor.getInt(0), cursor.getString(1), cursor.getString(2), 
					cursor.getDouble(3), cursor.getString(4));
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
						bank = new Bank(cursor.getInt(0), cursor.getString(1), cursor.getString(2), 
								cursor.getDouble(3), cursor.getString(4));
					}
					else if(cursor.getColumnCount()==4)
					{
						Toast.makeText(context, "DatabaseAdapter/getAllBanks - NumColumns = 4", 
								Toast.LENGTH_LONG).show();
						bank = new Bank(0, cursor.getString(0), cursor.getString(1), cursor.getDouble(2), 
								cursor.getString(3));
					}
					else
					{
						bank = new Bank(cursor.getInt(0), cursor.getString(1), cursor.getString(2), 
								cursor.getDouble(3), cursor.getString(4));
						Toast.makeText(context, "Error In Reading Bank Details\nDatabaseAdapter\\getAllBanks" 
								+ "\nContact Developer For Assistance\n", Toast.LENGTH_LONG).show();
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
		
		public void initializeWalletTable(double walletBalance)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put(KEY_ID, 1);
			values.put(KEY_NAME, "wallet_balance");
			values.put(KEY_AMOUNT, walletBalance);
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
			
			//double walletBalance=Double.parseDouble(cursor.getString(2));
			double walletBalance=cursor.getDouble(2);
			db.close();
			return walletBalance;
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
			Cursor cursor = db.query(TABLE_EXPENDITURE_TYPES, new String[] {KEY_ID, KEY_EXPENDITURE_TYPE_NAME}, 
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
					ExpenditureTypes expenditureType = new ExpenditureTypes(cursor.getString(0), 
							cursor.getString(1));
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
			db.delete(TABLE_EXPENDITURE_TYPES, KEY_ID + " = ?", new String[] 
					{ String.valueOf(expenditureType.getID()) });
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
		
		/**
		* Adds A New Row Of Counters
		* 
		*/
		public void addCountersRow(Counters counter)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put(KEY_DATE, counter.getDate().getSavableDate());
			values.put(KEY_EXP01, counter.getExp01());
			values.put(KEY_EXP02, counter.getExp02());
			values.put(KEY_EXP03, counter.getExp03());
			values.put(KEY_EXP04, counter.getExp04());
			values.put(KEY_EXP05, counter.getExp05());
			values.put(KEY_AMOUNT_SPENT, counter.getAmountSpent());
			values.put(KEY_INCOME, counter.getIncome());
			values.put(KEY_SAVINGS, counter.getSavings());
			values.put(KEY_WITHDRAWAL, counter.getWithdrawal());
			
			// Inserting Row
			db.insert(TABLE_COUNTERS, null, values);
			db.close(); // Closing database connection
		}
		
		/**
		 * Inserts the Counters Row at the position specified by the ID
		 * @param counter
		 */
		public void insertCountersRow(Counters counter)
		{
			int position = counter.getID();
			int numRows = getNumCountersRows();
			if(position>numRows)
			{
				addCountersRow(counter);
			}
			
			// Add an extra Row and shift rows down
			Counters tempCounter = getCountersRow(numRows);		// Will retrieve based on ID which starts from 1
			tempCounter.setID(tempCounter.getID() + 1);			// and not 0
			addCountersRow(tempCounter);
			for(int i=numRows-1; i>(position-1); i--)
			{
				tempCounter = getCountersRow(i);
				tempCounter.setID(tempCounter.getID() + 1);
				updateCountersRow(tempCounter);
			}
			
			// Insert i.e. update the Row
			updateCountersRow(counter);
		}
		
		public void addAllCountersRows(ArrayList<Counters> counters)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			
			for(Counters counter: counters)
			{
				ContentValues values = new ContentValues();
				values.put(KEY_DATE, counter.getDate().getSavableDate());
				values.put(KEY_EXP01, counter.getExp01());
				values.put(KEY_EXP02, counter.getExp02());
				values.put(KEY_EXP03, counter.getExp03());
				values.put(KEY_EXP04, counter.getExp04());
				values.put(KEY_EXP05, counter.getExp05());
				values.put(KEY_AMOUNT_SPENT, counter.getAmountSpent());
				values.put(KEY_INCOME, counter.getIncome());
				values.put(KEY_SAVINGS, counter.getSavings());
				values.put(KEY_WITHDRAWAL, counter.getWithdrawal());
				
				db.insert(TABLE_COUNTERS, null, values);
			}
			db.close();
		}
		
		// Getting single Row Of Counters
		public Counters getCountersRow(int id)
		{
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.query(TABLE_COUNTERS, new String[] { KEY_ID, KEY_DATE, KEY_EXP01, KEY_EXP02, 
					KEY_EXP03, KEY_EXP04, KEY_EXP05, KEY_AMOUNT_SPENT, KEY_INCOME, KEY_SAVINGS, KEY_WITHDRAWAL}
			, KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			
			Counters counter = new Counters(cursor.getInt(0), new Date(cursor.getString(1)), 
					cursor.getDouble(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), 
					cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), 
					cursor.getDouble(10));
			db.close();
			return counter;
		}
		
		// Getting All Transactions
		public ArrayList<Counters> getAllCountersRows()
		{
			ArrayList<Counters> countersRows = new ArrayList<Counters>();
			// Select All Query
			String selectQuery = "SELECT * FROM " + TABLE_COUNTERS;
			SQLiteDatabase db = this.getWritableDatabase();
			Cursor cursor = db.rawQuery(selectQuery, null);
			// looping through all rows and adding to list
			if (cursor.moveToFirst())
			{
				do
				{
					Counters counter = new Counters(cursor.getInt(0), new Date(cursor.getString(1)), 
							cursor.getDouble(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), 
							cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), 
							cursor.getDouble(10));
					countersRows.add(counter);
				}
				while (cursor.moveToNext());
			}
			db.close();
			return countersRows;
		}
		
		// Updating single Row Of Counters
		public void updateCountersRow(Counters counter)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(KEY_DATE, counter.getDate().getSavableDate());
			values.put(KEY_EXP01, counter.getExp01());
			values.put(KEY_EXP02, counter.getExp02());
			values.put(KEY_EXP03, counter.getExp03());
			values.put(KEY_EXP04, counter.getExp04());
			values.put(KEY_EXP05, counter.getExp05());
			values.put(KEY_AMOUNT_SPENT, counter.getAmountSpent());
			values.put(KEY_INCOME, counter.getIncome());
			values.put(KEY_SAVINGS, counter.getSavings());
			values.put(KEY_WITHDRAWAL, counter.getWithdrawal());
			// updating row
			db.update(TABLE_COUNTERS, values, KEY_ID + " = ?", 
					new String[] { String.valueOf(counter.getID()) });
			db.close();
		}
		
		// Deleting single Transaction
		public void deleteCountersRow(Counters counter)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			db.delete(TABLE_COUNTERS, KEY_ID + " = ?", new String[] { String.valueOf(counter.getID()) });
			db.close();
		}
		
		/**
		 * Deletes All The Transactions Along With The Table
		 */
		public void deleteAllCountersRows()
		{
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTERS);
			db.execSQL(CREATE_COUNTERS_TABLE);
			db.close();
		}
		
		// Getting Number Of Transaction
		public int getNumCountersRows()
		{
			String countQuery = "SELECT * FROM " + TABLE_COUNTERS;
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.rawQuery(countQuery, null);
			int numRows = cursor.getCount();
			cursor.close();
			db.close();
			return numRows;
		}
		
		// Getting Number Of Template
		public int getNumTemplates()
		{
			String countQuery = "SELECT * FROM " + TABLE_TEMPLATES;
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.rawQuery(countQuery, null);
			int numTemplates = cursor.getCount();
			cursor.close();
			db.close();
			return numTemplates;
		}
		
		/**
		* Adds A New Template
		* 
		*/
		public void addTemplate(Template template)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put(KEY_ID, template.getID());
			values.put(KEY_PARTICULARS, template.getParticular());
			values.put(KEY_TYPE, template.getType());
			values.put(KEY_AMOUNT, template.getAmount());
			
			// Inserting Row
			db.insert(TABLE_TEMPLATES, null, values);
			db.close(); // Closing database connection
		}
		
		public void addAllTemplates(ArrayList<Template> templates)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			
			for(Template template: templates)
			{
				ContentValues values = new ContentValues();
				values.put(KEY_ID, template.getID());
				values.put(KEY_PARTICULARS, template.getParticular());
				values.put(KEY_TYPE, template.getType());
				values.put(KEY_AMOUNT, template.getAmount());
				
				// Inserting Row
				db.insert(TABLE_TEMPLATES, null, values);
			}
			db.close();
		}
		
		/**
		 * Inserts the Template at the position specified by the ID
		 * @param template
		 */
		public void insertTemplate(Template template)
		{
			int position = template.getID();
			Toast.makeText(context, "Position: "+position, Toast.LENGTH_SHORT).show();
			int numTemplates = getNumTemplates();
			if(position>numTemplates)
			{
				addTemplate(template);
			}
			
			// Add an extra Row and shift rows down
			Template tempTemplate = getTemplate(numTemplates);		// Template will be retrieved based on Id 
			tempTemplate.setID(tempTemplate.getID() + 1);			// Which starts from 1 and not 0
			addTemplate(tempTemplate);
			for(int i=numTemplates; i>(position-1); i--)
			{
				tempTemplate = getTemplate(i);
				tempTemplate.setID(tempTemplate.getID() + 1);
				updateTemplate(tempTemplate);
			}
			
			// Insert i.e. update the Template
			updateTemplate(template);
		}
		
		// Updating single Template
		public void updateTemplate(Template template)
		{
			SQLiteDatabase db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(KEY_PARTICULARS, template.getParticular());
			values.put(KEY_TYPE, template.getType());
			values.put(KEY_AMOUNT, template.getAmount());
			// updating row
			db.update(TABLE_TEMPLATES, values, KEY_ID + " = ?", 
					new String[] { String.valueOf(template.getID()) });
			db.close();
		}
		
		// Getting single Template
		public Template getTemplate(int id)
		{
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.query(TABLE_TEMPLATES, new String[] { KEY_ID, KEY_PARTICULARS, KEY_TYPE, 
					KEY_AMOUNT }, KEY_ID + "=?",new String[] { String.valueOf(id) }, null, null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			
			Template template = new Template(cursor.getInt(0), cursor.getString(1), 
					cursor.getString(2), cursor.getDouble(3));
			db.close();
			return template;
		}
		
		// Getting All Templates
		public ArrayList<Template> getAllTemplates()
		{
			ArrayList<Template> templatesList = new ArrayList<Template>();
			// Select All Query
			String selectQuery = "SELECT * FROM " + TABLE_TEMPLATES;
			SQLiteDatabase db = this.getWritableDatabase();
			Cursor cursor = db.rawQuery(selectQuery, null);
			// looping through all rows and adding to list
			if (cursor.moveToFirst())
			{
				do
				{
					Template template = new Template(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
							cursor.getDouble(3));
					templatesList.add(template);
				}
				while (cursor.moveToNext());
			}
			db.close();
			return templatesList;
		}
		
		// Deleting single Template
		public void deleteTemplate(Template template)
		{
			int position = template.getID();
			int numTemplates = getNumTemplates();
			// Shift all the templates upwards from this template
			for(int i=position+1; i<=numTemplates; i++)
			{
				Template tempTemplate = getTemplate(i);
				tempTemplate.setID(tempTemplate.getID() - 1);
				updateTemplate(tempTemplate);
			}
			SQLiteDatabase db = this.getWritableDatabase();
			db.delete(TABLE_TEMPLATES, KEY_ID + " = ?", new String[] { String.valueOf(numTemplates) }); // Delete Last Template
			db.close();
		}
		
		/**
		 * Deletes All The Templates Along With The Table. Then recreate the Table.
		 */
		public void deleteAllTemplates()
		{
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEMPLATES);
			db.execSQL(CREATE_TEMPLATES_TABLE);
			db.close();
		}
}
