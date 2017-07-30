package com.chaturvedi.financemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.chaturvedi.financemanager.database.DatabaseManager;

public class SmsReceiver extends BroadcastReceiver
{
	private static final String ALL_PREFERENCES = "AllPreferences";
	private SharedPreferences preferences;
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
	private String bankSmsResponse = "Popup";
	
	private Context context;
	private String sender;
	private String message;
	
	private Intent summaryActivityIntent;
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		this.context = context;
		new DatabaseManager(context);
		DatabaseManager.readDatabase();
		readPreferences();
		
		if(bankSmsResponse.equals("Popup") || bankSmsResponse.equals("Automatic"))
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
						// Set the flag in the Preferences that a SMS has arrived
						SharedPreferences.Editor editor = preferences.edit();
						editor.putBoolean(KEY_BANK_SMS_ARRIVED, true);
						editor.commit();
						
						Toast.makeText(context, "Transaction In "+DatabaseManager.getBank(i).getName()+" Detected. Please Update The Same", Toast.LENGTH_LONG).show();
						summaryActivityIntent = new Intent(context, SummaryActivity.class);
						summaryActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						summaryActivityIntent.putExtra("Bank Sms", true);
						summaryActivityIntent.putExtra("Bank Number", i);
						
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
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs", message.indexOf("debited"))+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			summaryActivityIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs", message.indexOf("credited"))+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readSBIMessage()
	{
		if(message.toLowerCase().contains("to draw rs"))					// Format D01
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			String amountString = message.substring(startIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("withdrawing"))				// Format D02
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("from", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("withdrawn"))				// Format D03
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("withdrawn", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("purchase"))					// Format D04
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("on", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.contains("Internet banking"))						// Format D05
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+3;
			int endIndex = message.indexOf("on", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("credit"))					// Format C01
		{
			summaryActivityIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("on", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.startsWith("Available Balance"))					// Format O01
		{
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf(" ", startIndex);
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			if(amountString.charAt(amountString.length()-1) == '.')
			{
				amountString = amountString.substring(0,amountString.length()-1);
			}
			double newBalance = Double.parseDouble(amountString);
			int bankNo = summaryActivityIntent.getIntExtra("Bank Number", 0);
			double oldBalance = DatabaseManager.getBank(bankNo).getBalance();
			if(newBalance > oldBalance)
			{
				summaryActivityIntent.putExtra("Type", "credit");
				double amount = newBalance - oldBalance;
				summaryActivityIntent.putExtra("Amount", amount);
				context.startActivity(summaryActivityIntent);
			}
			else if(newBalance < oldBalance)
			{
				summaryActivityIntent.putExtra("Type", "debit");
				double amount = oldBalance - newBalance;
				summaryActivityIntent.putExtra("Amount", amount);
				context.startActivity(summaryActivityIntent);
			}
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}
	
	private void readSyndicateBankMessage()
	{
		if(message.toLowerCase().contains("debited to A/C No".toLowerCase()))
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("debited", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("debited to your account"))
		{
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("has", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			summaryActivityIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("INR")+4;
			int endIndex = message.indexOf("has", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
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
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+3;
			int endIndex = message.indexOf("ATM", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			summaryActivityIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+2;
			int endIndex = message.indexOf("on", startIndex)-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
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
			summaryActivityIntent.putExtra("Type", "debit");
			int startIndex = message.indexOf("Rs")+4;
			int endIndex = message.indexOf("is")-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			summaryActivityIntent.putExtra("Type", "credit");
			int startIndex = message.indexOf("Rs")+4;
			int endIndex = message.indexOf("is")-1;
			String amountString = message.substring(startIndex, endIndex);
			amountString = amountString.replaceAll(",", "");
			double amount = Double.parseDouble(amountString);
			summaryActivityIntent.putExtra("Amount", amount);
			context.startActivity(summaryActivityIntent);
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
			summaryActivityIntent.putExtra("Type", "debit");
			summaryActivityIntent.putExtra("Amount", 0);
			context.startActivity(summaryActivityIntent);
		}
		else if(message.toLowerCase().contains("credit"))
		{
			summaryActivityIntent.putExtra("Type", "credit");
			summaryActivityIntent.putExtra("Amount", 0);
			context.startActivity(summaryActivityIntent);
		}
		/*else
		{
			detailsIntent.putExtra("Bank Sms", false);
		}*/
	}

	private void readPreferences()
	{
		preferences = context.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		if(preferences.contains(KEY_RESPOND_BANK_SMS))
		{
			bankSmsResponse=preferences.getString(KEY_RESPOND_BANK_SMS, "Popup");
		}
	}
}