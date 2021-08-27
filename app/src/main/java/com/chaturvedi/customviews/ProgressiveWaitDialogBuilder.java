// Shree KRISHNAya Namaha

package com.chaturvedi.customviews;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chaturvedi.financemanager.R;

public class ProgressiveWaitDialogBuilder extends AlertDialog.Builder
{
	private LinearLayout layout;
	
	@SuppressLint("InflateParams")
	// For passing null to inflate method. Justified here, because there is no parent view
	public ProgressiveWaitDialogBuilder(Context context)
	{
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		layout = (LinearLayout) inflater.inflate(R.layout.dialog_wait_progress, null);
		super.setView(layout);
	}
	
	/**
	 * Sets the Wait Text to be displayed in the dialog
	 *
	 * @param waitText String to be displayed in Wait Dialog
	 */
	public void setWaitText(String waitText)
	{
		TextView textView = (TextView) layout.findViewById(R.id.textView_wait);
		textView.setText(waitText);
	}
	
	public void setProgress(int progress)
	{
		ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
		{
			progressBar.setProgress(progress, true);
		}
		else
		{
			progressBar.setProgress(progress);
		}
	}
}
