package com.chaturvedi.expenditurelist.database;

public class Counters
{
	private int id;
	private Date date;
	private double exp01;
	private double exp02;
	private double exp03;
	private double exp04;
	private double exp05;
	
	// Constructor
	public Counters(int id, Date date, double exp01, double exp02, double exp03, double exp04, double exp05)
	{
		this.id = id;
		this.date = date;
		this.exp01 = exp01;
		this.exp02 = exp02;
		this.exp03 = exp03;
		this.exp04 = exp04;
		this.exp05 = exp05;
	}
	
	// Constructor
	public Counters(Date date, double[] exp)
	{
		this.id = 0;
		this.date = date;
		this.exp01 = exp[0];
		this.exp02 = exp[1];
		this.exp03 = exp[2];
		this.exp04 = exp[3];
		this.exp05 = exp[4];
	}
	
	public void increamentCounters(double[] exp)
	{
		this.exp01 += exp[0];
		this.exp02 += exp[1];
		this.exp03 += exp[2];
		this.exp04 += exp[3];
		this.exp05 += exp[4];
	}
	
	public void decreamentCounters(double[] exp)
	{
		this.exp01 -= exp[0];
		this.exp02 -= exp[1];
		this.exp03 -= exp[2];
		this.exp04 -= exp[3];
		this.exp05 -= exp[4];
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
	public void setDate(Date date)
	{
		this.date = date;
	}

	/**
	 * @return the date
	 */
	public Date getDate()
	{
		return date;
	}

	/**
	 * @param exp01
	 */
	public void setExp01(double exp01)
	{
		this.exp01 = exp01;
	}

	/**
	 * @return exp01
	 */
	public double getExp01()
	{
		return exp01;
	}

	/**
	 * @param exp02
	 */
	public void setExp02(double exp02)
	{
		this.exp02 = exp02;
	}

	/**
	 * @return exp02
	 */
	public double getExp02()
	{
		return exp02;
	}

	/**
	 * @param exp03
	 */
	public void setExp03(double exp03)
	{
		this.exp03 = exp03;
	}

	/**
	 * @return exp03
	 */
	public double getExp03()
	{
		return exp03;
	}

	/**
	 * @param exp04
	 */
	public void setExp04(double exp04)
	{
		this.exp04 = exp04;
	}

	/**
	 * @return exp04
	 */
	public double getExp04()
	{
		return exp04;
	}

	/**
	 * @param exp05
	 */
	public void setExp05(double exp05)
	{
		this.exp05 = exp05;
	}

	/**
	 * @return exp05
	 */
	public double getExp05()
	{
		return exp05;
	}
}
