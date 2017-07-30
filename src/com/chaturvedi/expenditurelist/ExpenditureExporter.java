package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Environment;

public class ExpenditureExporter
{
	private String expenditureFolderName;
	private String prefFileName;
	private String walletFileName;
	private String bankFileName;
	private String particularsFileName;
	private String amountFileName;
	private String exportFileName;
	private File expenditureFolder;
	private File prefFile;
	private File walletFile;
	private File bankFile;
	private File particularsFile;
	private File amountFile;
	private File exportFile;
	private BufferedReader prefReader;
	private BufferedReader walletReader;
	private BufferedReader bankReader;
	private BufferedReader particularsReader;
	private BufferedReader amountReader;
	private BufferedWriter exportWriter;
	
	private int numBanks;
	private int numEntries;
	private int walletBalance;
	private int amountSpent;
	private ArrayList<String> bankNames;
	private ArrayList<Integer> bankBalances;
	private ArrayList<String> particulars;
	private ArrayList<Integer> amounts;
	
	private Calendar calendar;
	private String currentYear;
	private String currentMonth;
	private BufferedWriter prefWriter;
	private BufferedWriter walletWriter;
	
	public void export()
	{
		calendar=Calendar.getInstance();
		currentMonth=getMonth(calendar.get(Calendar.MONTH));
		currentYear=calendar.get(Calendar.YEAR)+"";
		exportFileName=currentMonth+"-"+currentYear+".txt";
		readFile();
		saveData();
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
			//Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void saveData()
	{
		String spaces="";
		try
		{
			expenditureFolderName="Expenditure List";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			exportFile=new File(expenditureFolder, exportFileName);
			exportWriter=new BufferedWriter(new FileWriter(exportFile));
			for(int i=0; i<numEntries; i++)
			{
				spaces="";
				for(int j=0; j<(20-(particulars.get(i).length()/4)); j++)
					spaces+="\t";
				exportWriter.write(particulars.get(i)+spaces);
				exportWriter.write(amounts.get(i)+"\n");
			}
			exportWriter.write("\n\n");
			exportWriter.write("Total Amount Spent In This Month=Rs"+amountSpent+"\n");
			exportWriter.write("Amount In wallet=Rs"+walletBalance+"\n");
			for(int i=0; i<numBanks; i++)
				exportWriter.write("Amount In "+bankNames.get(i)+"=Rs"+bankBalances.get(i)+"\n");
			exportWriter.close();
			
			// Empty the Particulars
			expenditureFolderName="Expenditure List/.temp";
			prefFileName="preferences.txt";
			walletFileName="wallet_info.txt";
			particularsFileName="particulars.txt";
			amountFileName="amount.txt";
			
			expenditureFolder=new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			
			prefFile=new File(expenditureFolder, prefFileName);
			walletFile=new File(expenditureFolder, walletFileName);
			particularsFile=new File(expenditureFolder, particularsFileName);
			amountFile=new File(expenditureFolder, amountFileName);
			
			prefWriter=new BufferedWriter(new FileWriter(prefFile));
			walletWriter=new BufferedWriter(new FileWriter(walletFile));
			
			prefWriter.write(numBanks+"\n");
			prefWriter.write("0"+"\n");
			walletWriter.write("wallet_balance=Rs"+walletBalance+"\n");
			walletWriter.write("amount_spent=Rs0"+"\n");
			prefWriter.close();
			walletWriter.close();
		}
		catch(Exception e)
		{
			//Toast.makeText(this, "Error In Saving To File", Toast.LENGTH_SHORT).show();
		}
	}
}
