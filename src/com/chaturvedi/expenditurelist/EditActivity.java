package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
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

public class EditActivity extends Activity
{
	private static DisplayMetrics displayMetrics;
	private static int screenWidth;
	private static int screenHeight;
	private static int MARGIN_TOP_PARENT_LAYOUT;
	private static int MARGIN_LEFT_PARENT_LAYOUT;
	private static int WIDTH_NAME_FIELDS;
	private static int WIDTH_AMOUNT_FIELDS;
	private static int MARGIN_LEFT_AMOUNT_FIELDS;
	
	private static LinearLayout parentLayout;
	private static RelativeLayout.LayoutParams parentLayoutParams;
	private static ArrayList<LinearLayout> linearLayouts;
	private static ArrayList<LayoutParams> linearLayoutParams;
	private static TextView walletView;
	private static LayoutParams walletViewParams;
	private static EditText walletField;
	private static LayoutParams walletFieldParams;
	private static ArrayList<EditText> bankNameFields;
	private static ArrayList<LayoutParams> bankNameFieldParams;
	private static ArrayList<EditText> bankBalanceFields;
	private static ArrayList<LayoutParams> bankBalanceFieldParams;
	private static ArrayList<ImageButton> removeButtons;
	private static ArrayList<LayoutParams> removeButtonParams;
	private static Button addBankButton;
	
