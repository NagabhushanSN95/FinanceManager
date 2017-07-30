package com.chaturvedi.financemanager.database;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.financemanager.functions.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static com.chaturvedi.financemanager.R.string.APP_VERSION_107;
import static com.chaturvedi.financemanager.R.string.walletBalance;

public class RestoreManager
{
	private Context context;

	// In version 107, all backup files were clubbed into a single file. Settings was saved as a JSON Object
	// In version 110, wallets were introduced along with hidden/delete attributes

	private static final int APP_VERSION_110 = 110;
	private int result;

	private int appVersionNo;
	private int numWallets;
	private int numBanks;
	private int numTransactions;
	private int numCountersRows;
	private int numExpTypes;
	private int numTemplates;

	private ArrayList<Wallet> wallets;
	private ArrayList<Bank> banks;
	private ArrayList<Transaction> transactions;
	private ArrayList<Counters> counters;
	private ArrayList<ExpenditureType> expTypes;
	private ArrayList<Template> templates;

	private boolean databaseInitialized;
	private int splashDuration;
	private int quoteNo;
	private String transactionsDisplayInterval;
	private String currencySymbol;
	private String respondBankSms;
	private boolean hasBankSmsArrived;

	/**
	 *
	 * @param cxt	Context Eg: ExtrasActivity.this
	 * @param filePath
	 * @param data	true if Data needs to be restored
	 *              false if Settings need to be restored
	 */
	public RestoreManager(Context cxt, String filePath, boolean data)
	{
		context = cxt;
		if(data)
		{
			result = readDataBackup(filePath);
		}
		else
		{
			result = readSettingsBackup(filePath);
		}

	}
	
	/**
	 * Reads the backed up data
	 * @param path:
	 * @return
	 * 		0 If Read Properly
	 * 		1 If No Backup Exists
	 * 		2 Old Data
	 * 		3 Error in Catch Block
	 */
	private int readDataBackup(String path)
	{
		File backupFile = new File(path);

		if(!backupFile.exists())
		{
			return 1;
		}
		try
		{
			BufferedReader backupReader = new BufferedReader(new FileReader(backupFile));
			
			// Read The KEY DATA
			backupReader.readLine();
			appVersionNo = Integer.parseInt(backupReader.readLine().trim());

			if(appVersionNo < APP_VERSION_110)
			{
				return 2;
			}

			numWallets = Integer.parseInt(backupReader.readLine().trim());
			numBanks = Integer.parseInt(backupReader.readLine().trim());
			numTransactions = Integer.parseInt(backupReader.readLine().trim());
			numCountersRows = Integer.parseInt(backupReader.readLine().trim());
			numExpTypes = Integer.parseInt(backupReader.readLine().trim());
			numTemplates = Integer.parseInt(backupReader.readLine().trim());
			backupReader.readLine();

			// Read Wallets Data
			backupReader.readLine();
			wallets = new ArrayList<Wallet>(numWallets);
			for(int i=0; i<numWallets; i++)
			{
				Wallet wallet = new Wallet(backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),
						backupReader.readLine());
				wallets.add(wallet);
				backupReader.readLine();
			}

			// Read Bank Details
			backupReader.readLine();
			banks = new ArrayList<Bank>(numBanks);
			for(int i=0; i<numBanks; i++)
			{
				Bank bank = new Bank(backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),
						backupReader.readLine(),backupReader.readLine(),backupReader.readLine());
				banks.add(bank);
				backupReader.readLine();
			}

