// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.content.SharedPreferences;

import com.chaturvedi.financemanager.functions.Constants;

public class Update124to125
{
	public Update124to125(Context context)
	{
		
		SharedPreferences preferences = context.getSharedPreferences(Constants.ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(Constants.KEY_DAILY_BACKUP, false);
		editor.apply();
	}
}
