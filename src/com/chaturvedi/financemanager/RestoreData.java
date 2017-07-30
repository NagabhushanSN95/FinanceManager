package com.chaturvedi.financemanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.Counters;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Date;
import com.chaturvedi.financemanager.database.Time;
import com.chaturvedi.financemanager.database.Transaction;

public class RestoreData
{
private Context context;
	
	public RestoreData(Context cxt)
	{
		context = cxt;
		
		String backupFolderName = "Finance Manager/Backups";
		
		File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!expenditureFolder.exists())
			expenditureFolder.mkdirs();
		
		String extension = ".snb";
		String keyDataFileName = "Key Data";
		String transactionsFileName = "Transactions";
		String banksFileName = "Banks";
		String countersFileName = "Counters";
		String expTypesFileName = "Expenditure Types";

		File keyDataFile = new File(expenditureFolder, keyDataFileName+extension);
		File transactionsFile = new File(expenditureFolder, transactionsFileName+extension);
		File banksFile = new File(expenditureFolder, banksFileName+extension);
		File countersFile = new File(expenditureFolder, countersFileName+extension);
		File expTypesFile = new File(expenditureFolder, expTypesFileName+extension);
		
		try
		{
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			BufferedReader transactionsReader = new BufferedReader(new FileReader(transactionsFile));
			BufferedReader banksReader = new BufferedReader(new FileReader(banksFile));
			BufferedReader countersReader = new BufferedReader(new FileReader(countersFile));
			BufferedReader expTypesReader = new BufferedReader(new FileReader(expTypesFile));
			
			// Read The KEY DATA
			int versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			if(Integer.parseInt(keyDataReader.readLine()) != versionNo)
			{
				Toast.makeText(context, "Old Data. Cannot be Restored. Sorry!", Toast.LENGTH_LONG).show();
				keyDataReader.close();
				transactionsReader.close();
				banksReader.close();
				countersReader.close();
				expTypesReader.close();
				return;
			}
			int numTransactions = Integer.parseInt(keyDataReader.readLine());
			int numBanks = Integer.parseInt(keyDataReader.readLine());
			int numCountersRows = Integer.parseInt(keyDataReader.readLine());
			DatabaseManager.setNumTransactions(numTransactions);
			DatabaseManager.setNumBanks(numBanks);
			DatabaseManager.setNumCountersRows(numCountersRows);
			keyDataReader.close();
			
			// Read The Transactions
			ArrayList<Transaction> transactions = new ArrayList<Transaction>();
			for(int i=0; i<numTransactions; i++)
			{
				Time createdTime = new Time(transactionsReader.readLine());
				Time modifiedTime = new Time(transactionsReader.readLine());
				Date date = new Date(transactionsReader.readLine());
				String type = transactionsReader.readLine();
				String particular = transactionsReader.readLine();
				double rate = Double.parseDouble(transactionsReader.readLine());
				double quantity = Double.parseDouble(transactionsReader.readLine());
				double amount = Double.parseDouble(transactionsReader.readLine());
				transactionsReader.readLine();
				Transaction transaction = new Transaction(i, createdTime, modifiedTime, date, type, particular, rate, quantity, amount);
				transactions.add(transaction);
			}
			DatabaseManager.setAllTransactions(transactions);
			transactionsReader.close();
			
			// Read Banks Data
			ArrayList<Bank> banks = new ArrayList<Bank>();
			for(int i=0; i<numBanks; i++)
			{
				String bankName = banksReader.readLine();
				String accNo = banksReader.readLine();
				double balance = Double.parseDouble(banksReader.readLine());
				String smsName = banksReader.readLine();
				banksReader.readLine();
				Bank bank = new Bank(i, bankName, accNo, balance, smsName);
				banks.add(bank);
			}
			DatabaseManager.setAllBanks(banks);
			banksReader.close();
			
			// Read Counters
			ArrayList<Counters> counters = new ArrayList<Counters>();
			for(int i=0; i<numCountersRows; i++)
			{
				Date date = new Date(countersReader.readLine());
				double exp01 = Double.parseDouble(countersReader.readLine());
				double exp02 = Double.parseDouble(countersReader.readLine());
				double exp03 = Double.parseDouble(countersReader.readLine());
				double exp04 = Double.parseDouble(countersReader.readLine());
				double exp05 = Double.parseDouble(countersReader.readLine());
				double amountSpent = Double.parseDouble(countersReader.readLine());
				double income = Double.parseDouble(countersReader.readLine());
				double savings = Double.parseDouble(countersReader.readLine());
				double withdrawal = Double.parseDouble(countersReader.readLine());
				countersReader.readLine();
				Counters counter = new Counters(i, date, exp01, exp02, exp03, exp04, exp05, amountSpent, income, savings, withdrawal);
				counters.add(counter);
			}
			DatabaseManager.setAllCounters(counters);
			countersReader.close();
			
			// Read Expenditure Types
			ArrayList<String> expTypes = new ArrayList<String>();
			int NUM_EXP_TYPES = 5;
			for(int i=0; i<NUM_EXP_TYPES ; i++)
			{
				expTypes.add(expTypesReader.readLine());
			}
			DatabaseManager.setAllExpenditureTypes(expTypes);
			expTypesReader.close();
			
			DatabaseManager.saveDatabase();
			Toast.makeText(context, "Data Has Been Restored Succesfully", Toast.LENGTH_LONG).show();
		}
		catch(IOException e)
		{
			Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (NameNotFoundException e)
		{
			Toast.makeText(context, "Error in retrieving Version No\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
