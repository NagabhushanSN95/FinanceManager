package com.chaturvedi.financemanager.database;

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
	private static ArrayList<Bank> banks;
	private static ArrayList<Transaction> transactions;
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
			
			if(numBanks>0)
			{
				banks = databaseAdapter.getAllBanks();
			}
			else
			{
				banks = new ArrayList<Bank>();
			}
			
			if(numTransactions>0)
			{
				transactions = databaseAdapter.getAllTransactions();
			}
			else
			{
				transactions = new ArrayList<Transaction>();
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
	
	public static void initialize(double walletBalance)
	{
		numTransactions = 0;
		transactions = new ArrayList<Transaction>();
		
		DatabaseManager.walletBalance = walletBalance;
		databaseAdapter.initializeWalletTable(walletBalance);
		
		numCountersRows = 0;
		counters = new ArrayList<Counters>();
	}
	
	public static void saveDatabase()
	{
		if(numTransactions>0)
		{
			databaseAdapter.deleteAllTransactions();
			databaseAdapter.addAllTransactions(transactions);
		}
		else
		{
			databaseAdapter.deleteAllTransactions();
		}
		
		if(numBanks>0)
		{
			databaseAdapter.deleteAllBanks();
			databaseAdapter.addAllBanks(banks);
		}
		else
		{
			databaseAdapter.deleteAllBanks();
		}
		
		databaseAdapter.setWalletBalance(walletBalance);
		
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
		else
		{
			databaseAdapter.deleteAllCountersRows();
		}
		
		databaseAdapter.close();
	}
	
	public static void clearDatabase()
	{
		numTransactions=0;
		transactions = new ArrayList<Transaction>();
		databaseAdapter.deleteAllTransactions();
		
		numCountersRows = 0;
		counters = new ArrayList<Counters>();
		databaseAdapter.deleteAllCountersRows();
	}
	
	public static void addTransaction(Transaction transaction)
	{
		DatabaseManager.transactions.add(transaction);
		databaseAdapter.addTransaction(transaction);
		
		if(transaction.getType().contains("Wallet Credit"))
		{
			DatabaseManager.increamentNumTransations();
			DatabaseManager.increamentWalletBalance(transaction.getAmount());
			DatabaseManager.increamentIncome(transaction.getDate(), transaction.getAmount());
		}
		else if(transaction.getType().contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(transaction.getType().substring(16, 18));   // Wallet Debit Exp01
			
			DatabaseManager.increamentNumTransations();
			DatabaseManager.decreamentWalletBalance(transaction.getAmount());
			DatabaseManager.increamentAmountSpent(transaction.getDate(), transaction.getAmount());
			DatabaseManager.increamentCounters(transaction.getDate(), expTypeNo, transaction.getAmount());
		}
		else if(transaction.getType().contains("Bank Credit"))
		{
			int bankNo = Integer.parseInt(transaction.getType().substring(12, 14));    // Bank Credit 01 Income
			if(transaction.getType().contains("Income"))   // Bank Credit 01 Income
			{
				DatabaseManager.increamentIncome(transaction.getDate(), transaction.getAmount());
			}
			else if(transaction.getType().contains("Savings"))  // Bank Credit 01 Savings
			{
				DatabaseManager.decreamentWalletBalance(transaction.getAmount());
				DatabaseManager.increamentSavings(transaction.getDate(), transaction.getAmount());
			}
			
			DatabaseManager.increamentBankBalance(bankNo, transaction.getAmount());
			DatabaseManager.increamentNumTransations();
		}
		else if(transaction.getType().contains("Bank Debit"))
		{
			int bankNo = Integer.parseInt(transaction.getType().substring(11, 13));  // Bank Debit 01 Exp01
			if(transaction.getType().contains("Withdraw"))   // Bank Debit 01 Withdraw
			{
				DatabaseManager.increamentWalletBalance(transaction.getAmount());
				DatabaseManager.increamentWithdrawal(transaction.getDate(), transaction.getAmount());
			}
			else if(transaction.getType().contains("Exp"))   // Bank Debit 01 Exp01
			{
				int expTypeNo = Integer.parseInt(transaction.getType().substring(17, 19));
				DatabaseManager.increamentAmountSpent(transaction.getDate(), transaction.getAmount());
				DatabaseManager.increamentCounters(transaction.getDate(), expTypeNo, transaction.getAmount());
			}
			
			DatabaseManager.decreamentBankBalance(bankNo, transaction.getAmount());
			DatabaseManager.increamentNumTransations();
		}
	}

	public static void setAllTransactions(ArrayList<Transaction> transactions)
	{
		DatabaseManager.transactions = transactions;
	}
	
	/**
	 * Edits an existing transaction
	 * @param transactionNo Number of the transaction
	 * @param data New data for the transaction
	 */
	public static void editTransaction(Transaction oldTransaction, Transaction newTransaction)
	{
		int transactionNo = transactions.indexOf(oldTransaction);
		
		Date oldDate = oldTransaction.getDate();
		String oldType = oldTransaction.getType();
		double oldAmount = oldTransaction.getAmount();

		Date newDate = newTransaction.getDate();
		String newType = newTransaction.getType();
		double newAmount = newTransaction.getAmount();
		
		if(newTransaction.getType().contains("Wallet Credit"))
		{
			double netAmount = newAmount-oldAmount;
			DatabaseManager.decreamentIncome(oldDate, oldAmount);
			DatabaseManager.increamentIncome(newDate, newAmount);
			DatabaseManager.increamentWalletBalance(netAmount);
		}
		else if(newTransaction.getType().contains("Wallet Debit"))
		{
			int oldExpTypeNo = Integer.parseInt(oldType.substring(16,18));
			int newExpTypeNo = Integer.parseInt(newType.substring(16,18));
			
			double netAmount = newAmount - oldAmount;
			DatabaseManager.decreamentCounters(oldDate, oldExpTypeNo, oldAmount);
			DatabaseManager.increamentCounters(newDate, newExpTypeNo, newAmount);
			DatabaseManager.decreamentAmountSpent(oldDate, oldAmount);
			DatabaseManager.increamentAmountSpent(newDate, newAmount);
			DatabaseManager.decreamentWalletBalance(netAmount);
		}
		else if(newTransaction.getType().contains("Bank Credit"))
		{
			// Determine Which Bank was previous Transaction
			int oldBankNo=Integer.parseInt(oldType.substring(12, 14));    // Bank Credit 01 Income;
			
			/** Determine what type was previous Transaction and undo it. */
			if(oldType.contains("Income"))   // Bank Credit 01 Income
			{
				DatabaseManager.decreamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentIncome(oldDate, oldAmount);
			}
			else if(oldType.contains("Savings"))   // Bank Credit 01 Savings
			{
				DatabaseManager.decreamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.increamentWalletBalance(oldAmount);
				DatabaseManager.decreamentSavings(oldDate, oldAmount);
			}
			
			int newBankNo=Integer.parseInt(newType.substring(12, 14));    // Bank Credit 01 Income;
			
			// Make the transaction
			if(oldType.contains("Income"))
			{
				DatabaseManager.increamentIncome(newDate, newAmount);
			}
			else if(oldType.contains("Savings"))
			{
				DatabaseManager.decreamentWalletBalance(newAmount);
				DatabaseManager.increamentSavings(newDate, newAmount);
			}
			DatabaseManager.increamentBankBalance(newBankNo, newAmount);
		}
		else if(newTransaction.getType().contains("Bank Debit"))
		{
			/** Determine what type was previous Transaction and undo it. */
			// Determine Which Bank
			int oldBankNo=Integer.parseInt(oldType.substring(11, 13));  // Bank Debit 01 Exp01;
			int oldExpTypeNo;
			if(oldType.contains("Withdraw"))   // Bank Debit 01 Withdraw
			{
				DatabaseManager.increamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentWalletBalance(oldAmount);
				DatabaseManager.decreamentWithdrawal(oldDate, oldAmount);
			}
			else if(oldType.contains("Exp"))   // Bank Debit 01 Exp01
			{
				oldExpTypeNo = Integer.parseInt(newTransaction.getType().substring(17, 19));
				DatabaseManager.decreamentCounters(oldDate, oldExpTypeNo, oldAmount);
				DatabaseManager.increamentBankBalance(oldBankNo, oldAmount);
				DatabaseManager.decreamentAmountSpent(oldDate, oldAmount);
			}
			
			/** Make New Transaction*/
			int newBankNo=Integer.parseInt(newType.substring(11, 13));  // Bank Debit 01 Exp01;
			int newExpTypeNo;
			if(newType.contains("Withdraw"))   // Bank Debit 01 Withdraw
			{
				DatabaseManager.increamentWalletBalance(newTransaction.getAmount());
				DatabaseManager.decreamentBankBalance(newBankNo, newAmount);
				DatabaseManager.increamentWithdrawal(newDate, newAmount);
			}
			else if(newType.contains("Exp"))   // Bank Debit 01 Exp01
			{
				newExpTypeNo = Integer.parseInt(newTransaction.getType().substring(17, 19));
				DatabaseManager.increamentAmountSpent(newDate, newAmount);
				DatabaseManager.increamentCounters(newDate, newExpTypeNo, newAmount);
				DatabaseManager.decreamentBankBalance(newBankNo, newAmount);
			}
		}
		newTransaction.setID(transactionNo+1);
		newTransaction.setCreatedTime(transactions.get(transactionNo).getCreatedTime());
		transactions.set(transactionNo, newTransaction);
		databaseAdapter.updateTransaction(newTransaction);
	}
	
	public static void deleteTransaction(Transaction transaction)
	{
		int transactionNo = transactions.indexOf(transaction);
		Date date = transactions.get(transactionNo).getDate();
		String type = transactions.get(transactionNo).getType();
		double amount = transactions.get(transactionNo).getAmount();
		if(type.contains("Wallet Credit"))
		{
			DatabaseManager.decreamentIncome(date, amount);
			DatabaseManager.decreamentWalletBalance(amount);
		}
		else if(type.contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(type.substring(16, 18));   // Wallet Debit Exp01
			DatabaseManager.increamentWalletBalance(amount);
			DatabaseManager.decreamentAmountSpent(date, amount);
			DatabaseManager.decreamentCounters(date, expTypeNo, amount);
		}
		else if(type.contains("Bank Credit"))
		{
			int bankNo = Integer.parseInt(type.substring(12, 14));    // Bank Credit 01 Income
			if(type.contains("Income"))
			{
				DatabaseManager.decreamentIncome(date, amount);
				DatabaseManager.decreamentBankBalance(bankNo, amount);
			}
			else if(type.contains("Savings"))
			{
				DatabaseManager.increamentWalletBalance(amount);
				DatabaseManager.decreamentBankBalance(bankNo, amount);
				DatabaseManager.decreamentSavings(date, amount);
			}
		}
		else if(type.contains("Bank Debit"))
		{
			int bankNo = Integer.parseInt(type.substring(11, 13));  // Bank Debit 01 Withdraw
			if(type.contains("Withdraw"))
			{
				DatabaseManager.decreamentWalletBalance(amount);
				DatabaseManager.increamentBankBalance(bankNo, amount);
				DatabaseManager.decreamentWithdrawal(date, amount);
			}
			else if(type.contains("Exp"))
			{
				int expTypeNo = Integer.parseInt(type.substring(17, 19)); 	// Bank Debit 01 Exp01
				DatabaseManager.increamentBankBalance(bankNo, amount);
				DatabaseManager.decreamentAmountSpent(date, amount);
				DatabaseManager.decreamentCounters(date, expTypeNo, amount);
			}
		}
		
		DatabaseManager.decreamentNumTransactions();
		DatabaseManager.transactions.remove(transactionNo);
		transaction.setID(transactionNo + 1);
		databaseAdapter.deleteTransaction(transaction);
	}
	
	public static ArrayList<Transaction> getAllTransactions()
	{
		return transactions;
	}
	
	public static ArrayList<Transaction> getMonthlyTransactions(long month)
	{
		ArrayList<Transaction> transactions1 = new ArrayList<Transaction>();
		
		for(Transaction transaction : DatabaseManager.transactions)
		{
			long month1 = (long) Math.floor(transaction.getDate().getLongDate()/100);
			if(month1 == month)
			{
				transactions1.add(transaction);
			}
		}
		return transactions1;
	}
	
	public static void addBank(Bank bank)
	{
		DatabaseManager.increamentNumBanks();
		banks.add(bank);
		databaseAdapter.addBank(bank);
	}
	
	public static void editBank(int bankNum, Bank bank)
	{
		banks.set(bankNum, bank);
		databaseAdapter.updateBank(bank);
	}
	
	public static void deleteBank(int bankNum)
	{
		DatabaseManager.decreamentNumBanks();
		banks.remove(bankNum);
		databaseAdapter.deleteBank(banks.get(bankNum));
	}

	public static void setAllCounters(ArrayList<Counters> counters)
	{
		DatabaseManager.counters = counters;
	}
	
	public static ArrayList<Counters> getAllCounters()
	{
		return DatabaseManager.counters;
	}
	
	public static void increamentCounters(Date date, int expTypeNo, double amount)
	{
		double[] exp = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		exp[expTypeNo] = amount;
		if(numCountersRows == 0)
		{
			counters = new ArrayList<Counters>();
			counters.add(new Counters(date, exp));
			numCountersRows++;
			
			Counters counter = new Counters(date, exp);
			counter.setID(1);
			databaseAdapter.addCountersRow(counter);
		}
		else if(date.getLongDate()<counters.get(0).getDate().getLongDate())
		{
			counters.add(0, new Counters(date, exp));
			numCountersRows++;
			
			Counters counter = new Counters(date, exp);
			counter.setID(1);
			databaseAdapter.insertCountersRow(counter);
		}
		else if(date.getLongDate()>counters.get(numCountersRows-1).getDate().getLongDate())
		{
			counters.add(new Counters(date, exp));
			numCountersRows++;
			
			Counters counter = new Counters(date, exp);
			counter.setID(numCountersRows);			// Which has been increamented already
			databaseAdapter.addCountersRow(counter);
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
					
					Counters counter = counters.get(middle);
					counter.setID(middle + 1);
					databaseAdapter.updateCountersRow(counter);
					break;
				}
				middle = (first + last)/2;
			}
			if(first>last)
			{
				counters.add(middle+1, new Counters(date, exp));   // Insert The New Counters Row.
				
				Counters counter = new Counters(date, exp);
				counter.setID(middle + 2);
				databaseAdapter.insertCountersRow(counter);
			}
		}
	}
	
	public static void decreamentCounters(Date date, int expTypeNo, double amount)
	{
		double[] exp = new double[9];
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
				
				Counters counter = counters.get(middle);
				counter.setID(middle + 1);
				databaseAdapter.updateCountersRow(counter);
				break;
			}
			middle = (first + last)/2;
		}
	}
	
	public static double[] getMonthlyCounters(long month)
	{
		double[] monthlyCounters = new double[9];
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
				monthlyCounters[5] += counter1.getAmountSpent();
				monthlyCounters[6] += counter1.getIncome();
				monthlyCounters[7] += counter1.getSavings();
				monthlyCounters[8] += counter1.getWithdrawal();
			}
			else if(month1 > month)
			{
				i = numCountersRows;
				break;
			}
		}
		return monthlyCounters;
	}
	
	public static double[] getTotalCounters()
	{
		double[] totalCounters = new double[9];
		for(int i=0; i<numCountersRows; i++)
		{
			Counters counter1 = counters.get(i);
			totalCounters[0] += counter1.getExp01();
			totalCounters[1] += counter1.getExp02();
			totalCounters[2] += counter1.getExp03();
			totalCounters[3] += counter1.getExp04();
			totalCounters[4] += counter1.getExp05();
			totalCounters[5] += counter1.getAmountSpent();
			totalCounters[6] += counter1.getIncome();
			totalCounters[7] += counter1.getSavings();
			totalCounters[8] += counter1.getWithdrawal();
		}
		return totalCounters;
	}
	
	public static ArrayList<String> getExportableMonths()
	{
		ArrayList<Long> months1 = new ArrayList<Long>();
		ArrayList<String> months = new ArrayList<String>();
		
		for(int i=0; i<numTransactions; i++)
		{
			long month1 = (long) transactions.get(i).getDate().getLongDate()/100;
			if(!months1.contains(month1))
			{
				months1.add(month1);
			}
		}
		
		for(int i=0; i<months1.size(); i++)
		{
			months.add(getMonth(months1.get(i)));
		}
		return months;
	}
	
	private static String getMonth(long fullMonth)
	{
		int month = (int) (fullMonth % 100);
		int year = (int) (fullMonth/100);
		switch(month)
		{
			case 1:
				return "January - " + year;
				
			case 2:
				return "February - " + year;
				
			case 3:
				return "March - " + year;
				
			case 4:
				return "April - " + year;
				
			case 5:
				return "May - " + year;
				
			case 6:
				return "June - " + year;
				
			case 7:
				return "July - " + year;
				
			case 8:
				return "August - " + year;
				
			case 9:
				return "September - " + year;
				
			case 10:
				return "October - " + year;
				
			case 11:
				return "November - " + year;
				
			case 12:
				return "December - " + year;
				
			default:
				return (fullMonth + "");
		}
	}
	
	public static String getExactExpType(int transactionNo)
	{
		String type = transactions.get(transactionNo).getType();
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
		databaseAdapter.setWalletBalance(walletBalance);
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
		databaseAdapter.setWalletBalance(walletBalance);
	}
	
	public static void increamentWalletBalance(String amount)
	{
		walletBalance+=Double.parseDouble(amount);
		databaseAdapter.setWalletBalance(walletBalance);
	}
	
	public static void decreamentWalletBalance(double amount)
	{
		walletBalance-=amount;
		databaseAdapter.setWalletBalance(walletBalance);
	}
	
	public static void decreamentWalletBalance(String amount)
	{
		walletBalance-=Double.parseDouble(amount);
		databaseAdapter.setWalletBalance(walletBalance);
	}
	
	public static void increamentAmountSpent(Date date, double amount)
	{
		// Amount Spent is the 6th column
		DatabaseManager.increamentCounters(date, 5, amount);
	}
	
	public static void decreamentAmountSpent(Date date, double amount)
	{
		// Amount Spent is the 6th column
		DatabaseManager.decreamentCounters(date, 5, amount);
	}
	
	public static double getMonthlyAmountSpent(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Amount Spent is the 6th Column
		return counters[5];
	}
	
	public static double getTotalAmountSpent()
	{
		double counters[] = getTotalCounters();
		// Amount Spent is the 6th Column
		return counters[5];
	}
	
	public static void increamentIncome(Date date, double amount)
	{
		// Income is the 7th column
		DatabaseManager.increamentCounters(date, 6, amount);
	}
	
	public static void decreamentIncome(Date date, double amount)
	{
		// Income is the 7th column
		DatabaseManager.decreamentCounters(date, 6, amount);
	}
	
	public static double getMonthlyIncome(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Income is the 7th Column
		return counters[6];
	}
	
	public static double getTotalIncome()
	{
		double counters[] = getTotalCounters();
		// Income is the 7th Column
		return counters[6];
	}
	
	public static void increamentSavings(Date date, double amount)
	{
		// Savings is the 8th column
		DatabaseManager.increamentCounters(date, 7, amount);
	}
	
	public static void decreamentSavings(Date date, double amount)
	{
		// Savings is the 8th column
		DatabaseManager.decreamentCounters(date, 7, amount);
	}
	
	public static double getMonthlySavings(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Savings is the 8th Column
		return counters[7];
	}
	
	public static double getTotalSavings()
	{
		double counters[] = getTotalCounters();
		// Savings is the 8th Column
		return counters[7];
	}
	
	public static void increamentWithdrawal(Date date, double amount)
	{
		// Withdrawal is the 9th column
		DatabaseManager.increamentCounters(date, 8, amount);
	}
	
	public static void decreamentWithdrawal(Date date, double amount)
	{
		// Withdrawal is the 9th column
		DatabaseManager.decreamentCounters(date, 8, amount);
	}
	
	public static double getMonthlyWithdrawal(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Withdrawal is the 9th column
		return counters[8];
	}
	
	public static double getTotalWithdrawal()
	{
		double counters[] = getTotalCounters();
		// Withdrawal is the 9th column
		return counters[8];
	}
	
	public static void setAllBanks(ArrayList<Bank> banks)
	{
		DatabaseManager.banks = banks;
		databaseAdapter.deleteAllBanks();
		databaseAdapter.addAllBanks(banks);
	}
	
	public static ArrayList<Bank> getAllBanks()
	{
		return banks;
	}
	
	public static Bank getBank(int bankNo)
	{
		return banks.get(bankNo);
	}
	
	public static void increamentBankBalance(int bankNum, double amount)
	{
		banks.get(bankNum).increamentBanlance(amount);
	}
	
	public static void decreamentBankBalance(int bankNum, double amount)
	{
		banks.get(bankNum).decreamentBanlance(amount);
	}
	
	public static void setAllExpenditureTypes(ArrayList<String> expTypes1)
	{
		expenditureTypes=expTypes1;
		
		ArrayList<ExpenditureTypes> expTypes = new ArrayList<ExpenditureTypes>();
		for(int i=0; i<expenditureTypes.size(); i++)
		{
			expTypes.add(new ExpenditureTypes(i, expenditureTypes.get(i)));
		}
		databaseAdapter.deleteAllExpenditureTypes();
		databaseAdapter.addAllExpenditureTypes(expTypes);
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
}