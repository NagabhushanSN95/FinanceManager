package com.chaturvedi.financemanager.extras;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.customviews.IndefiniteWaitDialogBuilder;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.extras.export.ExportActivity;
import com.chaturvedi.financemanager.functions.Constants;
import com.chaturvedi.financemanager.help.AboutActivity;

public class ExtrasActivity extends Activity
{
	private static final int CODE_FILE_CHOOSER = 102;
	private static final int EXPORT_REQUEST_PERMISSION = 201;
	private static final int BACKUP_REQUEST_PERMISSION = 202;
	private static final int RESTORE_REQUEST_PERMISSION = 203;


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_extras);
		if (getActionBar() != null)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
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

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults)
	{
		boolean permissionGranted =
				(grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
		switch (requestCode)
		{
			case EXPORT_REQUEST_PERMISSION:
				if (permissionGranted)
				{
					startExportActivity();
				}
				else
				{
					Toast.makeText(ExtrasActivity.this, "Please provide Write permission to " +
							"export data to SD card", Toast.LENGTH_LONG).show();
				}
				break;

			case BACKUP_REQUEST_PERMISSION:
				if (permissionGranted)
				{
					backupData();
				}
				else
				{
					Toast.makeText(ExtrasActivity.this, "Please provide Write permission to " +
							"backup data to SD card", Toast.LENGTH_LONG).show();
				}
				break;

			case RESTORE_REQUEST_PERMISSION:
				if (permissionGranted)
				{
					chooseRestoreFile();
				}
				else
				{
					Toast.makeText(ExtrasActivity.this, "Please provide Read permission to " +
							"restore data from SD card", Toast.LENGTH_LONG).show();
				}
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		//noinspection SwitchStatementWithTooFewBranches
		switch (requestCode)
		{
			case CODE_FILE_CHOOSER:
				if(resultCode == RESULT_OK)
				{
					// Get the Uri of the selected file
					Uri uri = intent.getData();
					restoreData(uri);
				}
		}
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
				checkExportPermissions();
			}
		});

		LinearLayout backupLayout = (LinearLayout) findViewById(R.id.layout_backup);
		backupLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				checkBackupPermissions();
			}
		});

		LinearLayout restoreLayout = (LinearLayout) findViewById(R.id.layout_restore);
		restoreLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				checkRestorePermissions();
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
		aboutDeveloperLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent aboutUsIntent = new Intent(ExtrasActivity.this, AboutActivity.class);
				startActivity(aboutUsIntent);
			}
		});
	}

	private void backupData()
	{
		IndefiniteWaitDialogBuilder backupDialogBuilder = new IndefiniteWaitDialogBuilder
				(ExtrasActivity.this);
		backupDialogBuilder.setTitle("Backing Up Data to SD Card");
		backupDialogBuilder.setWaitText("This may take few minutes depending on the Size of your Data");
		backupDialogBuilder.setCancelable(false);
		final AlertDialog backupDialog = backupDialogBuilder.show();

		// Define Handler to pass messages
		final Handler backupResultHandler = new Handler(Looper.getMainLooper())
		{
			@Override
			public void handleMessage(Message backupResultMessage)
			{
				String resultTitle;
				String resultMessage;
				switch (backupResultMessage.what)
				{
					case Constants.ACTION_BACKUP_SUCCESSFUL:
						String[] result = (String[]) backupResultMessage.obj;
						resultTitle = "Backup Successful";
						resultMessage = "Backup Folder: " + result[0] + "\n\n" +
								"Backup Filename: " + result[1] + "\n";
						break;

					case Constants.ACTION_BACKUP_FAILURE:
						resultTitle = "Backup Failed";
						resultMessage = "Please Try again.\n\n" +
								"If the problem persists, please contact Developer";
						break;

					default:
						resultTitle = "Backup Failed";
						resultMessage = "Unknown result. Please Try again. If the problem persists, please contact Developer";
				}

				AlertDialog.Builder resultDialogBuilder = new AlertDialog.Builder(ExtrasActivity.this);
				resultDialogBuilder.setTitle(resultTitle);
				resultDialogBuilder.setMessage(resultMessage);
				resultDialogBuilder.setPositiveButton("OK", null);
				resultDialogBuilder.show();
			}
		};

		/* Backup in a separate non-ui thread */
		Thread backupThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				String[] result = new BackupManager(ExtrasActivity.this).manualBackup();    // Backs Up Data to Backups Folder
				backupDialog.dismiss();

				if (result != null)
				{
					Message backupResultMessage = backupResultHandler.obtainMessage(
							Constants.ACTION_BACKUP_SUCCESSFUL, result);
					backupResultMessage.sendToTarget();
				}
				else
				{
					Message backupResultMessage = backupResultHandler.obtainMessage(
							Constants.ACTION_BACKUP_FAILURE);
					backupResultMessage.sendToTarget();
				}
			}
		});
		backupThread.start();
	}

	private void checkExportPermissions()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			// Permission is not granted
			ActivityCompat.requestPermissions(ExtrasActivity.this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					EXPORT_REQUEST_PERMISSION);
		}
		else
		{
			// Permission Granted
			startExportActivity();
		}
	}

	private void startExportActivity()
	{
		Intent exportIntent = new Intent(ExtrasActivity.this, ExportActivity.class);
		startActivity(exportIntent);
	}

	private void checkBackupPermissions()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			// Permission is not granted
			ActivityCompat.requestPermissions(ExtrasActivity.this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					BACKUP_REQUEST_PERMISSION);
		}
		else
		{
			// Permission Granted
			backupData();
		}
	}

	private void checkRestorePermissions()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			// Permission is not granted
			ActivityCompat.requestPermissions(ExtrasActivity.this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					RESTORE_REQUEST_PERMISSION);
		}
		else
		{
			// Permission Granted
			chooseRestoreFile();
		}
	}

	private void chooseRestoreFile()
	{
		Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		fileIntent.setType("*/*");
		startActivityForResult(fileIntent, CODE_FILE_CHOOSER);
	}

	private void restoreData(final Uri fileUri)
	{
		// TODO: CREATE a new Activity for Restoring. Give 2 options there. To select Data Backup file

		// and settings backup file. Restore both

		IndefiniteWaitDialogBuilder restoreDialogBuilder = new IndefiniteWaitDialogBuilder(this);
		restoreDialogBuilder.setTitle("Restoring Data");
		restoreDialogBuilder.setWaitText("This may take few minutes depending on the Size of your Data");
		restoreDialogBuilder.setCancelable(false);
		final AlertDialog restoreDialog = restoreDialogBuilder.show();

		// Todo: Restore in a seperate (non-ui) thread
		Thread restoreThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// Restore Data
				RestoreManager restoreManager = new RestoreManager(ExtrasActivity.this, fileUri, true);
				int result = restoreManager.getResult();
				if(result == 0)
				{
					DatabaseManager.clearDatabase(ExtrasActivity.this);
					DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(ExtrasActivity.this);
					// Remove existing data
					databaseAdapter.deleteAllWallets();
					databaseAdapter.deleteAllBanks();
					databaseAdapter.deleteAllTransactions();
					databaseAdapter.deleteAllExpenditureTypes();
					databaseAdapter.deleteAllCountersRows();
					databaseAdapter.deleteAllTemplates();

					// Add new data
					databaseAdapter.addAllWallets(restoreManager.getAllWallets());
					databaseAdapter.addAllBanks(restoreManager.getAllBanks());
					databaseAdapter.addAllTransactions(restoreManager.getAllTransactions());
					databaseAdapter.addAllExpenditureTypes(restoreManager.getAllExpTypes());
					// Initially, in DatabaseAdapter, Counters Table is configured to have 5 Exp Types By Deafult
					// If Number of Exp Types is not 5, then readjust it to have more columns
					if(restoreManager.getNumExpTypes() != 5)
					{
						Log.d("restoreData()","Readjusting Counters Table");
						databaseAdapter.readjustCountersTable();
					}

					databaseAdapter.addAllCountersRows(restoreManager.getAllCounters());
					databaseAdapter.addAllTemplates(restoreManager.getAllTemplates());

					restoreDialog.dismiss();
				}
				else if(result == 1)
				{
					// TODO: Send these messages using a handler. Display Toast in that handler
					/*Toast.makeText(getApplicationContext(), "No Backups Were Found.\nMake sure the Backup Files " +
							"are located in\nChaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();*/
					Log.d("restoreData()", "No Backups Were Found.\nPlease select a backup file created by Finance Manager App only");
					restoreDialog.dismiss();
				}
				else if(result == 2)
				{
					/*Toast.makeText(getApplicationContext(), "Old Data. Cannot be Restored. Sorry!",
							Toast.LENGTH_LONG).show();*/
					Log.d("restoreData()", "Old Data. Cannot be Restored. Sorry!");
					restoreDialog.dismiss();
				}
				else if(result == 3)
				{
					/*Toast.makeText(getApplicationContext(), "Error in Restoring Data\nControl Entered Catch Block",
							Toast.LENGTH_LONG).show();*/
					Log.d("restoreData()", "Error in Restoring Data\nControl Entered Catch Block");
					restoreDialog.dismiss();
				}
			}
		});
		restoreThread.start();
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
				DatabaseManager.clearDatabase(ExtrasActivity.this);
			}
		});
		clearDialog.setNegativeButton("Cancel", null);
		clearDialog.show();
	}
}
