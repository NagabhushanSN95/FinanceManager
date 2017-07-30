// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Date;
import com.chaturvedi.financemanager.database.ExpenditureType;
import com.chaturvedi.financemanager.database.MoneyStorage;
import com.chaturvedi.financemanager.database.NewWallet;
import com.chaturvedi.financemanager.database.Time;
import com.chaturvedi.financemanager.database.Transaction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ExpenseLayout extends RelativeLayout
{
	private ArrayList<NewWallet> wallets;
	private ArrayList<Bank> banks;

	private Spinner expenseSourcesSpinner;
	private EditText particularsEditText;
	private Spinner expenseTypeSpinner;
	private EditText rateEditText;
	private EditText quantityEditText;
	private EditText amountEditText;
	private EditText dateEditText;
	private CheckBox addTemplateCheckBox;

	public ExpenseLayout(Context context)
	{
		super(context);
	}

	public ExpenseLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public ExpenseLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	private void buildLayout()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		/*ArrayList<NewWallet> wallets = databaseAdapter.getAllWalletsNames();
		ArrayList<Bank> banks = databaseAdapter.getAllBanksNames();
		ArrayList<String> expenseSourcesList = new ArrayList<String>(wallets.size() + banks.size());
		for(NewWallet wallet : wallets)
		{
			expenseSourcesList.add(wallet.getName());
		}
		for(Bank bank : banks)
		{
			expenseSourcesList.add(bank.getName());
		}*/

		ArrayList<String> expenseSourcesList = databaseAdapter.getAllWalletsNames();
		expenseSourcesList.addAll(databaseAdapter.getAllBanksNames());
		ArrayList<String> expenditureTypesList = databaseAdapter.getAllVisibleExpTypeNames();

		expenseSourcesSpinner = (Spinner) findViewById(R.id.spinner_expenseSource);
		expenseSourcesSpinner.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
				expenseSourcesList));
		particularsEditText = (EditText) findViewById(R.id.editText_particulars);
		expenseTypeSpinner = (Spinner) findViewById(R.id.spinner_expenseType);
		expenseTypeSpinner.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
				expenditureTypesList));
		rateEditText = (EditText) findViewById(R.id.editText_rate);
		quantityEditText = (EditText) findViewById(R.id.editText_quantity);
		amountEditText = (EditText) findViewById(R.id.edit_amount);

		final Calendar myCalendar = Calendar.getInstance();
		final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
								  int dayOfMonth) {
				myCalendar.set(Calendar.YEAR, year);
				myCalendar.set(Calendar.MONTH, monthOfYear);
				myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				Date date1 = new Date(myCalendar);
				dateEditText.setText(date1.getDisplayDate());
			}
		};
		dateEditText = (EditText) findViewById(R.id.editText_date);
		dateEditText.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new DatePickerDialog(getContext(), onDateSetListener, myCalendar.get(Calendar.YEAR),
						myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));
			}
		});

		addTemplateCheckBox = (CheckBox) findViewById(R.id.checkBox_addTemplate);
	}

	public boolean submit()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());
		DecimalFormat formatter = new DecimalFormat("00");

		String rateString = rateEditText.getText().toString().trim();
		String quantityString = quantityEditText.getText().toString().trim();
		String amountString = amountEditText.getText().toString().trim();

		if((rateString.length()==0) && (amountString.length()==0))
		{
			// Both rate and amount are empty
			Toast.makeText(getContext(), "Please enter Rate or Amount", Toast.LENGTH_LONG);
			return false;
		}

		// Calculate Rate, Quantity, Amount if not specified
		double rate, quantity, amount;
		if(quantityString.length() > 0)
		{
			quantity = Integer.parseInt(quantityString);
		}
		else
		{
			quantity = 1;
		}
		if(rateString.length() > 0)
		{
			rate = Integer.parseInt(rateString);

			if(amountString.length() > 0)
			{
				amount = Integer.parseInt(amountString);
			}
			else
			{
				amount = rate*quantity;
			}
		}
		else
		{
			amount = Integer.parseInt(amountString);
			rate = amount/quantity;
		}

		int id;
		try
		{
			id = databaseAdapter.getIDforNextTransaction();
		}
		catch (Exception e)
		{
			Toast.makeText(getContext(), "Failed due to some internal error. Please try again", Toast.LENGTH_LONG).show();
			Log.d("ExpenseLayout/submit()", e.getMessage(), e.fillInStackTrace());
			return false;
		}

		String code;
		int expenseSourcePosition = expenseSourcesSpinner.getSelectedItemPosition();
		MoneyStorage expSource;
		if(expenseSourcePosition < wallets.size())
		{
			expSource = databaseAdapter.getWalletFromName((String) expenseSourcesSpinner.getSelectedItem());
			code = "Debit Wallet" + formatter.format(expSource.getID());
		}
		else
		{
			expSource = databaseAdapter.getBankFromName((String) expenseSourcesSpinner.getSelectedItem());
			code = "Debit Bank" + formatter.format(expSource.getID());
		}

		ExpenditureType expenditureType = databaseAdapter.getExpTypeFromName((String) expenseTypeSpinner.getSelectedItem());
		code += " Exp" + formatter.format(expenditureType.getId());

		String particulars = particularsEditText.getText().toString().trim();
		Date date = new Date(dateEditText.getText().toString().trim());
		Calendar now = Calendar.getInstance();
		Time createdTime = new Time(now);
		Time modifiedTime = new Time(now);

		Transaction transaction = new Transaction(id, createdTime, modifiedTime, date, code, particulars, rate, quantity, amount);
		addTransaction(transaction);

		if(addTemplateCheckBox.isChecked())
		{

		}

		return true;
	}

	private boolean addTransaction(Transaction transaction)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		databaseAdapter.addTransaction(transaction);
		if(transaction.getType().contains("Wallet"))
		{
			int walletID = Integer.parseInt(transaction.getType().substring(12,14));

		}
		/*DatabaseManager.decreamentWalletBalance(transaction.getAmount());
		DatabaseManager.increamentAmountSpent(transaction.getDate(), transaction.getAmount());
		DatabaseManager.increamentCounters(transaction.getDate(), expTypeNo, transaction.getAmount());*/
		return true;
	}
}
