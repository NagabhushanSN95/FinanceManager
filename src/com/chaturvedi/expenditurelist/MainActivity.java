// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private static int walletBalance;
	private static int amountSpent;
	private static int numBanks;
	private static ArrayList<String> bankNames;
	private static ArrayList<Integer> bankBalances;

	private static String expenditureFolderName;
	private static String walletFileName;
	private static String bankFileName;
	private static File expenditureFolder;
	private static File walletFile;
	private static File bankFile;
	private static BufferedReader walletReader;
	private static BufferedReader bankReader;
	
	private static TextView walletBalanceView;
	private static TextView amountSpentView;
	private static TextView bank01View;
	private static TextView bank02View;
	private static TextView bank03View;
	private static TextView balanceBank01View;
	private static TextView balanceBank02View;
	private static TextView balanceBank03View;
	
	private Intent detailsIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		readData();
		setData();
		
		detailsIntent=new Intent(this, DetailsActivity.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.action_details:
				startActivity(detailsIntent);
				return true;
				
				
		}
		return true;
	}
	
	private void readData()
	{
		String line;
		try
		{
			expenditureFolderName="Expenditure List/.temp";
			walletFileName="wallet_info.txt";
			bankFileName="bank_info.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			walletFile=new File(expenditureFolder, walletFileName);
			bankFile=new File(expenditureFolder, bankFileName);
			
			walletReader=new BufferedReader(new FileReader(walletFile));
			line=walletReader.readLine();
			walletBalance=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			line=walletReader.readLine();
			amountSpent=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			
			bankReader=new BufferedReader(new FileReader(bankFile));
			numBanks=2;
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
	
	private void setData()
	{
		try
		{
			walletBalanceView=(TextView)findViewById(R.id.balance_wallet);
			amountSpentView=(TextView)findViewById(R.id.amount_spent);
			walletBalanceView.setText("Rs "+walletBalance);
			amountSpentView.setText("Rs "+amountSpent);

			bank01View=(TextView)findViewById(R.id.bank_01);
			bank02View=(TextView)findViewById(R.id.bank_02);
			bank03View=(TextView)findViewById(R.id.bank_03);
			bank03View.setVisibility(View.GONE);
			balanceBank01View=(TextView)findViewById(R.id.balance_bank_01);
			balanceBank02View=(TextView)findViewById(R.id.balance_bank_02);
			balanceBank03View=(TextView)findViewById(R.id.balance_bank_03);
			balanceBank03View.setVisibility(View.GONE);
			
			bank01View.setText(bankNames.get(0));
			bank02View.setText(bankNames.get(1));
			balanceBank01View.setText("Rs "+bankBalances.get(0));
			balanceBank02View.setText("Rs "+bankBalances.get(1));
		}
		catch(Exception e)
		{
			
		}
		
	}

}
