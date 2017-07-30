// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Date;
import com.chaturvedi.financemanager.database.MoneyStorage;
import com.chaturvedi.financemanager.database.Template;
import com.chaturvedi.financemanager.database.Time;
import com.chaturvedi.financemanager.database.Transaction;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class IncomeLayout extends RelativeLayout
{
	private Spinner incomeDestinationSpinner;
	private EditText particularsEditText;
	private EditText rateEditText;
	private EditText quantityEditText;
	private EditText amountEditText;
	private EditText dateEditText;
	private CheckBox addTemplateCheckBox;

	public IncomeLayout(Context context)
	{
		super(context);
		buildLayout();
	}

	public IncomeLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		buildLayout();
	}

	public IncomeLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		buildLayout();
	}

	private void buildLayout()
	{
		LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_income, this);

		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		ArrayList<String> incomeDestinationsList = databaseAdapter.getAllVisibleWalletsNames();
		incomeDestinationsList.addAll(databaseAdapter.getAllVisibleBanksNames());

		incomeDestinationSpinner = (Spinner) this.findViewById(R.id.spinner_incomeDestination);
		incomeDestinationSpinner.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
				incomeDestinationsList));
		particularsEditText = (EditText) findViewById(R.id.editText_particulars);
		rateEditText = (EditText) findViewById(R.id.editText_rate);
		quantityEditText = (EditText) findViewById(R.id.editText_quantity);
		amountEditText = (EditText) findViewById(R.id.editText_amount);

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
		dateEditText.setText(new Date(Calendar.getInstance()).getDisplayDate());
		dateEditText.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new DatePickerDialog(getContext(), onDateSetListener, myCalendar.get(Calendar.YEAR),
						myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
			}
		});

		addTemplateCheckBox = (CheckBox) findViewById(R.id.checkBox_addTemplate);
	}

	public void setData(Transaction transaction)
	{
		String transactionType = transaction.getType();
		TransactionTypeParser parser = new TransactionTypeParser(getContext(), transactionType);
		if(!parser.isIncome())
		{
			Toast.makeText(getContext(), "Transaction is not Income", Toast.LENGTH_SHORT).show();
			return;
		}
		int incomeDestinationNo;
		if(parser.isIncomeDestinationWallet())
		{
			// IDs start with 1. Hence, 1 is subtracted
			incomeDestinationNo = parser.getIncomeDestinationWallet().getID() - 1;
		}
		else
		{
			// Banks are displayed after wallets. Hence, numWallets is added
			incomeDestinationNo = DatabaseAdapter.getInstance(getContext()).getNumVisibleWallets() +
					parser.getIncomeDestinationBank().getID() - 1;
		}
		incomeDestinationSpinner.setSelection(incomeDestinationNo);

		particularsEditText.setText(transaction.getParticular());
		rateEditText.setText(String.valueOf(transaction.getRate()));
		quantityEditText.setText(String.valueOf(transaction.getQuantity()));
		amountEditText.setText(String.valueOf(transaction.getAmount()));
		dateEditText.setText(String.valueOf(transaction.getDate().getDisplayDate()));
	}

	public void setData(int incomeDestinationNo, String particulars, String rateText, String quantityText, String amountText,
						String date, boolean addTemplate)
	{
		incomeDestinationSpinner.setSelection(incomeDestinationNo);
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
		int incomeDestinationNo = databaseAdapter.getNumVisibleWallets() + bankID-1;
		incomeDestinationSpinner.setSelection(incomeDestinationNo);

		amountEditText.setText(String.valueOf(amount));
		//dateEditText.setText(new Date(Calendar.getInstance()).getDisplayDate());
	}

	/**
	 *
	 * @return Transaction object created from the data entered (if data is valid data)
	 * 			null if the data entered is invalid
	 */
	public Transaction submit()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());
		DecimalFormat formatter = new DecimalFormat("00");

		String rateString = rateEditText.getText().toString().trim();
		String quantityString = quantityEditText.getText().toString().trim();
		String amountString = amountEditText.getText().toString().trim();

		if((rateString.length()==0) && (amountString.length()==0))
		{
			// Both rate and amount are empty
			Toast.makeText(getContext(), "Please enter Rate or Amount", Toast.LENGTH_LONG).show();
			return null;
		}

		// Calculate Rate, Quantity, Amount if not specified
		double rate, quantity, amount;
		if(quantityString.length() > 0)
		{
			quantity = Double.parseDouble(quantityString);
		}
		else
		{
			quantity = 1;
		}
		if(rateString.length() > 0)
		{
			rate = Double.parseDouble(rateString);

			if(amountString.length() > 0)
			{
				amount = Double.parseDouble(amountString);
			}
			else
			{
				amount = rate*quantity;
			}
		}
		else
		{
			amount = Double.parseDouble(amountString);
			rate = amount/quantity;
		}

		int id = databaseAdapter.getIDforNextTransaction();
		if(id == -1)
		{
			Toast.makeText(getContext(), "Failed due to some internal error. Please try again", Toast.LENGTH_LONG).show();
			return null;
		}

		String code;
		int incomeDestinationPosition = incomeDestinationSpinner.getSelectedItemPosition();
		MoneyStorage incomeDestination;
		// Credit Wallet01 or
		// Credit Bank01
		if(incomeDestinationPosition < databaseAdapter.getNumVisibleWallets())
		{
			incomeDestination = databaseAdapter.getWalletFromName((String) incomeDestinationSpinner.getSelectedItem());
			code = "Credit Wallet" + formatter.format(incomeDestination.getID());
		}
		else
		{
			incomeDestination = databaseAdapter.getBankFromName((String) incomeDestinationSpinner.getSelectedItem());
			code = "Credit Bank" + formatter.format(incomeDestination.getID());
		}

		String particulars = particularsEditText.getText().toString().trim();
		Date date = new Date(dateEditText.getText().toString().trim());
		Calendar now = Calendar.getInstance();
		Time createdTime = new Time(now);
		Time modifiedTime = new Time(now);

		Transaction transaction = new Transaction(id, createdTime, modifiedTime, date, code, particulars, rate, quantity, amount,
				false);
//		addTransaction(transaction);

		if(addTemplateCheckBox.isChecked())
		{
			// Here Template ID is not added. It has to be set in DatabaseManager
			Template template = new Template(0, transaction.getParticular(), transaction.getType(),
					transaction.getRate(), false);
			DatabaseManager.addTemplate(getContext(), template);
		}

		return transaction;
	}

	public int getIncomeDestinationPosition()
	{
		return incomeDestinationSpinner.getSelectedItemPosition();
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
