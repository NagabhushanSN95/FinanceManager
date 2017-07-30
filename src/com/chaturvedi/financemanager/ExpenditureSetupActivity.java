package com.chaturvedi.financemanager;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;

public class ExpenditureSetupActivity extends Activity
{
	private final int NUM_EXPENDITURE_TYPES=5;
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_ENABLE_SPLASH = "enable_splash";
	private static final String KEY_BANK_SMS = "respond_bank_messages";
	private static final String SHARED_PREFERENCES_VERSION = "app_version";
	private static final String KEY_VERSION = "version";
	private static int VERSION_NO;
	private static final String SHARED_PREFERENCES_DATABASE = "DatabaseInitialized";
	private static final String KEY_DATABASE_INITIALIZED = "database_initialized";
	
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_TEXT_FIELDS;
	private int MARGIN_TOP_TEXT_FIELDS;
	private int MARGIN_LEFT_TEXT_FIELDS;
	
	private ArrayList<EditText> typeTextFields;
	private ArrayList<LayoutParams> typeFieldParams;
	private ArrayList<String> expenditureTypes;
	
	private Intent summaryIntent;
	private boolean dataEntered;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_setup_expenditure);
		}
		else
		{
			setContentView(R.layout.activity_setup_expenditure);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		WIDTH_TEXT_FIELDS = screenWidth*60/100;
		MARGIN_TOP_TEXT_FIELDS = screenHeight*5/100;
		MARGIN_LEFT_TEXT_FIELDS = screenWidth*20/100;
		
		buildLayout();
		summaryIntent = new Intent(this, SummaryActivity.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		if(VERSION.SDK_INT>10)
		{
			getMenuInflater().inflate(R.menu.activity_startup_banks, menu);
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.next:
				saveToDatabase();
				if(dataEntered)
				{
					startActivity(summaryIntent);
					finish();
				}
		}
		return true;
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		typeTextFields = new ArrayList<EditText>();
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type1));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type2));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type3));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type4));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type5));
		
		typeFieldParams = new ArrayList<LayoutParams>();
		for(int i=0; i<NUM_EXPENDITURE_TYPES; i++)
		{
			LayoutParams params = new LayoutParams(WIDTH_TEXT_FIELDS, LayoutParams.WRAP_CONTENT);
			params.setMargins(MARGIN_LEFT_TEXT_FIELDS, MARGIN_TOP_TEXT_FIELDS, 0, 0);
			typeTextFields.get(i).setLayoutParams(params);
			typeFieldParams.add(params);
		}
		if(VERSION.SDK_INT<=10)
		{
			Button nextButton = (Button)findViewById(R.id.button_finish);
			nextButton.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v)
				{
					saveToDatabase();
					if(dataEntered)
					{
						startActivity(summaryIntent);
						finish();
					}
				}
			});
		}
	}

	private void saveToDatabase()
	{
		dataEntered=true;
		
		expenditureTypes = new ArrayList<String>();
		for(int i=0; i<NUM_EXPENDITURE_TYPES; i++)
		{
			String type = typeTextFields.get(i).getText().toString();
			if(dataEntered && type.length()!=0)
			{
				expenditureTypes.add(type);
			}
			else if(dataEntered)
			{
				Toast.makeText(getApplicationContext(), "Enter Something For Expenditure Type "+(i+1), 
						Toast.LENGTH_LONG).show();
				dataEntered=false;
			}
		}
		
		if(dataEntered)
		{
			DatabaseManager.setAllExpenditureTypes(expenditureTypes);
			
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
		}
	}
}
