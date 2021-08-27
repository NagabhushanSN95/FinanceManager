package com.chaturvedi.financemanager.edit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.customviews.InputDialog;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.ExpenditureType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditExpTypesActivity extends Activity
{
	public static final int ID_EDIT_EXP_TYPE = 1101;
	public static final int ID_DELETE_EXP_TYPE = 1102;
	private static final int ID_RESTORE_EXP_TYPE = 1103;
	
	private LinearLayout activeExpTypesLayout;
	private LinearLayout deletedExpTypesLayout;
	private boolean showDeletedExpTypes = false;
	
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds childItems to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_edit_exp_types, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_toggleDisplayDeletedExpTypes:
				showDeletedExpTypes = !showDeletedExpTypes;
				if (showDeletedExpTypes)
				{
					item.setTitle("Hide Deleted items");
					deletedExpTypesLayout.setVisibility(View.VISIBLE);
				}
				else
				{
					item.setTitle("Show Deleted items");
					deletedExpTypesLayout.setVisibility(View.GONE);
				}
				return true;
			
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		String expTypeName = ((TextView) view.findViewById(R.id.textView_expTypeName)).getText()
				.toString();
		ExpenditureType expType = DatabaseAdapter.getInstance(this).getExpenditureTypeFromName
				(expTypeName);
		menu.setHeaderTitle("Options");
		if (!expType.isDeleted())
		{
			menu.add(Menu.NONE, ID_EDIT_EXP_TYPE, Menu.NONE, "Edit \"" + expTypeName + "\"");
			menu.add(Menu.NONE, ID_DELETE_EXP_TYPE, Menu.NONE, "Delete \"" + expTypeName + "\"");
		}
		else
		{
			menu.add(Menu.NONE, ID_RESTORE_EXP_TYPE, Menu.NONE, "Restore \"" + expTypeName + "\"");
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		int expTypeId = getExpTypeIdFromMenuItem(item);
		if (expTypeId != -1)
		{
			switch (item.getItemId())
			{
				case ID_EDIT_EXP_TYPE:
					editExpType(expTypeId);
					rebuildLayout();
					break;
				
				case ID_DELETE_EXP_TYPE:
					deleteExpType(expTypeId);
					break;
				
				case ID_RESTORE_EXP_TYPE:
					restoreExpType(expTypeId);
					break;
				
				default:
					return super.onContextItemSelected(item);
			}
		}
		else
		{
			Toast.makeText(this, "Unable to detect selected Expenditure Type. Please try again. " +
					"Please contact developer if the problem persists", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private void addNewExpTypeToLayout(String expTypeName, int position, boolean isDeleted)
	{
		LinearLayout layout = isDeleted ? deletedExpTypesLayout : activeExpTypesLayout;
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout expTypeLayout = (LinearLayout) inflater.inflate(R.layout
				.layout_display_exp_type, layout, false);
		TextView expTypeView = (TextView) expTypeLayout.findViewById(R.id.textView_expTypeName);
		expTypeView.setText(expTypeName);
		if (isDeleted)
		{
			expTypeView.setTextColor(Color.GRAY);
		}
		registerForContextMenu(expTypeLayout);
		layout.addView(expTypeLayout, position);
	}
	
	private void rebuildLayout()
	{
		activeExpTypesLayout.removeAllViews();
		deletedExpTypesLayout.removeAllViews();
		buildLayout();
	}

	private void buildLayout()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(EditExpTypesActivity.this);
		@SuppressWarnings("unused")
		LinearLayout parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
		activeExpTypesLayout = (LinearLayout) findViewById(R.id.activeExpTypesLayout);
		deletedExpTypesLayout = (LinearLayout) findViewById(R.id.deletedExpTypesLayout);
		
		int position = 0;
		for (ExpenditureType expenditureType : databaseAdapter.getAllVisibleExpenditureTypes())
		{
			addNewExpTypeToLayout(expenditureType.getName(), position, false);
			position++;
		}
		
		position = 0;
		for (ExpenditureType expenditureType : databaseAdapter.getAllDeletedExpenditureTypes())
		{
			addNewExpTypeToLayout(expenditureType.getName(), position, true);
			position++;
		}
		
		if (!showDeletedExpTypes)
		{
			deletedExpTypesLayout.setVisibility(View.GONE);
		}
		
		buildAddExpButton();
	}
	
	private void buildAddExpButton()
	{
		Button addExpTypeButton = (Button) findViewById(R.id.button_add_expType);
		addExpTypeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance
						(EditExpTypesActivity.this);
				
				LayoutInflater inflater = LayoutInflater.from(EditExpTypesActivity.this);
				@SuppressLint("InflateParams")
				// For passing null to inflate method. Justified here, because there is no parent
						// view
						LinearLayout addExpTypeLayout = (LinearLayout) inflater.inflate(R.layout
						.dialog_add_exp_type, null);
				final EditText expTypeNameField = (EditText) addExpTypeLayout.findViewById(R.id
						.editText_expTypeName);
				final Spinner positionList = (Spinner) addExpTypeLayout.findViewById(R.id
						.spinner_position);
				int numExpTypes = databaseAdapter.getNumVisibleExpenditureTypes();
				String[] positions = new String[numExpTypes];
				for (int i = 0; i < numExpTypes; i++)
				{
					positions[i] = String.valueOf(i + 1);
				}
				positionList.setAdapter(new ArrayAdapter<>(EditExpTypesActivity.this,
						android.R.layout.simple_spinner_item, positions));
				positionList.setSelection(numExpTypes - 1);
				positionList.setVisibility(View.GONE);
				
				AlertDialog.Builder addExpTypeBuilder = new AlertDialog.Builder
						(EditExpTypesActivity.this);
				addExpTypeBuilder.setTitle("Enter New Expenditure Type:");
				addExpTypeBuilder.setView(addExpTypeLayout);
				addExpTypeBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						int expTypeID = databaseAdapter.getIDforNextExpenditureType();
						String expTypeName = expTypeNameField.getText().toString().trim();
						ExpenditureType expenditureType = new ExpenditureType(expTypeID,
								expTypeName, false);
						int position = Integer.parseInt(positionList.getSelectedItem().toString());
						addNewExpTypeToLayout(expTypeName, position - 1, false);
						databaseAdapter.addExpenditureType(expenditureType);
						databaseAdapter.readjustCountersTable();
					}
				});
				addExpTypeBuilder.setNegativeButton("Cancel", null);
				addExpTypeBuilder.show();
			}
		});
	}
	
	private void editExpType(final int expTypeId)
	{
		final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(this);
		final ExpenditureType expenditureType = databaseAdapter.getExpenditureType(expTypeId);
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
				databaseAdapter.updateExpenditureType(expenditureType);
				rebuildLayout();
			}
		});
		editDialog.setNegativeButton("Cancel", null);
		editDialog.show();
	}
	
	private void deleteExpType(final int expTypeId)
	{
		final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(this);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
		deleteDialog.setTitle("Delete Expenditure Type " + databaseAdapter.getExpenditureType
				(expTypeId).getName());
		deleteDialog.setMessage("Are you sure you want to delete this Expenditure Type?");
		deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				databaseAdapter.deleteExpenditureType(expTypeId);
				rebuildLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private void restoreExpType(final int expTypeId)
	{
		final DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(this);
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
		deleteDialog.setTitle("Restore Expenditure Type " + databaseAdapter.getExpenditureType
				(expTypeId).getName());
		deleteDialog.setMessage("Are you sure you want to restore this Expenditure Type?");
		deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				databaseAdapter.restoreExpenditureType(expTypeId);
				rebuildLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
	
	private int getExpTypeIdFromMenuItem(MenuItem menuItem)
	{
		int expTypeId = -1;
		String title = menuItem.getTitle().toString();
		Matcher expTypeNameMatcher = Pattern.compile(".+\"(.+)\"").matcher(title);
		if (expTypeNameMatcher.find())
		{
			String expTypeName = expTypeNameMatcher.group(1);
			expTypeId = DatabaseAdapter.getInstance(this).getExpenditureTypeFromName(expTypeName)
					.getId();
		}
		return expTypeId;
	}
}