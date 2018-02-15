package com.chaturvedi.financemanager.extras;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.financemanager.datastructures.*;
import com.chaturvedi.financemanager.functions.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;

public class RestoreManager
{
	private Context context;

	// In version 107, all backup files were clubbed into a single file. Settings was saved as a JSON Object
	// In version 110, wallets were introduced along with hidden/delete attributes
	// In version 124, includeInCounters field added in transactions

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
	 * @param fileUri
	 * @param data	true if Data needs to be restored
	 *              false if Settings need to be restored
	 */
	public RestoreManager(Context cxt, Uri fileUri, boolean data)
	{
		context = cxt;
		if(data)
		{
			result = readDataBackup(fileUri);
		}
		else
		{
			result = readSettingsBackup(fileUri);
		}

	}
	
	/**
	 * Reads the backed up data
	 * @param fileUri:
	 * @return
	 * 		0 If Read Properly
	 * 		1 If No Backup Exists
	 * 		2 Old Data
	 * 		3 Error in Catch Block
	 */
	private int readDataBackup(Uri fileUri)
	{
		try
		{
			InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
			if(inputStream == null)
			{
				return 1;
			}
			BufferedReader backupReader = new BufferedReader(new InputStreamReader(inputStream));
			
			// Read The KEY DATA
			backupReader.readLine();
			appVersionNo = Integer.parseInt(backupReader.readLine().trim());

			if(appVersionNo < Constants.APP_VERSION_124)
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
				Transaction transaction = new Transaction(backupReader.readLine(), backupReader.readLine(), backupReader.readLine(),
						backupReader.readLine(), backupReader.readLine(), backupReader.readLine(), backupReader.readLine(),
						backupReader.readLine(), backupReader.readLine(), backupReader.readLine(), backupReader.readLine());
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
	 * @param fileUri:
	 * @return
	 * 		0 If Read Properly
	 * 		1 If No Backup Exists
	 * 		2 Old Data
	 * 		3 Error in Catch Block
	 */
	private int readSettingsBackup(Uri fileUri)
	{
		/*File backupFile = new File(fileUri);
		if(!backupFile.exists())
		{
			return 1;
		}*/

		try
		{
//			BufferedReader reader = new BufferedReader(new FileReader(backupFile));
			
			InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
			if(inputStream == null)
			{
				return 1;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			
			String rawData = "";
			String line = reader.readLine();
			while(line != null)
			{
				rawData += line;
				line = reader.readLine();
			}
			JSONObject jsonObject = new JSONObject(rawData);

			appVersionNo = jsonObject.getInt(Constants.KEY_APP_VERSION);
			if(appVersionNo < Constants.APP_VERSION_107)
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
}
