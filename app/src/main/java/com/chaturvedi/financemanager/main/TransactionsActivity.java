// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.financemanager.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Template;
import com.chaturvedi.financemanager.database.Transaction;
import com.chaturvedi.financemanager.functions.Constants;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class TransactionsActivity extends Activity
{
	private String transactionsDisplayInterval = "Month";
	private boolean filteredState = false;    // true if Filtering and Search is applied. Else false.
	// This is used in onBackPressed() method.
	
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_SLNO;
	private int WIDTH_DATE;
	private int WIDTH_PARTICULARS;
	private int WIDTH_AMOUNT;
	
	private LinearLayout parentLayout;
	
	private ArrayList<Transaction> transactions;
	private ArrayList<Template> templates;
	private int contextMenuTransactionNo;
	private DecimalFormat formatterTextFields;
	private DecimalFormat formatterDisplay;
	private Intent templatesIntent;
	private Thread searchThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);
		if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		
		calculateDimensions();
		readPreferences();
		buildTitleLayout();
		readPreferences();
		buildBodyLayout();
		//buildButtonPanel();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		/*if (DatabaseManager.getNumTransactions() == 0)
		{
			DatabaseManager.setContext(TransactionsActivity.this);
			DatabaseManager.readDatabase();
			if (DatabaseManager.getNumTransactions() != 0)
			{
				refreshBodyLayout();
			}
		}*/

		// When screen orientation is changed, onCreate() is called. It builds titleLayout and bodyLayout.
		// Then, some unknown code is executed which changes the text of the particularsTitleView and the
		// particularView of all TransactionLayouts in the parentLayout.
		// After that, onResume is called. So here, I'm setting back the text of the particularsTitleView and
		// the particularView of all transactionLayouts to their original value
		// Todo: This is a bug. It needs to be fixed by determining which code is causing this problem and removing it.
		//((TextView) findViewById(R.id.particulars)).setText("Particulars");
		//buildBodyLayout();
	}

	@Override
	public void onBackPressed()
	{
		// If Filtering or Search is applied, when back button is pressed, the TransactionsActivity should not close.
		// Instead, filtering or search should be removed and original transactions should be displayed.
		// If the back button is pressed when original transactions are showed, then the activity should be closed
		if (filteredState)
		{
			refreshBodyLayout();
			filteredState = false;
		}
		else
		{
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_transactions, menu);

		// TODO: Quick Search button to be implemented Here
		// TODO: Full Quick to be embedded into filters
		/*
		// Associate searchable configuration with the SearchView
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
			{

				@Override
				public boolean onQueryTextChange(String newText)
				{
					/*if(newText.length() > 1)
					{
						if(searchThread != null)
						{
							searchThread.interrupt();
						}
						startSearchTransaction(newText.trim());
						searchView.requestFocus();
					}* /
					return false;
				}

				@Override
				public boolean onQueryTextSubmit(final String query)
				{
					searchView.clearFocus();
					startSearchTransaction(query.trim());
					return false;
				}
			});
		}
		*/

		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(TransactionsActivity.this);
				return true;

			case R.id.action_templates:
				templatesIntent = new Intent(TransactionsActivity.this, TemplatesActivity.class);
				startActivity(templatesIntent);
				return true;

			case R.id.action_transactionsDisplayOptions:
				Toast.makeText(getApplicationContext(), "Coming Soon!!!", Toast.LENGTH_LONG).show();
//	TODO:			displayFilterOptions();
				return true;

			// TODO: To be put as Quick Search Dialog
			/*
			case R.id.action_search:
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				{
					final InputDialog searchDialog = new InputDialog(this);
					searchDialog.setTitle("Search Transactions");
					searchDialog.setHint("Enter the word to be searched");
					searchDialog.setPositiveButton("Search", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String query = searchDialog.getInput().trim();
							startSearchTransaction(query);
						}
					});
					searchDialog.setNegativeButton("Cancel", null);
					searchDialog.show();
				}
				return true;
			*/
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		switch (requestCode)
		{
			case Constants.REQUEST_CODE_ADD_TRANSACTION:
				if(resultCode == RESULT_OK)
				{
					displayNewTransaction((Transaction)intent.getParcelableExtra(Constants.TRANSACTION));
					getTransactionsToDisplay();
				}
				break;

			case Constants.REQUEST_CODE_EDIT_TRANSACTION:
				if(resultCode == RESULT_OK)
				{
					editDisplayedTransaction(contextMenuTransactionNo, (Transaction) intent.getParcelableExtra(Constants.TRANSACTION));
					getTransactionsToDisplay();
				}
				break;
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuTransactionNo = parentLayout.indexOfChild(view);
		menu.setHeaderTitle("Options For Transaction " + (contextMenuTransactionNo + 1));
		menu.add(Menu.NONE, view.getId(), Menu.NONE, "Edit");
		menu.add(Menu.NONE, view.getId(), Menu.NONE, "Delete");
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if (item.getTitle().equals("Edit"))
		{
			editTransaction(contextMenuTransactionNo);
		}
		else if (item.getTitle().equals("Delete"))
		{
			deleteTransaction(contextMenuTransactionNo);
		}
		else
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Calculate the values of various Dimension Fields
	 */
	private void calculateDimensions()
	{
		displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth = displayMetrics.widthPixels;
		screenHeight = displayMetrics.heightPixels;
		
		if (VERSION.SDK_INT <= 10)
		{
			WIDTH_DATE = 25 * screenWidth / 100 - 6;
		}
		else
		{
			WIDTH_DATE = 25 * screenWidth / 100 - 12;
		}
		WIDTH_SLNO = 10 * screenWidth / 100;
		WIDTH_PARTICULARS = 45 * screenWidth / 100;
		WIDTH_AMOUNT = 20 * screenWidth / 100;
	}
	
	private void readPreferences()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(TransactionsActivity.this);
		DecimalFormat formatter = new DecimalFormat("00");
		SharedPreferences preferences = getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
		
		if (preferences.contains(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL))
		{
			transactionsDisplayInterval = preferences.getString(Constants.KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
		}
		
		if (transactionsDisplayInterval.equals("Month"))
		{
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH) + 1;
			String currentMonth = String.valueOf(year) + "/" + formatter.format(month);
			transactions = databaseAdapter.getMonthlyVisibleTransactions(currentMonth);
		}
		else if (transactionsDisplayInterval.equals("Year"))
		{
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			transactions = databaseAdapter.getYearlyVisibleTransactions(String.valueOf(year));
		}
		else
		{
			transactions = databaseAdapter.getAllVisibleTransactions();
			Log.d("SNB", "CP01: " + transactions.size());
		}
	}
	
	private void getTransactionsToDisplay()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(TransactionsActivity.this);
		DecimalFormat formatter = new DecimalFormat("00");
		if (transactionsDisplayInterval.equals("Month"))
		{
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH) + 1;
			String currentMonth = String.valueOf(year) + "/" + formatter.format(month);
			transactions = databaseAdapter.getMonthlyVisibleTransactions(currentMonth);
		}
		else if (transactionsDisplayInterval.equals("Year"))
		{
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			transactions = databaseAdapter.getYearlyVisibleTransactions(String.valueOf(year));
		}
		else
		{
			transactions = databaseAdapter.getAllVisibleTransactions();
		}
	}
	
	private void buildTitleLayout()
	{
		// To be removed later
		readPreferences();
		
		// If Release Version, Make Krishna TextView Invisible
		if (0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}

		TextView slnoTitleView = (TextView) findViewById(R.id.slno);
		LayoutParams slnoTitleParams = (LayoutParams) slnoTitleView.getLayoutParams();
		slnoTitleParams.width = WIDTH_SLNO;
		slnoTitleView.setLayoutParams(slnoTitleParams);
		
		TextView dateTitleView = (TextView) findViewById(R.id.date);
		LayoutParams dateTitleParams = (LayoutParams) dateTitleView.getLayoutParams();
		dateTitleParams.width = WIDTH_DATE;
		dateTitleView.setLayoutParams(dateTitleParams);
		
		TextView particularsTitleView = (TextView) findViewById(R.id.particulars);
		LayoutParams particularsTitleParams = (LayoutParams) particularsTitleView.getLayoutParams();
		particularsTitleParams.width = WIDTH_PARTICULARS;
		particularsTitleView.setLayoutParams(particularsTitleParams);
		
		TextView amountTitleView = (TextView) findViewById(R.id.amount);
		LayoutParams amountTitleParams = (LayoutParams) amountTitleView.getLayoutParams();
		amountTitleParams.width = WIDTH_AMOUNT;
		amountTitleView.setLayoutParams(amountTitleParams);
	}
	
	private void refreshBodyLayout()
	{
		getTransactionsToDisplay();                        // This will get the transactions
		buildBodyLayout();
	}
	
	private void buildBodyLayout()
	{
		try
		{
			parentLayout = (LinearLayout) findViewById(R.id.layout_parent);
			parentLayout.removeAllViews();
			
			formatterDisplay = new DecimalFormat("#,##0.##");
			formatterTextFields = new DecimalFormat("##0.##");
			for (int i = 0; i < transactions.size(); i++)
			{
				displayNewTransaction(i + 1, transactions.get(i));
			}
			
			// Scroll the ScrollView To Bottom
			int lastViewNo = parentLayout.getChildCount() - 1;
			if (lastViewNo >= 0)
			{
				parentLayout.getChildAt(lastViewNo).requestFocus();
			}
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Error In Building Body Layout\n" + e.getMessage(), Toast.LENGTH_LONG).show();
			Log.d("buildBodyLayout()", e.getMessage(), e.fillInStackTrace());
		}
	}
	
	private void displayNewTransaction(int slNo, Transaction transaction)
	{
		int colour = 0;
		if (transaction.getType().contains("Debit"))
		{
			colour = Color.RED;
		}
		else if (transaction.getType().contains("Credit"))
		{
			colour = Color.parseColor("#00CC00");
		}
		else
		{
			colour = Color.BLUE;
		}
		
		LayoutInflater layoutInflater = LayoutInflater.from(this);
		LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_transactions, null);

		TextView slnoView = (TextView) linearLayout.findViewById(R.id.slno);
		LayoutParams slnoParams = (LayoutParams) slnoView.getLayoutParams();
		slnoParams.width = WIDTH_SLNO;
		slnoView.setLayoutParams(slnoParams);
		slnoView.setText(String.valueOf(slNo));
		
		TextView dateView = (TextView) linearLayout.findViewById(R.id.date);
		LayoutParams dateParams = (LayoutParams) dateView.getLayoutParams();
		dateParams.width = WIDTH_DATE;
		dateView.setLayoutParams(dateParams);
		dateView.setText(transaction.getDate().getDisplayDate());
		
		TextView particularsView = (TextView) linearLayout.findViewById(R.id.particulars);
		LayoutParams particularsParams = (LayoutParams) particularsView.getLayoutParams();
		particularsParams.width = WIDTH_PARTICULARS;
		particularsView.setLayoutParams(particularsParams);
		particularsView.setText(transaction.getDisplayParticular(TransactionsActivity.this));
		particularsView.setTextColor(colour);
		
		TextView amountView = (TextView) linearLayout.findViewById(R.id.amount);
		LayoutParams amountParams = (LayoutParams) amountView.getLayoutParams();
		amountParams.width = WIDTH_AMOUNT;
		amountView.setLayoutParams(amountParams);
		amountView.setText(formatterDisplay.format(transaction.getAmount()));

		linearLayout.setFocusable(true);
		linearLayout.setFocusableInTouchMode(true);
		parentLayout.addView(linearLayout);
		registerForContextMenu(linearLayout);
	}
	
	private void displayNewTransaction(Transaction transaction)
	{
		int slNo = parentLayout.getChildCount() + 1;
		displayNewTransaction(slNo, transaction);
		parentLayout.getChildAt(slNo - 1).requestFocus();
	}
	
	private void deleteTransactionFromLayout(int transactionNo)
	{
		parentLayout.removeViewAt(transactionNo);
		for (int i = transactionNo; i < parentLayout.getChildCount(); i++)
		{
			TextView slnoView = (TextView) parentLayout.getChildAt(i).findViewById(R.id.slno);
			slnoView.setText(String.valueOf(i + 1));
		}
	}
	
	private void editDisplayedTransaction(int transactionNo, Transaction transaction)
	{
		LinearLayout layout = (LinearLayout) parentLayout.getChildAt(transactionNo);
		
		TextView dateView = (TextView) layout.findViewById(R.id.date);
		dateView.setText(transaction.getDate().getDisplayDate());
		
		TextView particularsView = (TextView) layout.findViewById(R.id.particulars);
		particularsView.setText(transaction.getDisplayParticular(TransactionsActivity.this));
		
		TextView amountView = (TextView) layout.findViewById(R.id.amount);
		amountView.setText(formatterDisplay.format(transaction.getAmount()));
	}
	
	/* *
	 * Set the LayoutParams, OnClickListeners to the buttons in ButtonPanel
	 * /
	private void buildButtonPanel()
	{
		walletCreditButton = (ImageButton) findViewById(R.id.button_wallet_credit);
		LayoutParams walletCreditButtonParams = (LayoutParams) walletCreditButton.getLayoutParams();
		walletCreditButtonParams.width = WIDTH_TRANSACTION_BUTTON;
		walletCreditButton.setLayoutParams(walletCreditButtonParams);
		walletCreditButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildWalletCreditDialog();
				walletCreditDialog.show();
			}
		});
		walletCreditButton.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Toast.makeText(getApplicationContext(), "Add An Income To The Wallet", Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		walletDebitButton = (ImageButton) findViewById(R.id.button_wallet_debit);
		LayoutParams walletDebitButtonParams = (LayoutParams) walletDebitButton.getLayoutParams();
		walletDebitButtonParams.width = WIDTH_TRANSACTION_BUTTON;
		walletDebitButton.setLayoutParams(walletDebitButtonParams);
		walletDebitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildWalletDebitDialog();
				walletDebitDialog.show();
			}
		});
		walletDebitButton.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Toast.makeText(getApplicationContext(), "Add An Expenditure", Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		bankCreditButton = (ImageButton) findViewById(R.id.button_bank_credit);
		LayoutParams bankCreditButtonParams = (LayoutParams) bankCreditButton.getLayoutParams();
		bankCreditButtonParams.width = WIDTH_TRANSACTION_BUTTON;
		bankCreditButton.setLayoutParams(bankCreditButtonParams);
		bankCreditButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// If user has not yet added any banks, display the same
				if (DatabaseManager.getNumBanks() == 0)
				{
					Toast.makeText(getApplicationContext(), "Please Add A Bank To Add A Bank Transaction",
							Toast.LENGTH_LONG).show();
				}
				else
				{
					buildBankCreditDialog();
					bankCreditDialog.show();
				}
			}
		});
		bankCreditButton.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Toast.makeText(getApplicationContext(), "Add An Income To A Bank Account", Toast.LENGTH_LONG).show();
				return true;
			}
		});
		
		bankDebitButton = (ImageButton) findViewById(R.id.button_bank_debit);
		LayoutParams bankDebitButtonParams = (LayoutParams) bankDebitButton.getLayoutParams();
		bankDebitButtonParams.width = WIDTH_TRANSACTION_BUTTON;
		bankDebitButton.setLayoutParams(bankDebitButtonParams);
		bankDebitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// If user has not yet added any banks, display the same
				if (DatabaseManager.getNumBanks() == 0)
				{
					Toast.makeText(getApplicationContext(), "Please Add A Bank To Add A Bank Transaction",
							Toast.LENGTH_LONG).show();
				}
				else
				{
					buildBankDebitDialog();
					bankDebitDialog.show();
				}
			}
		});
		bankDebitButton.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Toast.makeText(getApplicationContext(), "Add A Bank Withdrawal", Toast.LENGTH_LONG).show();
				return true;
			}
		});
	}*/
	
	/*private void buildWalletCreditDialog()
	{
		walletCreditDialog = new AlertDialog.Builder(this);
		walletCreditDialog.setTitle("Add An Income");
		walletCreditDialog.setMessage("Enter Details");
		walletCreditDialogLayout = LayoutInflater.from(this);
		walletCreditDialogView = walletCreditDialogLayout.inflate(R.layout.dialog_wallet_credit, null);
		walletCreditDialog.setView(walletCreditDialogView);
		walletCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String id = "" + (DatabaseManager.getNumTransactions() + 1);
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				String type = "Wallet Credit";
				String particulars = particularsField.getText().toString().trim();
				String amount = amountField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
				Transaction transaction = null;
				
				boolean validData = isValidData(data);
				if (validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if (saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getAmountText());
						DatabaseManager.addTemplate(template);
					}
					displayNewTransaction(transaction);
					getTransactionsToDisplay();
				}
				else
				{
					buildWalletCreditDialog();
					particularsField.setText(particulars);
					amountField.setText(amount);
					dateField.setText(date);
					walletCreditDialog.show();
				}
			}
		});
		walletCreditDialog.setNegativeButton("Cancel", null);
		dateField = (EditText) walletCreditDialogView.findViewById(R.id.field_date);
		particularsField = (MyAutoCompleteTextView) walletCreditDialogView.findViewById(R.id.field_particulars);
		amountField = (EditText) walletCreditDialogView.findViewById(R.id.field_amount);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox) walletCreditDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Wallet Credit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);	// To set drop down width to Match Parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo = 0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for (int i = 0; i < templateStrings.size(); i++)
				{
					if (particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i)))
					{
						selectedTemplateNo = i;
						break;
					}
				}
				Template selectedTemplate = templates.get(selectedTemplateNo);
				amountField.setText("" + selectedTemplate.getAmountText());
			}
		});
	}*/
	
	/*private void buildWalletDebitDialog()
	{
		walletDebitDialog = new AlertDialog.Builder(this);
		walletDebitDialog.setTitle("Add Expenditure");
		walletDebitDialog.setMessage("Enter Details");
		walletDebitDialogLayout = LayoutInflater.from(this);
		walletDebitDialogView = walletDebitDialogLayout.inflate(R.layout.dialog_wallet_debit, null);
		walletDebitDialog.setView(walletDebitDialogView);
		walletDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String id = "" + DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String particulars = particularsField.getText().toString();
				int expTypeNo = typesList.getSelectedItemPosition();
				DecimalFormat formatter = new DecimalFormat("00");
				String type = "Wallet Debit Exp" + formatter.format(expTypeNo);
				String rate = rateField.getText().toString();
				String quantity = quantityField.getText().toString();
				String amount = amountField.getText().toString();
				String date = dateField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, rate, quantity, amount};
				Transaction transaction;
				boolean validData = isValidData(data);
				
				if (validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if (saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getRateText());
						DatabaseManager.addTemplate(template);
					}
					displayNewTransaction(transaction);
					getTransactionsToDisplay();
				}
				else
				{
					buildWalletDebitDialog();
					particularsField.setText(particulars);
					typesList.setSelection(expTypeNo);
					rateField.setText(rate);
					quantityField.setText(quantity);
					amountField.setText(amount);
					dateField.setText(date);
					templateCheckBox.setChecked(saveAsTemplate);
					walletDebitDialog.show();
				}
			}
		});
		walletDebitDialog.setNegativeButton("Cancel", null);
		
		dateField = (EditText) walletDebitDialogView.findViewById(R.id.field_date);
		typesList = (Spinner) walletDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		particularsField = (MyAutoCompleteTextView) walletDebitDialogView.findViewById(R.id.field_particulars);
		rateField = (EditText) walletDebitDialogView.findViewById(R.id.field_rate);
		quantityField = (EditText) walletDebitDialogView.findViewById(R.id.field_quantity);
		amountField = (EditText) walletDebitDialogView.findViewById(R.id.field_amount);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox) walletDebitDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Wallet Debit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);		// DropDown Width will match Parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo = 0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for (int i = 0; i < templateStrings.size(); i++)
				{
					if (particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i).trim()))
					{
						selectedTemplateNo = i;
						break;
					}
				}
				Template selectedTemplate = templates.get(selectedTemplateNo);
				// Wallet Debit Exp01
				int expTypeNo = Integer.parseInt(selectedTemplate.getType().substring(16, 18));
				typesList.setSelection(expTypeNo);
				rateField.setText("" + selectedTemplate.getAmountText());
			}
		});
	}*/
	
	/*private void buildBankCreditDialog()
	{
		bankCreditDialogLayout = LayoutInflater.from(this);
		bankCreditDialogView = bankCreditDialogLayout.inflate(R.layout.dialog_bank_credit, null);
		
		RadioGroup banksRadioGroup = (RadioGroup) bankCreditDialogView.findViewById(R.id.radioGroup_banks);
		banks = new ArrayList<RadioButton>();
		for (int i = 0; i < DatabaseManager.getNumBanks(); i++)
		{
			banks.add(new RadioButton(this));
			banks.get(i).setText(DatabaseManager.getBank(i).getName());
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(Color.BLUE);
			banksRadioGroup.addView(banks.get(i));
		}
		banks.get(0).setChecked(true);
		
		particularsField = (MyAutoCompleteTextView) bankCreditDialogView.findViewById(R.id.field_particulars);
		creditTypesList = (Spinner) bankCreditDialogView.findViewById(R.id.list_creditTypes);
		creditTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, creditTypes));
		amountField = (EditText) bankCreditDialogView.findViewById(R.id.field_amount);
		dateField = (EditText) bankCreditDialogView.findViewById(R.id.field_date);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox) bankCreditDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Bank Credit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);	// To set Drop Down Width to Match Parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo = 0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for (int i = 0; i < templateStrings.size(); i++)
				{
					if (particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i).trim()))
					{
						selectedTemplateNo = i;
						break;
					}
				}
				Template selectedTemplate = templates.get(selectedTemplateNo);
				int bankNo = Integer.parseInt(selectedTemplate.getType().substring(12, 14));    // Bank Credit 01 Income
				banks.get(bankNo).setChecked(true);
				if (selectedTemplate.getType().contains("Income"))                                    // Bank Credit 01 Income
				{
					creditTypesList.setSelection(0);
				}
				else if (selectedTemplate.getType().contains("Savings"))                            // Bank Credit 01 Savings
				{
					creditTypesList.setSelection(1);
				}
				amountField.setText("" + selectedTemplate.getAmountText());
			}
		});
		
		bankCreditDialog = new AlertDialog.Builder(this);
		bankCreditDialog.setTitle("Add Bank Credit");
		bankCreditDialog.setMessage("Enter Details");
		bankCreditDialog.setView(bankCreditDialogView);
		bankCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Determine Which Bank Is Selected
				int bankNo = 0;
				for (int i = 0; i < DatabaseManager.getNumBanks(); i++)
				{
					if (banks.get(i).isChecked())
					{
						bankNo = i;
					}
				}
				
				// Read Data
				String id = "" + DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				DecimalFormat bankNoFormatter = new DecimalFormat("00");
				String type = "Bank Credit " + bankNoFormatter.format(bankNo);
				int creditTypesNo = creditTypesList.getSelectedItemPosition();
				if (creditTypesNo == 0)
				{
					type += " Income";
				}
				else if (creditTypesNo == 1)
				{
					type += " Savings";
				}
				String particulars = particularsField.getText().toString();
				String amount = amountField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
				Transaction transaction = null;
				boolean validData = isValidData(data);
				
				if (validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if (saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getAmountText());
						DatabaseManager.addTemplate(template);
					}
					displayNewTransaction(transaction);
					getTransactionsToDisplay();
				}
				else
				{
					buildBankCreditDialog();
					banks.get(bankNo).setChecked(true);
					particularsField.setText(particulars);
					creditTypesList.setSelection(creditTypesNo);
					amountField.setText(amount);
					dateField.setText(date);
					bankCreditDialog.show();
				}
			}
		});
		bankCreditDialog.setNegativeButton("Cancel", null);
	}*/
	
	/*private void buildBankDebitDialog()
	{
		bankDebitDialogLayout = LayoutInflater.from(this);
		bankDebitDialogView = bankDebitDialogLayout.inflate(R.layout.dialog_bank_debit, null);
		
		RadioGroup banksRadioGroup = (RadioGroup) bankDebitDialogView.findViewById(R.id.radioGroup_banks);
		banks = new ArrayList<RadioButton>();
		for (int i = 0; i < DatabaseManager.getNumBanks(); i++)
		{
			banks.add(new RadioButton(this));
			banks.get(i).setText(DatabaseManager.getBank(i).getName());
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(Color.BLUE);
			banksRadioGroup.addView(banks.get(i));
		}
		banks.get(0).setChecked(true);

		particularsField = (MyAutoCompleteTextView) bankDebitDialogView.findViewById(R.id.field_particulars);
		debitTypesList = (Spinner) bankDebitDialogView.findViewById(R.id.list_debitTypes);
		debitTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, debitTypes));
		typesList = (Spinner) bankDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		typesList.setVisibility(View.GONE);
		amountField = (EditText) bankDebitDialogView.findViewById(R.id.field_amount);
		dateField = (EditText) bankDebitDialogView.findViewById(R.id.field_date);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox) bankDebitDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Bank Debit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);	// Set Dropdown width to MatchParent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo = 0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for (int i = 0; i < templateStrings.size(); i++)
				{
					if (particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i).trim()))
					{
						selectedTemplateNo = i;
						break;
					}
				}
				Template selectedTemplate = templates.get(selectedTemplateNo);
				int bankNo = Integer.parseInt(selectedTemplate.getType().substring(11, 13));    // Bank Debit 01 Exp05
				banks.get(bankNo).setChecked(true);
				if (selectedTemplate.getType().contains("Withdraw"))                                // Bank Debit 01 Withdraw
				{
					debitTypesList.setSelection(0);
				}
				else if (selectedTemplate.getType().contains("Exp"))                                // Bank Debit 01 Exp01
				{
					debitTypesList.setSelection(1);
					int expTypeNo = Integer.parseInt(selectedTemplate.getType().substring(17, 19)); // Bank Debit 01 Exp01
					typesList.setSelection(expTypeNo);
				}
				amountField.setText("" + selectedTemplate.getAmountText());
			}
		});
		
		debitTypesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int itemNo, long arg3)
			{
				if (itemNo == 0)
				{
					typesList.setVisibility(View.GONE);
				}
				else
				{
					typesList.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
		
		bankDebitDialog = new AlertDialog.Builder(this);
		bankDebitDialog.setTitle("Add Bank Debit");
		bankDebitDialog.setMessage("Enter Details");
		bankDebitDialog.setView(bankDebitDialogView);
		bankDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Determine Which Bank Is Selected
				int bankNo = 0;
				for (int i = 0; i < DatabaseManager.getNumBanks(); i++)
				{
					if (banks.get(i).isChecked())
					{
						bankNo = i;
					}
				}
				
				// Validate Data
				String id = "" + DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				DecimalFormat formatter = new DecimalFormat("00");
				String type = "Bank Debit " + formatter.format(bankNo);
				int debitTypesNo = debitTypesList.getSelectedItemPosition();
				if (debitTypesNo == 0)
				{
					type += " Withdraw";
				}
				else if (debitTypesNo == 1)
				{
					int expTypeNo = typesList.getSelectedItemPosition();
					type += " Exp" + formatter.format(expTypeNo);
				}
				String particulars = particularsField.getText().toString();
				int expTypeNo = 0;
				String amount = amountField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
				Transaction transaction;
				boolean validData = isValidData(data);
				
				if (validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if (saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getAmountText());
						DatabaseManager.addTemplate(template);
					}
					displayNewTransaction(transaction);
					getTransactionsToDisplay();
				}
				else
				{
					buildBankDebitDialog();
					banks.get(bankNo).setChecked(true);
					particularsField.setText(particulars);
					debitTypesList.setSelection(debitTypesNo);
					typesList.setSelection(expTypeNo);
					amountField.setText(amount);
					dateField.setText(date);
					bankDebitDialog.show();
				}
			}
		});
		bankDebitDialog.setNegativeButton("Cancel", null);
	}*/
	
	/**
	 * Delete the transaction referred by transactionNo
	 *
	 * @param transactionNo Number of the transaction to be deleted
	 */
	private void deleteTransaction(int transactionNo)
	{
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
		deleteDialog.setTitle("Delete Transaction");
		deleteDialog.setMessage("Are You Sure You Want To Delete Transaction No " + (transactionNo + 1) + "?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseManager.deleteTransaction(TransactionsActivity.this, transactions.get(contextMenuTransactionNo));
				deleteTransactionFromLayout(contextMenuTransactionNo);
				getTransactionsToDisplay();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}

	private void editTransaction(int transactionNo)
	{
		Transaction oldTransaction = transactions.get(transactionNo);
		Intent editIntent = new Intent(TransactionsActivity.this, AddTransactionActivity.class);
		editIntent.putExtra(Constants.ACTION, Constants.ACTION_EDIT);
		editIntent.putExtra(Constants.TRANSACTION, oldTransaction);
		startActivityForResult(editIntent, Constants.REQUEST_CODE_EDIT_TRANSACTION);

		/*
		String expType = oldTransaction.getType();
		
		if (expType.contains("Wallet Credit"))
		{
			final double backupAmount = oldTransaction.getAmountText();
			buildWalletCreditDialog();
			walletCreditDialog.setTitle("Edit Transaction: Income");
			particularsField.setText(oldTransaction.getParticular());
			amountField.setText(formatterTextFields.format(backupAmount));
			dateField.setText(oldTransaction.getDate().getDisplayDate());
			walletCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String id = "" + oldTransaction.getID();
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					String type = "Wallet Credit";
					String particulars = particularsField.getText().toString().trim();
					String amount = amountField.getText().toString();
					boolean saveAsTemplate = templateCheckBox.isChecked();
					Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
					Transaction newTransaction = null;
					boolean validData = isValidData(data);
					
					if (validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if (saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(),
									newTransaction.getType(), newTransaction.getAmountText());
							DatabaseManager.addTemplate(template);
						}
						editDisplayedTransaction(transactionNo, newTransaction);
						getTransactionsToDisplay();
					}
					else
					{
						buildWalletCreditDialog();
						particularsField.setText(particulars);
						amountField.setText(amount);
						dateField.setText(date);
						walletCreditDialog.show();
					}
				}
			});
			walletCreditDialog.show();
		}
		else if (expType.contains("Wallet Debit"))
		{
			int oldExpTypeNo = Integer.parseInt(expType.substring(16, 18));   // Wallet Debit Exp01
			buildWalletDebitDialog();
			walletDebitDialog.setTitle("Edit Transaction: Expenditure");
			particularsField.setText(oldTransaction.getParticular());
			typesList.setSelection(oldExpTypeNo);
			rateField.setText(formatterTextFields.format(oldTransaction.getRateText()));
			quantityField.setText(formatterTextFields.format(oldTransaction.getQuantityText()));
			amountField.setText(formatterTextFields.format(oldTransaction.getAmountText()));
			dateField.setText(oldTransaction.getDate().getDisplayDate());
			
			walletDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String id = "" + oldTransaction.getID();
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					DecimalFormat formatter = new DecimalFormat("00");
					int newExpTypeNo = typesList.getSelectedItemPosition();
					String type = "Wallet Debit Exp" + formatter.format(newExpTypeNo);
					String particulars = particularsField.getText().toString();
					String rate = rateField.getText().toString();
					String quantity = quantityField.getText().toString();
					String amount = amountField.getText().toString();
					boolean saveAsTemplate = templateCheckBox.isChecked();
					Object[] data = {id, time, time, date, type, particulars, rate, quantity, amount};
					Transaction newTransaction;
					boolean validData = isValidData(data);
					
					if (validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if (saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(),
									newTransaction.getType(), newTransaction.getRateText());
							DatabaseManager.addTemplate(template);
						}
						editDisplayedTransaction(transactionNo, newTransaction);
						getTransactionsToDisplay();
					}
					else
					{
						buildWalletDebitDialog();
						particularsField.setText(particulars);
						typesList.setSelection(newExpTypeNo);
						rateField.setText(rate);
						quantityField.setText(quantity);
						amountField.setText(amount);
						dateField.setText(date);
						walletDebitDialog.show();
					}
				}
			});
			walletDebitDialog.show();
		}
		else if (expType.contains("Bank Credit"))
		{
			String oldParticulars = oldTransaction.getParticular();
			final double oldAmount = oldTransaction.getAmountText();
			buildBankCreditDialog();
			bankCreditDialog.setTitle("Edit Transaction: Bank Credit");
			String creditType = creditTypes[0];
			if (expType.contains("Income"))
			{
				creditType = creditTypes[0];
				creditTypesList.setSelection(0);
			}
			else if (expType.contains("Savings"))
			{
				creditType = creditTypes[1];
				creditTypesList.setSelection(1);
			}
			final int oldBankNo = Integer.parseInt(expType.substring(12, 14));    // Bank Credit 01 Income
			banks.get(oldBankNo).setChecked(true);
			String oldBankName = DatabaseManager.getBank(oldBankNo).getName();
			int start = oldBankName.length() + 9 + creditType.length() + 2;
			int end = oldParticulars.length();
			String netParticulars = oldParticulars.substring(start, end);
			particularsField.setText(netParticulars);
			amountField.setText(formatterTextFields.format(oldAmount));
			dateField.setText(oldTransaction.getDate().getDisplayDate());
			
			bankCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Determine Which Bank Is Selected
					int newBankNo = 0;
					for (int i = 0; i < DatabaseManager.getNumBanks(); i++)
					{
						if (banks.get(i).isChecked())
						{
							newBankNo = i;
						}
					}
					
					// Validate Data
					String id = "" + oldTransaction.getID();
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					DecimalFormat formatter = new DecimalFormat("00");
					String type = "Bank Credit " + formatter.format(newBankNo);
					int creditTypesNo = creditTypesList.getSelectedItemPosition();
					if (creditTypesNo == 0)
					{
						type += " Income";
					}
					else if (creditTypesNo == 1)
					{
						type += " Savings";
					}
					String particulars = particularsField.getText().toString();
					String amount = amountField.getText().toString();
					boolean saveAsTemplate = templateCheckBox.isChecked();
					Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
					Transaction newTransaction;
					boolean validData = isValidData(data);
					
					if (validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if (saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(),
									newTransaction.getType(), newTransaction.getAmountText());
							DatabaseManager.addTemplate(template);
						}
						editDisplayedTransaction(transactionNo, newTransaction);
						getTransactionsToDisplay();
					}
					else
					{
						buildBankCreditDialog();
						banks.get(newBankNo).setChecked(true);
						particularsField.setText(particulars);
						creditTypesList.setSelection(creditTypesNo);
						amountField.setText(amount);
						dateField.setText(date);
						bankCreditDialog.show();
					}
				}
			});
			bankCreditDialog.show();
		}
		
		if (expType.contains("Bank Debit"))
		{
			int oldBankNo = Integer.parseInt(expType.substring(11, 13));  // Bank Debit 01 Withdraw
			String oldParticulars = oldTransaction.getParticular();
			final double oldAmount = oldTransaction.getAmountText();
			buildBankDebitDialog();
			bankDebitDialog.setTitle("Edit Transaction: Bank Debit");
			String debitType = debitTypes[0];
			int oldExpTypeNo = 0;
			if (expType.contains("Withdraw"))
			{
				debitTypesList.setSelection(0);
				debitType = debitTypes[0];
			}
			else if (expType.contains("Exp"))
			{
				debitTypesList.setSelection(1);
				debitType = debitTypes[1];
				oldExpTypeNo = Integer.parseInt(expType.substring(17, 19)); //// Bank Debit 01 Exp01
				typesList.setSelection(oldExpTypeNo);
			}
			
			debitTypesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int itemNo, long arg3)
				{
					if (itemNo == 0)
					{
						typesList.setVisibility(View.GONE);
					}
					else
					{
						typesList.setVisibility(View.VISIBLE);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{
					
				}
			});
			String oldBankName = DatabaseManager.getBank(oldBankNo).getName();
			int start = oldBankName.length() + 13 + debitType.length() + 2;
			int end = oldParticulars.length();
			String netParticulars = oldParticulars.substring(start, end);
			
			banks.get(oldBankNo).setChecked(true);
			particularsField.setText(netParticulars);
			amountField.setText(formatterTextFields.format(oldAmount));
			dateField.setText(oldTransaction.getDate().getDisplayDate());
			
			bankDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// Determine Which Bank Is Selected
					int newBankNo = 0;
					for (int i = 0; i < DatabaseManager.getNumBanks(); i++)
					{
						if (banks.get(i).isChecked())
						{
							newBankNo = i;
						}
					}
					
					String id = "" + oldTransaction.getID();
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					DecimalFormat formatter = new DecimalFormat("00");
					String type = "Bank Debit " + formatter.format(newBankNo);
					int debitTypesNo = debitTypesList.getSelectedItemPosition();
					int expTypeNo = 0;
					if (debitTypesNo == 0)
					{
						type += " Withdraw";
					}
					else if (debitTypesNo == 1)
					{
						expTypeNo = typesList.getSelectedItemPosition();
						type += " Exp" + formatter.format(expTypeNo);
					}
					String particulars = particularsField.getText().toString();
					String amount = amountField.getText().toString();
					boolean saveAsTemplate = templateCheckBox.isChecked();
					Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
					Transaction newTransaction;
					boolean validData = isValidData(data);
					
					if (validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if (saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(), newTransaction.getType(),
									newTransaction.getAmountText());
							DatabaseManager.addTemplate(template);
						}
						editDisplayedTransaction(transactionNo, newTransaction);
						getTransactionsToDisplay();
					}
					else
					{
						buildBankDebitDialog();
						banks.get(newBankNo).setChecked(true);
						particularsField.setText(particulars);
						debitTypesList.setSelection(debitTypesNo);
						typesList.setSelection(expTypeNo);
						amountField.setText(amount);
						dateField.setText(date);
						bankDebitDialog.show();
					}
				}
			});
			bankDebitDialog.show();

		}*/
	}
	
	/* *
	 * Checks if the data provided by the user is valid
	 *
	 * @param data    An array of String holding all data
	 * @param data[0] credit/debit
	 * @param data[1] particulars
	 * @param data[2] type
	 * @param data[3] rate
	 * @param data[4] quantity
	 * @param data[5] amount
	 * @param data[6] date
	 * @return true if data is valid, else false
	 * /
	private boolean isValidData(Object[] data)
	{
		//int id = Integer.parseInt((String) data[0]);
		//Time createdTime = (Time) data[1];
		//Time modifiedTime = (Time) data[2];
		String date = (String) data[3];
		String type = (String) data[4];
		String particulars = (String) data[5];
		String rate = (String) data[6];
		String quantity = (String) data[7];
		String amount = (String) data[8];
		boolean validData = true;
		
		// Check the data for Wallet Credit
		if (type.contains("Wallet Credit"))
		{
			if (particulars.length() == 0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if (amount.length() == 0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if (!Date.isValidDate(date))
			{
				Toast.makeText(getApplicationContext(), "Please Enter A Valid Date", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else
			{
				validData = true;
			}
		}
		
		// Checks the data for Wallet Debit
		else if (type.contains("Wallet Debit"))
		{
			if (particulars.length() == 0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if (!Date.isValidDate(date))
			{
				Toast.makeText(getApplicationContext(), "Please Enter A Valid Date", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if (amount.length() == 0)
			{
				if (rate.length() == 0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Rate And Amount", Toast.LENGTH_LONG).show();
					validData = false;
				}
				else if (quantity.length() == 0)
				{
					validData = true;
				}
				else
				{
					validData = true;
				}
			}
			else if (rate.length() == 0)
			{
				validData = true;
			}
			else if (quantity.length() == 0)
			{
				validData = true;
			}
			else
			{
				validData = true;
			}
		}
		else if (type.contains("Bank Credit"))
		{
			if (amount.length() == 0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if (!Date.isValidDate(date))
			{
				Toast.makeText(getApplicationContext(), "Please Enter A Valid Date", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else
			{
				validData = true;
			}
		}
		else if (type.contains("Bank Debit"))
		{
			if (amount.length() == 0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if (!Date.isValidDate(date))
			{
				Toast.makeText(getApplicationContext(), "Please Enter A Valid Date", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else
			{
				validData = true;
			}
		}
		else
		{
			Toast.makeText(getApplicationContext(), "An Error Has Ocurred in \nDetailsActivity/isValidData()\nWrong Credit/Debit Type", Toast.LENGTH_LONG).show();
		}
		return validData;
	}*/
	
	/*private Transaction completeData(Object[] data)
	{
		int id = Integer.parseInt((String) data[0]);
		Time createdTime = (Time) data[1];
		Time modifiedTime = (Time) data[2];
		String date = (String) data[3];
		String type = (String) data[4];
		String particulars = (String) data[5];
		String rate = (String) data[6];
		String quantity = (String) data[7];
		String amount = (String) data[8];
		Transaction transaction = null;
		
		// Trim the Strings
		particulars = particulars.trim();
		
		if (type.contains("Wallet Credit"))
		{
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars,
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		else if (type.contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(type.substring(16, 18));   // Wallet Debit Exp01
			if (particulars.length() == 0)
			{
				particulars = DatabaseManager.getAllExpenditureTypes().get(expTypeNo);
			}
			if (amount.length() == 0)
			{
				if (quantity.length() == 0)
				{
					amount = rate;
					quantity = String.valueOf(1);
				}
				else
				{
					amount = "" + Double.parseDouble(rate) * Double.parseDouble(quantity);
				}
			}
			else if (rate.length() == 0)
			{
				if (quantity.length() == 0)
				{
					rate = amount;
					quantity = String.valueOf(1);
				}
				else
				{
					rate = "" + Double.parseDouble(amount) / Double.parseDouble(quantity);
				}
			}
			else if (quantity.length() == 0)
			{
				quantity = "" + Double.parseDouble(amount) / Double.parseDouble(rate);
			}
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars,
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		else if (type.contains("Bank Credit"))
		{
			int bankNo = Integer.parseInt(type.substring(12, 14));    // Bank Credit 01 Income
			int creditTypesNo = 0;
			if (type.contains("Income"))
			{
				creditTypesNo = 0;
			}
			else if (type.contains("Savings"))
			{
				creditTypesNo = 1;
			}
			particulars = DatabaseManager.getBank(bankNo).getName() + " Credit: " + creditTypes[creditTypesNo] + ": " + particulars;
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars,
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		else if (type.contains("Bank Debit"))
		{
			int bankNo = Integer.parseInt(type.substring(11, 13));  // Bank Debit 01 Withdraw
			int debitTypesNo = 0;
			if (type.contains("Withdraw"))
			{
				debitTypesNo = 0;
			}
			else if (type.contains("Exp"))
			{
				debitTypesNo = 1;
			}
			particulars = DatabaseManager.getBank(bankNo).getName() + " Withdrawal: " + debitTypes[debitTypesNo] + ": " + particulars;
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars,
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		
		return transaction;
	}*/
	
	/*private ArrayList<String> getTemplateStrings(String type)
	{
		templates = DatabaseManager.getAllTemplates();
		ArrayList<String> templateStrings = new ArrayList<String>();
		if (type.equalsIgnoreCase("All"))
		{
			for (int i = 0; i < templates.size(); i++)
			{
				templateStrings.add(templates.get(i).getParticular().trim());
			}
		}
		else
		{
			for (int i = 0; i < templates.size(); i++)
			{
				if (templates.get(i).getType().contains(type))
				{
					templateStrings.add(templates.get(i).getParticular().trim());
				}
			}
		}
		
		return templateStrings;
	}*/

	// TODO: Add Filters
	/*private void displayFilterOptions()
	{
		final ArrayList<String> expTypes = DatabaseManager.getAllExpenditureTypes();
		
		AlertDialog.Builder filterDialogBuilder = new AlertDialog.Builder(this);
		filterDialogBuilder.setTitle("Filter Transactions To Display");
		LayoutInflater inflater = LayoutInflater.from(this);
		
		final RelativeLayout filterDialogLayout = (RelativeLayout) inflater.inflate(R.layout.layout_filter_transactions, null);
		final Spinner intervalTypeSelector = (Spinner) filterDialogLayout.findViewById(R.id.spinner_intervalType);
		final LinearLayout monthYearSelectorLayout = (LinearLayout) filterDialogLayout.findViewById(R.id.monthYearSelectorLayout);
		final Spinner monthSelector = (Spinner) filterDialogLayout.findViewById(R.id.spinner_monthSelector);
		final Spinner yearSelector = (Spinner) filterDialogLayout.findViewById(R.id.spinner_yearSelector);
		final RelativeLayout customIntervalSelectorLayout = (RelativeLayout) filterDialogLayout.findViewById(R.id.customIntervalSelectorLayout);
		final TextView fromDateTextView = (TextView) filterDialogLayout.findViewById(R.id.textView_fromDate);
		final TextView toDateTextView = (TextView) filterDialogLayout.findViewById(R.id.textView_toDate);
		Button fromDateButton = (Button) filterDialogLayout.findViewById(R.id.button_fromDate);
		Button toDateButton = (Button) filterDialogLayout.findViewById(R.id.button_toDate);

		String intervalTypes[] = {"Month", "Year", "All", "Custom"};
		intervalTypeSelector.setAdapter(new ArrayAdapter<String>(TransactionsActivity.this, android.R.layout.simple_spinner_item, intervalTypes));
		intervalTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				switch (position)
				{
					case 0:
						monthYearSelectorLayout.setVisibility(View.VISIBLE);
						monthSelector.setVisibility(View.VISIBLE);
						yearSelector.setVisibility(View.VISIBLE);
						customIntervalSelectorLayout.setVisibility(View.GONE);
						break;

					case 1:
						monthYearSelectorLayout.setVisibility(View.VISIBLE);
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.VISIBLE);
						customIntervalSelectorLayout.setVisibility(View.GONE);
						break;

					case 2:
						monthYearSelectorLayout.setVisibility(View.GONE);
						customIntervalSelectorLayout.setVisibility(View.GONE);
						break;

					case 3:
						monthYearSelectorLayout.setVisibility(View.GONE);
						customIntervalSelectorLayout.setVisibility(View.VISIBLE);
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

		String[] months = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
		monthSelector.setAdapter(new ArrayAdapter<String>(TransactionsActivity.this, android.R.layout.simple_spinner_item, months));
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		String[] years = new String[currentYear - 2014];
		for (int i = 2015; i <= currentYear; i++)
		{
			years[i - 2015] = Integer.toString(i);
		}
		yearSelector.setAdapter(new ArrayAdapter<String>(TransactionsActivity.this, android.R.layout.simple_spinner_item, years));

		fromDateTextView.setText(new Date(2015, 1, 1).getDisplayDate());
		toDateTextView.setText(new Date(Calendar.getInstance()).getDisplayDate());
		fromDateButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Date fromDate = new Date(fromDateTextView.getText().toString());
				DatePickerDialog fromDatePicker = new DatePickerDialog(TransactionsActivity.this, new DatePickerDialog.OnDateSetListener()
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
		toDateButton.setOnClickListener(new View.OnClickListener()
		{
			Date toDate = new Date(toDateTextView.getText().toString());

			@Override
			public void onClick(View v)
			{
				DatePickerDialog toDatePicker = new DatePickerDialog(TransactionsActivity.this, new DatePickerDialog.OnDateSetListener()
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

		final LinearLayout expendituresLayout = (LinearLayout) filterDialogLayout.findViewById(R.id.expendituresLayout);
		final LinearLayout bankTransactionsLayout = (LinearLayout) filterDialogLayout.findViewById(R.id.bankTransactionsLayout);
		final ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
		CheckBox incomesCheckBox = (CheckBox) filterDialogLayout.findViewById(R.id.checkBox_incomes);
		checkBoxes.add(incomesCheckBox);
		for (String expType : expTypes)
		{
			CheckBox cb1 = new CheckBox(this);
			cb1.setId(expTypes.indexOf(expType));
			cb1.setText(expType);
			expendituresLayout.addView(cb1);
			checkBoxes.add(cb1);
		}
		final CheckBox bankIncomeCheckBox = new CheckBox(this);
		bankIncomeCheckBox.setText("Incomes (A/C Transfer)");
		bankTransactionsLayout.addView(bankIncomeCheckBox);
		checkBoxes.add(bankIncomeCheckBox);
		final CheckBox bankSavingsCheckBox = new CheckBox(this);
		bankSavingsCheckBox.setText("Bank Savings");
		bankTransactionsLayout.addView(bankSavingsCheckBox);
		checkBoxes.add(bankSavingsCheckBox);
		final CheckBox bankWithdrawCheckBox = new CheckBox(this);
		bankWithdrawCheckBox.setText("Bank Withdrawals");
		bankTransactionsLayout.addView(bankWithdrawCheckBox);
		checkBoxes.add(bankWithdrawCheckBox);
		for (String expType : expTypes)
		{
			CheckBox cb1 = new CheckBox(this);
			cb1.setId(expTypes.indexOf(expType) + 10);
			cb1.setText(expType + " A/C Transfer");
			bankTransactionsLayout.addView(cb1);
			checkBoxes.add(cb1);
		}
		
		CheckBox expendituresCheckBox = (CheckBox) filterDialogLayout.findViewById(R.id.checkBox_expenditures);
		expendituresCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					for (int i = 0; i < expTypes.size(); i++)
					{
						CheckBox cb1 = (CheckBox) expendituresLayout.findViewById(i);
						cb1.setChecked(true);
						CheckBox cb2 = (CheckBox) bankTransactionsLayout.findViewById(i + 10);
						cb2.setChecked(true);
					}
				}
			}
		});
		
		CheckBox allBankTransactionsCheckBox = (CheckBox) filterDialogLayout.findViewById(R.id.checkBox_allBankTransactions);
		allBankTransactionsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					for (int i = 0; i < expTypes.size(); i++)
					{
						bankIncomeCheckBox.setChecked(true);
						bankSavingsCheckBox.setChecked(true);
						bankWithdrawCheckBox.setChecked(true);
						CheckBox cb2 = (CheckBox) bankTransactionsLayout.findViewById(i + 10);
						cb2.setChecked(true);
					}
				}
			}
		});
		
		filterDialogBuilder.setView(filterDialogLayout);
		
		filterDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				filteredState = true;
				String interval1 = null;
				switch (intervalTypeSelector.getSelectedItemPosition())
				{
					case 0:            // Month
						int month = Integer.parseInt(monthSelector.getSelectedItem().toString().trim());
						int year = Integer.parseInt(yearSelector.getSelectedItem().toString().trim());
						interval1 = "month-" + (year * 100 + month);
						break;

					case 1:            // Year
						year = Integer.parseInt(yearSelector.getSelectedItem().toString().trim());
						interval1 = "year-" + year;
						break;

					case 2:
						interval1 = "all";
						break;

					case 3:
						long startDate = new Date(fromDateTextView.getText().toString()).getLongDate();
						long endDate = new Date(toDateTextView.getText().toString()).getLongDate();
						interval1 = "custom-" + startDate + "-" + endDate;
						break;
				}
				final String interval = interval1;

				DecimalFormat formatter = new DecimalFormat("00");
				final ArrayList<String> types = new ArrayList<String>();
				if (checkBoxes.get(0).isChecked())
				{
					types.add("Wallet Credit");
//					Toast.makeText(getApplicationContext(), "Check-Point 04", Toast.LENGTH_SHORT).show();
				}
				for (int i = 0; i < expTypes.size(); i++)
				{
					if (checkBoxes.get(i + 1).isChecked())
					{
						types.add("Wallet Debit Exp" + formatter.format(i));
					}
				}
				if (checkBoxes.get(expTypes.size() + 1).isChecked())
				{
					int numBanks = DatabaseManager.getNumBanks();
					for (int j = 0; j < numBanks; j++)
					{
						types.add("Bank Credit " + formatter.format(j) + " Income");
					}
				}
				if (checkBoxes.get(expTypes.size() + 2).isChecked())
				{
					int numBanks = DatabaseManager.getNumBanks();
					for (int j = 0; j < numBanks; j++)
					{
						types.add("Bank Credit " + formatter.format(j) + " Savings");
					}
				}
				if (checkBoxes.get(expTypes.size() + 3).isChecked())
				{
					int numBanks = DatabaseManager.getNumBanks();
					for (int j = 0; j < numBanks; j++)
					{
						types.add("Bank Debit " + formatter.format(j) + " Withdraw");
					}
				}
				for (int i = 0; i < expTypes.size(); i++)
				{
					if (checkBoxes.get(expTypes.size() + 4 + i).isChecked())
					{
						int numBanks = DatabaseManager.getNumBanks();
						for (int j = 0; j < numBanks; j++)
						{
							types.add("Bank Debit " + formatter.format(j) + " Exp" + formatter.format(i));
						}
					}
				}

				final Handler filterHandler = new Handler()
				{
					@Override
					public void handleMessage(Message transactionMessage)
					{
						switch (transactionMessage.what)
						{
							case DatabaseManager.ACTION_NEW_TRANSACTION_FOUND:
								Transaction transaction = (Transaction) transactionMessage.obj;
								transactions.add(transaction);
								displayNewTransaction(transaction);
								Log.d("SNB","Something Wrong. This shouldn't be executed");
								break;

							case DatabaseManager.ACTION_ALL_TRANSACTIONS_FOUND:
								transactions = (ArrayList<Transaction>)transactionMessage.obj;
								buildBodyLayout();
								break;

							case DatabaseManager.ACTION_TOAST_MESSAGE:
								String toastMessage = (String) transactionMessage.obj;
								Toast.makeText(TransactionsActivity.this, toastMessage, Toast.LENGTH_LONG).show();
								break;
						}
					}
				};

				Thread filterThread = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						transactions = DatabaseManager.getTransactions(interval, types, "Date-Ascending");
						Message message = filterHandler.obtainMessage(DatabaseManager.ACTION_ALL_TRANSACTIONS_FOUND, transactions);
						message.sendToTarget();
					}
				});
				filterThread.start();
			}
		});
		filterDialogBuilder.setNegativeButton("Cancel", null);
		filterDialogBuilder.show();
	}*/

	// TODO: Implement Quick Search
	/*private void startSearchTransaction(final String query)
	{
		filteredState = true;
		parentLayout.removeAllViews();
		transactions = new ArrayList<Transaction>();

		final int MESSAGE_NEW_MATCH_FOUND = 101;
		final int MESSAGE_SEARCH_FINISHED = 102;
		final Handler searchHandler = new Handler()
		{
			@Override
			public void handleMessage(Message transactionMessage)
			{
				switch (transactionMessage.what)
				{
					case MESSAGE_NEW_MATCH_FOUND:
						Transaction transaction = (Transaction) transactionMessage.obj;
						transactions.add(transaction);
						displayNewTransaction(transaction);
						break;

					case MESSAGE_SEARCH_FINISHED:
						Toast.makeText(TransactionsActivity.this, "Search Finished", Toast.LENGTH_LONG).show();
						break;
				}
			}
		};

		searchThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
//				Looper.prepare();

				ArrayList<Transaction> allTransactions = DatabaseManager.getAllTransactions();
				for (int i = allTransactions.size() - 1; i >= 0; i--)
				{
					Transaction transaction = allTransactions.get(i);
					if (transaction.getParticular().toLowerCase().contains(query.toLowerCase()))
					{
						Message message = searchHandler.obtainMessage(MESSAGE_NEW_MATCH_FOUND, transaction);
						message.sendToTarget();
					}

					if (Thread.interrupted())
					{
						return;
					}
				}
				Message message = searchHandler.obtainMessage(MESSAGE_SEARCH_FINISHED);
				message.sendToTarget();
			}
		});
		searchThread.start();
	}*/
}