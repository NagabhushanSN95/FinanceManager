package com.chaturvedi.financemanager.edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.customviews.MyAutoCompleteTextView;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.datastructures.Bank;
import com.chaturvedi.financemanager.database.DatabaseAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class EditBanksActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_BOTTOM_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_BALANCE_VIEWS;
	private int MARGIN_TOP_VIEWS;
	
	private LinearLayout parentLayout;
	private LayoutParams parentLayoutParams;
	private Button addBankButton;
	
	private AlertDialog.Builder addBankDialog;
	private MyAutoCompleteTextView bankNameField;
	private EditText bankAccNoField;
	private EditText bankBalanceField;
	private MyAutoCompleteTextView bankSmsNameField;

	private static ArrayList<Bank> banks;
	
	private ArrayList<String> bankNameSuggestions;
	private ArrayList<String> predictionSmsNames;
	private ArrayList<String> smsNameSuggestions;
	private int contextMenuBankNo;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_banks);
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=screenHeight*5/100;
		MARGIN_BOTTOM_PARENT_LAYOUT=20;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*3/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*3/100;
		WIDTH_NAME_VIEWS=screenWidth*60/100;
		WIDTH_BALANCE_VIEWS=screenWidth*30/100;
		MARGIN_TOP_VIEWS=5;
		
		readBankNames();
		buildLayout();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuBankNo = parentLayout.indexOfChild(view);
		menu.setHeaderTitle("Options For Bank "+(contextMenuBankNo+1));
		menu.add(0, view.getId(), 0, "Edit");
		menu.add(0, view.getId(), 0, "Delete");
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if(item.getTitle().equals("Edit"))
		{
			editBank(contextMenuBankNo);
			buildLayout();
		}
		else if(item.getTitle().equals("Delete"))
		{
			deleteBank(contextMenuBankNo);
			//buildLayout();
		}
		else
		{
			return false;
		}
		return true;
	}

	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT,
				MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		parentLayout.removeAllViews();
		
		addBankButton=(Button)findViewById(R.id.button_addBank);
		addBankButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildAddBankDialog();
				addBankDialog.show();
			}
		});

		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditBanksActivity.this);
		DecimalFormat formatter = new DecimalFormat("#,##0.##");
		int numBanks = databaseAdapter.getNumVisibleBanks();
		banks = databaseAdapter.getAllVisibleBanks();
		
		for(int i=0; i<numBanks; i++)
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_bank, null);
			TextView bankNameView = (TextView) layout.findViewById(R.id.bankName);
			TextView bankBalanceView = (TextView) layout.findViewById(R.id.bankBalance);
			
			bankNameView.setText(banks.get(i).getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			bankNameView.setLayoutParams(nameViewParams);
			
			bankBalanceView.setText(formatter.format(banks.get(i).getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_BALANCE_VIEWS, 0, 0, 0);
			bankBalanceView.setLayoutParams(balanceViewParams);
			bankBalanceView.setGravity(Gravity.END);
			
			parentLayout.addView(layout);
			registerForContextMenu(layout);
		}
	}
	
	private void buildAddBankDialog()
	{
		addBankDialog = new AlertDialog.Builder(this);
		addBankDialog.setTitle("Add A Bank Account");
		addBankDialog.setMessage("Enter The Particulars");
		LayoutInflater layoutInflater=LayoutInflater.from(this);
		final LinearLayout addBankLayout=(LinearLayout)layoutInflater.inflate(R.layout.dialog_add_bank, null);
		addBankDialog.setView(addBankLayout);
		
		bankNameField = (MyAutoCompleteTextView) addBankLayout.findViewById(R.id.bankName);
		ArrayAdapter<String> bankNameSuggestionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, bankNameSuggestions);
		bankNameField.setAdapter(bankNameSuggestionAdapter);
		bankNameField.setThreshold(1);
		
		bankAccNoField = (EditText) addBankLayout.findViewById(R.id.walletAccNo);
		bankBalanceField = (EditText) addBankLayout.findViewById(R.id.bankBalance);
		
		bankSmsNameField = (MyAutoCompleteTextView) addBankLayout.findViewById(R.id.bankSmsName);
		ArrayAdapter<String> bankSmsNameSuggestionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, smsNameSuggestions);
		bankSmsNameField.setAdapter(bankSmsNameSuggestionAdapter);
		bankSmsNameField.setThreshold(1);
		bankNameField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int bankNo=0;
				for(int i=0; i<bankNameSuggestions.size(); i++)
				{
					if(bankNameField.getText().toString().trim().equalsIgnoreCase(bankNameSuggestions.get(i)))
						bankNo=i;
				}
				bankSmsNameField.setText(predictionSmsNames.get(bankNo));
			}
		});
		
		addBankDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditBanksActivity.this);
				int id = databaseAdapter.getIDforNextBank();
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName, null);
				
				if(dataCorrect)
				{
					if(bankAccNo.length()==0)
						bankAccNo+="0000";
					
					Bank bank = new Bank(id, bankName, bankAccNo, Double.parseDouble(bankBalance), bankSmsName, false);
					databaseAdapter.addBank(bank);
				}
				else
				{
					buildAddBankDialog();
					bankNameField.setText(bankName+" ");
					bankAccNoField.setText(bankAccNo);
					bankBalanceField.setText(bankBalance);
					bankSmsNameField.setText(bankSmsName+" ");
					addBankDialog.show();
				}
				buildLayout();
			}
		});
	}
	
	private void readBankNames()
	{
		bankNameSuggestions=new ArrayList<String>();
		predictionSmsNames = new ArrayList<String>();
		smsNameSuggestions=new ArrayList<String>();
		InputStream bankStream;
		BufferedReader bankReader;
		String line;
		try
		{
			bankStream = getResources().openRawResource(R.raw.bank_names);
			bankReader = new BufferedReader(new InputStreamReader(bankStream));
			line=bankReader.readLine();
			while(line!=null)
			{
				bankNameSuggestions.add(line);
				line=bankReader.readLine();
				predictionSmsNames.add(line);
				line=bankReader.readLine();
			}
			
			bankStream = getResources().openRawResource(R.raw.bank_sms_names);
			bankReader = new BufferedReader(new InputStreamReader(bankStream));
			line=bankReader.readLine();
			while(line!=null)
			{
				smsNameSuggestions.add(line);
				line=bankReader.readLine();
			}
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void editBank(final int bankNo)
	{
		//ArrayList<Bank> banks = DatabaseManager.getAllBanks();
		final Bank bank = banks.get(bankNo);
		buildAddBankDialog();
		bankNameField.setText(bank.getName());
		bankAccNoField.setText(bank.getAccNo());
		bankBalanceField.setText(String.valueOf(bank.getBalance()));
		bankSmsNameField.setText(bank.getSmsName());
		
		addBankDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				int id = bank.getID();
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean isDeleted = bank.isDeleted();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName, null);
				
				if(dataCorrect)
				{
					if(bankAccNo.length()==0)
						bankAccNo+="0000";
					
					Bank bank = new Bank(id, bankName, bankAccNo, Double.parseDouble(bankBalance), bankSmsName, isDeleted);
					DatabaseAdapter.getInstance(EditBanksActivity.this).updateBank(bank);
					buildLayout();
				}
				else
				{
					buildAddBankDialog();
					bankNameField.setText(bankName+" ");
					bankAccNoField.setText(bankAccNo);
					bankBalanceField.setText(bankBalance);
					bankSmsNameField.setText(bankSmsName+" ");
					addBankDialog.show();
				}
			}
		});
		addBankDialog.show();
	}

	private void deleteBank(final int contextMenuBankNo)
	{
		final Bank bank = banks.get(contextMenuBankNo);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(EditBanksActivity.this);
		deleteDialog.setTitle("Delete Bank");
		deleteDialog.setMessage("Are you sure you want to delete bank '" + bank.getName() + "'?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter.getInstance(EditBanksActivity.this).deleteBank(bank.getID());
				banks.remove(contextMenuBankNo);
				parentLayout.removeViewAt(contextMenuBankNo);
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private boolean verifyData(String bankName, String bankBalance, String bankSmsName, String origBankName)
	{
		boolean dataCorrect;
		if(bankName.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Bank Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(bankBalance.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Bank Balance", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(bankSmsName.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Bank Sms Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(origBankName!=null && bankName.equals(origBankName))
		{
			dataCorrect = true;
		}
		else if(DatabaseAdapter.getInstance(EditBanksActivity.this).getBankFromName(bankName) != null)
		{
			Toast.makeText(getApplicationContext(), "Please Enter A Unique Bank Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
}
