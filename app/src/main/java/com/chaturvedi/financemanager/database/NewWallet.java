// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.database;

public class NewWallet implements MoneyStorage
{
	private int id;
	private String name;
	private double balance;
	private boolean deleted;

	public NewWallet(int id, String name, double balance, boolean deleted)
	{
		this.id = id;
		this.name = name;
		this.balance = balance;
		this.deleted = deleted;
	}

//	public NewWallet(String id, String name, String balance)
//	{
//		this.id=Integer.parseInt(id);
//		this.name=name;
//		this.balance=Double.parseDouble(balance);
//	}
//
//	public NewWallet(int id, String name, String balance)
//	{
//		this.id=id;
//		this.name=name;
//		this.balance=Double.parseDouble(balance);
//	}

	public NewWallet(String id, String name, String balance, String deleted)
	{
		this.id=Integer.parseInt(id);
		this.name=name;
		this.balance=Double.parseDouble(balance);
		this.deleted = Boolean.parseBoolean(deleted);
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
	 * @param balance the balance to set
	 */
	public void setBalance(double balance)
	{
		this.balance = balance;
	}

	/**
	 * @return the balance
	 */
	public double getBalance()
	{
		return balance;
	}
	
	public void incrementBalance(double amount)
	{
		balance += amount;
	}
	
	public void decrementBalance(double amount)
	{
		balance -= amount;
	}

	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}

	public boolean isDeleted()
	{
		return deleted;
	}
}
