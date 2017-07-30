package com.chaturvedi.financemanager.updates;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class Update89To95
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_AUTOMATIC_BACKUP_RESTORE = "AutomaticBackupAndRestore";
	
	
	public Update89To95(Context context)
	{
		Toast.makeText(context, "Updating The App", Toast.LENGTH_LONG).show();
		
		SharedPreferences preferences = context.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(KEY_AUTOMATIC_BACKUP_RESTORE, 3);
		editor.commit();
	}
}
