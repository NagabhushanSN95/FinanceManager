package com.chaturvedi.financemanager.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import com.chaturvedi.customviews.MyAutoCompleteTextView;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Bank;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditBanksActivity extends Activity
{
	public static final int ID_EDIT_BANK = 1101;
	public static final int ID_DELETE_BANK = 1102;
	private static final int ID_RESTORE_BANK = 1103;

	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_BOTTOM_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_BALANCE_VIEWS;
	
	private LinearLayout activeBanksLayout;
	private LinearLayout deletedBanksLayout;
	
	private AlertDialog.Builder addBankDialog;
	private MyAutoCompleteTextView bankNameField;
	private EditText bankAccNoField;
	private EditText bankBalanceField;
	private MyAutoCompleteTextView bankSmsNameField;
	
	private ArrayList<String> bankNameSuggestions;
	private ArrayList<String> predictionSmsNames;
	private ArrayList<String> smsNameSuggestions;
	private boolean showDeletedBanks = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_banks);
		// Provide Up Button in Action Bar
		if (getActionBar() != null)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		calculateDimensions();
		readBankNames();
		buildLayout();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds childItems to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_edit_banks, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_toggleDisplayDeletedBanks:
				showDeletedBanks = !showDeletedBanks;
				if (showDeletedBanks)
				{
					item.setTitle("Hide Deleted Banks");
					deletedBanksLayout.setVisibility(View.VISIBLE);
				}
				else
				{
					item.setTitle("Show Deleted Banks");
					deletedBanksLayout.setVisibility(View.GONE);
				}
				return true;
			
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		String bankName = ((TextView) view.findViewById(R.id.bankName)).getText().toString();
		Bank bank = DatabaseAdapter.getInstance(this).getBankFromName(bankName);
		menu.setHeaderTitle("Options");
		if (!bank.isDeleted())
		{
			menu.add(Menu.NONE, ID_EDIT_BANK, Menu.NONE, "Edit \"" + bankName + "\"");
			menu.add(Menu.NONE, ID_DELETE_BANK, Menu.NONE, "Delete \"" + bankName + "\"");
		}
		else
		{
			menu.add(Menu.NONE, ID_RESTORE_BANK, Menu.NONE, "Restore \"" + bankName + "\"");
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		int bankId = getBankIdFromMenuItem(item);
		if (bankId != -1)
		{
			switch (item.getItemId())
			{
				case ID_EDIT_BANK:
					editBank(bankId);
					rebuildLayout();
					break;
				
				case ID_DELETE_BANK:
					deleteBank(bankId);
					break;
				
				case ID_RESTORE_BANK:
					restoreBank(bankId);
					break;
				
				default:
					return super.onContextItemSelected(item);
			}
		}
		else
		{
			Toast.makeText(this, "Unable to detect selected Bank. Please try again. Please " +
					"contact" +
					" developer if the problem persists", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private void calculateDimensions()
	{
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int screenWidth = displayMetrics.widthPixels;
		int screenHeight = displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT = screenHeight * 5 / 100;
		MARGIN_BOTTOM_PARENT_LAYOUT = 20;
		MARGIN_LEFT_PARENT_LAYOUT = screenWidth * 3 / 100;
		MARGIN_RIGHT_PARENT_LAYOUT = screenWidth * 3 / 100;
		WIDTH_NAME_VIEWS = screenWidth * 60 / 100;
		WIDTH_BALANCE_VIEWS = screenWidth * 30 / 100;
	}
	
	private void rebuildLayout()
	{
		activeBanksLayout.removeAllViews();
		deletedBanksLayout.removeAllViews();
		buildLayout();
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if ((this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		LinearLayout parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		LayoutParams parentLayoutParams = (LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT,
				MARGIN_RIGHT_PARENT_LAYOUT,
				MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		Button addBankButton = (Button) findViewById(R.id.button_addBank);
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
		
		activeBanksLayout = (LinearLayout) findViewById(R.id.activeBanksLayout);
		for (Bank bank : databaseAdapter.getAllVisibleBanks())
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout
					.layout_display_bank, activeBanksLayout, false);
			TextView bankNameView = (TextView) layout.findViewById(R.id.bankName);
			TextView bankBalanceView = (TextView) layout.findViewById(R.id.bankBalance);
			
			bankNameView.setText(bank.getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams
					.WRAP_CONTENT);
			bankNameView.setLayoutParams(nameViewParams);
			
			bankBalanceView.setText(formatter.format(bank.getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams
					.WRAP_CONTENT);
			bankBalanceView.setLayoutParams(balanceViewParams);
			bankBalanceView.setGravity(Gravity.END);
			
			activeBanksLayout.addView(layout);
			registerForContextMenu(layout);
		}
		
		deletedBanksLayout = (LinearLayout) findViewById(R.id.deletedBanksLayout);
		for (Bank bank : databaseAdapter.getAllDeletedBanks())
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout
					.layout_display_bank, activeBanksLayout, false);
			TextView bankNameView = (TextView) layout.findViewById(R.id.bankName);
			TextView bankBalanceView = (TextView) layout.findViewById(R.id.bankBalance);
			
			bankNameView.setText(bank.getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams
					.WRAP_CONTENT);
			bankNameView.setLayoutParams(nameViewParams);
			
			bankBalanceView.setText(formatter.format(bank.getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams
					.WRAP_CONTENT);
			bankBalanceView.setLayoutParams(balanceViewParams);
			bankBalanceView.setGravity(Gravity.END);
			
			deletedBanksLayout.addView(layout);
			registerForContextMenu(layout);
		}
	}
	
	private void buildAddBankDialog()
	{
		addBankDialog = new AlertDialog.Builder(this);
		addBankDialog.setTitle("Add A New Bank Account");
		addBankDialog.setMessage("Enter The Particulars");
		LayoutInflater layoutInflater = LayoutInflater.from(this);
		@SuppressLint("InflateParams")
		// For passing null to inflate method. Justified here, because there is no parent view
		final LinearLayout addBankLayout = (LinearLayout) layoutInflater.inflate(R.layout
				.dialog_add_bank, null);
		addBankDialog.setView(addBankLayout);
		
		bankNameField = (MyAutoCompleteTextView) addBankLayout.findViewById(R.id.bankName);
		ArrayAdapter<String> bankNameSuggestionAdapter = new ArrayAdapter<>(this, android.R
				.layout.simple_dropdown_item_1line, bankNameSuggestions);
		bankNameField.setAdapter(bankNameSuggestionAdapter);
		bankNameField.setThreshold(1);
		
		bankAccNoField = (EditText) addBankLayout.findViewById(R.id.walletAccNo);
		bankBalanceField = (EditText) addBankLayout.findViewById(R.id.bankBalance);
		
		bankSmsNameField = (MyAutoCompleteTextView) addBankLayout.findViewById(R.id.bankSmsName);
		ArrayAdapter<String> bankSmsNameSuggestionAdapter = new ArrayAdapter<>(this, android
				.R.layout.simple_dropdown_item_1line, smsNameSuggestions);
		bankSmsNameField.setAdapter(bankSmsNameSuggestionAdapter);
		bankSmsNameField.setThreshold(1);
		bankNameField.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				int bankNo = 0;
				for (int i = 0; i < bankNameSuggestions.size(); i++)
				{
					if (bankNameField.getText().toString().trim().equalsIgnoreCase
							(bankNameSuggestions.get(i)))
					{
						bankNo = i;
					}
				}
				bankSmsNameField.setText(predictionSmsNames.get(bankNo));
			}
		});
		
		addBankDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditBanksActivity
						.this);
				int id = databaseAdapter.getIDforNextBank();
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName, null);
				
				if (dataCorrect)
				{
					if (bankAccNo.length() == 0)
					{
						bankAccNo += "0000";
					}
					
					Bank bank = new Bank(id, bankName, bankAccNo, Double.parseDouble(bankBalance),
							bankSmsName, false);
					databaseAdapter.addBank(bank);
				}
				else
				{
					buildAddBankDialog();
					bankNameField.setText(bankName);
					bankAccNoField.setText(bankAccNo);
					bankBalanceField.setText(bankBalance);
					bankSmsNameField.setText(bankSmsName);
					addBankDialog.show();
				}
				rebuildLayout();
			}
		});
	}
	
	private void readBankNames()
	{
		bankNameSuggestions = new ArrayList<>();
		predictionSmsNames = new ArrayList<>();
		smsNameSuggestions = new ArrayList<>();
		InputStream bankStream;
		BufferedReader bankReader;
		String line;
		try
		{
			bankStream = getResources().openRawResource(R.raw.bank_names);
			bankReader = new BufferedReader(new InputStreamReader(bankStream));
			line = bankReader.readLine();
			while (line != null)
			{
				bankNameSuggestions.add(line);
				line = bankReader.readLine();
				predictionSmsNames.add(line);
				line = bankReader.readLine();
			}
			
			bankStream = getResources().openRawResource(R.raw.bank_sms_names);
			bankReader = new BufferedReader(new InputStreamReader(bankStream));
			line = bankReader.readLine();
			while (line != null)
			{
				smsNameSuggestions.add(line);
				line = bankReader.readLine();
			}
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void editBank(int bankId)
	{
		final Bank bank = DatabaseAdapter.getInstance(this).getBank(bankId);
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
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName, bank.getName
						());
				
				if (dataCorrect)
				{
					if (bankAccNo.length() == 0)
					{
						bankAccNo += "0000";
					}
					
					Bank bank = new Bank(id, bankName, bankAccNo, Double.parseDouble(bankBalance),
							bankSmsName, isDeleted);
					DatabaseAdapter.getInstance(EditBanksActivity.this).updateBank(bank);
					rebuildLayout();
				}
				else
				{
					buildAddBankDialog();
					bankNameField.setText(bankName);
					bankAccNoField.setText(bankAccNo);
					bankBalanceField.setText(bankBalance);
					bankSmsNameField.setText(bankSmsName);
					addBankDialog.show();
				}
			}
		});
		addBankDialog.show();
	}
	
	private void deleteBank(int bankId)
	{
		final Bank bank = DatabaseAdapter.getInstance(this).getBank(bankId);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(EditBanksActivity.this);
		deleteDialog.setTitle("Delete Bank");
		deleteDialog.setMessage("Are you sure you want to delete bank '" + bank.getName() + "'?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter.getInstance(EditBanksActivity.this).deleteBank(bank.getID());
				rebuildLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private void restoreBank(int bankId)
	{
		final Bank bank = DatabaseAdapter.getInstance(this).getBank(bankId);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(EditBanksActivity.this);
		deleteDialog.setTitle("Restore Bank");
		deleteDialog.setMessage("Are you sure you want to restore bank '" + bank.getName() + "'?");
		deleteDialog.setPositiveButton("Restore", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter.getInstance(EditBanksActivity.this).restoreBank(bank.getID());
				rebuildLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private boolean verifyData(String bankName, String bankBalance, String bankSmsName, String
			origBankName)
	{
		boolean dataCorrect;
		if (bankName.length() == 0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Bank Name", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if (bankBalance.length() == 0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Bank Balance", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if (bankSmsName.length() == 0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Bank Sms Name", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if (origBankName != null && bankName.equals(origBankName))
		{
			dataCorrect = true;
		}
		else if (DatabaseAdapter.getInstance(EditBanksActivity.this).getBankFromName(bankName) !=
				null)
		{
			Toast.makeText(getApplicationContext(), "Please Enter A Unique Bank Name", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
	
	private int getBankIdFromMenuItem(MenuItem menuItem)
	{
		int bankId = -1;
		String title = menuItem.getTitle().toString();
		Matcher bankNameMatcher = Pattern.compile(".+\"(.+)\"").matcher(title);
		if (bankNameMatcher.find())
		{
			String bankName = bankNameMatcher.group(1);
			bankId = DatabaseAdapter.getInstance(this).getBankFromName(bankName).getID();
		}
		return bankId;
	}
}
