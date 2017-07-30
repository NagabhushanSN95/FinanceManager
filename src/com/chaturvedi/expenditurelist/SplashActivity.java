// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

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
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
//import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity 
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_ENABLE_SPLASH = "enable_splash";
	private static final String SHARED_PREFERENCES_DATABASE = "DatabaseInitialized";
	private static final String KEY_DATABASE_INITIALIZED = "database_initialized";
	
	private static boolean showSplash=true;
	private static int splashTime=5000;
	
	private TextView quoteView;
	private String quoteText;
	private View progressLine;
	private LayoutParams progressLineParams;
	private int deviceWidth=1000;
	private int progressStatus=00;
	
	private Intent nextActivityIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		readPreferences();
		// If Splash Screen is enabled by the user, read the quotes and start the Splash Screen
		// Else, start the NextActivity
		if(showSplash)
		{
			readQuotes();
			startSplash();
		}
		else
		{
			startActivity(nextActivityIntent);
			finish();
		}
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
			// Since Database Is Initialized, Start The SummaryActivity (Home Screen Of The App)
			nextActivityIntent = new Intent(this, SummaryActivity.class);
		}
		else
		{
			// Since Database is not Initialized, Start the Setup
			nextActivityIntent = new Intent(this, BanksSetupActivity.class);
		}
	}
	
	/**
	 * Reads The Quotes from the "quotes.txt" raw file to display in the Splash Screen
	 */
	private void readQuotes()
	{
		ArrayList<String> quoteStrings=new ArrayList<String>();
		InputStream quoteStream = getResources().openRawResource(R.raw.quotes);
		BufferedReader quotesReader = new BufferedReader(new InputStreamReader(quoteStream));
		Random randomNumber=new Random();
		try
		{
			String line=quotesReader.readLine();
			while(line!=null)
			{
				quoteStrings.add(line);
				line=quotesReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
		// Select A Random Quote
		quoteText=quoteStrings.get(randomNumber.nextInt(quoteStrings.size()));
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
		new Handler().postDelayed(new Runnable() 
		{
			@Override
			public void run()
			{
				startActivity(nextActivityIntent);
				finish();
			}
		} ,splashTime);
		
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
}
