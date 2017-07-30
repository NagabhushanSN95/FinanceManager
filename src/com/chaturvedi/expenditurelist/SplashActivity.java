// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity 
{
	private static boolean showSplash=true;
	private static int splashTime=5000;
	
	private TextView quoteView;
	private ArrayList<String> quoteStrings;
	private String quoteText;
	private View progressLine;
	private LinearLayout.LayoutParams lp;
	private int deviceWidth=1000;
	private int progressStatus=00;
	
	private InputStream quoteStream;
	private InputStreamReader quoteReader;
	private BufferedReader reader;
	private Random randomNumber;
	
	private Intent nextActivityIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		readFile();
		startSplash();
		
		
	}
	
	private void startSplash()
	{
		quoteView=(TextView)findViewById(R.id.quote);
		quoteStrings=new ArrayList<String>();
		quoteStream=getResources().openRawResource(R.raw.splash_screen_quotes);
		quoteReader=new InputStreamReader(quoteStream);
		reader=new BufferedReader(quoteReader);
		randomNumber=new Random();
		try
		{
			String line=reader.readLine();
			while(line!=null)
			{
				quoteStrings.add(line);
				line=reader.readLine();
			}
		}
		catch(Exception e)
		{
			
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
	
	private void readFile()
	{
		try
		{
			String expenditureFolderName = "Expenditure List/.temp";
			String settingsFileName="settings.txt";
			String prefFileName = "preferences.txt";
			
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			File settingsFile=new File(expenditureFolder, settingsFileName);
			BufferedReader settingsReader=new BufferedReader(new FileReader(settingsFile));
			String line=settingsReader.readLine();
			if(line.contains("false"))
			{
				showSplash=false;
				splashTime=0;
			}
			else
			{
				showSplash=true;
				splashTime=5000;
			}
			settingsReader.close();
			
			File prefFile = new File(expenditureFolder, prefFileName);
			if(prefFile.exists())
				nextActivityIntent=new Intent(this, MainActivity.class);
			else
				nextActivityIntent=new Intent(this, StartupActivity.class);
		}
		catch(Exception e)
		{
			showSplash=true;
			Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
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
	
	@Override
	public void onBackPressed()
	{
		
	}
}
