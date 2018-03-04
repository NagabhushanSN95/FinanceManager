// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.datastructures;

@SuppressWarnings("unused")
public class Bank implements MoneyStorage
{
	private int id;
	private String name;
	private String accNo;
	private double balance;
	private String smsName;
	private boolean deleted;
	
	public Bank(int id, String name, String accNo, double balance, String smsName, boolean deleted)
	{
		this.id = id;
		this.name = name;
		this.accNo = accNo;
		this.balance = balance;
		this.smsName = smsName;
		this.deleted = deleted;
	}
	
	public Bank(String id, String name, String accNo, String balance, String smsName, String deleted)
	
	{
		this.id=Integer.parseInt(id);
		this.name=name;
		this.accNo=accNo;
		this.balance=Double.parseDouble(balance);
		this.smsName=smsName;
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
	 * @return the accNo
	 */
	public String getAccNo()
	{
		return accNo;
	}
	
	/**
	 * @param accNo the accNo to set
	 */
	public void setAccNo(String accNo)
	{
		this.accNo = accNo;
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
	
	/**
	 * @return the smsName
	 */
	public String getSmsName()
	{
		return smsName;
	}
	
	/**
	 * @param smsName the smsName to set
	 */
	public void setSmsName(String smsName)
	{
		this.smsName = smsName;
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
