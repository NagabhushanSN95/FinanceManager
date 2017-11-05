// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.chaturvedi.customviews.ExpandCollapseItem;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Date;
import com.chaturvedi.financemanager.functions.Constants;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class StatisticsFilterActivity extends Activity
{
	private Spinner intervalTypeSelector;
	private Spinner monthSelector;
	private Spinner yearSelector;
	private ArrayAdapter<String> intervalTypesAdapter;
	private ArrayAdapter<String> yearAdapter;
	private ArrayAdapter<String> monthAdapter;
	private TextView fromDateTextView;
	private TextView toDateTextView;
	
	private CheckBox creditTitleCheckBox;
	private CheckBox debitTitleCheckBox;
	private CheckBox transferTitleCheckBox;
	private CheckBox walletsTitleCheckBox;
	private CheckBox banksTitleCheckBox;
	
	private ArrayList<CheckBox> debitChildCheckBoxes;
	private ArrayList<CheckBox> walletsChildCheckBoxes;
	private ArrayList<CheckBox> banksChildCheckBoxes;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filter_statistics);
		buildLayout();
		
		// Todo: Restore state
		/*Intent intent = getIntent();
		restoreState(intent);*/
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds childItems to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_filters, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(StatisticsFilterActivity.this);
				return true;
			
			case R.id.action_apply:
				applyFilters();
				return true;
		}
		return true;
	}
	
	@Override
	public void onBackPressed()
	{
		applyFilters();
	}
	
	private void buildLayout()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(StatisticsFilterActivity.this);
		LayoutInflater inflater = LayoutInflater.from(StatisticsFilterActivity.this);
		
		
		//-------------------------Filter by Date--------------------------------------------//
		intervalTypeSelector = (Spinner) findViewById(R.id.spinner_intervalType);
		monthSelector = (Spinner) findViewById(R.id.spinner_monthSelector);
		yearSelector = (Spinner) findViewById(R.id.spinner_yearSelector);
		fromDateTextView = (TextView) findViewById(R.id.textView_fromDate);
		final TextView toDateTextTextView = (TextView) findViewById(R.id.textView_toDateText);
		toDateTextView = (TextView) findViewById(R.id.textView_toDate);

