package com.chaturvedi.financemanager.setup;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chaturvedi.financemanager.R;

public class ExpTypesSetupFragment extends Fragment
{
	private View expTypesSetupView;
	private static ArrayList<String> expTypes;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		expTypesSetupView = inflater.inflate(R.layout.fragment_setup_exp_types, container, false);
		
		buildLayout();
		return expTypesSetupView;
	}
	
	public static ExpTypesSetupFragment newInstance(String txt)
	{
		ExpTypesSetupFragment f = new ExpTypesSetupFragment();
		Bundle b = new Bundle();
		b.putString("msg", txt);
		f.setArguments(b);
		return f;
	}
	
	public static ArrayList<String> getAllExpTypes()
	{
		return expTypes;
	}
	
	private void buildLayout()
	{
		final LinearLayout parentLayout = (LinearLayout) expTypesSetupView.findViewById(R.id.parentLayout);
		
		// Get all the 5 Hints  stored as String Resources
		String[] hints = new String[5];
		hints[0] = getResources().getString(R.string.hint_exp01);
		hints[1] = getResources().getString(R.string.hint_exp02);
		hints[2] = getResources().getString(R.string.hint_exp03);
		hints[3] = getResources().getString(R.string.hint_exp04);
		hints[4] = getResources().getString(R.string.hint_exp05);
		
		// Add all the hints for the ExpTypes i.e. default ExpTypes
		expTypes = new ArrayList<String>(5);
		for(int i=0; i<5; i++)
		{
			expTypes.add(hints[i]);
		}
		
		// Add the four default Exp Types
		for(int i=0; i<4; i++)
		{
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			final RelativeLayout expTypeLayout = (RelativeLayout) inflater.inflate(R.layout.layout_exp_type, null);
			final EditText expTypeField = (EditText) expTypeLayout.findViewById(R.id.expType);
			expTypeField.setHint(hints[i]);
			// Whenever the ExpType is changed, update the same in ExpTypes ArrayList
			expTypeField.addTextChangedListener(new TextWatcher()
			{
				@Override
				public void afterTextChanged(Editable arg0)
				{
					String expType = expTypeField.getText().toString();
					int position = parentLayout.indexOfChild(expTypeLayout)-1;
					//Toast.makeText(getActivity(), "Krishna:"+position, Toast.LENGTH_SHORT).show();
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
			parentLayout.addView(expTypeLayout, i+1);
		}
		
		final ImageButton editOthersButton = (ImageButton) expTypesSetupView.findViewById(R.id.imageButton_editOthers);
		editOthersButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder warningDialog = new AlertDialog.Builder(getActivity());
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
						EditText othersExpTypeField = (EditText) expTypesSetupView.findViewById(R.id.expTypeOther);
						othersExpTypeField.setEnabled(true);
						editOthersButton.setEnabled(false);
					}
				});
				warningDialog.setNeutralButton("Help", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Toast.makeText(getActivity(), "Coming Soon!!!", Toast.LENGTH_LONG).show();
					}
				});
				warningDialog.setNegativeButton("Cancel", null);
				warningDialog.show();
			}
		});
		
		Button addExpTypeButton = (Button) expTypesSetupView.findViewById(R.id.button_add_expType);
		addExpTypeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Add a New Text Field to enter one more ExpType
				LayoutInflater inflater = LayoutInflater.from(getActivity());
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
		});
	}
}
