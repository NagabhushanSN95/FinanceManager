// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.*;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.functions.Utilities;

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
		String[] transactionTypes = {Constants.TRANSACTION_INCOME, Constants.TRANSACTION_EXPENSE, Constants.TRANSACTION_TRANSFER};
		transactionTypeSpinner.setAdapter(new ArrayAdapter<>(AddTransactionActivity.this,
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
		switch (action)
		{
			case Constants.ACTION_ADD:
			{
				String transactionType = intent.getStringExtra(Constants.TRANSACTION_TYPE);
				switch (transactionType)
				{
					case Constants.TRANSACTION_INCOME:
						transactionTypeSpinner.setSelection(0);
						break;
					case Constants.TRANSACTION_EXPENSE:
						transactionTypeSpinner.setSelection(1);
						break;
					case Constants.TRANSACTION_TRANSFER:
						transactionTypeSpinner.setSelection(2);
						break;
				}
				break;
			}
			case Constants.ACTION_EDIT:
			{
				oldTransaction = intent.getParcelableExtra(Constants.TRANSACTION);
				checkForValidity(oldTransaction);
				if (oldTransaction.getType().contains("Credit"))
				{
					transactionTypeSpinner.setSelection(0);
					incomeLayout.setData(oldTransaction);
				}
				else if (oldTransaction.getType().contains("Debit"))
				{
					transactionTypeSpinner.setSelection(1);
					// The above statement initiates a callback to change layout from IncomeLayout
					// (which is default) to ExpenseLayout
					// The callback replaces contents of ExpenseLayout fields with those of
					// IncomeLayout fields
					// But the callback is called after all methods are executed.
					// So, if expenseLayout.setData() is called without a handler, this method is
					// executed first and then the callback.
					// As a result, there won't be any data filled in the fields of expenseLayout.
					// Using handler ensures that expenseLayout.setData() is called after the
					// callback
					new Handler().postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							expenseLayout.setData(oldTransaction);
						}
					}, 300);
				}
				else if (oldTransaction.getType().contains("Transfer"))
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
				}
				else
				{
					Toast.makeText(this, "Unable to detect Transaction Type of old transaction",
							Toast.LENGTH_SHORT).show();
					transactionTypeSpinner.setSelection(0);
				}
				break;
			}
			case Constants.ACTION_BANK_SMS:
			{
				String transactionType = intent.getStringExtra(Constants.TRANSACTION_TYPE);
				int bankID = intent.getIntExtra(Constants.KEY_BANK_ID, 1);
				double amount = intent.getDoubleExtra(Constants.KEY_AMOUNT, 0);
				switch (transactionType)
				{
					case Constants.TRANSACTION_INCOME:
						transactionTypeSpinner.setSelection(0);
						expenseLayout.setData(bankID, amount);
						break;
					case Constants.TRANSACTION_EXPENSE:
						transactionTypeSpinner.setSelection(1);
						incomeLayout.setData(bankID, amount);
						break;
					case Constants.TRANSACTION_TRANSFER:
						transactionTypeSpinner.setSelection(2);
						String transferType = intent.getStringExtra(Constants.TRANSFER_TYPE);
						transferLayout.setData(transferType, bankID, amount);
						break;
				}
				break;
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

		String particulars, rate, quantity, amount, date;
		boolean addTemplate, includeInCounters;
		
		switch (previousPosition)
		{
			case 0:					// Income Layout
				particulars = incomeLayout.getParticulars();
				rate = incomeLayout.getRateText();
				quantity = incomeLayout.getQuantityText();
				amount = incomeLayout.getAmountText();
				date = incomeLayout.getDate();
				addTemplate = incomeLayout.isAddTemplateSelected();
				includeInCounters = !incomeLayout.isExcludeInCounters();
				
				String incomeDestinationName = incomeLayout.getIncomeDestinationName();
				switch (currentPosition)
				{
					case 1:
						expenseLayout.setData(incomeDestinationName, particulars, rate, quantity,
								amount, date, addTemplate, !includeInCounters);
						break;

					case 2:
						transferLayout.setData(null, incomeDestinationName, particulars, rate,
								quantity, amount, date, addTemplate, !includeInCounters);
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
				includeInCounters = !expenseLayout.isExcludeInCounters();
				
				String expenseSourceName = expenseLayout.getExpenseSourceName();
				switch (currentPosition)
				{
					case 0:
						incomeLayout.setData(expenseSourceName, particulars, rate, quantity, amount, date,
								addTemplate, !includeInCounters);
						break;

					case 2:
						transferLayout.setData(expenseSourceName, null, particulars, rate, quantity, amount,
								date, addTemplate, !includeInCounters);
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
				includeInCounters = !transferLayout.isExcludeInCounters();

				switch (currentPosition)
				{
					case 0:
						String transferDestinationName = transferLayout
								.getTransferDestinationName();
						incomeLayout.setData(transferDestinationName, particulars, rate, quantity,
								amount, date, addTemplate, !includeInCounters);
						break;

					case 1:
						String transferSourceName = transferLayout.getTransferSourceName();
						expenseLayout.setData(transferSourceName, particulars, rate, quantity, amount,
								date, addTemplate, !includeInCounters);
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
		
		switch (action)
		{
			case Constants.ACTION_ADD:
				DatabaseManager.addTransaction(AddTransactionActivity.this, transaction, true);
				break;
			case Constants.ACTION_EDIT:
				transaction.setID(oldTransaction.getID());
				transaction.setCreatedTime(oldTransaction.getCreatedTime());
				DatabaseManager.editTransaction(AddTransactionActivity.this, oldTransaction,
						transaction);
				break;
			case Constants.ACTION_BANK_SMS:
				DatabaseManager.addTransaction(AddTransactionActivity.this, transaction, true);
				break;
			default:
				Toast.makeText(AddTransactionActivity.this, "Unknown Action: " + action, Toast
						.LENGTH_LONG).show();
				break;
		}
		return transaction;
	}

	private void checkForValidity(Transaction oldTransaction)
	{
		boolean invalid = Utilities.isTransactionValidForEditing(AddTransactionActivity.this, oldTransaction);
		
		if(invalid)
		{
			Toast.makeText(AddTransactionActivity.this, "Unsupported Action. This transaction is " +
					"dependent on Deleted Wallets," +
					"or Banks or Expenditure Types. Please restore it in EditData Activity and try" +
					" again. Please contact Developer for any assistance", Toast.LENGTH_LONG).show();
			Intent resultIntent = new Intent();
			setResult(Activity.RESULT_CANCELED, resultIntent);
			finish();
		}
	}
}
