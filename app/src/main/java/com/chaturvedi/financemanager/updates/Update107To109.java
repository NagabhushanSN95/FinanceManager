// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseAdapter;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Update107To109 extends SQLiteOpenHelper
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
	private static final String KEY_DELETED = "deleted";
	private static final String KEY_HIDDEN = "hidden";

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
	private static final String KEY_NAME = "name";
	private static final String KEY_ACC_NO = "account_number";
	private static final String KEY_BALANCE = "balance";
	private static final String KEY_SMS_NAME = "sms_name";

	// Expenditure Types Table Column Names
	private static final String KEY_EXPENDITURE_TYPE_NAME = "expenditure_type_name";

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

	public Update107To109(Context cxt)
	{
		super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
		context = cxt;
		Toast.makeText(context, "Updating...", Toast.LENGTH_LONG).show();

		SQLiteDatabase db = this.getWritableDatabase();
		updateTransactionsTable(db);
		updateBanksTable(db);
		updateWalletsTable(db);
		updateBanksTable(db);
		updateExpenditureTypesTable(db);
		updateTemplatesTable(db);

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
		db.execSQL(updateQuery);

		// Update "Wallet Debit Exp00" to "Debit Wallet01 Exp01"
		for(int i=0; i<getNumExpTypes(); i++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Debit Wallet01 Exp" +
					formatter.format(i+1) + "\" WHERE " + KEY_TYPE + "\"Wallet Debit Exp" + formatter.format(i) + "\"";
			Log.d("Updating Transactions", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Income" to "Credit Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Credit Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + "\"Bank Credit " + formatter.format(j) + " Income\"";
			Log.d("Updating Transactions", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Savings" to "Transfer Wallet01 Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Transfer Wallet01 Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + "\"Bank Credit " + formatter.format(j) + " Savings\"";
			Log.d("Updating Transactions", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Withdraw" to "Transfer Bank01 Wallet01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Transfer Bank" +
					formatter.format(j+1) + " Wallet01\" WHERE " + KEY_TYPE + "\"Bank Debit " + formatter.format(j) + " Withdraw\"";
			Log.d("Updating Transactions", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Exp00" to "Debit Bank01 Exp01"
		for(int j=0; j<getNumBanks(); j++)
		{
			for(int i=0; i<getNumExpTypes(); i++)
			{
				updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_TYPE + " = \"Debit Bank" +
						formatter.format(j+1) + " Exp" + formatter.format(i+1) + "\" WHERE " + KEY_TYPE + "\"Bank Debit " +
						formatter.format(j) + " Exp" + formatter.format(i) + "\"";
				Log.d("Updating Transactions", updateQuery);
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
					", \"" + bankName + " Credit: %\"";
			Log.d("Updating Transactions", updateQuery);
			db.execSQL(updateQuery);
		}

		// Remove "Union Bank Of India Withdrawal: "
		for(String bankName : bankNames)
		{
			updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
					", \"" + bankName + " Withdrawal: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
					", \"" + bankName + " Withdrawal: %\"";
			Log.d("Updating Transactions", updateQuery);
			db.execSQL(updateQuery);
		}

		// Remove "Account Transfer: "
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"Account Transfer: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				", \"Account Transfer: %\"";
		Log.d("Updating Transactions", updateQuery);
		db.execSQL(updateQuery);

		// Remove "From Wallet: "
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"From Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				", \"From Wallet: %\"";
		Log.d("Updating Transactions", updateQuery);
		db.execSQL(updateQuery);

		// Remove "To Wallet: "
		updateQuery = "UPDATE " + TABLE_TRANSACTIONS + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"To Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				", \"To Wallet: %\"";
		Log.d("Updating Transactions", updateQuery);
		db.execSQL(updateQuery);

		// Add Hidden Column
		updateQuery = "ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + KEY_HIDDEN + " boolean not null default false";
		Log.d("Updating Transactions", updateQuery);
		db.execSQL(updateQuery);
	}

	private void updateBanksTable(SQLiteDatabase db)
	{
		// Add Hidden Column
		String updateQuery = "ALTER TABLE " + TABLE_BANKS + " ADD COLUMN " + KEY_DELETED + " boolean not null default false";
		Log.d("Updating Banks", updateQuery);
		db.execSQL(updateQuery);
	}

	private void updateExpenditureTypesTable(SQLiteDatabase db)
	{
		// Add Hidden Column
		String updateQuery = "ALTER TABLE " + TABLE_EXPENDITURE_TYPES + " ADD COLUMN " + KEY_DELETED + " boolean not null default false";
		Log.d("Updating ExpTypes", updateQuery);
		db.execSQL(updateQuery);
	}

	private void updateWalletsTable(SQLiteDatabase db)
	{
		//Change name of 1st row to Wallet01
		String updateQuery = "UPDATE " + TABLE_WALLET + " SET " + KEY_NAME + " = \"Wallet01\"";
		Log.d("Updating Wallet", updateQuery);
		db.execSQL(updateQuery);

		// Add Hidden Column
		updateQuery = "ALTER TABLE " + TABLE_WALLET + " ADD COLUMN " + KEY_DELETED + " boolean not null default false";
		Log.d("Updating Wallet", updateQuery);
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
		db.execSQL(updateQuery);

		// Update "Wallet Debit Exp00" to "Debit Wallet01 Exp01"
		for(int i=0; i<getNumExpTypes(); i++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Debit Wallet01 Exp" +
					formatter.format(i+1) + "\" WHERE " + KEY_TYPE + "\"Wallet Debit Exp" + formatter.format(i) + "\"";
			Log.d("Updating Templates", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Income" to "Credit Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Credit Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + "\"Bank Credit " + formatter.format(j) + " Income\"";
			Log.d("Updating Templates", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Credit 00 Savings" to "Transfer Wallet01 Bank01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Transfer Wallet01 Bank" +
					formatter.format(j+1) + "\" WHERE " + KEY_TYPE + "\"Bank Credit " + formatter.format(j) + " Savings\"";
			Log.d("Updating Templates", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Withdraw" to "Transfer Bank01 Wallet01"
		for(int j=0; j<getNumBanks(); j++)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Transfer Bank" +
					formatter.format(j+1) + " Wallet01\" WHERE " + KEY_TYPE + "\"Bank Debit " + formatter.format(j) + " Withdraw\"";
			Log.d("Updating Templates", updateQuery);
			db.execSQL(updateQuery);
		}

		// Update "Bank Debit 00 Exp00" to "Debit Bank01 Exp01"
		for(int j=0; j<getNumBanks(); j++)
		{
			for(int i=0; i<getNumExpTypes(); i++)
			{
				updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_TYPE + " = \"Debit Bank" +
						formatter.format(j+1) + " Exp" + formatter.format(i+1) + "\" WHERE " + KEY_TYPE + "\"Bank Debit " +
						formatter.format(j) + " Exp" + formatter.format(i) + "\"";
				Log.d("Updating Templates", updateQuery);
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
					", \"" + bankName + " Credit: %\"";
			Log.d("Updating Templates", updateQuery);
			db.execSQL(updateQuery);
		}

		// Remove "Union Bank Of India Withdrawal: "
		for(String bankName : bankNames)
		{
			updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
					", \"" + bankName + " Withdrawal: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
					", \"" + bankName + " Withdrawal: %\"";
			Log.d("Updating Templates", updateQuery);
			db.execSQL(updateQuery);
		}

		// Remove "Account Transfer: "
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"Account Transfer: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				", \"Account Transfer: %\"";
		Log.d("Updating Templates", updateQuery);
		db.execSQL(updateQuery);

		// Remove "From Wallet: "
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"From Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				", \"From Wallet: %\"";
		Log.d("Updating Templates", updateQuery);
		db.execSQL(updateQuery);

		// Remove "To Wallet: "
		updateQuery = "UPDATE " + TABLE_TEMPLATES + " SET " + KEY_PARTICULARS + " = replace(" + KEY_PARTICULARS +
				", \"To Wallet: \", \"\") WHERE " + KEY_PARTICULARS + " LIKE " +
				", \"To Wallet: %\"";
		Log.d("Updating Templates", updateQuery);
		db.execSQL(updateQuery);

		// Add Hidden Column
		updateQuery = "ALTER TABLE " + TABLE_TEMPLATES + " ADD COLUMN " + KEY_HIDDEN + " boolean not null default false";
		Log.d("Updating Templates", updateQuery);
		db.execSQL(updateQuery);
	}

	//-------------------------------------------------------------------------------------------//
	// These methods are copied from older DatabaseAdapter class

	public int getNumExpTypes()
	{
		String countQuery = "SELECT * FROM " + TABLE_EXPENDITURE_TYPES;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int numExpTypes = cursor.getCount();
		cursor.close();
		db.close();
		return numExpTypes;
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

	public ArrayList<String> getAllBanksNames()
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
}
