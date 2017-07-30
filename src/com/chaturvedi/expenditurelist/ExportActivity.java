package com.chaturvedi.expenditurelist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class ExportActivity extends Activity
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_CURRENCY_SYMBOL = "currency_symbols";
	private String currencySymbol = " ";
	
	private String exportFileName;
	
	private TextView exportFileNameField;
	private CheckBox clearDataCheckBox;
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
		
		calendar=Calendar.getInstance();
		currentMonth=getMonth(calendar.get(Calendar.MONTH));
		currentYear=calendar.get(Calendar.YEAR)+"";
		exportFileName=currentMonth+"-"+currentYear+".doc";
		
		exportDialogLayout=LayoutInflater.from(this);
		exportDialogView=exportDialogLayout.inflate(R.layout.dialog_export, null);
		exportFileNameField=(TextView)exportDialogView.findViewById(R.id.editText_export_fileName);
		exportFileNameField.setText(exportFileName);
		clearDataCheckBox=(CheckBox)exportDialogView.findViewById(R.id.checkBox_erase_data);
		
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
				if(clearDataCheckBox.isChecked())
					DatabaseManager.clearDatabase();
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
				
			case 0:
				return "December";
				
			default:
				return "";
		}
	}
	
	private void saveData()
	{
		try
		{
			String expenditureFolderName = "Expenditure List";
			
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
			for(int i=0; i<DatabaseManager.getNumTransactions(); i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+(i+1)+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getDate(i)+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getExactExpType(i)+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getParticular(i)+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getRate(i)+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getQuantity(i)+"</td>");
				exportWriter.write("\t<td>"+DatabaseManager.getAmount(i)+"</td>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Income In This Month"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+DatabaseManager.getTotalIncome()+"</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Amount Spent In This Month"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+DatabaseManager.getTotalAmountSpent()+"</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Amount In wallet"+"</td>");
			exportWriter.write("\t<td>"+currencySymbol+DatabaseManager.getWalletBalance()+"</td>");
			exportWriter.write("</tr>\n");
			ArrayList<String> bankNames = DatabaseManager.getAllBankNames();
			ArrayList<Double> bankBalances = DatabaseManager.getAllBankBalances();
			for(int i=0; i<DatabaseManager.getNumBanks(); i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+"Amount In "+bankNames.get(i)+"</td>");
				exportWriter.write("\t<td>"+currencySymbol+bankBalances.get(i)+"</td>");
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
