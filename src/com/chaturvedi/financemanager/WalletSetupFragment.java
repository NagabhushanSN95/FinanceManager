package com.chaturvedi.financemanager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class WalletSetupFragment extends Fragment
{
	private static double walletBalance = 0;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.fragment_setup_wallet, container, false);
		final EditText walletBalanceEditText = (EditText) v.findViewById(R.id.editText_walletBalance);
		walletBalanceEditText.setTag("WalletBalanceEditText");
		walletBalanceEditText.setHint("Amount In Your Wallet");
		walletBalanceEditText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void afterTextChanged(Editable arg0)
			{
				walletBalance = Double.parseDouble(walletBalanceEditText.getText().toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				
			}
			
		});
		return v;
	}
	
	public static WalletSetupFragment newInstance()//(String txt)
	{
		WalletSetupFragment f = new WalletSetupFragment();
		//Bundle b = new Bundle();
		//b.putString("msg", txt);
		//f.setArguments(b);
		return f;
	}
	
	public static double getWalletBalance()
	{
		return walletBalance;
	}
}
