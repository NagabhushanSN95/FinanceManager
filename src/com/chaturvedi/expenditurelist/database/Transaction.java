package com.chaturvedi.expenditurelist.database;

public class Transaction
{
	private int id;
	private Time createdTime; // The Time At Which The Transaction Is Created
	private Time modifiedTime; // The Time At Which The Transaction Was Last Modified
	private String date; // The Date As Entered By The User
	private String type;
	private String particular;
	private double rate;
	private double quantity;
	private double amount;
	
	// Empty constructor
	public Transaction()
	{
		
	}
	
	// Constructor
	public Transaction(int id, Time createdTime, Time modifiedTime, String date, String type, String particular, double rate, double quantity, double amount)
	{
		this.setID(id);
		this.setCreatedTime(createdTime);
		this.setModifiedTime(modifiedTime);
		this.setDate(date);
		this.setType(type);
		this.setParticular(particular);
		this.setRate(rate);
		this.setQuantity(quantity);
		this.setAmount(amount);
	}
	
	// Constructor
	public Transaction(String id, Time createdTime, Time modifiedTime, String date, String type, String particular, String rate, String quantity, String amount)
	{
		this.setID(Integer.parseInt(id));
		this.setCreatedTime(createdTime);
		this.setModifiedTime(modifiedTime);
		this.setDate(date);
		this.setType(type);
		this.setParticular(particular);
		this.setRate(Double.parseDouble(rate));
		this.setQuantity(Double.parseDouble(quantity));
		this.setAmount(Double.parseDouble(amount));
	}

	/**
	 * @param id the id to set
	 */
	public void setID(int id)
	{
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getID()
	{
		return id;
	}

	/**
	 * @param time The Time at which the transaction is created
	 */
	public void setCreatedTime(Time createdTime)
	{
		this.createdTime = createdTime;
	}

	/**
	 * @return the time at which the transaction is created
	 */
	public Time getCreatedTime()
	{
		return createdTime;
	}

	/**
	 * @param time The Time at which the transaction is created
	 */
	public void setModifiedTime(Time modifiedTime)
	{
		this.modifiedTime = modifiedTime;
	}

	/**
	 * @return the time at which the transaction is created
	 */
	public Time getModifiedTime()
	{
		return modifiedTime;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(String date)
	{
		this.date = date;
	}

	/**
	 * @return the date
	 */
	public String getDate()
	{
		return date;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @param particular the particular to set
	 */
	public void setParticular(String particular)
	{
		this.particular = particular;
	}

	/**
	 * @return the particular
	 */
	public String getParticular()
	{
		return particular;
	}

	/**
	 * @param rate the rate to set
	 */
	public void setRate(double rate)
	{
		this.rate = rate;
	}

	/**
	 * @return the rate
	 */
	public double getRate()
	{
		return rate;
	}

	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(double quantity)
	{
		this.quantity = quantity;
	}

	/**
	 * @return the quantity
	 */
	public double getQuantity()
	{
		return quantity;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount)
	{
		this.amount = amount;
	}

	/**
	 * @return the amount
	 */
	public double getAmount()
	{
		return amount;
	}
	
	
}
