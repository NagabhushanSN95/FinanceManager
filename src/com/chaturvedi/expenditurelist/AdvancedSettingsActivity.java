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
import android.widget.TextView;
import android.widget.Toast;

public class AdvancedSettingsActivity extends Activity
{
	private int walletBalance;
	private int amountSpent;
	private int numBanks;
	private ArrayList<String> bankNames;
	private ArrayList<Integer> bankBalances;

	private ArrayList<TextView> banksView;
	private TextView walletView;
	private ArrayList<EditText> bankBalanceFields;
	private EditText walletField;
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
		
		banksView=new ArrayList<TextView>();
		bankBalanceFields=new ArrayList<EditText>();
		banksView.add((TextView)findViewById(R.id.bank01_view));
		banksView.add((TextView)findViewById(R.id.bank02_view));
		bankBalanceFields.add((EditText)findViewById(R.id.edit_balance_bank01));
		bankBalanceFields.add((EditText)findViewById(R.id.edit_balance_bank02));
		walletView=(TextView)findViewById(R.id.wallet);
		walletField=(EditText)findViewById(R.id.balance_wallet);
		
		walletView.setText("Wallet Balance\tRs");
		walletField.setText(""+walletBalance);
		for(int i=0; i<numBanks; i++)
		{
			banksView.get(i).setText(bankNames.get(i)+"\tRs");
			bankBalanceFields.get(i).setText(""+bankBalances.get(i));
		}
		
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
				bankBalances.set(i, Integer.parseInt(bankBalanceFields.get(i).getText().toString()));
			}
			walletBalance=Integer.parseInt(walletField.getText().toString());
			
			
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
