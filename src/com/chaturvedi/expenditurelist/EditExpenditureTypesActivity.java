package com.chaturvedi.expenditurelist;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class EditExpenditureTypesActivity extends Activity
{
	private final int NUM_EXPENDITURE_TYPES=5;
	
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_TEXT_FIELDS;
	private int MARGIN_TOP_TEXT_FIELDS;
	private int MARGIN_LEFT_TEXT_FIELDS;
	
	private ArrayList<EditText> typeTextFields;
	private ArrayList<LayoutParams> typeFieldParams;
	private ArrayList<String> expenditureTypes;
	
	private boolean dataEntered;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_edit_expenditure_types);
		}
		else
		{
			setContentView(R.layout.activity_edit_expenditure_types);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		WIDTH_TEXT_FIELDS = screenWidth*60/100;
		MARGIN_TOP_TEXT_FIELDS = screenHeight*5/100;
		MARGIN_LEFT_TEXT_FIELDS = screenWidth*20/100;
		
		buildLayout();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		if(VERSION.SDK_INT>10)
		{
			getMenuInflater().inflate(R.menu.activity_edit, menu);
		}
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.save:
				saveToDatabase();
				if(dataEntered)
				{
					finish();
				}
		}
		return true;
	}
	
	private void buildLayout()
	{
		typeTextFields = new ArrayList<EditText>();
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type1));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type2));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type3));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type4));
		typeTextFields.add((EditText) findViewById(R.id.expenditure_type5));
		
		expenditureTypes = DatabaseManager.getExpenditureTypes();
		typeFieldParams = new ArrayList<LayoutParams>();
		for(int i=0; i<NUM_EXPENDITURE_TYPES; i++)
		{
			LayoutParams params = new LayoutParams(WIDTH_TEXT_FIELDS, LayoutParams.WRAP_CONTENT);
			params.setMargins(MARGIN_LEFT_TEXT_FIELDS, MARGIN_TOP_TEXT_FIELDS, 0, 0);
			typeTextFields.get(i).setLayoutParams(params);
			typeFieldParams.add(params);
			typeTextFields.get(i).setText(expenditureTypes.get(i));
		}
		if(VERSION.SDK_INT<=10)
		{
			Button nextButton = (Button)findViewById(R.id.button_finish);
			nextButton.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v)
				{
					saveToDatabase();
					if(dataEntered)
					{
						finish();
					}
				}
			});
		}
	}

	private void saveToDatabase()
	{
		dataEntered=true;
		
		expenditureTypes = new ArrayList<String>();
		for(int i=0; i<NUM_EXPENDITURE_TYPES; i++)
		{
			String type = typeTextFields.get(i).getText().toString();
			if(dataEntered && type.length()!=0)
			{
				expenditureTypes.add(type);
			}
			else if(dataEntered)
			{
				Toast.makeText(getApplicationContext(), "Enter Something For Expenditure Type "+(i+1), Toast.LENGTH_LONG).show();
				dataEntered=false;
			}
		}
		
		if(dataEntered)
		{
			DatabaseManager.setExpenditureTypes(expenditureTypes);
		}
	}
}
