package com.chaturvedi.financemanager.setup;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.datastructures.Wallet;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class WalletsSetupFragment extends Fragment
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

	private Context context;
	private View walletsSetupView;

	private LinearLayout parentLayout;
	private LinearLayout.LayoutParams parentLayoutParams;
	private Button addWalletButton;

	private AlertDialog.Builder addWalletDialog;
	private EditText walletNameField;
	private EditText walletBalanceField;

	private static ArrayList<Wallet> wallets;
	private int contextMenuWalletNo;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		walletsSetupView = inflater.inflate(R.layout.fragment_setup_wallets, container, false);
		getActivity().setTheme(android.R.style.Theme);

		displayMetrics=new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		MARGIN_TOP_PARENT_LAYOUT=screenHeight*5/100;
		MARGIN_BOTTOM_PARENT_LAYOUT=20;
		MARGIN_LEFT_PARENT_LAYOUT=screenWidth*3/100;
		MARGIN_RIGHT_PARENT_LAYOUT=screenWidth*3/100;
		WIDTH_NAME_VIEWS=screenWidth*60/100;
		WIDTH_BALANCE_VIEWS=screenWidth*30/100;
		MARGIN_TOP_VIEWS=5;

		wallets = new ArrayList<Wallet>();
		buildLayout();
		return walletsSetupView;
	}

	public static WalletsSetupFragment newInstance()//(int screenWidth, int screenHeight)
	{
		WalletsSetupFragment f = new WalletsSetupFragment();
		/*Bundle bundle = new Bundle();
		bundle.putInt("ScreenWidth", screenWidth);
		bundle.putInt("ScreenHeight", screenHeight);
		f.setArguments(bundle);*/
		return f;
	}

	public static ArrayList<Wallet> getAllWallets()
	{
		// Set Right all IDs if they are not in order due to addition and deletion of wallets
		for(int i=0; i<wallets.size(); i++)
		{
			wallets.get(i).setID(i+1);	// ID starts with 1
		}
		return wallets;
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
			buildLayout();
		}
		else
		{
			return false;
		}
		return true;
	}

	private void buildLayout()
	{
		parentLayout=(LinearLayout) walletsSetupView.findViewById(R.id.parentLayout);
		FrameLayout.LayoutParams parentLayoutParams=(FrameLayout.LayoutParams) parentLayout.getLayoutParams();
		parentLayoutParams.setMargins(MARGIN_LEFT_PARENT_LAYOUT, MARGIN_TOP_PARENT_LAYOUT, MARGIN_RIGHT_PARENT_LAYOUT, MARGIN_BOTTOM_PARENT_LAYOUT);
		parentLayout.setLayoutParams(parentLayoutParams);
		
		parentLayout.removeAllViews();
		
		addWalletButton =(Button) walletsSetupView.findViewById(R.id.button_addWallet);
		addWalletButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buildAddWalletDialog();
				addWalletDialog.show();
			}
		});
		
		DecimalFormat formatter = new DecimalFormat("#,##0.##");
		
		for(int i = 0; i< wallets.size(); i++)
		{
			final String walletName    = wallets.get(i).getName();
			final double walletBalance = wallets.get(i).getBalance();
			
			LayoutInflater layoutInflater = LayoutInflater.from(getActivity().getApplicationContext());
			LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_wallet, null);
			TextView walletNameView = (TextView) layout.findViewById(R.id.walletName);
			TextView walletBalanceView = (TextView) layout.findViewById(R.id.walletBalance);
			
			walletNameView.setText(wallets.get(i).getName());
			LinearLayout.LayoutParams nameViewParams = new LinearLayout.LayoutParams(WIDTH_NAME_VIEWS,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_NAME_VIEWS, 0, 0, 0);
			walletNameView.setLayoutParams(nameViewParams);
			
			walletBalanceView.setText(formatter.format(wallets.get(i).getBalance()));
			LinearLayout.LayoutParams balanceViewParams = new LinearLayout.LayoutParams(WIDTH_BALANCE_VIEWS,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			//nameViewParams.setMargins(MARGIN_LEFT_BALANCE_VIEWS, 0, 0, 0);
			walletBalanceView.setLayoutParams(balanceViewParams);
			walletBalanceView.setGravity(Gravity.RIGHT);
			
			if(i%2==0)
				layout.setBackgroundColor(Color.parseColor("#88CC00CC"));
			else
				layout.setBackgroundColor(Color.parseColor("#880044FF"));
			
			parentLayout.addView(layout);
			registerForContextMenu(layout);
			
			//Display Wallet Details when the Layout is Touched/Clicked
			layout.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					LayoutInflater inflater = LayoutInflater.from(getActivity());
					View walletDetailsView = inflater.inflate(R.layout.dialog_wallet_details, null);
					TextView walletNameView = (TextView) walletDetailsView.findViewById(R.id.value_walletName);
					walletNameView.setText(walletName);
					TextView walletBalanceView = (TextView) walletDetailsView.findViewById(R.id.value_walletBalance);
					walletBalanceView.setText(""+walletBalance);
					
					AlertDialog.Builder walletDetailsBuilder = new AlertDialog.Builder(getActivity());
					walletDetailsBuilder.setTitle("Wallet Details");
					walletDetailsBuilder.setView(walletDetailsView);
					walletDetailsBuilder.show();
				}
			});
		}
	}
	
	private void buildAddWalletDialog()
	{
		addWalletDialog = new AlertDialog.Builder(getActivity());
		addWalletDialog.setTitle("Add A Wallet");
		addWalletDialog.setMessage("Enter The Particulars");
		LayoutInflater layoutInflater=LayoutInflater.from(getActivity());
		final LinearLayout addWalletLayout=(LinearLayout)layoutInflater.inflate(R.layout.dialog_add_wallet, null);
		addWalletDialog.setView(addWalletLayout);
		
		walletNameField = (EditText) addWalletLayout.findViewById(R.id.walletName);
		walletBalanceField = (EditText) addWalletLayout.findViewById(R.id.walletBalance);
		
		addWalletDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				int id = wallets.size()+1;
				String walletName = walletNameField.getText().toString().trim();
				String walletBalance = walletBalanceField.getText().toString().trim();
				boolean dataCorrect = verifyData(walletName, walletBalance);
				
				if(dataCorrect)
				{
					Wallet wallet = new Wallet(""+id, walletName, walletBalance, ""+false);
					wallets.add(wallet);
					
					buildLayout();
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
	
	private void deleteWallet(int walletNo)
	{
		// Remove Wallet Details
		wallets.remove(walletNo);
	}
	
	private void editWallet(final int walletNo)
	{
		buildAddWalletDialog();
		walletNameField.setText(wallets.get(walletNo).getName() + " ");
		walletBalanceField.setText(""+ wallets.get(walletNo).getBalance());
		
		addWalletDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				int id = wallets.size()+1;	// IDs start with 1 but array indexes start with 0
				String walletName = walletNameField.getText().toString().trim();
				String walletBalance = walletBalanceField.getText().toString().trim();
				boolean dataCorrect = verifyData(walletName, walletBalance);
				
				if(dataCorrect)
				{
					Wallet wallet = new Wallet(String.valueOf(id), walletName, walletBalance, String.valueOf(false));
					wallets.set(walletNo, wallet);
					
					buildLayout();
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
		addWalletDialog.show();
	}
	
	private boolean verifyData(String walletName, String walletBalance)
	{
		boolean dataCorrect = false;
		if(walletName.length()==0)
		{
			Toast.makeText(getActivity(), "Please Enter The Wallet Name", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else if(walletBalance.length()==0)
		{
			Toast.makeText(getActivity(), "Please Enter The Wallet Balance", Toast.LENGTH_LONG).show();
			dataCorrect = false;
		}
		else
		{
			dataCorrect = true;
		}
		return dataCorrect;
	}
}
