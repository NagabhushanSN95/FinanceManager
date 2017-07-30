package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DetailsActivity extends Activity
{
	private final int MARGIN_TOP=100;
	private final int MARGIN_LEFT=50;
	private final int MARGIN_LEFT_SLNO=10;
	private final int MARGIN_LEFT_PARTICULARS=70;
	private final int MARGIN_LEFT_AMOUNT=150;
	
	private RelativeLayout titleLayout;
	private ArrayList<RelativeLayout> itemsLayout;
	
	private TextView slnoTitleView;
	private TextView particularsTitleView;
	private TextView amountTitleView;
	private ArrayList<ArrayList<TextView>> itemsView;
	
	private RelativeLayout.LayoutParams titleLayoutParams;
	private RelativeLayout.LayoutParams slnoTitleParams;
	private RelativeLayout.LayoutParams particularsTitleParams;
	private RelativeLayout.LayoutParams amountTitleParams;
	private ArrayList<ArrayList<RelativeLayout.LayoutParams>> itemsLayoutParams;
	
	private String expenditureFolderName;
	private String particularsFileName;
	private String amountFileName;
	private File expenditureFolder;
	private File particularsFile;
	private File amountFile;
	private BufferedReader particularsReader;
	private BufferedReader amountReader;
	
	private int numEntries;
	private ArrayList<String> particulars;
	private ArrayList<String> amounts;
	
	private Intent detailsIntent;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_details);
		detailsIntent=getIntent();
		numEntries=detailsIntent.getIntExtra("Number Of Entries", 0);
		readFile();
		buildTitleLayout();
		buildBodyLayout();
		
	}
	
	private void readFile()
	{
		try
		{
			expenditureFolderName="Expenditure List/.temp";
			particularsFileName="particulars.txt";
			amountFileName="amount.txt";
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			particularsFile=new File(expenditureFolder, particularsFileName);
			amountFile=new File(expenditureFolder, amountFileName);
			particularsReader=new BufferedReader(new FileReader(particularsFile));
			amountReader=new BufferedReader(new FileReader(amountFile));
			particulars=new ArrayList<String>();
			amounts=new ArrayList<String>();
			for(int i=0; i<numEntries; i++)
			{
				particulars.add(particularsReader.readLine());
				amounts.add(amountReader.readLine());
			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void buildTitleLayout()
	{
		titleLayout=new RelativeLayout(this);
		titleLayoutParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		titleLayoutParams.topMargin=200;
		titleLayoutParams.leftMargin=200;
		titleLayoutParams.setMargins(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, MARGIN_TOP);
		titleLayout.setLayoutParams(titleLayoutParams);
		
		slnoTitleView=new TextView(this);
		slnoTitleView.setText("Sl No");
		slnoTitleParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		slnoTitleParams.setMargins(MARGIN_LEFT_SLNO, MARGIN_TOP, 30, 30);
		
		particularsTitleView=new TextView(this);
		particularsTitleView.setText("Particulars");
		particularsTitleParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		particularsTitleParams.setMargins(MARGIN_LEFT_PARTICULARS, MARGIN_TOP, 30, 30);
		
		amountTitleView=new TextView(this);
		amountTitleView.setText("Amount");
		amountTitleParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		amountTitleParams.setMargins(MARGIN_LEFT_AMOUNT, MARGIN_TOP, 30, 30);
		
		titleLayout.addView(slnoTitleView, slnoTitleParams);
		titleLayout.addView(particularsTitleView, particularsTitleParams);
		titleLayout.addView(amountTitleView, amountTitleParams);
		this.addContentView(titleLayout, titleLayoutParams);
	}
	
	private void buildBodyLayout()
	{
		try
		{
			itemsLayout=new ArrayList<RelativeLayout>();
			itemsView=new ArrayList<ArrayList<TextView>>();
			itemsLayoutParams=new ArrayList<ArrayList<RelativeLayout.LayoutParams>>();
			for(int i=0; i<numEntries; i++)
			{
				itemsLayout.add(new RelativeLayout(this));
				
				itemsView.add(new ArrayList<TextView>());
				itemsView.get(i).add(new TextView(this));
				itemsView.get(i).add(new TextView(this));
				itemsView.get(i).add(new TextView(this));
				itemsView.get(i).get(0).setText("0"+(i+1));
				itemsView.get(i).get(1).setText(particulars.get(i));
				itemsView.get(i).get(2).setText(amounts.get(i));
				
				itemsLayoutParams.add(new ArrayList<RelativeLayout.LayoutParams>());
				itemsLayoutParams.get(i).add(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
				itemsLayoutParams.get(i).add(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
				itemsLayoutParams.get(i).add(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
				itemsLayoutParams.get(i).get(0).setMargins(MARGIN_LEFT_SLNO, MARGIN_TOP+20+(20*i), 30, 30);
				itemsLayoutParams.get(i).get(1).setMargins(MARGIN_LEFT_PARTICULARS, MARGIN_TOP+20+(20*i), 30, 30);
				itemsLayoutParams.get(i).get(2).setMargins(MARGIN_LEFT_AMOUNT, MARGIN_TOP+20+(20*i), 30, 30);
				
				itemsLayout.get(i).addView(itemsView.get(i).get(0), itemsLayoutParams.get(i).get(0));
				itemsLayout.get(i).addView(itemsView.get(i).get(1), itemsLayoutParams.get(i).get(1));
				itemsLayout.get(i).addView(itemsView.get(i).get(2), itemsLayoutParams.get(i).get(2));
				this.addContentView(itemsLayout.get(i), titleLayoutParams);
			}
		}
		catch(Exception e)
		{
			
		}
		
	}
}
