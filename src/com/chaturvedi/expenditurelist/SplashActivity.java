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
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
	private ArrayList<String> quoteStrings;
	private String quoteText;
	private View progressLine;
	private LinearLayout.LayoutParams lp;
	private int deviceWidth=1000;
	private int progressStatus=00;
	private Random randomNumber;
	
	private Intent nextActivityIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_splash);
		}
		else
		{
			setContentView(R.layout.activity_splash);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		readPreferences();
		startSplash();
		//new DatabaseManager(this);
	}
	
	private void startSplash()
	{
		if(showSplash)
		{
			quoteView=(TextView)findViewById(R.id.quote);
			quoteStrings=new ArrayList<String>();
			InputStream quoteStream = getResources().openRawResource(R.raw.splash_screen_quotes);
			BufferedReader quotesReader = new BufferedReader(new InputStreamReader(quoteStream));
			randomNumber=new Random();
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
			quoteText=quoteStrings.get(randomNumber.nextInt(quoteStrings.size()));
			quoteView.setText(quoteText);
			
			new Handler().postDelayed(new Runnable() 
			{
				@Override
				public void run()
				{
					startActivity(nextActivityIntent);
					finish();
				}
			} ,splashTime);
			
			progressLine=(View)findViewById(R.id.progress_line);
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			
			deviceWidth=metrics.widthPixels;
			lp=new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
			progressLine.setLayoutParams(lp);
			Timer timer=new Timer();
			Refresh refresher= new Refresh();
			int refreshTime=5000/deviceWidth+1;
			timer.scheduleAtFixedRate(refresher, 0, refreshTime);
		}
		else
		{
			startActivity(nextActivityIntent);
			finish();
		}
			
	}
	
	public class Refresh extends TimerTask
	{
		@Override
		public void run() 
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					progressStatus++;
					lp=new LinearLayout.LayoutParams(progressStatus, LayoutParams.MATCH_PARENT);
					progressLine.setLayoutParams(lp);
				}
			});
		}
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_ENABLE_SPLASH))
		{
			showSplash=preferences.getBoolean(KEY_ENABLE_SPLASH, true);
			//nextActivityIntent = new Intent(this, SummaryActivity.class);
		}
		else
		{
			showSplash = true;
			//nextActivityIntent = new Intent(this, BanksSetupActivity.class);
		}
		
		preferences = getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0);
		if(preferences.contains(KEY_DATABASE_INITIALIZED))
		{
			nextActivityIntent = new Intent(this, SummaryActivity.class);
		}
		else
		{
			nextActivityIntent = new Intent(this, BanksSetupActivity.class);
		}
	}
	
	@Override
	public void onBackPressed()
	{
		
	}
}
