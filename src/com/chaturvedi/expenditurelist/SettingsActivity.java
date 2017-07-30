package com.chaturvedi.expenditurelist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class SettingsActivity extends Activity
{
	
	private CheckBox splashCheckBox;
	private static boolean enableSplash=true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_settings);
		}
		else
		{
			setContentView(R.layout.activity_settings);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		readFile();
		splashCheckBox=(CheckBox)findViewById(R.id.checkBox_splash);
		splashCheckBox.setChecked(enableSplash);
	}
	
	@Override
	public void onBackPressed()
	{
		saveData();
		super.onBackPressed();
	}

	private void readFile()
	{
		String line;
		
		try
		{
			String expenditureFolderName = "Expenditure List/.temp";
			String settingsFileName= "settings.txt";
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			File settingsFile=new File(expenditureFolder, settingsFileName);
			BufferedReader settingsReader=new BufferedReader(new FileReader(settingsFile));
			line=settingsReader.readLine();
			if(line.contains("false"))
				enableSplash=false;
			else
				enableSplash=true;
			settingsReader.close();
		}
		catch(Exception e)
		{
			enableSplash=true;
			Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void saveData()
	{
		String line;
		try
		{
			String expenditureFolderName = "Expenditure List/.temp";
			String settingsFileName= "settings.txt";
			File expenditureFolder = new File(Environment.getExternalStoragePublicDirectory("Chaturvedi"), expenditureFolderName);
			if(!expenditureFolder.exists())
				expenditureFolder.mkdirs();
			File settingsFile=new File(expenditureFolder, settingsFileName);
			BufferedWriter settingsWriter=new BufferedWriter(new FileWriter(settingsFile));
			
			enableSplash=splashCheckBox.isChecked();
			line="enable_splash="+enableSplash;
			settingsWriter.write(line);
			settingsWriter.close();
		}
		catch(Exception e)
		{
			enableSplash=true;
			Toast.makeText(this, "Error In Reading File", Toast.LENGTH_SHORT).show();
		}
	}
}
