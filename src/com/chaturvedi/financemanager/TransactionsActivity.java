// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.financemanager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Date;
import com.chaturvedi.financemanager.database.Template;
import com.chaturvedi.financemanager.database.Time;
import com.chaturvedi.financemanager.database.Transaction;

public class TransactionsActivity extends Activity
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private String transactionsDisplayInterval = "Month";
	
	/*private final int DISPLAY_TRANSACTIONS_ALL = 0;
	private final int DISPLAY_TRANSACTIONS_YEAR = 1;
	private final int DISPLAY_TRANSACTIONS_MONTH = 2;
	private int displayTransactions;*/
	
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_SLNO;
	private int WIDTH_DATE;
	private int WIDTH_PARTICULARS;
	private int WIDTH_AMOUNT;
	private int WIDTH_TRANSACTION_BUTTON;
	
	private LinearLayout parentLayout;
	
	private ImageButton walletCreditButton;
	private ImageButton walletDebitButton;
	private ImageButton bankCreditButton;
	private ImageButton bankDebitButton;

	private AlertDialog.Builder walletCreditDialog;
	private AlertDialog.Builder walletDebitDialog;
	private AlertDialog.Builder bankCreditDialog;
	private AlertDialog.Builder bankDebitDialog;
	private LayoutInflater walletCreditDialogLayout;
	private LayoutInflater walletDebitDialogLayout;
	private LayoutInflater bankCreditDialogLayout;
	private LayoutInflater bankDebitDialogLayout;
	private View walletCreditDialogView;
	private View walletDebitDialogView;
	private View bankCreditDialogView;
	private View bankDebitDialogView;
	
	private AutoCompleteTextView particularsField;
	private Spinner typesList;
	private EditText rateField;
	private EditText quantityField;
	private EditText amountField;
	private EditText dateField;
	private CheckBox templateCheckBox;
	private Spinner creditTypesList;
	private Spinner debitTypesList;
	private ArrayList<RadioButton> banks;
	private String[] creditTypes = new String[]{"Account Transfer", "From Wallet"};
	private String[] debitTypes = new String[]{"To Wallet", "Account Transfer"};
	
	private ArrayList<Transaction> transactions;
	private ArrayList<Template> templates;
	private int contextMenuTransactionNo;
	private DecimalFormat formatterTextFields;
	private Intent templatesIntent;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
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
		buildButtonPanel();
		
		/*smsIntent = getIntent();
		if(smsIntent.getBooleanExtra("Bank Sms", false))
		{
			performSMSTransaction();
		}*/
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(DatabaseManager.getNumTransactions()==0)
		{
			DatabaseManager.setContext(TransactionsActivity.this);
			DatabaseManager.readDatabase();
			refreshBodyLayout();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_transactions, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(TransactionsActivity.this);
				return true;
				
			case R.id.action_templates:
				templatesIntent = new Intent(TransactionsActivity.this, TemplatesActivity.class);
				startActivityForResult(templatesIntent, 0);
				return true;
				
			case R.id.action_transactionsDisplayOptions:
				displayOptions();
				return true;
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		calculateDimensions();
		readPreferences();
		buildTitleLayout();
		refreshBodyLayout();
		buildButtonPanel();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuTransactionNo = parentLayout.indexOfChild(view);
		menu.setHeaderTitle("Options For Transaction "+(contextMenuTransactionNo+1));
		menu.add(0, view.getId(), 0, "Edit");
		menu.add(0, view.getId(), 0, "Delete");
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if(item.getTitle().equals("Edit"))
		{
			editTransaction(contextMenuTransactionNo);
		}
		else if(item.getTitle().equals("Delete"))
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
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		
		if(VERSION.SDK_INT<=10)
		{
			WIDTH_DATE=20*screenWidth/100-6;
		}
		else
		{
			WIDTH_DATE=20*screenWidth/100-12;
		}
		WIDTH_SLNO=10*screenWidth/100;
		WIDTH_PARTICULARS=50*screenWidth/100;
		WIDTH_AMOUNT=20*screenWidth/100;
		WIDTH_TRANSACTION_BUTTON = screenWidth*25/100;
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		
		if(preferences.contains(KEY_TRANSACTIONS_DISPLAY_INTERVAL))
		{
			transactionsDisplayInterval=preferences.getString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
		}
		
		if(transactionsDisplayInterval.equals("Month"))
		{
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH) + 1;
			long currentMonth = year*100+month;
			transactions = DatabaseManager.getMonthlyTransactions(currentMonth);
		}
		else if(transactionsDisplayInterval.equals("Year"))
		{
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			transactions = DatabaseManager.getYearlyTransactions(year);
		}
		else
		{
			transactions = DatabaseManager.getAllTransactions();
		}
	}
	
	private void buildTitleLayout()
	{
		// To be removed later
		readPreferences();
		
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
				
		TextView slnoTitleView = (TextView)findViewById(R.id.slno);
		LayoutParams slnoTitleParams = (LayoutParams) slnoTitleView.getLayoutParams();
		slnoTitleParams.width = WIDTH_SLNO;
		slnoTitleView.setLayoutParams(slnoTitleParams);
		
		TextView dateTitleView = (TextView)findViewById(R.id.date);
		LayoutParams dateTitleParams = (LayoutParams) dateTitleView.getLayoutParams();
		dateTitleParams.width = WIDTH_DATE;
		dateTitleView.setLayoutParams(dateTitleParams);
		
		TextView particularsTitleView = (TextView)findViewById(R.id.particulars);
		LayoutParams particularsTitleParams = (LayoutParams) particularsTitleView.getLayoutParams();
		particularsTitleParams.width = WIDTH_PARTICULARS;
		particularsTitleView.setLayoutParams(particularsTitleParams);
		
		TextView amountTitleView = (TextView)findViewById(R.id.amount);
		LayoutParams amountTitleParams = (LayoutParams) amountTitleView.getLayoutParams();
		amountTitleParams.width = WIDTH_AMOUNT;
		amountTitleView.setLayoutParams(amountTitleParams);
	}
	
	private void refreshBodyLayout()
	{
		readPreferences();						// This will get the transactions
		buildBodyLayout();
	}
	
	private void buildBodyLayout()
	{
		
		try
		{
			parentLayout = (LinearLayout)findViewById(R.id.layout_parent);
			parentLayout.removeAllViews();
			
			DecimalFormat formatterDisplay = new DecimalFormat("#,##0.##");
			formatterTextFields = new DecimalFormat("##0.##");
			for(int i=0; i<transactions.size(); i++)
			{
				LayoutInflater layoutInflater = LayoutInflater.from(this);
				LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_transactions, null);

				TextView slnoView = (TextView)linearLayout.findViewById(R.id.slno);
				LayoutParams slnoParams = (LayoutParams) slnoView.getLayoutParams();
				slnoParams.width = WIDTH_SLNO;
				slnoView.setLayoutParams(slnoParams);
				slnoView.setText(""+(i+1));
				//slnoView.setMinLines(MIN_LINES);
				
				TextView dateView = (TextView)linearLayout.findViewById(R.id.date);
				LayoutParams dateParams = (LayoutParams) dateView.getLayoutParams();
				dateParams.width = WIDTH_DATE;
				dateView.setLayoutParams(dateParams);
				dateView.setText(transactions.get(i).getDate().getShortDate());
				//dateView.setMinLines(MIN_LINES);
				
				TextView particularsView = (TextView)linearLayout.findViewById(R.id.particulars);
				LayoutParams particularsParams = (LayoutParams) particularsView.getLayoutParams();
				particularsParams.width = WIDTH_PARTICULARS;
				particularsView.setLayoutParams(particularsParams);
				particularsView.setText(transactions.get(i).getParticular());
				//particularsView.setMinLines(MIN_LINES);
				
				TextView amountView = (TextView)linearLayout.findViewById(R.id.amount);
				LayoutParams amountParams = (LayoutParams) amountView.getLayoutParams();
				amountParams.width = WIDTH_AMOUNT;
				amountView.setLayoutParams(amountParams);
				amountView.setText(formatterDisplay.format(transactions.get(i).getAmount()));
				//amountView.setMinLines(MIN_LINES);

				parentLayout.addView(linearLayout);
				registerForContextMenu(linearLayout);
			}
			
			// Scroll the ScrollView To Bottom
			final ScrollView transactionsScrollView = (ScrollView) findViewById(R.id.scrollView_transactions);
			transactionsScrollView.post(new Runnable()
			{
				@Override
				public void run()
				{
					transactionsScrollView.fullScroll(View.FOCUS_DOWN);
					//transactionsScrollView.smoothScrollTo(0, transactionsScrollView.getBottom());
				}
			});
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Building Body Layout\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Set the LayoutParams, OnClickListeners to the buttons in ButtonPanel
	 */
	private void buildButtonPanel()
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
	}
	
	private void buildWalletCreditDialog()
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
				String id = ""+(DatabaseManager.getNumTransactions()+1);
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
								transaction.getAmount());
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
				refreshBodyLayout();
			}
		});
		walletCreditDialog.setNegativeButton("Cancel", null);
		dateField=(EditText)walletCreditDialogView.findViewById(R.id.field_date);
		particularsField=(AutoCompleteTextView)walletCreditDialogView.findViewById(R.id.field_particulars);
		amountField=(EditText)walletCreditDialogView.findViewById(R.id.field_amount);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)walletCreditDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, getTemplateStrings("Wallet Credit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
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
				amountField.setText("" + selectedTemplate.getAmount());
			}
		});
	}
	
	private void buildWalletDebitDialog()
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
								transaction.getRate());
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
				refreshBodyLayout();
			}
		});
		walletDebitDialog.setNegativeButton("Cancel", null);
		
		dateField=(EditText)walletDebitDialogView.findViewById(R.id.field_date);
		typesList = (Spinner)walletDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		particularsField =(AutoCompleteTextView)walletDebitDialogView.findViewById(R.id.field_particulars);
		rateField = (EditText)walletDebitDialogView.findViewById(R.id.field_rate);
		quantityField = (EditText)walletDebitDialogView.findViewById(R.id.field_quantity);
		amountField=(EditText)walletDebitDialogView.findViewById(R.id.field_amount);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)walletDebitDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, getTemplateStrings("Wallet Debit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
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
				rateField.setText("" + selectedTemplate.getAmount());
			}
		});
	}
	
	private void buildBankCreditDialog()
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
		
		particularsField = (AutoCompleteTextView)bankCreditDialogView.findViewById(R.id.field_particulars);
		creditTypesList = (Spinner)bankCreditDialogView.findViewById(R.id.list_creditTypes);
		creditTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, creditTypes));
		amountField=(EditText)bankCreditDialogView.findViewById(R.id.field_amount);
		dateField=(EditText)bankCreditDialogView.findViewById(R.id.field_date);
		dateField.setText(new Date(Calendar.getInstance()).getDisplayDate());
		// Related to Templates
		templateCheckBox = (CheckBox)bankCreditDialogView.findViewById(R.id.checkBox_template);
		ArrayAdapter<String> templatesAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_dropdown_item_1line, getTemplateStrings("Bank Credit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
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
				amountField.setText("" + selectedTemplate.getAmount());
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
								transaction.getAmount());
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
				refreshBodyLayout();
			}
		});
		bankCreditDialog.setNegativeButton("Cancel", null);
	}
	
	private void buildBankDebitDialog()
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

		particularsField = (AutoCompleteTextView)bankDebitDialogView.findViewById(R.id.field_particulars);
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
				android.R.layout.simple_dropdown_item_1line, getTemplateStrings("Bank Debit"));
		particularsField.setAdapter(templatesAdapter);
		particularsField.setThreshold(1);
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
				amountField.setText("" + selectedTemplate.getAmount());
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
								transaction.getAmount());
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
				
				refreshBodyLayout();
			}
		});
		bankDebitDialog.setNegativeButton("Cancel", null);
	}
	
	/**
	 * Delete the transaction referred by transactionNo
	 * @param transactionNo Number of the transaction to be deleted
	 */
	private void deleteTransaction(int transactionNo)
	{
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
		deleteDialog.setTitle("Delete Transaction");
		deleteDialog.setMessage("Are You Sure You Want To Delete Transaction No " + (transactionNo+1) + "?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseManager.deleteTransaction(transactions.get(contextMenuTransactionNo));
				//transactions = DatabaseManager.getAllTransactions();
				refreshBodyLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private void editTransaction(final int transactionNo)
	{
		final Transaction oldTransaction = transactions.get(transactionNo);
		String expType = oldTransaction.getType();
		
		if(expType.contains("Wallet Credit"))
		{
			final double backupAmount = oldTransaction.getAmount();
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
					
					if(validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if(saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(), 
									newTransaction.getType(), newTransaction.getAmount());
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
					refreshBodyLayout();
				}
			});
			walletCreditDialog.show();
		}
		else if(expType.contains("Wallet Debit"))
		{
			int oldExpTypeNo = Integer.parseInt(expType.substring(16, 18));   // Wallet Debit Exp01
			buildWalletDebitDialog();
			walletDebitDialog.setTitle("Edit Transaction: Expenditure");
			particularsField.setText(oldTransaction.getParticular());
			typesList.setSelection(oldExpTypeNo);
			rateField.setText(formatterTextFields.format(oldTransaction.getRate()));
			quantityField.setText(formatterTextFields.format(oldTransaction.getQuantity()));
			amountField.setText(formatterTextFields.format(oldTransaction.getAmount()));
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
					
					if(validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if(saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(), 
									newTransaction.getType(), newTransaction.getRate());
							DatabaseManager.addTemplate(template);
						}
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
					refreshBodyLayout();
				}
			});
			walletDebitDialog.show();
		}
		else if(expType.contains("Bank Credit"))
		{
			String oldParticulars = oldTransaction.getParticular();
			final double oldAmount = oldTransaction.getAmount();
			buildBankCreditDialog();
			bankCreditDialog.setTitle("Edit Transaction: Bank Credit");
			String creditType = creditTypes[0];
			if(expType.contains("Income"))
			{
				creditType = creditTypes[0];
				creditTypesList.setSelection(0);
			}
			else if(expType.contains("Savings"))
			{
				creditType = creditTypes[1];
				creditTypesList.setSelection(1);
			}
			final int oldBankNo = Integer.parseInt(expType.substring(12, 14));    // Bank Credit 01 Income
			banks.get(oldBankNo).setChecked(true);
			String oldBankName = DatabaseManager.getBank(oldBankNo).getName();
			int start=oldBankName.length() + 9 + creditType.length() + 2;
			int end=oldParticulars.length();
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
					int newBankNo=0;
					for(int i=0; i<DatabaseManager.getNumBanks(); i++)
					{
						if(banks.get(i).isChecked())
							newBankNo=i;
					}
					
					// Validate Data
					String id = "" + oldTransaction.getID();
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					DecimalFormat formatter = new DecimalFormat("00");
					String type = "Bank Credit " + formatter.format(newBankNo);
					int creditTypesNo = creditTypesList.getSelectedItemPosition();
					if(creditTypesNo == 0)
					{
						type += " Income";
					}
					else if(creditTypesNo == 1)
					{
						type += " Savings";
					}
					String particulars = particularsField.getText().toString();
					String amount = amountField.getText().toString();
					boolean saveAsTemplate = templateCheckBox.isChecked();
					Object[] data = {id, time, time, date, type, particulars, amount, "1", amount};
					Transaction newTransaction;
					boolean validData = isValidData(data);
					
					if(validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if(saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(), 
									newTransaction.getType(), newTransaction.getAmount());
							DatabaseManager.addTemplate(template);
						}
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
					refreshBodyLayout();
				}
			});
			bankCreditDialog.show();
		}
		
		if(expType.contains("Bank Debit"))
		{
			int oldBankNo = Integer.parseInt(expType.substring(11, 13));  // Bank Debit 01 Withdraw
			String oldParticulars = oldTransaction.getParticular();
			final double oldAmount = oldTransaction.getAmount();
			buildBankDebitDialog();
			bankDebitDialog.setTitle("Edit Transaction: Bank Debit");
			String debitType = debitTypes[0];
			int oldExpTypeNo = 0;
			if(expType.contains("Withdraw"))
			{
				debitTypesList.setSelection(0);
				debitType = debitTypes[0];
			}
			else if(expType.contains("Exp"))
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
			String oldBankName = DatabaseManager.getBank(oldBankNo).getName();
			int start=oldBankName.length() + 13 + debitType.length() + 2;
			int end=oldParticulars.length();
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
					int newBankNo=0;
					for(int i=0; i<DatabaseManager.getNumBanks(); i++)
					{
						if(banks.get(i).isChecked())
							newBankNo=i;
					}
					
					String id = "" + oldTransaction.getID();
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					DecimalFormat formatter = new DecimalFormat("00");
					String type = "Bank Debit " + formatter.format(newBankNo);
					int debitTypesNo = debitTypesList.getSelectedItemPosition();
					int expTypeNo = 0;
					if(debitTypesNo == 0)
					{
						type += " Withdraw";
					}
					else if(debitTypesNo == 1)
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
					
					if(validData)
					{
						newTransaction = completeData(data);
						DatabaseManager.editTransaction(oldTransaction, newTransaction);
						if(saveAsTemplate)
						{
							Template template = new Template(0, newTransaction.getParticular(), newTransaction.getType(), 
									newTransaction.getAmount());
							DatabaseManager.addTemplate(template);
						}
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
					refreshBodyLayout();
				}
			});
			bankDebitDialog.show();
		}
	}
	
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
	}
	
	private Transaction completeData(Object[] data)
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
	}
	
	private ArrayList<String> getTemplateStrings(String type)
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
	}
	
	private void displayOptions()
	{
		final ArrayList<String> expTypes = DatabaseManager.getAllExpenditureTypes();
		
		AlertDialog.Builder optionsDialogBuilder = new AlertDialog.Builder(this);
		optionsDialogBuilder.setTitle("Select Which Transactions To Display");
		LayoutInflater inflater = LayoutInflater.from(this);
		
		final RelativeLayout optionsDialogLayout = (RelativeLayout) inflater.inflate(R.layout.layout_transactions_display_options, null);
		final LinearLayout expendituresLayout = (LinearLayout) optionsDialogLayout.findViewById(R.id.expendituresLayout);
		final LinearLayout bankTransactionsLayout = (LinearLayout) optionsDialogLayout.findViewById(R.id.bankTransactionsLayout);
		final ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
		CheckBox incomesCheckBox = (CheckBox) optionsDialogLayout.findViewById(R.id.checkBox_incomes);
		checkBoxes.add(incomesCheckBox);
		for(String expType: expTypes)
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
		for(String expType: expTypes)
		{
			CheckBox cb1 = new CheckBox(this);
			cb1.setId(expTypes.indexOf(expType) + 10);
			cb1.setText(expType + "A/C Transfer");
			bankTransactionsLayout.addView(cb1);
			checkBoxes.add(cb1);
		}
		
		CheckBox expendituresCheckBox = (CheckBox) optionsDialogLayout.findViewById(R.id.checkBox_expenditures);
		expendituresCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if(isChecked)
				{
					for(int i=0; i<expTypes.size(); i++)
					{
						CheckBox cb1 = (CheckBox) expendituresLayout.findViewById(i);
						cb1.setChecked(true);
						CheckBox cb2 = (CheckBox) bankTransactionsLayout.findViewById(i + 10);
						cb2.setChecked(true);
					}
				}
			}
		});
		
		CheckBox allBankTransactionsCheckBox = (CheckBox) optionsDialogLayout.findViewById(R.id.checkBox_allBankTransactions);
		allBankTransactionsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if(isChecked)
				{
					for(int i=0; i<expTypes.size(); i++)
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
		
		optionsDialogBuilder.setView(optionsDialogLayout);
		
		optionsDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DecimalFormat formatter = new DecimalFormat("00");
				ArrayList<String> types = new ArrayList<String>();
				if(checkBoxes.get(0).isChecked())
				{
					types.add("Wallet Credit");
					Toast.makeText(getApplicationContext(), "Check-Point 04", Toast.LENGTH_SHORT).show();
				}
				for(int i=0; i<expTypes.size(); i++)
				{
					if(checkBoxes.get(i+1).isChecked())
					{
						types.add("Wallet Debit Exp " + formatter.format(i));
					}
				}
				if(checkBoxes.get(expTypes.size()+1).isChecked())
				{
					int numBanks = DatabaseManager.getNumBanks();
					for(int j=0; j<numBanks; j++)
					{
						types.add("Bank Credit " + formatter.format(j) + " Income");
					}
				}
				if(checkBoxes.get(expTypes.size()+2).isChecked())
				{
					int numBanks = DatabaseManager.getNumBanks();
					for(int j=0; j<numBanks; j++)
					{
						types.add("Bank Credit " + formatter.format(j) + " Savings");
					}
				}
				if(checkBoxes.get(expTypes.size()+3).isChecked())
				{
					int numBanks = DatabaseManager.getNumBanks();
					for(int j=0; j<numBanks; j++)
					{
						types.add("Bank Debit " + formatter.format(j) + " Withdraw");
					}
				}
				for(int i=0; i<expTypes.size(); i++)
				{
					if(checkBoxes.get(expTypes.size()+4+i).isChecked())
					{
						int numBanks = DatabaseManager.getNumBanks();
						for(int j=0; j<numBanks; j++)
						{
							types.add("Bank Debit " + formatter.format(j) + " Exp" + formatter.format(i));
						}
					}
				}
				transactions = DatabaseManager.getTransactions("all", types, null);
				//Toast.makeText(getApplicationContext(), ""+transactions.size(), Toast.LENGTH_SHORT).show();
				buildBodyLayout();
			}
		});
		optionsDialogBuilder.setNegativeButton("Cancel", null);
		optionsDialogBuilder.show();
	}
}