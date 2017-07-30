package com.chaturvedi.financemanager.database;

public class Wallet
{
	private int id;
	private String name;
	private double amount;
	
	public Wallet(int id, String name, double amount)
	{
		this.id=id;
		this.setName(name);
		this.setAmount(amount);
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
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
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