	private static int numBanks=0;
	private static int numEntries;
	private static int walletBalance;
	private static int amountSpent;
	private ArrayList<String> bankNames;
	private ArrayList<Integer> bankBalances;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_edit);
		}
		else
		{
			setContentView(R.layout.activity_edit);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		readFile();
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=(screenHeight-(numBanks*100))/6;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*10/100;
		WIDTH_NAME_FIELDS=screenWidth*50/100;
		WIDTH_AMOUNT_FIELDS=screenWidth*25/100;
		
		buildLayout();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_startup, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.finish:
				saveData();
				finish();
				return true;
		}
		return true;
	}
	
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}

	private void buildLayout()
	{
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(RelativeLayout.LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, 0, 0);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		walletView=(TextView)findViewById(R.id.wallet_view);
		walletViewParams=new LayoutParams(WIDTH_NAME_FIELDS, LayoutParams.WRAP_CONTENT);
		walletView.setLayoutParams(walletViewParams);
		walletField=(EditText)findViewById(R.id.wallet_field);
		walletFieldParams=new LayoutParams(WIDTH_AMOUNT_FIELDS, LayoutParams.WRAP_CONTENT);
		walletFieldParams.setMargins(MARGIN_LEFT_AMOUNT_FIELDS, 0, 0, 0);
		walletField.setLayoutParams(walletFieldParams);
		
		addBankButton=(Button)findViewById(R.id.add_bank);
		addBankButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				addBank(numBanks);
				numBanks++;
			}
		});

		linearLayouts=new ArrayList<LinearLayout>(numBanks+1);
		linearLayoutParams=new ArrayList<LayoutParams>(numBanks+1);
		bankNameFields=new ArrayList<EditText>(numBanks);
		bankNameFieldParams=new ArrayList<LayoutParams>(numBanks);
		bankBalanceFields=new ArrayList<EditText>(numBanks);
		bankBalanceFieldParams=new ArrayList<LayoutParams>(numBanks);
		removeButtons=new ArrayList<ImageButton>(numBanks);
		removeButtonParams=new ArrayList<LayoutParams>(numBanks);
		
		walletField.setText(walletBalance+"");
		for(int i=0; i<numBanks; i++)
		{
			addBank(i);
			bankNameFields.get(i).setText(bankNames.get(i));
			bankBalanceFields.get(i).setText(""+bankBalances.get(i));
		}
	}
	
	

	protected void addBank(int bankNum)
	{
		LayoutInflater layoutInflater=LayoutInflater.from(this);
		final LinearLayout layout=(LinearLayout)layoutInflater.inflate(R.layout.layout_add_bank, null);
		linearLayouts.add(layout);
		linearLayoutParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		bankNameFields.add((EditText)layout.findViewById(R.id.bankName));
		LayoutParams layoutParams=new LayoutParams(WIDTH_NAME_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, 0, 0, 0);
		bankNameFieldParams.add(layoutParams);
		bankNameFields.get(bankNum).setLayoutParams(layoutParams);

		bankBalanceFields.add((EditText)layout.findViewById(R.id.bankBalance));
		layoutParams=new LayoutParams(WIDTH_AMOUNT_FIELDS, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(MARGIN_LEFT_AMOUNT_FIELDS, 0, 0, 0);
		bankBalanceFieldParams.add(layoutParams);
		bankBalanceFields.get(bankNum).setLayoutParams(layoutParams);
		
		ImageButton removeButton=(ImageButton)layout.findViewById(R.id.button_remove);
		removeButtons.add(removeButton);
		removeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				removeBank(layout);
			}
		});
		
		parentLayout.addView(layout, linearLayoutParams.get(bankNum));
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
		removeButtons.remove(bankNum);
		numBanks--;
	}
	
	private void readFile()
	{
		String line;
		
		try
		{
			String expenditureFolderName = "Expenditure List/.temp";
			String prefFileName = "preferences.txt";
			String walletFileName = "wallet_info.txt";
			String bankFileName = "bank_info.txt";
			//String particularsFileName = "particulars.txt";
			//String amountFileName = "amount.txt";
			
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			File prefFile = new File(expenditureFolder, prefFileName);
			File walletFile = new File(expenditureFolder, walletFileName);
			File bankFile = new File(expenditureFolder, bankFileName);
			//File particularsFile = new File(expenditureFolder, particularsFileName);
			//File amountFile = new File(expenditureFolder, amountFileName);

			BufferedReader prefReader = new BufferedReader(new FileReader(prefFile));
			BufferedReader walletReader = new BufferedReader(new FileReader(walletFile));
			BufferedReader bankReader = new BufferedReader(new FileReader(bankFile));
			//BufferedReader particularsReader = new BufferedReader(new FileReader(particularsFile));
			//BufferedReader amountReader = new BufferedReader(new FileReader(amountFile));
			
			numBanks=Integer.parseInt(prefReader.readLine());
			numEntries = Integer.parseInt(prefReader.readLine());
			line=walletReader.readLine();
			walletBalance = Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			line=walletReader.readLine();
			amountSpent = Integer.parseInt(line.substring(line.indexOf("Rs")+2));
			
			bankNames=new ArrayList<String>();
			bankBalances=new ArrayList<Integer>();
			for(int i=0; i<numBanks; i++)
			{
				line=bankReader.readLine();
				bankNames.add(line.substring(0, line.indexOf("=")));
				bankBalances.add(Integer.parseInt(line.substring(line.indexOf("Rs")+2)));
			}
			
			/*particulars=new ArrayList<String>();
			amounts=new ArrayList<Integer>();
			for(int i=0; i<numEntries; i++)
			{
				particulars.add(particularsReader.readLine());
				amounts.add(Integer.parseInt(amountReader.readLine()));
			}*/
			
			prefReader.close();
			walletReader.close();
			bankReader.close();
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
			String expenditureFolderName = "Expenditure List/.temp";
			String prefFileName = "preferences.txt";
			String walletFileName = "wallet_info.txt";
			String bankFileName = "bank_info.txt";
			//String particularsFileName = "particulars.txt";
			//String amountFileName = "amount.txt";
			
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			File prefFile = new File(expenditureFolder, prefFileName);
			File walletFile = new File(expenditureFolder, walletFileName);
			File bankFile = new File(expenditureFolder, bankFileName);
			//File particularsFile = new File(expenditureFolder, particularsFileName);
			//File amountFile = new File(expenditureFolder, amountFileName);
			
			BufferedWriter prefWriter = new BufferedWriter(new FileWriter(prefFile));
			BufferedWriter walletWriter = new BufferedWriter(new FileWriter(walletFile));
			BufferedWriter bankWriter = new BufferedWriter(new FileWriter(bankFile));
			//BufferedWriter particularsWriter = new BufferedWriter(new FileWriter(particularsFile));
			//BufferedWriter amountWriter = new BufferedWriter(new FileWriter(amountFile));
			
			prefWriter.write(numBanks+"\n");
			prefWriter.write(numEntries+"\n");
			walletWriter.write("wallet_balance=Rs"+walletField.getText().toString()+"\n");
			walletWriter.write("amount_spent=Rs"+amountSpent+"\n");
			for(int i=0; i<numBanks; i++)
			{
				bankWriter.write(bankNameFields.get(i).getText().toString()+"=Rs"+bankBalanceFields.get(i).getText().toString()+"\n");
			}
			prefWriter.close();
			walletWriter.close();
			bankWriter.close();
			//particularsWriter.close();
			//amountWriter.close();
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Saving To File", Toast.LENGTH_SHORT).show();
		}
	}
}
