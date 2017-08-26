// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.extras;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.chaturvedi.financemanager.functions.Utilities;

public class DailyBackupService extends Service
{
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Utilities.logMethodStart(DailyBackupService.class.getName(), "onStartCommand");
		Utilities.logMethodCall(DailyBackupService.class.getName(), "backupData");
		backupData();
		Utilities.logMethodReturn(DailyBackupService.class.getName(), "backupData");
		Utilities.logMethodEnd(DailyBackupService.class.getName(), "onStartCommand", String.valueOf(Service.START_REDELIVER_INTENT));
		return Service.START_REDELIVER_INTENT;
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	private void backupData()
	{
		Utilities.logMethodStart(DailyBackupService.class.getName(), "backupData");
		/* Backup in a separate non-ui thread */
		Thread backupThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Utilities.logMethodCall(DailyBackupService.class.getName(), "dailyBackup");
				// Backs Up Data to Daily Backups Folder
				boolean result = new BackupManager(DailyBackupService.this).dailyBackup();
				Utilities.logMethodReturn(DailyBackupService.class.getName(), "dailyBackup", String.valueOf(result));
				if (!result)
				{
					// Todo: Add a message that daily backup failed.
					// This message should be displayed when the app is opened for the next time
				}
			}
		});
		backupThread.start();
		Utilities.logMethodEnd(DailyBackupService.class.getName(), "backupData");
	}
}
