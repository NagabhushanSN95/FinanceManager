package com.chaturvedi.financemanager.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class RestoreManager
{
	private Context context;
	//Backup Data introduced in v3.0.0(56)
	private final int APP_VERSION_NO_56 = 56;
	//While Backing Up in v3.0.0(56), Wallet Balance was not Backed-Up. v3.0.2(58) onwards, Wallet Balance 
	//Will Also Be Saved
	private final int APP_VERSION_NO_58 = 58;
	//In Version 71, backup and restore for Templates was enables
	private final int APP_VERSION_NO_71 = 71;
	
	private File backupFolder;
	private String extension;
	
	private int appVersionNo;
	private int numTransactions;
	private int numBanks;
	private int numCountersRows;
	private int numExpTypes;
	private int numTemplates;
	
	private ArrayList<Transaction> transactions;
	private ArrayList<Bank> banks;
	private ArrayList<Counters> counters;
	private ArrayList<String> expTypes;
	private double walletBalance;
	private ArrayList<Template> templates;
	
	public RestoreManager(Context cxt)
	{
		context = cxt;
	}
	
	/**
	 * Reads the backed up data
	 * @param backupFolderName: Finance Manager/Auto Backup
	 * @return
	 * 		0 If Read Properly
	 * 		1 If No Backup Exists
	 * 		2 Old Data
	 * 		3 Error in Catch Block
	 */
	public int readBackups(String backupFolderName)
	{
		backupFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!backupFolder.exists())
		{
			return 1;
		}
		extension = ".snb";
		
		String keyDataFileName = "Key Data";
		File keyDataFile = new File(backupFolder, keyDataFileName+extension);
		if(!keyDataFile.exists())
		{
			return 1;
		}
		
		try
		{
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			
			// Read The KEY DATA
			appVersionNo = Integer.parseInt(keyDataReader.readLine());
			if(appVersionNo < APP_VERSION_NO_56)
			{
				keyDataReader.close();
				return 2;
			}
			else if(appVersionNo < APP_VERSION_NO_58)
			{
				readKeyData();
				readTransactions();
				readBanks();
				readCounters();
				readExpTypes();
				walletBalance = 0;						// Since Wallet Balance was not backed up in that version
				templates = new ArrayList<Template>();	// Same explanation as above
			}
			else if(appVersionNo < APP_VERSION_NO_71)
			{
				readKeyData();
				readTransactions();
				readBanks();
				readCounters();
				readExpTypes();
				readWalletBalance();
				templates = new ArrayList<Template>();	// Since Templates were not backed up in that version
			}
			else
			{
				readKeyData();
				readTransactions();
				readBanks();
				readCounters();
				readExpTypes();
				readWalletBalance();
				readTemplates();
			}
			
			keyDataReader.close();
			return 0;
		}
		catch(IOException e)
		{
			Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			return 3;
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
			numExpTypes = Integer.parseInt(keyDataReader.readLine());
			numTemplates = Integer.parseInt(keyDataReader.readLine());
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
				int ID = Integer.parseInt(transactionsReader.readLine());
				Time createdTime = new Time(transactionsReader.readLine());
				Time modifiedTime = new Time(transactionsReader.readLine());
				Date date = new Date(transactionsReader.readLine());
				String type = transactionsReader.readLine();
				String particular = transactionsReader.readLine();
				double rate = Double.parseDouble(transactionsReader.readLine());
				double quantity = Double.parseDouble(transactionsReader.readLine());
				double amount = Double.parseDouble(transactionsReader.readLine());
				transactionsReader.readLine();
				Transaction transaction = new Transaction(ID, createdTime, modifiedTime, date, type, particular, rate, quantity, amount);
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
		catch(Exception e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Restore Aborted\n" + 
					"Error In RestoreManager/restoreTransactions()\n" + 
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
				int ID = Integer.parseInt(banksReader.readLine());
				String bankName = banksReader.readLine();
				String accNo = banksReader.readLine();
				double balance = Double.parseDouble(banksReader.readLine());
				String smsName = banksReader.readLine();
				banksReader.readLine();
				Bank bank = new Bank(ID, bankName, accNo, balance, smsName);
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
				int ID = Integer.parseInt(countersReader.readLine());
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
				Counters counter = new Counters(ID, date, exp01, exp02, exp03, exp04, exp05, amountSpent, income, savings, withdrawal);
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
			for(int i=0; i<numExpTypes ; i++)
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
	
	private void readTemplates()
	{
		try
		{
			String templatesFileName = "Templates";
			File templatesFile = new File(backupFolder, templatesFileName+extension);
			BufferedReader templatesReader = new BufferedReader(new FileReader(templatesFile));
			
			// Read templates
			templates = new ArrayList<Template>();
			for(int i=0; i<numTemplates; i++)
			{
				int ID = Integer.parseInt(templatesReader.readLine());
				String particulars = templatesReader.readLine();
				String type = templatesReader.readLine();
				double amount = Double.parseDouble(templatesReader.readLine());
				templatesReader.readLine();
				Template template = new Template(ID, particulars, type, amount);
				templates.add(template);
			}
			templatesReader.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readtemplates()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (NumberFormatException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readtemplates()\n" + 
					e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Backup Files Have Tampered. Read Aborted\n" + 
					"Error In RestoreManager/readtemplates()\n" + 
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
	
	public int getNumExpTypes()
	{
		return numExpTypes;
	}
	
	public int getNumTemplates()
	{
		return numTemplates;
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
	
	public ArrayList<Template> getAllTemplates()
	{
		return templates;
		
	}
}
