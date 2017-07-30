// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.functions;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.ExpenditureType;
import com.chaturvedi.financemanager.database.NewWallet;

public class TransactionTypeParser
{
	private String transactionType;
	
	private boolean isIncome = false;
	private boolean isIncomeDestinationWallet = false;
	private NewWallet incomeDestinationWallet = null;
	private boolean isIncomeDestinationBank = false;
	private Bank incomeDestinationBank = null;
	private String incomeDestinationName = null;
	
	private boolean isExpense = false;
	private boolean isExpenseSourceWallet = false;
	private NewWallet expenseSourceWallet = null;
	private boolean isExpenseSourceBank = false;
	private Bank expenseSourceBank = null;
	private ExpenditureType expenditureType = null;
	private String expenseSourceName = null;
	
	private boolean isTransfer = false;
	private boolean isTransferSourceWallet = false;
	private NewWallet transferSourceWallet = null;
	private boolean isTransferSourceBank = false;
	private Bank transferSourceBank = null;
	private boolean isTransferDestinationWallet = false;
	private NewWallet transferDestinationWallet = null;
	private boolean isTransferDestinationBank = false;
	private Bank transferDestinationBank = null;
	private String transferSourceName = null;
	private String transferDestinationName = null;
	
	public TransactionTypeParser(Context context, String type)
	{
		transactionType = type;
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(context);
		
		// Credit Wallet01
		// Credit Bank01
		if(type.contains("Credit"))
		{
			isIncome = true;
			
			if(type.contains("Wallet"))
			{
				isIncomeDestinationWallet = true;
				int id = Integer.parseInt(type.substring(13));
				incomeDestinationWallet = databaseAdapter.getWallet(id);
				incomeDestinationName = incomeDestinationWallet.getName();
			}
			else
			{
				isIncomeDestinationBank = true;
				int id = Integer.parseInt(type.substring(11));
				incomeDestinationBank = databaseAdapter.getBank(id);
				incomeDestinationName = incomeDestinationBank.getName();
			}
		}
		// Debit Wallet01 Exp01
		// Debit Bank01 Exp01
		else if(type.contains("Debit"))
		{
			isExpense = true;

			if(type.contains("Wallet"))
			{
				isExpenseSourceWallet = true;
				int walletID = Integer.parseInt(type.substring(12,14));
				expenseSourceWallet = databaseAdapter.getWallet(walletID);
				int expTypeID = Integer.parseInt(type.substring(18));
				expenditureType = databaseAdapter.getExpenditureType(expTypeID);
				expenseSourceName = expenseSourceWallet.getName();
			}
			else
			{
				isExpenseSourceBank = true;
				int bankID = Integer.parseInt(type.substring(10,12));
				expenseSourceBank = databaseAdapter.getBank(bankID);
				int expTypeID = Integer.parseInt(type.substring(16));
				expenditureType = databaseAdapter.getExpenditureType(expTypeID);
				expenseSourceName = expenseSourceBank.getName();
			}
		}
		// Transfer Wallet01 Wallet02
		// Transfer Wallet01 Bank01
		// Transfer Bank01 Wallet01
		// Transfer Bank01 Bank02
		else if(type.contains("Transfer"))
		{
			isTransfer = true;

			if(type.substring(9,15).equals("Wallet"))
			{
				isTransferSourceWallet = true;
				int walletID = Integer.parseInt(type.substring(15,17));
				transferSourceWallet = databaseAdapter.getWallet(walletID);
				transferSourceName = transferSourceWallet.getName();
			}
			else if(type.substring(9,13).equals("Bank"))
			{
				isTransferSourceBank = true;
				int bankID = Integer.parseInt(type.substring(13,15));
				transferSourceBank = databaseAdapter.getBank(bankID);
				transferSourceName = transferSourceBank.getName();
			}
			else
			{
				Toast.makeText(context, "Unknown Transfer Source", Toast.LENGTH_LONG).show();
			}

			if(type.substring(18,24).equals("Wallet"))
			{
				isTransferDestinationWallet = true;
				int walletID = Integer.parseInt(type.substring(24,26));
				transferDestinationWallet = databaseAdapter.getWallet(walletID);
				transferDestinationName = transferDestinationWallet.getName();
			}
			else if(type.substring(16,22).equals("Wallet"))
			{
				isTransferDestinationWallet = true;
				int walletID = Integer.parseInt(type.substring(22,24));
				transferDestinationWallet = databaseAdapter.getWallet(walletID);
				transferDestinationName = transferDestinationWallet.getName();
			}
			else if(type.substring(18,22).equals("Bank"))
			{
				isTransferDestinationBank = true;
				int bankID = Integer.parseInt(type.substring(22,24));
				transferDestinationBank = databaseAdapter.getBank(bankID);
				transferDestinationName = transferDestinationBank.getName();
			}
			else if(type.substring(16,20).equals("Bank"))
			{
				isTransferDestinationBank = true;
				int bankID = Integer.parseInt(type.substring(20,22));
				transferDestinationBank = databaseAdapter.getBank(bankID);
				transferDestinationName = transferDestinationBank.getName();
			}
			else
			{
				Toast.makeText(context, "Unknown Transfer Destination", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public String getTransactionType()
	{
		return transactionType;
	}
	
	public boolean isIncome()
	{
		return isIncome;
	}
	
	public boolean isIncomeDestinationWallet()
	{
		return isIncomeDestinationWallet;
	}
	
	public NewWallet getIncomeDestinationWallet()
	{
		return incomeDestinationWallet;
	}
	
	public boolean isIncomeDestinationBank()
	{
		return isIncomeDestinationBank;
	}
	
	public Bank getIncomeDestinationBank()
	{
		return incomeDestinationBank;
	}

	public String getIncomeDestinationName()
	{
		return incomeDestinationName;
	}

	public boolean isExpense()
	{
		return isExpense;
	}
	
	public boolean isExpenseSourceWallet()
	{
		return isExpenseSourceWallet;
	}
	
	public NewWallet getExpenseSourceWallet()
	{
		return expenseSourceWallet;
	}
	
	public boolean isExpenseSourceBank()
	{
		return isExpenseSourceBank;
	}
	
	public Bank getExpenseSourceBank()
	{
		return expenseSourceBank;
	}

	public ExpenditureType getExpenditureType()
	{
		return expenditureType;
	}

	public String getExpenseSourceName()
	{
		return expenseSourceName;
	}

	public boolean isTransfer()
	{
		return isTransfer;
	}
	
	public boolean isTransferSourceWallet()
	{
		return isTransferSourceWallet;
	}
	
	public NewWallet getTransferSourceWallet()
	{
		return transferSourceWallet;
	}
	
	public boolean isTransferSourceBank()
	{
		return isTransferSourceBank;
	}
	
	public Bank getTransferSourceBank()
	{
		return transferSourceBank;
	}
	
	public boolean isTransferDestinationWallet()
	{
		return isTransferDestinationWallet;
	}
	
	public NewWallet getTransferDestinationWallet()
	{
		return transferDestinationWallet;
	}
	
	public boolean isTransferDestinationBank()
	{
		return isTransferDestinationBank;
	}
	
	public Bank getTransferDestinationBank()
	{
		return transferDestinationBank;
	}

	public String getTransferSourceName()
	{
		return transferSourceName;
	}

	public String getTransferDestinationName()
	{
		return transferDestinationName;
	}
}
