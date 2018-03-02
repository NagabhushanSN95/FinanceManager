// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.chaturvedi.customviews.HintAdapter;
import com.chaturvedi.customviews.MyAutoCompleteTextView;
import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Time;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.datastructures.MoneyStorage;
import com.chaturvedi.financemanager.datastructures.Template;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class IncomeLayout extends RelativeLayout
{
	private Spinner incomeDestinationSpinner;
	private MyAutoCompleteTextView particularsEditText;
	private EditText rateEditText;
	private EditText quantityEditText;
	private EditText amountEditText;
	private EditText dateEditText;
	private CheckBox addTemplateCheckBox;
	private CheckBox excludeInCountersCheckBox;

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

		final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		ArrayList<String> incomeDestinationsList = databaseAdapter.getAllVisibleWalletsNames();
		incomeDestinationsList.addAll(databaseAdapter.getAllVisibleBanksNames());

		incomeDestinationSpinner = (Spinner) this.findViewById(R.id.spinner_incomeDestination);
		final HintAdapter incomeDestinationAdapter = new HintAdapter(getContext(), android.R
				.layout.simple_spinner_item,
				incomeDestinationsList);
		incomeDestinationSpinner.setAdapter(incomeDestinationAdapter);
		incomeDestinationSpinner.setSelection(incomeDestinationAdapter.getCount());    // Set Hint
		particularsEditText = (MyAutoCompleteTextView) findViewById(R.id.editText_particulars);
		rateEditText = (EditText) findViewById(R.id.editText_rate);
		quantityEditText = (EditText) findViewById(R.id.editText_quantity);
		amountEditText = (EditText) findViewById(R.id.editText_amount);

		final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
								  int dayOfMonth) {
				Calendar myCalendar = Calendar.getInstance();
				myCalendar.set(Calendar.YEAR, year);
				myCalendar.set(Calendar.MONTH, monthOfYear);
				myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				Date date1 = new Date(myCalendar);
				dateEditText.setText(date1.getDisplayDate("/"));
			}
		};
		dateEditText = (EditText) findViewById(R.id.editText_date);
		dateEditText.setText(new Date(Calendar.getInstance()).getDisplayDate("/"));
		dateEditText.setOnClickListener(new OnClickListener()
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
		excludeInCountersCheckBox = (CheckBox) findViewById(R.id.checkBox_excludeFromCounters);
		
		final ArrayAdapter<String> templatesAdapter = new ArrayAdapter<>(getContext(),
				R.layout.dropdown_multiline_item, R.id.textView_option, databaseAdapter.getVisibleCreditTemplatesNames());
		particularsEditText.setAdapter(templatesAdapter);
		particularsEditText.setThreshold(1);
		particularsEditText.setDropDownWidth(-1);	// To set drop down width to Match Parent
		particularsEditText.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				Template selectedTemplate = databaseAdapter.getTemplate(particularsEditText.getText().toString());
				String incomeDestinationName = (new TransactionTypeParser(getContext(), selectedTemplate.getType())).
						getIncomeDestinationName();
				int incomeDestinationPosition = incomeDestinationAdapter.getPosition(incomeDestinationName);
				incomeDestinationSpinner.setSelection(incomeDestinationPosition);
				rateEditText.setText(String.valueOf(selectedTemplate.getAmount()));
			}
		});
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
		dateEditText.setText(String.valueOf(transaction.getDate().getDisplayDate("/")));
		excludeInCountersCheckBox.setChecked(!transaction.isIncludeInCounters());
	}

	public void setData(int incomeDestinationNo, String particulars, String rateText, String quantityText, String amountText,
						String date, boolean addTemplate, boolean excludeInCounters)
	{
		incomeDestinationSpinner.setSelection(incomeDestinationNo);
		particularsEditText.setText(particulars);
		rateEditText.setText(rateText);
		quantityEditText.setText(quantityText);
		amountEditText.setText(amountText);
		dateEditText.setText(date);
		addTemplateCheckBox.setSelected(addTemplate);
		excludeInCountersCheckBox.setChecked(excludeInCounters);
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
		if (incomeDestinationSpinner.getSelectedItem().equals(HintAdapter.HINT_TEXT))
		{
			Toast.makeText(getContext(), "Please select a wallet or bank where the money is " +
					"credited", Toast.LENGTH_LONG).show();
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
		boolean excludeInCounters = excludeInCountersCheckBox.isChecked();

		Transaction transaction = new Transaction(id, createdTime, modifiedTime, date, code, particulars, rate, quantity, amount,
				false, !excludeInCounters);

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
		return addTemplateCheckBox.isChecked();
	}
	
	public boolean isExcludeInCounters()
	{
		return excludeInCountersCheckBox.isChecked();
	}
}
