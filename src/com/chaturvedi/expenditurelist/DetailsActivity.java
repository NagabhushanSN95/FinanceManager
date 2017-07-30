// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.DatabaseManager;
import com.chaturvedi.expenditurelist.database.Time;
import com.chaturvedi.expenditurelist.database.Transaction;

public class DetailsActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MIN_LINES = 3;
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
	
	private EditText particularsField;
	private Spinner typesList;
	private EditText rateField;
	private EditText quantityField;
	private EditText amountField;
	private EditText dateField;
	private Spinner creditTypesList;
	private Spinner debitTypesList;
	private ArrayList<RadioButton> banks;
	private String[] creditTypes = new String[]{"Account Transfer", "From Wallet"};
	private String[] debitTypes = new String[]{"To Wallet", "Account Transfer"};
	
	private int contextMenuTransactionNo;
	private Intent smsIntent;
	private DecimalFormat formatterTextFields;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_details);
		}
		else
		{
			setContentView(R.layout.activity_details);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		calculateDimensions();
		buildTitleLayout();
		buildBodyLayout();
		buildButtonPanel();
		
		smsIntent = getIntent();
		if(smsIntent.getBooleanExtra("Bank Sms", false))
		{
			performSMSTransaction();
		}
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		DatabaseManager.saveDatabase();
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
	
	private void buildTitleLayout()
	{
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
	
	private void buildBodyLayout()
	{
		try
		{
			parentLayout = (LinearLayout)findViewById(R.id.layout_parent);
			parentLayout.removeAllViews();
			
			ArrayList<String> dates = DatabaseManager.getAllDates();
			ArrayList<String> particulars = DatabaseManager.getAllParticulars();
			ArrayList<Double> amounts = DatabaseManager.getAllAmounts();
			DecimalFormat formatterDisplay = new DecimalFormat("#,##0.##");
			formatterTextFields = new DecimalFormat("##0.##");
			for(int i=0; i<DatabaseManager.getNumTransactions(); i++)
			{
				LayoutInflater layoutInflater = LayoutInflater.from(this);
				LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_details, null);

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
				dateView.setText(dates.get(i).substring(0, dates.get(i).indexOf("/20")));
				//dateView.setMinLines(MIN_LINES);
				
				TextView particularsView = (TextView)linearLayout.findViewById(R.id.particulars);
				LayoutParams particularsParams = (LayoutParams) particularsView.getLayoutParams();
				particularsParams.width = WIDTH_PARTICULARS;
				particularsView.setLayoutParams(particularsParams);
				particularsView.setText(particulars.get(i));
				//particularsView.setMinLines(MIN_LINES);
				
				TextView amountView = (TextView)linearLayout.findViewById(R.id.amount);
				LayoutParams amountParams = (LayoutParams) amountView.getLayoutParams();
				amountParams.width = WIDTH_AMOUNT;
				amountView.setLayoutParams(amountParams);
				amountView.setText(formatterDisplay.format(amounts.get(i)));
				//amountView.setMinLines(MIN_LINES);

				parentLayout.addView(linearLayout);
				registerForContextMenu(linearLayout);
			}
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
				buildBankCreditDialog();
				bankCreditDialog.show();
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
				buildBankDebitDialog();
				bankDebitDialog.show();
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
				String id = ""+DatabaseManager.getNumTransactions();
				Time time = new Time(Calendar.getInstance());
				String date = dateField.getText().toString();
				String type = "Wallet Credit";
				String particulars = particularsField.getText().toString().trim();
				String amount = amountField.getText().toString();
				String[] data = {id, date, type, particulars, amount, "1", amount};
				Transaction transaction = null;
				
				boolean validData = isValidData(data);
				if(validData)
				{
					transaction = completeData(data, time, time);
					DatabaseManager.addTransaction(transaction);
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
			}
		});
		walletCreditDialog.setNegativeButton("Cancel", null);
		dateField=(EditText)walletCreditDialogView.findViewById(R.id.field_date);
		particularsField=(EditText)walletCreditDialogView.findViewById(R.id.field_particulars);
		amountField=(EditText)walletCreditDialogView.findViewById(R.id.field_amount);
		
		Calendar calendar=Calendar.getInstance();
		String date=calendar.get(Calendar.DATE)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.YEAR);
		dateField.setText(date);
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
				String[] data = {id, date, type, particulars, rate, quantity, amount};
				Transaction transaction;
				boolean validData = isValidData(data);
				
				if(validData)
				{
					transaction = completeData(data, time, time);
					DatabaseManager.addTransaction(transaction);
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
					walletDebitDialog.show();
				}
				buildBodyLayout();
			}
		});
		walletDebitDialog.setNegativeButton("Cancel", null);
		
		dateField=(EditText)walletDebitDialogView.findViewById(R.id.field_date);
		typesList = (Spinner)walletDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		particularsField=(EditText)walletDebitDialogView.findViewById(R.id.field_particulars);
		rateField = (EditText)walletDebitDialogView.findViewById(R.id.field_rate);
		quantityField = (EditText)walletDebitDialogView.findViewById(R.id.field_quantity);
		amountField=(EditText)walletDebitDialogView.findViewById(R.id.field_amount);
		
		Calendar calendar=Calendar.getInstance();
		String date=calendar.get(Calendar.DATE)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.YEAR);
		dateField.setText(date);
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
			banks.get(i).setText(DatabaseManager.getBankName(i));
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(Color.BLUE);
			banksRadioGroup.addView(banks.get(i));
		}
		banks.get(0).setChecked(true);
		
		particularsField = (EditText)bankCreditDialogView.findViewById(R.id.field_particulars);
		creditTypesList = (Spinner)bankCreditDialogView.findViewById(R.id.list_creditTypes);
		creditTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, creditTypes));
		amountField=(EditText)bankCreditDialogView.findViewById(R.id.field_amount);
		dateField=(EditText)bankCreditDialogView.findViewById(R.id.field_date);
		
		Calendar calendar=Calendar.getInstance();
		String date=calendar.get(Calendar.DATE)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.YEAR);
		dateField.setText(date);
		
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
				String[] data = {id, date, type, particulars, amount, "1", amount};
				Transaction transaction = null;
				boolean validData = isValidData(data);
				
				if(validData)
				{
					transaction = completeData(data, time, time);
					DatabaseManager.addTransaction(transaction);
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
			banks.get(i).setText(DatabaseManager.getBankName(i));
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(Color.BLUE);
			banksRadioGroup.addView(banks.get(i));
		}
		banks.get(0).setChecked(true);

		particularsField = (EditText)bankDebitDialogView.findViewById(R.id.field_particulars);
		debitTypesList = (Spinner)bankDebitDialogView.findViewById(R.id.list_debitTypes);
		debitTypesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, debitTypes));
		typesList = (Spinner)bankDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getAllExpenditureTypes()));
		typesList.setVisibility(View.GONE);
		amountField=(EditText)bankDebitDialogView.findViewById(R.id.field_amount);
		dateField=(EditText)bankDebitDialogView.findViewById(R.id.field_date);
		Calendar calendar=Calendar.getInstance();
		String date=calendar.get(Calendar.DATE)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.YEAR);
		dateField.setText(date);
		
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
				String[] data = {id, date, type, particulars, amount, "1", amount};
				Transaction transaction;
				boolean validData = isValidData(data);
				
				if(validData)
				{
					transaction = completeData(data, time, time);
					DatabaseManager.addTransaction(transaction);
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
				DatabaseManager.deleteTransaction(contextMenuTransactionNo);
				buildBodyLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private void editTransaction(final int transactionNo)
	{
		String expType = DatabaseManager.getType(transactionNo);
		
		if(expType.contains("Wallet Credit"))
		{
			final double backupAmount = DatabaseManager.getAmount(transactionNo);
			buildWalletCreditDialog();
			particularsField.setText(DatabaseManager.getParticular(transactionNo));
			amountField.setText(formatterTextFields.format(backupAmount));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			walletCreditDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String id = "" + transactionNo;
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					String type = "Wallet Credit";
					String particulars = particularsField.getText().toString().trim();
					String amount = amountField.getText().toString();
					String[] data = {id, date, type, particulars, amount, "1", amount};
					Transaction transaction = null;
					boolean validData = isValidData(data);
					
					if(validData)
					{
						transaction = completeData(data, time, time);
						DatabaseManager.editTransaction(transactionNo, transaction);
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
				}
			});
			walletCreditDialog.show();
		}
		else if(expType.contains("Wallet Debit"))
		{
			int oldExpTypeNo = Integer.parseInt(expType.substring(16, 18));   // Wallet Debit Exp01
			buildWalletDebitDialog();
			particularsField.setText(DatabaseManager.getParticular(transactionNo));
			typesList.setSelection(oldExpTypeNo);
			rateField.setText(formatterTextFields.format(DatabaseManager.getRate(transactionNo)));
			quantityField.setText(formatterTextFields.format(DatabaseManager.getQuantity(transactionNo)));
			amountField.setText(formatterTextFields.format(DatabaseManager.getAmount(transactionNo)));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			
			walletDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					String id = "" + transactionNo;
					Time time = new Time(Calendar.getInstance());
					String date = dateField.getText().toString();
					DecimalFormat formatter = new DecimalFormat("00");
					int newExpTypeNo = typesList.getSelectedItemPosition();
					String type = "Wallet Debit Exp" + formatter.format(newExpTypeNo);
					String particulars = particularsField.getText().toString();
					String rate = rateField.getText().toString();
					String quantity = quantityField.getText().toString();
					String amount = amountField.getText().toString();
					String[] data = {id, date, type, particulars, rate, quantity, amount};
					Transaction transaction;
					boolean validData = isValidData(data);
					
					if(validData)
					{
						transaction = completeData(data, time, time);
						DatabaseManager.editTransaction(transactionNo, transaction);
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
					buildBodyLayout();
				}
			});
			walletDebitDialog.show();
		}
		else if(expType.contains("Bank Credit"))
		{
			String oldParticulars = DatabaseManager.getParticular(transactionNo);
			final double oldAmount = DatabaseManager.getAmount(transactionNo);
			buildBankCreditDialog();
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
			String oldBankName = DatabaseManager.getBankName(oldBankNo);
			int start=oldBankName.length() + 9 + creditType.length() + 2;
			int end=oldParticulars.length();
			String netParticulars = oldParticulars.substring(start, end);
			particularsField.setText(netParticulars);
			amountField.setText(formatterTextFields.format(oldAmount));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			
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
					String id = "" + transactionNo;
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
					String[] data = {id, date, type, particulars, amount, "1", amount};
					Transaction transaction;
					boolean validData = isValidData(data);
					
					if(validData)
					{
						transaction = completeData(data, time, time);
						DatabaseManager.editTransaction(transactionNo, transaction);
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
					buildBodyLayout();
				}
			});
			bankCreditDialog.show();
		}
		
		if(expType.contains("Bank Debit"))
		{
			int oldBankNo = Integer.parseInt(expType.substring(11, 13));  // Bank Debit 01 Withdraw
			String oldParticulars = DatabaseManager.getParticular(transactionNo);
			final double oldAmount = DatabaseManager.getAmount(transactionNo);
			buildBankDebitDialog();
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
			String oldBankName = DatabaseManager.getBankName(oldBankNo);
			int start=oldBankName.length() + 13 + debitType.length() + 2;
			int end=oldParticulars.length();
			String netParticulars = oldParticulars.substring(start, end);
			
			banks.get(oldBankNo).setChecked(true);
			particularsField.setText(netParticulars);
			amountField.setText(formatterTextFields.format(oldAmount));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			
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
					
					String id = "" + transactionNo;
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
					String[] data = {id, date, type, particulars, amount, "1", amount};
					Transaction transaction;
					boolean validData = isValidData(data);
					
					if(validData)
					{
						transaction = completeData(data, time, time);
						DatabaseManager.editTransaction(transactionNo, transaction);
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
					buildBodyLayout();
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
	private boolean isValidData(String[] data)
	{
		//int id = Integer.parseInt(data[0]);
		String date = data[1];
		String type = data[2];
		String particulars = data[3];
		String rate = data[4];
		String quantity = data[5];
		String amount = data[6];
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
			else if(!isValidDate(date))
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
			else if(!isValidDate(date))
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
			else if(!isValidDate(date))
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
			else if(!isValidDate(date))
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
	
	/**
	 * Checks if the date is valid
	 * @param dateString
	 * @return true if date is valid, else false
	 */
	private boolean isValidDate(String dateString)
	{
		boolean validDate = true;
		try
		{
			StringTokenizer tokens = new StringTokenizer(dateString, "/-.,");
			int date = Integer.parseInt(tokens.nextToken());
			int month = Integer.parseInt(tokens.nextToken());
			int year = Integer.parseInt(tokens.nextToken());
			
			if(month==1 || month==3 || month==5 || month==7 || month==8 || month==10 || month==12) // Months Having 31 days
			{
				if(date>0 && date<=31)
					validDate = true;
				else
					validDate = false;
			}
			else if(month==4 || month==6 || month==9 || month==11) // Months having 30 days
			{
				if(date>0 && date<=30)
					validDate = true;
				else
					validDate = false;
			}
			else if(month==2) // February
			{
				if(year%4==0) // Leap Year
				{
					if(date>0 && date<=29)
						validDate = true;
					else
						validDate = false;
				}
				else
				{
					if(date>0 && date<=28)
						validDate = true;
					else
						validDate = false;
				}
			}
		}
		catch(Exception e)
		{
			validDate = false;
		}
		
		return validDate;
	}
	
	private Transaction completeData(String[] data, Time createdTime, Time modifiedTime)
	{
		int id = Integer.parseInt(data[0]);
		String date = data[1];
		String type = data[2];
		String particulars = data[3];
		String rate = data[4];
		String quantity = data[5];
		String amount = data[6];
		Transaction transaction = null;
		
		if(type.contains("Wallet Credit"))
		{
			transaction = new Transaction(id, createdTime, modifiedTime, date, type, particulars, 
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
			transaction = new Transaction(id, createdTime, modifiedTime, date, type, particulars, 
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
			particulars = DatabaseManager.getBankName(bankNo) + " Credit: " + creditTypes[creditTypesNo] + ": " + particulars;
			transaction = new Transaction(id, createdTime, modifiedTime, date, type, particulars, 
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
			particulars = DatabaseManager.getBankName(bankNo) + " Withdrawal: "+ debitTypes[debitTypesNo] + ": " + particulars;
			transaction = new Transaction(id, createdTime, modifiedTime, date, type, particulars, 
					Double.parseDouble(rate), Double.parseDouble(quantity), Double.parseDouble(amount));
		}
		
		return transaction;	
	}
	
	/**
	 * Takes the details of the sms from the intent and performs the necessary transaction
	 */
	private void performSMSTransaction()
	{
		int bankNo = smsIntent.getIntExtra("Bank Number", 0);
		String type = smsIntent.getStringExtra("Type");
		double amount = smsIntent.getDoubleExtra("Amount", 0);
		
		if(type.equals("credit"))
		{
			buildBankCreditDialog();
			banks.get(bankNo).setChecked(true);
			amountField.setText(formatterTextFields.format(amount));
			bankCreditDialog.show();
		}
		else
		{
			buildBankDebitDialog();
			banks.get(bankNo).setChecked(true);
			amountField.setText(formatterTextFields.format(amount));
			bankDebitDialog.show();
		}
	}
}