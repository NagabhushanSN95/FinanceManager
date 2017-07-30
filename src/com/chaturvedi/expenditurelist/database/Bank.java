package com.chaturvedi.expenditurelist.database;

public class Bank
{
	private int id;
	private String name;
	private String accNo;
	private double balance;
	private String smsName;
	
	public Bank()
	{
		
	}
	
	public Bank(int id, String name, String accNo, double balance, String smsName)
	{
		this.setID(id);
		this.setName(name);
		this.setAccNo(accNo);
		this.setBalance(balance);
		this.setSmsName(smsName);
	}
	
	public Bank(String id, String name, String accNo, String balance, String smsName)
	{
		this.id=Integer.parseInt(id);
		this.name=name;
		this.accNo=accNo;
		this.balance=Double.parseDouble(balance);
		this.smsName=smsName;
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
	 * @param accNo the accNo to set
	 */
	public void setAccNo(String accNo)
	{
		this.accNo = accNo;
	}

	/**
	 * @return the accNo
	 */
	public String getAccNo()
	{
		return accNo;
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

	/**
	 * @param smsName the smsName to set
	 */
	public void setSmsName(String smsName)
	{
		this.smsName = smsName;
	}

	/**
	 * @return the smsName
	 */
	public String getSmsName()
	{
		return smsName;
	}
}
