package com.chaturvedi.financemanager.database;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.content.Context;
import android.widget.Toast;

public class DatabaseManager
{
	private static Context context;
	private static DatabaseAdapter databaseAdapter;
	
	private static double walletBalance;
	private static ArrayList<Bank> banks;
	private static ArrayList<Transaction> transactions;
	private static ArrayList<String> expenditureTypes;
	private static ArrayList<Counters> counters;
	private static ArrayList<Template> templates;
	
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
	
	public static void initialize(double walletBalance)
	{
		transactions = new ArrayList<Transaction>();
		DatabaseManager.walletBalance = walletBalance;
		databaseAdapter.initializeWalletTable(walletBalance);
		counters = new ArrayList<Counters>();
		templates = new ArrayList<Template>(); 
	}
	
	public static void readDatabase()
	{
		try
		{
			setWalletBalance(databaseAdapter.getWalletBalance());

			int numBanks = databaseAdapter.getNumBanks();
			if(numBanks>0)
			{
				banks = databaseAdapter.getAllBanks();
			}
			else
			{
				banks = new ArrayList<Bank>();
			}

			int numTransactions = databaseAdapter.getNumTransactions();
			if(numTransactions>0)
			{
				transactions = databaseAdapter.getAllTransactions();
			}
			else
			{
				transactions = new ArrayList<Transaction>();
			}
			
			expenditureTypes = databaseAdapter.getAllExpTypes();
			
			int numCountersRows = databaseAdapter.getNumCountersRows();
			if(numCountersRows>0)
			{
				counters = databaseAdapter.getAllCountersRows();
			}
			else
			{
				counters = new ArrayList<Counters>();
			}
			
			int numTemplates = databaseAdapter.getNumTemplates();
			if(numTemplates>0)
			{
				templates = databaseAdapter.getAllTemplates();
			}
			else
			{
				templates = new ArrayList<Template>();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(context, "Error In Reading Database\nDatabaseManager/readDatabase\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	public static void clearDatabase()
	{
		transactions = new ArrayList<Transaction>();
		databaseAdapter.deleteAllTransactions();
		counters = new ArrayList<Counters>();
		databaseAdapter.deleteAllCountersRows();
	}

	/**
	 * @return the numTransactions
	 */
	public static int getNumTransactions()
	{
		return transactions.size();
	}
	
	public static void addTransaction(Transaction transaction)
	{
		
		if(transaction.getType().contains("Wallet Credit"))
		{
			DatabaseManager.increamentWalletBalance(transaction.getAmount());
			DatabaseManager.increamentIncome(transaction.getDate(), transaction.getAmount());
		}
		else if(transaction.getType().contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(transaction.getType().substring(16, 18));   // Wallet Debit Exp01
			
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
			
			// Save the new Bank Balance in Database
			Bank bank = banks.get(bankNo);
			databaseAdapter.updateBank(bank);
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
			
			// Save the new Bank Balance in Database
			Bank bank = banks.get(bankNo);
			databaseAdapter.updateBank(bank);
		}
		
		transaction.setID(transactions.size()+1);
		DatabaseManager.transactions.add(transaction);
		databaseAdapter.addTransaction(transaction);
	}

	public static void setAllTransactions(ArrayList<Transaction> transactions)
	{
		DatabaseManager.transactions = transactions;
		databaseAdapter.deleteAllTransactions();
		databaseAdapter.addAllTransactions(transactions);
	}
	
	/**
	 * Edits an existing transaction
	 * @param oldTransaction Transaction Object for Old Transaction
	 * @param newTransaction Transaction Object for Edited Transaction
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
			
			DatabaseManager.decreamentCounters(oldDate, oldExpTypeNo, oldAmount);
			DatabaseManager.increamentCounters(newDate, newExpTypeNo, newAmount);
			DatabaseManager.decreamentAmountSpent(oldDate, oldAmount);
			DatabaseManager.increamentAmountSpent(newDate, newAmount);
			//double netAmount = newAmount - oldAmount;
			//DatabaseManager.increamentWalletBalance(netAmount);
			DatabaseManager.increamentWalletBalance(oldAmount);
			DatabaseManager.decreamentWalletBalance(newAmount);
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
			if(newType.contains("Income"))
			{
				DatabaseManager.increamentIncome(newDate, newAmount);
			}
			else if(newType.contains("Savings"))
			{
				DatabaseManager.decreamentWalletBalance(newAmount);
				DatabaseManager.increamentSavings(newDate, newAmount);
			}
			DatabaseManager.increamentBankBalance(newBankNo, newAmount);
			
			// Save the new Bank Balance in Database
			Bank oldBank = banks.get(oldBankNo);
			databaseAdapter.updateBank(oldBank);
			Bank newBank = banks.get(newBankNo);
			databaseAdapter.updateBank(newBank);
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
			
			// Save the new Bank Balance in Database
			Bank oldBank = banks.get(oldBankNo);
			databaseAdapter.updateBank(oldBank);
			Bank newBank = banks.get(newBankNo);
			databaseAdapter.updateBank(newBank);
		}
		//newTransaction.setID(oldTransaction.getID());
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
			
			// Save the new Bank Balance in Database
			Bank bank = banks.get(bankNo);
			databaseAdapter.updateBank(bank);
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
			
			// Save the new Bank Balance in Database
			Bank bank = banks.get(bankNo);
			databaseAdapter.updateBank(bank);
		}
		
		//DatabaseManager.decreamentNumTransactions();
		DatabaseManager.transactions.remove(transactionNo);
		databaseAdapter.deleteTransaction(transaction);
		for(int i=transactionNo; i<transactions.size(); i++)
		{
			transactions.get(i).setID(transactions.get(i).getID()-1);
		}
	}
	
	public static ArrayList<Transaction> getAllTransactions()
	{
		return transactions;
	}
	
	public static ArrayList<Transaction> getYearlyTransactions(long year)
	{
		ArrayList<Transaction> transactions1 = new ArrayList<Transaction>();
		
		for(Transaction transaction : DatabaseManager.transactions)
		{
			long year1 = (long) Math.floor(transaction.getDate().getLongDate()/10000);
			if(year1 == year)
			{
				transactions1.add(transaction);
			}
		}
		return transactions1;
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
	
	/**
	 * Returns transactions with the given parameters
	 * @param interval: What is the interval of transactions e.g. year-2015 or month-201508
	 * @param types: Types of transactions e.g. incomes or {bank savings, exp04}
	 * @param sortOrder: Sorting Type and Order e.g. date-ascending or amount-descending
	 * @return
	 */
	public static ArrayList<Transaction> getTransactions(String interval, ArrayList<String> types, String sortOrder)
	{
		ArrayList<Transaction> transactions1 = new ArrayList<Transaction>();

		Toast.makeText(context, "Check-Point 01", Toast.LENGTH_SHORT).show();
		StringTokenizer tokens = new StringTokenizer(interval, "-");
		String interval1 = tokens.nextToken();
		if(interval1.equals("all"))
		{
			Toast.makeText(context, "Check-Point 02:" + types.size(), Toast.LENGTH_SHORT).show();
			for(Transaction transaction : DatabaseManager.transactions)
			{
				for(String expType: types)
				{
					if(transaction.getType().equals(expType))
					{
						transactions1.add(new Transaction(transaction));
					}
				}
				Toast.makeText(context, "Check-Point 03:"+transactions1.size(), Toast.LENGTH_SHORT).show();
			}
		}
		else if(interval1.equals("month"))
		{
			long month = Long.parseLong(tokens.nextToken());
			for(Transaction transaction : DatabaseManager.transactions)
			{
				long month1 = (long) Math.floor(transaction.getDate().getLongDate()/100);
				if(month1 == month)
				{
					for(String expType: types)
					{
						if(transaction.getType().equals(expType))
						{
							transactions1.add(new Transaction(transaction));
						}
					}
				}
			}
		}
		else if(interval1.equals("year"))
		{
			long year = Long.parseLong(tokens.nextToken());
			for(Transaction transaction : DatabaseManager.transactions)
			{
				long year1 = (long) Math.floor(transaction.getDate().getLongDate()/10000);
				if(year1 == year)
				{
					for(String expType: types)
					{
						if(transaction.getType().equals(expType))
						{
							transactions1.add(new Transaction(transaction));
						}
					}
				}
			}
		}
		return transactions1;
	}
	
	public static void addTemplate(Template template)
	{
		int numTemplates = templates.size();
		if(numTemplates == 0)
		{
			templates = new ArrayList<Template>();
			template.setID(1);
			templates.add(template);
			databaseAdapter.addTemplate(template);
		}
		else if(template.getParticular().compareTo(templates.get(0).getParticular()) < 0)
		{
			template.setID(1);
			templates.add(0, template);
			for(int i=1; i<templates.size();i++)// Update IDs of the templates below
			{
				Template tempTemplate = templates.get(i);
				tempTemplate.setID(tempTemplate.getID()+1);
			}
			
			databaseAdapter.insertTemplate(template);
		}
		else if(template.getParticular().compareTo(templates.get(numTemplates-1).getParticular()) > 0)
		{
			template.setID(numTemplates+1);
			templates.add(template);
			databaseAdapter.addTemplate(template);
		}
		else
		{
			// Search For The Template Within
			int first = 0;
			int last = numTemplates-1;
			int middle = (first+last)/2;
			while(first<=last)
			{
				if(templates.get(middle).getParticular().compareTo(template.getParticular()) < 0)
				{
					first = middle+1;
				}
				else if(templates.get(middle).getParticular().compareTo(template.getParticular()) > 0)
				{
					last = middle-1;
				}
				else
				{
					template.setID(middle+1);
					templates.set(middle, template);
					databaseAdapter.updateTemplate(template);
					Toast.makeText(context, "Existing Template Updated", Toast.LENGTH_SHORT).show();
					break;
				}
				middle = (first + last)/2;
			}
			if(first>last)
			{
				template.setID(middle+2);
				templates.add(middle+1, template);		   // Insert The New Counters Row.
				for(int i=middle+2; i<templates.size();i++)// Update IDs of the templates below
				{
					Template tempTemplate = templates.get(i);
					tempTemplate.setID(tempTemplate.getID()+1);
				}
				databaseAdapter.insertTemplate(template);
			}
		}
	}

	public static void setAllTemplates(ArrayList<Template> templates)
	{
		DatabaseManager.templates = templates;
		databaseAdapter.deleteAllTemplates();
		databaseAdapter.addAllTemplates(templates);
	}
	
	public static void deleteTemplate(Template template)
	{
		int templateNo = templates.indexOf(template);
		DatabaseManager.templates.remove(templateNo);
		// Update the IDs of the remaining (below) Templates
		for(int i=templateNo; i<templates.size(); i++)
		{
			templates.get(i).setID(templates.get(i).getID()-1);
		}
		databaseAdapter.deleteTemplate(template);
	}
	
	public static ArrayList<Template> getAllTemplates()
	{
		return templates;
	}

	/**
	 * @return the numBanks
	 */
	public static int getNumBanks()
	{
		return banks.size();
	}
	
	public static void addBank(Bank bank)
	{
		banks.add(bank);
		databaseAdapter.addBank(bank);
	}
	
	public static void setAllBanks(ArrayList<Bank> banks)
	{
		DatabaseManager.banks = banks;
		databaseAdapter.deleteAllBanks();
		databaseAdapter.addAllBanks(banks);
	}
	
	public static void editBank(int bankNum, Bank bank)
	{
		banks.set(bankNum, bank);
		databaseAdapter.updateBank(bank);
	}
	
	public static Bank getBank(int bankNo)
	{
		return banks.get(bankNo);
	}
	
	public static ArrayList<Bank> getAllBanks()
	{
		return banks;
	}
	
	public static void deleteBank(int bankNum)
	{
		banks.remove(bankNum);
		databaseAdapter.deleteBank(banks.get(bankNum));
	}
	
	public static void increamentBankBalance(int bankNum, double amount)
	{
		banks.get(bankNum).increamentBanlance(amount);
	}
	
	public static void decreamentBankBalance(int bankNum, double amount)
	{
		banks.get(bankNum).decreamentBanlance(amount);
	}
	
	public static int getNumExpTypes()
	{
		return expenditureTypes.size();
	}
	
	/**
	 * Adds a new Expenditure Type
	 * @param expTypeName
	 * @param position 1 for 1st position and so on
	 */
	public static void addExpType(String expTypeName, int position)
	{
		position = position-1;	// Make it start from 0
		expenditureTypes.add(position, expTypeName);
		
		DecimalFormat formatter = new DecimalFormat("00");
		// Update Exp Types of all Transactions
		for(Transaction transaction: transactions)
		{
			if(transaction.getType().contains("Wallet Credit"))
			{
				
			}
			else if(transaction.getType().contains("Wallet Debit"))
			{
				int expTypeNo = Integer.parseInt(transaction.getType().substring(16, 18));   // Wallet Debit Exp01
				if(expTypeNo >= position)
				{
					transaction.setType("Wallet Debit Exp" + formatter.format(expTypeNo+1));
				}
			}
			else if(transaction.getType().contains("Bank Credit"))
			{
				
			}
			else if(transaction.getType().contains("Bank Debit"))
			{
				int bankNo = Integer.parseInt(transaction.getType().substring(11, 13));  // Bank Debit 01 Exp01
				if(transaction.getType().contains("Withdraw"))   // Bank Debit 01 Withdraw
				{
				
				}
				else if(transaction.getType().contains("Exp"))   // Bank Debit 01 Exp01
				{
					int expTypeNo = Integer.parseInt(transaction.getType().substring(17, 19));
					if(expTypeNo >= position)
					{
						transaction.setType("Bank Debit " + formatter.format(bankNo) + " Exp" + formatter.format(expTypeNo+1));
					}
				}
			}
		}
		
		// Update Exp Types of all Templates
		for(Template template: templates)
		{
			if(template.getType().contains("Wallet Credit"))
			{
				
			}
			else if(template.getType().contains("Wallet Debit"))
			{
				int expTypeNo = Integer.parseInt(template.getType().substring(16, 18));   // Wallet Debit Exp01
				if(expTypeNo >= position)
				{
					template.setType("Wallet Debit Exp" + formatter.format(expTypeNo+1));
				}
			}
			else if(template.getType().contains("Bank Credit"))
			{
				
			}
			else if(template.getType().contains("Bank Debit"))
			{
				int bankNo = Integer.parseInt(template.getType().substring(11, 13));  // Bank Debit 01 Exp01
				if(template.getType().contains("Withdraw"))   // Bank Debit 01 Withdraw
				{
				
				}
				else if(template.getType().contains("Exp"))   // Bank Debit 01 Exp01
				{
					int expTypeNo = Integer.parseInt(template.getType().substring(17, 19));
					if(expTypeNo >= position)
					{
						template.setType("Bank Debit " + formatter.format(bankNo) + " Exp" + formatter.format(expTypeNo+1));
					}
				}
			}
		}
		
		// Update Counters
		for(Counters counter: counters)
		{
			counter.addNewExpToCounters(position);
		}
		
		// Update the same in Database
		databaseAdapter.deleteAllTransactions();
		databaseAdapter.addAllTransactions(transactions);
		databaseAdapter.deleteAllTemplates();
		databaseAdapter.addAllTemplates(templates);
		databaseAdapter.deleteAllExpTypes();
		databaseAdapter.addAllExpTypes(expenditureTypes);
		databaseAdapter.readjustCountersTable();
		databaseAdapter.addAllCountersRows(counters);
	}
	
	public static void setExpenditureType(int expTypeNo, String expType)
	{
		DatabaseManager.expenditureTypes.set(expTypeNo, expType);
	}
	
	public static void setAllExpenditureTypes(ArrayList<String> expTypes1)
	{
		DatabaseManager.expenditureTypes=expTypes1;
		databaseAdapter.deleteAllExpTypes();
		databaseAdapter.addAllExpTypes(expTypes1);
	}
	
	public static String getExpenditureType(int expTypeNo)
	{
		return DatabaseManager.expenditureTypes.get(expTypeNo);
	}
	
	public static ArrayList<String> getAllExpenditureTypes()
	{
		return DatabaseManager.expenditureTypes;
	}
	
	public static void readjustCountersTable()
	{
		databaseAdapter.readjustCountersTable();
	}
	
	public static int getNumCountersRows()
	{
		return counters.size();
	}

	public static void setAllCounters(ArrayList<Counters> counters)
	{
		DatabaseManager.counters = counters;
		databaseAdapter.deleteAllCountersRows();
		databaseAdapter.addAllCountersRows(counters);
	}
	
	public static ArrayList<Counters> getAllCounters()
	{
		return DatabaseManager.counters;
	}
	
	public static void increamentCounters(Date date, int expTypeNo, double amount)
	{
		int numCountersRows = counters.size();
		double[] exp = new double[expenditureTypes.size()+4];
		exp[expTypeNo] = amount;
		if(numCountersRows == 0)
		{
			counters = new ArrayList<Counters>();
			//counters.add(new Counters(1,date, exp));
			
			Counters counter = new Counters(1,date, exp);
			counters.add(counter);
			numCountersRows++;
			//counter.setID(1);
			databaseAdapter.addCountersRow(counter);
		}
		else if(date.getLongDate()<counters.get(0).getDate().getLongDate())
		{
			//counters.add(0, new Counters(1,date, exp));
			
			Counters counter = new Counters(1,date, exp);
			counters.add(0,counter);
			numCountersRows++;
			// Update IDs of following counters
			for(int i=1; i<numCountersRows; i++)
			{
				counters.get(i).setID(i+1);
			}
			//counter.setID(1);
			databaseAdapter.insertCountersRow(counter);
		}
		else if(date.getLongDate()>counters.get(numCountersRows-1).getDate().getLongDate())
		{
			//counters.add(new Counters(date, exp));
			
			Counters counter = new Counters(numCountersRows+1,date, exp);
			counters.add(counter);
			numCountersRows++;
			//counter.setID(numCountersRows);			// Which has been increamented already
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
				//counters.add(middle+1, new Counters(date, exp));   // Insert The New Counters Row.
				
				Counters counter = new Counters(middle+2,date, exp);
				counters.add(middle+1, counter);   // Insert The New Counters Row.
				//counter.setID(middle + 2);
				databaseAdapter.insertCountersRow(counter);
			}
		}
	}
	
	public static void decreamentCounters(Date date, int expTypeNo, double amount)
	{
		int numCountersRows = counters.size();
		double[] exp = new double[expenditureTypes.size()+4];
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
				//counter.setID(middle + 1);
				databaseAdapter.updateCountersRow(counter);
				break;
			}
			middle = (first + last)/2;
		}
	}
	
	public static double[] getMonthlyCounters(long month)
	{
		int numCountersRows = counters.size();
		double[] monthlyCounters = new double[expenditureTypes.size()+4];
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
				double[] allExpenditures = counter1.getAllExpenditures();
				int numExpTypes = expenditureTypes.size();
				for(int j=0; j<numExpTypes; j++)
				{
					monthlyCounters[j] += allExpenditures[j];
				}
				/*monthlyCounters[0] += counter1.getExp01();
				monthlyCounters[1] += counter1.getExp02();
				monthlyCounters[2] += counter1.getExp03();
				monthlyCounters[3] += counter1.getExp04();
				monthlyCounters[4] += counter1.getExp05();*/
				monthlyCounters[numExpTypes] += counter1.getAmountSpent();
				monthlyCounters[numExpTypes+1] += counter1.getIncome();
				monthlyCounters[numExpTypes+2] += counter1.getSavings();
				monthlyCounters[numExpTypes+3] += counter1.getWithdrawal();
			}
			else if(month1 > month)
			{
				i = numCountersRows;
				break;
			}
		}
		return monthlyCounters;
	}
	
	public static double[] getYearlyCounters(long year)
	{
		int numCountersRows = counters.size();
		double[] yearlyCounters = new double[expenditureTypes.size()+4];
		boolean found = false;
		for(int i=0; i<numCountersRows; i++)
		{
			Counters counter1 = counters.get(i);
			long year1 = (long) Math.floor(counter1.getDate().getLongDate()/10000);
			if(!found && year1 < year)				// Required Year not yet found
			{
				i += 10;
				if(i>=numCountersRows)
					i-=10;
			}
			else if(!found && year1 == year)		// Required Year encountered for the first time. 
			{										//previous ones may contain required Year. So, go back
				i -= 10;
				if(i<0)
					i=-1;
				found = true;
			}
			else if(found && year1<year)
			{
				
			}
			else if(found && year1 == year)
			{
				double[] allExpenditures = counter1.getAllExpenditures();
				int numExpTypes = expenditureTypes.size();
				for(int j=0; j<numExpTypes; j++)
				{
					yearlyCounters[j] += allExpenditures[j];
				}
				/*yearlyCounters[0] += counter1.getExp01();
				yearlyCounters[1] += counter1.getExp02();
				yearlyCounters[2] += counter1.getExp03();
				yearlyCounters[3] += counter1.getExp04();
				yearlyCounters[4] += counter1.getExp05();*/
				yearlyCounters[numExpTypes] += counter1.getAmountSpent();
				yearlyCounters[numExpTypes+1] += counter1.getIncome();
				yearlyCounters[numExpTypes+2] += counter1.getSavings();
				yearlyCounters[numExpTypes+3] += counter1.getWithdrawal();
			}
			else if(year1 > year)
			{
				i = numCountersRows;
				break;
			}
		}
		return yearlyCounters;
	}
	
	public static double[] getTotalCounters()
	{
		int numCountersRows = counters.size();
		double[] totalCounters = new double[expenditureTypes.size()+4];
		for(int i=0; i<numCountersRows; i++)
		{
			Counters counter1 = counters.get(i);
			double[] allExpenditures = counter1.getAllExpenditures();
			int numExpTypes = expenditureTypes.size();
			for(int j=0; j<numExpTypes; j++)
			{
				totalCounters[j] += allExpenditures[j];
			}
			/*totalCounters[0] += counter1.getExp01();
			totalCounters[1] += counter1.getExp02();
			totalCounters[2] += counter1.getExp03();
			totalCounters[3] += counter1.getExp04();
			totalCounters[4] += counter1.getExp05();*/
			totalCounters[numExpTypes] += counter1.getAmountSpent();
			totalCounters[numExpTypes+1] += counter1.getIncome();
			totalCounters[numExpTypes+2] += counter1.getSavings();
			totalCounters[numExpTypes+3] += counter1.getWithdrawal();
		}
		return totalCounters;
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
		// Amount Spent is the (N+1)th column where N is numExpTypes
		DatabaseManager.increamentCounters(date, expenditureTypes.size(), amount);
	}
	
	public static void decreamentAmountSpent(Date date, double amount)
	{
		// Amount Spent is the (N+1)th column where N is numExpTypes
		DatabaseManager.decreamentCounters(date, expenditureTypes.size(), amount);
	}
	
	public static double getMonthlyAmountSpent(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Amount Spent is the (N+1)th column where N is numExpTypes
		return counters[expenditureTypes.size()];
	}
	
	public static double getYearlyAmountSpent(long year)
	{
		double counters[] = getYearlyCounters(year);
		// Amount Spent is the (N+1)th column where N is numExpTypes
		return counters[expenditureTypes.size()];
	}
	
	public static double getTotalAmountSpent()
	{
		double counters[] = getTotalCounters();
		// Amount Spent is the (N+1)th column where N is numExpTypes
		return counters[expenditureTypes.size()];
	}
	
	public static void increamentIncome(Date date, double amount)
	{
		// Income is the (N+2)th column where N is numExpTypes
		DatabaseManager.increamentCounters(date, expenditureTypes.size()+1, amount);
	}
	
	public static void decreamentIncome(Date date, double amount)
	{
		// Income is the (N+2)th column where N is numExpTypes
		DatabaseManager.decreamentCounters(date, expenditureTypes.size()+1, amount);
	}
	
	public static double getMonthlyIncome(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Income is the (N+2)th column where N is numExpTypes
		return counters[expenditureTypes.size()+1];
	}
	
	public static double getYearlyIncome(long year)
	{
		double counters[] = getYearlyCounters(year);
		// Income is the (N+2)th column where N is numExpTypes
		return counters[expenditureTypes.size()+1];
	}
	
	public static double getTotalIncome()
	{
		double counters[] = getTotalCounters();
		// Income is the (N+2)th column where N is numExpTypes
		return counters[expenditureTypes.size()+1];
	}
	
	public static void increamentSavings(Date date, double amount)
	{
		// Savings is the (N+3)th column where N is numExpTypes
		DatabaseManager.increamentCounters(date, expenditureTypes.size()+2, amount);
	}
	
	public static void decreamentSavings(Date date, double amount)
	{
		// Savings is the (N+3)th column where N is numExpTypes
		DatabaseManager.decreamentCounters(date, expenditureTypes.size()+2, amount);
	}
	
	public static double getMonthlySavings(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Savings is the (N+3)th column where N is numExpTypes
		return counters[expenditureTypes.size()+2];
	}
	
	public static double getYearlySavings(long year)
	{
		double counters[] = getYearlyCounters(year);
		// Savings is the (N+3)th column where N is numExpTypes
		return counters[expenditureTypes.size()+2];
	}
	
	public static double getTotalSavings()
	{
		double counters[] = getTotalCounters();
		// Savings is the (N+3)th column where N is numExpTypes
		return counters[expenditureTypes.size()+2];
	}
	
	public static void increamentWithdrawal(Date date, double amount)
	{
		// Withdrawal is the (N+4)th column where N is numExpTypes
		DatabaseManager.increamentCounters(date, expenditureTypes.size()+3, amount);
	}
	
	public static void decreamentWithdrawal(Date date, double amount)
	{
		// Withdrawal is the (N+4)th column where N is numExpTypes
		DatabaseManager.decreamentCounters(date, expenditureTypes.size()+3, amount);
	}
	
	public static double getMonthlyWithdrawal(long month)
	{
		double counters[] = getMonthlyCounters(month);
		// Withdrawal is the (N+4)th column where N is numExpTypes
		return counters[expenditureTypes.size()+3];
	}
	
	public static double getYearlyWithdrawal(long year)
	{
		double counters[] = getYearlyCounters(year);
		// Withdrawal is the (N+4)th column where N is numExpTypes
		return counters[expenditureTypes.size()+3];
	}
	
	public static double getTotalWithdrawal()
	{
		double counters[] = getTotalCounters();
		// Withdrawal is the (N+4)th column where N is numExpTypes
		return counters[expenditureTypes.size()+3];
	}
	
	/**
	 * @return The months in which Transactions were made in the format
	 * 		January - 2015
	 * 		February - 2015 and so on
	 */
	public static ArrayList<String> getExportableMonths()
	{
		ArrayList<Long> longMonths = new ArrayList<Long>();		//201501, 201502,...
		ArrayList<String> months = new ArrayList<String>();		//January - 2015, February - 2015
		
		// Gets All the months(In which transactions were made) in the format 201501, 201502, 201503,....
		for(int i=0; i<transactions.size(); i++)
		{
			long longMonth = (long) transactions.get(i).getDate().getLongDate()/100;	//201501
			if(!longMonths.contains(longMonth))
			{
				longMonths.add(longMonth);
			}
		}
		
		// Convert the months format from long (201501) to words (January - 2015)
		for(int i=0; i<longMonths.size(); i++)
		{
			int month = (int) (longMonths.get(i) % 100);
			int year = (int) (longMonths.get(i)/100);
			String monthName = Date.getMonthName(month);
			months.add(monthName + " - " + year);			//"January" + " - " + 2015 = "January - 2015"
		}
		return months;
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
	
	// Static Methods to compare two objects like transactions, banks...
	public static boolean areEqualTransactions(ArrayList<Transaction> transactions1, 
			ArrayList<Transaction> transactions2)
	{
		//Toast.makeText(context, "Check-Point01", Toast.LENGTH_SHORT).show();
		// Go on comparing every fields. Whenever you find a difference, return false;
		if(transactions1.size() != transactions2.size())
		{
			Toast.makeText(context, "NumTransactions Mismatch", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		boolean isValid = true;
		int numTransactions = transactions1.size(), i;
		for(i=0; isValid && i<numTransactions; i++)
		{
			Transaction transaction1 = transactions1.get(i);
			Transaction transaction2 = transactions2.get(i);
			if(transaction1.getID() != transaction2.getID())
			{
				Toast.makeText(context, "ID Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(transaction1.getCreatedTime().isNotEqualTo(transaction2.getCreatedTime()))
			{
				Toast.makeText(context, "02 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(transaction1.getModifiedTime().isNotEqualTo(transaction2.getModifiedTime()))
			{
				Toast.makeText(context, "03 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(transaction1.getDate().isNotEqualTo(transaction2.getDate()))
			{
				Toast.makeText(context, "04 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(!transaction1.getType().equals(transaction2.getType()))
			{
				Toast.makeText(context, "05 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(!transaction1.getParticular().equals(transaction2.getParticular()))
			{
				Toast.makeText(context, "06 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(transaction1.getRate() != transaction2.getRate())
			{
				Toast.makeText(context, "07 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(transaction1.getQuantity() != transaction2.getQuantity())
			{
				Toast.makeText(context, "08 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(transaction1.getAmount() != transaction2.getAmount())
			{
				Toast.makeText(context, "09 Error: i="+i+", id1="+transaction1.getID()+", id2="+transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
		}
		return isValid;
	}
	
	public static boolean areEqualBanks(ArrayList<Bank> banks1, ArrayList<Bank> banks2)
	{
		if(banks1.size() != banks2.size())
		{
			Toast.makeText(context, "Error In NumBanks", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		// Go on comparing every fields. Whenever you find a difference, set isValid to false;
		boolean isValid = true;
		int numBanks = banks1.size(), i=0;
		for(i=0; isValid && i<numBanks; i++)
		{
			if(banks1.get(i).getID() != banks2.get(i).getID())
			{
				Toast.makeText(context, "01 Error: i="+i+", id1="+banks1.get(i).getID()+", id2="+banks2.get(i).getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(!banks1.get(i).getName().equals(banks2.get(i).getName()))
			{
				Toast.makeText(context, "02 Error: i="+i+", id1="+banks1.get(i).getName()+", id2="+banks2.get(i).getName(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(!banks1.get(i).getAccNo().equals(banks2.get(i).getAccNo()))
			{
				Toast.makeText(context, "03 Error: i="+i+", id1="+banks1.get(i).getAccNo()+", id2="+banks2.get(i).getAccNo(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(banks1.get(i).getBalance() != banks2.get(i).getBalance())
			{
				Toast.makeText(context, "04 Error: i="+i+", DB Balance="+banks1.get(i).getBalance()+", SD Balance="+banks2.get(i).getBalance(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if(!banks1.get(i).getSmsName().equals(banks2.get(i).getSmsName()))
			{
				Toast.makeText(context, "05 Error: i="+i+", id1="+banks1.get(i).getSmsName()+", id2="+banks2.get(i).getSmsName(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
		}
		if(!isValid)
		{
			Toast.makeText(context, "Error In " + i + "th Bank", Toast.LENGTH_SHORT).show();
		}
		return isValid;
	}
	
	public static boolean areEqualCounters(ArrayList<Counters> counters1, ArrayList<Counters> counters2)
	{
		// Go on comparing every fields. Whenever you find a difference, return false;
		if(counters1.size() != counters2.size())
		{
			return false;
		}
		int numCountersRows = counters1.size();
		for(int i=0; i<numCountersRows; i++)
		{
			if(counters1.get(i).getID() != counters2.get(i).getID())
			{
				return false;
			}
			//Log.d("DatabaseManager/areEqualCounters()", "Check-Point 02:" + counters1.get(i).getAllExpenditures().length);
			for(int j=0; j<expenditureTypes.size(); j++)
			{
				//Log.d("DatabaseManager/areEqualCounters(", "Check-Point 03:i=" + i + "|j=" + j);
				if(counters1.get(i).getAllExpenditures()[j] != counters2.get(i).getAllExpenditures()[j])
				{
					return false;
				}
			}
			/*if(counters1.get(i).getExp01() != counters2.get(i).getExp01())
			{
				return false;
			}
			if(counters1.get(i).getExp02() != counters2.get(i).getExp02())
			{
				return false;
			}
			if(counters1.get(i).getExp03() != counters2.get(i).getExp03())
			{
				return false;
			}
			if(counters1.get(i).getExp04() != counters2.get(i).getExp04())
			{
				return false;
			}
			if(counters1.get(i).getExp05() != counters2.get(i).getExp05())
			{
				return false;
			}*/
			if(counters1.get(i).getAmountSpent() != counters2.get(i).getAmountSpent())
			{
				return false;
			}
			if(counters1.get(i).getIncome() != counters2.get(i).getIncome())
			{
				return false;
			}
			if(counters1.get(i).getWithdrawal() != counters2.get(i).getWithdrawal())
			{
				return false;
			}
			if(counters1.get(i).getSavings() != counters2.get(i).getSavings())
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean areEqualExpTypes(ArrayList<String> expTypes1, ArrayList<String> expTypes2)
	{
		// Go on comparing every fields. Whenever you find a difference, return false;
		if(expTypes1.size() != expTypes2.size())
		{
			return false;
		}
		int numExpTypes = expTypes1.size();
		for(int i=0; i<numExpTypes; i++)
		{
			if(!expTypes1.get(i).equals(expTypes2.get(i)))
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean areEqualTemplates(ArrayList<Template> templates1, ArrayList<Template> templates2)
	{
		// Go on comparing every fields. Whenever you find a difference, return false;
		if(templates1.size() != templates2.size())
		{
			return false;
		}
		int numTemplates = templates1.size();
		for(int i=0; i<numTemplates; i++)
		{
			if(templates1.get(i).getID() != templates2.get(i).getID())
			{
				return false;
			}
			if(!(templates1.get(i).getParticular().equals(templates2.get(i).getParticular())))
			{
				return false;
			}
			if(!(templates1.get(i).getType().equals(templates2.get(i).getType())))
			{
				return false;
			}
			if(templates1.get(i).getAmount() != templates2.get(i).getAmount())
			{
				return false;
			}
		}
		return true;
	}

	public static void updateTemplates()
	{
		templates = new ArrayList<Template>();
		databaseAdapter.deleteAllTemplates();
		databaseAdapter.addAllTemplates(templates);
	}
}
