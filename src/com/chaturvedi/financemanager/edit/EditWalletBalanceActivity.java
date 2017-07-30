package com.chaturvedi.financemanager.edit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.R.id;
import com.chaturvedi.financemanager.R.layout;
import com.chaturvedi.financemanager.database.DatabaseManager;

public class EditWalletBalanceActivity extends Activity
{
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_wallet_balance);
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
			case android.R.id.home:						// Up Button in Action Bar
				NavUtils.navigateUpFromSameTask(EditWalletBalanceActivity.this);
				return true;
		}
		return true;
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		final TextView walletBalanceView = (TextView) findViewById(R.id.textView_walletBalance);
		walletBalanceView.setText("The Amount In Your Wallet is: " + DatabaseManager.getWalletBalance());
		
		Button editWalletBalanceButton = (Button) findViewById(R.id.button_editWalletBalance);
		editWalletBalanceButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder walletBalanceDialog = new AlertDialog.Builder(EditWalletBalanceActivity.this);
				LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
				View walletBalanceDialogView = inflater.inflate(R.layout.dialog_edit_wallet_balance, null);
				TextView instructionView = (TextView) walletBalanceDialogView.findViewById(R.id.textView_instruction);
				instructionView.setText("Enter The Amount in your Wallet");
				final EditText walletBalanceField = 
						(EditText) walletBalanceDialogView.findViewById(R.id.editText_walletBalance);
				walletBalanceField.setText("" + DatabaseManager.getWalletBalance());
				walletBalanceDialog.setView(walletBalanceDialogView);
				walletBalanceDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						double newWalletBalance = Double.parseDouble(walletBalanceField.getText().toString());
						DatabaseManager.setWalletBalance(newWalletBalance);
						walletBalanceView.setText("The Amount In Your Wallet is: " + DatabaseManager.getWalletBalance());
					}
				});
				walletBalanceDialog.setNegativeButton("Cancel", null);
				walletBalanceDialog.show();
			}
		});
	}
}
