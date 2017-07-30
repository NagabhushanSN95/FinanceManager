package com.chaturvedi.financemanager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.pm.ApplicationInfo;
import android.os.Build.VERSION;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HelpActivity extends Activity
{
	private ArrayList<TextView> textViews;
	private ArrayList<String> helpTexts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_help);
		}
		else
		{
			setContentView(R.layout.activity_help);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		findViews();
		readHelpFile();
		setHelpText();
		
	}
	
	private void findViews()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		textViews = new ArrayList<TextView>();
		textViews.add((TextView) findViewById(R.id.heading_1));
		textViews.add((TextView) findViewById(R.id.description_1));
		textViews.add((TextView) findViewById(R.id.heading_2));
		textViews.add((TextView) findViewById(R.id.description_2));
		textViews.add((TextView) findViewById(R.id.heading_3));
		textViews.add((TextView) findViewById(R.id.description_3));
		textViews.add((TextView) findViewById(R.id.heading_4));
		textViews.add((TextView) findViewById(R.id.description_4));
		textViews.add((TextView) findViewById(R.id.heading_5));
		textViews.add((TextView) findViewById(R.id.description_5));
		textViews.add((TextView) findViewById(R.id.heading_6));
		textViews.add((TextView) findViewById(R.id.description_6));
		textViews.add((TextView) findViewById(R.id.heading_7));
		textViews.add((TextView) findViewById(R.id.description_7));
	}
	
	private void readHelpFile()
	{
		helpTexts=new ArrayList<String>();
		InputStream textStream = getResources().openRawResource(R.raw.help);
		BufferedReader helpReader = new BufferedReader(new InputStreamReader(textStream));
		try
		{
			String line=helpReader.readLine();
			while(line!=null)
			{
				helpTexts.add(line);
				line=helpReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void setHelpText()
	{
		for(int i=0; i<14; i++)
		{
			textViews.get(i).setText(helpTexts.get(i));
		}
	}
	
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		overridePendingTransition(R.anim.old_activity_enter, R.anim.new_activity_leave);
	}
}
