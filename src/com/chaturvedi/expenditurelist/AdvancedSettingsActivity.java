package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class AdvancedSettingsActivity extends Activity
{
	private int walletBalance;
	private int amountSpent;
	private int numBanks;
	private ArrayList<String> bankNames;
	private ArrayList<Integer> bankBalances;

	private LinearLayout parentLayout;
	private ArrayList<LinearLayout> linearLayouts;
	private ArrayList<LayoutParams> linearLayoutParams;
	private ArrayList<TextView> textViews;
	private ArrayList<LayoutParams> textViewParams;
	private ArrayList<EditText> amountFields;
	private ArrayList<LayoutParams> amountFieldParams;
	private ImageButton saveButton;
	
	private String expenditureFolderName;
	private String prefFileName;
	private String walletFileName;
	private String bankFileName;
	private File expenditureFolder;
	private File prefFile;
	private File walletFile;
	private File bankFile;
	private BufferedReader prefReader;
	private BufferedReader walletReader;
	private BufferedReader bankReader;
	private BufferedWriter walletWriter;
	private BufferedWriter bankWriter;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_advanced_settings);
		readFile();
		
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		linearLayouts=new ArrayList<LinearLayout>(numBanks+1);
		linearLayoutParams=new ArrayList<LayoutParams>(numBanks+1);
		textViews=new ArrayList<TextView>(numBanks+1);
		textViewParams=new ArrayList<LayoutParams>(numBanks+1);
		amountFields=new ArrayList<EditText>(numBanks+1);
		amountFieldParams=new ArrayList<LayoutParams>(numBanks+1);
		for(int i=0; i<numBanks+1; i++)
		{
			linearLayouts.add(new LinearLayout(this));
			linearLayoutParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			textViews.add(new TextView(this));
			textViewParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			amountFields.add(new EditText(this));
			amountFieldParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			
			linearLayouts.get(i).setOrientation(LinearLayout.HORIZONTAL);
			linearLayouts.get(i).addView(textViews.get(i), textViewParams.get(i));
			linearLayouts.get(i).addView(amountFields.get(i), amountFieldParams.get(i));
			parentLayout.addView(linearLayouts.get(i), linearLayoutParams.get(i));
		}
		
		for(int i=0; i<numBanks; i++)
		{
			textViews.get(i).setText(bankNames.get(i)+"\tRs");
			amountFields.get(i).setText(""+bankBalances.get(i));
		}
		textViews.get(numBanks).setText("Wallet Balance\tRs");
		amountFields.get(numBanks).setText(""+walletBalance);
		
		saveButton=(ImageButton)findViewById(R.id.button_save);
		saveButton.setOnClickListener(new SaveListener());
	}

	private void readFile()
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
			walletReader=new BufferedReader(new FileReader(walletFile));
			bankReader=new BufferedReader(new FileReader(bankFile));
			
			numBanks=Integer.parseInt(prefReader.readLine());
			line=walletReader.readLine();
			walletBalance=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			line=walletReader.readLine();
			amountSpent=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			
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
			Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void saveData()
	{
		try
		{
			for(int i=0; i<numBanks; i++)
			{
				bankBalances.set(i, Integer.parseInt(amountFields.get(i).getText().toString()));
			}
			walletBalance=Integer.parseInt(amountFields.get(numBanks).getText().toString());
			
			
			expenditureFolderName="Expenditure List/.temp";
			walletFileName="wallet_info.txt";
			bankFileName="bank_info.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			walletFile=new File(expenditureFolder, walletFileName);
			bankFile=new File(expenditureFolder, bankFileName);
			
			walletWriter=new BufferedWriter(new FileWriter(walletFile));
			bankWriter=new BufferedWriter(new FileWriter(bankFile));
			
			walletWriter.write("wallet_balance=Rs"+walletBalance+"\n");
			walletWriter.write("amount_spent=Rs"+amountSpent+"\n");
			for(int i=0; i<numBanks; i++)
			{
				bankWriter.write(bankNames.get(i)+"=Rs"+bankBalances.get(i)+"\n");
			}
			walletWriter.close();
			bankWriter.close();
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Saving To File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private class SaveListener implements OnClickListener
	{

		@Override
		public void onClick(View v)
		{
			saveData();
			onBackPressed();
		}
		
	}
}
