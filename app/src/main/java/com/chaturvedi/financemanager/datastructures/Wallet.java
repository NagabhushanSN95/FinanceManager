// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.datastructures;

public class Wallet implements MoneyStorage
{
	private int id;
	private String name;
	private double balance;
	private boolean deleted;

	public Wallet(int id, String name, double balance, boolean deleted)
	{
		this.id = id;
		this.name = name;
		this.balance = balance;
		this.deleted = deleted;
	}

	public Wallet(String id, String name, String balance, String deleted)
	{
		this.id=Integer.parseInt(id);
		this.name=name;
		this.balance=Double.parseDouble(balance);
		this.deleted = Boolean.parseBoolean(deleted);
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
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the balance
	 */
	public double getBalance()
	{
		return balance;
	}
	
	/**
	 * @param balance the balance to set
	 */
	public void setBalance(double balance)
	{
		this.balance = balance;
	}
	
	public void incrementBalance(double amount)
	{
		balance += amount;
	}
	
	public void decrementBalance(double amount)
	{
		balance -= amount;
	}

	public boolean isDeleted()
	{
		return deleted;
	}
	
	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}
}
