package com.chaturvedi.financemanager.setup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.R.id;
import com.chaturvedi.financemanager.R.layout;
import com.chaturvedi.financemanager.R.raw;
import com.chaturvedi.financemanager.database.Bank;

public class BanksSetupFragment extends Fragment
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
	
	private Context context;
	private View banksSetupView;
	
	private LinearLayout parentLayout;
	private LayoutParams parentLayoutParams;
	private Button addBankButton;
	
	private AlertDialog.Builder addBankDialog;
	private AutoCompleteTextView bankNameField;
	private EditText bankAccNoField;
	private EditText bankBalanceField;
	private AutoCompleteTextView bankSmsNameField;

	private static ArrayList<Bank> banks;
	
	private ArrayList<String> bankNameSuggestions;
	private ArrayList<String> predictionSmsNames;
	private ArrayList<String> smsNameSuggestions;
	private int contextMenuBankNo;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		banksSetupView = inflater.inflate(R.layout.fragment_setup_banks, container, false);
		getActivity().setTheme(android.R.style.Theme);
		
		displayMetrics=new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=screenHeight*5/100;
		MARGIN_BOTTOM_PARENT_LAYOUT=20;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*3/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*3/100;
		WIDTH_NAME_VIEWS=screenWidth*60/100;
		WIDTH_BALANCE_VIEWS=screenWidth*30/100;
		MARGIN_TOP_VIEWS=5;
		
		banks = new ArrayList<Bank>();
		readBankNames();
		buildLayout();
		return banksSetupView;
	}
	
	public static BanksSetupFragment newInstance()//(int screenWidth, int screenHeight)
	{
		BanksSetupFragment f = new BanksSetupFragment();
		/*Bundle bundle = new Bundle();
		bundle.putInt("ScreenWidth", screenWidth);
		bundle.putInt("ScreenHeight", screenHeight);
		f.setArguments(bundle);*/
		return f;
	}
	
	public static ArrayList<Bank> getAllBanks()
	{
		return banks;
		
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
			buildLayout();
		}
		else
		{
			return false;
		}
		return true;
	}

	private void buildLayout()
	{
		parentLayout=(LinearLayout) banksSetupView.findViewById(R.id.parentLayout);
		LinearLayout.LayoutParams parentLayoutParams=(LinearLayout.LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT, MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		parentLayout.removeAllViews();
		
		addBankButton=(Button)banksSetupView.findViewById(R.id.button_addBank);
		addBankButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildAddBankDialog();
				addBankDialog.show();
			}
		});
		
		DecimalFormat formatter = new DecimalFormat("#,##0.##");
		
		for(int i=0; i<banks.size(); i++)
		{
			final String bankName    = banks.get(i).getName();
			final String bankAccNo   = banks.get(i).getAccNo();
			final double bankBalance = banks.get(i).getBalance();
			final String bankSmsName = banks.get(i).getSmsName();
			
			LayoutInflater layoutInflater = LayoutInflater.from(getActivity().getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_bank, null);
			TextView bankNameView = (TextView) layout.findViewById(R.id.bankName);
			//TextView bankAccNoView = (TextView) layout.findViewById(R.id.bankAccNo);
			TextView bankBalanceView = (TextView) layout.findViewById(R.id.bankBalance);
			//TextView bankSmsNameView = (TextView) layout.findViewById(R.id.bankSmsName);
			
			bankNameView.setText(banks.get(i).getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			bankNameView.setLayoutParams(nameViewParams);
			
			/*bankAccNoView.setText(banks.get(i).getAccNo().toString());
			LayoutParams accNoViewParams = new LayoutParams(WIDTH_ACC_NO_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_ACC_NO_VIEWS, 0, 0, 0);
			bankAccNoView.setLayoutParams(accNoViewParams);
			bankAccNoView.setGravity(Gravity.CENTER);*/
			
			bankBalanceView.setText(formatter.format(banks.get(i).getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_BALANCE_VIEWS, 0, 0, 0);
			bankBalanceView.setLayoutParams(balanceViewParams);
			bankBalanceView.setGravity(Gravity.RIGHT);
			
			/*bankSmsNameView.setText(banks.get(i).getSmsName());
			LayoutParams smsViewParams = new LayoutParams(WIDTH_SMS_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_SMS_VIEWS, 0, 0, 0);
			bankSmsNameView.setLayoutParams(smsViewParams);*/
			
			if(i%2==0)
				layout.setBackgroundColor(Color.parseColor("#88CC00CC"));
			else
				layout.setBackgroundColor(Color.parseColor("#880044FF"));
			
			parentLayout.addView(layout);
			registerForContextMenu(layout);
			
			//Display Bank Details when the Layout is Touched/Clicked
			layout.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					LayoutInflater inflater = LayoutInflater.from(getActivity());
					View bankDetailsView = inflater.inflate(R.layout.dialog_bank_details, null);
					TextView bankNameView = (TextView) bankDetailsView.findViewById(R.id.value_bankName);
					bankNameView.setText(bankName);
					TextView bankAccNoView = (TextView) bankDetailsView.findViewById(R.id.value_bankAccNo);
					bankAccNoView.setText(bankAccNo);
					TextView bankBalanceView = (TextView) bankDetailsView.findViewById(R.id.value_bankBalance);
					bankBalanceView.setText(""+bankBalance);
					TextView bankSmsNameView = (TextView) bankDetailsView.findViewById(R.id.value_bankSmsName);
					bankSmsNameView.setText(bankSmsName);
					
					AlertDialog.Builder bankDetailsBuilder = new AlertDialog.Builder(getActivity());
					bankDetailsBuilder.setTitle("Bank Details");
					bankDetailsBuilder.setView(bankDetailsView);
					bankDetailsBuilder.show();
				}
			});
		}
	}
	
	private void buildAddBankDialog()
	{
		addBankDialog = new AlertDialog.Builder(getActivity());
		addBankDialog.setTitle("Add A Bank Account");
		addBankDialog.setMessage("Enter The Particulars");
		LayoutInflater layoutInflater=LayoutInflater.from(getActivity());
		final LinearLayout addBankLayout=(LinearLayout)layoutInflater.inflate(R.layout.dialog_add_bank, null);
		addBankDialog.setView(addBankLayout);
		
		bankNameField = (AutoCompleteTextView) addBankLayout.findViewById(R.id.bankName);
		ArrayAdapter<String> bankNameSuggestionAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, bankNameSuggestions);
		bankNameField.setAdapter(bankNameSuggestionAdapter);
		bankNameField.setThreshold(1);
		
		bankAccNoField = (EditText) addBankLayout.findViewById(R.id.bankAccNo);
		bankBalanceField = (EditText) addBankLayout.findViewById(R.id.bankBalance);
		
		bankSmsNameField = (AutoCompleteTextView) addBankLayout.findViewById(R.id.bankSmsName);
		ArrayAdapter<String> bankSmsNameSuggestionAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, smsNameSuggestions);
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
				int id = banks.size() + 1;
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName);
				
				if(dataCorrect)
				{
					if(bankAccNo.length()==0)
						bankAccNo+="0000";
					
					Bank bank = new Bank(id, bankName, bankAccNo, bankBalance, bankSmsName);
					banks.add(bank);
					
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
			Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void deleteBank(int bankNo)
	{
		// Remove Bank Details
		banks.remove(bankNo);
	}
	
	private void editBank(final int bankNo)
	{
		buildAddBankDialog();
		bankNameField.setText(banks.get(bankNo).getName() + " ");
		bankAccNoField.setText(""+banks.get(bankNo).getAccNo());
		bankBalanceField.setText(""+banks.get(bankNo).getBalance());
		bankSmsNameField.setText(banks.get(bankNo).getSmsName() + " ");
		
		addBankDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				int id = bankNo + 1;	// IDs start with 1 but array indexes start with 0
				String bankName = bankNameField.getText().toString().trim();
				String bankAccNo = bankAccNoField.getText().toString().trim();
				String bankBalance = bankBalanceField.getText().toString().trim();
				String bankSmsName = bankSmsNameField.getText().toString().trim();
				boolean dataCorrect = verifyData(bankName, bankBalance, bankSmsName);
				
				if(dataCorrect)
				{
					if(bankAccNo.length()==0)
						bankAccNo+="0000";
					
					Bank bank = new Bank(id, bankName, bankAccNo, bankBalance, bankSmsName);
					banks.set(bankNo, bank);
					
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
			Toast.makeText(getActivity(), "Please Enter The Bank Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(bankBalance.length()==0)
		{
			Toast.makeText(getActivity(), "Please Enter The Bank Balance", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(bankSmsName.length()==0)
		{
			Toast.makeText(getActivity(), "Please Enter The Bank Sms Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
}
