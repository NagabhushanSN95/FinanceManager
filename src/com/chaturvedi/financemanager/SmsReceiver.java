package com.chaturvedi.financemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;

public class SmsReceiver extends BroadcastReceiver
{
	private static final String SHARED_PREFERENCES_SETTINGS = "Settings";
	private static final String KEY_BANK_SMS = "respond_bank_messages";
	private boolean respondBankMessages = true;
	
	private Context context;
	private String sender;
	private String message;
	
	private Intent transactionsIntent;
	
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
					if(sender.toLowerCase().contains(DatabaseManager.getBank(i).getSmsName().toLowerCase()))
					{
						Toast.makeText(context, "Transaction In "+DatabaseManager.getBank(i).getName()+" Detected. Please Update The Same", Toast.LENGTH_LONG).show();
						transactionsIntent = new Intent(context, TransactionsActivity.class);
						transactionsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						transactionsIntent.putExtra("Bank Sms", true);
						transactionsIntent.putExtra("Bank Number", i);
						// Add ActivityStack As DetailsActivity
						TaskStackBuilder stackBuilder=TaskStackBuilder.create(context);
						stackBuilder.addParentStack(SummaryActivity.class);
						stackBuilder.addNextIntent(transactionsIntent);
						
						if(sender.toUpperCase().contains("SBI"))
							readSBIMessage();
						else if(sender.toUpperCase().contains("UNION"))
							readUBIMessage();
						else if(sender.toUpperCase().contains("SYND"))
							readSyndicateBankMessage();
						else if(sender.toUpperCase().contains("KTK"))
							readKarnatakaBankMessage();
						else if(sender.toUpperCase().contains("AND"))
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
			transactionsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs", message.indexOf("debited"))+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			transactionsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs", message.indexOf("credited"))+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
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
			transactionsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			double amount = Double.parseDouble(message.substring(startIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("withdraw"))
		{
			transactionsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("from", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			transactionsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+2;
			double amount = Double.parseDouble(message.substring(startIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
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
			transactionsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("has", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			transactionsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("has", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
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
			transactionsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+3;
			int endIndex = message.indexOf("ATM", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			transactionsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("on", startIndex)-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
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
			transactionsIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+4;
			int endIndex = message.indexOf("is")-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			transactionsIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+4;
			int endIndex = message.indexOf("is")-1;
			double amount = Double.parseDouble(message.substring(startIndex, endIndex));
			transactionsIntent.putExtra("Amount", amount);
			context.startActivity(transactionsIntent);
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
			transactionsIntent.putExtra("Type", "debit");
			transactionsIntent.putExtra("Amount", 0);
			context.startActivity(transactionsIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			transactionsIntent.putExtra("Type", "credit");
			transactionsIntent.putExtra("Amount", 0);
			context.startActivity(transactionsIntent);
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
