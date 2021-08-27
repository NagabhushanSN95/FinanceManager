// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.functions.Constants;

public class TransactionButtonsLayout extends LinearLayout
{
	private LinearLayout layout;

	private ImageButton creditButton;
	private ImageButton debitButton;
	private ImageButton transferButton;

	public TransactionButtonsLayout(Context context)
	{
		super(context);
		buildLayout();
	}

	public TransactionButtonsLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		buildLayout();
	}

	private void buildLayout()
	{
		LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_buttons, this);

		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int screenWidth = displayMetrics.widthPixels;
		int screenHeight = displayMetrics.heightPixels;
		int buttonWidth = screenWidth/3;

		creditButton = (ImageButton) findViewById(R.id.imageButton_credit);
		creditButton.getLayoutParams().width = buttonWidth;
		creditButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(getContext(), AddTransactionActivity.class);
				intent.putExtra(Constants.ACTION, Constants.ACTION_ADD);
				intent.putExtra(Constants.TRANSACTION_TYPE, Constants.TRANSACTION_INCOME);
				((Activity) getContext()).startActivityForResult(intent, Constants.REQUEST_CODE_ADD_TRANSACTION);
			}
		});

		debitButton = (ImageButton) findViewById(R.id.imageButton_debit);
		debitButton.getLayoutParams().width = buttonWidth;
		debitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(getContext(), AddTransactionActivity.class);
				intent.putExtra(Constants.ACTION, Constants.ACTION_ADD);
				intent.putExtra(Constants.TRANSACTION_TYPE, Constants.TRANSACTION_EXPENSE);
				((Activity) getContext()).startActivityForResult(intent, Constants.REQUEST_CODE_ADD_TRANSACTION);
			}
		});

		transferButton = (ImageButton) findViewById(R.id.imageButton_transfer);
		transferButton.getLayoutParams().width = buttonWidth;
		transferButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(getContext(), AddTransactionActivity.class);
				intent.putExtra(Constants.ACTION, Constants.ACTION_ADD);
				intent.putExtra(Constants.TRANSACTION_TYPE, Constants.TRANSACTION_TRANSFER);
				((Activity) getContext()).startActivityForResult(intent, Constants.REQUEST_CODE_ADD_TRANSACTION);
			}
		});
	}
}
