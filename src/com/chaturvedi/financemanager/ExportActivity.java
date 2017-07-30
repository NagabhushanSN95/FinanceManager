package com.chaturvedi.financemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.Bank;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Transaction;

public class ExportActivity extends Activity
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_CURRENCY_SYMBOL = "currency_symbols";
	private String currencySymbol = " ";
	
	private String exportFileName;
	private long exportMonth;
	
	private TextView exportFileNameField;
	private Spinner monthsList;
	ArrayList<String> months;
	private LayoutInflater exportDialogLayout;
	private View exportDialogView;
	private AlertDialog.Builder exportDialog;
	
	private Calendar calendar;
	private String currentYear;
	private String currentMonth;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_CURRENCY_SYMBOL))
		{
			currencySymbol = preferences.getString(KEY_CURRENCY_SYMBOL, " ");
		}
		
		exportDialogLayout=LayoutInflater.from(this);
		exportDialogView=exportDialogLayout.inflate(R.layout.dialog_export, null);
		exportFileNameField=(TextView)exportDialogView.findViewById(R.id.editText_export_fileName);
		
		monthsList = (Spinner) exportDialogView.findViewById(R.id.monthsList);
		months = DatabaseManager.getExportableMonths();
		months.add(0, "Current Month");
		monthsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, months));
		monthsList.setSelection(0);
		monthsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int monthNo, long arg3)
			{
				if(monthNo != 0)
				{
					exportFileName = months.get(monthNo) + ".doc";
					exportFileNameField.setText(exportFileName);
					exportMonth = getLongMonth(months.get(monthNo));
				}
				else
				{
					calendar=Calendar.getInstance();
					currentMonth=getMonth(calendar.get(Calendar.MONTH) + 1);
					currentYear=calendar.get(Calendar.YEAR)+"";
					exportFileName=currentMonth+" - "+currentYear+".doc";
					exportFileNameField.setText(exportFileName);
					exportMonth = calendar.get(Calendar.YEAR)*100 + calendar.get(Calendar.MONTH) + 1;
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
				
			}
		});
		
		exportDialog=new AlertDialog.Builder(this);
		exportDialog.setTitle("Export Data");
		exportDialog.setView(exportDialogView);
		exportDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				exportFileName=exportFileNameField.getText().toString();
				saveData();
				finish();
			}
		});
		exportDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				finish();
			}
		});
		exportDialog.setCancelable(false);
		exportDialog.create();
		exportDialog.show();
		
	}
	
	private String getMonth(int month)
	{
		switch(month)
		{
			case 1:
				return "January";
				
			case 2:
				return "February";
				
			case 3:
				return "March";
				
			case 4:
				return "April";
				
			case 5:
				return "May";
				
			case 6:
				return "June";
				
			case 7:
				return "July";
				
			case 8:
				return "August";
				
			case 9:
				return "September";
				
			case 10:
				return "October";
				
			case 11:
				return "November";
				
			case 12:
				return "December";
				
			default:
				return "";
		}
	}
	
	private long getLongMonth(String fullMonth)
	{
		StringTokenizer tokens = new StringTokenizer(fullMonth, "-");
		String month = tokens.nextToken();
		int month1;
		int year = Integer.parseInt(tokens.nextToken().trim());
		
		if(month.contains("January"))
		{
			month1 = 01;
		}
		else if(month.contains("February"))
		{
			month1 = 02;
		}
		else if(month.contains("March"))
		{
			month1 = 03;
		}
		else if(month.contains("April"))
		{
			month1 = 04;
		}
		else if(month.contains("May"))
		{
			month1 = 05;
		}
		else if(month.contains("June"))
		{
			month1 = 06;
		}
		else if(month.contains("July"))
		{
			month1 = 07;
		}
		else if(month.contains("August"))
		{
			month1 = 8;
		}
		else if(month.contains("September"))
		{
			month1 = 9;
		}
		else if(month.contains("October"))
		{
			month1 = 10;
		}
		else if(month.contains("November"))
		{
			month1 = 11;
		}
		else if(month.contains("December"))
		{
			month1 = 12;
		}
		else
		{
			month1 = 01;
		}
		long longMonth = year*100 + month1;
		return longMonth;
	}
	
	private void saveData()
	{
		try
		{
			String expenditureFolderName = "Finance Manager";
			
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			File exportFile = new File(expenditureFolder, exportFileName);
			BufferedWriter exportWriter = new BufferedWriter(new FileWriter(exportFile));
			
			exportWriter.write("<html>\n<body>");
			exportWriter.write("<h1>"+currentMonth+"-"+currentYear+"</h1>\n");
			
			/*exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			for(int i=0; i<initialConditions.size(); i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+initialConditions.get(i).get(0)+"</td>");
				exportWriter.write("\t<td>"+initialConditions.get(i).get(1)+"</td>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");*/

			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Sl No"+"</td>");
			exportWriter.write("\t<td>"+"Date"+"</td>");
			exportWriter.write("\t<td>"+"Type"+"</td>");
			exportWriter.write("\t<td>"+"Particulars"+"</td>");
			exportWriter.write("\t<td>"+"Rate"+"</td>");
			exportWriter.write("\t<td>"+"Quantity"+"</td>");
			exportWriter.write("\t<td>"+"Amount"+"</td>");
			exportWriter.write("</tr>\n");
			
			ArrayList<Transaction> transactions = DatabaseManager.getMonthlyTransactions(exportMonth);
			for(int i=0; i<transactions.size(); i++)
			{
				Transaction transaction = transactions.get(i);
				int transactionNo = DatabaseManager.getAllTransactions().indexOf(transaction);
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+(i+1)+"</td>");
				exportWriter.write("\t<td>"+transaction.getDate().getDisplayDate()+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getExactExpType(transactionNo)+"</td>");
				exportWriter.write("\t<td>"+transaction.getParticular()+"</td>");
				exportWriter.write("\t<td>"+transaction.getRate()+"</td>");
				exportWriter.write("\t<td>"+transaction.getQuantity()+"</td>");
				exportWriter.write("\t<td>"+transaction.getAmount()+"</td>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Income In This Month"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+DatabaseManager.getMonthlyIncome(exportMonth)+"</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Amount Spent In This Month"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+DatabaseManager.getMonthlyAmountSpent(exportMonth)+"</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Amount In wallet"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+DatabaseManager.getWalletBalance()+"</td>");
			exportWriter.write("</tr>\n");
			ArrayList<Bank> banks = DatabaseManager.getAllBanks();
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+"Amount In "+banks.get(i).getName()+"</td>");
				exportWriter.write("\t<td>"+currencySymbol+banks.get(i).getBalance()+"</td>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.write("</html>\n</body>");
			exportWriter.close();
			
			Toast.makeText(getApplicationContext(), "Data Has Been Exported Successfully", Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), "Error In Saving To File\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
