package com.chaturvedi.expenditurelist;

import java.text.DecimalFormat;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class StatisticsActivity extends Activity
{
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_NAMES;
	private int WIDTH_AMOUNTS;
	private int MARGIN_LEFT_PARENT_LAYOUT;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_statistics);
		}
		else
		{
			setContentView(R.layout.activity_statistics);
			RelativeLayout actionBar = (RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		WIDTH_NAMES=screenWidth*50/100;
		WIDTH_AMOUNTS=screenWidth*30/100;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*10/100;
		
		buildLayout();
	}
	
	private void buildLayout()
	{
		DecimalFormat formatter = new DecimalFormat("#,##0");
		
		LinearLayout parentLayout = (LinearLayout)findViewById(R.id.layout_parent);
		LayoutParams parentLayoutParams = (LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, 0, MARGIN_LEFT_PARENT_LAYOUT, 0);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		LayoutInflater monthLayoutInflater = LayoutInflater.from(this);
		LinearLayout monthHeadingLayout = (LinearLayout) monthLayoutInflater.inflate(R.layout.layout_title_statistics, null);
		TextView monthHeading = (TextView)monthHeadingLayout.findViewById(R.id.heading);
		monthHeading.setText("Current Month Statistics");
		
		TextView titleView = (TextView)monthHeadingLayout.findViewById(R.id.title_expenditureType);
		titleView.setLayoutParams(new LayoutParams(WIDTH_NAMES, LayoutParams.WRAP_CONTENT));
		titleView = (TextView)monthHeadingLayout.findViewById(R.id.title_amountSpent);
		titleView.setLayoutParams(new LayoutParams(WIDTH_AMOUNTS, LayoutParams.WRAP_CONTENT));
		parentLayout.addView(monthHeadingLayout);
		
		for(int i=0; i<5; i++)
		{
			LayoutInflater statDisplayInflater = LayoutInflater.from(this);
			LinearLayout statDisplayLayout = (LinearLayout) statDisplayInflater.inflate(R.layout.layout_display_statistics, null);
			/*LayoutParams layoutParams = (LayoutParams) statDisplayLayout.getLayoutParams();
			layoutParams.setMargins(MARGIN_LEFT_STAT_DISPLAY_LAYOUTS, 0, 0, 0);
			statDisplayLayout.setLayoutParams(layoutParams);*/
			
			TextView expenditureTypeName = (TextView)statDisplayLayout.findViewById(R.id.expendtureTypeName);
			expenditureTypeName.setText(DatabaseManager.getExpenditureTypes().get(i));
			expenditureTypeName.setLayoutParams(new LayoutParams(WIDTH_NAMES, LayoutParams.WRAP_CONTENT));
			
			TextView expenditureTypeAmount = (TextView)statDisplayLayout.findViewById(R.id.amount);
			expenditureTypeAmount.setText(formatter.format(DatabaseManager.getCounter(i)));
			expenditureTypeAmount.setLayoutParams(new LayoutParams(WIDTH_AMOUNTS, LayoutParams.WRAP_CONTENT));
			
			parentLayout.addView(statDisplayLayout);
		}
		
		LayoutInflater overallLayoutInflater = LayoutInflater.from(this);
		LinearLayout overallHeadingLayout = (LinearLayout) overallLayoutInflater.inflate(R.layout.layout_title_statistics, null);
		TextView overallHeading = (TextView)overallHeadingLayout.findViewById(R.id.heading);
		overallHeading.setText("Overall Statistics");
		
		titleView = (TextView)overallHeadingLayout.findViewById(R.id.title_expenditureType);
		titleView.setLayoutParams(new LayoutParams(WIDTH_NAMES, LayoutParams.WRAP_CONTENT));
		titleView = (TextView)overallHeadingLayout.findViewById(R.id.title_amountSpent);
		titleView.setLayoutParams(new LayoutParams(WIDTH_AMOUNTS, LayoutParams.WRAP_CONTENT));
		parentLayout.addView(overallHeadingLayout);
		
		for(int i=0; i<5; i++)
		{
			LayoutInflater statDisplayInflater = LayoutInflater.from(this);
			LinearLayout statDisplayLayout = (LinearLayout) statDisplayInflater.inflate(R.layout.layout_display_statistics, null);
			/*LayoutParams layoutParams = (LayoutParams) statDisplayLayout.getLayoutParams();
			layoutParams.setMargins(MARGIN_LEFT_STAT_DISPLAY_LAYOUTS, 0, 0, 0);
			statDisplayLayout.setLayoutParams(layoutParams);*/
			
			TextView expenditureTypeName = (TextView)statDisplayLayout.findViewById(R.id.expendtureTypeName);
			expenditureTypeName.setText(DatabaseManager.getExpenditureTypes().get(i));
			expenditureTypeName.setLayoutParams(new LayoutParams(WIDTH_NAMES, LayoutParams.WRAP_CONTENT));
			
			TextView expenditureTypeAmount = (TextView)statDisplayLayout.findViewById(R.id.amount);
			expenditureTypeAmount.setText(formatter.format(DatabaseManager.getCounter(i+5)));
			expenditureTypeAmount.setLayoutParams(new LayoutParams(WIDTH_AMOUNTS, LayoutParams.WRAP_CONTENT));
			
			parentLayout.addView(statDisplayLayout);
		}
	}
}