package com.chaturvedi.financemanager.edit;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
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

import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.customviews.IndefiniteWaitDialog;
import com.chaturvedi.financemanager.customviews.InputDialog;
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
		parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		
		expTypes = DatabaseManager.getAllExpenditureTypes();
		for(int i=0; i<expTypes.size(); i++)
		{
			addNewExpTypeToLayout(expTypes.get(i),i);
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
						final String expTypeName = expTypeNameField.getText().toString().trim();
						final int position = Integer.parseInt(positionList.getSelectedItem().toString());
						addNewExpTypeToLayout(expTypeName, position-1);
						
						IndefiniteWaitDialog waitDialogBuilder = new IndefiniteWaitDialog(EditExpTypesActivity.this);
						waitDialogBuilder.setWaitText("Please Wait While Your Expenditure Type Is Added " + 
								"And The App Is Configured For The Changes");
						final AlertDialog waitDialog = waitDialogBuilder.show();
						/** Add Exp Type in a seperate (non-ui) thread */
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
	}

	private void deleteExpType(final int expTypeNo)
	{
		//Toast.makeText(getApplicationContext(), "Coming Soon", Toast.LENGTH_SHORT).show();
		AlertDialog.Builder deleteDialog= new AlertDialog.Builder(this);
		deleteDialog.setTitle("Delete Expenditure Type " + expTypeNo);
		deleteDialog.setMessage("Are you sure you want to delete this Expenditure Type?1 " + 
				"Please do not proceed if you don't know what you are doing. The effects are irreversible. " + 
				"Please read FAQs for more information");
		deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				
				IndefiniteWaitDialog waitDialogBuilder = new IndefiniteWaitDialog(EditExpTypesActivity.this);
				waitDialogBuilder.setWaitText("Please Wait While Your Expenditure Type Is Removed " + 
						"And The App Is Configured For The Changes");
				final AlertDialog waitDialog = waitDialogBuilder.show();
				/** Remove Exp Type in a seperate (non-ui) thread */
				Thread deleteExpTypeThread = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						DatabaseManager.deleteExpType(expTypeNo);
						waitDialog.dismiss();
					}
				});
				deleteExpTypeThread.start();
				parentLayout.removeViewAt(expTypeNo);
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}

	private void editExpType(final int expTypeNo)
	{
		final InputDialog editDialog = new InputDialog(this);
		editDialog.setTitle("Edit Expenditure Type " + (expTypeNo-1));
		editDialog.setInstruction("Enter The New Name For The Expenditure Type");
		editDialog.setHint("Exp Type Name");
		editDialog.setInputText(DatabaseManager.getExpenditureType(expTypeNo-1));
		editDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String newExpType = editDialog.getInput();
				DatabaseManager.setExpenditureType(expTypeNo-1, newExpType);
				TextView expTypeView = (TextView) parentLayout.getChildAt(expTypeNo).findViewById(R.id.textView_expType);
				expTypeView.setText(newExpType);
			}
		});
		editDialog.setNegativeButton("Cancel", null);
		editDialog.show();
	}
}
