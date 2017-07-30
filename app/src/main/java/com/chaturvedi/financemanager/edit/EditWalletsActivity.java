package com.chaturvedi.financemanager.edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Wallet;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class EditWalletsActivity extends Activity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_BOTTOM_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_BALANCE_VIEWS;
	private int MARGIN_TOP_VIEWS;
	
	private LinearLayout parentLayout;
	private LayoutParams parentLayoutParams;
	private Button addWalletButton;
	
	private AlertDialog.Builder addWalletDialog;
	private EditText walletNameField;
	private EditText walletBalanceField;

	private static ArrayList<Wallet> wallets;
	private int contextMenuWalletNo;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_wallets);
		
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=screenHeight*5/100;
		MARGIN_BOTTOM_PARENT_LAYOUT=20;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*3/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*3/100;
		WIDTH_NAME_VIEWS=screenWidth*60/100;
		WIDTH_BALANCE_VIEWS=screenWidth*30/100;
		MARGIN_TOP_VIEWS=5;
		
		buildLayout();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuWalletNo = parentLayout.indexOfChild(view);
		menu.setHeaderTitle("Options For Wallet "+(contextMenuWalletNo +1));
		menu.add(0, view.getId(), 0, "Edit");
		menu.add(0, view.getId(), 0, "Delete");
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if(item.getTitle().equals("Edit"))
		{
			editWallet(contextMenuWalletNo);
			buildLayout();
		}
		else if(item.getTitle().equals("Delete"))
		{
			deleteWallet(contextMenuWalletNo);
			//buildLayout();
		}
		else
		{
			return false;
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
		
		parentLayout=(LinearLayout)findViewById(R.id.parentLayout);
		parentLayoutParams=(LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT,
				MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		parentLayout.removeAllViews();
		
		addWalletButton =(Button)findViewById(R.id.button_addWallet);
		addWalletButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildAddWalletDialog();
				addWalletDialog.show();
			}
		});

		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditWalletsActivity.this);
		DecimalFormat formatter = new DecimalFormat("#,##0.##");
		int numWallets = databaseAdapter.getNumVisibleWallets();
		wallets = databaseAdapter.getAllVisibleWallets();
		
		for(int i=0; i<numWallets; i++)
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_wallet, null);
			TextView walletNameView = (TextView) layout.findViewById(R.id.walletName);
			TextView walletBalanceView = (TextView) layout.findViewById(R.id.walletBalance);
			
			walletNameView.setText(wallets.get(i).getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			walletNameView.setLayoutParams(nameViewParams);
			
			walletBalanceView.setText(formatter.format(wallets.get(i).getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_BALANCE_VIEWS, 0, 0, 0);
			walletBalanceView.setLayoutParams(balanceViewParams);
			walletBalanceView.setGravity(Gravity.END);
			
			parentLayout.addView(layout);
			registerForContextMenu(layout);
		}
	}
	
	private void buildAddWalletDialog()
	{
		addWalletDialog = new AlertDialog.Builder(this);
		addWalletDialog.setTitle("Add A New Wallet");
		addWalletDialog.setMessage("Enter The Particulars");
		LayoutInflater layoutInflater=LayoutInflater.from(this);
		final LinearLayout addWalletLayout=(LinearLayout)layoutInflater.inflate(R.layout.dialog_add_wallet, null);
		addWalletDialog.setView(addWalletLayout);
		
		walletNameField = (EditText) addWalletLayout.findViewById(R.id.walletName);
		walletBalanceField = (EditText) addWalletLayout.findViewById(R.id.walletBalance);
		
		addWalletDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditWalletsActivity.this);
				int id = databaseAdapter.getIDforNextWallet();
				String walletName = walletNameField.getText().toString().trim();
				String walletBalance = walletBalanceField.getText().toString().trim();
				boolean dataCorrect = verifyData(walletName, walletBalance, null);
				
				if(dataCorrect)
				{
					Wallet wallet = new Wallet(id, walletName, Double.parseDouble(walletBalance), false);
					databaseAdapter.addWallet(wallet);
				}
				else
				{
					buildAddWalletDialog();
					walletNameField.setText(walletName+" ");
					walletBalanceField.setText(walletBalance);
					addWalletDialog.show();
				}
				buildLayout();
			}
		});
	}
	
	private void editWallet(final int walletNo)
	{
		//ArrayList<Bank> wallets = DatabaseManager.getAllBanks();
		final Wallet wallet = wallets.get(walletNo);
		buildAddWalletDialog();
		walletNameField.setText(wallet.getName());
		walletBalanceField.setText(String.valueOf(wallet.getBalance()));
		
		addWalletDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				int id = wallet.getID();
				String walletName = walletNameField.getText().toString().trim();
				String walletBalance = walletBalanceField.getText().toString().trim();
				boolean isDeleted = wallet.isDeleted();
				boolean dataCorrect = verifyData(walletName, walletBalance, wallet.getName());
				
				if(dataCorrect)
				{
					Wallet newWallet = new Wallet(id, walletName, Double.parseDouble(walletBalance), isDeleted);
					DatabaseAdapter.getInstance(EditWalletsActivity.this).updateWallet(newWallet);
					buildLayout();
				}
				else
				{
					buildAddWalletDialog();
					walletNameField.setText(walletName+" ");
					walletBalanceField.setText(walletBalance);
					addWalletDialog.show();
				}
			}
		});
		addWalletDialog.show();
	}

	private void deleteWallet(final int contextMenuWalletNo)
	{
		final Wallet wallet = wallets.get(contextMenuWalletNo);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(EditWalletsActivity.this);
		deleteDialog.setTitle("Delete Wallet");
		deleteDialog.setMessage("Are you sure you want to delete wallet '" + wallet.getName() + "'?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter.getInstance(EditWalletsActivity.this).deleteWallet(wallet.getID());
				wallets.remove(contextMenuWalletNo);
				parentLayout.removeViewAt(contextMenuWalletNo);
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private boolean verifyData(String walletName, String walletBalance, String origWalletName)
	{
		boolean dataCorrect;
		if(walletName.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Wallet Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(walletBalance.length()==0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Wallet Balance", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(origWalletName!= null && walletName.equals(origWalletName))
		{
			dataCorrect = true;
		}
		else if(DatabaseAdapter.getInstance(EditWalletsActivity.this).getWalletFromName(walletName) != null)
		{
			Toast.makeText(getApplicationContext(), "Please Enter A Unique Wallet Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
}