//		Todo: Currently, filtering by month and custom date not supported.
//		String intervalTypes[] = {Constants.VALUE_MONTH, Constants.VALUE_YEAR, Constants.VALUE_ALL, Constants.VALUE_CUSTOM};
		String intervalTypes[] = {Constants.VALUE_YEAR, Constants.VALUE_ALL};
		intervalTypesAdapter = new ArrayAdapter<String>(StatisticsFilterActivity.this, android.R.layout.simple_spinner_item, intervalTypes);
		intervalTypeSelector.setAdapter(intervalTypesAdapter);
		intervalTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				switch (position)
				{
					/*case 0:
						// Month Selector
						monthSelector.setVisibility(View.VISIBLE);
						yearSelector.setVisibility(View.VISIBLE);
						fromDateTextView.setVisibility(View.GONE);
						toDateTextTextView.setVisibility(View.GONE);
						toDateTextView.setVisibility(View.GONE);
						break;*/
					
					case 0:
						// Year Selector
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.VISIBLE);
						fromDateTextView.setVisibility(View.GONE);
						toDateTextTextView.setVisibility(View.GONE);
						toDateTextView.setVisibility(View.GONE);
						break;
					
					case 1:
						// All
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.GONE);
						fromDateTextView.setVisibility(View.GONE);
						toDateTextTextView.setVisibility(View.GONE);
						toDateTextView.setVisibility(View.GONE);
						break;

					/*case 3:
						// Custom Interval Selector
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.GONE);
						fromDateTextView.setVisibility(View.VISIBLE);
						toDateTextTextView.setVisibility(View.VISIBLE);
						toDateTextView.setVisibility(View.VISIBLE);
						break;*/
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				
			}
		});
		intervalTypeSelector.setSelection(1);
		
		String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
				"November", "December"};
		monthAdapter = new ArrayAdapter<String>(StatisticsFilterActivity.this, android.R.layout.simple_spinner_item, months);
		monthSelector.setAdapter(monthAdapter);
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		String[] years = new String[currentYear - 2014];
		for (int i = 2015; i <= currentYear; i++)
		{
			years[i - 2015] = Integer.toString(i);
		}
		yearAdapter = new ArrayAdapter<String>(StatisticsFilterActivity.this,
				android.R.layout.simple_spinner_item, years);
		yearSelector.setAdapter(yearAdapter);
		
		fromDateTextView.setText(new Date(2015, 1, 1).getDisplayDate());
		toDateTextView.setText(new Date(Calendar.getInstance()).getDisplayDate());
		fromDateTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Date fromDate = new Date(fromDateTextView.getText().toString());
				DatePickerDialog fromDatePicker = new DatePickerDialog(StatisticsFilterActivity.this,
						new DatePickerDialog.OnDateSetListener()
						{
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
							{
								fromDateTextView.setText(new Date(year, monthOfYear + 1, dayOfMonth).getDisplayDate());
							}
						}, fromDate.getYear(), fromDate.getMonth() - 1, fromDate.getDate());
				fromDatePicker.show();
			}
		});
		toDateTextView.setOnClickListener(new View.OnClickListener()
		{
			Date toDate = new Date(toDateTextView.getText().toString());
			
			@Override
			public void onClick(View v)
			{
				DatePickerDialog toDatePicker = new DatePickerDialog(StatisticsFilterActivity.this, new DatePickerDialog.OnDateSetListener()
				{
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
					{
						// +1 is added to month because, DatePickerDialog represents Jan by 0, Feb by 1 and so on
						toDateTextView.setText(new Date(year, monthOfYear + 1, dayOfMonth).getDisplayDate());
					}
				}, toDate.getYear(), toDate.getMonth() - 1, toDate.getDate());
				toDatePicker.show();
			}
		});
		
		//-------------------------Filter by Transaction Types-------------------------------//
		// Filters for Credit Transactions
		ExpandCollapseItem creditExpandCollapseItem = (ExpandCollapseItem) findViewById(R.id.creditExpandCollapseItem);
		LinearLayout creditTileLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_title, null);
		creditTitleCheckBox = (CheckBox) creditTileLayout.findViewById(R.id.checkBox);
		creditTitleCheckBox.setText(Constants.KEY_CREDIT_TRANSACTIONS);
		creditExpandCollapseItem.setViews(creditTileLayout, null);
		
		// Filters for Debit Transactions
		ExpandCollapseItem debitExpandCollapseItem = (ExpandCollapseItem) findViewById(R.id.debitExpandCollapseItem);
		LinearLayout debitTitleLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_title, null);
		debitTitleCheckBox = (CheckBox) debitTitleLayout.findViewById(R.id.checkBox);
		debitTitleCheckBox.setText(Constants.KEY_DEBIT_TRANSACTIONS);
		ArrayList<String> expenditureTypesNames = databaseAdapter.getAllVisibleExpenditureTypeNames();
		ArrayList<View> debitChildLayouts = new ArrayList<View>(expenditureTypesNames.size());
		debitChildCheckBoxes = new ArrayList<CheckBox>(expenditureTypesNames.size());
		for (String expenditureTypeName : expenditureTypesNames)
		{
			LinearLayout debitChildLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_child, null);
			CheckBox debitChildCheckBox = (CheckBox) debitChildLayout.findViewById(R.id.checkBox);
			debitChildCheckBox.setText(expenditureTypeName);
			debitChildCheckBoxes.add(debitChildCheckBox);
			debitChildLayouts.add(debitChildLayout);
		}
		debitExpandCollapseItem.setViews(debitTitleLayout, debitChildLayouts);
		
		// Filters for Transfer Transactions
		ExpandCollapseItem transferExpandCollapseItem = (ExpandCollapseItem) findViewById(R.id.transferExpandCollapseItem);
		LinearLayout transferTileLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_title, null);
		transferTitleCheckBox = (CheckBox) transferTileLayout.findViewById(R.id.checkBox);
		transferTitleCheckBox.setText(Constants.KEY_TRANSFER_TRANSACTIONS);
		transferExpandCollapseItem.setViews(transferTileLayout, null);
		
		// Filters for Wallets Transactions
		ExpandCollapseItem walletsExpandCollapseItem = (ExpandCollapseItem) findViewById(R.id.walletsExpandCollapseItem);
		final LinearLayout walletsTileLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_title, null);
		walletsTitleCheckBox = (CheckBox) walletsTileLayout.findViewById(R.id.checkBox);
		walletsTitleCheckBox.setText(Constants.KEY_WALLETS);
		ArrayList<String> walletNames = databaseAdapter.getAllVisibleWalletsNames();
		ArrayList<View> walletChildLayouts = new ArrayList<View>(walletNames.size());
		walletsChildCheckBoxes = new ArrayList<CheckBox>(walletNames.size());
		for (String walletName : walletNames)
		{
			LinearLayout walletChildLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_child, null);
			CheckBox walletChildCheckBox = (CheckBox) walletChildLayout.findViewById(R.id.checkBox);
			walletChildCheckBox.setText(walletName);
			walletsChildCheckBoxes.add(walletChildCheckBox);
			walletChildLayouts.add(walletChildLayout);
		}
		walletsExpandCollapseItem.setViews(walletsTileLayout, walletChildLayouts);
		
		// Filters for Banks Transactions
		ExpandCollapseItem banksExpandCollapseItem = (ExpandCollapseItem) findViewById(R.id.banksExpandCollapseItem);
		final LinearLayout banksTileLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_title, null);
		banksTitleCheckBox = (CheckBox) banksTileLayout.findViewById(R.id.checkBox);
		banksTitleCheckBox.setText(Constants.KEY_BANKS);
		ArrayList<String> bankNames = databaseAdapter.getAllVisibleBanksNames();
		ArrayList<View> bankChildLayouts = new ArrayList<View>(bankNames.size());
		banksChildCheckBoxes = new ArrayList<CheckBox>(bankNames.size());
		for (String bankName : bankNames)
		{
			LinearLayout bankChildLayout = (LinearLayout) inflater.inflate(R.layout.layout_filter_group_child, null);
			CheckBox bankChildCheckBox = (CheckBox) bankChildLayout.findViewById(R.id.checkBox);
			bankChildCheckBox.setText(bankName);
			banksChildCheckBoxes.add(bankChildCheckBox);
			bankChildLayouts.add(bankChildLayout);
		}
		banksExpandCollapseItem.setViews(banksTileLayout, bankChildLayouts);
		
		// Checking or Unchecking a title CheckBox should do the same to all its child CheckBoxes
		// Unchecking a child CheckBox should uncheck the title CheckBox
		// Checking all child CheckBoxes should check the title CheckBox
		setCheckBoxesBehavior(debitTitleCheckBox, debitChildCheckBoxes);
		setCheckBoxesBehavior(walletsTitleCheckBox, walletsChildCheckBoxes);
		setCheckBoxesBehavior(banksTitleCheckBox, banksChildCheckBoxes);
	}
	
	private void setCheckBoxesBehavior(final CheckBox titleCheckBox, final ArrayList<CheckBox> childCheckBoxes)
	{
		// Checking or Unchecking a title CheckBox should do the same to all its child CheckBoxes
		titleCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				for (CheckBox childCheckBox : childCheckBoxes)
				{
					childCheckBox.setChecked(isChecked);
				}
			}
		});
		
		for (CheckBox childCheckBox : childCheckBoxes)
		{
			childCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					if (isChecked)
					{
						// Checking all child CheckBoxes should check the title CheckBox
						boolean allChecked = true;
						for (CheckBox childCheckBox : childCheckBoxes)
						{
							if (!childCheckBox.isChecked())
							{
								allChecked = false;
								break;
							}
						}
						if (allChecked)
						{
							titleCheckBox.setChecked(true);
						}
					}
					else
					{
						// Unchecking a child CheckBox should uncheck the title CheckBox.
						// But this causes all Child CheckBoxes to be unchecked.
						// So, save the states and restore them
						boolean[] checkedStates = new boolean[childCheckBoxes.size()];
						for (int i = 0; i < childCheckBoxes.size(); i++)
						{
							checkedStates[i] = childCheckBoxes.get(i).isChecked();
						}
						titleCheckBox.setChecked(false);
						for (int i = 0; i < childCheckBoxes.size(); i++)
						{
							childCheckBoxes.get(i).setChecked(checkedStates[i]);
						}
					}
				}
			});
		}
	}
	
	private void applyFilters()
	{
		Intent resultIntent = new Intent();
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(StatisticsFilterActivity.this);
		DecimalFormat idFormatter = new DecimalFormat("00");
		
		// Put info on Date Filters
		String intervalType = (String) intervalTypeSelector.getSelectedItem();
		if (intervalType.equals(Constants.VALUE_ALL))
		{
			resultIntent.putExtra(Constants.KEY_INTERVAL_TYPE, Constants.VALUE_ALL);
		}
		else if (intervalType.equals(Constants.VALUE_YEAR))
		{
			resultIntent.putExtra(Constants.KEY_INTERVAL_TYPE, Constants.VALUE_YEAR);
			int year = Integer.parseInt((String) yearSelector.getSelectedItem());
			resultIntent.putExtra(Constants.KEY_INTERVAL_TYPE_YEAR, year + "");
		}
		else if (intervalType.equals(Constants.VALUE_MONTH))
		{
			resultIntent.putExtra(Constants.KEY_INTERVAL_TYPE, Constants.VALUE_MONTH);
			String fullMonth = monthSelector.getSelectedItem() + " - " + yearSelector.getSelectedItem();
			long longMonth = Date.getLongMonth(fullMonth);
			String month = (longMonth / 100) + "/" + idFormatter.format(longMonth % 100) + "/";
			resultIntent.putExtra(Constants.KEY_INTERVAL_TYPE_MONTH, month);
		}
		else if (intervalType.equals(Constants.VALUE_CUSTOM))
		{
			resultIntent.putExtra(Constants.KEY_INTERVAL_TYPE, Constants.VALUE_CUSTOM);
			String startDate = new Date(fromDateTextView.getText().toString()).getSavableDate();
			String endDate = new Date(toDateTextView.getText().toString()).getSavableDate();
			resultIntent.putExtra(Constants.KEY_START_DATE, startDate);
			resultIntent.putExtra(Constants.KEY_END_DATE, endDate);
		}
		
		// Todo: To be done
		/*// Put info on TransactionType Filters
		if (creditTitleCheckBox.isChecked() && debitTitleCheckBox.isChecked() && transferTitleCheckBox.isChecked() &&
				walletsTitleCheckBox.isChecked() && banksTitleCheckBox.isChecked())
		{
			resultIntent.putStringArrayListExtra(Constants.KEY_ALLOWED_TRANSACTION_TYPES, null);
		}
		else
		{
			ArrayList<String> allowedTransactionTypes = new ArrayList<String>();
			if (creditTitleCheckBox.isChecked())
			{
				if (walletsTitleCheckBox.isChecked())
				{
					allowedTransactionTypes.add("Credit Wallet");
				}
				else
				{
					for (CheckBox walletChildCheckBox : walletsChildCheckBoxes)
					{
						if (walletChildCheckBox.isChecked())
						{
							String walletName = walletChildCheckBox.getText().toString();
							int walletId = databaseAdapter.getWalletFromName(walletName).getID();
							allowedTransactionTypes.add("Credit Wallet" + idFormatter.format(walletId));
						}
					}
				}
				if (banksTitleCheckBox.isChecked())
				{
					allowedTransactionTypes.add("Credit Bank");
				}
				else
				{
					for (CheckBox bankChildCheckBox : banksChildCheckBoxes)
					{
						if (bankChildCheckBox.isChecked())
						{
							String bankName = bankChildCheckBox.getText().toString();
							int bankId = databaseAdapter.getBankFromName(bankName).getID();
							allowedTransactionTypes.add("Credit Bank" + idFormatter.format(bankId));
						}
					}
				}
			}

			if (debitTitleCheckBox.isChecked())
			{
				if (walletsTitleCheckBox.isChecked())
				{
					allowedTransactionTypes.add("Debit Wallet");
				}
				else
				{
					for (CheckBox walletChildCheckBox : walletsChildCheckBoxes)
					{
						if (walletChildCheckBox.isChecked())
						{
							String walletName = walletChildCheckBox.getText().toString();
							int walletId = databaseAdapter.getWalletFromName(walletName).getID();
							allowedTransactionTypes.add("Debit Wallet" + idFormatter.format(walletId));
						}
					}
				}
				if (banksTitleCheckBox.isChecked())
				{
					allowedTransactionTypes.add("Debit Bank");
				}
				else
				{
					for (CheckBox bankChildCheckBox : banksChildCheckBoxes)
					{
						if (bankChildCheckBox.isChecked())
						{
							String bankName = bankChildCheckBox.getText().toString();
							int bankId = databaseAdapter.getBankFromName(bankName).getID();
							allowedTransactionTypes.add("Debit Bank" + idFormatter.format(bankId));
						}
					}
				}
			}
			else
			{
				for (CheckBox expenditureTypeCheckBox : debitChildCheckBoxes)
				{
					if(expenditureTypeCheckBox.isChecked())
					{
						String expenditureTypeName = expenditureTypeCheckBox.getText().toString();
						int expTypeId = databaseAdapter.getExpenditureTypeFromName(expenditureTypeName).getId();
						if (walletsTitleCheckBox.isChecked())
						{
							allowedTransactionTypes.add("Debit Wallet%Exp" + idFormatter.format(expTypeId));
						}
						else
						{
							for (CheckBox walletChildCheckBox : walletsChildCheckBoxes)
							{
								if (walletChildCheckBox.isChecked())
								{
									String walletName = walletChildCheckBox.getText().toString();
									int walletId = databaseAdapter.getWalletFromName(walletName).getID();
									allowedTransactionTypes.add("Debit Wallet" + idFormatter.format(walletId) +
											" Exp" + idFormatter.format(expTypeId));
								}
							}
						}
						if (banksTitleCheckBox.isChecked())
						{
							allowedTransactionTypes.add("Debit Bank%Exp" + idFormatter.format(expTypeId));
						}
						else
						{
							for (CheckBox bankChildCheckBox : banksChildCheckBoxes)
							{
								if (bankChildCheckBox.isChecked())
								{
									String bankName = bankChildCheckBox.getText().toString();
									int bankId = databaseAdapter.getBankFromName(bankName).getID();
									allowedTransactionTypes.add("Debit Bank" + idFormatter.format(bankId) +
											" Exp" + idFormatter.format(expTypeId));
								}
							}
						}
					}
				}
			}

			if (transferTitleCheckBox.isChecked())
			{
				if (walletsTitleCheckBox.isChecked())
				{
					allowedTransactionTypes.add("Transfer%Wallet");
				}
				else
				{
					for (CheckBox walletChildCheckBox : walletsChildCheckBoxes)
					{
						if (walletChildCheckBox.isChecked())
						{
							String walletName = walletChildCheckBox.getText().toString();
							int walletId = databaseAdapter.getWalletFromName(walletName).getID();
							allowedTransactionTypes.add("Transfer%Wallet" + idFormatter.format(walletId));
						}
					}
				}
				if (banksTitleCheckBox.isChecked())
				{
					allowedTransactionTypes.add("Transfer%Bank");
				}
				else
				{
					for (CheckBox bankChildCheckBox : banksChildCheckBoxes)
					{
						if (bankChildCheckBox.isChecked())
						{
							String bankName = bankChildCheckBox.getText().toString();
							int bankId = databaseAdapter.getBankFromName(bankName).getID();
							allowedTransactionTypes.add("Transfer%Bank" + idFormatter.format(bankId));
						}
					}
				}
			}

			resultIntent.putStringArrayListExtra(Constants.KEY_ALLOWED_TRANSACTION_TYPES, allowedTransactionTypes);
		}

		// Put info on Search Keyword
		EditText searchEditText = (EditText) findViewById(R.id.editText_search);
		resultIntent.putExtra(Constants.KEY_SEARCH_KEYWORD, searchEditText.getText().toString());*/
		
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}
	
	private void restoreState(Intent intent)
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(StatisticsFilterActivity.this);
		
		// Restore Date Filter
		String intervalType = intent.getStringExtra(Constants.KEY_INTERVAL_TYPE);
		if (intervalType.equals(Constants.VALUE_ALL))
		{
			intervalTypeSelector.setSelection(2);
		}
		else if (intervalType.equals(Constants.VALUE_YEAR))
		{
			String yearString = intent.getStringExtra(Constants.KEY_INTERVAL_TYPE_YEAR);
			String year = yearString.split("/")[0];
			int yearPosition = yearAdapter.getPosition(year);
			intervalTypeSelector.setSelection(1);
			yearSelector.setSelection(yearPosition);
		}
		else if (intervalType.equals(Constants.VALUE_MONTH))
		{
			String yearString = intent.getStringExtra(Constants.KEY_INTERVAL_TYPE_MONTH);
			String year = yearString.split("/")[0];
			String month = yearString.split("/")[1];
			int yearPosition = yearAdapter.getPosition(year);
			int monthPosition = monthAdapter.getPosition(month);
			intervalTypeSelector.setSelection(1);
			yearSelector.setSelection(yearPosition);
			monthSelector.setSelection(monthPosition);
		}
		else if (intervalType.equals(Constants.VALUE_CUSTOM))
		{
			String startDate = intent.getStringExtra(Constants.KEY_START_DATE);
			String endDate = intent.getStringExtra(Constants.KEY_END_DATE);
			startDate = new Date(startDate).getDisplayDate();
			endDate = new Date(endDate).getDisplayDate();
			fromDateTextView.setText(startDate);
			toDateTextView.setText(endDate);
		}
		
		// Restore TransactionTypes Filter
		ArrayList<String> allowedTransactionTypes = intent.getStringArrayListExtra(Constants.KEY_ALLOWED_TRANSACTION_TYPES);
		if (allowedTransactionTypes == null)
		{
			creditTitleCheckBox.setChecked(true);
			debitTitleCheckBox.setChecked(true);
			transferTitleCheckBox.setChecked(true);
			walletsTitleCheckBox.setChecked(true);
			banksTitleCheckBox.setChecked(true);
		}
		else
		{
			for (String type : allowedTransactionTypes)
			{
				if (type.contains(Constants.VALUE_CREDIT))
				{
					creditTitleCheckBox.setChecked(true);
				}
				else if (type.contains(Constants.VALUE_DEBIT))
				{
					if (type.contains("Exp"))
					{
						int startPosition = type.indexOf("Exp") + 3;
						int expTypeID = Integer.parseInt(type.substring(startPosition, startPosition + 2));
						String expenditureTypeName = databaseAdapter.getExpenditureType(expTypeID).getName();
//						TransactionTypeParser parser = new TransactionTypeParser(TransactionsFilterActivity.this, type);
//						String expenditureTypeName = parser.getExpenditureType().getName();
						for (CheckBox debitChildCheckBox : debitChildCheckBoxes)
						{
							if (debitChildCheckBox.getText().toString().equals(expenditureTypeName))
							{
								debitChildCheckBox.setChecked(true);
							}
						}
					}
					else
					{
						debitTitleCheckBox.setChecked(true);
					}
				}
				else if (type.contains(Constants.VALUE_TRANSFER))
				{
					transferTitleCheckBox.setChecked(true);
				}
				
				if (type.contains(Constants.VALUE_WALLET))
				{
					int startPosition = type.indexOf(Constants.VALUE_WALLET) + 6;
					if (type.length() > startPosition + 1 && Character.isDigit(type.charAt(startPosition)))
					{
						int walletID = Integer.parseInt(type.substring(startPosition, startPosition + 2));
						String walletName = databaseAdapter.getWallet(walletID).getName();
						for (CheckBox walletChildCheckBox : walletsChildCheckBoxes)
						{
							if (walletChildCheckBox.getText().toString().equals(walletName))
							{
								walletChildCheckBox.setChecked(true);
							}
						}
					}
					else
					{
						walletsTitleCheckBox.setChecked(true);
					}
				}
				else if (type.contains(Constants.VALUE_BANK))
				{
					int startPosition = type.indexOf(Constants.VALUE_BANK) + 4;
					if (type.length() > startPosition + 1 && Character.isDigit(type.charAt(startPosition)))
					{
						int bankID = Integer.parseInt(type.substring(startPosition, startPosition + 2));
						String bankName = databaseAdapter.getBank(bankID).getName();
						for (CheckBox bankChildCheckBox : banksChildCheckBoxes)
						{
							if (bankChildCheckBox.getText().toString().equals(bankName))
							{
								bankChildCheckBox.setChecked(true);
							}
						}
					}
					else
					{
						banksTitleCheckBox.setChecked(true);
					}
				}
			}
		}
	}
}
