package com.chaturvedi.financemanager.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.Counters;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Transaction;

public class BackupManager
{
	private Context context;
	
	public BackupManager(Context cxt)
	{
		context = cxt;
	}
	
	public void autoBackup()
	{
		backup("Finance Manager/Auto Backup");
	}
	
	public void manualBackup()
	{
		backup("Finance Manager/Backups");
	}
	
	public void backup(String backupFolderName)
	{
		File financeFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!financeFolder.exists())
			financeFolder.mkdirs();
		
		String extension = ".snb";
		String keyDataFileName = "Key Data";
		String transactionsFileName = "Transactions";
		String banksFileName = "Banks";
		String countersFileName = "Counters";
		String expTypesFileName = "Expenditure Types";
		String walletFileName = "Wallet";
		String templatesFileName = "Templates";
		String preferencesFileName = "Preferences";

		File keyDataFile = new File(financeFolder, keyDataFileName+extension);
		File transactionsFile = new File(financeFolder, transactionsFileName+extension);
		File banksFile = new File(financeFolder, banksFileName+extension);
		File countersFile = new File(financeFolder, countersFileName+extension);
		File expTypesFile = new File(financeFolder, expTypesFileName+extension);
		File walletFile = new File(financeFolder, walletFileName+extension);
		File templatesFile = new File(financeFolder, templatesFileName+extension);
		File preferencesFile = new File(financeFolder, preferencesFileName+extension);
		
		try
		{
			BufferedWriter keyDataWriter = new BufferedWriter(new FileWriter(keyDataFile));
			BufferedWriter transactionsWriter = new BufferedWriter(new FileWriter(transactionsFile));
			BufferedWriter banksWriter = new BufferedWriter(new FileWriter(banksFile));
			BufferedWriter countersWriter = new BufferedWriter(new FileWriter(countersFile));
			BufferedWriter expTypesWriter = new BufferedWriter(new FileWriter(expTypesFile));
			BufferedWriter walletWriter = new BufferedWriter(new FileWriter(walletFile));
			BufferedWriter templatesWriter = new BufferedWriter(new FileWriter(templatesFile));
			BufferedWriter preferencesWriter = new BufferedWriter(new FileWriter(preferencesFile));
			
			// Store The KEY DATA
			int versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			keyDataWriter.write(versionNo + "\n");
			keyDataWriter.write(DatabaseManager.getNumTransactions() + "\n");
			keyDataWriter.write(DatabaseManager.getNumBanks() + "\n");
			keyDataWriter.write(DatabaseManager.getNumCountersRows() + "\n");
			keyDataWriter.write(DatabaseManager.getAllExpenditureTypes().size() + "\n");
			keyDataWriter.write(DatabaseManager.getAllTemplates().size() + "\n");
			keyDataWriter.close();
			
			// Backup The Transactions
			ArrayList<Transaction> transactions = DatabaseManager.getAllTransactions();
			int i=0;				// For Debug Purposes Only
			for(Transaction transaction : transactions)
			{
				i++;
				if(transaction.getID() != i)
				{
					Toast.makeText(context, "ID Error in " + i + "th Transaction\n" + "BackupManager/backup\n"
							+ "i=" + i + "id=" + transaction.getID()
							,Toast.LENGTH_LONG).show();
					transaction.setID(i);
				}
				/*else
				{
					Toast.makeText(context, "" + i + "th Transaction\n"
							+ "i=" + i + "id=" + transaction.getID()
							,Toast.LENGTH_SHORT).show();
				}*/
				transactionsWriter.write(transaction.getID() + "\n");
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
				banksWriter.write(bank.getID() + "\n");
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
				countersWriter.write(counter.getID() + "\n");
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
			
			// Backup Wallet Balance
			walletWriter.write(DatabaseManager.getWalletBalance() + "\n");
			walletWriter.close();
			
			// Backup The Templates
			ArrayList<Template> templates = DatabaseManager.getAllTemplates();
			for(Template template : templates)
			{
				templatesWriter.write(template.getID() + "\n");
				templatesWriter.write(template.getParticular() + "\n");
				templatesWriter.write(template.getType() + "\n");
				templatesWriter.write(template.getAmount() + "\n");
				templatesWriter.write("\n");
			}
			templatesWriter.close();
			
			// Backup Preferences
			final String ALL_PREFERENCES = "AllPreferences";
			final String KEY_APP_VERSION = "AppVersionNo";
			final int CURRENT_APP_VERSION_NO = 
					Integer.parseInt(context.getResources().getString(R.string.currentAppVersion)); 
			final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
			final String KEY_SPLASH_DURATION = "SplashDuration";
			final String KEY_QUOTE_NO = "QuoteNo";
			final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
			final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
			final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
			final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
			
			SharedPreferences preferences = context.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
			preferencesWriter.write(preferences.getInt(KEY_APP_VERSION, CURRENT_APP_VERSION_NO) + "\n");
			preferencesWriter.write(preferences.getBoolean(KEY_DATABASE_INITIALIZED, true) + "\n");
			preferencesWriter.write(preferences.getInt(KEY_SPLASH_DURATION, 5000) + "\n");
			preferencesWriter.write(preferences.getInt(KEY_QUOTE_NO, 0) + "\n");
			preferencesWriter.write(preferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month") + "\n");
			preferencesWriter.write(preferences.getString(KEY_CURRENCY_SYMBOL, " ") + "\n");
			preferencesWriter.write(preferences.getString(KEY_RESPOND_BANK_SMS, "Popup") + "\n");
			preferencesWriter.write(preferences.getBoolean(KEY_BANK_SMS_ARRIVED, false) + "\n");
			preferencesWriter.close();
			
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