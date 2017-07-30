package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class ExportActivity extends Activity
{
	private String expenditureFolderName;
	private String prefFileName;
	private String walletFileName;
	private String bankFileName;
	private String particularsFileName;
	private String amountFileName;
	private String dateFileName;
	private String exportFileName;
	private File expenditureFolder;
	private File prefFile;
	private File walletFile;
	private File bankFile;
	private File particularsFile;
	private File amountFile;
	private File dateFile;
	private File exportFile;
	private BufferedReader prefReader;
	private BufferedReader walletReader;
	private BufferedReader bankReader;
	private BufferedReader particularsReader;
	private BufferedReader amountReader;
	private BufferedReader dateReader;
	private BufferedWriter exportWriter;
	
	private TextView exportFileNameField;
	private CheckBox clearDataCheckBox;
	private LayoutInflater exportDialogLayout;
	private View exportDialogView;
	private AlertDialog.Builder exportDialog;
	
	private int numBanks;
	private int numEntries;
	private int walletBalance;
	private int amountSpent;
	private ArrayList<String> bankNames;
	private ArrayList<Integer> bankBalances;
	private ArrayList<String> particulars;
	private ArrayList<Integer> amounts;
	private ArrayList<String> dates;
	
	private Calendar calendar;
	private String currentYear;
	private String currentMonth;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		calendar=Calendar.getInstance();
		currentMonth=getMonth(calendar.get(Calendar.MONTH));
		currentYear=calendar.get(Calendar.YEAR)+"";
		exportFileName=currentMonth+"-"+currentYear+".doc";
		readFile();
		
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
					clearData();
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
			dateFileName="date.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			prefFile=new File(expenditureFolder, prefFileName);
			walletFile=new File(expenditureFolder, walletFileName);
			bankFile=new File(expenditureFolder, bankFileName);
			particularsFile=new File(expenditureFolder, particularsFileName);
			amountFile=new File(expenditureFolder, amountFileName);
			dateFile=new File(expenditureFolder, dateFileName);

			prefReader=new BufferedReader(new FileReader(prefFile));
			walletReader=new BufferedReader(new FileReader(walletFile));
			bankReader=new BufferedReader(new FileReader(bankFile));
			particularsReader=new BufferedReader(new FileReader(particularsFile));
			amountReader=new BufferedReader(new FileReader(amountFile));
			dateReader=new BufferedReader(new FileReader(dateFile));
			
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
			dates=new ArrayList<String>();
			for(int i=0; i<numEntries; i++)
			{
				particulars.add(particularsReader.readLine());
				amounts.add(Integer.parseInt(amountReader.readLine()));
				dates.add(dateReader.readLine());
			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Reading File\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void saveData()
	{
		//String spaces="";
		try
		{
			expenditureFolderName="Expenditure List";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			exportFile=new File(expenditureFolder, exportFileName);
			exportWriter=new BufferedWriter(new FileWriter(exportFile));
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			for(int i=0; i<numEntries; i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+(i+1)+"</td>");
				exportWriter.write("\t<td>"+dates.get(i)+"</td>");
				exportWriter.write("\t<td>"+particulars.get(i)+"</td>");
				exportWriter.write("\t<td>"+amounts.get(i)+"</td>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.write("<table border=\"1\" style=\"width:600px\">");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Total Amount Spent In This Month"+"</td>");
			exportWriter.write("\t<td>"+"Rs "+amountSpent+"</td>");
			exportWriter.write("</tr>\n");
			exportWriter.write("<tr>\n");
			exportWriter.write("\t<td>"+"Amount In wallet"+"</td>");
			exportWriter.write("\t<td>"+"Rs "+walletBalance+"</td>");
			exportWriter.write("</tr>\n");
			for(int i=0; i<numBanks; i++)
			{
				exportWriter.write("<tr>\n");
				exportWriter.write("\t<td>"+"Amount In "+bankNames.get(i)+"</td>");
				exportWriter.write("\t<td>"+"Rs "+bankBalances.get(i)+"</td>");
				exportWriter.write("</tr>\n");
			}
			exportWriter.write("</table>");
			
			exportWriter.close();
			
			Toast.makeText(getApplicationContext(), "Data Has Been Exported Successfully", Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), "Error In Saving To File\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void clearData()
	{
		try
		{
			// Empty the Particulars
			expenditureFolderName="Expenditure List/.temp";
			prefFileName="preferences.txt";
			walletFileName="wallet_info.txt";
			particularsFileName="particulars.txt";
			amountFileName="amount.txt";
			dateFileName="date.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			prefFile=new File(expenditureFolder, prefFileName);
			walletFile=new File(expenditureFolder, walletFileName);
			particularsFile=new File(expenditureFolder, particularsFileName);
			amountFile=new File(expenditureFolder, amountFileName);
			dateFile=new File(expenditureFolder, dateFileName);
			
			BufferedWriter prefWriter = new BufferedWriter(new FileWriter(prefFile));
			BufferedWriter walletWriter = new BufferedWriter(new FileWriter(walletFile));
			BufferedWriter particularsWriter = new BufferedWriter(new FileWriter(particularsFile));
			BufferedWriter amountWriter = new BufferedWriter(new FileWriter(amountFile));
			BufferedWriter dateWriter=new BufferedWriter(new FileWriter(dateFile));
			
			prefWriter.write(numBanks+"\n");
			prefWriter.write("0"+"\n");
			walletWriter.write("wallet_balance=Rs"+walletBalance+"\n");
			walletWriter.write("amount_spent=Rs0"+"\n");
			particularsWriter.write("");
			amountWriter.write("");
			dateWriter.write("");
			
			prefWriter.close();
			walletWriter.close();
			particularsWriter.close();
			amountWriter.close();
			dateWriter.close();
			
			Toast.makeText(getApplicationContext(), "Data Has Been Cleared Successfully", Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			Toast.makeText(getApplicationContext(), "Error In Clearing Data\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
