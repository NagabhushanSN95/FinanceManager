// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SummaryActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int WIDTH_TEXT_VIEWS; 
	
	private static int walletBalance;
	private static int amountSpent;
	private static int income;
	private static int numBanks;
	private static ArrayList<String> bankNames;
	private static ArrayList<Integer> bankBalances;
	private static int numEntries;

	private static String expenditureFolderName;
	private static String prefFileName;
	private static String walletFileName;
	private static String bankFileName;
	private static File expenditureFolder;
	private static File prefFile;
	private static File walletFile;
	private static File bankFile;
	private static BufferedReader prefReader;
	private static BufferedReader walletReader;
	private static BufferedReader bankReader;
	
	private static LinearLayout parentLayout;
	private static LayoutParams parentLayoutParams;
	private static ArrayList<LinearLayout> layouts;
	private static ArrayList<LinearLayout.LayoutParams> layoutParams;
	private static ArrayList<TextView> nameViews;
	private static ArrayList<LinearLayout.LayoutParams> nameViewParams;
	private static ArrayList<TextView> amountViews;
	private static ArrayList<LinearLayout.LayoutParams> amountViewParams;
	
	private Intent detailsIntent;
	private Intent editIntent;
	private Intent exportIntent;
	private Intent settingsIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_summary);
		}
		else
		{
			setContentView(R.layout.activity_summary);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		readData();
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=(screenHeight-(numBanks*100))/6;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*15/100;
		WIDTH_TEXT_VIEWS=screenWidth*40/100;
		
		buildLayout();
		setData();
		
		detailsIntent=new Intent(this, DetailsActivity.class);
		detailsIntent.putExtra("Number Of Entries", numEntries);
		detailsIntent.putExtra("Number Of Banks", numBanks);
		editIntent=new Intent(this, EditActivity.class);
		exportIntent=new Intent(this, ExportActivity.class);
		settingsIntent=new Intent(this, SettingsActivity.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_summary, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.action_details:
				startActivityForResult(detailsIntent, 0);
				return true;
				
			case R.id.action_edit:
				startActivityForResult(editIntent, 0);
				return true;
				
			case R.id.action_settings:
				startActivity(settingsIntent);
				return true;
				
			case R.id.action_export:
				startActivityForResult(exportIntent, 0);
				refresh();
				return true;
		}
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		refresh();
	}
	
	private void refresh()
	{
		readData();
		setData();
	}
	
	private void readData()
	{
		String line;
		try
		{
			expenditureFolderName="Expenditure List/.temp";
			prefFileName="preferences.txt";
			walletFileName="wallet_info.txt";
			bankFileName="bank_info.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			prefFile=new File(expenditureFolder, prefFileName);
			walletFile=new File(expenditureFolder, walletFileName);
			bankFile=new File(expenditureFolder, bankFileName);
			
			prefReader=new BufferedReader(new FileReader(prefFile));
			numBanks=Integer.parseInt(prefReader.readLine());
			numEntries=Integer.parseInt(prefReader.readLine());
			
			walletReader=new BufferedReader(new FileReader(walletFile));
			line=walletReader.readLine();
			walletBalance=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			line=walletReader.readLine();
			amountSpent=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			line=walletReader.readLine();
			income=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			
			bankReader=new BufferedReader(new FileReader(bankFile));
			bankNames=new ArrayList<String>();
			bankBalances=new ArrayList<Integer>();
			for(int i=0; i<numBanks; i++)
			{
				line=bankReader.readLine();
				bankNames.add(line.substring(0, line.indexOf("=")));
				bankBalances.add(Integer.parseInt(line.substring(line.indexOf("Rs")+2)));
			}
		}
		catch(Exception e)
		{
			if(!(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)))
				Toast.makeText(this, "External Storage Not Available", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void buildLayout()
	{
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, 0, 0);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		layouts=new ArrayList<LinearLayout>(numBanks+3);
		layoutParams=new ArrayList<LayoutParams>(numBanks+3);
		nameViews=new ArrayList<TextView>(numBanks+3);
		nameViewParams=new ArrayList<LayoutParams>(numBanks+3);
		amountViews=new ArrayList<TextView>(numBanks+3);
		amountViewParams=new ArrayList<LayoutParams>(numBanks+3);
		for(int i=0; i<numBanks+3; i++)
		{
			layouts.add(new LinearLayout(this));
			layoutParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			nameViews.add(new TextView(this));
			nameViewParams.add(new LayoutParams(WIDTH_TEXT_VIEWS, LayoutParams.WRAP_CONTENT));
			amountViews.add(new TextView(this));
			amountViewParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			
			layoutParams.get(i).setMargins(10, 10, 10, 10);
			nameViewParams.get(i).setMargins(10,0,0,0);
			amountViewParams.get(i).setMargins(30,0,0,0);

			layouts.get(i).addView(nameViews.get(i), nameViewParams.get(i));
			layouts.get(i).addView(amountViews.get(i), amountViewParams.get(i));
			parentLayout.addView(layouts.get(i), layoutParams.get(i));
		}
	}
	
	private void setData()
	{
		try
		{
			// Set The Data
			for(int i=0; i<numBanks; i++)
			{
				nameViews.get(i).setText(bankNames.get(i));
				amountViews.get(i).setText("Rs "+bankBalances.get(i));
			}
			nameViews.get(numBanks).setText("Wallet");
			nameViews.get(numBanks+1).setText("Amount Spent");
			nameViews.get(numBanks+2).setText("Income");
			amountViews.get(numBanks).setText("Rs "+walletBalance);
			amountViews.get(numBanks+1).setText("Rs "+amountSpent);
			amountViews.get(numBanks+2).setText("Rs "+income);
		}
		catch(Exception e)
		{
			
		}
	}
}