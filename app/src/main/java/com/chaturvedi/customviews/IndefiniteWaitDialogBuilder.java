package com.chaturvedi.customviews;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chaturvedi.financemanager.R;

public class IndefiniteWaitDialogBuilder extends AlertDialog.Builder
{
	private LinearLayout layout;
	
	@SuppressLint("InflateParams")
	// For passing num to inflate. Justified here, because there is no parent view
	public IndefiniteWaitDialogBuilder(Context cxt)
	{
		super(cxt);
		LayoutInflater inflater = LayoutInflater.from(cxt);
		layout = (LinearLayout) inflater.inflate(R.layout.dialog_wait, null);
		super.setView(layout);
	}
	
	/**
	 * Sets the Wait Text to be displayed in the dialog
	 * @param waitText String to be displayed in Wait Dialog
	 */
	public void setWaitText(String waitText)
	{
		TextView textView = (TextView) layout.findViewById(R.id.textView_wait);
		textView.setText(waitText);
	}
}
