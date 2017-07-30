// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

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

public class DetailsActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_SLNO;
	private int WIDTH_DATE;
	private int WIDTH_PARTICULARS;
	private int WIDTH_AMOUNT;
	
	//private LinearLayout titleLayout;
	//private ArrayList<LinearLayout> itemsLayout;
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
		
		buildTitleLayout();
		buildBodyLayout();
		buildButtonPanel();
		
		smsIntent = getIntent();
		if(smsIntent.getBooleanExtra("Bank Sms", false))
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
	
	@Override
	public void onPause()
	{
		super.onPause();
		DatabaseManager.saveDatabase();
	}
	
	/*@Override
	public void onStart()
	{
		super.onPause();
		DatabaseManager.readDatabase();
	}*/
	
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
			DatabaseManager.deleteTransaction(contextMenuTransactionNo);
			buildBodyLayout();
		}
		else
		{
			return false;
		}
		return true;
	}
	
	private void buildTitleLayout()
	{
		//titleLayout=(LinearLayout)findViewById(R.id.layout_title);
		
		TextView slnoTitleView = (TextView)findViewById(R.id.slno);
		LayoutParams slnoTitleParams = new LayoutParams(WIDTH_SLNO, LayoutParams.WRAP_CONTENT);
		slnoTitleView.setLayoutParams(slnoTitleParams);
		
		TextView dateTitleView = (TextView)findViewById(R.id.date);
		LayoutParams dateTitleParams = new LayoutParams(WIDTH_DATE, LayoutParams.WRAP_CONTENT);
		dateTitleView.setLayoutParams(dateTitleParams);
		
		TextView particularsTitleView = (TextView)findViewById(R.id.particulars);
		LayoutParams particularsTitleParams = new LayoutParams(WIDTH_PARTICULARS, LayoutParams.WRAP_CONTENT);
		particularsTitleView.setLayoutParams(particularsTitleParams);
		
		TextView amountTitleView = (TextView)findViewById(R.id.amount);
		LayoutParams amountTitleParams = new LayoutParams(WIDTH_AMOUNT, LayoutParams.WRAP_CONTENT);
		amountTitleView.setLayoutParams(amountTitleParams);
	}
	
	private void buildBodyLayout()
	{
		try
		{
			parentLayout = (LinearLayout)findViewById(R.id.layout_parent);
			parentLayout.removeAllViews();
			
			//itemsLayout = new ArrayList<LinearLayout>();
			ArrayList<String> dates = DatabaseManager.getDates();
			ArrayList<String> particulars = DatabaseManager.getParticulars();
			ArrayList<Double> amounts = DatabaseManager.getAmounts();
			DecimalFormat formatterDisplay = new DecimalFormat("#,##0.##");
			formatterTextFields = new DecimalFormat("##0.##");
			for(int i=0; i<DatabaseManager.getNumTransactions(); i++)
			{
				LayoutInflater layoutInflater = LayoutInflater.from(this);
				LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_details, null);

				TextView slnoView = (TextView)linearLayout.findViewById(R.id.slno);
				LayoutParams slnoParams = new LayoutParams(WIDTH_SLNO, LayoutParams.WRAP_CONTENT);
				slnoView.setLayoutParams(slnoParams);
				slnoView.setText(""+(i+1));
				
				TextView dateView = (TextView)linearLayout.findViewById(R.id.date);
				LayoutParams dateParams = new LayoutParams(WIDTH_DATE, LayoutParams.WRAP_CONTENT);
				dateView.setLayoutParams(dateParams);
				dateView.setText(dates.get(i).substring(0, dates.get(i).indexOf("/20")));
				
				TextView particularsView = (TextView)linearLayout.findViewById(R.id.particulars);
				LayoutParams particularsParams = new LayoutParams(WIDTH_PARTICULARS, LayoutParams.WRAP_CONTENT);
				particularsView.setLayoutParams(particularsParams);
				particularsView.setText(particulars.get(i));
				
				TextView amountView = (TextView)linearLayout.findViewById(R.id.amount);
				LayoutParams amountParams = new LayoutParams(WIDTH_AMOUNT, LayoutParams.WRAP_CONTENT);
				amountView.setLayoutParams(amountParams);
				amountView.setText(formatterDisplay.format(amounts.get(i)));

				//itemsLayout.add(linearLayout);
				parentLayout.addView(linearLayout);
				registerForContextMenu(linearLayout);
			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Building Body Layout\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void buildButtonPanel()
	{
		walletCreditButton=(ImageButton)findViewById(R.id.button_wallet_credit);
		LayoutParams walletCreditButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
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
		LayoutParams walletDebitButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
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
		LayoutParams bankCreditButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
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
		LayoutParams bankDebitButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
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
				String particulars = particularsField.getText().toString().trim();
				String amount = amountField.getText().toString();
				String date = dateField.getText().toString();
				boolean dataCorrect= false;
				
				if(particulars.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else if(amount.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else if(date.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					DatabaseManager.increamentNumTransations();
					DatabaseManager.increamentWalletBalance(amount);
					DatabaseManager.increamentIncome(amount);
					DatabaseManager.addDate(date);
					DatabaseManager.addType("Income");
					DatabaseManager.addParticular(particularsField.getText().toString());
					DatabaseManager.addRate(amount);
					DatabaseManager.addQuantity(1);
					DatabaseManager.addAmount(amount);
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
				String particulars = particularsField.getText().toString();
				int expTypeNo = typesList.getSelectedItemPosition();
				String rate = rateField.getText().toString();
				String quantity = quantityField.getText().toString();
				String amount = amountField.getText().toString();
				String date = dateField.getText().toString();
				boolean dataCorrect = false;
				
				if(particulars.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else if(date.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else if(amount.length()==0)
				{
					if(rate.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Rate And Amount", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else if(quantity.length()==0)
					{
						amount = rate;
						quantity = String.valueOf(1);
						dataCorrect = true;
					}
					else
					{
						amount = ""+Double.parseDouble(rate)*Double.parseDouble(quantity);
						dataCorrect = true;
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
					dataCorrect = true;
				}
				else if(quantity.length()==0)
				{
					quantity = ""+Math.round(Double.parseDouble(amount)/Double.parseDouble(rate));
					dataCorrect = true;
				}
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					DatabaseManager.increamentNumTransations();
					DatabaseManager.decreamentWalletBalance(amount);
					DatabaseManager.increamentAmountSpent(amount);
					DatabaseManager.addDate(date);
					DatabaseManager.addType(expTypeNo);
					DatabaseManager.addParticular(particulars);
					DatabaseManager.addRate(rate);
					DatabaseManager.addQuantity(quantity);
					DatabaseManager.addAmount(amount);
					DatabaseManager.increamentCounter(expTypeNo, amount);
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
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getExpenditureTypes()));
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
				
				// Validate Data
				String particular = particularsField.getText().toString();
				String type = "Income Bank";
				int creditTypesNo = creditTypesList.getSelectedItemPosition();
				String amount = amountField.getText().toString();
				String date = dateField.getText().toString();
				boolean dataCorrect = false;
				
				if(amount.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else if(date.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					if(particular.length()==0)
					{
						particular = DatabaseManager.getBankName(bankNo) + " Credit: "+ creditTypes[creditTypesList.getSelectedItemPosition()];
					}
					else
					{
						particular = DatabaseManager.getBankName(bankNo) + " Credit: " + particular;
					}

					if(creditTypesNo==0)
					{
						type = "Income Bank";
						DatabaseManager.increamentIncome(amount);
					}
					else if(creditTypesNo==1)
					{
						type = "Bank Savings";
						DatabaseManager.decreamentWalletBalance(amount);
					}
					DatabaseManager.increamentBankBalance(bankNo, amount);
					DatabaseManager.increamentNumTransations();
					DatabaseManager.addDate(date);
					DatabaseManager.addType(type);
					DatabaseManager.addParticular(particular);
					DatabaseManager.addRate(amount);
					DatabaseManager.addQuantity(1);
					DatabaseManager.addAmount(amount);
				}
				else
				{
					buildBankCreditDialog();
					banks.get(bankNo).setChecked(true);
					particularsField.setText(particular);
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
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getExpenditureTypes()));
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
				String particular = particularsField.getText().toString();
				String type = "Bank Withdraw";
				int debitTypesNo = debitTypesList.getSelectedItemPosition();
				int expTypeNo = 0;
				String amount = amountField.getText().toString();
				String date = dateField.getText().toString();
				boolean dataCorrect = false;
				
				if(amount.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else if(date.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					if(particular.length()==0)
					{
						particular = DatabaseManager.getBankName(bankNo) + " Withdrawal: "+ debitTypes[debitTypesList.getSelectedItemPosition()];
					}
					else
					{
						particular = DatabaseManager.getBankName(bankNo) + " Withdrawal: " + particular;
					}
					
					if(debitTypesNo==0)
					{
						type = "Bank Withdraw";
						DatabaseManager.increamentWalletBalance(amount);
					}
					else if(debitTypesNo==1)
					{
						type = (String) typesList.getSelectedItem() + " Bank";
						expTypeNo = typesList.getSelectedItemPosition();
						DatabaseManager.increamentAmountSpent(amount);
						DatabaseManager.increamentCounter(typesList.getSelectedItemPosition(), amount);
					}
					
					DatabaseManager.decreamentBankBalance(bankNo, amount);
					DatabaseManager.increamentNumTransations();
					DatabaseManager.addDate(date);
					DatabaseManager.addType(type);
					DatabaseManager.addParticular(particular);
					DatabaseManager.addRate(amount);
					DatabaseManager.addQuantity(1);
					DatabaseManager.addAmount(amount);
				}
				else
				{
					buildBankDebitDialog();
					banks.get(bankNo).setChecked(true);
					particularsField.setText(particular);
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
	
	private void editTransaction(final int transactionNo)
	{
		String expType = DatabaseManager.getType(transactionNo);
		
		if(expType.equals("Income"))
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
					String particulars = particularsField.getText().toString().trim();
					String amount = amountField.getText().toString();
					String date = dateField.getText().toString();
					boolean dataCorrect= false;
					
					if(particulars.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else if(amount.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else if(date.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else
					{
						dataCorrect = true;
					}
					
					if(dataCorrect)
					{
						double newAmount = Double.parseDouble(amount);
						double netAmount = newAmount-backupAmount;
						DatabaseManager.setDate(transactionNo, date);
						DatabaseManager.setParticular(transactionNo, particulars);
						if(newAmount!=DatabaseManager.getAmount(transactionNo))
						{
							DatabaseManager.increamentIncome(netAmount);
							DatabaseManager.increamentWalletBalance(netAmount);
							DatabaseManager.setRate(transactionNo, amount);
							DatabaseManager.setAmount(transactionNo, amount);
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
				}
			});
			walletCreditDialog.show();
		}
		
		for(int i=0; i<5; i++)
		{
			if(expType.equals(DatabaseManager.getExpenditureTypes().get(i)))
			{
				final int expTypeNumBackup = i;
				final double rateBackup = DatabaseManager.getRate(transactionNo);
				final int quantityBackup = DatabaseManager.getQuantity(transactionNo);
				final double amountBackup = DatabaseManager.getAmount(transactionNo);
				
				buildWalletDebitDialog();
				particularsField.setText(DatabaseManager.getParticular(transactionNo));
				typesList.setSelection(expTypeNumBackup);
				rateField.setText(formatterTextFields.format(rateBackup));
				quantityField.setText(formatterTextFields.format(quantityBackup));
				amountField.setText(formatterTextFields.format(amountBackup));
				dateField.setText(DatabaseManager.getDate(transactionNo));
				
				walletDebitDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						String particulars = particularsField.getText().toString();
						int exptypeNo = typesList.getSelectedItemPosition();
						String rate = rateField.getText().toString();
						String quantity = quantityField.getText().toString();
						String amount = amountField.getText().toString();
						String date = dateField.getText().toString();
						boolean dataCorrect = false;
						
						if(particulars.length()==0)
						{
							Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
							dataCorrect = false;
						}
						else if(date.length()==0)
						{
							Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
							dataCorrect = false;
						}
						else if(amount.length()==0)
						{
							if(rate.length()==0)
							{
								Toast.makeText(getApplicationContext(), "Please Enter The Rate And Amount", Toast.LENGTH_LONG).show();
								dataCorrect = false;
							}
							else if(quantity.length()==0)
							{
								amount = rate;
								quantity = String.valueOf(1);
								dataCorrect = true;
							}
							else
							{
								amount = ""+Double.parseDouble(rate)*Double.parseDouble(quantity);
								dataCorrect = true;
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
							dataCorrect = true;
						}
						else if(quantity.length()==0)
						{
							quantity = ""+Math.round(Double.parseDouble(amount)/Double.parseDouble(rate));
							dataCorrect = true;
						}
						else
						{
							dataCorrect = true;
						}
						
						if(dataCorrect)
						{
							int newExpTypeNo = typesList.getSelectedItemPosition();
							double newRate = Double.parseDouble(rate);
							int newQuantity = Integer.parseInt(quantity);
							double newAmount = Double.parseDouble(amount);
							if(newExpTypeNo!=expTypeNumBackup || newRate!=rateBackup || newQuantity!=quantityBackup || newAmount!=amountBackup)
							{
								double netAmount = newAmount - amountBackup;
								DatabaseManager.decreamentCounter(expTypeNumBackup, amountBackup);
								DatabaseManager.increamentCounter(newExpTypeNo, newAmount);
								DatabaseManager.increamentAmountSpent(netAmount);
								DatabaseManager.decreamentWalletBalance(netAmount);
							}
							DatabaseManager.setDate(transactionNo, date);
							DatabaseManager.setType(transactionNo, newExpTypeNo);
							DatabaseManager.setParticular(transactionNo, particulars);
							DatabaseManager.setRate(transactionNo, rate);
							DatabaseManager.setQuantity(transactionNo, quantity);
							DatabaseManager.setAmount(transactionNo, amount);
						}
						else
						{
							buildWalletDebitDialog();
							particularsField.setText(particulars);
							typesList.setSelection(exptypeNo);
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
		}
		
		if(expType.equals("Income Bank"))
		{
			String particularBackup = DatabaseManager.getParticular(transactionNo);
			final double amountBackup = DatabaseManager.getAmount(transactionNo);
			buildBankCreditDialog();
			particularsField.setText(particularBackup);
			creditTypesList.setSelection(0);
			amountField.setText(formatterTextFields.format(DatabaseManager.getAmount(transactionNo)));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			
			// Determine Which Bank
			int bankNo=0;
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				if(particularBackup.contains(DatabaseManager.getBankName(i)))
				{
					banks.get(i).setChecked(true);
					bankNo = i;
					break;
				}
			}
			final int bankNoBackup = bankNo;
			
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
					String particular = particularsField.getText().toString();
					String type = "Income Bank";
					int creditTypeNo = creditTypesList.getSelectedItemPosition();
					String amount = amountField.getText().toString();
					String date = dateField.getText().toString();
					boolean dataCorrect = false;
					
					if(amount.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else if(date.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else
					{
						dataCorrect = true;
					}
					
					if(dataCorrect)
					{
						if(particular.length()==0)
						{
							particular = DatabaseManager.getBankName(newBankNo) + " Credit: "+ creditTypes[creditTypesList.getSelectedItemPosition()];
						}
						else
						{
							particular = DatabaseManager.getBankName(newBankNo) + " Credit: " + particular;
						}
						DatabaseManager.setParticular(transactionNo, particular);
						DatabaseManager.setDate(transactionNo, date);
						
						double newAmount = Double.parseDouble(amount);
						if(bankNoBackup!=newBankNo || newAmount!=amountBackup || creditTypesList.getSelectedItemPosition()!=0)
						{
							DatabaseManager.decreamentIncome(amountBackup);
							DatabaseManager.decreamentBankBalance(bankNoBackup, amountBackup);
							
							if(creditTypesList.getSelectedItemPosition()==0)
							{
								type = "Income Bank";
								DatabaseManager.increamentIncome(amount);
							}
							else if(creditTypesList.getSelectedItemPosition()==1)
							{
								type = "Bank Savings";
								DatabaseManager.decreamentWalletBalance(amount);
							}
							DatabaseManager.increamentBankBalance(newBankNo, amount);
							DatabaseManager.setType(transactionNo, type);
							DatabaseManager.setRate(transactionNo, amount);
							DatabaseManager.setQuantity(transactionNo, 1);
							DatabaseManager.setAmount(transactionNo, amount);
						}
					}
					else
					{
						buildBankCreditDialog();
						banks.get(newBankNo).setChecked(true);
						particularsField.setText(particular);
						creditTypesList.setSelection(creditTypeNo);
						amountField.setText(amount);
						dateField.setText(date);
						bankCreditDialog.show();
					}
					buildBodyLayout();
				}
			});
			bankCreditDialog.show();
		}
		
		if(expType.equals("Bank Savings"))
		{
			String particularBackup = DatabaseManager.getParticular(transactionNo);
			final double amountBackup = DatabaseManager.getAmount(transactionNo);
			buildBankCreditDialog();
			particularsField.setText(particularBackup);
			creditTypesList.setSelection(1);
			amountField.setText(formatterTextFields.format(DatabaseManager.getAmount(transactionNo)));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			
			// Determine Which Bank
			int bankNo=0;
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				if(particularBackup.contains(DatabaseManager.getBankName(i)))
				{
					banks.get(i).setChecked(true);
					bankNo = i;
					break;
				}
			}
			final int bankNoBackup = bankNo;
			
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
					String particular = particularsField.getText().toString();
					String type = "Bank Savings";
					int creditTypeNo = creditTypesList.getSelectedItemPosition();
					String amount = amountField.getText().toString();
					String date = dateField.getText().toString();
					boolean dataCorrect = false;
					
					if(amount.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else if(date.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else
					{
						dataCorrect = true;
					}
					
					if(dataCorrect)
					{
						if(particular.length()==0)
						{
							particular = DatabaseManager.getBankName(newBankNo) + " Credit: "+ creditTypes[creditTypesList.getSelectedItemPosition()];
						}
						else
						{
							particular = DatabaseManager.getBankName(newBankNo) + " Credit: " + particular;
						}
						DatabaseManager.setParticular(transactionNo, particular);
						DatabaseManager.setDate(transactionNo, date);
						
						double newAmount = Double.parseDouble(amount);
						if(bankNoBackup!=newBankNo || newAmount!=amountBackup || creditTypeNo!=1)
						{
							DatabaseManager.increamentWalletBalance(amountBackup);
							DatabaseManager.decreamentBankBalance(bankNoBackup, amountBackup);
							
							if(creditTypeNo==0)
							{
								type = "Income Bank";
								DatabaseManager.increamentIncome(amount);
							}
							else if(creditTypeNo==1)
							{
								type = "Bank Savings";
								DatabaseManager.decreamentWalletBalance(amount);
							}
							DatabaseManager.increamentBankBalance(newBankNo, amount);
							DatabaseManager.setType(transactionNo, type);
							DatabaseManager.setRate(transactionNo, amount);
							DatabaseManager.setQuantity(transactionNo, 1);
							DatabaseManager.setAmount(transactionNo, amount);
						}
					}
					else
					{
						buildBankCreditDialog();
						banks.get(newBankNo).setChecked(true);
						particularsField.setText(particular);
						creditTypesList.setSelection(creditTypeNo);
						amountField.setText(amount);
						dateField.setText(date);
						bankCreditDialog.show();
					}
					buildBodyLayout();
				}
			});
			bankCreditDialog.show();
		}
		
		if(expType.equals("Bank Withdraw"))
		{
			String particularBackup = DatabaseManager.getParticular(transactionNo);
			final double amountBackup = DatabaseManager.getAmount(transactionNo);
			buildBankDebitDialog();
			particularsField.setText(particularBackup);
			debitTypesList.setSelection(0);
			amountField.setText(formatterTextFields.format(DatabaseManager.getAmount(transactionNo)));
			dateField.setText(DatabaseManager.getDate(transactionNo));
			
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
			
			// Determine Which Bank
			int bankNo=0;
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				if(particularBackup.contains(DatabaseManager.getBankName(i)))
				{
					banks.get(i).setChecked(true);
					bankNo = i;
					break;
				}
			}
			final int bankNoBackup = bankNo;
			
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
					
					// Validate Data
					String particular = particularsField.getText().toString();
					String type = "Bank Withdraw";
					int debitTypeNo = debitTypesList.getSelectedItemPosition();
					int expTypeNo = 0;
					String amount = amountField.getText().toString();
					String date = dateField.getText().toString();
					boolean dataCorrect = false;
					
					if(amount.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else if(date.length()==0)
					{
						Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
						dataCorrect = false;
					}
					else
					{
						dataCorrect = true;
					}
					
					if(dataCorrect)
					{
						if(particular.length()==0)
						{
							particular = DatabaseManager.getBankName(newBankNo) + " Withdrawal: "+ debitTypes[debitTypesList.getSelectedItemPosition()];
						}
						else
						{
							particular = DatabaseManager.getBankName(newBankNo) + " Withdrawal: " + particular;
						}
						DatabaseManager.setParticular(transactionNo, particular);
						DatabaseManager.setDate(transactionNo, date);
						
						double newAmount = Double.parseDouble(amount);
						if(bankNoBackup!=newBankNo || newAmount!=amountBackup || debitTypeNo!=0)
						{
							DatabaseManager.decreamentWalletBalance(amountBackup);
							DatabaseManager.increamentBankBalance(bankNoBackup, amountBackup);
							
							if(debitTypeNo==0)
							{
								type = "Bank Withdraw";
								DatabaseManager.increamentWalletBalance(amount);
							}
							else
							{
								type = (String) typesList.getSelectedItem() + " Bank";
								expTypeNo = typesList.getSelectedItemPosition();
								DatabaseManager.increamentAmountSpent(amount);
								DatabaseManager.increamentCounter(typesList.getSelectedItemPosition(), amount);
							}
							
							DatabaseManager.decreamentBankBalance(newBankNo, amount);
							DatabaseManager.setType(transactionNo, type);
							DatabaseManager.setRate(transactionNo, amount);
							DatabaseManager.setQuantity(transactionNo, 1);
							DatabaseManager.setAmount(transactionNo, amount);
						}
					}
					else
					{
						buildBankDebitDialog();
						banks.get(newBankNo).setChecked(true);
						particularsField.setText(particular);
						debitTypesList.setSelection(debitTypeNo);
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
		
		for(int i=0; i<5; i++)
		{
			if(expType.equals(DatabaseManager.getExpenditureTypes().get(i) + " Bank"))
			{
				final int expTypeNumBackup = i;
				String particularBackup = DatabaseManager.getParticular(transactionNo);
				final double amountBackup = DatabaseManager.getAmount(transactionNo);
				buildBankDebitDialog();
				// Determine Which Bank
				int bankNo=0;
				for(int j=0; j<DatabaseManager.getNumBanks(); j++)
				{
					if(particularBackup.contains(DatabaseManager.getBankName(j)))
					{
						banks.get(j).setChecked(true);
						bankNo = j;
						break;
					}
				}
				final int bankNoBackup = bankNo;
				
				banks.get(bankNoBackup).setChecked(true);
				particularsField.setText(DatabaseManager.getParticular(transactionNo));
				debitTypesList.setSelection(1);
				typesList.setSelection(expTypeNumBackup);
				amountField.setText(formatterTextFields.format(amountBackup));
				dateField.setText(DatabaseManager.getDate(transactionNo));
				
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
						String particular = particularsField.getText().toString();
						String type = DatabaseManager.getExpenditureTypes().get(0) + " Bank";
						int debitTypeNo = debitTypesList.getSelectedItemPosition();
						int expTypeNo = 0;
						String amount = amountField.getText().toString();
						String date = dateField.getText().toString();
						boolean dataCorrect = false;
						
						if(amount.length()==0)
						{
							Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
							dataCorrect = false;
						}
						else if(date.length()==0)
						{
							Toast.makeText(getApplicationContext(), "Please Enter The Date", Toast.LENGTH_LONG).show();
							dataCorrect = false;
						}
						else
						{
							dataCorrect = true;
						}
						
						if(dataCorrect)
						{
							DatabaseManager.decreamentAmountSpent(amountBackup);
							DatabaseManager.decreamentCounter(expTypeNumBackup, amountBackup);
							DatabaseManager.increamentBankBalance(bankNoBackup, amountBackup);
							if(particular.length()==0)
							{
								particular = DatabaseManager.getBankName(bankNo) + " Withdrawal: "+ debitTypes[debitTypeNo];
							}
							else
							{
								particular = DatabaseManager.getBankName(bankNo) + " Withdrawal: " + particular;
							}
							
							if(debitTypeNo==0)
							{
								type = "Bank Withdraw";
								DatabaseManager.increamentWalletBalance(amount);
							}
							else if(debitTypeNo==1)
							{
								type = (String) typesList.getSelectedItem() + " Bank";
								expTypeNo = typesList.getSelectedItemPosition();
								DatabaseManager.increamentAmountSpent(amount);
								DatabaseManager.increamentCounter(typesList.getSelectedItemPosition(), amount);
							}
							
							DatabaseManager.decreamentBankBalance(bankNo, amount);
							DatabaseManager.setDate(transactionNo, dateField.getText().toString());
							DatabaseManager.setType(transactionNo, type);
							DatabaseManager.setParticular(transactionNo, particular);
							DatabaseManager.setRate(transactionNo, amount);
							DatabaseManager.setQuantity(transactionNo, 1);
							DatabaseManager.setAmount(transactionNo, amount);
						}
						else
						{
							buildBankDebitDialog();
							banks.get(bankNo).setChecked(true);
							particularsField.setText(particular);
							debitTypesList.setSelection(debitTypeNo);
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
	}
}