package com.chaturvedi.expenditurelist.database;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;


public class DatabaseManager
{
	private static Context context;
	private static DatabaseAdapter databaseAdapter;
	
	private static int numBanks;
	private static int numTransactions;
	
	private static double walletBalance;
	private static double amountSpent;
	private static double income;
	
	private static ArrayList<String> bankNames;
	private static ArrayList<Double> bankBalances;
	private static ArrayList<String> bankSmsNames;
	
	private static ArrayList<String> dates;
	private static ArrayList<String> types;
	private static ArrayList<String> particulars;
	private static ArrayList<Double> rates;
	private static ArrayList<Integer> quantities;
	private static ArrayList<Double> amounts;
	
	private static ArrayList<String> expenditureTypes;
	
	private static ArrayList<Double> counters;
	
	/*private static boolean transactionsTableEdited;
	private static boolean transactionTableAdded;
	private static boolean banksTableEdited;
	private static boolean banksTableAdded;
	private static boolean walletTableEdited;*/
	
	public DatabaseManager(Context cxt)
	{
		context=cxt;
		databaseAdapter = new DatabaseAdapter(context);
		
		/*// Initialize ArrayLists
		bankNames = new ArrayList<String>();
		bankBalances = new ArrayList<Double>();
		bankSmsNames = new ArrayList<String>();
		
		dates = new ArrayList<String>();
		types = new ArrayList<String>();
		particulars = new ArrayList<String>();
		rates = new ArrayList<Double>();
		quantities = new ArrayList<Integer>();
		amounts = new ArrayList<Double>();*/
	}
	
	public static void setContext(Context cxt)
	{
		context=cxt;
		databaseAdapter = new DatabaseAdapter(context);
	}
	
