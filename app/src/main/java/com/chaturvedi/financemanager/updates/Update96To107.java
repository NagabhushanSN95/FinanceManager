// Shree KRISHNAya Namaha

/** Before version 107, data was backed up in separate files like Key Data.snb, Transactions.snb, Banks.snb etc
 *  From version 107, data will be backed up in a single file eg: Data Backup - 20161026082416255 (year/month/date/hour/minute/second/millisecond)
 *  There might be previous backups which cannot be read by the new version. So, those backups need to be updated
 *  This class reads the Auto Backups and Manual Backups (if they exist) and re-create them in the new format
 * */
package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.Counters;
import com.chaturvedi.financemanager.database.Date;
import com.chaturvedi.financemanager.database.Template;
import com.chaturvedi.financemanager.database.Time;
import com.chaturvedi.financemanager.database.Transaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class Update96To107
{
	private Context context;

	public Update96To107(Context cxt)
	{
		context = cxt;
		Toast.makeText(context, "Updating...", Toast.LENGTH_LONG).show();
		updateBackupFiles("Finance Manager/Auto Backups");
		updateBackupFiles("Finance Manager/Backups");
	}

	private void updateBackupFiles(String backupFolderName)
	{
		File backupFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if (!backupFolder.exists())
		{
			return;
		}
		String extension = ".snb";

		String keyDataFileName = "Key Data";
		File keyDataFile = new File(backupFolder, keyDataFileName + extension);
		if (!keyDataFile.exists())
		{
			return;
		}

		try
		{
			// Read The KEY DATA
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			keyDataReader.readLine();
			int numTransactions = Integer.parseInt(keyDataReader.readLine().trim());
			int numBanks = Integer.parseInt(keyDataReader.readLine().trim());
			int numCountersRows = Integer.parseInt(keyDataReader.readLine().trim());
			int numExpTypes = Integer.parseInt(keyDataReader.readLine().trim());
			int numTemplates = Integer.parseInt(keyDataReader.readLine().trim());
			keyDataReader.close();

			// Read Wallet Balance
			String walletFileName = "Wallet";
			File walletFile = new File(backupFolder, walletFileName + extension);
			BufferedReader walletReader = new BufferedReader(new FileReader(walletFile));
			double walletBalance = Double.parseDouble(walletReader.readLine().trim());
			walletReader.close();

			// Read Banks
			String banksFileName = "Banks";
			File banksFile = new File(backupFolder, banksFileName + extension);
			BufferedReader banksReader = new BufferedReader(new FileReader(banksFile));
			ArrayList<Bank> banks = new ArrayList<Bank>();
			for (int i = 0; i < numBanks; i++)
			{
				int ID = Integer.parseInt(banksReader.readLine().trim());
				String bankName = banksReader.readLine().trim();
				String accNo = banksReader.readLine().trim();
				double balance = Double.parseDouble(banksReader.readLine().trim());
				String smsName = banksReader.readLine().trim();
				banksReader.readLine();
				Bank bank = new Bank(ID, bankName, accNo, balance, smsName);
				banks.add(bank);
			}
			banksReader.close();

			// Read Transactions
			String transactionsFileName = "Transactions";
			File transactionsFile = new File(backupFolder, transactionsFileName + extension);
			BufferedReader transactionsReader = new BufferedReader(new FileReader(transactionsFile));
			ArrayList<Transaction> transactions = new ArrayList<Transaction>();
			for (int i = 0; i < numTransactions; i++)
			{
				int ID = Integer.parseInt(transactionsReader.readLine().trim());
				Time createdTime = new Time(transactionsReader.readLine().trim());
				Time modifiedTime = new Time(transactionsReader.readLine().trim());
				Date date = new Date(transactionsReader.readLine().trim());
				String type = transactionsReader.readLine().trim();
				String particular = transactionsReader.readLine().trim();
				double rate = Double.parseDouble(transactionsReader.readLine().trim());
				double quantity = Double.parseDouble(transactionsReader.readLine().trim());
				double amount = Double.parseDouble(transactionsReader.readLine().trim());
				transactionsReader.readLine();
				Transaction transaction = new Transaction(ID, createdTime, modifiedTime, date, type, particular, rate, quantity, amount);
				transactions.add(transaction);
			}
			transactionsReader.close();

			// Read Counters
			String countersFileName = "Counters";
			File countersFile = new File(backupFolder, countersFileName + extension);
			BufferedReader countersReader = new BufferedReader(new FileReader(countersFile));
			ArrayList<Counters> counters = new ArrayList<Counters>();
			for (int i = 0; i < numCountersRows; i++)
			{
				int ID = Integer.parseInt(countersReader.readLine().trim());
				Date date = new Date(countersReader.readLine().trim());
				double[] counters1 = new double[numExpTypes + 4];
				for (int j = 0; j < numExpTypes + 4; j++)
				{
					counters1[j] = Double.parseDouble(countersReader.readLine().trim());
				}
				countersReader.readLine();
				Counters counter = new Counters(ID, date, counters1);
				counters.add(counter);
			}
			countersReader.close();

			// Read Expenditure Types
			String expTypesFileName = "Expenditure Types";
			File expTypesFile = new File(backupFolder, expTypesFileName + extension);
			BufferedReader expTypesReader = new BufferedReader(new FileReader(expTypesFile));

			// Read Expenditure Types
			ArrayList<String> expTypes = new ArrayList<String>();
			for (int i = 0; i < numExpTypes; i++)
			{
				expTypes.add(expTypesReader.readLine().trim());
			}
			expTypesReader.close();

			// Read Templates
			String templatesFileName = "Templates";
			File templatesFile = new File(backupFolder, templatesFileName + extension);
			BufferedReader templatesReader = new BufferedReader(new FileReader(templatesFile));
			ArrayList<Template> templates = new ArrayList<Template>();
			for (int i = 0; i < numTemplates; i++)
			{
				int ID = Integer.parseInt(templatesReader.readLine().trim());
				String particulars = templatesReader.readLine().trim();
				String type = templatesReader.readLine().trim();
				double amount = Double.parseDouble(templatesReader.readLine().trim());
				templatesReader.readLine();
				Template template = new Template(ID, particulars, type, amount);
				templates.add(template);
			}
			templatesReader.close();

			// Read Preferences
			String preferencesFileName = "Preferences";
			File preferencesFile = new File(backupFolder, preferencesFileName + extension);
			BufferedReader preferencesReader = new BufferedReader(new FileReader(preferencesFile));
			String[] preferences = new String[8];
			for(int i=0; i<8; i++)
			{
				preferences[i] = preferencesReader.readLine();
			}

			// Move backup files to Android/Chaturvedi/Finance Manager folder from Chaturvedi/Finance Manager
			backupFolderName = "Chaturvedi/" + backupFolderName;
			backupFolder = new File(Environment.getExternalStoragePublicDirectory("Android"), backupFolderName);
			// Check if folder exist. If it doesn't, create it. If it fails, return
			if(!backupFolder.exists() && !backupFolder.mkdirs())
			{
				return;
			}

			// Create Backup File
			String backupFileName = "Data Backup - " + (new Time(Calendar.getInstance())).getTimeForFileName();
			File backupFile = new File(backupFolder, backupFileName + extension);
			// Check if file exist. If it doesn't, create it. If it fails, return
			if(!backupFile.exists() && !backupFile.createNewFile())
			{
				return;
			}
			BufferedWriter backupWriter = new BufferedWriter(new FileWriter(backupFile));

			// Write Key Data
			backupWriter.write("---------------Key Data---------------\n");
			int versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			backupWriter.write(versionNo + "\n");
			backupWriter.write(banks.size() + "\n");
			backupWriter.write(transactions.size() + "\n");
			backupWriter.write(counters.size() + "\n");
			backupWriter.write(expTypes.size() + "\n");
			backupWriter.write(templates.size() + "\n");
			backupWriter.write("\n");

			// Write Wallet Balance
			backupWriter.write("---------------Wallet---------------\n");
			backupWriter.write(walletBalance + "\n");
			backupWriter.write("\n");

			// Write Banks
			backupWriter.write("---------------Banks---------------\n");
			for (Bank bank : banks)
			{
				backupWriter.write(bank.getID() + "\n");
				backupWriter.write(bank.getName() + "\n");
				backupWriter.write(bank.getAccNo() + "\n");
				backupWriter.write(bank.getBalance() + "\n");
				backupWriter.write(bank.getSmsName() + "\n");
				backupWriter.write("\n");
			}

			// Write Transactions
			backupWriter.write("---------------Transactions---------------\n");
			for (Transaction transaction : transactions)
			{
				backupWriter.write(transaction.getID() + "\n");
				backupWriter.write(transaction.getCreatedTime().toString() + "\n");
				backupWriter.write(transaction.getModifiedTime().toString() + "\n");
				backupWriter.write(transaction.getDate().getSavableDate() + "\n");
				backupWriter.write(transaction.getType() + "\n");
				backupWriter.write(transaction.getParticular() + "\n");
				backupWriter.write(transaction.getRate() + "\n");
				backupWriter.write(transaction.getQuantity() + "\n");
				backupWriter.write(transaction.getAmount() + "\n");
				backupWriter.write("\n");
			}

			// Write Counters
			backupWriter.write("---------------Counters---------------\n");
			for (Counters counter : counters)
			{
				backupWriter.write(counter.getID() + "\n");
				backupWriter.write(counter.getDate().getSavableDate() + "\n");
				double[] expenditures = counter.getAllExpenditures();
				for (double expenditure : expenditures)
				{
					backupWriter.write(expenditure + "\n");
				}
				backupWriter.write(counter.getAmountSpent() + "\n");
				backupWriter.write(counter.getIncome() + "\n");
				backupWriter.write(counter.getSavings() + "\n");
				backupWriter.write(counter.getWithdrawal() + "\n");
				backupWriter.write("\n");
			}

			// Write Expenditure Types
			backupWriter.write("---------------Expenditure Types---------------\n");
			for (String expType : expTypes)
			{
				backupWriter.write(expType + "\n");
			}
			backupWriter.write("\n");

			// Writing Templates
			backupWriter.write("---------------Templates---------------\n");
			for (Template template : templates)
			{
				backupWriter.write(template.getID() + "\n");
				backupWriter.write(template.getParticular() + "\n");
				backupWriter.write(template.getType() + "\n");
				backupWriter.write(template.getAmount() + "\n");
				backupWriter.write("\n");
			}

			backupWriter.write("---------------End---------------");
			backupWriter.close();


			// Write Settings to a separate file
			final String KEY_APP_VERSION = "AppVersionNo";
			final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
			final String KEY_SPLASH_DURATION = "SplashDuration";
			final String KEY_QUOTE_NO = "QuoteNo";
			final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
			final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
			final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
			final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(KEY_APP_VERSION, Integer.parseInt(preferences[0]));
			jsonObject.put(KEY_DATABASE_INITIALIZED, Boolean.parseBoolean(preferences[1]));
			jsonObject.put(KEY_SPLASH_DURATION, Integer.parseInt(preferences[2]));
			jsonObject.put(KEY_QUOTE_NO, Integer.parseInt(preferences[3]));
			jsonObject.put(KEY_TRANSACTIONS_DISPLAY_INTERVAL, preferences[4]);
			jsonObject.put(KEY_CURRENCY_SYMBOL, preferences[5]);
			jsonObject.put(KEY_RESPOND_BANK_SMS, preferences[6]);
			jsonObject.put(KEY_BANK_SMS_ARRIVED, Boolean.parseBoolean(preferences[7]));

			String settingsFileName = "Settings Backup - " + (new Time(Calendar.getInstance())).getTimeForFileName();
			File settingsBackupFile = new File(backupFolder, settingsFileName + extension);
			// Check if file exist. If it doesn't, create it. If it fails, return
			if(!settingsBackupFile.exists() && !settingsBackupFile.createNewFile())
			{
				return;
			}
			BufferedWriter settingsWriter = new BufferedWriter(new FileWriter(settingsBackupFile));
			settingsWriter.write(jsonObject.toString(4));
			settingsWriter.close();
		}
		catch (FileNotFoundException e)
		{
			Log.d("Update96To107", e.getMessage(), e.fillInStackTrace());
		}
		catch (IOException e)
		{
			Log.d("Update96To107", e.getMessage(), e.fillInStackTrace());
		}
		catch (NumberFormatException e)
		{
			Log.d("Update96To107", e.getMessage(), e.fillInStackTrace());
		}
		catch (PackageManager.NameNotFoundException e)
		{
			Log.d("Update96To107", e.getMessage(), e.fillInStackTrace());
		}
		catch (JSONException e)
		{
			Log.d("Update96To107", e.getMessage(), e.fillInStackTrace());
		}
	}
}
