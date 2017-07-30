package com.chaturvedi.financemanager;


import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.customviews.IndefiniteWaitDialog;
import com.chaturvedi.financemanager.database.BackupManager;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Date;
import com.chaturvedi.financemanager.database.ExportManager;
import com.chaturvedi.financemanager.database.RestoreManager;
import com.chaturvedi.financemanager.help.AboutActivity;

public class ExtrasActivity extends Activity
{
	private Intent aboutUsIntent;
	
	// Fields required for exportData()
	private ArrayList<String> months;
	private long exportLongMonth;
	private EditText exportFileNameField;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_extras);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		
		buildLayout();
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(ExtrasActivity.this);
				return true;
		}
		return true;
	}
	
	protected void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		LinearLayout exportLayout = (LinearLayout) findViewById(R.id.layout_export);
		exportLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				exportData();
			}
		});
		
		LinearLayout backupLayout = (LinearLayout) findViewById(R.id.layout_backup);
		backupLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				backupData();
			}
		});
		
		LinearLayout restoreLayout = (LinearLayout) findViewById(R.id.layout_restore);
		restoreLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				restoreData();
			}
		});
		
		LinearLayout clearLayout = (LinearLayout) findViewById(R.id.layout_clear);
		clearLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				clearData();
			}
		});
		
		LinearLayout aboutDeveloperLayout = (LinearLayout) findViewById(R.id.layout_aboutUs);
		aboutUsIntent = new Intent(this, AboutActivity.class);
		aboutDeveloperLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(aboutUsIntent);
			}
		});
	}
	
	private void exportData()
	{
		Spinner monthsList;
		LayoutInflater exportDialogLayout;
		View exportDialogView;
		AlertDialog.Builder exportDialog;
		
		exportDialogLayout=LayoutInflater.from(this);
		exportDialogView=exportDialogLayout.inflate(R.layout.dialog_export, null);
		exportFileNameField=(EditText)exportDialogView.findViewById(R.id.editText_export_fileName);
		
		monthsList = (Spinner) exportDialogView.findViewById(R.id.monthsList);
		months = DatabaseManager.getExportableMonths(); // Assigned at the top
		months.add(0, "Current Month");					// Insert The Current Month at the top of the list
		monthsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, months));
		monthsList.setSelection(0);
		monthsList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int monthNo, long arg3)
			{
				if(monthNo == 0)		// Current Month
				{
					Calendar calendar=Calendar.getInstance();
					// Calendar return 0 for Jan, 1 for Feb,.. But Date.getMonthName requires 1 for Jan,..
					int currentMonthNo = calendar.get(Calendar.MONTH) + 1;
					String currentMonthName=Date.getMonthName(currentMonthNo);
					int currentYear=calendar.get(Calendar.YEAR);
					String exportFileName=currentMonthName + " - " + currentYear+".doc";
					exportFileNameField.setText(exportFileName);
					exportLongMonth = currentYear*100 + currentMonthNo;		//201501 for Jan 2015
				}
				else
				{
					String exportFileName = months.get(monthNo) + ".doc";
					exportFileNameField.setText(exportFileName);
					//months.get(monthNo) ==> January - 2015 
					//exportLongMonth	  ==> 201501
					exportLongMonth = Date.getLongMonth(months.get(monthNo));
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
				String exportFileName=exportFileNameField.getText().toString();
				new ExportManager().export(getApplicationContext(), exportFileName, exportLongMonth);
			}
		});
		exportDialog.setNegativeButton("Cancel", null);
		exportDialog.setCancelable(false);
		exportDialog.create();
		exportDialog.show();
	}
	
	private void backupData()
	{
		AlertDialog.Builder backupDialog = new AlertDialog.Builder(this);
		backupDialog.setTitle("Back-Up Data");
		backupDialog.setMessage("Are You Sure To Backup Data And Overwrite Previous Backup (If Any)?");
		backupDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				new BackupManager(ExtrasActivity.this).manualBackup();	// Backs Up Data to Backups Folder
			}
		});
		backupDialog.setNegativeButton("Cancel", null);
		backupDialog.show();
	}
	
	private void restoreData()
	{
		AlertDialog.Builder restoreDialog = new AlertDialog.Builder(this);
		restoreDialog.setTitle("Restore Data");
		restoreDialog.setMessage("Are You Sure To Restore Data From SD Card And Overwrite Existing Data?");
		restoreDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				IndefiniteWaitDialog restoreDialogBuilder = new IndefiniteWaitDialog(ExtrasActivity.this);
				restoreDialogBuilder.setWaitText("Restoring Data. This may take few minutes depending on the Size of your Data");
				final AlertDialog restoreDialog = restoreDialogBuilder.show();
				/** Restore in a seperate (non-ui) thread*/
				Thread restoreThread = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						RestoreManager restoreManager = new RestoreManager(ExtrasActivity.this);
						int result = restoreManager.readBackups("Finance Manager/Backups");
						if(result == 0)
						{
							DatabaseManager.setWalletBalance(restoreManager.getWalletBalance());
							DatabaseManager.setAllTransactions(restoreManager.getAllTransactions());
							DatabaseManager.setAllBanks(restoreManager.getAllBanks());
							int numExpTypesInDatabase = DatabaseManager.getNumExpTypes();
							DatabaseManager.setAllExpenditureTypes(restoreManager.getAllExpTypes());
							if(restoreManager.getNumExpTypes() != numExpTypesInDatabase)
							{
								Log.d("ExtrasActivity/restoreData()","Readjusting Counters Table: SD Card: " + 
										restoreManager.getNumExpTypes() + "|Database: " + numExpTypesInDatabase);
								DatabaseManager.readjustCountersTable();
							}
							DatabaseManager.setAllCounters(restoreManager.getAllCounters());
							DatabaseManager.setAllTemplates(restoreManager.getAllTemplates());
							
							restoreDialog.dismiss();
							//Toast.makeText(getApplicationContext(), "Data Restored Successfully", Toast.LENGTH_LONG).show();
						}
						else if(result == 1)
						{
							Toast.makeText(getApplicationContext(), "No Backups Were Found.\n" + 
									"Make sure the Backup Files are located in\n" + "Chaturvedi/Finance Manager Folder", 
									Toast.LENGTH_LONG).show();
						}
						else if(result == 2)
						{
							Toast.makeText(getApplicationContext(), "Old Data. Cannot be Restored. Sorry!", 
							Toast.LENGTH_LONG).show();
						}
						else if(result == 3)
						{
							Toast.makeText(getApplicationContext(), "Error in Restoring Data\nControl Entered Catch Block",
									Toast.LENGTH_LONG).show();
						}
					}
				});
				restoreThread.start();
			}
		});
		restoreDialog.setNegativeButton("Cancel", null);
		restoreDialog.show();
	}
	
	private void clearData()
	{
		AlertDialog.Builder clearDialog = new AlertDialog.Builder(this);
		clearDialog.setTitle("Clear All Data");
		clearDialog.setMessage("Are You Sure To Delete All Your Transactions?");
		clearDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseManager.clearDatabase();
			}
		});
		clearDialog.setNegativeButton("Cancel", null);
		clearDialog.show();
	}
}
