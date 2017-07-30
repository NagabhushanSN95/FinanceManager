package com.chaturvedi.financemanager;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;

public class EditExpTypesActivity extends Activity
{
	private LinearLayout parentLayout;
	private int contextMenuExpTypeNo;
	
	private static ArrayList<String> expTypes;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_exp_types);
		buildLayout();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuExpTypeNo = parentLayout.indexOfChild(view);
		menu.setHeaderTitle("Options For Transaction "+(contextMenuExpTypeNo+1));
		menu.add(0, view.getId(), 0, "Edit");
		menu.add(0, view.getId(), 0, "Delete");
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if(item.getTitle().equals("Edit"))
		{
			editExpType(contextMenuExpTypeNo);
		}
		else if(item.getTitle().equals("Delete"))
		{
			deleteExpType(contextMenuExpTypeNo);
		}
		else
		{
			return false;
		}
		return true;
	}

	private void buildLayout()
	{
		parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		
		// Get all the 5 Hints  stored as String Resources
		String[] hints = new String[5];
		hints[0] = getResources().getString(R.string.hint_exp01);
		hints[1] = getResources().getString(R.string.hint_exp02);
		hints[2] = getResources().getString(R.string.hint_exp03);
		hints[3] = getResources().getString(R.string.hint_exp04);
		hints[4] = getResources().getString(R.string.hint_exp05);
		
		expTypes = DatabaseManager.getAllExpenditureTypes();
		
		for(int i=0; i<expTypes.size(); i++)
		{
			LayoutInflater inflater = LayoutInflater.from(this);
			LinearLayout expTypeLayout = (LinearLayout) inflater.inflate(R.layout.layout_display_exp_type, null);
			TextView expTypeView = (TextView) expTypeLayout.findViewById(R.id.textView_expType);
			expTypeView.setText(expTypes.get(i));
			registerForContextMenu(expTypeLayout);
			/*final EditText expTypeField = (EditText) expTypeLayout.findViewById(R.id.expType);
			expTypeField.setHint(hints[i]);
			expTypeField.setText(expTypes.get(i));
			// Whenever the ExpType is changed, update the same in ExpTypes ArrayList
			expTypeField.addTextChangedListener(new TextWatcher()
			{
				@Override
				public void afterTextChanged(Editable arg0)
				{
					String expType = expTypeField.getText().toString();
					int position = parentLayout.indexOfChild(expTypeLayout)-1;
					Toast.makeText(getApplicationContext(), "Krishna:"+position, Toast.LENGTH_SHORT).show();
					expTypes.set(position, expType);
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
			ImageButton deleteButton = (ImageButton) expTypeLayout.findViewById(R.id.imageButton_delete);
			deleteButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					parentLayout.removeView(expTypeLayout);
				}
			});*/
			parentLayout.addView(expTypeLayout, i+1);
		}
		
		/*final ImageButton editOthersButton = (ImageButton) findViewById(R.id.imageButton_editOthers);
		editOthersButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder warningDialog = new AlertDialog.Builder(getApplicationContext());
				warningDialog.setTitle("Edit System Field");
				warningDialog.setMessage("Others is an Default Expenditure Type. " + 
							"If you don't know what you are doing, please DON'T PROCEED. " + 
							"Contact the Developer for more Details");
				warningDialog.setIcon(android.R.drawable.ic_dialog_alert);
				warningDialog.setPositiveButton("Proceed", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						EditText othersExpTypeField = (EditText) findViewById(R.id.expTypeOther);
						othersExpTypeField.setEnabled(true);
						editOthersButton.setEnabled(false);
					}
				});
				warningDialog.setNeutralButton("Help", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Toast.makeText(getApplicationContext(), "Coming Soon!!!", Toast.LENGTH_LONG).show();
					}
				});
				warningDialog.setNegativeButton("Cancel", null);
				warningDialog.show();
			}
		});
		
		Button addExpTypeButton = (Button) findViewById(R.id.button_add_expType);
		addExpTypeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Add a New Text Field to enter one more ExpType
				LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
				final RelativeLayout expTypeLayout = (RelativeLayout) inflater.inflate(R.layout.layout_exp_type, null);
				final EditText expTypeField = (EditText) expTypeLayout.findViewById(R.id.expType);
				// Whenever the ExpType is changed, update the same in ExpTypes ArrayList
				expTypeField.addTextChangedListener(new TextWatcher()
				{
					@Override
					public void afterTextChanged(Editable arg0)
					{
						String expType = expTypeField.getText().toString();
						int position = parentLayout.indexOfChild(expTypeLayout)-1;
						expTypes.set(position, expType);
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
				ImageButton deleteButton = (ImageButton) expTypeLayout.findViewById(R.id.imageButton_delete);
				deleteButton.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						parentLayout.removeView(expTypeLayout);
					}
				});
				// Add the new Field above the Others ExpType. 
				parentLayout.addView(expTypeLayout, parentLayout.getChildCount()-1);
				expTypes.add(parentLayout.getChildCount()-3,"");
			}
		});*/
	}
	
	private void deleteExpType(int contextMenuExpTypeNo2)
	{
		Toast.makeText(getApplicationContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
	}

	private void editExpType(int contextMenuExpTypeNo2)
	{
		Toast.makeText(getApplicationContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
	}
}
