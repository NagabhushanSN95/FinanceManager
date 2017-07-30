// Shree KRISHNAya Namaha
// Author: Nagabhushan S N
// Line 195

package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class BanksSetupActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_RIGHT_WALLET_LAYOUT;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_BOTTOM_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_ACC_NO_VIEWS;
	private int WIDTH_BALANCE_VIEWS;
	private int WIDTH_SMS_VIEWS;
	//private int WIDTH_REMOVE_BUTTON;
	private int MARGIN_TOP_VIEWS;
	
	private LinearLayout parentLayout;
	private LayoutParams parentLayoutParams;
	/*private ArrayList<LinearLayout> linearLayouts;
	private ArrayList<LayoutParams> linearLayoutParams;*/
	private TextView walletView;
	private LayoutParams walletViewParams;
	private EditText walletField;
	private LayoutParams walletFieldParams;
	/*private ArrayList<TextView> bankNameViews;
	private ArrayList<LayoutParams> bankNameViewParams;
	private ArrayList<TextView> bankAccNoViews;
	private ArrayList<LayoutParams> bankAccNoViewParams;
	private ArrayList<TextView> bankBalanceViews;
	private ArrayList<LayoutParams> bankBalanceViewParams;
	private ArrayList<TextView> bankSmsNameViews;
	private ArrayList<LayoutParams> bankSmsNameViewParams;
	private ArrayList<ImageButton> removeButtons;
	private ArrayList<LayoutParams> removeButtonParams;*/
	private Button addBankButton;
	
	private AlertDialog.Builder addBankDialog;
	private AutoCompleteTextView bankNameField;
	private EditText bankAccNoField;
	private EditText bankBalanceField;
	private AutoCompleteTextView bankSmsNameField;

	private double walletBalance;
	private int numBanks=0;
	private ArrayList<String> bankNames;
	private ArrayList<String> bankAccNos;
	private ArrayList<Double> bankBalances;
	private ArrayList<String> bankSmsNames;
	
	private Intent expendituresSetupIntent;
	private ArrayList<String> bankNameSuggestions;
	private ArrayList<String> predictionSmsNames;
	private ArrayList<String> smsNameSuggestions;
	private int contextMenuBankNo;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_setup_banks);
		}
		else
		{
			setContentView(R.layout.activity_setup_banks);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_RIGHT_WALLET_LAYOUT=screenWidth*2/100;
		MARGIN_TOP_PARENT_LAYOUT=screenHeight*5/100;
		MARGIN_BOTTOM_PARENT_LAYOUT=20;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*3/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*3/100;
		WIDTH_NAME_VIEWS=screenWidth*40/100;
		WIDTH_ACC_NO_VIEWS=screenWidth*20/100;
		WIDTH_BALANCE_VIEWS=screenWidth*15/100;
		WIDTH_SMS_VIEWS=screenWidth*15/100;
		//WIDTH_REMOVE_BUTTON=screenWidth*10/100;
		MARGIN_TOP_VIEWS=5;
		
		initializeFields();
		readBankNames();
		buildLayout();
		expendituresSetupIntent=new Intent(this, ExpenditureSetupActivity.class);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		if(VERSION.SDK_INT>10)
		{
			getMenuInflater().inflate(R.menu.activity_startup_banks, menu);
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.next:
				saveToDatabase();
		}
		return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuBankNo = parentLayout.indexOfChild(view)-1;		// Wallet View is the first view
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
			buildLayout();
		}
		else
		{
			return false;
		}
		return true;
	}

	private void initializeFields()
	{
		bankNames = new ArrayList<String>();
		bankAccNos = new ArrayList<String>();
		bankBalances = new ArrayList<Double>();
		bankSmsNames = new ArrayList<String>();
	}

	private void buildLayout()
	{
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT, MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		LinearLayout walletLayout = (LinearLayout)findViewById(R.id.walletLayout);
		LayoutParams walletLayoutParams = (LayoutParams)walletLayout.getLayoutParams();
		walletLayoutParams.setMargins(0, 0, MARGIN_RIGHT_WALLET_LAYOUT, 0);
		walletLayout.setLayoutParams(walletLayoutParams);
		
		parentLayout.removeAllViews();
		parentLayout.addView(walletLayout);
		
		walletView=(TextView)findViewById(R.id.wallet_view);
		walletViewParams=new LayoutParams(WIDTH_NAME_VIEWS-20, LayoutParams.WRAP_CONTENT);
		walletViewParams.setMargins(20, 0, 0, 0);
		walletView.setLayoutParams(walletViewParams);
		walletField=(EditText)findViewById(R.id.wallet_field);
		walletFieldParams=new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams.WRAP_CONTENT);
		walletFieldParams.setMargins(0, MARGIN_TOP_VIEWS, 0, 0);
		walletField.setLayoutParams(walletFieldParams);
		
		addBankButton=(Button)findViewById(R.id.add_bank);
		addBankButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildAddBankDialog();
				addBankDialog.show();
				//addBank();
			}
		});
		
		DecimalFormat formatter = new DecimalFormat("#,##0.##"); 

		/*linearLayouts=new ArrayList<LinearLayout>(numBanks+1);
		linearLayoutParams=new ArrayList<LayoutParams>(numBanks+1);
		bankNameViews=new ArrayList<TextView>(numBanks);
		bankNameViewParams=new ArrayList<LayoutParams>(numBanks);
		bankAccNoViews=new ArrayList<TextView>(numBanks);
		bankAccNoViewParams=new ArrayList<LayoutParams>(numBanks);
		bankBalanceViews=new ArrayList<TextView>(numBanks);
		bankBalanceViewParams=new ArrayList<LayoutParams>(numBanks);
		bankSmsNameViews=new ArrayList<TextView>(numBanks);
		bankSmsNameViewParams=new ArrayList<LayoutParams>(numBanks);
		removeButtons=new ArrayList<ImageButton>(numBanks);
		removeButtonParams=new ArrayList<LayoutParams>(numBanks);*/
		
		for(int i=0; i<numBanks; i++)
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_bank, null);
			TextView bankNameView = (TextView) layout.findViewById(R.id.bankName);
			TextView bankAccNoView = (TextView) layout.findViewById(R.id.bankAccNo);
			TextView bankBalanceView = (TextView) layout.findViewById(R.id.bankBalance);
			TextView bankSmsNameView = (TextView) layout.findViewById(R.id.bankSmsName);
			
			bankNameView.setText(bankNames.get(i));
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			bankNameView.setLayoutParams(nameViewParams);
			
			bankAccNoView.setText(bankAccNos.get(i).toString());
			LayoutParams accNoViewParams = new LayoutParams(WIDTH_ACC_NO_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_ACC_NO_VIEWS, 0, 0, 0);
			bankAccNoView.setLayoutParams(accNoViewParams);
			bankAccNoView.setGravity(Gravity.CENTER);
			
			bankBalanceView.setText(formatter.format(bankBalances.get(i)));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_BALANCE_VIEWS, 0, 0, 0);
			bankBalanceView.setLayoutParams(balanceViewParams);
			bankBalanceView.setGravity(Gravity.RIGHT);
			
			bankSmsNameView.setText(bankSmsNames.get(i));
			LayoutParams smsViewParams = new LayoutParams(WIDTH_SMS_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_SMS_VIEWS, 0, 0, 0);
			bankSmsNameView.setLayoutParams(smsViewParams);
			
			if(i%2==0)
				layout.setBackgroundColor(Color.parseColor("#88CC00CC"));
			else
				layout.setBackgroundColor(Color.parseColor("#880044FF"));
			
			parentLayout.addView(layout);
			registerForContextMenu(layout);
		}
		
		if(VERSION.SDK_INT<=10)
		{
			Button nextButton = (Button)findViewById(R.id.button_next);
			nextButton.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v)
				{
					saveToDatabase();
				}
			});
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
		
		bankNameField = (AutoCompleteTextView) addBankLayout.findViewById(R.id.bankName);
		ArrayAdapter<String> bankNameSuggestionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, bankNameSuggestions);
		bankNameField.setAdapter(bankNameSuggestionAdapter);
		bankNameField.setThreshold(1);
		
		bankAccNoField = (EditText) addBankLayout.findViewById(R.id.bankAccNo);
		bankBalanceField = (EditText) addBankLayout.findViewById(R.id.bankBalance);
		
		bankSmsNameField = (AutoCompleteTextView) addBankLayout.findViewById(R.id.bankSmsName);
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
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName);
				
				/*boolean dataCorrect = false;
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
				else
				{
					dataCorrect = true;
				}*/
				
				if(dataCorrect)
				{
					if(bankAccNo.length()==0)
						bankAccNo+="0000";
					
					numBanks++;
					bankNames.add(bankName);
					bankAccNos.add(bankAccNo);
					bankBalances.add(Double.parseDouble(bankBalance));
					bankSmsNames.add(bankSmsName);
					
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
				buildLayout();
			}
		});
	}

	/*protected void addBank()
	{
		buildAddBankDialog();
		addBankDialog.show();
		/*if(numBanks%2==0)
			addBankLayout.setBackgroundColor(Color.parseColor("#FF88FF88"));
		else
			addBankLayout.setBackgroundColor(Color.parseColor("#FF00FFFF"));
		linearLayouts.add(addBankLayout);
		linearLayoutParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		/*AutoCompleteTextView bankNameField = (AutoCompleteTextView) layout.findViewById(R.id.bankName);
		String[] bankNameSuggestions = new String[]{"State Bank Of India", "Union Bank Of India"};
		ArrayAdapter<String> bankNameSuggestionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, bankNameSuggestions);
		bankNameField.setAdapter(bankNameSuggestionAdapter);
		bankNameField.setThreshold(1);
		LayoutParams layoutParams=new LayoutParams(WIDTH_NAME_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankNameField.setLayoutParams(layoutParams);
		bankNameFields.add(bankNameField);
		bankNameFieldParams.add(layoutParams);*/
		
		/*bankNameFields.add((EditText)addBankLayout.findViewById(R.id.bankName));
		LayoutParams layoutParams=new LayoutParams(WIDTH_NAME_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankNameFieldParams.add(layoutParams);
		bankNameFields.get(numBanks).setLayoutParams(layoutParams);

		bankBalanceFields.add((EditText)addBankLayout.findViewById(R.id.bankBalance));
		layoutParams=new LayoutParams(WIDTH_AMOUNT_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankBalanceFieldParams.add(layoutParams);
		bankBalanceFields.get(numBanks).setLayoutParams(layoutParams);
		
		bankSmsNameFields.add((EditText)addBankLayout.findViewById(R.id.bankSmsName));
		layoutParams=new LayoutParams(WIDTH_SMS_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, MARGIN_TOP_FIELDS, 0, 0);
		bankSmsNameFieldParams.add(layoutParams);
		bankSmsNameFields.get(numBanks).setLayoutParams(layoutParams);
		
		ImageButton removeButton=(ImageButton)addBankLayout.findViewById(R.id.button_remove);
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
				removeBank(addBankLayout);
			}
		});
		
		parentLayout.addView(addBankLayout, linearLayoutParams.get(numBanks));
		numBanks++; * /
	}*/
	
	/*protected void removeBank(View view)
	{
		int bankNum=parentLayout.indexOfChild(view)-1;
		parentLayout.removeViewAt(bankNum+1);
		linearLayouts.remove(bankNum);
		linearLayoutParams.remove(bankNum);
		bankNameViews.remove(bankNum);
		bankNameViewParams.remove(bankNum);
		bankAccNoViews.remove(bankNum);
		bankAccNoViewParams.remove(bankNum);
		bankBalanceViews.remove(bankNum);
		bankBalanceViewParams.remove(bankNum);
		bankSmsNameViews.remove(bankNum);
		bankSmsNameViewParams.remove(bankNum);
		removeButtons.remove(bankNum);
		removeButtonParams.remove(bankNum);
		numBanks--;
	}*/
	
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
	
	private void saveToDatabase()
	{
		boolean dataEntered=true;
		if(walletField.getText().toString().length()!=0)
		{
			walletBalance=Double.parseDouble(walletField.getText().toString());
		}
		else
		{
			Toast.makeText(getApplicationContext(), "Enter The Amount In Your Wallet", Toast.LENGTH_LONG).show();
			dataEntered=false;
		}
		
		if(dataEntered)
		{
			DatabaseManager.setWalletBalance(walletBalance);
			DatabaseManager.setAmountSpent(0);
			DatabaseManager.setIncome(0);
			DatabaseManager.setNumBanks(numBanks);
			DatabaseManager.setBankNames(bankNames);
			DatabaseManager.setAllBankAccNos(bankAccNos);
			DatabaseManager.setBankBalances(bankBalances);
			DatabaseManager.setBankSmsNames(bankSmsNames);
			
			// Start The Next Activity And Finish This Activity
			startActivity(expendituresSetupIntent);
			finish();
		}
	}
	
	private void deleteBank(int bankNo)
	{
		// Remove Bank Details
		bankNames.remove(bankNo);
		bankAccNos.remove(bankNo);
		bankBalances.remove(bankNo);
		bankSmsNames.remove(bankNo);
		numBanks--;
	}
	
	private void editBank(final int bankNo)
	{
		buildAddBankDialog();
		bankNameField.setText(bankNames.get(bankNo));
		bankAccNoField.setText(""+bankAccNos.get(bankNo));
		bankBalanceField.setText(""+bankBalances.get(bankNo));
		bankSmsNameField.setText(bankSmsNames.get(bankNo));
		
		addBankDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName);
				
				/* booloean dataCorrect = false;
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
				else
				{
					dataCorrect = true;
				}*/
				
				if(dataCorrect)
				{
					if(bankAccNo.length()==0)
						bankAccNo+="0000";
					
					bankNames.set(bankNo, bankName);
					bankAccNos.set(bankNo, bankAccNo);
					bankBalances.set(bankNo, Double.parseDouble(bankBalance));
					bankSmsNames.set(bankNo, bankSmsName);
					
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
				buildLayout();
			}
		});
		addBankDialog.show();
	}
	
	private boolean verifyData(String bankName, String bankBalance, String bankSmsName)
	{
		boolean dataCorrect = false;
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
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
}
