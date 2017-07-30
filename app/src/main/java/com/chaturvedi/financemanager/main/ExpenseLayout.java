// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.chaturvedi.customviews.MyAutoCompleteTextView;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.datastructures.Date;
import com.chaturvedi.financemanager.datastructures.ExpenditureType;
import com.chaturvedi.financemanager.datastructures.MoneyStorage;
import com.chaturvedi.financemanager.datastructures.Template;
import com.chaturvedi.financemanager.datastructures.Time;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ExpenseLayout extends RelativeLayout
{
	private Spinner expenseSourcesSpinner;
	private MyAutoCompleteTextView particularsEditText;
	private Spinner expenseTypeSpinner;
	private EditText rateEditText;
	private EditText quantityEditText;
	private EditText amountEditText;
	private EditText dateEditText;
	private CheckBox addTemplateCheckBox;

	public ExpenseLayout(Context context)
	{
		super(context);
		buildLayout();
	}

	public ExpenseLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		buildLayout();
	}

	public ExpenseLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		buildLayout();
	}

	private void buildLayout()
	{
		LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_expense, this);

		final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		ArrayList<String> expenseSourcesList = databaseAdapter.getAllVisibleWalletsNames();
		expenseSourcesList.addAll(databaseAdapter.getAllVisibleBanksNames());
		ArrayList<String> expenditureTypesList = databaseAdapter.getAllVisibleExpenditureTypeNames();

		expenseSourcesSpinner = (Spinner) findViewById(R.id.spinner_expenseSource);
		final ArrayAdapter<String> expenseSourcesAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
				expenseSourcesList);
		expenseSourcesSpinner.setAdapter(expenseSourcesAdapter);
		particularsEditText = (MyAutoCompleteTextView) findViewById(R.id.editText_particulars);
		expenseTypeSpinner = (Spinner) findViewById(R.id.spinner_expenseType);
		final ArrayAdapter<String> expenseTypeAdapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item, expenditureTypesList);
		expenseTypeSpinner.setAdapter(expenseTypeAdapter);
		rateEditText = (EditText) findViewById(R.id.editText_rate);
		quantityEditText = (EditText) findViewById(R.id.editText_quantity);
		amountEditText = (EditText) findViewById(R.id.editText_amount);

		final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener()
		{

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
			{
				Calendar myCalendar = Calendar.getInstance();
				myCalendar.set(Calendar.YEAR, year);
				myCalendar.set(Calendar.MONTH, monthOfYear);
				myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				Date date1 = new Date(myCalendar);
				dateEditText.setText(date1.getDisplayDate());
			}
		};
		dateEditText = (EditText) findViewById(R.id.editText_date);
		dateEditText.setText(new Date(Calendar.getInstance()).getDisplayDate());
		dateEditText.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Date date = new Date(dateEditText.getText().toString());
				// For DatePickerDialog, Month starts with 0. But in custom Date class, Month starts with 1.
				// Hence, 1 is subtracted
				new DatePickerDialog(getContext(), onDateSetListener, date.getYear(), date.getMonth()-1, date.getDate()).show();
			}
		});
		addTemplateCheckBox = (CheckBox) findViewById(R.id.checkBox_addTemplate);

		final ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(getContext(),
				R.layout.dropdown_multiline_item, R.id.textView_option, databaseAdapter.getVisibleDebitTemplatesNames());
		particularsEditText.setAdapter(templatesAdapter);
		particularsEditText.setThreshold(1);
		particularsEditText.setDropDownWidth(-1);	// To set drop down width to Match Parent
		particularsEditText.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				Template selectedTemplate = databaseAdapter.getTemplate(particularsEditText.getText().toString());
				TransactionTypeParser parser = new TransactionTypeParser(getContext(), selectedTemplate.getType());
				String expenseSourceName = parser.getExpenseSourceName();
				int expenseSourcePosition = expenseSourcesAdapter.getPosition(expenseSourceName);
				expenseSourcesSpinner.setSelection(expenseSourcePosition);
				String expenditureTypeName = parser.getExpenditureType().getName();
				int expenditureTypePosition = expenseTypeAdapter.getPosition(expenditureTypeName);
				expenseTypeSpinner.setSelection(expenditureTypePosition);
				rateEditText.setText(String.valueOf(selectedTemplate.getAmount()));
			}
		});

	}

	public void setData(Transaction transaction)
	{
		String transactionType = transaction.getType();
		TransactionTypeParser parser = new TransactionTypeParser(getContext(), transactionType);
		if (!parser.isExpense())
		{
			Toast.makeText(getContext(), "Transaction is not Expense", Toast.LENGTH_SHORT).show();
			return;
		}
		int expenseSourceNo;
		if (parser.isExpenseSourceWallet())
		{
			// IDs start with 1. Hence, 1 is subtracted
			expenseSourceNo = parser.getExpenseSourceWallet().getID() - 1;
		}
		else
		{
			// Banks are displayed after wallets. Hence, numWallets is added
			expenseSourceNo = DatabaseAdapter.getInstance(getContext()).getNumVisibleWallets() +
					parser.getExpenseSourceBank().getID() - 1;
		}
		expenseSourcesSpinner.setSelection(expenseSourceNo);

		particularsEditText.setText(transaction.getParticular());
		expenseTypeSpinner.setSelection(parser.getExpenditureType().getId() - 1); // -1 Since IDs start with 1
		rateEditText.setText(String.valueOf(transaction.getRate()));
		quantityEditText.setText(String.valueOf(transaction.getQuantity()));
		amountEditText.setText(String.valueOf(transaction.getAmount()));
		dateEditText.setText(String.valueOf(transaction.getDate().getDisplayDate()));
	}

	public void setData(int expenseSourceNo, String particulars, String rateText, String quantityText, String amountText,
						String date, boolean addTemplate)
	{
		expenseSourcesSpinner.setSelection(expenseSourceNo);
		particularsEditText.setText(particulars);
		rateEditText.setText(rateText);
		quantityEditText.setText(quantityText);
		amountEditText.setText(amountText);
		dateEditText.setText(date);
		addTemplateCheckBox.setSelected(addTemplate);
	}

	public void setData(int bankID, double amount)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		// -1 because BankIDs start with 1 and index start with 0
		int expenseSourceNo = databaseAdapter.getNumVisibleWallets() + bankID - 1;
		expenseSourcesSpinner.setSelection(expenseSourceNo);

		amountEditText.setText(String.valueOf(amount));
		//dateEditText.setText(new Date(Calendar.getInstance()).getDisplayDate());
	}

	public Transaction submit()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());
		DecimalFormat formatter = new DecimalFormat("00");

		String rateString = rateEditText.getText().toString().trim();
		String quantityString = quantityEditText.getText().toString().trim();
		String amountString = amountEditText.getText().toString().trim();

		if ((rateString.length() == 0) && (amountString.length() == 0))
		{
			// Both rate and amount are empty
			Toast.makeText(getContext(), "Please enter Rate or Amount", Toast.LENGTH_LONG).show();
			return null;
		}

		// Calculate Rate, Quantity, Amount if not specified
		double rate, quantity, amount;
		if (quantityString.length() > 0)
		{
			quantity = Double.parseDouble(quantityString);
		}
		else
		{
			quantity = 1;
		}
		if (rateString.length() > 0)
		{
			rate = Double.parseDouble(rateString);

			if (amountString.length() > 0)
			{
				amount = Double.parseDouble(amountString);
			}
			else
			{
				amount = rate * quantity;
			}
		}
		else
		{
			amount = Double.parseDouble(amountString);
			rate = amount / quantity;
		}

		int id = databaseAdapter.getIDforNextTransaction();
		if (id == -1)
		{
			Toast.makeText(getContext(), "Failed due to some internal error. Please try again", Toast.LENGTH_LONG).show();
			return null;
		}

		String code;
		int expenseSourcePosition = expenseSourcesSpinner.getSelectedItemPosition();
		MoneyStorage expSource;
		// Debit Wallet01 Exp01 or
		// Debit Bank01 Exp01
		if (expenseSourcePosition < databaseAdapter.getNumVisibleWallets())
		{
			expSource = databaseAdapter.getWalletFromName((String) expenseSourcesSpinner.getSelectedItem());
			code = "Debit Wallet" + formatter.format(expSource.getID());
		}
		else
		{
			expSource = databaseAdapter.getBankFromName((String) expenseSourcesSpinner.getSelectedItem());
			code = "Debit Bank" + formatter.format(expSource.getID());
		}

		ExpenditureType expenditureType = databaseAdapter.getExpenditureTypeFromName((String) expenseTypeSpinner.getSelectedItem());
		code += " Exp" + formatter.format(expenditureType.getId());

		String particulars = particularsEditText.getText().toString().trim();
		Date date = new Date(dateEditText.getText().toString().trim());
		Calendar now = Calendar.getInstance();
		Time createdTime = new Time(now);
		Time modifiedTime = new Time(now);

		Transaction transaction = new Transaction(id, createdTime, modifiedTime, date, code, particulars, rate, quantity, amount,
				false);
//		addTransaction(transaction);

		if (addTemplateCheckBox.isChecked())
		{
			// Here Template ID is not added. It has to be set in DatabaseManager
			Template template = new Template(0, transaction.getParticular(), transaction.getType(),
					transaction.getRate(), false);
			DatabaseManager.addTemplate(getContext(), template);
		}

		return transaction;
	}

	public int getExpenseSourcePosition()
	{
		return expenseSourcesSpinner.getSelectedItemPosition();
	}

	public String getParticulars()
	{
		return particularsEditText.getText().toString().trim();
	}

	public String getRateText()
	{
		return rateEditText.getText().toString();
	}

	public String getQuantityText()
	{
		return quantityEditText.getText().toString();
	}

	public String getAmountText()
	{
		return amountEditText.getText().toString();
	}

	public String getDate()
	{
		return dateEditText.getText().toString();
	}

	public boolean isAddTemplateSelected()
	{
		return addTemplateCheckBox.isSelected();
	}

}
