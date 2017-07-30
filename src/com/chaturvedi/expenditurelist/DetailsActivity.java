// Shree KRISHNAya Namaha
// Author: Nagabhushan S N

package com.chaturvedi.expenditurelist;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
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
	private ArrayList<RadioButton> banks;
	
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
			DecimalFormat formatter = new DecimalFormat("#,##0");
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
				amountView.setText(formatter.format(amounts.get(i)));

				//itemsLayout.add(linearLayout);
				parentLayout.addView(linearLayout);
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
		walletCreditButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildWalletCreditDialog();
				walletCreditDialog.show();
			}
		});
		LayoutParams walletCreditButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
		walletCreditButton.setLayoutParams(walletCreditButtonParams);
		
		walletDebitButton=(ImageButton)findViewById(R.id.button_wallet_debit);
		walletDebitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildWalletDebitDialog();
				walletDebitDialog.show();
			}
		});
		LayoutParams walletDebitButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
		walletDebitButton.setLayoutParams(walletDebitButtonParams);
		
		bankCreditButton=(ImageButton)findViewById(R.id.button_bank_credit);
		bankCreditButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildBankCreditDialog();
				bankCreditDialog.show();
			}
		});
		LayoutParams bankCreditButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
		bankCreditButton.setLayoutParams(bankCreditButtonParams);
		
		bankDebitButton=(ImageButton)findViewById(R.id.button_bank_debit);
		bankDebitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildBankDebitDialog();
				bankDebitDialog.show();
			}
		});
		LayoutParams bankDebitButtonParams = new LayoutParams(screenWidth/4, LayoutParams.WRAP_CONTENT);
		bankDebitButton.setLayoutParams(bankDebitButtonParams);
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
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					DatabaseManager.increamentNumTransations();
					DatabaseManager.increamentWalletBalance(amount);
					DatabaseManager.increamentIncome(amount);
					DatabaseManager.addDate(dateField.getText().toString());
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
					walletCreditDialog.show();
				}
				buildBodyLayout();
			}
		});
		walletCreditDialog.setNegativeButton("Cancel", null);
		dateField=(EditText)walletCreditDialogView.findViewById(R.id.edit_date);
		particularsField=(EditText)walletCreditDialogView.findViewById(R.id.edit_particulars);
		amountField=(EditText)walletCreditDialogView.findViewById(R.id.edit_amount);
		
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
				String rate = rateField.getText().toString();
				String quantity = quantityField.getText().toString();
				String amount = amountField.getText().toString();
				boolean dataCorrect = false;
				
				if(particulars.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Particulars", Toast.LENGTH_LONG).show();
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
				
				if(dataCorrect)
				{
					DatabaseManager.increamentNumTransations();
					DatabaseManager.decreamentWalletBalance(amount);
					DatabaseManager.increamentAmountSpent(amount);
					DatabaseManager.addDate(dateField.getText().toString());
					DatabaseManager.addType(typesList.getSelectedItemPosition());
					DatabaseManager.addParticular(particulars);
					DatabaseManager.addRate(rate);
					DatabaseManager.addQuantity(quantity);
					DatabaseManager.addAmount(amount);
					DatabaseManager.increamentCounter(typesList.getSelectedItemPosition(), amount);
				}
				else
				{
					buildWalletDebitDialog();
					particularsField.setText(particulars);
					rateField.setText(rate);
					quantityField.setText(quantity);
					amountField.setText(amount);
					walletDebitDialog.show();
				}
				buildBodyLayout();
			}
		});
		walletDebitDialog.setNegativeButton("Cancel", null);
		dateField=(EditText)walletDebitDialogView.findViewById(R.id.edit_date);
		typesList = (Spinner)walletDebitDialogView.findViewById(R.id.list_types);
		typesList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DatabaseManager.getExpenditureTypes()));
		particularsField=(EditText)walletDebitDialogView.findViewById(R.id.edit_particulars);
		rateField = (EditText)walletDebitDialogView.findViewById(R.id.edit_rate);
		quantityField = (EditText)walletDebitDialogView.findViewById(R.id.edit_quantity);
		amountField=(EditText)walletDebitDialogView.findViewById(R.id.edit_amount);
		
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
		amountField=(EditText)bankCreditDialogView.findViewById(R.id.edit_amount);
		dateField=(EditText)bankCreditDialogView.findViewById(R.id.edit_date);
		
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
				String amount = amountField.getText().toString();
				boolean dataCorrect = false;
				
				if(amount.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					DatabaseManager.increamentBankBalance(bankNo, amount);
					DatabaseManager.increamentIncome(amount);
					DatabaseManager.increamentNumTransations();
					DatabaseManager.addDate(dateField.getText().toString());
					DatabaseManager.addType("Income");
					DatabaseManager.addParticular(DatabaseManager.getBankName(bankNo) + " Credit");
					DatabaseManager.addRate(amount);
					DatabaseManager.addQuantity(1);
					DatabaseManager.addAmount(amount);
				}
				else
				{
					buildBankCreditDialog();
					amountField.setText(amount);
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
		amountField=(EditText)bankDebitDialogView.findViewById(R.id.edit_amount);
		dateField=(EditText)bankDebitDialogView.findViewById(R.id.edit_date);
		Calendar calendar=Calendar.getInstance();
		String date=calendar.get(Calendar.DATE)+"/"+(calendar.get(Calendar.MONTH)+1)+"/"+calendar.get(Calendar.YEAR);
		dateField.setText(date);
		
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
				String amount = amountField.getText().toString();
				boolean dataCorrect = false;
				
				if(amount.length()==0)
				{
					Toast.makeText(getApplicationContext(), "Please Enter The Amount", Toast.LENGTH_LONG).show();
					dataCorrect = false;
				}
				else
				{
					dataCorrect = true;
				}
				
				if(dataCorrect)
				{
					DatabaseManager.decreamentBankBalance(bankNo, amountField.getText().toString());
					DatabaseManager.increamentWalletBalance(amountField.getText().toString());
					DatabaseManager.increamentNumTransations();
					DatabaseManager.addDate(dateField.getText().toString());
					DatabaseManager.addType("Bank Debit");
					DatabaseManager.addParticular(DatabaseManager.getBankName(bankNo) + " Withdrawal");
					DatabaseManager.addRate(0);
					DatabaseManager.addQuantity(0);
					DatabaseManager.addAmount(amountField.getText().toString());
				}
				else
				{
					buildBankDebitDialog();
					amountField.setText(amount);
					bankDebitDialog.show();
				}
				
				buildBodyLayout();
			}
		});
		bankDebitDialog.setNegativeButton("Cancel", null);
	}
}
