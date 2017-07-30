package com.chaturvedi.expenditurelist;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class EditBanksActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_RIGHT_WALLET_LAYOUT;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_BOTTOM_PARENT_LAYOUT;
	private int WIDTH_NAME_FIELDS;
	private int WIDTH_AMOUNT_FIELDS;
	private int WIDTH_SMS_FIELDS;
	private int WIDTH_REMOVE_BUTTON;
	private int MARGIN_TOP_FIELDS;
	
	private LinearLayout parentLayout;
	private LayoutParams parentLayoutParams;
	private ArrayList<LinearLayout> linearLayouts;
	private ArrayList<LayoutParams> linearLayoutParams;
	private TextView walletView;
	private LayoutParams walletViewParams;
	private EditText walletField;
	private LayoutParams walletFieldParams;
	private ArrayList<EditText> bankNameFields;
	private ArrayList<LayoutParams> bankNameFieldParams;
	private ArrayList<EditText> bankBalanceFields;
	private ArrayList<LayoutParams> bankBalanceFieldParams;
	private ArrayList<EditText> bankSmsNameFields;
	private ArrayList<LayoutParams> bankSmsNameFieldParams;
	private ArrayList<ImageButton> removeButtons;
	private ArrayList<LayoutParams> removeButtonParams;
	private Button addBankButton;

	private double walletBalance;
	private int numBanks=0;
	private ArrayList<String> bankNames;
	private ArrayList<Double> bankBalances;
	private ArrayList<String> bankSmsNames;
	
	private boolean dataEntered=true;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_edit_banks);
		}
		else
		{
			setContentView(R.layout.activity_edit_banks);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_RIGHT_WALLET_LAYOUT=screenWidth*2/100;
		MARGIN_TOP_PARENT_LAYOUT=screenHeight*5/100;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*3/100;
		MARGIN_BOTTOM_PARENT_LAYOUT=20;
		WIDTH_NAME_FIELDS=screenWidth*40/100;
		WIDTH_AMOUNT_FIELDS=screenWidth*25/100;
		WIDTH_SMS_FIELDS=screenWidth*20/100;
		WIDTH_REMOVE_BUTTON=screenWidth*10/100;
		MARGIN_TOP_FIELDS=5;
		
		buildLayout();
		setData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		if(VERSION.SDK_INT>10)
		{
			getMenuInflater().inflate(R.menu.activity_edit, menu);
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.save:
				saveToDatabase();
				if(dataEntered)
				{
					finish();
				}
		}
		return true;
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

	private void buildLayout()
	{
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, 0, MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		LinearLayout walletLayout = (LinearLayout)findViewById(R.id.wallet_layout);
		LayoutParams walletLayoutParams = (LayoutParams)walletLayout.getLayoutParams();
		walletLayoutParams.setMargins(0, 0, MARGIN_RIGHT_WALLET_LAYOUT, 0);
		walletLayout.setLayoutParams(walletLayoutParams);
		
		walletView=(TextView)findViewById(R.id.wallet_view);
		walletViewParams=new LayoutParams(WIDTH_NAME_FIELDS-20, LayoutParams.WRAP_CONTENT);
		walletViewParams.setMargins(20, 0, 0, 0);
		walletView.setLayoutParams(walletViewParams);
		walletField=(EditText)findViewById(R.id.wallet_field);
		walletFieldParams=new LayoutParams(WIDTH_AMOUNT_FIELDS, LayoutParams.WRAP_CONTENT);
		walletFieldParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		walletField.setLayoutParams(walletFieldParams);
		
		addBankButton=(Button)findViewById(R.id.add_bank);
		addBankButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				addBank();
			}
		});

		linearLayouts=new ArrayList<LinearLayout>(numBanks+1);
		linearLayoutParams=new ArrayList<LayoutParams>(numBanks+1);
		bankNameFields=new ArrayList<EditText>(numBanks);
		bankNameFieldParams=new ArrayList<LayoutParams>(numBanks);
		bankBalanceFields=new ArrayList<EditText>(numBanks);
		bankBalanceFieldParams=new ArrayList<LayoutParams>(numBanks);
		bankSmsNameFields=new ArrayList<EditText>(numBanks);
		bankSmsNameFieldParams=new ArrayList<LayoutParams>(numBanks);
		removeButtons=new ArrayList<ImageButton>(numBanks);
		removeButtonParams=new ArrayList<LayoutParams>(numBanks);
		
		if(VERSION.SDK_INT<=10)
		{
			Button nextButton = (Button)findViewById(R.id.button_save);
			nextButton.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v)
				{
					saveToDatabase();
					if(dataEntered)
					{
						finish();
					}
				}
			});
		}
	}
	
	private void setData()
	{
		DecimalFormat formatter = new DecimalFormat("###0");
		walletField.setText(formatter.format(DatabaseManager.getWalletBalance()));
		int numBanks = DatabaseManager.getNumBanks();
		ArrayList<String> bankNames = DatabaseManager.getBankNames();
		ArrayList<Double> bankBalances = DatabaseManager.getBankBalances();
		ArrayList<String> bankSmsNames = DatabaseManager.getBankSmsNames();
		for(int i=0; i<numBanks; i++)
		{
			addBank();
			bankNameFields.get(i).setText(bankNames.get(i));
			bankBalanceFields.get(i).setText(formatter.format(bankBalances.get(i)));
			bankSmsNameFields.get(i).setText(bankSmsNames.get(i));
		}
	}

	protected void addBank()
	{
		LayoutInflater layoutInflater=LayoutInflater.from(this);
		final LinearLayout layout=(LinearLayout)layoutInflater.inflate(R.layout.layout_add_bank, null);
		if(numBanks%2==0)
			layout.setBackgroundColor(Color.parseColor("#FF88FF88"));
		else
			layout.setBackgroundColor(Color.parseColor("#FF00FFFF"));
		linearLayouts.add(layout);
		linearLayoutParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		bankNameFields.add((EditText)layout.findViewById(R.id.bankName));
		LayoutParams layoutParams=new LayoutParams(WIDTH_NAME_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankNameFieldParams.add(layoutParams);
		bankNameFields.get(numBanks).setLayoutParams(layoutParams);

		bankBalanceFields.add((EditText)layout.findViewById(R.id.bankBalance));
		layoutParams=new LayoutParams(WIDTH_AMOUNT_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankBalanceFieldParams.add(layoutParams);
		bankBalanceFields.get(numBanks).setLayoutParams(layoutParams);
		
		bankSmsNameFields.add((EditText)layout.findViewById(R.id.bankSmsName));
		layoutParams=new LayoutParams(WIDTH_SMS_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankSmsNameFieldParams.add(layoutParams);
		bankSmsNameFields.get(numBanks).setLayoutParams(layoutParams);
		
		ImageButton removeButton=(ImageButton)layout.findViewById(R.id.button_remove);
		LayoutParams removeParams = new LayoutParams(WIDTH_REMOVE_BUTTON, LayoutParams.WRAP_CONTENT);
		removeParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		removeButton.setLayoutParams(removeParams);
		removeButtonParams.add(removeParams);
		removeButtons.add(removeButton);
		removeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				removeBank(layout);
			}
		});
		
		parentLayout.addView(layout, linearLayoutParams.get(numBanks));
		numBanks++;
	}
	
	protected void removeBank(View view)
	{
		int bankNum=parentLayout.indexOfChild(view)-1;
		parentLayout.removeViewAt(bankNum+1);
		linearLayouts.remove(bankNum);
		linearLayoutParams.remove(bankNum);
		bankNameFields.remove(bankNum);
		bankNameFieldParams.remove(bankNum);
		bankBalanceFields.remove(bankNum);
		bankBalanceFieldParams.remove(bankNum);
		bankSmsNameFields.remove(bankNum);
		bankSmsNameFieldParams.remove(bankNum);
		removeButtons.remove(bankNum);
		removeButtonParams.remove(bankNum);
		numBanks--;
	}
	
	private void saveToDatabase()
	{
		dataEntered=true;
		if(walletField.getText().toString().length()!=0)
		{
			walletBalance=Double.parseDouble(walletField.getText().toString());
		}
		else
		{
			Toast.makeText(getApplicationContext(), "Enter The Amount In Your Wallet", Toast.LENGTH_LONG).show();
			dataEntered=false;
		}
		
		bankNames = new ArrayList<String>();
		bankBalances = new ArrayList<Double>();
		bankSmsNames = new ArrayList<String>();
		for(int i=0; i<numBanks; i++)
		{
			if(dataEntered && bankNameFields.get(i).getText().toString().length()!=0)
			{
				bankNames.add(bankNameFields.get(i).getText().toString());
			}
			else if(dataEntered)
			{
				Toast.makeText(getApplicationContext(), "Enter The Name Of Bank "+(i+1), Toast.LENGTH_LONG).show();
				dataEntered=false;
			}
			
			if(dataEntered && bankBalanceFields.get(i).getText().toString().length() != 0)
			{
				bankBalances.add(Double.parseDouble(bankBalanceFields.get(i).getText().toString()));
			}
			else if(dataEntered)
			{
				Toast.makeText(getApplicationContext(), "Enter The Balance In Bank "+(i+1), Toast.LENGTH_LONG).show();
				dataEntered=false;
			}
			
			if(dataEntered && bankSmsNameFields.get(i).getText().toString().length() != 0)
			{
				bankSmsNames.add(bankSmsNameFields.get(i).getText().toString().trim());
			}
			else if(dataEntered)
			{
				Toast.makeText(getApplicationContext(), "Enter The Sms Name Of Bank "+(i+1), Toast.LENGTH_LONG).show();
				dataEntered=false;
			}
		}
		
		if(dataEntered)
		{
			DatabaseManager.setWalletBalance(walletBalance);
			DatabaseManager.setAmountSpent(0);
			DatabaseManager.setIncome(0);
			DatabaseManager.setNumBanks(numBanks);
			DatabaseManager.setBankNames(bankNames);
			DatabaseManager.setBankBalances(bankBalances);
			DatabaseManager.setBankSmsNames(bankSmsNames);
		}
	}
}
