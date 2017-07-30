package com.chaturvedi.financemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.Counters;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Transaction;

public class BackupData
{
	private Context context;
	
	public BackupData(Context cxt)
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
			BufferedWriter keyDataWriter = new BufferedWriter(new FileWriter(keyDataFile));
			BufferedWriter transactionsWriter = new BufferedWriter(new FileWriter(transactionsFile));
			BufferedWriter banksWriter = new BufferedWriter(new FileWriter(banksFile));
			BufferedWriter countersWriter = new BufferedWriter(new FileWriter(countersFile));
			BufferedWriter expTypesWriter = new BufferedWriter(new FileWriter(expTypesFile));
			
			// Store The KEY DATA
			int versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			keyDataWriter.write(versionNo + "\n");
			keyDataWriter.write(DatabaseManager.getNumTransactions() + "\n");
			keyDataWriter.write(DatabaseManager.getNumBanks() + "\n");
			keyDataWriter.write(DatabaseManager.getNumCountersRows() + "\n");
			keyDataWriter.close();
			
			// Backup The Transactions
			ArrayList<Transaction> transactions = DatabaseManager.getAllTransactions();
			for(Transaction transaction : transactions)
			{
				transactionsWriter.write(transaction.getCreatedTime().toString() + "\n");
				transactionsWriter.write(transaction.getModifiedTime().toString() + "\n");
				transactionsWriter.write(transaction.getDate().getSavableDate() + "\n");
				transactionsWriter.write(transaction.getType() + "\n");
				transactionsWriter.write(transaction.getParticular() + "\n");
				transactionsWriter.write(transaction.getRate() + "\n");
				transactionsWriter.write(transaction.getQuantity() + "\n");
				transactionsWriter.write(transaction.getAmount() + "\n");
				transactionsWriter.write("\n");
			}
			transactionsWriter.close();
			
			// Backup Banks Data
			ArrayList<Bank> banks = DatabaseManager.getAllBanks();
			for(Bank bank : banks)
			{
				banksWriter.write(bank.getName() + "\n");
				banksWriter.write(bank.getAccNo() + "\n");
				banksWriter.write(bank.getBalance() + "\n");
				banksWriter.write(bank.getSmsName() + "\n");
				banksWriter.write("\n");
			}
			banksWriter.close();
			
			// Backup Counters Data
			ArrayList<Counters> counters = DatabaseManager.getAllCounters();
			for(Counters counter : counters)
			{
				countersWriter.write(counter.getDate().getSavableDate() + "\n");
				countersWriter.write(counter.getExp01() + "\n");
				countersWriter.write(counter.getExp02() + "\n");
				countersWriter.write(counter.getExp03() + "\n");
				countersWriter.write(counter.getExp04() + "\n");
				countersWriter.write(counter.getExp05() + "\n");
				countersWriter.write(counter.getAmountSpent() + "\n");
				countersWriter.write(counter.getIncome() + "\n");
				countersWriter.write(counter.getSavings() + "\n");
				countersWriter.write(counter.getWithdrawal() + "\n");
				countersWriter.write("\n");
			}
			countersWriter.close();
			
			// Backup Expenditure Types
			ArrayList<String> expTypes = DatabaseManager.getAllExpenditureTypes();
			for(String expType : expTypes)
			{
				expTypesWriter.write(expType + "\n");
			}
			expTypesWriter.close();
			
			Toast.makeText(context, "Data Has Been Backed-Up Succesfully", Toast.LENGTH_LONG).show();
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