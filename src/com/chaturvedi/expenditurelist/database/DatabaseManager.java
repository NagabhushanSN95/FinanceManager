package com.chaturvedi.expenditurelist.database;

import java.util.ArrayList;

import android.content.Context;
import android.widget.Toast;

public class DatabaseManager
{
	private static Context context;
	private static DatabaseAdapter databaseAdapter;
	
	private static int numBanks;
	private static int numTransactions;
	private static int numCountersRows;
	
	private static double walletBalance;
	private static double amountSpent;
	private static double income;
	
	private static ArrayList<String> bankNames;
	private static ArrayList<String> bankAccNos;
	private static ArrayList<Double> bankBalances;
	private static ArrayList<String> bankSmsNames;

	private static ArrayList<Time> createdTimes;
	private static ArrayList<Time> modifiedTimes;
	private static ArrayList<Date> dates;
	private static ArrayList<String> types;
	private static ArrayList<String> particulars;
	private static ArrayList<Double> rates;
	private static ArrayList<Double> quantities;
	private static ArrayList<Double> amounts;
	
	private static ArrayList<String> expenditureTypes;
	
	private static ArrayList<Counters> counters;
	
	/*private static boolean transactionsTableEdited;
	private static boolean transactionTableAdded;
	private static boolean banksTableEdited;
	private static boolean banksTableAdded;
	private static boolean walletTableEdited;*/
	
