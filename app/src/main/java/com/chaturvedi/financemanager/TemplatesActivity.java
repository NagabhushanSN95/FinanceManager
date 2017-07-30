package com.chaturvedi.financemanager;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.database.Template;

public class TemplatesActivity extends Activity
{
	/*private final int DISPLAY_TRANSACTIONS_ALL = 0;
	private final int DISPLAY_TRANSACTIONS_YEAR = 1;
	private final int DISPLAY_TRANSACTIONS_MONTH = 2;
	private int displayTransactions;*/
	
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_SLNO;
	private int WIDTH_PARTICULARS;
	private int WIDTH_TYPE;
	private int WIDTH_AMOUNT;
	
	private LinearLayout parentLayout;
	private ArrayList<Template> templates;
	private ArrayList<String> templateStrings;
	private int contextMenuTemplateNo;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_templates);
		if(VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			// Provide Up Button in Action Bar
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else
		{
			// No Up Button in Action Bar
		}
		
		calculateDimensions();
		readTemplates();
		buildTitleLayout();
		buildBodyLayout();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(DatabaseManager.getNumTransactions()==0)
		{
			DatabaseManager.setContext(TemplatesActivity.this);
			DatabaseManager.readDatabase();
			buildBodyLayout();
		}
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(TemplatesActivity.this);
				return true;
		}
		return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		contextMenuTemplateNo = parentLayout.indexOfChild(view);
		menu.setHeaderTitle("Options For Transaction "+(contextMenuTemplateNo+1));
		menu.add(0, view.getId(), 0, "Edit");
		menu.add(0, view.getId(), 0, "Delete");
	}
	
	public boolean onContextItemSelected(MenuItem item)
	{
		if(item.getTitle().equals("Edit"))
		{
			editTemplate(contextMenuTemplateNo);
		}
		else if(item.getTitle().equals("Delete"))
		{
			deleteTemplate(contextMenuTemplateNo);
		}
		else
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Calculate the values of various Dimension Fields
	 */
	private void calculateDimensions()
	{
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;
		
		/*if(VERSION.SDK_INT<=10)
		{
			WIDTH_TYPE=20*screenWidth/100-6;
		}
		else
		{
			WIDTH_TYPE=20*screenWidth/100-12;
		}*/
		WIDTH_SLNO=10*screenWidth/100;
		WIDTH_PARTICULARS=50*screenWidth/100;
		WIDTH_TYPE=20*screenWidth/100;
		WIDTH_AMOUNT=20*screenWidth/100;
	}
	
	private void readTemplates()
	{
		templates = DatabaseManager.getAllTemplates();
		templateStrings = new ArrayList<String>();
		for(int i=0; i<templates.size(); i++)
		{
			templateStrings.add(templates.get(i).getParticular());
		}
	}
	
	private void buildTitleLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if(0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}
				
		TextView slnoTitleView = (TextView)findViewById(R.id.slno);
		LayoutParams slnoTitleParams = (LayoutParams) slnoTitleView.getLayoutParams();
		slnoTitleParams.width = WIDTH_SLNO;
		slnoTitleView.setLayoutParams(slnoTitleParams);
		
		TextView particularsTitleView = (TextView)findViewById(R.id.particulars);
		LayoutParams particularsTitleParams = (LayoutParams) particularsTitleView.getLayoutParams();
		particularsTitleParams.width = WIDTH_PARTICULARS;
		particularsTitleView.setLayoutParams(particularsTitleParams);
		
		TextView typeTitleView = (TextView)findViewById(R.id.type);
		LayoutParams typeTitleParams = (LayoutParams) typeTitleView.getLayoutParams();
		typeTitleParams.width = WIDTH_TYPE;
		typeTitleView.setLayoutParams(typeTitleParams);
		
		TextView amountTitleView = (TextView)findViewById(R.id.amount);
		LayoutParams amountTitleParams = (LayoutParams) amountTitleView.getLayoutParams();
		amountTitleParams.width = WIDTH_AMOUNT;
		amountTitleView.setLayoutParams(amountTitleParams);
	}
	
	private void buildBodyLayout()
	{
		try
		{
			parentLayout = (LinearLayout)findViewById(R.id.layout_parent);
			parentLayout.removeAllViews();
			
			DecimalFormat formatterDisplay = new DecimalFormat("#,##0.##");
			for(int i=0; i<templates.size(); i++)
			{
				LayoutInflater layoutInflater = LayoutInflater.from(this);
				LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.layout_display_templates, null);

				TextView slnoView = (TextView)linearLayout.findViewById(R.id.slno);
				LayoutParams slnoParams = (LayoutParams) slnoView.getLayoutParams();
				slnoParams.width = WIDTH_SLNO;
				slnoView.setLayoutParams(slnoParams);
				slnoView.setText(""+(i+1));
				//slnoView.setMinLines(MIN_LINES);

				TextView typeView = (TextView)linearLayout.findViewById(R.id.type);
				LayoutParams typeParams = (LayoutParams) typeView.getLayoutParams();
				typeParams.width = WIDTH_TYPE;
				typeView.setLayoutParams(typeParams);
				typeView.setText(templates.get(i).getType());

				TextView particularsView = (TextView)linearLayout.findViewById(R.id.particulars);
				LayoutParams particularsParams = (LayoutParams) particularsView.getLayoutParams();
				particularsParams.width = WIDTH_PARTICULARS;
				particularsView.setLayoutParams(particularsParams);
				particularsView.setText(templates.get(i).getParticular());
				//particularsView.setMinLines(MIN_LINES);

				TextView amountView = (TextView)linearLayout.findViewById(R.id.amount);
				LayoutParams amountParams = (LayoutParams) amountView.getLayoutParams();
				amountParams.width = WIDTH_AMOUNT;
				amountView.setLayoutParams(amountParams);
				amountView.setText(formatterDisplay.format(templates.get(i).getAmount()));
				//amountView.setMinLines(MIN_LINES);

				parentLayout.addView(linearLayout);
				registerForContextMenu(linearLayout);
			}
		}
		catch(Exception e)
		{
			Toast.makeText(this, "Error In Building Body Layout\n"+e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void editTemplate(int templateNo)
	{
		Toast.makeText(getApplicationContext(), "Coming Soon", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Delete the transaction referred by transactionNo
	 * @param transactionNo Number of the transaction to be deleted
	 */
	private void deleteTemplate(int templateNo)
	{
		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
		deleteDialog.setTitle("Delete Template");
		deleteDialog.setMessage("Are You Sure You Want To Delete Template No " + (templateNo+1) + "?");
		deleteDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				DatabaseManager.deleteTemplate(templates.get(contextMenuTemplateNo));
				//transactions = DatabaseManager.getAllTransactions();
				buildBodyLayout();
			}
		});
		deleteDialog.setNegativeButton("Cancel", null);
		deleteDialog.show();
	}
}
