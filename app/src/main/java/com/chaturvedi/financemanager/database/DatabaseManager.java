package com.chaturvedi.financemanager.database;

import android.content.Context;
import android.util.Log;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.financemanager.datastructures.*;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.util.ArrayList;

public class DatabaseManager
{
	public static final int ACTION_DATABASE_READ_PROGRESS = 101;
	public static final int ACTION_INITIALIZATION_COMPLETE = 102;
	public static final int ACTION_NEW_TRANSACTION_FOUND = 103;
	public static final int ACTION_ALL_TRANSACTIONS_FOUND = 104;
	public static final int ACTION_TOAST_MESSAGE = 105;
	
	public static void clearDatabase(Context context)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		databaseAdapter.deleteAllTransactions();
		databaseAdapter.deleteAllCountersRows();
	}
	
	public static void addTemplate(Context context, Template template)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		// ID of the template will not be set. It has to be set now
		// Check if the template Exists
		Template oldTemplate = databaseAdapter.getTemplate(template.getParticular());
		if (oldTemplate == null)
		{
			int templateID = databaseAdapter.getIDforNextTemplate();
			template.setID(templateID);
			databaseAdapter.addTemplate(template);
		}
		else
		{
			template.setID(oldTemplate.getID());
			databaseAdapter.updateTemplate(template);
		}
	}

	/**
	 * Adds a transaction
	 * @param context used to get instance of DatabaseAdapter
	 * @param transaction Transaction to delete
	 * @param addInDatabase If true, transaction will be added in TransactionsTable in database.
	 *                         If false, wallets, banks and counters will be updated but transaction will not be added in
	 *                         transactions table. This is used in DatabaseManager.editTransaction() method
	 */
	public static void addTransaction(Context context, Transaction transaction, boolean addInDatabase)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		Counters counters = databaseAdapter.getCountersRow(transaction.getDate().getSavableDate());
		int numExpenditureTypes = databaseAdapter.getNumExpenditureTypes();
		double amount = transaction.getAmount();

		TransactionTypeParser parser = new TransactionTypeParser(context, transaction.getType());
		if (parser.isIncome())
		{
			if(transaction.isIncludeInCounters())
			{
				// Counters: couneterForEachExpenseType[0-4], TotalExpense(5), TotalIncome(6), TotalSavings(7), TotalWithdrawal(8)
				// Example Counter Numbers are assigned assuming numExpTypes = 5
				counters.increamentCounter(numExpenditureTypes + 1, amount);
			}

			if (parser.isIncomeDestinationWallet())
			{
				Wallet wallet = parser.getIncomeDestinationWallet();
				wallet.incrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getIncomeDestinationBank();
				bank.incrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}
		}
		else if (parser.isExpense())
		{
			if (transaction.isIncludeInCounters())
			{
				counters.increamentCounter(parser.getExpenditureType().getId() - 1, amount);
				counters.increamentCounter(numExpenditureTypes, amount);    // Increament Total Expense
			}
			
			if (parser.isExpenseSourceWallet())
			{
				Wallet wallet = parser.getExpenseSourceWallet();
				wallet.decrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getExpenseSourceBank();
				bank.decrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}
		}
		else if (parser.isTransfer())
		{
			if (transaction.isIncludeInCounters())
			{
				// Savings
				if (parser.isTransferSourceWallet() && parser.isTransferDestinationBank())
				{
					counters.increamentCounter(numExpenditureTypes + 2, amount);
				}
				// Withdrawal
				else if (parser.isTransferSourceBank() && parser.isTransferDestinationWallet())
				{
					counters.increamentCounter(numExpenditureTypes + 3, amount);
				}
			}
			
			if (parser.isTransferSourceWallet())
			{
				Wallet wallet = parser.getTransferSourceWallet();
				wallet.decrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getTransferSourceBank();
				bank.decrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}

			if (parser.isTransferDestinationWallet())
			{
				Wallet wallet = parser.getTransferDestinationWallet();
				wallet.incrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getTransferDestinationBank();
				bank.incrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}
		}

		if(addInDatabase)
		{
			databaseAdapter.addTransaction(transaction);
		}
		if (transaction.isIncludeInCounters())
		{
			databaseAdapter.updateCountersRow(counters);
		}
	}

	/**
	 * Deletes a transaction
	 * @param context used to get instance of DatabaseAdapter
	 * @param transaction Transaction to delete
	 * @param deleteInDatabase If true, transaction will be deleted in TransactionsTable in database.
	 *                         If false, wallets, banks and counters will be updated but transaction will not be deleted in
	 *                         transactions table. This is used in DatabaseManager.editTransaction() method
	 */
	public static void deleteTransaction(Context context, Transaction transaction, boolean deleteInDatabase)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		Counters counters = databaseAdapter.getCountersRow(transaction.getDate().getSavableDate());
		int numExpenditureTypes = databaseAdapter.getNumExpenditureTypes();
		double amount = transaction.getAmount();

		TransactionTypeParser parser = new TransactionTypeParser(context, transaction.getType());
		if (parser.isIncome())
		{
			if (transaction.isIncludeInCounters())
			{
				// Counters: couneterForEachExpenseType[0-4], TotalExpense(5), TotalIncome(6), TotalSavings(7), TotalWithdrawal(8)
				// Example Counter Numbers are assigned assuming numExpTypes = 5
				counters.decreamentCounter(numExpenditureTypes + 1, amount);
			}
			
			if (parser.isIncomeDestinationWallet())
			{
				Wallet wallet = parser.getIncomeDestinationWallet();
				wallet.decrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getIncomeDestinationBank();
				bank.decrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}
		}
		else if (parser.isExpense())
		{
			if (transaction.isIncludeInCounters())
			{
				counters.decreamentCounter(parser.getExpenditureType().getId() - 1, amount);
				counters.decreamentCounter(numExpenditureTypes, amount);    // Increament Total Expense
			}
			
			if (parser.isExpenseSourceWallet())
			{
				Wallet wallet = parser.getExpenseSourceWallet();
				wallet.incrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getExpenseSourceBank();
				bank.incrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}
		}
		else if (parser.isTransfer())
		{
			if (transaction.isIncludeInCounters())
			{
				// Savings
				if (parser.isTransferSourceWallet() && parser.isTransferDestinationBank())
				{
					counters.decreamentCounter(numExpenditureTypes + 2, amount);
				}
				// Withdrawal
				else if (parser.isTransferSourceBank() && parser.isTransferDestinationWallet())
				{
					counters.decreamentCounter(numExpenditureTypes + 3, amount);
				}
			}
			
			if (parser.isTransferSourceWallet())
			{
				Wallet wallet = parser.getTransferSourceWallet();
				wallet.incrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getTransferSourceBank();
				bank.incrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}

			if (parser.isTransferDestinationWallet())
			{
				Wallet wallet = parser.getTransferDestinationWallet();
				wallet.decrementBalance(amount);
				databaseAdapter.updateWallet(wallet);
			}
			else
			{
				Bank bank = parser.getTransferDestinationBank();
				bank.decrementBalance(amount);
				databaseAdapter.updateBank(bank);
			}
		}

		if(deleteInDatabase)
		{
			databaseAdapter.deleteTransaction(transaction);
		}
		if (transaction.isIncludeInCounters())
		{
			databaseAdapter.updateCountersRow(counters);
		}
	}

	public static void editTransaction(Context context, Transaction oldTransaction, Transaction newTransaction)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		databaseAdapter.updateTransaction(newTransaction);

		if(oldTransaction.differOnlyInParticular(newTransaction))
		{
			return;
		}

		DatabaseManager.deleteTransaction(context, oldTransaction, false);
		DatabaseManager.addTransaction(context, newTransaction, false);
	}
	
	/**
	 * @return The months in which Transactions were made in the format
	 * January - 2015
	 * February - 2015 and so on
	 */
	public static ArrayList<String> getExportableMonths(Context context)
	{
		// Todo: Use the below sqlite query
		// SELECT DISTINCT REPLACE(substr(date,0,8), "/", "") AS month FROM counters WHERE month >= "201501" AND month <= "201512"
		ArrayList<Transaction> transactions = DatabaseAdapter.getInstance(context).getAllTransactions();
		ArrayList<Long> longMonths = new ArrayList<Long>();        //201501, 201502,...
		ArrayList<String> months = new ArrayList<String>();        //January - 2015, February - 2015
		
		// Gets All the months(In which transactions were made) in the format 201501, 201502, 201503,....
		for (int i = 0; i < transactions.size(); i++)
		{
			long longMonth = transactions.get(i).getDate().getLongDate() / 100;    //201501
			if (!longMonths.contains(longMonth))
			{
				longMonths.add(longMonth);
			}
		}
		
		// Convert the months format from long (201501) to words (January - 2015)
		for (int i = 0; i < longMonths.size(); i++)
		{
			int month = (int) (longMonths.get(i) % 100);
			int year = (int) (longMonths.get(i) / 100);
			String monthName = Date.getMonthName(month);
			months.add(monthName + " - " + year);            //"January" + " - " + 2015 = "January - 2015"
		}
		return months;
	}
	
	// Static Methods to compare two objects like transactions, banks...
	public static boolean areEqualTransactions(ArrayList<Transaction> transactions1,
											   ArrayList<Transaction> transactions2)
	{
		//Toast.makeText(context, "Check-Point01", Toast.LENGTH_SHORT).show();
		// Go on comparing every fields. Whenever you find a difference, return false;
		if (transactions1.size() != transactions2.size())
		{
			//Toast.makeText(context, "NumTransactions Mismatch", Toast.LENGTH_SHORT).show();
			Log.d("AreEqualTransactions()", "NumTransactions Mismatch");
			return false;
		}
		
		boolean isValid = true;
		int numTransactions = transactions1.size(), i;
		for (i = 0; isValid && i < numTransactions; i++)
		{
			Transaction transaction1 = transactions1.get(i);
			Transaction transaction2 = transactions2.get(i);
			if (transaction1.getID() != transaction2.getID())
			{
				//Toast.makeText(context, "ID Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.getCreatedTime().isNotEqualTo(transaction2.getCreatedTime()))
			{
				//Toast.makeText(context, "02 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.getModifiedTime().isNotEqualTo(transaction2.getModifiedTime()))
			{
				//Toast.makeText(context, "03 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.getDate().isNotEqualTo(transaction2.getDate()))
			{
				//Toast.makeText(context, "04 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (!transaction1.getType().equals(transaction2.getType()))
			{
				//Toast.makeText(context, "05 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (!transaction1.getParticular().equals(transaction2.getParticular()))
			{
				//Toast.makeText(context, "06 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.getRate() != transaction2.getRate())
			{
				//Toast.makeText(context, "07 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.getQuantity() != transaction2.getQuantity())
			{
				//Toast.makeText(context, "08 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.getAmount() != transaction2.getAmount())
			{
				//Toast.makeText(context, "09 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.isHidden() != transaction2.isHidden())
			{
				//Toast.makeText(context, "09 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (transaction1.isIncludeInCounters() != transaction2.isIncludeInCounters())
			{
				//Toast.makeText(context, "09 Error: i=" + i + ", id1=" + transaction1.getID() + ", id2=" + transaction2.getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
		}
		return isValid;
	}
	
	public static boolean areEqualWallets(ArrayList<Wallet> wallets1, ArrayList<Wallet> wallets2)
	{
		if (wallets1.size() != wallets2.size())
		{
			//Toast.makeText(context, "Error In NumWallets", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		// Go on comparing every fields. Whenever you find a difference, set isValid to false;
		boolean isValid = true;
		int numWallets = wallets1.size(), i;
		for (i = 0; isValid && i < numWallets; i++)
		{
			if (wallets1.get(i).getID() != wallets2.get(i).getID())
			{
				isValid = false;
			}
			if (!wallets1.get(i).getName().equals(wallets2.get(i).getName()))
			{
				isValid = false;
			}
			if (wallets1.get(i).getBalance() != wallets2.get(i).getBalance())
			{
				isValid = false;
			}
			if (wallets1.get(i).isDeleted() != wallets2.get(i).isDeleted())
			{
				isValid = false;
			}
		}
		/*if (!isValid)
		{
			Toast.makeText(context, "Error In " + i + "th Bank", Toast.LENGTH_SHORT).show();
		}*/
		return isValid;
	}

	public static boolean areEqualBanks(ArrayList<Bank> banks1, ArrayList<Bank> banks2)
	{
		if (banks1.size() != banks2.size())
		{
			//Toast.makeText(context, "Error In NumBanks", Toast.LENGTH_SHORT).show();
			return false;
		}

		// Go on comparing every fields. Whenever you find a difference, set isValid to false;
		boolean isValid = true;
		int numBanks = banks1.size(), i;
		for (i = 0; isValid && i < numBanks; i++)
		{
			if (banks1.get(i).getID() != banks2.get(i).getID())
			{
				//Toast.makeText(context, "01 Error: i=" + i + ", id1=" + banks1.get(i).getID() + ", id2=" + banks2.get(i).getID(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (!banks1.get(i).getName().equals(banks2.get(i).getName()))
			{
				//Toast.makeText(context, "02 Error: i=" + i + ", id1=" + banks1.get(i).getName() + ", id2=" + banks2.get(i).getName(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (!banks1.get(i).getAccNo().equals(banks2.get(i).getAccNo()))
			{
				//Toast.makeText(context, "03 Error: i=" + i + ", id1=" + banks1.get(i).getAccNo() + ", id2=" + banks2.get(i).getAccNo(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (banks1.get(i).getBalance() != banks2.get(i).getBalance())
			{
				//Toast.makeText(context, "04 Error: i=" + i + ", DB Balance=" + banks1.get(i).getBalance() + ", SD Balance=" + banks2.get(i).getBalance(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (!banks1.get(i).getSmsName().equals(banks2.get(i).getSmsName()))
			{
				//Toast.makeText(context, "05 Error: i=" + i + ", id1=" + banks1.get(i).getSmsName() + ", id2=" + banks2.get(i).getSmsName(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
			if (banks1.get(i).isDeleted() != (banks2.get(i).isDeleted()))
			{
				//Toast.makeText(context, "05 Error: i=" + i + ", id1=" + banks1.get(i).getSmsName() + ", id2=" + banks2.get(i).getSmsName(), Toast.LENGTH_SHORT).show();
				isValid = false;
			}
		}
		/*if (!isValid)
		{
			Toast.makeText(context, "Error In " + i + "th Bank", Toast.LENGTH_SHORT).show();
		}*/
		return isValid;
	}
	
	public static boolean areEqualCounters(ArrayList<Counters> counters1, ArrayList<Counters> counters2, int numExpenditureTypes)
	{
		// Go on comparing every fields. Whenever you find a difference, return false;
		if (counters1.size() != counters2.size())
		{
			return false;
		}
		int numCountersRows = counters1.size();
		for (int i = 0; i < numCountersRows; i++)
		{
			if (counters1.get(i).getID() != counters2.get(i).getID())
			{
				return false;
			}
			double[] expenditures1 = counters1.get(i).getAllExpenditures();
			double[] expenditures2 = counters2.get(i).getAllExpenditures();
			for (int j = 0; j < numExpenditureTypes; j++)
			{
				if (expenditures1[j] != expenditures2[j])
				{
					return false;
				}
			}
			if (counters1.get(i).getAmountSpent() != counters2.get(i).getAmountSpent())
			{
				return false;
			}
			if (counters1.get(i).getIncome() != counters2.get(i).getIncome())
			{
				return false;
			}
			if (counters1.get(i).getWithdrawal() != counters2.get(i).getWithdrawal())
			{
				return false;
			}
			if (counters1.get(i).getSavings() != counters2.get(i).getSavings())
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean areEqualExpTypes(ArrayList<ExpenditureType> expTypes1, ArrayList<ExpenditureType> expTypes2)
	{
		// Go on comparing every fields. Whenever you find a difference, return false;
		if (expTypes1.size() != expTypes2.size())
		{
			return false;
		}
		int numExpTypes = expTypes1.size();
		for (int i = 0; i < numExpTypes; i++)
		{
			if (expTypes1.get(i).getId() != expTypes2.get(i).getId())
			{
				return false;
			}
			if (!expTypes1.get(i).getName().equals(expTypes2.get(i).getName()))
			{
				return false;
			}
			if (expTypes1.get(i).isDeleted() != expTypes2.get(i).isDeleted())
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean areEqualTemplates(ArrayList<Template> templates1, ArrayList<Template> templates2)
	{
		// Go on comparing every fields. Whenever you find a difference, return false;
		if (templates1.size() != templates2.size())
		{
			return false;
		}
		int numTemplates = templates1.size();
		for (int i = 0; i < numTemplates; i++)
		{
			if (templates1.get(i).getID() != templates2.get(i).getID())
			{
				return false;
			}
			if (!(templates1.get(i).getParticular().equals(templates2.get(i).getParticular())))
			{
				return false;
			}
			if (!(templates1.get(i).getType().equals(templates2.get(i).getType())))
			{
				return false;
			}
			if (templates1.get(i).getAmount() != templates2.get(i).getAmount())
			{
				return false;
			}
			if (templates1.get(i).isHidden() != templates2.get(i).isHidden())
			{
				return false;
			}
		}
		return true;
	}
}
