package com.chaturvedi.financemanager;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
		
		expTypes = DatabaseManager.getAllExpenditureTypes();
		for(int i=0; i<expTypes.size(); i++)
		{
			LayoutInflater inflater = LayoutInflater.from(this);
			LinearLayout expTypeLayout = (LinearLayout) inflater.inflate(R.layout.layout_display_exp_type, null);
			TextView expTypeView = (TextView) expTypeLayout.findViewById(R.id.textView_expType);
			expTypeView.setText(expTypes.get(i));
			registerForContextMenu(expTypeLayout);
			parentLayout.addView(expTypeLayout, i+1);
		}
		
		Button addExpTypeButton = (Button) findViewById(R.id.button_add_expType);
		addExpTypeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				LayoutInflater inflater = LayoutInflater.from(EditExpTypesActivity.this);
				LinearLayout addExpTypeLayout = (LinearLayout) inflater.inflate(R.layout.dialog_add_exp_type, null);
				final EditText expTypeNameField = (EditText) addExpTypeLayout.findViewById(R.id.editText_expTypeName);
				final Spinner positionList = (Spinner) addExpTypeLayout.findViewById(R.id.spinner_position);
				int numExpTypes = DatabaseManager.getNumExpTypes();
				String[] positions = new String[numExpTypes];
				for(int i=0; i<numExpTypes; i++)
				{
					positions[i] = ""+(i+1);
				}
				positionList.setAdapter(new ArrayAdapter<String>(EditExpTypesActivity.this, 
						android.R.layout.simple_spinner_item, positions));
				positionList.setSelection(numExpTypes-1);
				
				AlertDialog.Builder addExpTypeBuilder = new AlertDialog.Builder(EditExpTypesActivity.this);
				addExpTypeBuilder.setTitle("Enter New Expenditure Type:");
				addExpTypeBuilder.setView(addExpTypeLayout);
				addExpTypeBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						final String expTypeName = expTypeNameField.getText().toString();
						final int position = Integer.parseInt(positionList.getSelectedItem().toString());
						addNewExpTypeToLayout(expTypeName, position-1);
						
						AlertDialog.Builder waitDialogBuilder = new AlertDialog.Builder(EditExpTypesActivity.this);
						LayoutInflater inflater = LayoutInflater.from(EditExpTypesActivity.this);
						View waitView = inflater.inflate(R.layout.dialog_wait, null);
						TextView waitTextView = (TextView) waitView.findViewById(R.id.textView_wait);
						waitTextView.setText("Please Wait While Your Expenditure Type Is Added " + 
								"And The App Is Configured For The Changes");
						waitDialogBuilder.setView(waitView);
						final AlertDialog waitDialog = waitDialogBuilder.show();
						// Add Exp Type in a seperate (non-ui) thread
						Thread addExpTypeThread = new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								DatabaseManager.addExpType(expTypeName, position);
								waitDialog.dismiss();
							}
						});
						addExpTypeThread.start();
					}
				});
				addExpTypeBuilder.setNegativeButton("Cancel", null);
				addExpTypeBuilder.show();
			}
		});
		
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
	
	private void addNewExpTypeToLayout(String expTypeName, int position)
	{
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout expTypeLayout = (LinearLayout) inflater.inflate(R.layout.layout_display_exp_type, null);
		TextView expTypeView = (TextView) expTypeLayout.findViewById(R.id.textView_expType);
		expTypeView.setText(expTypeName);
		registerForContextMenu(expTypeLayout);
		parentLayout.addView(expTypeLayout, position+1);
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
