// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.financemanager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.RestoreManager;
//import android.view.ViewGroup.LayoutParams;

public class SplashActivity extends Activity 
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_ENABLE_SPLASH = "enable_splash";
	private static final String SHARED_PREFERENCES_DATABASE = "DatabaseInitialized";
	private static final String SHARED_PREFERENCES_DATABASE_INITIALIZED = "DatabaseInitialized";
	private static final String KEY_DATABASE_INITIALIZED = "database_initialized";
	private static final String SHARED_PREFERENCES_APP = "Other App Preferences";
	private static final String KEY_QUOTE_NUMREADS = "quote_numReads";
	
	private static boolean showSplash=true;
	private static int splashTime=5000;
	private boolean databaseInitialized = true;
	
	private TextView quoteView;
	private String quoteText;
	private View progressLine;
	private LayoutParams progressLineParams;
	private int deviceWidth=1000;
	private int progressStatus=00;
	
	private int quotesNumReads = 0;
	private int NUM_TIPS = 0;
	private int NUM_QUOTES = 0;
	private int NUM_FACTS = 0;
	private int NUM_CRICKET_QUOTES = 0;
	private int NUM_MOVIE_QUOTES = 0;
	
	private Intent nextActivityIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		readPreferences();
		new DatabaseManager(this);
		readQuotes();
		startSplash();
		
		// Read the database in a seperate (non-ui) thread
		Thread databaseReaderThread = new Thread(new DatabaseReaderRunnable());
		databaseReaderThread.start();
	}
	
	private void readPreferences()
	{
		// Read If Splash Screen Is Enabled By The User
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_ENABLE_SPLASH))
		{
			showSplash=preferences.getBoolean(KEY_ENABLE_SPLASH, true);
		}
		else
		{
			showSplash = true;
		}
		
		// Check If The Database Is Initialized
		preferences = getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0);
		if(preferences.contains(KEY_DATABASE_INITIALIZED))
		{
			// Since Database Is Initialized, Read the Database And start The SummaryActivity 
			//(Home Screen Of The App)
			//DatabaseManager.readDatabase();
			databaseInitialized = true;
			nextActivityIntent = new Intent(this, SummaryActivity.class);
		}
		else
		{
			// In the update 3.1.0, the name of the Shared Preferences Changed 
			// From DatabaseInitialized to Database.
			// So, check if that preferences file is there 
			SharedPreferences preferences_copy = getSharedPreferences(SHARED_PREFERENCES_DATABASE_INITIALIZED, 0);
			if(preferences_copy.contains(KEY_DATABASE_INITIALIZED))
			{
				// Since Database Is Initialized, Read the Database And start The SummaryActivity 
				//(Home Screen Of The App) and save it in Database Preferences File
				//DatabaseManager.readDatabase();
				databaseInitialized = true;
				nextActivityIntent = new Intent(this, SummaryActivity.class);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean(KEY_DATABASE_INITIALIZED, true);
				editor.commit();
			}
			else
			{
				// Since Database is not Initialized, Start the Setup
				databaseInitialized = false;
				nextActivityIntent = new Intent(this, StartupActivity.class);
			}
		}
		
		// Retrieve Num Quote Reads
		preferences = getSharedPreferences(SHARED_PREFERENCES_APP, 0);
		if(preferences.contains(KEY_QUOTE_NUMREADS))
		{
			quotesNumReads=preferences.getInt(KEY_QUOTE_NUMREADS, 0);
		}
		else
		{
			quotesNumReads = 0;
		}
		
		// If Splash Screen is Enabled, increament quoteNumReads and save it
		if(showSplash)
		{
			quotesNumReads++;
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt(KEY_QUOTE_NUMREADS, quotesNumReads);
			editor.commit();
		}
	}
	
	/**
	 * Reads The Quotes from the "quotes.txt" raw file to display in the Splash Screen
	 */
	private void readQuotes()
	{
		ArrayList<String> quotes=new ArrayList<String>();
		InputStream tipsStream = getResources().openRawResource(R.raw.tips);
		BufferedReader tipsReader = new BufferedReader(new InputStreamReader(tipsStream));
		InputStream quoteStream = getResources().openRawResource(R.raw.quotes);
		BufferedReader quotesReader = new BufferedReader(new InputStreamReader(quoteStream));
		InputStream factsStream = getResources().openRawResource(R.raw.facts);
		BufferedReader factsReader = new BufferedReader(new InputStreamReader(factsStream));
		InputStream cricketStream = getResources().openRawResource(R.raw.cricket);
		BufferedReader cricketReader = new BufferedReader(new InputStreamReader(cricketStream));
		InputStream moviesStream = getResources().openRawResource(R.raw.movies);
		BufferedReader moviesReader = new BufferedReader(new InputStreamReader(moviesStream));
		Random randomNumber=new Random();
		try
		{
			// Read the lines in "tips" Raw File
			String line=tipsReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_TIPS++;
				line=tipsReader.readLine();
			}
			
			// Read the lines in "quotes" Raw File
			line=quotesReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_QUOTES++;
				line=quotesReader.readLine();
			}
			
			// Read the lines in "facts" Raw File
			line=factsReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_FACTS++;
				line=factsReader.readLine();
			}
			
			// Read the lines in "cricket" Raw File
			line=cricketReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_CRICKET_QUOTES++;
				line=cricketReader.readLine();
			}
			
			// Read the lines in "movies" Raw File
			line=moviesReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_MOVIE_QUOTES++;
				line=moviesReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		// If Debug Version, Don't display Tips
		if(0 != (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			quotesNumReads += NUM_TIPS;
		}	
				
		int NUM_TOTAL_QUOTES = NUM_QUOTES + NUM_FACTS + NUM_CRICKET_QUOTES + NUM_MOVIE_QUOTES;
		// Select A Random Quote depending on Number Of Quote Reads
		if(quotesNumReads<=NUM_TIPS)
		{
			// Very less reads. So, display only Tips in order
			quoteText=quotes.get(quotesNumReads);
		}
		else if(quotesNumReads<=1000)
		{
			// Average reads. So, display both tips and quotes
			quoteText=quotes.get(randomNumber.nextInt(NUM_TIPS + NUM_TOTAL_QUOTES));
		}
		else
		{
			// Very high reads. So, display only quotes
			quoteText=quotes.get(randomNumber.nextInt(NUM_TOTAL_QUOTES) + NUM_TIPS);
		}
	}
	
	/**
	 *  Displays The Splash Screen
	 */
	private void startSplash()
	{
		// Set The Quote as the text for QuoteTextView
		quoteView=(TextView)findViewById(R.id.quote);
		quoteView.setText(quoteText);
		
		// Schedule to start the NextActivity after the specified time (splashTime)
		if(showSplash)
		{
			new Handler().postDelayed(new Runnable() 
			{
				@Override
				public void run()
				{
					startActivity(nextActivityIntent);
					finish();
				}
			} ,splashTime);
		}
		
		// Get a reference to Progress Line View
		progressLine=(View)findViewById(R.id.progress_line);
		progressLineParams=(LayoutParams) progressLine.getLayoutParams();
		// Calculate the device width in pixels
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		deviceWidth=metrics.widthPixels;

		// Calculate the refresh time to update the ProgressBar
		int refreshTime=(splashTime/deviceWidth)+1;
		// Schedule to increment the length of ProgressBar repeatedly at intervals calculated above
		Timer timer=new Timer();
		timer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// Increment the length of ProgressBar by 1dp and update
						progressStatus++;
						progressLineParams=new LayoutParams(progressStatus, LayoutParams.MATCH_PARENT);
						progressLine.setLayoutParams(progressLineParams);
					}
				});
			}
		}, 0, refreshTime);
	}
	
	/**
	 * Disable the Back Button
	 */
	@Override
	public void onBackPressed()
	{
		
	}
	
	private class DatabaseReaderRunnable implements Runnable
	{
		@Override
		public void run()
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			Looper.prepare();
			
			if(databaseInitialized)
			{
				DatabaseManager.readDatabase();
				
				// Read the backups and see if there is any change
				RestoreManager restoreManager = new RestoreManager(SplashActivity.this);
				int restoreResult = restoreManager.readBackups("Finance Manager/Auto Backup");
				// If read backups, proceed
				if(restoreResult == 0)
				{
					//If found any error, restore
					
					// Wallet Balance
					if(DatabaseManager.getWalletBalance() != restoreManager.getWalletBalance())
					{
						DatabaseManager.setWalletBalance(restoreManager.getWalletBalance());
					}
					// Transactions
					if(!DatabaseManager.areEqualTransactions(DatabaseManager.getAllTransactions(), 
							restoreManager.getAllTransactions()))
					{
						Toast.makeText(getApplicationContext(), "Database: " + DatabaseManager.getAllTransactions().size() + "SD Card: " + restoreManager.getAllTransactions().size(),
								Toast.LENGTH_LONG).show();
						DatabaseManager.setAllTransactions(restoreManager.getAllTransactions());
						Toast.makeText(getApplicationContext(), "Error Found In Transactions. Data Recovered",
								Toast.LENGTH_SHORT).show();
					}
					
					// Banks
					if(!DatabaseManager.areEqualBanks(DatabaseManager.getAllBanks(), 
							restoreManager.getAllBanks()))
					{
						DatabaseManager.setAllBanks(restoreManager.getAllBanks());
						Toast.makeText(getApplicationContext(), "Error Found In Banks. Data Recovered", 
								Toast.LENGTH_SHORT).show();
					}
					
					// Counters
					if(!DatabaseManager.areEqualCounters(DatabaseManager.getAllCounters(), 
							restoreManager.getAllCounters()))
					{
						DatabaseManager.setAllCounters(restoreManager.getAllCounters());
						Toast.makeText(getApplicationContext(), "Error Found In Counters. Data Recovered",
								Toast.LENGTH_SHORT).show();
					}
					
					// Expenditure Types
					if(!DatabaseManager.areEqualExpTypes(DatabaseManager.getAllExpenditureTypes(), 
							restoreManager.getAllExpTypes()))
					{
						DatabaseManager.setAllExpenditureTypes(restoreManager.getAllExpTypes());
						Toast.makeText(getApplicationContext(), "Error Found In Exp Types. Data Recovered",
								Toast.LENGTH_SHORT).show();
					}
					
					// Templates
					if(!DatabaseManager.areEqualTemplates(DatabaseManager.getAllTemplates(), 
							restoreManager.getAllTemplates()))
					{
						DatabaseManager.setAllTemplates(restoreManager.getAllTemplates());
						Toast.makeText(getApplicationContext(), "Error Found In Templates. Data Recovered", 
								Toast.LENGTH_SHORT).show();
					}
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Error In Automatic Restore\n" + 
							"Error Code: " + restoreResult, Toast.LENGTH_LONG).show();
				}
			}
			if(!showSplash)
			{
				startActivity(nextActivityIntent);
				finish();
			}
			Looper.loop();
			Looper.myLooper().quit();
			//new Looper().quit();
		}
	}
}