	public DatabaseManager(Context cxt)
	{
		context=cxt;
		databaseAdapter = new DatabaseAdapter(context);
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
			DatabaseManager.numCountersRows = databaseAdapter.getNumCountersRows();
			
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
				createdTimes = new ArrayList<Time>();
				modifiedTimes = new ArrayList<Time>();
				dates = new ArrayList<Date>();
				types = new ArrayList<String>();
				particulars = new ArrayList<String>();
				rates = new ArrayList<Double>();
				quantities = new ArrayList<Double>();
				amounts = new ArrayList<Double>();
				ArrayList<Transaction> transactions = databaseAdapter.getAllTransactions();
				for(Transaction transaction : transactions)
				{
					createdTimes.add(transaction.getCreatedTime());
					modifiedTimes.add(transaction.getModifiedTime());
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
			
			if(numCountersRows>0)
			{
				counters = databaseAdapter.getAllCountersRows();
			}
			else
			{
				counters = new ArrayList<Counters>();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(context, "Error In Reading Database\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
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
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		databaseAdapter.initializeWalletTable(walletBalance, amountSpent, income);
		
		ArrayList<ExpenditureTypes> expTypes = new ArrayList<ExpenditureTypes>();
		for(int i=0; i<expenditureTypes.size(); i++)
		{
			expTypes.add(new ExpenditureTypes(i, expenditureTypes.get(i)));
		}
		databaseAdapter.addAllExpenditureTypes(expTypes);
		
		//databaseAdapter.initializeCountersTable();
		
		databaseAdapter.close();
	}
	
	public static void saveDatabase()
	{
		if(numTransactions>0)
		{
			ArrayList<Transaction> transactions = new ArrayList<Transaction>();
			for(int i=0; i<numTransactions; i++)
			{
				Transaction transaction=new Transaction(i, createdTimes.get(i), modifiedTimes.get(i), dates.get(i), types.get(i), particulars.get(i),
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
		
		if(numCountersRows>0)
		{
			databaseAdapter.deleteAllCountersRows();
			databaseAdapter.addAllCountersRows(counters);
		}
		
		databaseAdapter.close();
	}
	
	public static void clearDatabase()
	{
		numTransactions=0;
		createdTimes = new ArrayList<Time>();
		modifiedTimes = new ArrayList<Time>();
		dates = new ArrayList<Date>();
		types = new ArrayList<String>();
		particulars = new ArrayList<String>();
		rates = new ArrayList<Double>();
		quantities = new ArrayList<Double>();
		amounts = new ArrayList<Double>();
		
		amountSpent = 0;
		income = 0;
		
		//resetMonthlyCounters();
	}
	
	public static void addTransaction(Transaction transaction)
	{
		if(transaction.getType().contains("Wallet Credit"))
		{
			DatabaseManager.increamentNumTransations();
			DatabaseManager.increamentWalletBalance(transaction.getAmount());
			DatabaseManager.increamentIncome(transaction.getAmount());
			DatabaseManager.addCreatedTime(transaction.getCreatedTime());
			DatabaseManager.addModifiedTime(transaction.getModifiedTime());
			DatabaseManager.addDate(transaction.getDate());
			DatabaseManager.addType(transaction.getType());
			DatabaseManager.addParticular(transaction.getParticular());
			DatabaseManager.addRate(transaction.getAmount());
			DatabaseManager.addQuantity(1);
			DatabaseManager.addAmount(transaction.getAmount());
		}
		else if(transaction.getType().contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(transaction.getType().substring(16, 18));   // Wallet Debit Exp01
			
			DatabaseManager.increamentNumTransations();
			DatabaseManager.decreamentWalletBalance(transaction.getAmount());
			DatabaseManager.increamentAmountSpent(transaction.getAmount());
			DatabaseManager.addCreatedTime(transaction.getCreatedTime());
			DatabaseManager.addModifiedTime(transaction.getModifiedTime());
			DatabaseManager.addDate(transaction.getDate());
			DatabaseManager.addType(transaction.getType());
			DatabaseManager.addParticular(transaction.getParticular());
			DatabaseManager.addRate(transaction.getRate());
			DatabaseManager.addQuantity(transaction.getQuantity());
			DatabaseManager.addAmount(transaction.getAmount());
			DatabaseManager.increamentCounters(transaction.getDate(), expTypeNo, transaction.getAmount());
		}
		else if(transaction.getType().contains("Bank Credit"))
		{
			int bankNo = Integer.parseInt(transaction.getType().substring(12, 14));    // Bank Credit 01 Income
			if(transaction.getType().contains("Income"))   // Bank Credit 01 Income
			{
				DatabaseManager.increamentIncome(transaction.getAmount());
			}
			else if(transaction.getType().contains("Savings"))  // Bank Credit 01 Savings
			{
				DatabaseManager.decreamentWalletBalance(transaction.getAmount());
			}
			
			DatabaseManager.increamentBankBalance(bankNo, transaction.getAmount());
			DatabaseManager.increamentNumTransations();
			DatabaseManager.addCreatedTime(transaction.getCreatedTime());
			DatabaseManager.addModifiedTime(transaction.getModifiedTime());
			DatabaseManager.addDate(transaction.getDate());
			DatabaseManager.addType(transaction.getType());
			DatabaseManager.addParticular(transaction.getParticular());
			DatabaseManager.addRate(transaction.getRate());
			DatabaseManager.addQuantity(1);
			DatabaseManager.addAmount(transaction.getAmount());
		}
		else if(transaction.getType().contains("Bank Debit"))
		{
			int bankNo = Integer.parseInt(transaction.getType().substring(11, 13));  // Bank Debit 01 Exp01
			if(transaction.getType().contains("Withdraw"))   // Bank Debit 01 Withdraw
			{
				DatabaseManager.increamentWalletBalance(transaction.getAmount());
			}
			else if(transaction.getType().contains("Exp"))   // Bank Debit 01 Exp01
			{
				int expTypeNo = Integer.parseInt(transaction.getType().substring(17, 19));
				DatabaseManager.increamentAmountSpent(transaction.getAmount());
				DatabaseManager.increamentCounters(transaction.getDate(), expTypeNo, transaction.getAmount());
			}
			
			DatabaseManager.decreamentBankBalance(bankNo, transaction.getAmount());
			DatabaseManager.increamentNumTransations();
			DatabaseManager.addCreatedTime(transaction.getCreatedTime());
			DatabaseManager.addModifiedTime(transaction.getModifiedTime());
			DatabaseManager.addDate(transaction.getDate());
			DatabaseManager.addType(transaction.getType());
			DatabaseManager.addParticular(transaction.getParticular());
			DatabaseManager.addRate(transaction.getAmount());
			DatabaseManager.addQuantity(1);
			DatabaseManager.addAmount(transaction.getAmount());
		}
	}
	
	/**
	 * Edits an existing transaction
	 * @param transactionNo Number of the transaction
	 * @param data New data for the transaction
	 */
	public static void editTransaction(int transactionNo, Transaction transaction)
	{
		//Time oldCreatedTime = DatabaseManager.getCreatedTime(transactionNo);
		//Time oldModifiedTime = DatabaseManager.getModifiedTime(transactionNo);
		Date oldDate = DatabaseManager.getDate(transactionNo);
		String oldType = DatabaseManager.getType(transactionNo);
		//String oldParticulars = DatabaseManager.getParticular(transactionNo);
		//double oldRate = DatabaseManager.getRate(transactionNo);
		//double oldQuantity = DatabaseManager.getQuantity(transactionNo);
		double oldAmount = DatabaseManager.getAmount(transactionNo);

		//Time newCreatedTime = transaction.getCreatedTime();
		Time newModifiedTime = transaction.getModifiedTime();
		DatabaseManager.setModifiedTime(transactionNo, newModifiedTime);  // Update The Last Modified Time
		Date newDate = transaction.getDate();
		String newType = transaction.getType();
		String newParticulars = transaction.getParticular();
		double newRate = transaction.getRate();
		double newQuantity = transaction.getQuantity();
		double newAmount = transaction.getAmount();
		
		if(transaction.getType().contains("Wallet Credit"))
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
		else if(transaction.getType().contains("Wallet Debit"))
		{
			int oldExpTypeNo = Integer.parseInt(oldType.substring(16,18));
			int newExpTypeNo = Integer.parseInt(newType.substring(16,18));
			
			double netAmount = newAmount - oldAmount;
			DatabaseManager.decreamentCounters(oldDate, oldExpTypeNo, oldAmount);
			DatabaseManager.increamentCounters(newDate, newExpTypeNo, newAmount);
			DatabaseManager.increamentAmountSpent(netAmount);
			DatabaseManager.decreamentWalletBalance(netAmount);
			
			DatabaseManager.setParticular(transactionNo, newParticulars);
			DatabaseManager.setType(transactionNo, newType);
			DatabaseManager.setRate(transactionNo, newRate);
			DatabaseManager.setQuantity(transactionNo, newQuantity);
			DatabaseManager.setAmount(transactionNo, newAmount);
			DatabaseManager.setDate(transactionNo, newDate);
		}
		else if(transaction.getType().contains("Bank Credit"))
		{
			// Determine Which Bank was previous Transaction
			int oldBankNo=Integer.parseInt(oldType.substring(12, 14));    // Bank Credit 01 Income;
			
			/** Determine what type was previous Transaction and undo it. */
			if(oldType.contains("Income"))   // Bank Credit 01 Income
			{
				DatabaseManager.decreamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentIncome(oldAmount);
			}
			else if(oldType.contains("Savings"))   // Bank Credit 01 Savings
			{
				DatabaseManager.decreamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.increamentWalletBalance(oldAmount);
			}
			
			int newBankNo=Integer.parseInt(newType.substring(12, 14));    // Bank Credit 01 Income;
			
			// Make the transaction
			if(oldType.contains("Income"))
			{
				DatabaseManager.increamentIncome(newAmount);
			}
			else if(oldType.contains("Savings"))
			{
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
		else if(transaction.getType().contains("Bank Debit"))
		{
			/** Determine what type was previous Transaction and undo it. */
			// Determine Which Bank
			int oldBankNo=Integer.parseInt(oldType.substring(11, 13));  // Bank Debit 01 Exp01;
			int oldExpTypeNo;
			if(oldType.contains("Withdraw"))   // Bank Debit 01 Withdraw
			{
				DatabaseManager.increamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentWalletBalance(oldAmount);
			}
			else if(oldType.contains("Exp"))   // Bank Debit 01 Exp01
			{
				oldExpTypeNo = Integer.parseInt(transaction.getType().substring(17, 19));
				DatabaseManager.decreamentCounters(oldDate, oldExpTypeNo, oldAmount);
				DatabaseManager.increamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentAmountSpent(oldAmount);
			}
			
			/** Make New Transaction*/
			int newBankNo=Integer.parseInt(newType.substring(11, 13));  // Bank Debit 01 Exp01;
			int newExpTypeNo;
			if(newType.contains("Withdraw"))   // Bank Debit 01 Withdraw
			{
				DatabaseManager.increamentWalletBalance(transaction.getAmount());
				DatabaseManager.decreamentBankBalance(newBankNo, newAmount);
			}
			else if(newType.contains("Exp"))   // Bank Debit 01 Exp01
			{
				newExpTypeNo = Integer.parseInt(transaction.getType().substring(17, 19));
				DatabaseManager.increamentAmountSpent(transaction.getAmount());
				DatabaseManager.increamentCounters(newDate, newExpTypeNo, transaction.getAmount());
				DatabaseManager.decreamentBankBalance(newBankNo, newAmount);
			}
			DatabaseManager.setParticular(transactionNo, newParticulars);
			DatabaseManager.setType(transactionNo, newType);
			DatabaseManager.setRate(transactionNo, newAmount);
			DatabaseManager.setQuantity(transactionNo, 1);
			DatabaseManager.setAmount(transactionNo, newAmount);
			DatabaseManager.setDate(transactionNo, newDate);
		}
	}
	
	public static void deleteTransaction(int transactionNo)
	{
		Date date = DatabaseManager.dates.get(transactionNo);
		String type = DatabaseManager.types.get(transactionNo);
		double amount = DatabaseManager.amounts.get(transactionNo);
		if(type.contains("Wallet Credit"))
		{
			DatabaseManager.decreamentIncome(amount);
			DatabaseManager.decreamentWalletBalance(amount);
		}
		else if(type.contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(type.substring(16, 18));   // Wallet Debit Exp01
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(amount);
			DatabaseManager.decreamentCounters(date, expTypeNo, amount);
		}
		else if(type.contains("Bank Credit"))
		{
			int bankNo = Integer.parseInt(type.substring(12, 14));    // Bank Credit 01 Income
			if(type.contains("Income"))
			{
				DatabaseManager.decreamentIncome(amount);
				DatabaseManager.decreamentBankBalance(bankNo, amount);
			}
			else if(type.contains("Savings"))
			{
				DatabaseManager.increamentWalletBalance(amount);
				DatabaseManager.decreamentBankBalance(bankNo, amount);
			}
		}
		else if(type.contains("Bank Debit"))
		{
			int bankNo = Integer.parseInt(type.substring(11, 13));  // Bank Debit 01 Withdraw
			if(type.contains("Withdraw"))
			{
				DatabaseManager.decreamentWalletBalance(amount);
				DatabaseManager.increamentBankBalance(bankNo, amount);
			}
			else if(type.contains("Exp"))
			{
				int expTypeNo = Integer.parseInt(type.substring(17, 19)); //// Bank Debit 01 Exp01
				DatabaseManager.increamentBankBalance(bankNo, amount);
				DatabaseManager.decreamentAmountSpent(amount);
				DatabaseManager.decreamentCounters(date, expTypeNo, amount);
			}
		}
		
		DatabaseManager.decreamentNumTransactions();
		DatabaseManager.createdTimes.remove(transactionNo);
		DatabaseManager.modifiedTimes.remove(transactionNo);
		DatabaseManager.dates.remove(transactionNo);
		DatabaseManager.types.remove(transactionNo);
		DatabaseManager.particulars.remove(transactionNo);
		DatabaseManager.rates.remove(transactionNo);
		DatabaseManager.quantities.remove(transactionNo);
		DatabaseManager.amounts.remove(transactionNo);
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
	
	public static void increamentCounters(Date date, int expTypeNo, double amount)
	{
		double[] exp = {0.0, 0.0, 0.0, 0.0, 0.0};
		exp[expTypeNo] = amount;
		if(numCountersRows == 0)
		{
			counters = new ArrayList<Counters>();
			counters.add(new Counters(date, exp));
			numCountersRows++;
		}
		else if(date.getLongDate()<counters.get(0).getDate().getLongDate())
		{
			counters.add(0, new Counters(date, exp));
			numCountersRows++;
		}
		else if(date.getLongDate()>counters.get(numCountersRows-1).getDate().getLongDate())
		{
			counters.add(new Counters(date, exp));
			numCountersRows++;
		}
		else
		{
			// Search For The Date Within
			int first = 0;
			int last = numCountersRows-1;
			int middle = (first+last)/2;
			while(first<=last)
			{
				if(counters.get(middle).getDate().getLongDate()<date.getLongDate())
				{
					first = middle+1;
				}
				else if(counters.get(middle).getDate().getLongDate()>date.getLongDate())
				{
					last = middle-1;
				}
				else
				{
					counters.get(middle).increamentCounters(exp);
					break;
				}
				middle = (first + last)/2;
			}
			if(first>last)
			{
				counters.add(middle+1, new Counters(date, exp));   // Insert The New Counters Row.
			}
		}
	}
	
	public static void decreamentCounters(Date date, int expTypeNo, double amount)
	{
		double[] exp = new double[5];
		exp[expTypeNo] = amount;
		
		// Search For The Date
		int first = 0;
		int last = numCountersRows-1;
		int middle = (first+last)/2;
		while(first<=last)
		{
			if(counters.get(middle).getDate().getLongDate()<date.getLongDate())
			{
				first = middle+1;
			}
			else if(counters.get(middle).getDate().getLongDate()>date.getLongDate())
			{
				last = middle-1;
			}
			else
			{
				counters.get(middle).decreamentCounters(exp);
				break;
			}
			middle = (first + last)/2;
		}
	}
	
	public static double[] getMonthlyCounters(long month)
	{
		double[] monthlyCounters = new double[5];
		boolean found = false;
		for(int i=0; i<numCountersRows; i++)
		{
			Counters counter1 = counters.get(i);
			long month1 = (long) Math.floor(counter1.getDate().getLongDate()/100);
			if(!found && month1 < month)			// Required Month not yet found
			{
				i += 10;
				if(i>=numCountersRows)
					i-=10;
			}
			else if(!found && month1 == month)		// Required Month encountered for the first time. 
			{										//previous ones may contain required Month. So, go back
				i -= 10;
				if(i<0)
					i=-1;
				found = true;
			}
			else if(found && month1<month)
			{
				
			}
			else if(found && month1 == month)
			{
				monthlyCounters[0] += counter1.getExp01();
				monthlyCounters[1] += counter1.getExp02();
				monthlyCounters[2] += counter1.getExp03();
				monthlyCounters[3] += counter1.getExp04();
				monthlyCounters[4] += counter1.getExp05();
			}
			else if(month1 > month)
			{
				Toast.makeText(context, "08) "+i, Toast.LENGTH_SHORT).show();
				i = numCountersRows;
				break;
			}
		}
		return monthlyCounters;
	}
	
	public static double[] getTotalCounters()
	{
		double[] totalCounters = new double[5];
		for(int i=0; i<numCountersRows; i++)
		{
			Counters counter1 = counters.get(i);
			totalCounters[0] += counter1.getExp01();
			totalCounters[1] += counter1.getExp02();
			totalCounters[2] += counter1.getExp03();
			totalCounters[3] += counter1.getExp04();
			totalCounters[4] += counter1.getExp05();
		}
		return totalCounters;
	}
	
	public static String getExactExpType(int transactionNo)
	{
		String type = DatabaseManager.getType(transactionNo);
		String expType = "";
		if(type.contains("Wallet Credit"))
		{
			expType = "Income";
		}
		else if(type.contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(type.substring(16, 18));   // Wallet Debit Exp01
			expType = DatabaseManager.getExpenditureType(expTypeNo);
		}
		else if(type.contains("Bank Credit"))
		{
			if(type.contains("Income"))
				expType = "Income";
			else if(type.contains("Savings"))
				expType = "Bank Savings";
		}
		else if(type.contains("Bank Debit"))
		{
			if(type.contains("Withdrawal"))
			{
				expType = "Bank Withdrawal";
			}
			else if(type.contains("Exp"))
			{
				int expTypeNo = Integer.parseInt(type.substring(17, 19)); //// Bank Debit 01 Exp01
				expType = DatabaseManager.getExpenditureType(expTypeNo);
			}
		}
		return expType;
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
	
	public static void setNumCountersRows(int numRows)
	{
		DatabaseManager.numCountersRows = numRows;
	}
	
	public static int getNumCountersRows()
	{
		return DatabaseManager.numCountersRows;
	}
	
	public static void increamentNumCountersRows()
	{
		DatabaseManager.numCountersRows++;
	}
	
	public static void decreamentNumCountersRows()
	{
		DatabaseManager.numCountersRows--;
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
	public static void setAllBankNames(ArrayList<String> bankNames)
	{
		DatabaseManager.bankNames = bankNames;
	}

	/**
	 * @return the bankNames
	 */
	public static ArrayList<String> getAllBankNames()
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
	public static void setAllBankBalances(ArrayList<Double> bankBalances)
	{
		DatabaseManager.bankBalances = bankBalances;
	}

	/**
	 * @return the bankBalances
	 */
	public static ArrayList<Double> getAllBankBalances()
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
	public static void setAllBankSmsNames(ArrayList<String> bankSmsNames)
	{
		DatabaseManager.bankSmsNames = bankSmsNames;
	}

	/**
	 * @return the bankSmsNames
	 */
	public static ArrayList<String> getAllBankSmsNames()
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
	
	// Methods Related To Transaction
	public static void setAllCreatedTimes(ArrayList<Time> createdTimes)
	{
		DatabaseManager.createdTimes = createdTimes;
	}
	
	public static ArrayList<Time> getAllCreatedTimes()
	{
		return DatabaseManager.createdTimes;
	}
	
	public static void setCreatedTime(int transactionNo, Time createdTime)
	{
		DatabaseManager.createdTimes.set(transactionNo, createdTime);
	}
	
	public static Time getCreatedTime(int transactionNo)
	{
		return DatabaseManager.createdTimes.get(transactionNo);
	}
	
	public static void addCreatedTime(Time createdTime)
	{
		if(DatabaseManager.createdTimes == null)
		{
			DatabaseManager.createdTimes = new ArrayList<Time>();
		}
		DatabaseManager.createdTimes.add(createdTime);
	}
	
	public static void deleteCreatedTime(int transactionNo)
	{
		DatabaseManager.createdTimes.remove(transactionNo);
	}
	
	public static void setAllModifiedTimes(ArrayList<Time> modifiedTimes)
	{
		DatabaseManager.modifiedTimes = modifiedTimes;
	}
	
	public static ArrayList<Time> getAllModifiedTimes()
	{
		return DatabaseManager.modifiedTimes;
	}
	
	public static void setModifiedTime(int transactionNo, Time modifiedTime)
	{
		DatabaseManager.modifiedTimes.set(transactionNo, modifiedTime);
	}
	
	public static Time getModifiedTime(int transactionNo)
	{
		return DatabaseManager.modifiedTimes.get(transactionNo);
	}
	
	public static void addModifiedTime(Time modifiedTime)
	{
		if(DatabaseManager.modifiedTimes == null)
		{
			DatabaseManager.modifiedTimes = new ArrayList<Time>();
		}
		DatabaseManager.modifiedTimes.add(modifiedTime);
	}
	
	public static void deleteModifiedTime(int transactionNo)
	{
		DatabaseManager.modifiedTimes.remove(transactionNo);
	}
	
	/**
	 * @param dates the dates to set
	 */
	public static void setAllDates(ArrayList<Date> dates)
	{
		DatabaseManager.dates = dates;
	}

	/**
	 * @return the dates
	 */
	public static ArrayList<Date> getAllDates()
	{
		return dates;
	}
	
	public static void addDate(Date date)
	{
		if(DatabaseManager.dates==null)
		{
			DatabaseManager.dates = new ArrayList<Date>();
		}
		DatabaseManager.dates.add(date);
	}
	
	public static void setDate(int transactionNo, Date date)
	{
		DatabaseManager.dates.set(transactionNo, date);
	}
	
	public static Date getDate(int transactionNo)
	{
		return DatabaseManager.dates.get(transactionNo);
	}

	/**
	 * @param types the types to set
	 */
	public static void setAllTypes(ArrayList<String> types)
	{
		DatabaseManager.types = types;
	}

	/**
	 * @return the types
	 */
	public static ArrayList<String> getAllTypes()
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
	public static void setAllParticulars(ArrayList<String> particulars)
	{
		DatabaseManager.particulars = particulars;
	}

	/**
	 * @return the particulars
	 */
	public static ArrayList<String> getAllParticulars()
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
	public static void setAllRates(ArrayList<Double> rates)
	{
		DatabaseManager.rates = rates;
	}

	/**
	 * @return the rates
	 */
	public static ArrayList<Double> getAllRates()
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
	public static void setAllQuantities(ArrayList<Double> quantities)
	{
		DatabaseManager.quantities = quantities;
	}

	/**
	 * @return the quantities
	 */
	public static ArrayList<Double> getAllQuantities()
	{
		return quantities;
	}
	
	public static void addQuantity(double quantity)
	{
		if(DatabaseManager.quantities == null)
		{
			DatabaseManager.quantities = new ArrayList<Double>();
		}
		DatabaseManager.quantities.add(quantity);
	}
	
	public static void setQuantity(int transactionNo, double quantity)
	{
		DatabaseManager.quantities.set(transactionNo, quantity);
	}
	
	public static void setQuantity(int transactionNo, String quantity)
	{
		DatabaseManager.quantities.set(transactionNo, Double.parseDouble(quantity));
	}
	
	public static double getQuantity(int transactionNo)
	{
		return DatabaseManager.quantities.get(transactionNo);
	}
	
	public static void addQuantity(String quantity)
	{
		if(DatabaseManager.quantities == null)
		{
			DatabaseManager.quantities = new ArrayList<Double>();
		}
		DatabaseManager.quantities.add(Double.parseDouble(quantity));
	}

	/**
	 * @param amounts the amounts to set
	 */
	public static void setAllAmounts(ArrayList<Double> amounts)
	{
		DatabaseManager.amounts = amounts;
	}

	/**
	 * @return the amounts
	 */
	public static ArrayList<Double> getAllAmounts()
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
	
	public static void setAllExpenditureTypes(ArrayList<String> expTypes)
	{
		expenditureTypes=expTypes;
	}
	
	public static ArrayList<String> getAllExpenditureTypes()
	{
		return DatabaseManager.expenditureTypes;
	}
	
	public static void setExpenditureType(int expTypeNo, String expType)
	{
		DatabaseManager.expenditureTypes.set(expTypeNo, expType);
	}
	
	public static String getExpenditureType(int expTypeNo)
	{
		return DatabaseManager.expenditureTypes.get(expTypeNo);
	}

	/*public static void setAllCounters(ArrayList<Double> counters)
	{
		DatabaseManager.counters = counters;
	}
	
	public static ArrayList<Double> getAllCounters()
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
	}*/
}
