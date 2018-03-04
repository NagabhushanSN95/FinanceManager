package com.chaturvedi.financemanager.edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.chaturvedi.customviews.InputDialog;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.ExpenditureType;

import java.util.ArrayList;

public class EditExpTypesActivity extends Activity
{
	private static ArrayList<ExpenditureType> expTypes;
	private LinearLayout parentLayout;
	private int contextMenuExpTypeNo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_exp_types);
		// Provide Up Button in Action Bar
		if (getActionBar() != null)
		{
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		buildLayout();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		// -1 because there is an additional TextView child
		contextMenuExpTypeNo = parentLayout.indexOfChild(view) - 1;
		menu.setHeaderTitle("Options For Expenditure Type "+(contextMenuExpTypeNo));
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
	
	private void addNewExpTypeToLayout(String expTypeName, int position)
	{
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout expTypeLayout = (LinearLayout) inflater.inflate(R.layout.layout_display_exp_type, null);
		TextView expTypeView = (TextView) expTypeLayout.findViewById(R.id.textView_expType);
		expTypeView.setText(expTypeName);
		registerForContextMenu(expTypeLayout);
		parentLayout.addView(expTypeLayout, position+1);
	}

	private void buildLayout()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditExpTypesActivity.this);
		parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		
		expTypes = databaseAdapter.getAllVisibleExpenditureTypes();
		for(int i=0; i<expTypes.size(); i++)
		{
			addNewExpTypeToLayout(expTypes.get(i).getName(),i);
		}
		
		buildAddExpButton();
		
		
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
		});*/
	}
	
	private void buildAddExpButton()
	{
		Button addExpTypeButton = (Button) findViewById(R.id.button_add_expType);
		addExpTypeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditExpTypesActivity.this);

				LayoutInflater inflater = LayoutInflater.from(EditExpTypesActivity.this);
				LinearLayout addExpTypeLayout = (LinearLayout) inflater.inflate(R.layout.dialog_add_exp_type, null);
				final EditText expTypeNameField = (EditText) addExpTypeLayout.findViewById(R.id.editText_expTypeName);
				final Spinner positionList = (Spinner) addExpTypeLayout.findViewById(R.id.spinner_position);
				int numExpTypes = databaseAdapter.getNumVisibleExpenditureTypes();
				String[] positions = new String[numExpTypes];
				for(int i=0; i<numExpTypes; i++)
				{
					positions[i] = String.valueOf(i+1);
				}
				positionList.setAdapter(new ArrayAdapter<String>(EditExpTypesActivity.this, 
						android.R.layout.simple_spinner_item, positions));
				positionList.setSelection(numExpTypes-1);
				positionList.setVisibility(View.GONE);
				
				AlertDialog.Builder addExpTypeBuilder = new AlertDialog.Builder(EditExpTypesActivity.this);
				addExpTypeBuilder.setTitle("Enter New Expenditure Type:");
				addExpTypeBuilder.setView(addExpTypeLayout);
				addExpTypeBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						int expTypeID = databaseAdapter.getIDforNextExpenditureType();
						String expTypeName = expTypeNameField.getText().toString().trim();
						ExpenditureType expenditureType = new ExpenditureType(expTypeID, expTypeName, false);
						int position = Integer.parseInt(positionList.getSelectedItem().toString());
						addNewExpTypeToLayout(expTypeName, position-1);
						databaseAdapter.addExpenditureType(expenditureType);
						databaseAdapter.readjustCountersTable();
					}
				});
				addExpTypeBuilder.setNegativeButton("Cancel", null);
				addExpTypeBuilder.show();
			}
		});
	}

	private void deleteExpType(final int expTypeNo)
	{
		//Toast.makeText(getApplicationContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
		AlertDialog.Builder deleteDialog= new AlertDialog.Builder(this);
		deleteDialog.setTitle("Delete Expenditure Type " + expTypeNo);
		deleteDialog.setMessage("Are you sure you want to delete this Expenditure Type? " +
				"Please do not proceed if you don't know what you are doing. The effects are irreversible. " + 
				"Please read FAQs for more information");
		deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				int expTypeID = expTypes.get(expTypeNo).getId();
				DatabaseAdapter.getInstance(EditExpTypesActivity.this).deleteExpenditureType(expTypeID);
				// +1 becuase there is additional textview child
				parentLayout.removeViewAt(expTypeNo+1);
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}

	private void editExpType(final int expTypeNo)
	{
		final ExpenditureType expenditureType = expTypes.get(expTypeNo);
		final InputDialog editDialog = new InputDialog(this);
		editDialog.setTitle("Edit Expenditure Type '" + expenditureType.getName() + "'");
		editDialog.setInstruction("Enter The New Name For The Expenditure Type");
		editDialog.setHint("Expenditure Type Name");
		editDialog.setInputText(expenditureType.getName());
		editDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String newExpTypeName = editDialog.getInput();
				expenditureType.setName(newExpTypeName);
				DatabaseAdapter.getInstance(EditExpTypesActivity.this).updateExpenditureType(expenditureType);
				// +1 becuase there is additional textview child
				TextView expTypeView = (TextView) parentLayout.getChildAt(expTypeNo+1).findViewById(R.id.textView_expType);
				expTypeView.setText(newExpTypeName);
			}
		});
		editDialog.setNegativeButton("Cancel", null);
		editDialog.show();
	}
}