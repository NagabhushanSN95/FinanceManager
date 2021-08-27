package com.chaturvedi.financemanager.datastructures;

public class Template
{
	private int id;
	private String particular;
	private String type;
	private double amount;
	private boolean hidden;
	
	// Constructor
	public Template(int id, String particular, String type, double amount, boolean hidden)
	{
		this.id = id;
		this.particular = particular;
		this.type = type;
		this.amount = amount;
		this.hidden = hidden;
	}
	
	public Template(String id, String particular, String type, String amount, String hidden)
	{
		this.id = Integer.parseInt(id);
		this.particular = particular;
		this.type = type;
		this.amount = Double.parseDouble(amount);
		this.hidden = Boolean.parseBoolean(hidden);
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

	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}

	public boolean isHidden()
	{
		return hidden;
	}
}
