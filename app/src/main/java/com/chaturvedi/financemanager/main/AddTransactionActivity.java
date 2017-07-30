// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Transaction;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

public class AddTransactionActivity extends Activity
{
	private String action;
	private Transaction oldTransaction;

	private int WIDTH_BUTTONS;

	private Spinner transactionTypeSpinner;
	private int transactionTypeSpinnerPosition;
	private IncomeLayout incomeLayout;
	private ExpenseLayout expenseLayout;
	private TransferLayout transferLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_transaction);

		calculateDimensions();
		buildLayout(getIntent());
	}

	private void calculateDimensions()
	{
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int screenWidth = displayMetrics.widthPixels;
		//int screenHeight = displayMetrics.heightPixels;

		WIDTH_BUTTONS = screenWidth/2;
	}

	private void buildLayout(Intent intent)
	{
		incomeLayout = (IncomeLayout) findViewById(R.id.incomeLayout);
		expenseLayout = (ExpenseLayout) findViewById(R.id.expenseLayout);
		transferLayout = (TransferLayout) findViewById(R.id.transferLayout);

		transactionTypeSpinner = (Spinner) findViewById(R.id.spinner_transactionType);
		transactionTypeSpinnerPosition = 0;
		String[] transactionTypes = {"Income", "Expense", "Transfer"};
		transactionTypeSpinner.setAdapter(new ArrayAdapter<String>(AddTransactionActivity.this,
				android.R.layout.simple_spinner_item, transactionTypes));
		transactionTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				changeLayout(transactionTypeSpinnerPosition, position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

		action = intent.getStringExtra(Constants.ACTION);
		if (action.equals(Constants.ACTION_ADD))
		{
			String transactionType = intent.getStringExtra(Constants.TRANSACTION_TYPE);
			if(transactionType.equals(Constants.TRANSACTION_INCOME))
			{
				transactionTypeSpinner.setSelection(0);
			}
			else if(transactionType.equals(Constants.TRANSACTION_EXPENSE))
			{
				transactionTypeSpinner.setSelection(1);
			}
			else if(transactionType.equals(Constants.TRANSACTION_TRANSFER))
			{
				transactionTypeSpinner.setSelection(2);
			}
		}
		else if (action.equals(Constants.ACTION_EDIT))
		{
			oldTransaction = intent.getParcelableExtra(Constants.TRANSACTION);
			checkForValidity(oldTransaction);
			if(oldTransaction.getType().contains("Credit"))
			{
				transactionTypeSpinner.setSelection(0);
				incomeLayout.setData(oldTransaction);
			}
			else if(oldTransaction.getType().contains("Debit"))
			{
				transactionTypeSpinner.setSelection(1);
				// The above statement initiates a callback to change layout from IncomeLayout (which is default) to ExpenseLayout
				// The callback replaces contents of ExpenseLayout fields with those of IncomeLayout fields
				// But the call back is called after all methods are executed. So, is expenseLayout.setData() is called
				// without a handler, this method is executed first and then the callback. As a result, there won't be any data
				// filled in the fields of expenseLayout. Using handler ensures that expenseLayout.setData() is called
				// after the callback
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						expenseLayout.setData(oldTransaction);
					}
				}, 300);
				//expenseLayout.setData(oldTransaction);
			}
			else if(oldTransaction.getType().contains("Transfer"))
			{
				transactionTypeSpinner.setSelection(2);
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						transferLayout.setData(oldTransaction);
					}
				}, 300);
				//transferLayout.setData(oldTransaction);
			}
			else
			{
				Toast.makeText(this, "Unable to detect Transaction Type of old transaction", Toast.LENGTH_SHORT).show();
				transactionTypeSpinner.setSelection(0);
			}
		}
		else if(action.equals(Constants.ACTION_BANK_SMS))
		{
			String transactionType = intent.getStringExtra(Constants.TRANSACTION_TYPE);
			int bankID = intent.getIntExtra(Constants.KEY_BANK_ID, 1);
			double amount = intent.getDoubleExtra(Constants.KEY_AMOUNT, 0);
			if(transactionType.equals(Constants.TRANSACTION_INCOME))
			{
				transactionTypeSpinner.setSelection(0);
				expenseLayout.setData(bankID, amount);
			}
			else if(transactionType.equals(Constants.TRANSACTION_EXPENSE))
			{
				transactionTypeSpinner.setSelection(1);
				incomeLayout.setData(bankID, amount);
			}
			else if(transactionType.equals(Constants.TRANSACTION_TRANSFER))
			{
				transactionTypeSpinner.setSelection(2);
				String transferType = intent.getStringExtra(Constants.TRANSFER_TYPE);
				transferLayout.setData(transferType, bankID, amount);
			}
		}

		Button addButton = (Button) findViewById(R.id.button_add);
		LinearLayout.LayoutParams addButtonParams = (LinearLayout.LayoutParams)addButton.getLayoutParams();
		addButtonParams.width = WIDTH_BUTTONS;
		addButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Transaction transaction = submit();
				if(transaction != null)
				{
					Intent resultIntent = new Intent();
					resultIntent.putExtra(Constants.TRANSACTION, transaction);
					setResult(Activity.RESULT_OK, resultIntent);
					finish();
				}
			}
		});
		if(action.equals(Constants.ACTION_EDIT))
		{
			addButton.setText(getResources().getText(R.string.save));
		}

		Button cancelButton = (Button) findViewById(R.id.button_cancel);
		LinearLayout.LayoutParams cancelButtonParams = (LinearLayout.LayoutParams)cancelButton.getLayoutParams();
		cancelButtonParams.width = WIDTH_BUTTONS;
		cancelButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED, resultIntent);
				finish();
			}
		});
	}

	private void changeLayout(int previousPosition, int currentPosition)
	{
		if(previousPosition == currentPosition)
		{
			return;
		}

		int position;
		String particulars, rate, quantity, amount, date;
		boolean addTemplate;
		
		switch (previousPosition)
		{
			case 0:					// Income Layout
				particulars = incomeLayout.getParticulars();
				rate = incomeLayout.getRateText();
				quantity = incomeLayout.getQuantityText();
				amount = incomeLayout.getAmountText();
				date = incomeLayout.getDate();
				addTemplate = incomeLayout.isAddTemplateSelected();

				position = incomeLayout.getIncomeDestinationPosition();
				switch (currentPosition)
				{
					case 1:
						expenseLayout.setData(position,particulars,rate,quantity,amount,date,addTemplate);
						break;

					case 2:
						transferLayout.setData(-1,position,particulars,rate,quantity,amount,date,addTemplate);
						break;
				}
				break;
			
			case 1:					// Expense Layout
				particulars = expenseLayout.getParticulars();
				rate = expenseLayout.getRateText();
				quantity = expenseLayout.getQuantityText();
				amount = expenseLayout.getAmountText();
				date = expenseLayout.getDate();
				addTemplate = expenseLayout.isAddTemplateSelected();

				position = expenseLayout.getExpenseSourcePosition();
				switch (currentPosition)
				{
					case 0:
						incomeLayout.setData(position,particulars,rate,quantity,amount,date,addTemplate);
						break;

					case 2:
						transferLayout.setData(position,-1,particulars,rate,quantity,amount,date,addTemplate);
						break;
				}
				break;
			
			case 2:					// Transfer Layout
				particulars = transferLayout.getParticulars();
				rate = transferLayout.getRateText();
				quantity = transferLayout.getQuantityText();
				amount = transferLayout.getAmountText();
				date = transferLayout.getDate();
				addTemplate = transferLayout.isAddTemplateSelected();

				switch (currentPosition)
				{
					case 0:
						int destinationPosition = transferLayout.getTransferDestinationPosition();
						incomeLayout.setData(destinationPosition,particulars,rate,quantity,amount,date,addTemplate);
						break;

					case 1:
						int sourcePosition = transferLayout.getTransferSourcePosition();
						expenseLayout.setData(sourcePosition,particulars,rate,quantity,amount,date,addTemplate);
						break;
				}
				break;
		}

		// Display a single Layout based on current position in transactionTypeSpinner
		switch (currentPosition)
		{
			case 0:
				incomeLayout.setVisibility(View.VISIBLE);
				expenseLayout.setVisibility(View.GONE);
				transferLayout.setVisibility(View.GONE);
				break;

			case 1:
				incomeLayout.setVisibility(View.GONE);
				expenseLayout.setVisibility(View.VISIBLE);
				transferLayout.setVisibility(View.GONE);
				break;

			case 2:
				incomeLayout.setVisibility(View.GONE);
				expenseLayout.setVisibility(View.GONE);
				transferLayout.setVisibility(View.VISIBLE);
				break;
		}
		transactionTypeSpinnerPosition = currentPosition;
	}

	@Override
	public void onBackPressed()
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(AddTransactionActivity.this);
		dialog.setTitle("Cancel Transaction?");
		dialog.setMessage("Do you want to abort adding Transaction and return?");
		dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED, resultIntent);
				AddTransactionActivity.super.onBackPressed();
			}
		});
		dialog.setNegativeButton("No", null);
		dialog.show();
	}

	/**
	 * Retrieves Transaction Object from the corresponding Layout class and
	 * @return Transaction object created from the data entered (if data is valid data)
	 * 			null if the data entered is invalid
	 */
	private Transaction submit()
	{
		Transaction transaction = null;
		switch (transactionTypeSpinner.getSelectedItemPosition())
		{
			case 0:
				transaction = incomeLayout.submit();
				break;

			case 1:
				transaction = expenseLayout.submit();
				break;

			case 2:
				transaction = transferLayout.submit();
		}

		if(transaction == null)
		{
			return null;
		}

		if(action.equals(Constants.ACTION_ADD))
		{
			DatabaseManager.addTransaction(AddTransactionActivity.this, transaction, true);
		}
		else if(action.equals(Constants.ACTION_EDIT))
		{
			transaction.setID(oldTransaction.getID());
			transaction.setCreatedTime(oldTransaction.getCreatedTime());
			DatabaseManager.editTransaction(AddTransactionActivity.this, oldTransaction, transaction);
		}
		else if(action.equals(Constants.ACTION_BANK_SMS))
		{
			DatabaseManager.addTransaction(AddTransactionActivity.this, transaction, true);
		}
		else
		{
			Toast.makeText(AddTransactionActivity.this, "Unknown Action: " + action, Toast.LENGTH_LONG).show();
		}
		return transaction;
	}

	private void checkForValidity(Transaction oldTransaction)
	{
		boolean invalid = false;
		TransactionTypeParser parser = new TransactionTypeParser(AddTransactionActivity.this, oldTransaction.getType());
		if(parser.isIncome())
		{
			if(parser.isIncomeDestinationWallet() && parser.getIncomeDestinationWallet().isDeleted())
			{
				invalid = true;
			}
			else if(parser.isIncomeDestinationBank() && parser.getIncomeDestinationBank().isDeleted())
			{
				invalid = true;
			}
			/*else
			{
				Toast.makeText(AddTransactionActivity.this, "Unknown Income Destination", Toast.LENGTH_LONG).show();
			}*/
		}
		else if(parser.isExpense())
		{
			if(parser.isExpenseSourceWallet() && parser.getExpenseSourceWallet().isDeleted())
			{
				invalid = true;
			}
			else if(parser.isExpenseSourceBank() && parser.getExpenseSourceBank().isDeleted())
			{
				invalid = true;
			}
			/*else
			{
				Toast.makeText(AddTransactionActivity.this, "Unknown Expense Source", Toast.LENGTH_LONG).show();
			}*/

			if(parser.getExpenditureType().isDeleted())
			{
				invalid = true;
			}
		}
		else if(parser.isTransfer())
		{
			if(parser.isTransferSourceWallet() && parser.getTransferSourceWallet().isDeleted())
			{
				invalid = true;
			}
			else if(parser.isTransferSourceBank() && parser.getTransferSourceBank().isDeleted())
			{
				invalid = true;
			}
			/*else
			{
				Toast.makeText(AddTransactionActivity.this, "Unknown Transfer Source", Toast.LENGTH_LONG).show();
			}*/

			if(parser.isTransferDestinationWallet() && parser.getTransferDestinationWallet().isDeleted())
			{
				invalid = true;
			}
			else if(parser.isTransferDestinationBank() && parser.getTransferDestinationBank().isDeleted())
			{
				invalid = true;
			}
			/*else
			{
				Toast.makeText(AddTransactionActivity.this, "Unknown Transfer Destination", Toast.LENGTH_LONG).show();
			}*/
		}
		else
		{
			Toast.makeText(AddTransactionActivity.this, "Unknown Transaction Type", Toast.LENGTH_LONG).show();
		}

		if(invalid)
		{
			Toast.makeText(AddTransactionActivity.this, "Unsupported Action. This transaction is dependent on Deleted Wallets," +
					"or Banks or Expenditure Types. Please contact Developer for assistance", Toast.LENGTH_LONG).show();
			Intent resultIntent = new Intent();
			setResult(Activity.RESULT_CANCELED, resultIntent);
			finish();
		}
	}
}
