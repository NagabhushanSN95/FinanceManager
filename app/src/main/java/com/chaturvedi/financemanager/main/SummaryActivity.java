// Author: Nagabhushan S N

package com.chaturvedi.financemanager.main;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Bank;
import com.chaturvedi.financemanager.datastructures.Wallet;
import com.chaturvedi.financemanager.edit.EditActivity;
import com.chaturvedi.financemanager.extras.BackupManager;
import com.chaturvedi.financemanager.extras.ExtrasActivity;
import com.chaturvedi.financemanager.functions.AutomaticBackupAndRestoreManager;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.help.HelpActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class SummaryActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
	private static final String KEY_AUTOMATIC_BACKUP_RESTORE = "AutomaticBackupAndRestore";
	private static final int DOUBLE_BACK_PRESS_INTERVAL = 2000;
	private static LinearLayout parentLayout;
	private static LayoutParams parentLayoutParams;
	private static ArrayList<LinearLayout> layouts;
	private static ArrayList<TextView> nameViews;
	private static ArrayList<TextView> amountViews;
	private SharedPreferences preferences;
	private String currencySymbol = " ";
	private String transactionsDisplayInterval = "Month";
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_AMOUNT_VIEWS;
	private int MARGIN_LEFT_NAME_VIEWS;
	private int HEIGHT_TRANSACTION_BUTTONS;
	private Intent transactionsIntent;
	private Intent editIntent;
	private Intent statisticsIntent;
	private Intent settingsIntent;
	private Intent helpIntent;
	private Intent extrasIntent;
	private Intent smsIntent;
	private long lastBackPressedTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_summary);
		
		preferences = this.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		calculateDimensions();
		buildBodyLayout();
		setData();
		doTransactionsActivityOperations();
		
		transactionsIntent=new Intent(this, TransactionsActivity.class);
		editIntent=new Intent(this, EditActivity.class);
		statisticsIntent=new Intent(this, StatisticsActivity.class);
		settingsIntent=new Intent(this, SettingsActivity.class);
		helpIntent = new Intent(this, HelpActivity.class);
		extrasIntent = new Intent(this, ExtrasActivity.class);
		
		// Read SMS Intent. 
		smsIntent = getIntent();
		if(smsIntent.getBooleanExtra(Constants.ACTION_BANK_SMS, false))
		{
			boolean newSmsArrived = preferences.getBoolean(Constants.KEY_BANK_SMS_ARRIVED, false);
			if(newSmsArrived)
			{
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean(Constants.KEY_BANK_SMS_ARRIVED, false);
				editor.apply();
				performSMSTransaction();
			}
		}
	}
	
	@Override
	public void onBackPressed()
	{
		if(lastBackPressedTime + DOUBLE_BACK_PRESS_INTERVAL > System.currentTimeMillis())
		{
			// If set, auto-backup data and close the activity
			SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
			boolean backup = false;
			if(preferences.contains(KEY_AUTOMATIC_BACKUP_RESTORE))
			{
				int value = preferences.getInt(KEY_AUTOMATIC_BACKUP_RESTORE, 3);
				AutomaticBackupAndRestoreManager manager = new AutomaticBackupAndRestoreManager(value);
				backup = manager.isAutomaticBackup();
			}
			if(backup &&
					(ContextCompat.checkSelfPermission(SummaryActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
			{
				final Thread backupThread = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						new BackupManager(SummaryActivity.this).autoBackup();	// Backs-Up Data to Auto Backup Folder
//						backupThread.join();
					}
				});
				backupThread.start();
				//new BackupManager(this).autoBackup();	// Backs-Up Data to Auto Backup Folder
			}

			super.onBackPressed();
		}
		else
		{
			Toast.makeText(SummaryActivity.this, "Press again to exit", Toast.LENGTH_SHORT).show();
			lastBackPressedTime = System.currentTimeMillis();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds childItems to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_summary, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.action_transactions:
				startActivityForResult(transactionsIntent, Constants.REQUEST_CODE_TRANSACTIONS_ACTIVITY);
				return true;
			
			case R.id.action_edit:
				startActivityForResult(editIntent, Constants.REQUEST_CODE_EDIT_ACTIVITY);
				return true;
			
			case R.id.action_statistics:
				startActivity(statisticsIntent);
				return true;
			
			case R.id.action_settings:
				startActivityForResult(settingsIntent, Constants.REQUEST_CODE_SETTINGS_ACTIVITY);
				return true;
			
			case R.id.action_help:
				startHelpActivity();
				return true;
			
			case R.id.action_extras:
				startActivityForResult(extrasIntent, Constants.REQUEST_CODE_EXTRAS_ACTIVITY);
				return true;
		}
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case Constants.REQUEST_CODE_TRANSACTIONS_ACTIVITY:
			case Constants.REQUEST_CODE_SETTINGS_ACTIVITY:
			case Constants.REQUEST_CODE_ADD_TRANSACTION:
				setData();
				break;

			case Constants.REQUEST_CODE_EDIT_ACTIVITY:
			case Constants.REQUEST_CODE_EXTRAS_ACTIVITY:
				buildBodyLayout();
				setData();
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * Calculate the values of various Dimension Fields
	 */
	private void calculateDimensions()
	{
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT = screenHeight * 5 / 100;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*5/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*5/100;
		WIDTH_NAME_VIEWS=screenWidth*55/100;
		WIDTH_AMOUNT_VIEWS = screenWidth*35/100;
		MARGIN_LEFT_NAME_VIEWS = 5;
		HEIGHT_TRANSACTION_BUTTONS = screenHeight/10;
	}
	
	private void buildBodyLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}

		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(SummaryActivity.this);
		
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(KEY_CURRENCY_SYMBOL, " ");
		}

		int numWallets = databaseAdapter.getNumVisibleWallets();
		int numBanks = databaseAdapter.getNumVisibleBanks();
		
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT, 0);
		parentLayout.setLayoutParams(parentLayoutParams);
		parentLayout.removeAllViews();
		
		View line = new View(this);
		LayoutParams lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		line.setLayoutParams(lineParams);
		line.setBackgroundColor(Color.parseColor("#FFFFFF"));
		
		layouts=new ArrayList<LinearLayout>(numWallets+numBanks+2);
		nameViews=new ArrayList<TextView>(numWallets+numBanks+2);
		amountViews=new ArrayList<TextView>(numWallets+numBanks+2);
		for(int i=0; i<numWallets+numBanks+2; i++)
		{
			LayoutInflater layoutInflater = LayoutInflater.from(this);
			LinearLayout summaryLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_summary, null);
			if(i%2==0)
				summaryLayout.setBackgroundColor(Color.parseColor("#88CC00CC"));
			else
				summaryLayout.setBackgroundColor(Color.parseColor("#880044FF"));
			
			TextView nameView = (TextView)summaryLayout.findViewById(R.id.name);
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams.WRAP_CONTENT);
			nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			nameView.setLayoutParams(nameViewParams);
			
			TextView currencySymbolView = (TextView)summaryLayout.findViewById(R.id.currencySymbol);
			currencySymbolView.setText(currencySymbol);
			
			TextView amountView = (TextView)summaryLayout.findViewById(R.id.amount);
			LayoutParams amountViewParams = new LayoutParams(WIDTH_AMOUNT_VIEWS, LayoutParams.WRAP_CONTENT);
			amountView.setLayoutParams(amountViewParams);
			
			layouts.add(summaryLayout);
			nameViews.add(nameView);
			amountViews.add(amountView);
			parentLayout.addView(summaryLayout);
		}
		
		line = new View(this);
		lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		line.setLayoutParams(lineParams);
		line.setBackgroundColor(Color.parseColor("#FFFFFF"));
		parentLayout.addView(line);


		TransactionButtonsLayout buttonsLayout = (TransactionButtonsLayout) findViewById(R.id.button_layout);
		RelativeLayout.LayoutParams buttonsLayoutParams = (RelativeLayout.LayoutParams) buttonsLayout.getLayoutParams();
		buttonsLayoutParams.height = HEIGHT_TRANSACTION_BUTTONS;
	}
	
	private void setData()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(SummaryActivity.this);
		int numWallets = databaseAdapter.getNumVisibleWallets();
		int numBanks = databaseAdapter.getNumVisibleBanks();
		ArrayList<Wallet> wallets = databaseAdapter.getAllVisibleWallets();
		ArrayList<Bank> banks = databaseAdapter.getAllVisibleBanks();
		DecimalFormat formatter = new DecimalFormat("###,##0.##");
		try
		{
			// Set The Data
			for(int i=0; i<numWallets; i++)
			{
				nameViews.get(i).setText(wallets.get(i).getName());
				amountViews.get(i).setText(formatter.format(wallets.get(i).getBalance()));
			}
			for(int i=numWallets, j=0; j<numBanks; i++, j++)
			{
				nameViews.get(i).setText(banks.get(j).getName());
				amountViews.get(i).setText(formatter.format(banks.get(j).getBalance()));
			}
			nameViews.get(numWallets+numBanks).setText("Amount Spent");
			nameViews.get(numWallets+numBanks+1).setText("Income");
			
			if(preferences.contains(KEY_TRANSACTIONS_DISPLAY_INTERVAL))
			{
				transactionsDisplayInterval=preferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
			}
			
			if(transactionsDisplayInterval.equals("Month"))
			{
				DecimalFormat monthFormatter = new DecimalFormat("00");
				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				String currentMonth = String.valueOf(year) + "/" + monthFormatter.format(month);
				double amountSpent = databaseAdapter.getMonthlyAmountSpent(currentMonth);
				double income = databaseAdapter.getMonthlyIncome(currentMonth);
				amountViews.get(numWallets+numBanks).setText(formatter.format(amountSpent));
				amountViews.get(numWallets+numBanks+1).setText(formatter.format(income));
			}
			else if(transactionsDisplayInterval.equals("Year"))
			{
				Calendar calendar = Calendar.getInstance();
				String year = String.valueOf(calendar.get(Calendar.YEAR));
				amountViews.get(numWallets+numBanks).setText(formatter.format(databaseAdapter.getYearlyAmountSpent(year)));
				amountViews.get(numWallets+numBanks+1).setText(formatter.format(databaseAdapter.getYearlyIncome(year)));
			}
			else
			{
				amountViews.get(numWallets+numBanks).setText(formatter.format(databaseAdapter.getTotalAmountSpent()));
				amountViews.get(numWallets+numBanks+1).setText(formatter.format(databaseAdapter.getTotalIncome()));
			}
		}
		catch(Exception e)
		{
			Log.d("SetData()", e.getMessage(), e.fillInStackTrace());
			Toast.makeText(getApplicationContext(), "Error In SummaryActivity.setData()\n"+e.getMessage(), 
					Toast.LENGTH_LONG).show();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void startHelpActivity()
	{
		int androidVersionNo = android.os.Build.VERSION.SDK_INT;
		if(androidVersionNo < Build.VERSION_CODES.JELLY_BEAN)
		{
			startActivity(helpIntent);
			overridePendingTransition(R.anim.new_activity_enter, R.anim.old_activity_leave);
		}
		else
		{
			Bundle animationBundle = ActivityOptions.makeCustomAnimation(getApplicationContext(), 
					R.anim.new_activity_enter, R.anim.old_activity_leave).toBundle();
			startActivity(helpIntent, animationBundle);
		}
	}

	// TODO: Update AddTransactionsActivity to accomodate this
	/**
	 * Takes the details of the sms from the intent and performs the necessary transaction
	 */
	private void performSMSTransaction()
	{
		Intent addTransactionIntent = new Intent(SummaryActivity.this, AddTransactionActivity.class);
		addTransactionIntent.putExtra(Constants.ACTION, Constants.ACTION_BANK_SMS);
		int bankID = smsIntent.getIntExtra(Constants.KEY_BANK_ID, 1);
		addTransactionIntent.putExtra(Constants.KEY_BANK_ID, bankID);
		String transactionType = smsIntent.getStringExtra(Constants.TRANSACTION_TYPE);
		addTransactionIntent.putExtra(Constants.TRANSACTION_TYPE, transactionType);
		if(transactionType.equals(Constants.TRANSACTION_TRANSFER))
		{
			String transferType = smsIntent.getStringExtra(Constants.TRANSFER_TYPE);
			addTransactionIntent.putExtra(Constants.TRANSFER_TYPE, transferType);
		}
		double amount = smsIntent.getDoubleExtra(Constants.KEY_AMOUNT, 0);
		addTransactionIntent.putExtra(Constants.KEY_AMOUNT, amount);
		startActivityForResult(addTransactionIntent, Constants.REQUEST_CODE_ADD_TRANSACTION);
	}
	
	// Copied from Transactions Activity
	// Add setData after buildLayout below
	
	private void doTransactionsActivityOperations()
	{
		//buildButtonPanel();
	}
	
	/**
	 * Set the LayoutParams, OnClickListeners to the buttons in ButtonPanel
	 */
	/*private void buildButtonPanel()
	{
		walletCreditButton=(ImageButton)findViewById(R.id.button_wallet_credit);
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
		
		walletDebitButton=(ImageButton)findViewById(R.id.button_wallet_debit);
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
		
		bankCreditButton=(ImageButton)findViewById(R.id.button_bank_credit);
		LayoutParams bankCreditButtonParams = (LayoutParams) bankCreditButton.getLayoutParams();
		bankCreditButtonParams.width = WIDTH_TRANSACTION_BUTTON;
		bankCreditButton.setLayoutParams(bankCreditButtonParams);
		bankCreditButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// If user has not yet added any banks, display the same
				if(DatabaseManager.getNumBanks() == 0)
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
		
		bankDebitButton=(ImageButton)findViewById(R.id.button_bank_debit);
		LayoutParams bankDebitButtonParams = (LayoutParams) bankDebitButton.getLayoutParams();
		bankDebitButtonParams.width = WIDTH_TRANSACTION_BUTTON;
		bankDebitButton.setLayoutParams(bankDebitButtonParams);
		bankDebitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// If user has not yet added any banks, display the same
				if(DatabaseManager.getNumBanks() == 0)
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
		walletCreditDialog=new AlertDialog.Builder(this);
		walletCreditDialog.setTitle("Add An Income");
		walletCreditDialog.setMessage("Enter Details");
		walletCreditDialogLayout=LayoutInflater.from(this);
		walletCreditDialogView=walletCreditDialogLayout.inflate(R.layout.dialog_wallet_credit, null);
		walletCreditDialog.setView(walletCreditDialogView);
		walletCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String id = ""+DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				String type = "Wallet Credit";
				String particulars = particularsField.getText().toString().trim();
				String amount = amountField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
				Transaction transaction = null;

				boolean validData = isValidData(data);
				if(validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if(saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getAmountText());
						DatabaseManager.addTemplate(template);
					}
				}
				else
				{
					buildWalletCreditDialog();
					particularsField.setText(particulars);
					amountField.setText(amount);
					dateField.setText(date);
					walletCreditDialog.show();
				}
				buildBodyLayout();
				setData();
			}
		});
		walletCreditDialog.setNegativeButton("Cancel", null);
		dateField=(EditText)walletCreditDialogView.findViewById(R.id.field_date);
		particularsField=(MyAutoCompleteTextView)walletCreditDialogView.findViewById(R.id.field_particulars);
		amountField=(EditText)walletCreditDialogView.findViewById(R.id.field_amount);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)walletCreditDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Wallet Credit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);	// Set dropdown width to match_parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo=0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for(int i=0; i<templateStrings.size(); i++)
				{
					if(particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i)))
					{
						selectedTemplateNo=i;
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
		walletDebitDialog=new AlertDialog.Builder(this);
		walletDebitDialog.setTitle("Add Expenditure");
		walletDebitDialog.setMessage("Enter Details");
		walletDebitDialogLayout=LayoutInflater.from(this);
		walletDebitDialogView=walletDebitDialogLayout.inflate(R.layout.dialog_wallet_debit, null);
		walletDebitDialog.setView(walletDebitDialogView);
		walletDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String id = ""+DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String particulars = particularsField.getText().toString();
				int expTypeNo = typesList.getSelectedItemPosition();
				DecimalFormat formatter = new DecimalFormat("00");
				String type = "Wallet Debit Exp"+formatter.format(expTypeNo);
				String rate = rateField.getText().toString();
				String quantity = quantityField.getText().toString();
				String amount = amountField.getText().toString();
				String date = dateField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, rate, quantity, amount};
				Transaction transaction;
				boolean validData = isValidData(data);

				if(validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if(saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getRateText());
						DatabaseManager.addTemplate(template);
					}
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
				buildBodyLayout();
				setData();
			}
		});
		walletDebitDialog.setNegativeButton("Cancel", null);

		dateField=(EditText)walletDebitDialogView.findViewById(R.id.field_date);
		typesList = (Spinner)walletDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		particularsField =(MyAutoCompleteTextView)walletDebitDialogView.findViewById(R.id.field_particulars);
		rateField = (EditText)walletDebitDialogView.findViewById(R.id.field_rate);
		quantityField = (EditText)walletDebitDialogView.findViewById(R.id.field_quantity);
		amountField=(EditText)walletDebitDialogView.findViewById(R.id.field_amount);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)walletDebitDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Wallet Debit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);		// Set dropdown width to Match Parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo=0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for(int i=0; i<templateStrings.size(); i++)
				{
					if(particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i).trim()))
					{
						selectedTemplateNo=i;
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
		bankCreditDialogLayout=LayoutInflater.from(this);
		bankCreditDialogView=bankCreditDialogLayout.inflate(R.layout.dialog_bank_credit, null);

		RadioGroup banksRadioGroup=(RadioGroup)bankCreditDialogView.findViewById(R.id.radioGroup_banks);
		banks=new ArrayList<RadioButton>();
		for(int i=0; i<DatabaseManager.getNumBanks(); i++)
		{
			banks.add(new RadioButton(this));
			banks.get(i).setText(DatabaseManager.getBank(i).getName());
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(Color.BLUE);
			banksRadioGroup.addView(banks.get(i));
		}
		banks.get(0).setChecked(true);

		particularsField = (MyAutoCompleteTextView)bankCreditDialogView.findViewById(R.id.field_particulars);
		creditTypesList = (Spinner)bankCreditDialogView.findViewById(R.id.list_creditTypes);
		creditTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, creditTypes));
		amountField=(EditText)bankCreditDialogView.findViewById(R.id.field_amount);
		dateField=(EditText)bankCreditDialogView.findViewById(R.id.field_date);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)bankCreditDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Bank Credit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);		// set dropdown width to match_parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo=0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for(int i=0; i<templateStrings.size(); i++)
				{
					if(particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i).trim()))
					{
						selectedTemplateNo=i;
						break;
					}
				}
				Template selectedTemplate = templates.get(selectedTemplateNo);
				int bankNo = Integer.parseInt(selectedTemplate.getType().substring(12, 14));    // Bank Credit 01 Income
				banks.get(bankNo).setChecked(true);
				if(selectedTemplate.getType().contains("Income"))   									// Bank Credit 01 Income
				{
					creditTypesList.setSelection(0);
				}
				else if(selectedTemplate.getType().contains("Savings"))  							// Bank Credit 01 Savings
				{
					creditTypesList.setSelection(1);
				}
				amountField.setText("" + selectedTemplate.getAmountText());
			}
		});

		bankCreditDialog=new AlertDialog.Builder(this);
		bankCreditDialog.setTitle("Add Bank Credit");
		bankCreditDialog.setMessage("Enter Details");
		bankCreditDialog.setView(bankCreditDialogView);
		bankCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Determine Which Bank Is Selected
				int bankNo=0;
				for(int i=0; i<DatabaseManager.getNumBanks(); i++)
				{
					if(banks.get(i).isChecked())
						bankNo=i;
				}

				// Read Data
				String id = "" + DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				DecimalFormat bankNoFormatter = new DecimalFormat("00");
				String type = "Bank Credit " + bankNoFormatter.format(bankNo);
				int creditTypesNo = creditTypesList.getSelectedItemPosition();
				if(creditTypesNo==0)
				{
					type += " Income";
				}
				else if(creditTypesNo==1)
				{
					type += " Savings";
				}
				String particulars = particularsField.getText().toString();
				String amount = amountField.getText().toString();
				boolean saveAsTemplate = templateCheckBox.isChecked();
				Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
				Transaction transaction = null;
				boolean validData = isValidData(data);

				if(validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if(saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getAmountText());
						DatabaseManager.addTemplate(template);
					}
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
				buildBodyLayout();
				setData();
			}
		});
		bankCreditDialog.setNegativeButton("Cancel", null);
	}*/

	/*private void buildBankDebitDialog()
	{
		bankDebitDialogLayout=LayoutInflater.from(this);
		bankDebitDialogView=bankDebitDialogLayout.inflate(R.layout.dialog_bank_debit, null);

		RadioGroup banksRadioGroup=(RadioGroup)bankDebitDialogView.findViewById(R.id.radioGroup_banks);
		banks=new ArrayList<RadioButton>();
		for(int i=0; i<DatabaseManager.getNumBanks(); i++)
		{
			banks.add(new RadioButton(this));
			banks.get(i).setText(DatabaseManager.getBank(i).getName());
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(Color.BLUE);
			banksRadioGroup.addView(banks.get(i));
		}
		banks.get(0).setChecked(true);

		particularsField = (MyAutoCompleteTextView)bankDebitDialogView.findViewById(R.id.field_particulars);
		debitTypesList = (Spinner)bankDebitDialogView.findViewById(R.id.list_debitTypes);
		debitTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, debitTypes));
		typesList = (Spinner)bankDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		typesList.setVisibility(View.GONE);
		amountField=(EditText)bankDebitDialogView.findViewById(R.id.field_amount);
		dateField=(EditText)bankDebitDialogView.findViewById(R.id.field_date);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)bankDebitDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this,
				R.layout.dropdown_multiline_item, R.id.textView_option, getTemplateStrings("Bank Debit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
		particularsField.setDropDownWidth(-1);		// Set dropdown width to match_parent
		particularsField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int selectedTemplateNo=0;
				ArrayList<String> templateStrings = getTemplateStrings("All");
				for(int i=0; i<templateStrings.size(); i++)
				{
					if(particularsField.getText().toString().trim().equalsIgnoreCase(templateStrings.get(i).trim()))
					{
						selectedTemplateNo=i;
						break;
					}
				}
				Template selectedTemplate = templates.get(selectedTemplateNo);
				int bankNo = Integer.parseInt(selectedTemplate.getType().substring(11, 13));    // Bank Debit 01 Exp05
				banks.get(bankNo).setChecked(true);
				if(selectedTemplate.getType().contains("Withdraw"))			   					// Bank Debit 01 Withdraw
				{
					debitTypesList.setSelection(0);
				}
				else if(selectedTemplate.getType().contains("Exp"))				  				// Bank Debit 01 Exp01
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
				if(itemNo==0)
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

		bankDebitDialog=new AlertDialog.Builder(this);
		bankDebitDialog.setTitle("Add Bank Debit");
		bankDebitDialog.setMessage("Enter Details");
		bankDebitDialog.setView(bankDebitDialogView);
		bankDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// Determine Which Bank Is Selected
				int bankNo=0;
				for(int i=0; i<DatabaseManager.getNumBanks(); i++)
				{
					if(banks.get(i).isChecked())
						bankNo=i;
				}

				// Validate Data
				String id = ""+DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				DecimalFormat formatter = new DecimalFormat("00");
				String type = "Bank Debit " + formatter.format(bankNo);
				int debitTypesNo = debitTypesList.getSelectedItemPosition();
				if(debitTypesNo == 0)
				{
					type += " Withdraw";
				}
				else if(debitTypesNo == 1)
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

				if(validData)
				{
					transaction = completeData(data);
					DatabaseManager.addTransaction(transaction);
					if(saveAsTemplate)
					{
						Template template = new Template(0, transaction.getParticular(), transaction.getType(),
								transaction.getAmountText());
						DatabaseManager.addTemplate(template);
					}
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

				buildBodyLayout();
				setData();
			}
		});
		bankDebitDialog.setNegativeButton("Cancel", null);
	}*/
	
	// Do this in TransactionsActivity only
	/**
	 * Checks if the data provided by the user is valid
	 * @param data An array of String holding all data
	 * @param data[0] credit/debit
	 * @param data[1] particulars
	 * @param data[2] type
	 * @param data[3] rate
	 * @param data[4] quantity
	 * @param data[5] amount
	 * @param data[6] date
	 * @return true if data is valid, else false
	 */
	/*private boolean isValidData(Object[] data)
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
		if(type.contains("Wallet Credit"))
		{
			if(particulars.length()==0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if(amount.length()==0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if(!Date.isValidDate(date))
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
		else if(type.contains("Wallet Debit"))
		{
			if(particulars.length()==0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if(!Date.isValidDate(date))
			{
				Toast.makeText(getApplicationContext(), "Please Enter A Valid Date", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if(amount.length()==0)
			{
				if(rate.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Rate And Amount", Toast.LENGTH_LONG).show();
					validData = false;
				}
				else if(quantity.length()==0)
				{
					validData = true;
				}
				else
				{
					validData = true;
				}
			}
			else if(rate.length()==0)
			{
				validData = true;
			}
			else if(quantity.length()==0)
			{
				validData = true;
			}
			else
			{
				validData = true;
			}
		}
		else if(type.contains("Bank Credit"))
		{
			if(amount.length()==0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if(!Date.isValidDate(date))
			{
				Toast.makeText(getApplicationContext(), "Please Enter A Valid Date", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else
			{
				validData = true;
			}
		}
		else if(type.contains("Bank Debit"))
		{
			if(amount.length()==0)
			{
				Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
				validData = false;
			}
			else if(!Date.isValidDate(date))
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
		
		if(type.contains("Wallet Credit"))
		{
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars, 
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		else if(type.contains("Wallet Debit"))
		{
			int expTypeNo = Integer.parseInt(type.substring(16, 18));   // Wallet Debit Exp01
			if(particulars.length()==0)
			{
				particulars = DatabaseManager.getAllExpenditureTypes().get(expTypeNo);
			}
			if(amount.length()==0)
			{
				if(quantity.length()==0)
				{
					amount = rate;
					quantity = String.valueOf(1);
				}
				else
				{
					amount = ""+Double.parseDouble(rate)*Double.parseDouble(quantity);
				}
			}
			else if(rate.length()==0)
			{
				if(quantity.length()==0)
				{
					rate = amount;
					quantity = String.valueOf(1);
				}
				else
				{
					rate = ""+Double.parseDouble(amount)/Double.parseDouble(quantity);
				}
			}
			else if(quantity.length()==0)
			{
				quantity = "" + Double.parseDouble(amount)/Double.parseDouble(rate);
			}
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars, 
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		else if(type.contains("Bank Credit"))
		{
			int bankNo = Integer.parseInt(type.substring(12, 14));    // Bank Credit 01 Income
			int creditTypesNo = 0;
			if(type.contains("Income"))
			{
				creditTypesNo = 0;
			}
			else if(type.contains("Savings"))
			{
				creditTypesNo = 1;
			}
			particulars = DatabaseManager.getBank(bankNo).getName() + " Credit: " + creditTypes[creditTypesNo] + ": " + particulars;
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars, 
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		else if(type.contains("Bank Debit"))
		{
			int bankNo = Integer.parseInt(type.substring(11, 13));  // Bank Debit 01 Withdraw
			int debitTypesNo = 0;
			if(type.contains("Withdraw"))
			{
				debitTypesNo = 0;
			}
			else if(type.contains("Exp"))
			{
				debitTypesNo = 1;
			}
			particulars = DatabaseManager.getBank(bankNo).getName() + " Withdrawal: "+ debitTypes[debitTypesNo] + ": " + particulars;
			transaction = new Transaction(id, createdTime, modifiedTime, new Date(date), type, particulars, 
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		
		return transaction;	
	}*/
	
	/*private ArrayList<String> getTemplateStrings(String type)
	{
		templates = DatabaseManager.getAllTemplates();
		ArrayList<String> templateStrings = new ArrayList<String>();
		if(type.equalsIgnoreCase("All"))
		{
			for(int i=0; i<templates.size(); i++)
			{
				templateStrings.add(templates.get(i).getParticular().trim());
			}
		}
		else
		{
			for(int i=0; i<templates.size(); i++)
			{
				if(templates.get(i).getType().contains(type))
				{
					templateStrings.add(templates.get(i).getParticular().trim());
				}
			}
		}
		
		return templateStrings;
	}*/
}