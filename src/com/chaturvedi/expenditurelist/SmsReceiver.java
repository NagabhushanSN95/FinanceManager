package com.chaturvedi.expenditurelist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.chaturvedi.expenditurelist.database.DatabaseManager;

public class SmsReceiver extends BroadcastReceiver
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_BANK_SMS = "respond_bank_messages";
	private boolean respondBankMessages = true;
	
	private Context context;
	private String sender;
	private String message;
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		new DatabaseManager(context);
		this.context = context;
		DatabaseManager.readDatabase();
		readPreferences();
		
		if(respondBankMessages)
		{
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;
			if (bundle != null)
			{
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];
				for (int i=0; i<msgs.length; i++)
				{
					msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
					if (i==0) 
					{
						sender= msgs[i].getOriginatingAddress();
						message=msgs[i].getMessageBody().toString();
					}
				}
				for(int i=0; i<DatabaseManager.getNumBanks(); i++)
				{
					if(sender.toLowerCase().contains(DatabaseManager.getBankSmsName(i).toLowerCase()))
					{
						Toast.makeText(context, "Transaction In "+DatabaseManager.getBankName(i)+" Detected. Please Update The Same", Toast.LENGTH_LONG).show();
						Intent detailsIntent = new Intent(context, DetailsActivity.class);
						detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(detailsIntent);
						break;
					}
				}
			}
		}
	}

	private void readPreferences()
	{
		SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_SETTINGS, 0);
		if(preferences.contains(KEY_BANK_SMS))
		{
			respondBankMessages=preferences.getBoolean(KEY_BANK_SMS, true);
		}
	}
}
