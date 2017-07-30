package com.chaturvedi.financemanager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FAQSummaryActivity extends Activity
{
	private ArrayList<String> FAQQuestions;
	private Intent FAQAnswerIntent;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_faq_summary);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		readFAQs();
		buildLayout();
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(FAQSummaryActivity.this);
				return true;
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
		
		final LinearLayout parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		for(int i=0; i<FAQQuestions.size(); i++)
		{
			LayoutInflater inflater = LayoutInflater.from(this);
			final LinearLayout FAQLayout = (LinearLayout) inflater.inflate(R.layout.layout_faq_question, null);
			((TextView)FAQLayout.findViewById(R.id.textView_FAQQuestion)).setText(FAQQuestions.get(i));
			FAQLayout.setOnClickListener(new View.OnClickListener()
			{
				
				@Override
				public void onClick(View v)
				{
					FAQAnswerIntent = new Intent(FAQSummaryActivity.this,FAQAnswerActivity.class);
					FAQAnswerIntent.putExtra("FAQ No", parentLayout.indexOfChild(FAQLayout)-1);	// Because, FAQ TextView is the first child
					startActivity(FAQAnswerIntent);
				}
			});
			parentLayout.addView(FAQLayout);
		}
		
		
	}

	private void readFAQs()
	{
		FAQQuestions=new ArrayList<String>();
		InputStream textStream = getResources().openRawResource(R.raw.faq);
		BufferedReader FAQReader = new BufferedReader(new InputStreamReader(textStream));
		try
		{
			String line=FAQReader.readLine();
			while(line!=null)
			{
				FAQQuestions.add(line);
				line=FAQReader.readLine();
				line=FAQReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
