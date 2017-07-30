package com.chaturvedi.financemanager.customviews;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chaturvedi.financemanager.R;

public class IndefiniteWaitDialog extends AlertDialog.Builder
{
	Context context;
	LinearLayout layout;
	
	public IndefiniteWaitDialog(Context cxt)
	{
		super(cxt);
		context = cxt;
		LayoutInflater inflater = LayoutInflater.from(context);
		layout = (LinearLayout) inflater.inflate(R.layout.dialog_wait, null);
		super.setView(layout);
	}
	
	/**
	 * Sets the Wait Text to be displayed in the dialog
	 * @param waitText
	 */
	public void setWaitText(String waitText)
	{
		TextView textView = (TextView) layout.findViewById(R.id.textView_wait);
		textView.setText(waitText);
	}
}
