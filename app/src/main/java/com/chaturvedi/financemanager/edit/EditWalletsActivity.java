package com.chaturvedi.financemanager.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.Wallet;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditWalletsActivity extends Activity
{
	public static final int ID_EDIT_WALLET = 1101;
	public static final int ID_DELETE_WALLET = 1102;
	private static final int ID_RESTORE_WALLET = 1103;
	
	private int MARGIN_TOP_PARENT_LAYOUT;
	private int MARGIN_BOTTOM_PARENT_LAYOUT;
	private int MARGIN_LEFT_PARENT_LAYOUT;
	private int MARGIN_RIGHT_PARENT_LAYOUT;
	private int WIDTH_NAME_VIEWS;
	private int WIDTH_BALANCE_VIEWS;
	
	private LinearLayout activeWalletsLayout;
	private LinearLayout deletedWalletsLayout;
	
	private AlertDialog.Builder addWalletDialog;
	private EditText walletNameField;
	private EditText walletBalanceField;
	
	private boolean showDeletedWallets;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_wallets);
		// Provide Up Button in Action Bar
		if (getActionBar() != null)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		calculateDimensions();
		buildLayout();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds childItems to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_edit_wallets, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_toggleDisplayDeletedWallets:
				showDeletedWallets = !showDeletedWallets;
				if (showDeletedWallets)
				{
					item.setTitle("Hide Deleted Wallets");
					deletedWalletsLayout.setVisibility(View.VISIBLE);
				}
				else
				{
					item.setTitle("Show Deleted Wallets");
					deletedWalletsLayout.setVisibility(View.GONE);
				}
				return true;
			
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		String walletName = ((TextView) view.findViewById(R.id.walletName)).getText().toString();
		Wallet wallet = DatabaseAdapter.getInstance(this).getWalletFromName(walletName);
		menu.setHeaderTitle("Options");
		if (!wallet.isDeleted())
		{
			menu.add(Menu.NONE, ID_EDIT_WALLET, Menu.NONE, "Edit \"" + walletName + "\"");
			menu.add(Menu.NONE, ID_DELETE_WALLET, Menu.NONE, "Delete \"" + walletName + "\"");
		}
		else
		{
			menu.add(Menu.NONE, ID_RESTORE_WALLET, Menu.NONE, "Restore \"" + walletName + "\"");
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		int walletId = getWalletIdFromMenuItem(item);
		if (walletId != -1)
		{
			switch (item.getItemId())
			{
				case ID_EDIT_WALLET:
					editWallet(walletId);
					rebuildLayout();
					break;
				
				case ID_DELETE_WALLET:
					deleteWallet(walletId);
					break;
				
				case ID_RESTORE_WALLET:
					restoreWallet(walletId);
					break;
				
				default:
					return super.onContextItemSelected(item);
			}
		}
		else
		{
			Toast.makeText(this, "Unable to detect selected Wallet. Please try again. Please " +
					"contact developer if the problem persists", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private void calculateDimensions()
	{
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int screenWidth = displayMetrics.widthPixels;
		int screenHeight = displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT = screenHeight * 5 / 100;
		MARGIN_BOTTOM_PARENT_LAYOUT = 20;
		MARGIN_LEFT_PARENT_LAYOUT = screenWidth * 3 / 100;
		MARGIN_RIGHT_PARENT_LAYOUT = screenWidth * 3 / 100;
		WIDTH_NAME_VIEWS = screenWidth * 60 / 100;
		WIDTH_BALANCE_VIEWS = screenWidth * 30 / 100;
	}
	
	private void rebuildLayout()
	{
		activeWalletsLayout.removeAllViews();
		deletedWalletsLayout.removeAllViews();
		buildLayout();
	}
	
	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if (0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
		
		LinearLayout parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		LayoutParams parentLayoutParams = (LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT,
				MARGIN_RIGHT_PARENT_LAYOUT,
				MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		Button addWalletButton = (Button) findViewById(R.id.button_addWallet);
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
		
		activeWalletsLayout = (LinearLayout) findViewById(R.id.activeWalletsLayout);
		for (Wallet wallet : databaseAdapter.getAllVisibleWallets())
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout
					.layout_display_wallet, activeWalletsLayout, false);
			TextView walletNameView = (TextView) layout.findViewById(R.id.walletName);
			TextView walletBalanceView = (TextView) layout.findViewById(R.id.walletBalance);
			
			walletNameView.setText(wallet.getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams
					.WRAP_CONTENT);
			walletNameView.setLayoutParams(nameViewParams);
			
			walletBalanceView.setText(formatter.format(wallet.getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams
					.WRAP_CONTENT);
			walletBalanceView.setLayoutParams(balanceViewParams);
			walletBalanceView.setGravity(Gravity.END);
			
			activeWalletsLayout.addView(layout);
			registerForContextMenu(layout);
		}
		
		deletedWalletsLayout = (LinearLayout) findViewById(R.id.deletedWalletsLayout);
		for (Wallet wallet : databaseAdapter.getAllDeletedWallets())
		{
			LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout
					.layout_display_wallet, activeWalletsLayout, false);
			TextView walletNameView = (TextView) layout.findViewById(R.id.walletName);
			TextView walletBalanceView = (TextView) layout.findViewById(R.id.walletBalance);
			
			walletNameView.setText(wallet.getName());
			LayoutParams nameViewParams = new LayoutParams(WIDTH_NAME_VIEWS, LayoutParams
					.WRAP_CONTENT);
			walletNameView.setLayoutParams(nameViewParams);
			
			walletBalanceView.setText(formatter.format(wallet.getBalance()));
			LayoutParams balanceViewParams = new LayoutParams(WIDTH_BALANCE_VIEWS, LayoutParams
					.WRAP_CONTENT);
			walletBalanceView.setLayoutParams(balanceViewParams);
			walletBalanceView.setGravity(Gravity.END);
			
			deletedWalletsLayout.addView(layout);
			registerForContextMenu(layout);
		}
	}
	
	private void buildAddWalletDialog()
	{
		addWalletDialog = new AlertDialog.Builder(this);
		addWalletDialog.setTitle("Add A New Wallet");
		addWalletDialog.setMessage("Enter The Particulars");
		LayoutInflater layoutInflater = LayoutInflater.from(this);
		@SuppressLint("InflateParams")
		// For passing null to inflate method. Justified here, because there is no parent view
		final LinearLayout addWalletLayout = (LinearLayout) layoutInflater.inflate(R.layout
				.dialog_add_wallet, null);
		addWalletDialog.setView(addWalletLayout);
		
		walletNameField = (EditText) addWalletLayout.findViewById(R.id.walletName);
		walletBalanceField = (EditText) addWalletLayout.findViewById(R.id.walletBalance);
		
		addWalletDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditWalletsActivity
						.this);
				int id = databaseAdapter.getIDforNextWallet();
				String walletName = walletNameField.getText().toString().trim();
				String walletBalance = walletBalanceField.getText().toString().trim();
				boolean dataCorrect = verifyData(walletName, walletBalance, null);
				
				if (dataCorrect)
				{
					Wallet wallet = new Wallet(id, walletName, Double.parseDouble(walletBalance),
							false);
					databaseAdapter.addWallet(wallet);
				}
				else
				{
					buildAddWalletDialog();
					walletNameField.setText(walletName);
					walletBalanceField.setText(walletBalance);
					addWalletDialog.show();
				}
				rebuildLayout();
			}
		});
	}
	
	private void editWallet(int walletId)
	{
		final Wallet wallet = DatabaseAdapter.getInstance(this).getWallet(walletId);
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
				
				if (dataCorrect)
				{
					Wallet newWallet = new Wallet(id, walletName, Double.parseDouble
							(walletBalance), isDeleted);
					DatabaseAdapter.getInstance(EditWalletsActivity.this).updateWallet(newWallet);
					rebuildLayout();
				}
				else
				{
					buildAddWalletDialog();
					walletNameField.setText(walletName);
					walletBalanceField.setText(walletBalance);
					addWalletDialog.show();
				}
			}
		});
		addWalletDialog.show();
	}
	
	private void deleteWallet(int walletId)
	{
		final Wallet wallet = DatabaseAdapter.getInstance(this).getWallet(walletId);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(EditWalletsActivity.this);
		deleteDialog.setTitle("Delete Wallet");
		deleteDialog.setMessage("Are you sure you want to delete wallet '" + wallet.getName() +
				"'?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter.getInstance(EditWalletsActivity.this).deleteWallet(wallet.getID());
				rebuildLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private void restoreWallet(int walletId)
	{
		final Wallet wallet = DatabaseAdapter.getInstance(this).getWallet(walletId);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(EditWalletsActivity.this);
		deleteDialog.setTitle("Restore Wallet");
		deleteDialog.setMessage("Are you sure you want to restore wallet '" + wallet.getName() +
				"'?");
		deleteDialog.setPositiveButton("Restore", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseAdapter.getInstance(EditWalletsActivity.this).restoreWallet(wallet.getID
						());
				rebuildLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private boolean verifyData(String walletName, String walletBalance, String origWalletName)
	{
		boolean dataCorrect;
		if (walletName.length() == 0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Wallet Name", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if (walletBalance.length() == 0)
		{
			Toast.makeText(getApplicationContext(), "Please Enter The Wallet Balance", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if (origWalletName != null && walletName.equals(origWalletName))
		{
			dataCorrect = true;
		}
		else if (DatabaseAdapter.getInstance(EditWalletsActivity.this).getWalletFromName
				(walletName) != null)
		{
			Toast.makeText(getApplicationContext(), "Please Enter A Unique Wallet Name", Toast
					.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
	
	private int getWalletIdFromMenuItem(MenuItem menuItem)
	{
		int walletId = -1;
		String title = menuItem.getTitle().toString();
		Matcher walletNameMatcher = Pattern.compile(".+\"(.+)\"").matcher(title);
		if (walletNameMatcher.find())
		{
			String walletName = walletNameMatcher.group(1);
			walletId = DatabaseAdapter.getInstance(this).getWalletFromName(walletName).getID();
		}
		return walletId;
	}
}
