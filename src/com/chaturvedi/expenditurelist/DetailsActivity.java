package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DetailsActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_TOP=100;
	private int MARGIN_TOP_SCROLL_VIEW;
	private int MARGIN_BOTTOM_SCROLL_VIEW;
	private int MARGIN_LEFT_SLNO;
	private int MARGIN_LEFT_PARTICULARS;
	private int MARGIN_LEFT_AMOUNT;
	private int MARGIN_RIGHT_AMOUNT;
	private int MARGIN_TOP_ITEMS=20;
	private int WIDTH_SLNO;
	private int WIDTH_PARTICULARS;
	private int WIDTH_AMOUNT;
	

	private int walletBalance;
	private int amountSpent;
	private int numEntries;
	private ArrayList<String> particulars;
	private ArrayList<Integer> amounts;
	private int numBanks;
	private ArrayList<String> bankNames;
	private ArrayList<Integer> bankBalances;
	
	private RelativeLayout titleLayout;
	private ArrayList<RelativeLayout> itemsLayout;
	private ScrollView detailsScrollView;
	private LinearLayout scrollLayout;
	
	private TextView slnoTitleView;
	private TextView particularsTitleView;
	private TextView amountTitleView;
	private ArrayList<ArrayList<TextView>> itemsView;
	
	private RelativeLayout.LayoutParams titleLayoutParams;
	private RelativeLayout.LayoutParams slnoTitleParams;
	private RelativeLayout.LayoutParams particularsTitleParams;
	private RelativeLayout.LayoutParams amountTitleParams;
	private RelativeLayout.LayoutParams scrollViewParams;
	private ArrayList<ArrayList<RelativeLayout.LayoutParams>> itemsLayoutParams;
	
	private String expenditureFolderName;
	private String prefFileName;
	private String walletFileName;
	private String bankFileName;
	private String particularsFileName;
	private String amountFileName;
	private File expenditureFolder;
	private File prefFile;
	private File walletFile;
	private File bankFile;
	private File particularsFile;
	private File amountFile;
	private BufferedReader prefReader;
	private BufferedReader walletReader;
	private BufferedReader bankReader;
	private BufferedReader particularsReader;
	private BufferedReader amountReader;
	private BufferedWriter prefWriter;
	private BufferedWriter walletWriter;
	private BufferedWriter bankWriter;
	private BufferedWriter particularsWriter;
	private BufferedWriter amountWriter;
	
	private Button debitButton;
	private Button creditButton;
	private AlertDialog.Builder expenditureDialog;
	private AlertDialog.Builder atmWithdrawalDialog;
	private LayoutInflater expenditureDialogLayout;
	private LayoutInflater atmWithdrawalDialogLayout;
	private View expenditureDialogView;
	private View atmWithdrawalDialogView;
	private EditText particularsField;
	private EditText amountField;
	ArrayList<RadioButton> banks;
	private EditText withdrawalAmountField;
	private Intent detailsIntent;
	
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
		detailsIntent=getIntent();
		numEntries=detailsIntent.getIntExtra("Number Of Entries", 0);
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		if(VERSION.SDK_INT<=10)
		{
			MARGIN_TOP=screenHeight*18/100;
			MARGIN_TOP_SCROLL_VIEW=screenHeight*6/100;
			MARGIN_BOTTOM_SCROLL_VIEW=screenHeight*15/100;
		}
		else
		{
			MARGIN_TOP=screenHeight*8/100;
			MARGIN_TOP_SCROLL_VIEW=screenHeight*12/100;
			MARGIN_BOTTOM_SCROLL_VIEW=screenHeight*12/100;
		}
		
		MARGIN_LEFT_SLNO=5*screenWidth/100;
		MARGIN_LEFT_PARTICULARS=20*screenWidth/100;
		MARGIN_LEFT_AMOUNT=80*screenWidth/100;
		MARGIN_RIGHT_AMOUNT=5*screenWidth/100;
		WIDTH_SLNO=15*screenWidth/100;
		WIDTH_PARTICULARS=60*screenWidth/100;
		WIDTH_AMOUNT=20*screenWidth/100;
		
		readFile();
		buildTitleLayout();
		buildBodyLayout();
		buildButtonPanel();
	}

	private void readFile()
	{
		String line;
		
		try
		{
			expenditureFolderName="Expenditure List/.temp";
			prefFileName="preferences.txt";
			walletFileName="wallet_info.txt";
			bankFileName="bank_info.txt";
			particularsFileName="particulars.txt";
			amountFileName="amount.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			prefFile=new File(expenditureFolder, prefFileName);
			walletFile=new File(expenditureFolder, walletFileName);
			bankFile=new File(expenditureFolder, bankFileName);
			particularsFile=new File(expenditureFolder, particularsFileName);
			amountFile=new File(expenditureFolder, amountFileName);

			prefReader=new BufferedReader(new FileReader(prefFile));
			walletReader=new BufferedReader(new FileReader(walletFile));
			bankReader=new BufferedReader(new FileReader(bankFile));
			particularsReader=new BufferedReader(new FileReader(particularsFile));
			amountReader=new BufferedReader(new FileReader(amountFile));
			
			numBanks=Integer.parseInt(prefReader.readLine());
			numEntries=Integer.parseInt(prefReader.readLine());
			line=walletReader.readLine();
			walletBalance=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			line=walletReader.readLine();
			amountSpent=Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			
			bankNames=new ArrayList<String>();
			bankBalances=new ArrayList<Integer>();
			for(int i=0; i<numBanks; i++)
			{
				line=bankReader.readLine();
				bankNames.add(line.substring(0, line.indexOf("=")));
				bankBalances.add(Integer.parseInt(line.substring(line.indexOf("Rs")+2)));
			}
			
			particulars=new ArrayList<String>();
			amounts=new ArrayList<Integer>();
			for(int i=0; i<numEntries; i++)
			{
				particulars.add(particularsReader.readLine());
				amounts.add(Integer.parseInt(amountReader.readLine()));
			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void saveData()
	{
		try
		{
			expenditureFolderName="Expenditure List/.temp";
			prefFileName="preferences.txt";
			walletFileName="wallet_info.txt";
			bankFileName="bank_info.txt";
			particularsFileName="particulars.txt";
			amountFileName="amount.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			prefFile=new File(expenditureFolder, prefFileName);
			walletFile=new File(expenditureFolder, walletFileName);
			bankFile=new File(expenditureFolder, bankFileName);
			particularsFile=new File(expenditureFolder, particularsFileName);
			amountFile=new File(expenditureFolder, amountFileName);
			
			prefWriter=new BufferedWriter(new FileWriter(prefFile));
			walletWriter=new BufferedWriter(new FileWriter(walletFile));
			bankWriter=new BufferedWriter(new FileWriter(bankFile));
			particularsWriter=new BufferedWriter(new FileWriter(particularsFile));
			amountWriter=new BufferedWriter(new FileWriter(amountFile));
			
			prefWriter.write(numBanks+"\n");
			prefWriter.write(""+numEntries+"\n");
			walletWriter.write("wallet_balance=Rs"+walletBalance+"\n");
			walletWriter.write("amount_spent=Rs"+amountSpent+"\n");
			for(int i=0; i<numBanks; i++)
			{
				bankWriter.write(bankNames.get(i)+"=Rs"+bankBalances.get(i)+"\n");
			}
			for(int i=0; i<numEntries; i++)
			{
				particularsWriter.write(particulars.get(i)+"\n");
				amountWriter.write(amounts.get(i)+"\n");
			}
			prefWriter.close();
			walletWriter.close();
			bankWriter.close();
			particularsWriter.close();
			amountWriter.close();
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Saving To File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void buildTitleLayout()
	{
		titleLayout=new RelativeLayout(this);
		titleLayoutParams=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		titleLayout.setLayoutParams(titleLayoutParams);
		
		slnoTitleView=new TextView(this);
		slnoTitleView.setText("Sl No");
		slnoTitleParams=new RelativeLayout.LayoutParams(WIDTH_SLNO, RelativeLayout.LayoutParams.WRAP_CONTENT);
		slnoTitleParams.setMargins(MARGIN_LEFT_SLNO, MARGIN_TOP, 0, 0);
		
		particularsTitleView=new TextView(this);
		particularsTitleView.setText("Particulars");
		particularsTitleParams=new RelativeLayout.LayoutParams(WIDTH_PARTICULARS, RelativeLayout.LayoutParams.WRAP_CONTENT);
		particularsTitleParams.setMargins(screenWidth*40/100, MARGIN_TOP, 0, 0);
		
		amountTitleView=new TextView(this);
		amountTitleView.setText("Amount");
		amountTitleParams=new RelativeLayout.LayoutParams(WIDTH_AMOUNT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		amountTitleParams.setMargins(screenWidth*80/100, MARGIN_TOP, 0, 0);
		
		titleLayout.addView(slnoTitleView, slnoTitleParams);
		titleLayout.addView(particularsTitleView, particularsTitleParams);
		titleLayout.addView(amountTitleView, amountTitleParams);
		this.addContentView(titleLayout, titleLayoutParams);
	}
	
	private void buildBodyLayout()
	{
		try
		{
			detailsScrollView=(ScrollView)findViewById(R.id.details_scroll);
			scrollViewParams=(RelativeLayout.LayoutParams)detailsScrollView.getLayoutParams();
			scrollViewParams.setMargins(0, MARGIN_TOP_SCROLL_VIEW, 0, MARGIN_BOTTOM_SCROLL_VIEW);
			detailsScrollView.setLayoutParams(scrollViewParams);
			
			scrollLayout=(LinearLayout)findViewById(R.id.scroll_layout);
			scrollLayout.removeAllViews();
			
			itemsLayout=new ArrayList<RelativeLayout>();
			itemsView=new ArrayList<ArrayList<TextView>>();
			itemsLayoutParams=new ArrayList<ArrayList<RelativeLayout.LayoutParams>>();
			for(int i=0; i<numEntries; i++)
			{
				itemsLayout.add(new RelativeLayout(this));
				
				itemsView.add(new ArrayList<TextView>());
				itemsView.get(i).add(new TextView(this));
				itemsView.get(i).add(new TextView(this));
				itemsView.get(i).add(new TextView(this));
				itemsView.get(i).get(0).setText(""+(i+1));
				itemsView.get(i).get(1).setText(particulars.get(i));
				itemsView.get(i).get(2).setText(amounts.get(i)+"");
				itemsView.get(i).get(2).setGravity(Gravity.RIGHT);
				
				itemsLayoutParams.add(new ArrayList<RelativeLayout.LayoutParams>());
				itemsLayoutParams.get(i).add(new RelativeLayout.LayoutParams(WIDTH_SLNO, RelativeLayout.LayoutParams.WRAP_CONTENT));
				itemsLayoutParams.get(i).add(new RelativeLayout.LayoutParams(WIDTH_PARTICULARS, RelativeLayout.LayoutParams.WRAP_CONTENT));
				itemsLayoutParams.get(i).add(new RelativeLayout.LayoutParams(WIDTH_AMOUNT, RelativeLayout.LayoutParams.WRAP_CONTENT));
				itemsLayoutParams.get(i).get(0).setMargins(MARGIN_LEFT_SLNO, MARGIN_TOP_ITEMS, 0, 0);
				itemsLayoutParams.get(i).get(1).setMargins(MARGIN_LEFT_PARTICULARS, MARGIN_TOP_ITEMS, 0, 0);
				itemsLayoutParams.get(i).get(2).setMargins(MARGIN_LEFT_AMOUNT, MARGIN_TOP_ITEMS, MARGIN_RIGHT_AMOUNT, 0);
				
				itemsLayout.get(i).addView(itemsView.get(i).get(0), itemsLayoutParams.get(i).get(0));
				itemsLayout.get(i).addView(itemsView.get(i).get(1), itemsLayoutParams.get(i).get(1));
				itemsLayout.get(i).addView(itemsView.get(i).get(2), itemsLayoutParams.get(i).get(2));
				scrollLayout.addView(itemsLayout.get(i), titleLayoutParams);
			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Building Body Layout\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	private void buildButtonPanel()
	{
		debitButton=(Button)findViewById(R.id.button_debit);
		debitButton.setOnClickListener(new DebitListener());
		debitButton.bringToFront();
		creditButton=(Button)findViewById(R.id.button_atm_withdrawal);
		creditButton.setOnClickListener(new CreditListener());
		creditButton.bringToFront();
	}
	
	private void buildExpenditureDialog()
	{
		expenditureDialog=new AlertDialog.Builder(this);
		expenditureDialog.setTitle("Add Expenditure");
		expenditureDialog.setMessage("Enter Details");
		expenditureDialogLayout=LayoutInflater.from(this);
		expenditureDialogView=expenditureDialogLayout.inflate(R.layout.layout_expenditure_dialog, null);
		expenditureDialog.setView(expenditureDialogView);
		expenditureDialog.setPositiveButton("OK", new ExpenditureDialogListener(1));
		expenditureDialog.setNegativeButton("Cancel", new ExpenditureDialogListener(0));
		particularsField=(EditText)expenditureDialogView.findViewById(R.id.edit_particulars);
		amountField=(EditText)expenditureDialogView.findViewById(R.id.edit_amount);
	}
	
	private void buildWithdrawalDialog()
	{
		atmWithdrawalDialog=new AlertDialog.Builder(this);
		atmWithdrawalDialog.setTitle("Add ATM Withdrawal");
		atmWithdrawalDialog.setMessage("Enter Details");
		atmWithdrawalDialogLayout=LayoutInflater.from(this);
		atmWithdrawalDialogView=atmWithdrawalDialogLayout.inflate(R.layout.layout_atm_withdrawal_dialog, null);
		atmWithdrawalDialog.setView(atmWithdrawalDialogView);
		atmWithdrawalDialog.setPositiveButton("OK", new AtmWithdrawalDialogListener(1));
		atmWithdrawalDialog.setNegativeButton("Cancel", new AtmWithdrawalDialogListener(0));
		banks=new ArrayList<RadioButton>();
		banks.add((RadioButton)atmWithdrawalDialogView.findViewById(R.id.bank_01));
		banks.add((RadioButton)atmWithdrawalDialogView.findViewById(R.id.bank_02));
		banks.add((RadioButton)atmWithdrawalDialogView.findViewById(R.id.bank_03));
		banks.get(2).setVisibility(View.GONE);
		for(int i=0; i<numBanks; i++)
		{
			banks.get(i).setText(bankNames.get(i));
			banks.get(i).setTextSize(20);
			banks.get(i).setTextColor(999999999);
		}
		banks.get(0).setChecked(true);
		withdrawalAmountField=(EditText)atmWithdrawalDialogView.findViewById(R.id.edit_amount);
	}
	
	/*private String breakParticularsString(String line)
	{
		ArrayList<String> lines=new ArrayList<String>();
		for(int i=0; i<line.length()/10; i++)
		{
			lines.add(line.substring(0, 10));
			line=line.substring(10);
		}
		line="";
		for(int i=0; i<lines.size(); i++)
		{
			line+=lines.get(i)+"\n";
		}
		return line;
	}*/
	
	private class DebitListener implements OnClickListener
	{

		@Override
		public void onClick(View v)
		{
			buildExpenditureDialog();
			expenditureDialog.show();
		}
		
	}
	
	private class CreditListener implements OnClickListener
	{

		@Override
		public void onClick(View v)
		{
			buildWithdrawalDialog();
			atmWithdrawalDialog.show();
		}
		
	}
	
	private class ExpenditureDialogListener implements DialogInterface.OnClickListener
	{
		private int action;
		
		public ExpenditureDialogListener(int act)
		{
			action=act;
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if(action==1)
			{
				numEntries++;
				int amount=Integer.parseInt(amountField.getText().toString());
				walletBalance-=amount;
				amountSpent+=amount;
				particulars.add(particularsField.getText().toString());
				amounts.add(amount);
				saveData();
				//readFile();
				buildTitleLayout();
				buildBodyLayout();
				buildButtonPanel();
				Toast.makeText(getApplicationContext(), "Data Saved", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	private class AtmWithdrawalDialogListener implements DialogInterface.OnClickListener
	{
		private int action;
		
		public AtmWithdrawalDialogListener(int act)
		{
			action=act;
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if(action==1)
			{
				int bankNo=0;
				int withdrawalAmount=Integer.parseInt(withdrawalAmountField.getText().toString());
				if(banks.get(0).isChecked())
					bankNo=0;
				else if(banks.get(1).isChecked())
					bankNo=1;
				else if(banks.get(2).isChecked())
					bankNo=2;
				
				bankBalances.set(bankNo, bankBalances.get(bankNo)-withdrawalAmount);
				walletBalance+=withdrawalAmount;
				numEntries++;
				particulars.add(bankNames.get(bankNo)+" withdrawal");
				amounts.add(withdrawalAmount);
				saveData();
				//readFile();
				buildTitleLayout();
				buildBodyLayout();
				buildButtonPanel();
				Toast.makeText(getApplicationContext(), "Data Saved", Toast.LENGTH_SHORT).show();
			}
			//Toast.makeText(getApplicationContext(), banks.get(0).getText()+banks.get(0).toString(), Toast.LENGTH_SHORT).show();
		}
		
	}
}
