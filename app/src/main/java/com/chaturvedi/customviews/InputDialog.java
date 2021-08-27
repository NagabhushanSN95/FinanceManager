package com.chaturvedi.customviews;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chaturvedi.financemanager.R;

public class InputDialog extends AlertDialog.Builder
{
	Context context;
	LinearLayout layout;
	
	public InputDialog(Context cxt)
	{
		super(cxt);
		context = cxt;
		LayoutInflater inflater = LayoutInflater.from(context);
		layout = (LinearLayout) inflater.inflate(R.layout.dialog_input, null);
		super.setView(layout);
	}
	
	/**
	 * Sets the instruction to be displayed in the dialog
	 * @param instruction
	 */
	public void setInstruction(String instruction)
	{
		TextView textView = (TextView) layout.findViewById(R.id.textView);
		textView.setText(instruction);
	}
	
	/**
	 * Sets the hint for EditText Input Field
	 * @param hint
	 */
	public void setHint(String hint)
	{
		EditText editText = (EditText) layout.findViewById(R.id.editText);
		editText.setHint(hint);
	}
	
	/**
	 * Sets the text for EditText Input Field
	 * @param text
	 */
	public void setInputText(String text)
	{
		EditText editText = (EditText) layout.findViewById(R.id.editText);
		editText.setText(text);
	}
	
	/**
	 * Returns the input entered by user
	 * @return
	 */
	public String getInput()
	{
		EditText editText = (EditText) layout.findViewById(R.id.editText);
		String input = editText.getText().toString().trim();
		return input;
	}

}
