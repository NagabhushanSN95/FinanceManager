// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

/**
 * Adds one more column "Include in Counters" to Transactions Table
 */
public class Update111to124 extends SQLiteOpenHelper
{
	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "expenditureManager";
	private static final String TABLE_TRANSACTIONS = "transactions";
	private static final String KEY_INCLUDE_IN_COUNTERS = "include_in_counters";
	private Context context;

	public Update111to124(Context cxt)
	{
		super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
		context = cxt;

		updateDatabase();

		this.close();
	}

	private void updateDatabase()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		String updateQuery;
		
		updateQuery = "ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + KEY_INCLUDE_IN_COUNTERS +
				" boolean not null default 1";
		Log.d("Updating Transactions", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);
	}

	// These methods are related to Database. Since this class extends, SQLiteOpenHelper class, these methods
	// have to be impemented, but will never be called. So. don't worry
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// This method will be called only when the database is created for the first time.
		// Since the database is already created, this method will not be called. So, nothing is required here
		Toast.makeText(context, "Database onCreate called in Update111to124 Class\nPlease Contact Developer ASAP",
				Toast.LENGTH_LONG).show();
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Called when database is upgraded. Not called here
		Toast.makeText(context, "Database onUpgrade called in Update111to124 Class\nPlease Contact Developer ASAP",
				Toast.LENGTH_LONG).show();
	}

}
