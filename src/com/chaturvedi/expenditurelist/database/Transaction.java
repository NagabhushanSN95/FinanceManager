package com.chaturvedi.expenditurelist.database;

public class Transaction
{
	private int id;
	private String date;
	private String type;
	private String particular;
	private double rate;
	private int quantity;
	private double amount;
	
	// Empty constructor
	public Transaction()
	{
		
	}
	
	// Constructor
	public Transaction(int id, String date, String type, String particular, double rate, int quantity, double amount)
	{
		this.setID(id);
		this.setDate(date);
		this.setType(type);
		this.setParticular(particular);
		this.setRate(rate);
		this.setQuantity(quantity);
		this.setAmount(amount);
	}
	
	// Constructor
	public Transaction(String id, String date, String type, String particular, String rate, String quantity, String amount)
	{
		this.setID(Integer.parseInt(id));
		this.setDate(date);
		this.setType(type);
		this.setParticular(particular);
		this.setRate(Double.parseDouble(rate));
		this.setQuantity(Integer.parseInt(quantity));
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
	public void setQuantity(int quantity)
	{
		this.quantity = quantity;
	}

	/**
	 * @return the quantity
	 */
	public int getQuantity()
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
