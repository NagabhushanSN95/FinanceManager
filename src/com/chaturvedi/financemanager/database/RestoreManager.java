package com.chaturvedi.financemanager.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.Toast;

public class RestoreManager
{
	private Context context;
	//Backup Data introduced in v3.0.0(56)
	private final int VERSION_NO_BACKUP_01 = 56;
	//While Backing Up in v3.0.0(56), Wallet Balance was not Backed-Up. v3.0.2(58) onwards, Wallet Balance 
	//Will Also Be Saved
	private final int VERSION_NO_BACKUP_02 = 58;
	
	private File backupFolder;
	private String extension;
	
	private int appVersionNo;
	private int numTransactions;
	private int numBanks;
	private int numCountersRows;
	private ArrayList<Transaction> transactions;
	private ArrayList<Bank> banks;
	private ArrayList<Counters> counters;
	private ArrayList<String> expTypes;
	private double walletBalance;
	
	public RestoreManager(Context cxt)
	{
		context = cxt;
	}
	
	/**
	 * Restores Backed-up Data
	 * @return
	 * 		1 If Restored Properly
	 * 		0 If No Backup Exists
	 * 		-1 Old Data
	 * 		2 Error in Catch Block
	 */
	public int restore()
	{
		String backupFolderName = "Finance Manager/Backups";
		
		backupFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!backupFolder.exists())
		{
			Toast.makeText(context, "No Backups Were Found.\nMake sure the Backup Files are located in " + 
					"Chaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();
			return 0;
		}
		extension = ".snb";
		
		String keyDataFileName = "Key Data";
		File keyDataFile = new File(backupFolder, keyDataFileName+extension);
		if(!keyDataFile.exists())
		{
			Toast.makeText(context, "No Backups Were Found.\nMake sure they are located in " + 
					"Chaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();
			return 0;
		}
		
		try
		{
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			
			// Read The KEY DATA
			if(Integer.parseInt(keyDataReader.readLine()) < VERSION_NO_BACKUP_01)
			{
				Toast.makeText(context, "Old Data. Cannot be Restored. Sorry!", Toast.LENGTH_LONG).show();
				keyDataReader.close();
				return -1;
			}
			else if(Integer.parseInt(keyDataReader.readLine()) < VERSION_NO_BACKUP_02)
			{
				restoreKeyData();
				restoreTransactions();
				restoreBanks();
				restoreCounters();
				restoreExpTypes();
			}
			else
			{
				restoreKeyData();
				restoreTransactions();
				restoreBanks();
				restoreCounters();
				restoreExpTypes();
				restoreWalletBalance();
			}
			
			keyDataReader.close();
			DatabaseManager.saveDatabase();
			Toast.makeText(context, "Data Has Been Restored Succesfully", Toast.LENGTH_LONG).show();
			return 1;
		}
		catch(IOException e)
		{
			Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			return 2;
		}
	}
	
	private void restoreKeyData()
	{
		try
		{
			String keyDataFileName = "Key Data";
			File keyDataFile = new File(backupFolder, keyDataFileName+extension);
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			
			// Read the Data
			keyDataReader.readLine();			// Empty ReadLine to Read the Version No and move to next line
			numTransactions = Integer.parseInt(keyDataReader.readLine());
			numBanks = Integer.parseInt(keyDataReader.readLine());
			numCountersRows = Integer.parseInt(keyDataReader.readLine());
			keyDataReader.close();
			
			// Save The Data Into Database And Overwrite Existing Data In Database
			DatabaseManager.setNumTransactions(numTransactions);
			DatabaseManager.setNumBanks(numBanks);
			DatabaseManager.setNumCountersRows(numCountersRows);
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/readKeyData()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (NumberFormatException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/readKeyData()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/readKeyData()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	private void restoreTransactions()
	{
		try
		{
			String transactionsFileName = "Transactions";
			File transactionsFile = new File(backupFolder, transactionsFileName+extension);
			BufferedReader transactionsReader = new BufferedReader(new FileReader(transactionsFile));
			
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
			// Save The Transactions Into Database And Overwrite Existing Transactions In Database
			DatabaseManager.setAllTransactions(transactions);
			transactionsReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreTransactions()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreTransactions()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void restoreBanks()
	{
		try
		{
			String banksFileName = "Banks";
			File banksFile = new File(backupFolder, banksFileName+extension);
			BufferedReader banksReader = new BufferedReader(new FileReader(banksFile));
			
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
			
			// Save The Banks Data into Database and Overwrite Existing Banks Data in Database
			DatabaseManager.setAllBanks(banks);
			banksReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreBanks()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreBanks()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	private void restoreCounters()
	{
		try
		{
			String countersFileName = "Counters";
			File countersFile = new File(backupFolder, countersFileName+extension);
			BufferedReader countersReader = new BufferedReader(new FileReader(countersFile));
			
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
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreCounters()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (NumberFormatException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreCounters()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreCounters()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void restoreExpTypes()
	{
		try
		{
			String expTypesFileName = "Expenditure Types";
			File expTypesFile = new File(backupFolder, expTypesFileName+extension);
			BufferedReader expTypesReader = new BufferedReader(new FileReader(expTypesFile));
			
			// Read Expenditure Types
			ArrayList<String> expTypes = new ArrayList<String>();
			int NUM_EXP_TYPES = 5;
			for(int i=0; i<NUM_EXP_TYPES ; i++)
			{
				expTypes.add(expTypesReader.readLine());
			}
			DatabaseManager.setAllExpenditureTypes(expTypes);
			expTypesReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreExpTypes()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreExpTypes()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void restoreWalletBalance()
	{
		try
		{
			String walletFileName = "Wallet";
			File walletFile = new File(backupFolder, walletFileName+extension);
			BufferedReader walletReader = new BufferedReader(new FileReader(walletFile));
			
			DatabaseManager.setWalletBalance(Double.parseDouble(walletReader.readLine()));
			walletReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreWalletBalance()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreWalletBalance()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Reads the backed up data
	 * @param backupFolderName: Finance Manager/Auto Backup
	 * @return
	 * 		1 If Read Properly
	 * 		0 If No Backup Exists
	 * 		-1 Old Data
	 * 		2 Error in Catch Block
	 */
	public int readBackups(String backupFolderName)
	{
		backupFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!backupFolder.exists())
		{
			Toast.makeText(context, "No Backups Were Found.\nMake sure the Backup Files are located in " + 
					"Chaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();
			return 0;
		}
		extension = ".snb";
		
		String keyDataFileName = "Key Data";
		File keyDataFile = new File(backupFolder, keyDataFileName+extension);
		if(!keyDataFile.exists())
		{
			Toast.makeText(context, "No Backups Were Found.\nMake sure they are located in " + 
					"Chaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();
			return 0;
		}
		
		try
		{
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			
			// Read The KEY DATA
			appVersionNo = Integer.parseInt(keyDataReader.readLine());
			if(appVersionNo < VERSION_NO_BACKUP_01)
			{
				Toast.makeText(context, "Old Data. Cannot be Restored. Sorry!", Toast.LENGTH_LONG).show();
				keyDataReader.close();
				return -1;
			}
			else if(appVersionNo < VERSION_NO_BACKUP_02)
			{
				readKeyData();
				readTransactions();
				readBanks();
				readCounters();
				readExpTypes();
			}
			else
			{
				readKeyData();
				readTransactions();
				readBanks();
				readCounters();
				readExpTypes();
				readWalletBalance();
			}
			
			keyDataReader.close();
			return 1;
		}
		catch(IOException e)
		{
			Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			return 2;
		}
	}
	
	private void readKeyData()
	{
		try
		{
			String keyDataFileName = "Key Data";
			File keyDataFile = new File(backupFolder, keyDataFileName+extension);
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			
			// Read the Data
			appVersionNo = Integer.parseInt(keyDataReader.readLine());			// Empty ReadLine to Read the Version No and move to next line
			numTransactions = Integer.parseInt(keyDataReader.readLine());
			numBanks = Integer.parseInt(keyDataReader.readLine());
			numCountersRows = Integer.parseInt(keyDataReader.readLine());
			keyDataReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readKeyData()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (NumberFormatException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readKeyData()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readKeyData()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	private void readTransactions()
	{
		try
		{
			String transactionsFileName = "Transactions";
			File transactionsFile = new File(backupFolder, transactionsFileName+extension);
			BufferedReader transactionsReader = new BufferedReader(new FileReader(transactionsFile));
			
			// Read The Transactions
			transactions = new ArrayList<Transaction>();
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
			transactionsReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readTransactions()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readTransactions()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void readBanks()
	{
		try
		{
			String banksFileName = "Banks";
			File banksFile = new File(backupFolder, banksFileName+extension);
			BufferedReader banksReader = new BufferedReader(new FileReader(banksFile));
			
			// Read Banks Data
			banks = new ArrayList<Bank>();
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
			banksReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readBanks()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readBanks()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	private void readCounters()
	{
		try
		{
			String countersFileName = "Counters";
			File countersFile = new File(backupFolder, countersFileName+extension);
			BufferedReader countersReader = new BufferedReader(new FileReader(countersFile));
			
			// Read Counters
			counters = new ArrayList<Counters>();
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
			countersReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readCounters()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (NumberFormatException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readCounters()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readCounters()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void readExpTypes()
	{
		try
		{
			String expTypesFileName = "Expenditure Types";
			File expTypesFile = new File(backupFolder, expTypesFileName+extension);
			BufferedReader expTypesReader = new BufferedReader(new FileReader(expTypesFile));
			
			// Read Expenditure Types
			expTypes = new ArrayList<String>();
			int NUM_EXP_TYPES = 5;
			for(int i=0; i<NUM_EXP_TYPES ; i++)
			{
				expTypes.add(expTypesReader.readLine());
			}
			expTypesReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readExpTypes()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readExpTypes()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void readWalletBalance()
	{
		try
		{
			String walletFileName = "Wallet";
			File walletFile = new File(backupFolder, walletFileName+extension);
			BufferedReader walletReader = new BufferedReader(new FileReader(walletFile));
			walletBalance = Double.parseDouble(walletReader.readLine());
			walletReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/readWalletBalance()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/readWalletBalance()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	public int getAppVersionNo()
	{
		return appVersionNo;
	}
	
	public int getNumTransactions()
	{
		return numTransactions;
	}
	
	public int getNumBanks()
	{
		return numBanks;
	}
	
	public int getNumCountersRows()
	{
		return numCountersRows;
	}
	
	public ArrayList<Transaction> getAllTransactions()
	{
		return transactions;
	}
	
	public ArrayList<Bank> getAllBanks()
	{
		return banks;
	}
	
	public ArrayList<Counters> getAllCounters()
	{
		return counters;
	}
	
	public ArrayList<String> getAllExpTypes()
	{
		return expTypes;
	}
	
	public double getWalletBalance()
	{
		return walletBalance;
	}
	
	/**
	 * Stores Default Preferences
	 * @return
	 */
	public void restoreDefaultPreferences()
	{
		String SHARED_PREFERENCES_DATABASE = "DatabaseInitialized";
		String KEY_DATABASE_INITIALIZED = "database_initialized";
		SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_DATABASE, 0).edit();
		editor.putBoolean(KEY_DATABASE_INITIALIZED, true);
		editor.commit();
	}
}
