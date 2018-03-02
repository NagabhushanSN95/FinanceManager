// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class Update110To111 extends SQLiteOpenHelper
{
	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "expenditureManager";
	private static final String TABLE_EXPENDITURE_TYPES = "expenditure_types";
	private static final String KEY_ID = "id";
	private static final String KEY_EXPENDITURE_TYPE_NAME = "expenditure_type_name";
	private static final String KEY_NAME = "name";
	private static final String KEY_DELETED = "deleted";
	private Context context;

	public Update110To111(Context cxt)
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

		updateQuery = "ALTER TABLE wallet RENAME TO wallets";
		Log.d("Updating Wallets", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		String oldTableName = "OldExpendituresTypes";
		updateQuery = "ALTER TABLE " + TABLE_EXPENDITURE_TYPES + " RENAME TO " + oldTableName;
		Log.d("Updating ExpTypes", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		updateQuery = "CREATE TABLE " + TABLE_EXPENDITURE_TYPES + "(" +
			KEY_ID + " INTEGER PRIMARY KEY," +
			KEY_NAME + " TEXT," +
			KEY_DELETED + " BOOLEAN" + ")";
		Log.d("Updating ExpTypes", updateQuery);
		if(!db.isOpen())
		{
			db = this.getWritableDatabase();
		}
		db.execSQL(updateQuery);

		updateQuery = "INSERT INTO " + TABLE_EXPENDITURE_TYPES + "(" + KEY_ID + ", " + KEY_NAME + ", " + KEY_DELETED + ")" +
				" SELECT " + KEY_ID + ", " + KEY_EXPENDITURE_TYPE_NAME + ", " + KEY_DELETED + " FROM " + oldTableName;
		Log.d("Updating ExpTypes", updateQuery);
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
