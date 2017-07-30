package com.chaturvedi.financemanager.edit;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.R.id;
import com.chaturvedi.financemanager.R.layout;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EditActivity extends Activity
{
	private Intent editWalletBalanceIntent;
	private Intent editBanksIntent;
	private Intent editExpTypesIntent;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
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
		
		editWalletBalanceIntent = new Intent(this, EditWalletBalanceActivity.class);
		editBanksIntent = new Intent(this, EditBanksActivity.class);
		editExpTypesIntent = new Intent(this, EditExpTypesActivity.class);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:						// Up Button in Action Bar
				NavUtils.navigateUpFromSameTask(EditActivity.this);
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
		
		LinearLayout editWalletBalanceLayout = (LinearLayout) findViewById(R.id.layout_editWalletBalance);
		editWalletBalanceLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(editWalletBalanceIntent);
			}
		});
		
		LinearLayout editBanksLayout = (LinearLayout) findViewById(R.id.layout_editBanks);
		editBanksLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(editBanksIntent);
			}
		});
		
		LinearLayout editExpTypesLayout = (LinearLayout) findViewById(R.id.layout_editExpTypes);
		editExpTypesLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(editExpTypesIntent);
			}
		});
	}
}
