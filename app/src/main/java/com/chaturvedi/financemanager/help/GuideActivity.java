package com.chaturvedi.financemanager.help;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NavUtils;

import com.chaturvedi.financemanager.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GuideActivity extends Activity {
	private ArrayList<TextView> textViews;
	private ArrayList<String> helpTexts;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_guide);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		findViews();
		readHelpFile();
		setHelpText();
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(GuideActivity.this);
				return true;
		}
		return true;
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
		InputStream textStream = getResources().openRawResource(R.raw.guide);
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
