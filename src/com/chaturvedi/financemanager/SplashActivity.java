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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
import com.chaturvedi.financemanager.updates.Update68To76;
//import android.view.ViewGroup.LayoutParams;

public class SplashActivity extends Activity 
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_APP_VERSION = "AppVersionNo";
	private static final int APP_VERSION_NO_76 = 76;
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private int splashDuration = 5000;
	private static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	private boolean databaseInitialized = true;
	private static final String KEY_QUOTE_NO = "QuoteNo";
	
	private TextView quoteView;
	private String quoteText;
	private View progressLine;
	private LayoutParams progressLineParams;
	private int deviceWidth=1000;
	private int progressStatus=00;
	
	private int quotesNo = 0;
	private int NUM_TIPS = 0;
	private int NUM_TOTAL_QUOTES = 0;
	/*private int NUM_QUOTES = 0;
	private int NUM_FACTS = 0;
	private int NUM_CRICKET_QUOTES = 0;
	private int NUM_MOVIE_QUOTES = 0;*/
	
	private Intent nextActivityIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		checkForUpdates();
		readPreferences();
		new DatabaseManager(this);
		readQuotes();
		startSplash();
		
		// Read the database in a seperate (non-ui) thread
		Thread databaseReaderThread = new Thread(new DatabaseReaderRunnable());
		databaseReaderThread.start();
	}
	
	private void checkForUpdates()
	{
		int currentVersionNo = 0, previousVersionNo = 0;
		
		// Get the Current Version No Of The App
		try
		{
			currentVersionNo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			Toast.makeText(getApplicationContext(), "Error In Retrieving Version No In\n" + 
					"SplashActivity\\checkForUpdates\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		// Get the version no stored in the preferences. This contains the version no of the app, when it was 
		// previously opened. So, it the app is updated now, this field contains version no of old app.
		// So, update classes can be run
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		if(preferences.contains(KEY_APP_VERSION))
		{
			previousVersionNo = preferences.getInt(KEY_APP_VERSION, 0);
		}
		else
		{
			// From v3.2.1 and onwards all Preferences are stored in a single SharedPreferences file
			// But, in previous versions, AppVersionNo was stored in app_version file
			SharedPreferences versionPreferences = getSharedPreferences("app_version", 0);
			if(versionPreferences.contains("version"))
			{
				previousVersionNo = versionPreferences.getInt("version", 0);
			}
			else
			{
				previousVersionNo = -1;				// Denotes App has been opened for first time
			}
		}
		
		boolean canProceed = (currentVersionNo != 0) && (previousVersionNo > 0);
		if(canProceed && (previousVersionNo != currentVersionNo))
		{
			if(previousVersionNo < APP_VERSION_NO_76)
			{
				new Update68To76(SplashActivity.this);
			}
			editor.putInt(KEY_APP_VERSION, currentVersionNo);
			editor.commit();
		}
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();

		// Read the Splash Duration set by the user
		if(preferences.contains(KEY_SPLASH_DURATION))
		{
			splashDuration=preferences.getInt(KEY_SPLASH_DURATION, 5000);
		}
		else
		{
			splashDuration = 5000;
			editor.putInt(KEY_SPLASH_DURATION, 5000);
		}
		
		// Check If The Database Is Initialized
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
			// Database is not initialized. So, start StartupActivity
			databaseInitialized = false;
			nextActivityIntent = new Intent(this, StartupActivity.class);
		}
		
		// Retrieve Num Quote Reads
		if(preferences.contains(KEY_QUOTE_NO))
		{
			quotesNo=preferences.getInt(KEY_QUOTE_NO, 0);
			editor.putInt(KEY_QUOTE_NO, quotesNo+1);
		}
		else
		{
			quotesNo = 0;
			editor.putInt(KEY_QUOTE_NO, 1);
		}
		editor.commit();
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
				NUM_TOTAL_QUOTES++;
				line=quotesReader.readLine();
			}
			
			// Read the lines in "facts" Raw File
			line=factsReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
				line=factsReader.readLine();
			}
			
			// Read the lines in "cricket" Raw File
			line=cricketReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
				line=cricketReader.readLine();
			}
			
			// Read the lines in "movies" Raw File
			line=moviesReader.readLine();
			while(line!=null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
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
			quotesNo += NUM_TIPS;
		}	
				
		// Select A Random Quote depending on Number Of Quote Number To Be Displayed
		if(quotesNo<=NUM_TIPS*2)
		{
			// Very less reads. So, display only Tips in order
			quoteText=quotes.get(quotesNo%NUM_TIPS);
		}
		else if(quotesNo<=1000)
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
		if(splashDuration > 0)
		{
			new Handler().postDelayed(new Runnable() 
			{
				@Override
				public void run()
				{
					startActivity(nextActivityIntent);
					finish();
				}
			} ,splashDuration);
		}
		
		// Get a reference to Progress Line View
		progressLine=(View)findViewById(R.id.progress_line);
		progressLineParams=(LayoutParams) progressLine.getLayoutParams();
		// Calculate the device width in pixels
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		deviceWidth=metrics.widthPixels;

		// Calculate the refresh time to update the ProgressBar
		int refreshTime=(splashDuration/deviceWidth)+1;
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
			if(splashDuration == 0)
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