			// Read Transactions
			backupReader.readLine();
			transactions = new ArrayList<Transaction>(numTransactions);
			for(int i=0; i<numTransactions; i++)
			{
				Transaction transaction = new Transaction(backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),
						backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),backupReader.readLine()
						,backupReader.readLine(),backupReader.readLine(),backupReader.readLine());
				transactions.add(transaction);
				backupReader.readLine();
			}

			// Read Counters
			backupReader.readLine();
			counters = new ArrayList<Counters>(numCountersRows);
			for(int i=0; i<numCountersRows; i++)
			{
				int ID = Integer.parseInt(backupReader.readLine().trim());
				Date date = new Date(backupReader.readLine().trim());
				double[] counters1 = new double[numExpTypes+4];
				for(int j=0; j<numExpTypes+4; j++)
				{
					counters1[j] = Double.parseDouble(backupReader.readLine().trim());
				}
				Counters counter = new Counters(ID, date, counters1);
				counters.add(counter);
				backupReader.readLine();
			}

			// Read Expenditure Types
			backupReader.readLine();
			expTypes = new ArrayList<ExpenditureType>(numExpTypes);
			for(int i=0; i<numExpTypes ; i++)
			{
				ExpenditureType expenditureType = new ExpenditureType(backupReader.readLine(),backupReader.readLine(),
						backupReader.readLine());
				backupReader.readLine();
				expTypes.add(expenditureType);
			}

			// Read templates
			backupReader.readLine();
			templates = new ArrayList<Template>();
			for(int i=0; i<numTemplates; i++)
			{
				Template template = new Template(backupReader.readLine(),backupReader.readLine(),backupReader.readLine(),
						backupReader.readLine(),backupReader.readLine());
				backupReader.readLine();
				templates.add(template);
			}
			backupReader.close();
			return 0;
		}
		catch(IOException e)
		{
			Toast.makeText(context, "Error in Backing Up Data\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			return 3;
		}
	}

	/**
	 * Reads the backed up data
	 * @param path:
	 * @return
	 * 		0 If Read Properly
	 * 		1 If No Backup Exists
	 * 		2 Old Data
	 * 		3 Error in Catch Block
	 */
	private int readSettingsBackup(String path)
	{
		File backupFile = new File(path);
		if(!backupFile.exists())
		{
			return 1;
		}

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(backupFile));
			String rawData = "";
			String line = reader.readLine();
			while(line != null)
			{
				rawData += line;
				line = reader.readLine();
			}
			JSONObject jsonObject = new JSONObject(rawData);

			appVersionNo = jsonObject.getInt(Constants.KEY_APP_VERSION);
			if(appVersionNo < APP_VERSION_107)
			{
				return 2;
			}
			databaseInitialized = jsonObject.getBoolean(Constants.KEY_DATABASE_INITIALIZED);
			splashDuration = jsonObject.getInt(Constants.KEY_SPLASH_DURATION);
			quoteNo = jsonObject.getInt(Constants.KEY_QUOTE_NO);
			transactionsDisplayInterval = jsonObject.getString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL);
			currencySymbol = jsonObject.getString(Constants.KEY_CURRENCY_SYMBOL);
			respondBankSms = jsonObject.getString(Constants.KEY_RESPOND_BANK_SMS);
			hasBankSmsArrived = jsonObject.getBoolean(Constants.KEY_BANK_SMS_ARRIVED);

			return 0;
		}
		catch (FileNotFoundException e)
		{
			Log.d("Settings Restore", e.getMessage(), e.fillInStackTrace());
			return 3;
		}
		catch (IOException e)
		{
			Log.d("Settings Restore", e.getMessage(), e.fillInStackTrace());
			return 3;
		}
		catch (JSONException e)
		{
			Log.d("Settings Restore", e.getMessage(), e.fillInStackTrace());
			return 3;
		}
	}

	public int getResult()
	{
		return result;
	}

	public int getAppVersionNo()
	{
		return appVersionNo;
	}

	public int getNumWallets()
	{
		return numWallets;
	}

	public int getNumBanks()
	{
		return numBanks;
	}

	public int getNumTransactions()
	{
		return numTransactions;
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

	public ArrayList<Wallet> getAllWallets()
	{
		return wallets;
	}

	public ArrayList<Bank> getAllBanks()
	{
		return banks;
	}

	public ArrayList<Transaction> getAllTransactions()
	{
		return transactions;
	}

	public ArrayList<Counters> getAllCounters()
	{
		return counters;
	}

	public ArrayList<ExpenditureType> getAllExpTypes()
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

	public boolean isDatabaseInitialized()
	{
		return databaseInitialized;
	}

	public int getSplashDuration()
	{
		return splashDuration;
	}

	public int getQuoteNo()
	{
		return quoteNo;
	}

	public String getTransactionsDisplayInterval()
	{
		return transactionsDisplayInterval;
	}

	public String getCurrencySymbol()
	{
		return currencySymbol;
	}

	public String getRespondBankSms()
	{
		return respondBankSms;
	}

	public boolean isHasBankSmsArrived()
	{
		return hasBankSmsArrived;
	}
	/**
	 * Reads the backed up data
	 * @param backupFolderName: Finance Manager/Auto Backup
	 * @return
	 * 		0 If Read Properly
	 * 		1 If No Backup Exists
	 * 		2 Old Data
	 * 		3 Error in Catch Block
	 * /
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
			appVersionNo = Integer.parseInt(keyDataReader.readLine().trim());
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
			else if(appVersionNo < APP_VERSION_NO_88)
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
			appVersionNo = Integer.parseInt(keyDataReader.readLine().trim());			// Empty ReadLine to Read the Version No and move to next line
			numTransactions = Integer.parseInt(keyDataReader.readLine().trim());
			numBanks = Integer.parseInt(keyDataReader.readLine().trim());
			numCountersRows = Integer.parseInt(keyDataReader.readLine().trim());
			numExpTypes = Integer.parseInt(keyDataReader.readLine().trim());
			numTemplates = Integer.parseInt(keyDataReader.readLine().trim());
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
				int ID = Integer.parseInt(transactionsReader.readLine().trim());
				//Toast.makeText(context, "i="+i+", id="+ID, Toast.LENGTH_SHORT).show();
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
				int ID = Integer.parseInt(countersReader.readLine().trim());
				Date date = new Date(countersReader.readLine().trim());
				double[] counters1 = new double[numExpTypes+4];
				for(int j=0; j<numExpTypes+4; j++)
				{
					counters1[j] = Double.parseDouble(countersReader.readLine().trim());
				}
				countersReader.readLine().trim();
				Counters counter = new Counters(ID, date, counters1);
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
				expTypes.add(expTypesReader.readLine().trim());
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
			walletBalance = Double.parseDouble(walletReader.readLine().trim());
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
				int ID = Integer.parseInt(templatesReader.readLine().trim());
				String particulars = templatesReader.readLine().trim();
				String type = templatesReader.readLine().trim();
				double amount = Double.parseDouble(templatesReader.readLine().trim());
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
	}*/
}
