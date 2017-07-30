package com.chaturvedi.expenditurelist.database;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.content.Context;
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
	private static ArrayList<String> bankAccNos;
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
				bankAccNos = new ArrayList<String>();
				bankBalances = new ArrayList<Double>();
				bankSmsNames = new ArrayList<String>();
				ArrayList<Bank> banks = databaseAdapter.getAllBanks();
				for(Bank bank:banks)
				{
					bankNames.add(bank.getName());
					bankAccNos.add(bank.getAccNo());
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
			Toast.makeText(context, "Error In Reading Database\n"+e.getMessage(), Toast.LENGTH_LONG).show();
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
					Bank bank = new Bank(i, bankNames.get(i), bankAccNos.get(i), bankBalances.get(i), bankSmsNames.get(i));
					banks.add(bank);
				}
				databaseAdapter.addAllBanks(banks);
			}
		}
		catch(Exception e)
		{
			//Toast.makeText(context, ""+bankNames.size(), Toast.LENGTH_SHORT).show();
			//Toast.makeText(context, ""+bankBalances.size(), Toast.LENGTH_SHORT).show();
			//Toast.makeText(context, ""+bankSmsNames.size(), Toast.LENGTH_SHORT).show();
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
				Bank bank = new Bank(i, bankNames.get(i), bankAccNos.get(i), bankBalances.get(i), bankSmsNames.get(i));
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
	
	public static void addBankName(String bankName)
	{
		DatabaseManager.bankNames.add(bankName);
	}
	
	public static void deleteBankName(int bankNum)
	{
		DatabaseManager.bankNames.remove(bankNum);
	}

	/**
	 * @param bankAccNos the Bank Account Numbers to set
	 */
	public static void setAllBankAccNos(ArrayList<String> bankAccNos)
	{
		DatabaseManager.bankAccNos = bankAccNos;
	}

	/**
	 * @return the Bank Account Numbers
	 */
	public static ArrayList<String> getAllBankAccNos()
	{
		return DatabaseManager.bankAccNos;
	}
	
	public static void setBankAccNo(int bankNum, String bankAccNo)
	{
		DatabaseManager.bankAccNos.set(bankNum, bankAccNo);
	}
	
	public static String getBankAccNo(int bankNum)
	{
		return DatabaseManager.bankAccNos.get(bankNum);
	}
	
	public static void addBankAccNo(String bankAccNo)
	{
		DatabaseManager.bankAccNos.add(bankAccNo);
	}
	
	public static void deleteBankAccNo(int bankNum)
	{
		DatabaseManager.bankAccNos.remove(bankNum);
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
	
	public static void addBankBalance(double balance)
	{
		DatabaseManager.bankBalances.add(balance);
	}
	
	public static void addBankBalance(String balance)
	{
		DatabaseManager.bankBalances.add(Double.parseDouble(balance));
	}
	
	public static void deleteBankBalance(int bankNum)
	{
		DatabaseManager.bankBalances.remove(bankNum);
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
	
	public static void addBankSmsName(String smsName)
	{
		DatabaseManager.bankSmsNames.add(smsName);
	}
	
	public static void deleteBankSmsName(int bankNum)
	{
		DatabaseManager.bankSmsNames.remove(bankNum);
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
	
	public static void setDate(int transactionNo, String date)
	{
		DatabaseManager.dates.set(transactionNo, date);
	}
	
	public static String getDate(int transactionNo)
	{
		return DatabaseManager.dates.get(transactionNo);
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
	
	public static void setType(int transactionNo, int expTypeNo)
	{
		DatabaseManager.types.set(transactionNo, expenditureTypes.get(expTypeNo));
	}
	
	public static void setType(int transactionNo, String type)
	{
		DatabaseManager.types.set(transactionNo, type);
	}
	
	public static String getType(int transactionNo)
	{
		return DatabaseManager.types.get(transactionNo);
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
	
	public static void setParticular(int transactionNo, String particular)
	{
		DatabaseManager.particulars.set(transactionNo, particular);
	}
	
	public static String getParticular(int transactionNo)
	{
		return DatabaseManager.particulars.get(transactionNo);
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
	
	public static void setRate(int transactionNo, double rate)
	{
		DatabaseManager.rates.set(transactionNo, rate);
	}
	
	public static void setRate(int transactionNo, String rate)
	{
		DatabaseManager.rates.set(transactionNo, Double.parseDouble(rate));
	}
	
	public static double getRate(int transactionNo)
	{
		return DatabaseManager.rates.get(transactionNo);
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
	
	public static void setQuantity(int transactionNo, int quantity)
	{
		DatabaseManager.quantities.set(transactionNo, quantity);
	}
	
	public static void setQuantity(int transactionNo, String quantity)
	{
		DatabaseManager.quantities.set(transactionNo, Integer.parseInt(quantity));
	}
	
	public static int getQuantity(int transactionNo)
	{
		return DatabaseManager.quantities.get(transactionNo);
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
	
	public static void setAmount(int transactionNo, double amount)
	{
		DatabaseManager.amounts.set(transactionNo, amount);
	}
	
	public static void setAmount(int transactionNo, String amount)
	{
		DatabaseManager.amounts.set(transactionNo, Double.parseDouble(amount));
	}
	
	public static double getAmount(int transactionNo)
	{
		return DatabaseManager.amounts.get(transactionNo);
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
	
	public static void deleteTransaction(int transactionNo)
	{
		String type = DatabaseManager.types.get(transactionNo);
		String particulars = DatabaseManager.particulars.get(transactionNo);
		double amount = DatabaseManager.amounts.get(transactionNo);
		if(type.equals("Income"))
		{
			DatabaseManager.decreamentIncome(amount);
			DatabaseManager.decreamentWalletBalance(amount);
		}
		else if(type.equals("Income Bank"))
		{
			DatabaseManager.decreamentIncome(amount);
			int bankNo = getBankNumber(particulars);
			DatabaseManager.decreamentBankBalance(bankNo, amount);
		}
		else if(type.equals("Bank Savings"))
		{
			DatabaseManager.increamentWalletBalance(amount);
			int bankNo = getBankNumber(particulars);
			DatabaseManager.decreamentBankBalance(bankNo, amount);
		}
		else if(type.equals("Bank Withdraw"))
		{
			DatabaseManager.decreamentWalletBalance(amount);
			int bankNo = getBankNumber(particulars);
			DatabaseManager.increamentBankBalance(bankNo, amount);
		}
		else if(type.equals(expenditureTypes.get(0)))
		{
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(0, amount);
		}
		else if(type.equals(expenditureTypes.get(1)))
		{
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(1, amount);
		}
		else if(type.equals(expenditureTypes.get(2)))
		{
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(2, amount);
		}
		else if(type.equals(expenditureTypes.get(3)))
		{
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(3, amount);
		}
		else if(type.equals(expenditureTypes.get(4)))
		{
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(4, amount);
		}
		else if(type.equals(expenditureTypes.get(0) + " Bank"))
		{
			int bankNo = getBankNumber(particulars);
			DatabaseManager.increamentBankBalance(bankNo, amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(0, amount);
		}
		else if(type.equals(expenditureTypes.get(1) + " Bank"))
		{
			int bankNo = getBankNumber(particulars);
			DatabaseManager.increamentBankBalance(bankNo, amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(1, amount);
		}
		else if(type.equals(expenditureTypes.get(2) + " Bank"))
		{
			int bankNo = getBankNumber(particulars);
			DatabaseManager.increamentBankBalance(bankNo, amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(2, amount);
		}
		else if(type.equals(expenditureTypes.get(3) + " Bank"))
		{
			int bankNo = getBankNumber(particulars);
			DatabaseManager.increamentBankBalance(bankNo, amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(3, amount);
		}
		else if(type.equals(expenditureTypes.get(4) + " Bank"))
		{
			int bankNo = getBankNumber(particulars);
			DatabaseManager.increamentBankBalance(bankNo, amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounter(4, amount);
		}
		
		DatabaseManager.decreamentNumTransactions();
		DatabaseManager.dates.remove(transactionNo);
		DatabaseManager.types.remove(transactionNo);
		DatabaseManager.particulars.remove(transactionNo);
		DatabaseManager.rates.remove(transactionNo);
		DatabaseManager.quantities.remove(transactionNo);
		DatabaseManager.amounts.remove(transactionNo);
	}

	private static int getBankNumber(String particulars)
	{
		for(int i=0; i<numBanks; i++)
		{
			if(particulars.contains(bankNames.get(i)))
				return i;
		}
		return 0;
	}
	
	/**
	 * Adds a Transaction to the Database
	 * @param data An array of String holding all data
	 * @param data[0] credit/debit
	 * @param data[1] particulars
	 * @param data[2] type
	 * @param data[3] rate
	 * @param data[4] quantity
	 * @param data[5] amount
	 * @param data[6] date
	 */
	public static void addTransaction(String[] data)
	{
		String particulars = data[1];
		String type = data[2];
		String rate = data[3];
		//Toast.makeText(context, "Rate: "+rate, Toast.LENGTH_SHORT).show();
		String quantity = data[4];
		String amount = data[5];
		String date = data[6];
		
		if(data[0].equalsIgnoreCase("Wallet Credit"))
		{
			DatabaseManager.increamentNumTransations();
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.increamentIncome(amount);
			DatabaseManager.addDate(date);
			DatabaseManager.addType("Income");
			DatabaseManager.addParticular(particulars);
			DatabaseManager.addRate(amount);
			DatabaseManager.addQuantity(1);
			DatabaseManager.addAmount(amount);
		}
		else if(data[0].equals("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(type);
			
			// Add the Transaction
			DatabaseManager.increamentNumTransations();
			DatabaseManager.decreamentWalletBalance(amount);
			DatabaseManager.increamentAmountSpent(amount);
			DatabaseManager.addDate(date);
			DatabaseManager.addType(expTypeNo);
			DatabaseManager.addParticular(particulars);
			DatabaseManager.addRate(rate);
			DatabaseManager.addQuantity(quantity);
			DatabaseManager.addAmount(amount);
			DatabaseManager.increamentCounter(expTypeNo, amount);
		}
		else if(data[0].equalsIgnoreCase("Bank Credit"))
		{
			// Calculate Blank Fields
			// BankNo and creditTypesNo both will be sent in types in the form bankNo/creditTypesNo like 2/1
			String[] creditTypes = new String[]{"Account Transfer", "From Wallet"};
			StringTokenizer tokens = new StringTokenizer(type,"/");
			int bankNo = Integer.parseInt(tokens.nextToken());
			int creditTypesNo = Integer.parseInt(tokens.nextToken());
			
			if(creditTypesNo==0)
			{
				type = "Income Bank";
				DatabaseManager.increamentIncome(amount);
			}
			else if(creditTypesNo==1)
			{
				type = "Bank Savings";
				DatabaseManager.decreamentWalletBalance(amount);
			}
			DatabaseManager.increamentBankBalance(bankNo, amount);
			DatabaseManager.increamentNumTransations();
			DatabaseManager.addDate(date);
			DatabaseManager.addType(type);
			DatabaseManager.addParticular(particulars);
			DatabaseManager.addRate(amount);
			DatabaseManager.addQuantity(1);
			DatabaseManager.addAmount(amount);
		}
		else if(data[0].equalsIgnoreCase("Bank Debit"))
		{
			// Calculate Blank Fields
			// BankNo and debitTypesNo both will be sent in types in the form bankNo/debitTypesNo like 2/1
			String[] debitTypes = new String[]{"To Wallet", "Account Transfer"};
			StringTokenizer tokens = new StringTokenizer(type,"/");
			int bankNo = Integer.parseInt(tokens.nextToken());
			int debitTypesNo = Integer.parseInt(tokens.nextToken());
			
			if(debitTypesNo==0)
			{
				type = "Bank Withdraw";
				DatabaseManager.increamentWalletBalance(amount);
			}
			else if(debitTypesNo==1)
			{
				// In this case expTypeNo is concatenated with data[2]
				int expTypeNo = Integer.parseInt(tokens.nextToken());
				type = DatabaseManager.getExpenditureTypes().get(expTypeNo) + " Bank";
				DatabaseManager.increamentAmountSpent(amount);
				DatabaseManager.increamentCounter(expTypeNo, amount);
			}
			
			DatabaseManager.decreamentBankBalance(bankNo, amount);
			DatabaseManager.increamentNumTransations();
			DatabaseManager.addDate(date);
			DatabaseManager.addType(type);
			DatabaseManager.addParticular(particulars);
			DatabaseManager.addRate(amount);
			DatabaseManager.addQuantity(1);
			DatabaseManager.addAmount(amount);
		}
	}
	
	/**
	 * Edits an existing transaction
	 * @param transactionNo Number of the transaction
	 * @param data New data for the transaction
	 */
	public static void editTransaction(int transactionNo, String[] data)
	{
		String oldParticulars = DatabaseManager.getParticular(transactionNo);
		String oldType = DatabaseManager.getType(transactionNo);
		double oldRate = DatabaseManager.getRate(transactionNo);
		int oldQuantity = DatabaseManager.getQuantity(transactionNo);
		double oldAmount = DatabaseManager.getAmount(transactionNo);
		String oldDate = DatabaseManager.getDate(transactionNo);
		
		String newParticulars = data[1];
		String newType = data[2];
		double newRate = Double.parseDouble(data[3]);
		int newQuantity = Integer.parseInt(data[4]);
		double newAmount = Double.parseDouble(data[5]);
		String newDate = data[6];
		
		if(data[0].equalsIgnoreCase("Wallet Credit"))
		{
			DatabaseManager.setParticular(transactionNo, newParticulars);
			DatabaseManager.setDate(transactionNo, newDate);
			double netAmount = newAmount-oldAmount;
			if(newAmount!=DatabaseManager.getAmount(transactionNo))
			{
				DatabaseManager.increamentIncome(netAmount);
				DatabaseManager.increamentWalletBalance(netAmount);
				DatabaseManager.setRate(transactionNo, newAmount);
				DatabaseManager.setAmount(transactionNo, newAmount);
			}
		}
		else if(data[0].equalsIgnoreCase("Wallet Debit"))
		{
			int oldExpTypeNo = DatabaseManager.getExpenditureTypes().indexOf(oldType);
			int newExpTypeNo = Integer.parseInt(newType);
			if(newExpTypeNo!=oldExpTypeNo || newAmount!=oldAmount)
			{
				double netAmount = newAmount - oldAmount;
				DatabaseManager.decreamentCounter(oldExpTypeNo, oldAmount);
				DatabaseManager.increamentCounter(newExpTypeNo, newAmount);
				DatabaseManager.increamentAmountSpent(netAmount);
				DatabaseManager.decreamentWalletBalance(netAmount);
			}
			DatabaseManager.setParticular(transactionNo, newParticulars);
			DatabaseManager.setType(transactionNo, newExpTypeNo);
			DatabaseManager.setRate(transactionNo, newRate);
			DatabaseManager.setQuantity(transactionNo, newQuantity);
			DatabaseManager.setAmount(transactionNo, newAmount);
			DatabaseManager.setDate(transactionNo, newDate);
		}
		else if(data[0].equalsIgnoreCase("Bank Credit"))
		{
			// Determine Which Bank was previous Transaction
			int oldBankNo=0;
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				if(oldParticulars.contains(DatabaseManager.getBankName(i)))
				{
					oldBankNo = i;
					break;
				}
			}
			// Determine what type was previous Transaction and undo it.
			int oldCreditsTypeNo;
			if(oldType.equalsIgnoreCase("Income Bank"))
			{
				oldCreditsTypeNo = 0;
				DatabaseManager.decreamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentIncome(oldAmount);
			}
			else if(oldType.equalsIgnoreCase("Bank Savings"))
			{
				oldCreditsTypeNo = 1;
				DatabaseManager.decreamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.increamentWalletBalance(oldAmount);
			}
			
			// BankNo and creditTypesNo both will be sent in types in the form bankNo/creditTypesNo like 2/1
			String[] creditTypes = new String[]{"Account Transfer", "From Wallet"};
			StringTokenizer tokens = new StringTokenizer(newType,"/");
			int newBankNo = Integer.parseInt(tokens.nextToken());
			int newCreditTypesNo = Integer.parseInt(tokens.nextToken());
			
			// Make the transaction
			if(newCreditTypesNo==0)
			{
				newType = "Income Bank";
				DatabaseManager.increamentIncome(newAmount);
			}
			else if(newCreditTypesNo==1)
			{
				newType = "Bank Savings";
				DatabaseManager.decreamentWalletBalance(newAmount);
			}
			DatabaseManager.increamentBankBalance(newBankNo, newAmount);
			DatabaseManager.setParticular(transactionNo, newParticulars);
			DatabaseManager.setType(transactionNo, newType);
			DatabaseManager.setRate(transactionNo, newAmount);
			DatabaseManager.setQuantity(transactionNo, 1);
			DatabaseManager.setAmount(transactionNo, newAmount);
			DatabaseManager.setDate(transactionNo, newDate);
		}
		else if(data[0].equalsIgnoreCase("Bank Debit"))
		{
			// Determine Which Bank
			int oldBankNo=0;
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				if(oldParticulars.contains(DatabaseManager.getBankName(i)))
				{
					oldBankNo = i;
					break;
				}
			}
			// Determine what type was previous Transaction and undo it.
			int oldDebitsTypeNo, oldExpTypeNo;
			if(oldType.equals("Bank Withdraw"))
			{
				oldDebitsTypeNo = 0;
				DatabaseManager.increamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentWalletBalance(oldAmount);
			}
			else if(oldType.contains("Bank")) // Bank Expenditure
			{
				oldDebitsTypeNo = 1;
				for(int i=0; i<5; i++)
				{
					if(oldType.equals(DatabaseManager.getExpenditureTypes().get(i) + " Bank"))
					{
						 oldExpTypeNo = i;
						 DatabaseManager.decreamentCounter(oldExpTypeNo, oldAmount);
					}
				}
				DatabaseManager.increamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentAmountSpent(oldAmount);
			}
			
			// Make the Transaction
			// Calculate Blank Fields
			// BankNo and debitTypesNo both will be sent in types in the form bankNo/debitTypesNo like 2/1
			String[] debitTypes = new String[]{"To Wallet", "Account Transfer"};
			StringTokenizer tokens = new StringTokenizer(newType,"/");
			int bankNo = Integer.parseInt(tokens.nextToken());
			int debitTypesNo = Integer.parseInt(tokens.nextToken());
			
			if(debitTypesNo==0)
			{
				newType = "Bank Withdraw";
				DatabaseManager.increamentWalletBalance(newAmount);
				DatabaseManager.decreamentBankBalance(bankNo, newAmount);
			}
			else if(debitTypesNo==1)
			{
				// In this case expTypeNo is concatenated with data[2]
				int expTypeNo = Integer.parseInt(tokens.nextToken());
				newType = DatabaseManager.getExpenditureTypes().get(expTypeNo) + " Bank";
				DatabaseManager.increamentAmountSpent(newAmount);
				DatabaseManager.increamentCounter(expTypeNo, newAmount);
				DatabaseManager.decreamentBankBalance(bankNo, newAmount);
			}
			DatabaseManager.setParticular(transactionNo, newParticulars);
			DatabaseManager.setType(transactionNo, newType);
			DatabaseManager.setRate(transactionNo, newAmount);
			DatabaseManager.setQuantity(transactionNo, 1);
			DatabaseManager.setAmount(transactionNo, newAmount);
			DatabaseManager.setDate(transactionNo, newDate);
		}
	}
	
	public static void addBank(String bankName, String bankAccNo, String bankBalance, String bankSmsName)
	{
		DatabaseManager.increamentNumBanks();
		DatabaseManager.addBankName(bankName);
		DatabaseManager.addBankAccNo(bankAccNo);
		DatabaseManager.addBankBalance(bankBalance);
		DatabaseManager.addBankSmsName(bankSmsName);
	}
	
	public static void editBank(int bankNum, String bankName, String bankAccNo, String bankBalance, String bankSmsName)
	{
		DatabaseManager.setBankName(bankNum, bankName);
		DatabaseManager.setBankAccNo(bankNum, bankAccNo);
		DatabaseManager.setBankBalance(bankNum, bankBalance);
		DatabaseManager.setBankSmsName(bankNum, bankSmsName);
	}
	
	public static void deleteBank(int bankNum)
	{
		DatabaseManager.decreamentNumBanks();
		DatabaseManager.deleteBankName(bankNum);
		DatabaseManager.deleteBankAccNo(bankNum);
		DatabaseManager.deleteBankBalance(bankNum);
		DatabaseManager.deleteBankSmsName(bankNum);
	}
}
