package com.chaturvedi.financemanager.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.functions.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class BackupManager
{
	private Context context;
	
	public BackupManager(Context cxt)
	{
		context = cxt;
	}
	
	public void autoBackup()
	{
		String backupFolderName = "Chaturvedi/Finance Manager/Auto Backups";
		File financeFolder = new File(Environment.getExternalStoragePublicDirectory("Android"), backupFolderName);
		if(!financeFolder.exists() && !financeFolder.mkdirs())
			return;

		String backupFileName = "Auto Data Backup.snb";
		File backupFile = new File(financeFolder, backupFileName);
		backupData(backupFile);
	}
	
	public void dailyBackup()
	{
		String backupFolderName = "Chaturvedi/Finance Manager/Auto Backups";
		File financeFolder = new File(Environment.getExternalStoragePublicDirectory("Android"), backupFolderName);
		if(!financeFolder.exists() && !financeFolder.mkdirs())
			return;

		String backupFileName = "Data Backup - " + (new Time(Calendar.getInstance())).getTimeForFileName() + ".snb";
		File backupFile = new File(financeFolder, backupFileName);
		backupData(backupFile);
	}

	public void manualBackup()
	{
		String backupFolderName = "Chaturvedi/Finance Manager/Backups";
		File financeFolder = new File(Environment.getExternalStoragePublicDirectory("Android"), backupFolderName);
		if(!financeFolder.exists() && !financeFolder.mkdirs())
			return;

		String backupFileName = "Data Backup - " + (new Time(Calendar.getInstance())).getTimeForFileName() + ".snb";
		File backupFile = new File(financeFolder, backupFileName);
		backupData(backupFile);
	}
	
	private void backupData(File backupFile)
	{
		try
		{
			DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
			BufferedWriter backupWriter = new BufferedWriter(new FileWriter(backupFile));
			
			// Store The KEY DATA
			backupWriter.write("---------------Key Data---------------\n");
			int versionNo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			backupWriter.write(versionNo + "\n");
			backupWriter.write(databaseAdapter.getNumWallets() + "\n");
			backupWriter.write(databaseAdapter.getNumBanks() + "\n");
			backupWriter.write(databaseAdapter.getNumTransactions() + "\n");
			backupWriter.write(databaseAdapter.getNumCountersRows() + "\n");
			backupWriter.write(databaseAdapter.getAllExpenditureTypes().size() + "\n");
			backupWriter.write(databaseAdapter.getAllTemplates().size() + "\n");
			backupWriter.write("\n");

			// Write Wallets Data
			backupWriter.write("---------------Wallets---------------\n");
			ArrayList<NewWallet> wallets = databaseAdapter.getAllWallets();
			for(NewWallet wallet : wallets)
			{
				backupWriter.write(wallet.getID() + "\n");
				backupWriter.write(wallet.getName() + "\n");
				backupWriter.write(wallet.getBalance() + "\n");
				backupWriter.write(wallet.isDeleted() + "\n");
				backupWriter.write("\n");
			}

			// Backup Banks Data
			backupWriter.write("---------------Banks---------------\n");
			ArrayList<Bank> banks = databaseAdapter.getAllBanks();
			for(Bank bank : banks)
			{
				backupWriter.write(bank.getID() + "\n");
				backupWriter.write(bank.getName() + "\n");
				backupWriter.write(bank.getAccNo() + "\n");
				backupWriter.write(bank.getBalance() + "\n");
				backupWriter.write(bank.getSmsName() + "\n");
				backupWriter.write(bank.isDeleted() + "\n");
				backupWriter.write("\n");
			}
			
			// Backup The Transactions
			backupWriter.write("---------------Transactions---------------\n");
			ArrayList<Transaction> transactions = databaseAdapter.getAllTransactions();
			for(Transaction transaction : transactions)
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
				backupWriter.write(transaction.isHidden() + "\n");
				backupWriter.write("\n");
			}

			// Backup Counters Data
			backupWriter.write("---------------Counters---------------\n");
			ArrayList<Counters> counters = databaseAdapter.getAllCountersRows();
			for(Counters counter : counters)
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

			// Backup Expenditure Types
			backupWriter.write("---------------Expenditure Types---------------\n");
			ArrayList<ExpenditureType> expTypes = databaseAdapter.getAllExpenditureTypes();
			for(ExpenditureType expType : expTypes)
			{
				backupWriter.write(expType.getId() + "\n");
				backupWriter.write(expType.getName() + "\n");
				backupWriter.write(expType.isDeleted() + "\n");
				backupWriter.write("\n");
			}

			// Backup The Templates
			backupWriter.write("---------------Templates---------------\n");
			ArrayList<Template> templates = databaseAdapter.getAllTemplates();
			for(Template template : templates)
			{
				backupWriter.write(template.getID() + "\n");
				backupWriter.write(template.getParticular() + "\n");
				backupWriter.write(template.getType() + "\n");
				backupWriter.write(template.getAmount() + "\n");
				backupWriter.write(template.isHidden() + "\n");
				backupWriter.write("\n");
			}

			backupWriter.close();
			
			Toast.makeText(context, "Data Has Been Backed-Up Successfully", Toast.LENGTH_LONG).show();
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

	public void backupSettings(File backupFile)
	{
		try
		{

			final int CURRENT_APP_VERSION_NO =
					Integer.parseInt(context.getResources().getString(R.string.currentAppVersion));

			SharedPreferences preferences = context.getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(Constants.KEY_APP_VERSION, preferences.getInt(Constants.KEY_APP_VERSION, CURRENT_APP_VERSION_NO));
			jsonObject.put(Constants.KEY_DATABASE_INITIALIZED, preferences.getBoolean(Constants.KEY_DATABASE_INITIALIZED, true));
			jsonObject.put(Constants.KEY_SPLASH_DURATION, preferences.getInt(Constants.KEY_SPLASH_DURATION, 5000));
			jsonObject.put(Constants.KEY_QUOTE_NO, preferences.getInt(Constants.KEY_QUOTE_NO, 0));
			jsonObject.put(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, preferences.getString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month"));
			jsonObject.put(Constants.KEY_CURRENCY_SYMBOL, preferences.getString(Constants.KEY_CURRENCY_SYMBOL, " "));
			jsonObject.put(Constants.KEY_RESPOND_BANK_SMS, preferences.getString(Constants.KEY_RESPOND_BANK_SMS, "Popup"));
			jsonObject.put(Constants.KEY_BANK_SMS_ARRIVED, preferences.getBoolean(Constants.KEY_BANK_SMS_ARRIVED, false));

			BufferedWriter backupWriter = new BufferedWriter(new FileWriter(backupFile));
			backupWriter.write(jsonObject.toString(4));
			backupWriter.close();

			Toast.makeText(context, "Settings Has Been Backed-Up Successfully", Toast.LENGTH_LONG).show();
		}
		catch(IOException e)
		{
			Log.d("backupSettings()", e.getMessage(), e.fillInStackTrace());
		}
		catch (JSONException e)
		{
			Log.d("backupSettings()", e.getMessage(), e.fillInStackTrace());
		}
	}
}