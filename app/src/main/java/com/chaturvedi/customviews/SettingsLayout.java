package com.chaturvedi.customviews;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsLayout extends LinearLayout
{
	private Context context;
	private String settingName;
	private String[] options;
	private int optionSelected = 0;
	
	private TextView settingNameView;
	private TextView settingSelectedView;
	private OnSettingChangedListener settingListener;

	public SettingsLayout(Context cxt)
	{
		super(cxt);
		context = cxt;
		options = new String[]{""};
		this.setOrientation(LinearLayout.VERTICAL);
		buildLayout();
	}

	public SettingsLayout(Context cxt, AttributeSet attrs)
	{
		super(cxt, attrs);
		context = cxt;
		options = new String[]{""};
		this.setOrientation(LinearLayout.VERTICAL);
		buildLayout();
	}

	private void buildLayout()
	{
		settingNameView = new TextView(context);
		LayoutParams nameViewParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		nameViewParams.setMargins(20, 5, 20, 0);
		settingNameView.setLayoutParams(nameViewParams);
		settingNameView.setTextSize(20);
		this.addView(settingNameView);
		
		settingSelectedView = new TextView(context);
		LayoutParams settingSelectedViewParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		settingSelectedViewParams.setMargins(20, 0, 20, 0);
		settingSelectedView.setLayoutParams(settingSelectedViewParams);
		this.addView(settingSelectedView);
		
		View borderLine = new View(context);
		LayoutParams lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		lineParams.setMargins(5, 5, 5, 0);
		borderLine.setLayoutParams(lineParams);
		borderLine.setBackgroundColor(Color.BLACK);
		this.addView(borderLine);
		
		this.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder optionsBuilder = new AlertDialog.Builder(context);
				optionsBuilder.setTitle(settingName);
				optionsBuilder.setItems(options, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						optionSelected = which;
						settingSelectedView.setText(options[which]);
						settingListener.onSettingChanged();
					}
				});
				optionsBuilder.setNegativeButton("Cancel", null);
				optionsBuilder.show();
			}
		});
		
		this.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				switch(event.getAction())
				{
					case MotionEvent.ACTION_DOWN:
						SettingsLayout.this.setBackgroundColor(Color.parseColor("#CCCCCC"));
						break;
						
					case MotionEvent.ACTION_UP:
						SettingsLayout.this.setBackgroundColor(Color.TRANSPARENT);
						break;
						
					case MotionEvent.ACTION_CANCEL:
						SettingsLayout.this.setBackgroundColor(Color.TRANSPARENT);
						break;
				}
				
				return false;
			}
		});
	}
	
	public void setSettingName(String name)
	{
		settingName = name;
		settingNameView.setText(settingName);
	}
	
	public void setOptions(String[] options)
	{
		this.options = options;
	}

	public void setOptions(ArrayList<String> optionsList)
	{
		options = new String[optionsList.size()];
		for(int i=0; i<optionsList.size(); i++)
		{
			options[i] = optionsList.get(i);
		}
	}

	public void setSelection(int optionNo)
	{
		settingSelectedView.setText(options[optionNo]);
	}

    public void setSelection(String option) {
        int optionNo = Arrays.asList(options).indexOf(option);
        setSelection(optionNo);
    }
	
	public int getSelectedOptionNo()
	{
		return optionSelected;
	}
	
	public String getSelectedOption()
	{
		return options[optionSelected];
	}
	
	public void setSettingChangedListener(OnSettingChangedListener listener)
	{
		settingListener = listener;
	}
	
	public interface OnSettingChangedListener
	{
		void onSettingChanged();
	}

}
