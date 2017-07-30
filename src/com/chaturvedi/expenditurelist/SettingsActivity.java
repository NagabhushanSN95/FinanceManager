package com.chaturvedi.expenditurelist;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.RelativeLayout;

public class SettingsActivity extends Activity
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_ENABLE_SPLASH = "enable_splash";
	private static final String KEY_BANK_SMS = "respond_bank_messages";
	
	private CheckBox splashCheckBox;
	private CheckBox bankSmsCheckBox;
	private static boolean enableSplash=true;
	private static boolean respondBankMessages = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_settings);
		}
		else
		{
			setContentView(R.layout.activity_settings);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		readPreferences();
		splashCheckBox=(CheckBox)findViewById(R.id.checkBox_splash);
		splashCheckBox.setChecked(enableSplash);
		bankSmsCheckBox = (CheckBox)findViewById(R.id.checkBox_bank_sms);
		bankSmsCheckBox.setChecked(respondBankMessages);
	}
	
	@Override
	public void onBackPressed()
	{
		savePreferences();
		super.onBackPressed();
	}
	
	private void readPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_ENABLE_SPLASH))
		{
			enableSplash=preferences.getBoolean(KEY_ENABLE_SPLASH, true);
		}
		if(preferences.contains(KEY_BANK_SMS))
		{
			respondBankMessages=preferences.getBoolean(KEY_BANK_SMS, true);
		}
	}
	
	private void savePreferences()
	{
		enableSplash=splashCheckBox.isChecked();
		respondBankMessages = bankSmsCheckBox.isChecked();
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(KEY_ENABLE_SPLASH, enableSplash);
		editor.putBoolean(KEY_BANK_SMS, respondBankMessages);
		editor.commit();
	}
}
