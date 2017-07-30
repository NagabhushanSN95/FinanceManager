package com.chaturvedi.financemanager.updates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class Update68To75
{
	private Context context;
	
	public Update68To75(Context cxt)
	{
		context = cxt;
		
		updateBackups();
	}
	
	private void updateBackups()
	{
		int backupVersionNo = 0;
		int numTransactions = 0;
		int numBanks 		= 0;
		int numCountersRows = 0;
		
		String backupFolderName = "Finance Manager/BackupTrial";
		String extension = ".snb";
		File backupFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), backupFolderName);
		if(!backupFolder.exists())
			return;
		
		String keyDataFileName = "Key Data";
		File keyDataFile = new File(backupFolder, keyDataFileName+extension);
		if(!keyDataFile.exists())
			return;
		
		// Append NumExpTypes and NumTemplates to Key Data File
		try
		{
			BufferedReader keyDataReader = new BufferedReader(new FileReader(keyDataFile));
			ArrayList<String> lines = new ArrayList<String>();
			for(int i=0; i<4; i++)
			{
				lines.add(keyDataReader.readLine().trim());
			}
			backupVersionNo = Integer.parseInt(lines.get(0));
			numTransactions = Integer.parseInt(lines.get(1));
			numBanks 		= Integer.parseInt(lines.get(2));
			numCountersRows = Integer.parseInt(lines.get(3));
			keyDataReader.close();
			
			lines.add(5 + "");	// NumExpTypes
			lines.add(0 + "");	// NumTemplates
			keyDataFile = new File(backupFolder, keyDataFileName+extension);
			BufferedWriter keyDataWriter = new BufferedWriter(new FileWriter(keyDataFile));
			for(int i=0; i<lines.size(); i++)
			{
				keyDataWriter.write(lines.get(i).trim() + "\n");
			}
			keyDataWriter.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Error Found in Update68\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		catch (IOException e)
		{
			Toast.makeText(context, "Error Found in Update68\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		
		// In Transactions File, add ID before each transaction
		String transactionsFileName = "Key Data";
		File transactionsFile = new File(backupFolder, transactionsFileName+extension);
		try
		{
			BufferedReader transactionsReader = new BufferedReader(new FileReader(transactionsFile));
			ArrayList<String> lines = new ArrayList<String>();
			for(int i=0; i<numTransactions*9; i++)
			{
				lines.add(transactionsReader.readLine());
			}
			transactionsReader.close();
			
			BufferedWriter transactionsWriter = new BufferedWriter(new FileWriter(transactionsFile));
			for(int i=0; i<numTransactions*9; i++)
			{
				if(i%9==0)
					transactionsWriter.write((i/9)+1);
				transactionsWriter.write(lines.get(i) + "\n");
			}
			transactionsWriter.close();
		}
		catch(FileNotFoundException e)
		{
			Toast.makeText(context, "Error Found in Update68\nPlease Report To The Developer", 
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
}
