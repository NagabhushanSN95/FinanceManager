package com.chaturvedi.financemanager;

import com.chaturvedi.financemanager.database.DatabaseManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExtrasActivity extends Activity
{
	private Intent exportIntent;
	private Intent aboutUsIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_extras);
		
		buildLayout();
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
		exportIntent = new Intent(this, ExportActivity.class);
		exportLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(exportIntent);
			}
		});
		
		LinearLayout backupLayout = (LinearLayout) findViewById(R.id.layout_backup);
		backupLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new BackupData(ExtrasActivity.this);
			}
		});
		
		LinearLayout restoreLayout = (LinearLayout) findViewById(R.id.layout_restore);
		restoreLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new RestoreData(ExtrasActivity.this);
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
