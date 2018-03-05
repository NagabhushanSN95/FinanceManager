// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.financemanager.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.extras.RestoreManager;
import com.chaturvedi.financemanager.functions.AutomaticBackupAndRestoreManager;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.setup.StartupActivity;
import com.chaturvedi.financemanager.updates.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_APP_VERSION = "AppVersionNo";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	private static final String KEY_QUOTE_NO = "QuoteNo";
	private static final String KEY_AUTOMATIC_BACKUP_RESTORE = "AutomaticBackupAndRestore";
	private int splashDuration = 5000;
	private boolean databaseInitialized = true;
	private AutomaticBackupAndRestoreManager autoRestoreManager;
	
	private boolean splashComplete = false;                // Completion Of Splash Duration
	private boolean initializationComplete = false;        // Completion Of Database Reading & Auto Restore
	private boolean activityAlive = true;                // Set to false when back button is
	// pressed. So, next activity will not be started
	
	private String quoteText;
	private int deviceWidth = 1000;
	private int timerProgress = 0;
	private int databaseReadProgress = 0;
	
	private int quotesNo = 0;
	private int NUM_TIPS = 0;
	private int NUM_TOTAL_QUOTES = 0;
	
	private Handler databaseHandler;
	private Intent nextActivityIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		checkForUpdates();
		readPreferences();
		readQuotes();
		startSplash();
		defineHandler();
		
		// Read the database in a separate (non-ui) thread
		Thread databaseReaderThread = new Thread(new DatabaseReaderRunnable());
		databaseReaderThread.start();
	}

	private void checkForUpdates()
	{
		int currentVersionNo = 0, previousVersionNo;
		
		// Get the Current Version No Of The Current App
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
		// previously opened. So, if the app is updated now, this field contains version no of old app.
		// So, update classes can be run
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		if (preferences.contains(KEY_APP_VERSION))
		{
			previousVersionNo = preferences.getInt(KEY_APP_VERSION, 0);
		}
		else
		{
			// From v3.2.1 and onwards all Preferences are stored in a single SharedPreferences file
			// But, in previous versions, AppVersionNo was stored in app_version file
			SharedPreferences versionPreferences = getSharedPreferences("app_version", 0);
			if (versionPreferences.contains("version"))
			{
				previousVersionNo = versionPreferences.getInt("version", 0);
			}
			else
			{
				previousVersionNo = -1;                // Denotes App has been opened for first time
			}
		}
		
		// Compare the version of current and previous App. If the previous app was of old version, 
		// run the Update Classes
		boolean canProceed = (currentVersionNo != 0) && (previousVersionNo > 0);
		if (canProceed && (previousVersionNo < currentVersionNo))
		{
			Toast.makeText(this, "Updating...", Toast.LENGTH_LONG).show();
			if (previousVersionNo < Constants.APP_VERSION_88)
			{
				new Update68To88(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_89)
			{
				new Update88To89(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_96)
			{
				new Update89To96(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_107)
			{
				new Update96To107(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_110)
			{
				new Update107To110(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_111)
			{
				new Update110To111(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_124)
			{
				new Update111to124(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_125)
			{
				new Update124to125(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_131)
			{
				new Update125to131(SplashActivity.this);
			}
			if (previousVersionNo < Constants.APP_VERSION_134)
			{
				new Update131to134(SplashActivity.this);
			}
			
			editor.putInt(KEY_APP_VERSION, currentVersionNo);
			editor.apply();
		}
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();

		// Read the Splash Duration set by the user
		if (preferences.contains(KEY_SPLASH_DURATION))
		{
			splashDuration = preferences.getInt(KEY_SPLASH_DURATION, 5000);
		}
		else
		{
			splashDuration = 5000;
			editor.putInt(KEY_SPLASH_DURATION, 5000);
		}
		
		// Check If The Database Is Initialized
		if (preferences.contains(KEY_DATABASE_INITIALIZED))
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
		if (preferences.contains(KEY_QUOTE_NO))
		{
			quotesNo = preferences.getInt(KEY_QUOTE_NO, 0);
			editor.putInt(KEY_QUOTE_NO, quotesNo + 1);
		}
		else
		{
			quotesNo = 0;
			editor.putInt(KEY_QUOTE_NO, 1);
		}
		
		// Retrieve Automatic Backup And Restore Status
		if (preferences.contains(KEY_AUTOMATIC_BACKUP_RESTORE))
		{
			int value = preferences.getInt(KEY_AUTOMATIC_BACKUP_RESTORE, 3);
			autoRestoreManager = new AutomaticBackupAndRestoreManager(value);
		}
		else
		{
			autoRestoreManager = new AutomaticBackupAndRestoreManager(3);
			editor.putInt(KEY_AUTOMATIC_BACKUP_RESTORE, 3);
		}
		editor.apply();
	}
	
	/**
	 * Reads The Quotes from the "quotes.txt" raw file to display in the Splash Screen
	 */
	private void readQuotes()
	{
		ArrayList<String> quotes = new ArrayList<>();
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
		Random randomNumber = new Random();
		try
		{
			// Read the lines in "tips" Raw File
			String line = tipsReader.readLine();
			while (line != null)
			{
				quotes.add(line);
				NUM_TIPS++;
				line = tipsReader.readLine();
			}
			
			// Read the lines in "quotes" Raw File
			line = quotesReader.readLine();
			while (line != null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
				line = quotesReader.readLine();
			}
			
			// Read the lines in "facts" Raw File
			line = factsReader.readLine();
			while (line != null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
				line = factsReader.readLine();
			}
			
			// Read the lines in "cricket" Raw File
			line = cricketReader.readLine();
			while (line != null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
				line = cricketReader.readLine();
			}
			
			// Read the lines in "movies" Raw File
			line = moviesReader.readLine();
			while (line != null)
			{
				quotes.add(line);
				NUM_TOTAL_QUOTES++;
				line = moviesReader.readLine();
			}
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		// If Debug Version, Don't display Tips
		if (0 != (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			quotesNo += NUM_TIPS * 2;
		}

		// Select A Random Quote depending on Number Of Quote Number To Be Displayed
		if (quotesNo <= NUM_TIPS * 2)
		{
			// Very less reads. So, display only Tips in order
			quoteText = quotes.get(quotesNo % NUM_TIPS);
		}
		else if (quotesNo <= 1000)
		{
			// Average reads. So, display both tips and quotes
			quoteText = quotes.get(randomNumber.nextInt(NUM_TIPS + NUM_TOTAL_QUOTES));
		}
		else
		{
			// Very high reads. So, display only quotes
			quoteText = quotes.get(randomNumber.nextInt(NUM_TOTAL_QUOTES) + NUM_TIPS);
		}
	}
	
	/**
	 * Displays The Splash Screen
	 */
	private void startSplash()
	{
		// For release version, display AppIcon instead of Krishna Picture
		if ((this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
			ImageView splashIcon = (ImageView) findViewById(R.id.photo_krishna);
			splashIcon.setImageResource(R.drawable.splash_icon);
		}

		// Set The Quote as the text for QuoteTextView
		TextView quoteView = (TextView) findViewById(R.id.quote);
		quoteView.setText(quoteText);
		
		// Schedule to start the NextActivity after the specified time (splashTime)
		//if(splashDuration > 0)
		{
			new Handler().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					splashComplete = true;
					if (activityAlive && initializationComplete)
					{
						startActivity(nextActivityIntent);
						finish();
					}
				}
			}, splashDuration);
		}
		
		// Calculate the device width in pixels
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		deviceWidth = metrics.widthPixels;
		
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar_loading);
		progressBar.setMax(deviceWidth);
		
		// Change the colour (from blue) to pink
		Drawable progressBarDrawable = this.getResources().getDrawable(R.drawable
				.progress_bar_pink);
		
		progressBar.setProgressDrawable(progressBarDrawable);
		progressBar.getLayoutParams().height = 6;
		
		// Calculate the refresh time to update the ProgressBar
		int refreshTime = (splashDuration / deviceWidth) + 1;
		// Schedule to increment the length of ProgressBar repeatedly at intervals calculated above
		Timer timer = new Timer();
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
						timerProgress++;
						// databaseReadProgress will be in percentage, so normalize it w.r.t deviceWidth
						// Find the progress (minimum of timerProgress and dataaseReadProgress)
						int progress = Math.min(timerProgress, databaseReadProgress * deviceWidth / 100);
						progressBar.setProgress(progress);
					}
				});
			}
		}, 0, refreshTime);
	}
	
	private void defineHandler()
	{
		databaseHandler = new Handler(Looper.getMainLooper())
		{
			@Override
			public void handleMessage(Message databaseMessage)
			{
				// Get the Percentage Of Database Read Completion
				switch (databaseMessage.what)
				{
					case DatabaseManager.ACTION_DATABASE_READ_PROGRESS:
						databaseReadProgress = databaseMessage.arg1;
						break;

					case DatabaseManager.ACTION_INITIALIZATION_COMPLETE:
						initializationComplete = true;
						if (activityAlive && splashComplete)
						{
							startActivity(nextActivityIntent);
							finish();
						}
						break;

					case DatabaseManager.ACTION_TOAST_MESSAGE:
						String message = (String) databaseMessage.obj;
						Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
						break;

					default:
						super.handleMessage(databaseMessage);
				}
				
			}
		};
	}
	
	/**
	 * Disable the Back Button
	 */
	@Override
	public void onBackPressed()
	{
		activityAlive = false;
		super.onBackPressed();
	}
	
	private class DatabaseReaderRunnable implements Runnable
	{
		@Override
		public void run()
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			Looper.prepare();
			
			if (databaseInitialized)
			{
				if (autoRestoreManager.getValue() > 1)
				{
					// Read the backups and see if there is any change
					String autoBackupPath = Environment.getExternalStoragePublicDirectory("Android").getPath() +
							"/Chaturvedi/Finance Manager/Auto Backups/Auto Data Backup.snb";
//					RestoreManager restoreManager = new RestoreManager(SplashActivity.this, autoBackupPath, true);
					Uri fileUri = Uri.fromFile(new File(autoBackupPath));
					RestoreManager restoreManager = new RestoreManager(SplashActivity.this, fileUri, true);
					int restoreResult = restoreManager.getResult();
					// If read backups, proceed
					if (restoreResult == 0)
					{
						DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(SplashActivity.this);
						//If found any error, restore
						
						// Wallet Balance
						if (!DatabaseManager.areEqualWallets(databaseAdapter.getAllWallets(), restoreManager.getAllWallets()))
						{
							if (autoRestoreManager.isAutomaticRestore())
							{
								databaseAdapter.deleteAllWallets();
								databaseAdapter.addAllWallets(restoreManager.getAllWallets());
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Wallets. Data Recovered");
								databaseMessage.sendToTarget();
							}
							else
							{
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Wallets. Data Not Recovered");
								databaseMessage.sendToTarget();
							}
						}
						databaseReadProgress = 55;

						// Banks
						if (!DatabaseManager.areEqualBanks(databaseAdapter.getAllBanks(), restoreManager.getAllBanks()))
						{
							if (autoRestoreManager.isAutomaticRestore())
							{
								databaseAdapter.deleteAllBanks();
								databaseAdapter.addAllBanks(restoreManager.getAllBanks());
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Banks. Data Recovered");
								databaseMessage.sendToTarget();
							}
							else
							{
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Banks. Data Not Recovered");
								databaseMessage.sendToTarget();
							}
						}
						databaseReadProgress = 60;

						// Transactions
						if (!DatabaseManager.areEqualTransactions(databaseAdapter.getAllTransactions(),
								restoreManager.getAllTransactions()))
						{
							if (autoRestoreManager.isAutomaticRestore())
							{
								databaseAdapter.deleteAllTransactions();
								databaseAdapter.addAllTransactions(restoreManager.getAllTransactions());
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Transactions. Data Recovered");
								databaseMessage.sendToTarget();
							}
							else
							{
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Transactions. Data Not Recovered");
								databaseMessage.sendToTarget();
							}
						}
						databaseReadProgress = 75;
						
						// Expenditure Types
						if (!DatabaseManager.areEqualExpTypes(databaseAdapter.getAllExpenditureTypes(),
								restoreManager.getAllExpTypes()))
						{
							if (autoRestoreManager.isAutomaticRestore())
							{
								databaseAdapter.deleteAllExpenditureTypes();
								databaseAdapter.addAllExpenditureTypes(restoreManager.getAllExpTypes());
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Exp Types. Data Recovered");
								databaseMessage.sendToTarget();
							}
							else
							{
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Exp Types. Data Not Recovered");
								databaseMessage.sendToTarget();
							}
						}
						databaseReadProgress = 80;
						
						// Counters
						if (!DatabaseManager.areEqualCounters(databaseAdapter.getAllCountersRows(),
								restoreManager.getAllCounters(), databaseAdapter.getNumExpenditureTypes()))
						{
							if (autoRestoreManager.isAutomaticRestore())
							{
								databaseAdapter.deleteAllCountersRows();
								databaseAdapter.addAllCountersRows(restoreManager.getAllCounters());
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Counters. Data Recovered");
								databaseMessage.sendToTarget();
							}
							else
							{
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Counters. Data Not Recovered");
								databaseMessage.sendToTarget();
							}
						}
						databaseReadProgress = 90;
						
						// Templates
						if (!DatabaseManager.areEqualTemplates(databaseAdapter.getAllTemplates(),
								restoreManager.getAllTemplates()))
						{
							if (autoRestoreManager.isAutomaticRestore())
							{
								databaseAdapter.deleteAllTemplates();
								databaseAdapter.addAllTemplates(restoreManager.getAllTemplates());
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Templates. Data Recovered");
								databaseMessage.sendToTarget();
							}
							else
							{
								Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
										"Error Found In Templates. Data Not Recovered");
								databaseMessage.sendToTarget();
							}
						}
						databaseReadProgress = 100;
					}
					else
					{
						Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_TOAST_MESSAGE,
								"Error In Automatic Restore\n" + "Error Code: " + restoreResult);
						databaseMessage.sendToTarget();
					}
				}
			}
			Message databaseMessage = databaseHandler.obtainMessage(DatabaseManager.ACTION_INITIALIZATION_COMPLETE);
			databaseMessage.sendToTarget();
			Looper.loop();
			Looper.myLooper().quit();
		}
	}
}
