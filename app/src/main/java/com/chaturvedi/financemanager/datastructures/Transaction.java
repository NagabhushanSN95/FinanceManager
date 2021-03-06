package com.chaturvedi.financemanager.datastructures;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Time;
import com.chaturvedi.financemanager.functions.TransactionTypeParser;

public class Transaction implements Parcelable
{
	public static final Parcelable.Creator<Transaction> CREATOR = new Parcelable
			.Creator<Transaction>()
	{
		@Override
		public Transaction createFromParcel(Parcel in)
		{
			return new Transaction(in);
		}
		
		@Override
		public Transaction[] newArray(int size)
		{
			return new Transaction[size];
		}
	};
	private int id;
	private Time createdTime; // The Time At Which The Transaction Is Created
	private Time modifiedTime; // The Time At Which The Transaction Was Last Modified
	private Date date; // The Date As Entered By The User
	private String type;
	private String particular;
	private double rate;
	private double quantity;
	private double amount;
	private boolean hidden;
	private boolean includeInCounters;

	public Transaction(int id, Time createdTime, Time modifiedTime, Date date, String type, String particular, double rate, double quantity, double amount, boolean hidden, boolean includeInCounters)
	{
		this.id = id;
		this.createdTime = createdTime;
		this.modifiedTime = modifiedTime;
		this.date = date;
		this.type = type;
		this.particular = particular;
		this.rate = rate;
		this.quantity = quantity;
		this.amount = amount;
		this.hidden = hidden;
		this.includeInCounters = includeInCounters;
	}

	// Constructor
	public Transaction(String id, String createdTime, String modifiedTime, String date, String type, String particular,
					   String rate, String quantity, String amount, String hidden, String includeInCounters)
	{
		this.setID(Integer.parseInt(id));
		this.setCreatedTime(new Time(createdTime));
		this.setModifiedTime(new Time(modifiedTime));
		this.setDate(new Date(date));
		this.setType(type);
		this.setParticular(particular);
		this.setRate(Double.parseDouble(rate));
		this.setQuantity(Double.parseDouble(quantity));
		this.setAmount(Double.parseDouble(amount));
		this.hidden = Boolean.parseBoolean(hidden);
		this.includeInCounters = Boolean.parseBoolean(includeInCounters);
	}

	/**
	 * Clone a Transaction
	 *
	 * @param transaction
	 */
	public Transaction(Transaction transaction)
	{
		this.id = transaction.id;
		this.createdTime = transaction.createdTime;
		this.modifiedTime = transaction.modifiedTime;
		this.date = transaction.date;
		this.type = transaction.type;
		this.particular = transaction.particular;
		this.rate = transaction.rate;
		this.quantity = transaction.quantity;
		this.amount = transaction.amount;
		this.hidden = transaction.hidden;
		this.includeInCounters = transaction.includeInCounters;
	}
	
	// Parcelable Part
	public Transaction(Parcel in)
	{
		String[] data = new String[11];
		in.readStringArray(data);
		id = Integer.parseInt(data[0]);
		createdTime = new Time(data[1]);
		modifiedTime = new Time(data[2]);
		date = new Date(data[3]);
		type = data[4];
		particular = data[5];
		rate = Double.parseDouble(data[6]);
		quantity = Double.parseDouble(data[7]);
		amount = Double.parseDouble(data[8]);
		hidden = Boolean.parseBoolean(data[9]);
		includeInCounters = Boolean.parseBoolean(data[10]);
	}

