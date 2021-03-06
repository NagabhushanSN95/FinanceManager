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
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class TransferLayout extends RelativeLayout
{
	private Spinner transferSourcesSpinner;
	private HintAdapter transferSourcesAdapter;
	private Spinner transferDestinationsSpinner;
	private HintAdapter transferDestinationsAdapter;
	private MyAutoCompleteTextView particularsEditText;
	private EditText rateEditText;
	private EditText quantityEditText;
	private EditText amountEditText;
	private EditText dateEditText;
	private CheckBox addTemplateCheckBox;
	private CheckBox excludeInCountersCheckBox;

	public TransferLayout(Context context)
	{
		super(context);
		buildLayout();
	}

	public TransferLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		buildLayout();
	}

	public TransferLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		buildLayout();
	}

	private void buildLayout()
	{
		LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_transfer, this);

		final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());

		ArrayList<String> transferSourcesList = databaseAdapter.getAllVisibleWalletsNames();
		transferSourcesList.addAll(databaseAdapter.getAllVisibleBanksNames());

		ArrayList<String> transferDestinationsList = databaseAdapter.getAllVisibleWalletsNames();
		transferDestinationsList.addAll(databaseAdapter.getAllVisibleBanksNames());

		transferSourcesSpinner = (Spinner) findViewById(R.id.spinner_transferSource);
		transferSourcesAdapter = new HintAdapter(getContext(), android.R.layout
				.simple_spinner_item,
				transferSourcesList);
		transferSourcesSpinner.setAdapter(transferSourcesAdapter);
		transferSourcesSpinner.setSelection(transferSourcesAdapter.getCount());
		transferDestinationsSpinner = (Spinner) findViewById(R.id.spinner_transferDestination);
		transferDestinationsAdapter = new HintAdapter(getContext(), android.R
				.layout.simple_spinner_item,
				transferDestinationsList);
		transferDestinationsSpinner.setAdapter(transferDestinationsAdapter);
		transferDestinationsSpinner.setSelection(transferDestinationsAdapter.getCount());
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
				R.layout.dropdown_multiline_item, R.id.textView_option, databaseAdapter.getVisibleTransferTemplatesNames());
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
				String transferSourceName = parser.getTransferSourceName();
				String transferDestinationName = parser.getIncomeDestinationName();
				transferSourcesSpinner.setSelection(transferSourcesAdapter.getPosition
						(transferSourceName));
				transferDestinationsSpinner.setSelection(transferDestinationsAdapter.getPosition
						(transferDestinationName));
				rateEditText.setText(String.valueOf(selectedTemplate.getAmount()));
			}
		});
	}

	public void setData(Transaction transaction)
	{
		String transactionType = transaction.getType();
		TransactionTypeParser parser = new TransactionTypeParser(getContext(), transactionType);
		if(!parser.isTransfer())
		{
			Toast.makeText(getContext(), "Transaction is not Transfer", Toast.LENGTH_SHORT).show();
			return;
		}
		
		String transferSourceName;
		if(parser.isTransferSourceWallet())
		{
			transferSourceName = parser.getTransferSourceWallet().getName();
		}
		else
		{
			transferSourceName = parser.getTransferSourceBank().getName();
		}
		transferSourcesSpinner.setSelection(transferSourcesAdapter.getPosition(transferSourceName));
		
		String transferDestinationName;
		if(parser.isTransferDestinationWallet())
		{
			transferDestinationName = parser.getTransferDestinationWallet().getName();
		}
		else
		{
			transferDestinationName = parser.getTransferDestinationBank().getName();
		}
		transferDestinationsSpinner.setSelection(transferDestinationsAdapter.getPosition(transferDestinationName));

		particularsEditText.setText(transaction.getParticular());
		rateEditText.setText(String.valueOf(transaction.getRate()));
		quantityEditText.setText(String.valueOf(transaction.getQuantity()));
		amountEditText.setText(String.valueOf(transaction.getAmount()));
		dateEditText.setText(String.valueOf(transaction.getDate().getDisplayDate("/")));
		excludeInCountersCheckBox.setChecked(!transaction.isIncludeInCounters());
	}
	
	public void setData(String transferSourceName, String transferDestinationName, String
			particulars, String rateText, String quantityText,
						String amountText, String date, boolean addTemplate, boolean excludeInCounters)
	{
		if (transferSourceName != null)
		{
			transferSourcesSpinner.setSelection(transferSourcesAdapter.getPosition(transferSourceName));
		}
		if (transferDestinationName != null)
		{
			transferDestinationsSpinner.setSelection(transferDestinationsAdapter.getPosition(transferDestinationName));
		}
		particularsEditText.setText(particulars);
		rateEditText.setText(rateText);
		quantityEditText.setText(quantityText);
		amountEditText.setText(amountText);
		dateEditText.setText(date);
		addTemplateCheckBox.setSelected(addTemplate);
		excludeInCountersCheckBox.setChecked(excludeInCounters);
	}
	
	public void setData(String transferType, int bankId, double amount)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(getContext());
		
		String bankName = databaseAdapter.getBank(bankId).getName();
		if(transferType.equals(Constants.TRANSFER_PAY_IN))
		{
			transferDestinationsSpinner.setSelection(transferDestinationsAdapter.getPosition(bankName));
		}
		else
		{
			transferSourcesSpinner.setSelection(transferSourcesAdapter.getPosition(bankName));
		}

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

		if((rateString.length()==0) && (amountString.length()==0))
		{
			// Both rate and amount are empty
			Toast.makeText(getContext(), "Please enter Rate or Amount", Toast.LENGTH_LONG).show();
			return null;
		}
		if(transferSourcesSpinner.getSelectedItemPosition() == transferDestinationsSpinner.getSelectedItemPosition())
		{
			Toast.makeText(getContext(), "Please select different Source and Destination for Money Transfer", Toast.LENGTH_LONG)
					.show();
		}
		if (transferSourcesSpinner.getSelectedItem().equals(HintAdapter.HINT_TEXT))
		{
			Toast.makeText(getContext(), "Please select a wallet or bank where the money is " +
					"debited", Toast.LENGTH_LONG).show();
			return null;
		}
		if (transferDestinationsSpinner.getSelectedItem().equals(HintAdapter.HINT_TEXT))
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

		// Transfer Wallet01 Wallet02 or
		// Transfer Wallet01 Bank01 or
		// Transfer Bank01 Wallet01 or
		// Transfer Bank01 Bank02
		String code = "Transfer";
		int transferSourcePosition = transferSourcesSpinner.getSelectedItemPosition();

		if(transferSourcePosition < databaseAdapter.getNumVisibleWallets())
		{
			MoneyStorage transferSource = databaseAdapter.getWalletFromName((String) transferSourcesSpinner.getSelectedItem());
			code += " Wallet" + formatter.format(transferSource.getID());
		}
		else
		{
			MoneyStorage transferSource = databaseAdapter.getBankFromName((String) transferSourcesSpinner.getSelectedItem());
			code += " Bank" + formatter.format(transferSource.getID());
		}

		int transferDestinationPosition = transferDestinationsSpinner.getSelectedItemPosition();
		if(transferDestinationPosition < databaseAdapter.getNumVisibleWallets())
		{
			MoneyStorage transferSource = databaseAdapter.getWalletFromName((String) transferDestinationsSpinner.getSelectedItem());
			code += " Wallet" + formatter.format(transferSource.getID());
		}
		else
		{
			MoneyStorage transferSource = databaseAdapter.getBankFromName((String) transferDestinationsSpinner.getSelectedItem());
			code += " Bank" + formatter.format(transferSource.getID());
		}

		String particulars = particularsEditText.getText().toString().trim();
		Date date = new Date(dateEditText.getText().toString().trim());
		Calendar now = Calendar.getInstance();
		Time createdTime = new Time(now);
		Time modifiedTime = new Time(now);
		boolean excludeInCounters = excludeInCountersCheckBox.isChecked();

		Transaction transaction = new Transaction(id, createdTime, modifiedTime, date, code, particulars, rate, quantity, amount,
				false, !excludeInCounters);
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
	
	public String getTransferSourceName()
	{
		return (String) transferSourcesSpinner.getSelectedItem();
	}
	
	public String getTransferDestinationName()
	{
		return (String) transferDestinationsSpinner.getSelectedItem();
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
