// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.functions;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.chaturvedi.financemanager.BuildConfig;
import com.chaturvedi.financemanager.datastructures.Transaction;
import com.chaturvedi.financemanager.extras.DailyBackupService;

import java.util.Arrays;
import java.util.Calendar;

@SuppressWarnings("WeakerAccess")
public class Utilities
{
	@SuppressWarnings("unused")
	public static void logDebugMode(String message)
	{
		logDebugMode("Finance Manager", message);
	}
	
	public static void logDebugMode(String tag, String message)
	{
		if (BuildConfig.DEBUG)
		{
			Log.d(tag, message);
		}
	}
	
	public static void flowLog(String message)
	{
		if (Constants.FLOW_LOGS_ENABLED && BuildConfig.DEBUG)
		{
			Log.d(Constants.VALUE_FLOW_LOGS, message);
		}
	}
	
	public static void logMethodStart(String className, String methodName, String... args)
	{
		flowLog("Starts " + methodName + "() in " + className + ".java with arguments " + Arrays.toString(args));
	}
	
	public static void logMethodEnd(String className, String methodName, String... returnValue)
	{
		flowLog("Ends " + methodName + "() in " + className + ".java with return value " + Arrays.toString(returnValue));
	}
	
	public static void logMethodCall(String className, String methodName, String... args)
	{
		flowLog("Calling " + methodName + "() from " + className + ".java with parameters " + Arrays.toString(args));
	}
	
	public static void logMethodReturn(String className, String methodName, String... returnValue)
	{
		flowLog("Returned " + methodName + "() to " + className + ".java with return value" + Arrays.toString(returnValue));
	}
	
	public static void setDailyBackupService(Context context, boolean enabled)
	{
		Utilities.logMethodStart(Utilities.class.getName(), "setDailyBackupService");
		if (enabled)
		{
			Utilities.logMethodCall(Utilities.class.getName(), "enableDailyBackupService");
			enableDailyBackupService(context);
			Utilities.logMethodReturn(Utilities.class.getName(), "enableDailyBackupService");
		}
		else
		{
			Utilities.logMethodCall(Utilities.class.getName(), "disableDailyBackupService");
			disableDailyBackupService(context);
			Utilities.logMethodReturn(Utilities.class.getName(), "disableDailyBackupService");
		}
		Utilities.logMethodEnd(Utilities.class.getName(), "setDailyBackupService");
	}
	
	private static void enableDailyBackupService(Context context)
	{
		Utilities.logMethodStart(Utilities.class.getName(), "enableDailyBackupService");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 50);
		calendar.set(Calendar.SECOND, 0);
		
		Intent intent = new Intent(context, DailyBackupService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
		Utilities.logMethodEnd(Utilities.class.getName(), "enableDailyBackupService");
	}
	
	private static void disableDailyBackupService(Context context)
	{
		Utilities.logMethodStart(Utilities.class.getName(), "disableDailyBackupService");
		Intent intent = new Intent(context, DailyBackupService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		Utilities.logMethodEnd(Utilities.class.getName(), "disableDailyBackupService");
	}
	
	public static boolean isTransactionValidForEditing(Context context, Transaction oldTransaction)
	{
		boolean isValid = true;
		TransactionTypeParser parser = new TransactionTypeParser(context, oldTransaction.getType
				());
		if (parser.isIncome())
		{
			if (parser.isIncomeDestinationWallet() && parser.getIncomeDestinationWallet()
					.isDeleted())
			{
				isValid = false;
			}
			else if (parser.isIncomeDestinationBank() && parser.getIncomeDestinationBank()
					.isDeleted())
			{
				isValid = false;
			}
		}
		else if (parser.isExpense())
		{
			if (parser.isExpenseSourceWallet() && parser.getExpenseSourceWallet().isDeleted())
			{
				isValid = false;
			}
			else if (parser.isExpenseSourceBank() && parser.getExpenseSourceBank().isDeleted())
			{
				isValid = false;
			}
			
			if (parser.getExpenditureType().isDeleted())
			{
				isValid = false;
			}
		}
		else if (parser.isTransfer())
		{
			if (parser.isTransferSourceWallet() && parser.getTransferSourceWallet().isDeleted())
			{
				isValid = false;
			}
			else if (parser.isTransferSourceBank() && parser.getTransferSourceBank().isDeleted())
			{
				isValid = false;
			}
			
			if (parser.isTransferDestinationWallet() && parser.getTransferDestinationWallet()
					.isDeleted())
			{
				isValid = false;
			}
			else if (parser.isTransferDestinationBank() && parser.getTransferDestinationBank()
					.isDeleted())
			{
				isValid = false;
			}
		}
		else
		{
			Toast.makeText(context, "Unknown Transaction Type", Toast.LENGTH_LONG).show();
		}
		return !isValid;
	}
}
