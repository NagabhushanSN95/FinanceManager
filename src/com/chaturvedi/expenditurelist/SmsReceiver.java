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
	
	private Intent detailsIntent;
	
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
						detailsIntent = new Intent(context, DetailsActivity.class);
						detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						detailsIntent.putExtra("Bank Sms", true);
						detailsIntent.putExtra("Bank Number", i);
						
						if(sender.toUpperCase().contains("ATMSBI"))
							readSBIMessage();
						else if(sender.toUpperCase().contains("UNIONB"))
							readUBIMessage();
						else if(sender.toUpperCase().contains("SYNDBK"))
							readSyndicateBankMessage();
						else if(sender.toUpperCase().contains("KTKBNK"))
							readKarnatakaBankMessage();
						else if(sender.toUpperCase().contains("ANDBNK"))
							readAndhraBankMessage();
						else
							readBankMessage();
						
						break;
					}
				}
			}
		}
	}

	private void readUBIMessage()
	{
		if(message.toLowerCase().contains("debit"))
		{
			detailsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs", message.indexOf("debited"))+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			detailsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs", message.indexOf("credited"))+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readSBIMessage()
	{
		if(message.toLowerCase().contains("debit"))
		{
			detailsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			double amount = Double.parseDouble(message.substring(startIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("withdraw"))
		{
			detailsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("from", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			detailsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+2;
			double amount = Double.parseDouble(message.substring(startIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readSyndicateBankMessage()
	{
		if(message.toLowerCase().contains("debit"))
		{
			detailsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("has", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			detailsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("has", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readKarnatakaBankMessage()
	{
		if(message.toLowerCase().contains("debit"))
		{
			detailsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			detailsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readAndhraBankMessage()
	{
		if(message.toLowerCase().contains("debit"))
		{
			detailsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+4;
			int endIndex = message.indexOf("is")-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			detailsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+4;
			int endIndex = message.indexOf("is")-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			detailsIntent.putExtra("Amount", amount);
			context.startActivity(detailsIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readBankMessage()
	{
		if(message.toLowerCase().contains("debit"))
		{
			detailsIntent.putExtra("Type", "debit");
			detailsIntent.putExtra("Amount", 0);
			context.startActivity(detailsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			detailsIntent.putExtra("Type", "credit");
			detailsIntent.putExtra("Amount", 0);
			context.startActivity(detailsIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
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