	/**
	 * @return the id
	 */
	public int getID()
	{
		return id;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setID(int id)
	{
		this.id = id;
	}
	
	/**
	 * @return the time at which the transaction is created
	 */
	public Time getCreatedTime()
	{
		return createdTime;
	}

	/**
	 * @param createdTime The Time at which the transaction is created
	 */
	public void setCreatedTime(Time createdTime)
	{
		this.createdTime = createdTime;
	}

	/**
	 * @return the time at which the transaction is created
	 */
	public Time getModifiedTime()
	{
		return modifiedTime;
	}

	/**
	 * @param modifiedTime The Time at which the transaction is modified
	 */
	public void setModifiedTime(Time modifiedTime)
	{
		this.modifiedTime = modifiedTime;
	}

	/**
	 * @return the date
	 */
	public Date getDate()
	{
		return date;
	}
	
	/**
	 * @param date the date to set
	 */
	public void setDate(Date date)
	{
		this.date = date;
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @return the particular
	 */
	public String getParticular()
	{
		return particular;
	}
	
	/**
	 * @param particular the particular to set
	 */
	public void setParticular(String particular)
	{
		this.particular = particular;
	}

	/**
	 * @return the particular in the format
	 * Credit to State Bank Of India: MHRD Scholarship for BE 1st year
	 * Debit from HDFC Bank: Fruits
	 * Transfer from HDFC Bank to Wallet:
	 */
	public String getDisplayParticular(Context context)
	{
		TransactionTypeParser parser = new TransactionTypeParser(context, type);
		String fullParticular = "";
		if(parser.isIncome())
		{
			fullParticular += parser.getIncomeDestinationName();
		}
		else if(parser.isExpense())
		{
			fullParticular += parser.getExpenseSourceName();
		}
		else if(parser.isTransfer())
		{
			fullParticular += parser.getTransferSourceName() + " -> " + parser.getTransferDestinationName();
		}
		else
		{
			Toast.makeText(context, "Unknown Transaction Type\nTransaction/getDisplayParticular()", Toast.LENGTH_LONG).show();
		}
		fullParticular += ":\n" + particular;

		/*if(particular.equals(""))
		{
			fullParticular += ": " + particular;
		}
		else
		{
			fullParticular = particular;
		}*/
		return fullParticular;
	}

	/**
	 * @return the rate
	 */
	public double getRate()
	{
		return rate;
	}
	
	/**
	 * @param rate the rate to set
	 */
	public void setRate(double rate)
	{
		this.rate = rate;
	}

	/**
	 * @return the quantity
	 */
	public double getQuantity()
	{
		return quantity;
	}
	
	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(double quantity)
	{
		this.quantity = quantity;
	}
	
	/**
	 * @return the amount
	 */
	public double getAmount()
	{
		return amount;
	}
	
	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount)
	{
		this.amount = amount;
	}
	
	public boolean isHidden()
	{
		return hidden;
	}
	
	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}
	
	public boolean isIncludeInCounters()
	{
		return includeInCounters;
	}
	
	public void setIncludeInCounters(boolean includeInCounters)
	{
		this.includeInCounters = includeInCounters;
	}

	/**
	 * Checks if the passed transaction is equal to this transaction except Modified Time, Particulars and Hidden
	 * This is used while editing a transaction. If these are the only changes, only transaction needs to be updated and
	 * nothing else
	 * @param transaction2 Transaction to compare
	 * @return true if this transaction differs only in Particulars
	 * 			false otherwise
	 */
	public boolean differOnlyInParticular(Transaction transaction2)
	{
		if(id != transaction2.id)
		{
			return false;
		}
		if(!createdTime.isEqualTo(transaction2.getCreatedTime()))
		{
			return false;
		}
		if(!modifiedTime.isEqualTo(transaction2.modifiedTime))
		{
			return false;
		}
		if(date.isNotEqualTo(transaction2.date))
		{
			return false;
		}
		if(!type.equalsIgnoreCase(transaction2.type))
		{
			return false;
		}
		if(rate != transaction2.rate)
		{
			return false;
		}
		if(quantity != transaction2.quantity)
		{
			return false;
		}
		if(amount != transaction2.amount)
		{
			return false;
		}
		if(hidden != transaction2.hidden)
		{
			return false;
		}
		return includeInCounters == transaction2.includeInCounters;
	}

	public String createTag()
	{
		return date.getYear() + "-" + id;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		String[] data = {String.valueOf(id), createdTime.toString(), modifiedTime.toString(), date.getSavableDate(), type,
				particular, String.valueOf(rate), String.valueOf(quantity), String.valueOf(amount), String.valueOf(hidden),
				String.valueOf(includeInCounters)};
		dest.writeStringArray(data);
	}

}
