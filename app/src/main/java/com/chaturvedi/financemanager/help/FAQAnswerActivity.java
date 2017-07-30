package com.chaturvedi.financemanager.help;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;

public class FAQAnswerActivity extends Activity
{
	private int FAQNo;
	private ArrayList<String> FAQQuestions;
	private ArrayList<String> FAQAnswers;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_faq_answer);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		FAQNo = getIntent().getIntExtra("FAQ No", 1);
		readFAQs();
		buildLayout();
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(FAQAnswerActivity.this);
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

		((TextView)findViewById(R.id.textView_FAQQuestion)).setText(FAQQuestions.get(FAQNo));
		((TextView)findViewById(R.id.textView_FAQAnswer)).setText(FAQAnswers.get(FAQNo));
	}

	private void readFAQs()
	{
		FAQQuestions=new ArrayList<String>();
		FAQAnswers=new ArrayList<String>();
		InputStream textStream = getResources().openRawResource(R.raw.faq);
		BufferedReader FAQReader = new BufferedReader(new InputStreamReader(textStream));
		try
		{
			String line=FAQReader.readLine();
			while(line!=null)
			{
				FAQQuestions.add(line);
				FAQAnswers.add(FAQReader.readLine());
				line=FAQReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