	public static void readDatabase()
	{
		try
		{
			setNumBanks(databaseAdapter.getNumBanks());
			setNumTransactions(databaseAdapter.getNumTransactions());
			
			setWalletBalance(databaseAdapter.getWalletBalance());
			setAmountSpent(databaseAdapter.getAmountSpent());
			setIncome(databaseAdapter.getIncome());
			
			if(numBanks>0)
			{
				bankNames = new ArrayList<String>();
				bankBalances = new ArrayList<Double>();
				bankSmsNames = new ArrayList<String>();
				ArrayList<Bank> banks = databaseAdapter.getAllBanks();
				for(Bank bank:banks)
				{
					bankNames.add(bank.getName());
					bankBalances.add(bank.getBalance());
					bankSmsNames.add(bank.getSmsName());
				}
			}
			
			if(numTransactions>0)
			{
				dates = new ArrayList<String>();
				types = new ArrayList<String>();
				particulars = new ArrayList<String>();
				rates = new ArrayList<Double>();
				quantities = new ArrayList<Integer>();
				amounts = new ArrayList<Double>();
				ArrayList<Transaction> transactions = databaseAdapter.getAllTransactions();
				for(Transaction transaction : transactions)
				{
					dates.add(transaction.getDate());
					types.add(transaction.getType());
					particulars.add(transaction.getParticular());
					rates.add(transaction.getRate());
					quantities.add(transaction.getQuantity());
					amounts.add(transaction.getAmount());
				}
			}
			
			ArrayList<ExpenditureTypes> expTypes = databaseAdapter.getAllExpenditureTypes();
			expenditureTypes = new ArrayList<String>();
			for(ExpenditureTypes expType: expTypes)
			{
				expenditureTypes.add(expType.getExpenditureTypeName());
			}
			
			counters = databaseAdapter.getCounters();
		}
		catch(Exception e)
		{
			//Toast.makeText(context, "Error In Reading Database\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		/*DatabaseManager.numBanks = databaseAdapter.getNumBanks();
		bankNames = new ArrayList<String>();
		bankBalances = new ArrayList<Double>();
		bankSmsNames = new ArrayList<String>();
		Bank firstBank = databaseAdapter.getBank(0);
		if(firstBank!=null)
		{
			bankNames.add(firstBank.getName());
			bankBalances.add(firstBank.getBalance());
			bankSmsNames.add(firstBank.getSmsName());
		}
		else
		{
			bankNames.add("Null Bank");
			bankBalances.add(0.0);
			bankSmsNames.add("null");
		}

		Bank secondBank = databaseAdapter.getBank(1);
		if(secondBank!=null)
		{
			bankNames.add(secondBank.getName());
			bankBalances.add(secondBank.getBalance());
			bankSmsNames.add(secondBank.getSmsName());
		}
		else
		{
			bankNames.add("Null Bank");
			bankBalances.add(0.0);
			bankSmsNames.add("null");
		}
		
		walletBalance = 333;
		amountSpent=40;
		income=30;*/
		
	}
	
	public static void initializeDatabase()
	{
		
		try
		{
			if(numBanks>0)
			{
				ArrayList<Bank> banks = new ArrayList<Bank>();
				for(int i=0; i<numBanks; i++)
				{
					Bank bank = new Bank(i, bankNames.get(i), bankBalances.get(i), bankSmsNames.get(i));
					banks.add(bank);
				}
				databaseAdapter.addAllBanks(banks);
			}
		}
		catch(Exception e)
		{
			Toast.makeText(context, ""+bankNames.size(), Toast.LENGTH_SHORT).show();
			Toast.makeText(context, ""+bankBalances.size(), Toast.LENGTH_SHORT).show();
			Toast.makeText(context, ""+bankSmsNames.size(), Toast.LENGTH_SHORT).show();
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		
		databaseAdapter.initializeWalletTable(walletBalance, amountSpent, income);
		
		ArrayList<ExpenditureTypes> expTypes = new ArrayList<ExpenditureTypes>();
		for(int i=0; i<expenditureTypes.size(); i++)
		{
			expTypes.add(new ExpenditureTypes(i, expenditureTypes.get(i)));
		}
		databaseAdapter.addAllExpenditureTypes(expTypes);
		
		//counters = new ArrayList<Double>(10);
		databaseAdapter.initializeCountersTable();
		
		databaseAdapter.close();
	}
	
	public static void saveDatabase()
	{
		/*dates = new ArrayList<String>();
		dates.add("12-12-2014");
		types = new ArrayList<String>();
		types.add("Book");
		particulars = new ArrayList<String>();
		particulars.add("Tony Gaddis Text Book");
		rates = new ArrayList<Double>();
		rates.add(650.0);
		quantities = new ArrayList<Integer>();
		quantities.add(1);
		amounts = new ArrayList<Double>();
		amounts.add(650.0);*/
		
		if(numTransactions>0)
		{
			ArrayList<Transaction> transactions = new ArrayList<Transaction>();
			for(int i=0; i<numTransactions; i++)
			{
				Transaction transaction=new Transaction(i, dates.get(i), types.get(i), particulars.get(i),
						rates.get(i), quantities.get(i), amounts.get(i));
				transactions.add(transaction);
			}
			databaseAdapter.deleteAllTransactions();
			databaseAdapter.addAllTransactions(transactions);
		}
		else
		{
			databaseAdapter.deleteAllTransactions();
		}
		
		if(numBanks>0)
		{
			ArrayList<Bank> banks = new ArrayList<Bank>();
			for(int i=0; i<numBanks; i++)
			{
				Bank bank = new Bank(i, bankNames.get(i), bankBalances.get(i), bankSmsNames.get(i));
				banks.add(bank);
			}
			databaseAdapter.deleteAllBanks();
			databaseAdapter.addAllBanks(banks);
		}
		else
		{
			databaseAdapter.deleteAllBanks();
		}
		
		databaseAdapter.setWalletBalance(walletBalance);
		databaseAdapter.setAmountSpent(amountSpent);
		databaseAdapter.setIncome(income);
		
		ArrayList<ExpenditureTypes> expTypes = new ArrayList<ExpenditureTypes>();
		for(int i=0; i<expenditureTypes.size(); i++)
		{
			expTypes.add(new ExpenditureTypes(i, expenditureTypes.get(i)));
		}
		databaseAdapter.deleteAllExpenditureTypes();
		databaseAdapter.addAllExpenditureTypes(expTypes);
		
		databaseAdapter.setCounters(counters);
		
		databaseAdapter.close();
	}
	
	public static void clearDatabase()
	{
		numTransactions=0;
		dates = new ArrayList<String>();
		types = new ArrayList<String>();
		particulars = new ArrayList<String>();
		rates = new ArrayList<Double>();
		quantities = new ArrayList<Integer>();
		amounts = new ArrayList<Double>();
		
		amountSpent = 0;
		income = 0;
		
		resetMonthlyCounters();
	}

	/**
	 * @param numBanks the numBanks to set
	 */
	public static void setNumBanks(int numBanks)
	{
		DatabaseManager.numBanks = numBanks;
	}

	/**
	 * @return the numBanks
	 */
	public static int getNumBanks()
	{
		return numBanks;
	}
	
	public static void increamentNumBanks()
	{
		numBanks++;
	}
	
	public static void decreamentNumBanks()
	{
		numBanks--;
	}

	/**
	 * @param numTransactions the numTransactions to set
	 */
	public static void setNumTransactions(int numTransactions)
	{
		DatabaseManager.numTransactions = numTransactions;
	}

	/**
	 * @return the numTransactions
	 */
	public static int getNumTransactions()
	{
		return numTransactions;
	}
	
	public static void increamentNumTransations()
	{
		numTransactions++;
	}
	
	public static void decreamentNumTransactions()
	{
		numTransactions--;
	}

	/**
	 * @param walletBalance the walletBalance to set
	 */
	public static void setWalletBalance(double walletBalance)
	{
		DatabaseManager.walletBalance = walletBalance;
	}
	
	public static void setWalletBalance(String walletBalance)
	{
		DatabaseManager.walletBalance = Double.parseDouble(walletBalance);
	}

	/**
	 * @return the walletBalance
	 */
	public static double getWalletBalance()
	{
		return walletBalance;
	}
	
	public static void increamentWalletBalance(double amount)
	{
		walletBalance+=amount;
	}
	
	public static void increamentWalletBalance(String amount)
	{
		walletBalance+=Double.parseDouble(amount);
	}
	
	public static void decreamentWalletBalance(double amount)
	{
		walletBalance-=amount;
	}
	
	public static void decreamentWalletBalance(String amount)
	{
		walletBalance-=Double.parseDouble(amount);
	}

	/**
	 * @param amountSpent the amountSpent to set
	 */
	public static void setAmountSpent(double amountSpent)
	{
		DatabaseManager.amountSpent = amountSpent;
	}

	public static void setAmountSpent(String amountSpent)
	{
		DatabaseManager.amountSpent = Double.parseDouble(amountSpent);
	}

	/**
	 * @return the amountSpent
	 */
	public static double getAmountSpent()
	{
		return amountSpent;
	}
	
	public static void increamentAmountSpent(double amount)
	{
		amountSpent+=amount;
	}
	
	public static void increamentAmountSpent(String amount)
	{
		amountSpent+=Double.parseDouble(amount);
	}
	
	public static void decreamentAmountSpent(double amount)
	{
		amountSpent-=amount;
	}
	
	public static void decreamentAmountSpent(String amount)
	{
		amountSpent-=Double.parseDouble(amount);
	}

	/**
	 * @param income the income to set
	 */
	public static void setIncome(double income)
	{
		DatabaseManager.income = income;
	}

	public static void setIncome(String income)
	{
		DatabaseManager.income = Double.parseDouble(income);
	}

	/**
	 * @return the income
	 */
	public static double getIncome()
	{
		return income;
	}
	
	public static void increamentIncome(double amount)
	{
		income+=amount;
	}
	
	public static void increamentIncome(String amount)
	{
		income+=Double.parseDouble(amount);
	}
	
	public static void decreamentIncome(double amount)
	{
		income-=amount;
	}
	
	public static void decreamentIncome(String amount)
	{
		income-=Double.parseDouble(amount);
	}

	/**
	 * @param bankNames the bankNames to set
	 */
	public static void setBankNames(ArrayList<String> bankNames)
	{
		DatabaseManager.bankNames = bankNames;
	}

	/**
	 * @return the bankNames
	 */
	public static ArrayList<String> getBankNames()
	{
		return bankNames;
	}
	
	public static void setBankName(int bankNum, String bankName)
	{
		DatabaseManager.bankNames.set(bankNum, bankName);
	}
	
	public static String getBankName(int bankNum)
	{
		return DatabaseManager.bankNames.get(bankNum);
	}

	/**
	 * @param bankBalances the bankBalances to set
	 */
	public static void setBankBalances(ArrayList<Double> bankBalances)
	{
		DatabaseManager.bankBalances = bankBalances;
	}

	/**
	 * @return the bankBalances
	 */
	public static ArrayList<Double> getBankBalances()
	{
		return bankBalances;
	}
	
	public static void setBankBalance(int bankNum, double balance)
	{
		DatabaseManager.bankBalances.set(bankNum, balance);
	}
	
	public static void setBankBalance(int bankNum, String balance)
	{
		DatabaseManager.bankBalances.set(bankNum, Double.parseDouble(balance));
	}
	
	public static double getBankBalance(int bankNum)
	{
		return DatabaseManager.bankBalances.get(bankNum);
	}
	
	public static void increamentBankBalance(int bankNum, double amount)
	{
		DatabaseManager.bankBalances.set(bankNum, DatabaseManager.bankBalances.get(bankNum)+amount);
	}
	
	public static void increamentBankBalance(int bankNum, String amount)
	{
		DatabaseManager.bankBalances.set(bankNum, DatabaseManager.bankBalances.get(bankNum)+Double.parseDouble(amount));
	}
	
	public static void decreamentBankBalance(int bankNum, double amount)
	{
		DatabaseManager.bankBalances.set(bankNum, DatabaseManager.bankBalances.get(bankNum)-amount);
	}
	
	public static void decreamentBankBalance(int bankNum, String amount)
	{
		DatabaseManager.bankBalances.set(bankNum, DatabaseManager.bankBalances.get(bankNum)-Double.parseDouble(amount));
	}

	/**
	 * @param bankSmsNames the bankSmsNames to set
	 */
	public static void setBankSmsNames(ArrayList<String> bankSmsNames)
	{
		DatabaseManager.bankSmsNames = bankSmsNames;
	}

	/**
	 * @return the bankSmsNames
	 */
	public static ArrayList<String> getBankSmsNames()
	{
		return bankSmsNames;
	}
	
	public static void setBankSmsName(int bankNo, String smsName)
	{
		DatabaseManager.bankSmsNames.set(bankNo, smsName);
	}
	
	public static String getBankSmsName(int bankNo)
	{
		return DatabaseManager.bankSmsNames.get(bankNo);
	}

	/**
	 * @param dates the dates to set
	 */
	public static void setDates(ArrayList<String> dates)
	{
		DatabaseManager.dates = dates;
	}

	/**
	 * @return the dates
	 */
	public static ArrayList<String> getDates()
	{
		return dates;
	}
	
	public static void addDate(String date)
	{
		if(DatabaseManager.dates==null)
		{
			DatabaseManager.dates = new ArrayList<String>();
		}
		DatabaseManager.dates.add(date);
	}

	/**
	 * @param types the types to set
	 */
	public static void setTypes(ArrayList<String> types)
	{
		DatabaseManager.types = types;
	}

	/**
	 * @return the types
	 */
	public static ArrayList<String> getTypes()
	{
		return types;
	}
	
	public static void addType(int expenditureTypeNo)
	{
		if(DatabaseManager.types==null)
		{
			DatabaseManager.types = new ArrayList<String>();
		}
		DatabaseManager.types.add(expenditureTypes.get(expenditureTypeNo));
	}
	
	public static void addType(String type)
	{
		if(DatabaseManager.types==null)
		{
			DatabaseManager.types = new ArrayList<String>();
		}
		DatabaseManager.types.add(type);
	}

	/**
	 * @param particulars the particulars to set
	 */
	public static void setParticulars(ArrayList<String> particulars)
	{
		DatabaseManager.particulars = particulars;
	}

	/**
	 * @return the particulars
	 */
	public static ArrayList<String> getParticulars()
	{
		return particulars;
	}
	
	public static void addParticular(String particular)
	{
		if(DatabaseManager.particulars == null)
		{
			DatabaseManager.particulars = new ArrayList<String>();
		}
		DatabaseManager.particulars.add(particular);
	}

	/**
	 * @param rates the rates to set
	 */
	public static void setRates(ArrayList<Double> rates)
	{
		DatabaseManager.rates = rates;
	}

	/**
	 * @return the rates
	 */
	public static ArrayList<Double> getRates()
	{
		return rates;
	}
	
	public static void addRate(double rate)
	{
		if(DatabaseManager.rates == null)
		{
			DatabaseManager.rates = new ArrayList<Double>();
		}
		DatabaseManager.rates.add(rate);
	}
	
	public static void addRate(String rate)
	{
		if(DatabaseManager.rates == null)
		{
			DatabaseManager.rates = new ArrayList<Double>();
		}
		DatabaseManager.rates.add(Double.parseDouble(rate));
	}

	/**
	 * @param quantities the quantities to set
	 */
	public static void setQuantities(ArrayList<Integer> quantities)
	{
		DatabaseManager.quantities = quantities;
	}

	/**
	 * @return the quantities
	 */
	public static ArrayList<Integer> getQuantities()
	{
		return quantities;
	}
	
	public static void addQuantity(int quantity)
	{
		if(DatabaseManager.quantities == null)
		{
			DatabaseManager.quantities = new ArrayList<Integer>();
		}
		DatabaseManager.quantities.add(quantity);
	}
	
	public static void addQuantity(String quantity)
	{
		if(DatabaseManager.quantities == null)
		{
			DatabaseManager.quantities = new ArrayList<Integer>();
		}
		DatabaseManager.quantities.add(Integer.parseInt(quantity));
	}

	/**
	 * @param amounts the amounts to set
	 */
	public static void setAmounts(ArrayList<Double> amounts)
	{
		DatabaseManager.amounts = amounts;
	}

	/**
	 * @return the amounts
	 */
	public static ArrayList<Double> getAmounts()
	{
		return amounts;
	}

	public static void addAmount(double amount)
	{
		if(DatabaseManager.amounts == null)
		{
			DatabaseManager.amounts = new ArrayList<Double>();
		}
		DatabaseManager.amounts.add(amount);
	}
	
	public static void addAmount(String amount)
	{
		if(DatabaseManager.amounts == null)
		{
			DatabaseManager.amounts = new ArrayList<Double>();
		}
		DatabaseManager.amounts.add(Double.parseDouble(amount));
	}
	
	public static void setExpenditureTypes(ArrayList<String> expTypes)
	{
		expenditureTypes=expTypes;
	}
	
	public static ArrayList<String> getExpenditureTypes()
	{
		return DatabaseManager.expenditureTypes;
	}

	public static void setCounters(ArrayList<Double> counters)
	{
		DatabaseManager.counters = counters;
	}
	
	public static ArrayList<Double> getCounters()
	{
		return DatabaseManager.counters;
		
	}
	
	public static void setCounter(int counterNo, double amount)
	{
		counters.set(counterNo, amount);
	}
	
	public static double getCounter(int counterNo)
	{
		return counters.get(counterNo);
	}
	
	public static void increamentCounter(int counterNo, double amount)
	{
		double updatedAmount = counters.get(counterNo)+amount;
		counters.set(counterNo, updatedAmount);
		updatedAmount = counters.get(counterNo+5)+amount;
		counters.set(counterNo+5, updatedAmount);
	}
	
	public static void increamentCounter(int counterNo, String amount)
	{
		double updatedAmount = counters.get(counterNo)+Double.parseDouble(amount);
		counters.set(counterNo, updatedAmount);
		updatedAmount = counters.get(counterNo+5)+Double.parseDouble(amount);
		counters.set(counterNo+5, updatedAmount);
	}
	
	public static void decreamentCounter(int counterNo, double amount)
	{
		double updatedAmount = counters.get(counterNo)-amount;
		counters.set(counterNo, updatedAmount);
		updatedAmount = counters.get(counterNo+5)-amount;
		counters.set(counterNo+5, updatedAmount);
	}
	
	public static void decreamentCounter(int counterNo, String amount)
	{
		double updatedAmount = counters.get(counterNo)-Double.parseDouble(amount);
		counters.set(counterNo, updatedAmount);
		updatedAmount = counters.get(counterNo+5)-Double.parseDouble(amount);
		counters.set(counterNo+5, updatedAmount);
	}
	
	public static void resetAllCounters()
	{
		for(int i=0; i<10; i++)
		{
			counters.set(i, 0.0);
		}
	}
	
	public static void resetMonthlyCounters()
	{
		for(int i=0; i<5; i++)
		{
			counters.set(i, 0.0);
		}
	}
	
	public static void resetTotalCounters()
	{
		for(int i=5; i<10; i++)
		{
			counters.set(i, 0.0);
		}
	}
}
