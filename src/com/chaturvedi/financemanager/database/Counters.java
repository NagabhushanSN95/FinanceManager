package com.chaturvedi.financemanager.database;

public class Counters
{
	private int id;
	private Date date;
	private double[] exp;
	/*private double exp01;
	private double exp02;
	private double exp03;
	private double exp04;
	private double exp05;*/
	private double amountSpent;
	private double income;
	private double savings;
	private double withdrawal;
	
	// Constructor
	public Counters(int id, Date date, double[] exp, double amountSpent, double income, double savings,
			double withdrawal)
	{
		this.id = id;
		this.date = new Date(date);
		this.exp = new double[exp.length];
		for(int i=0; i<exp.length; i++)
		{
			this.exp[i] = exp[i];
		}
		this.setAmountSpent(amountSpent);
		this.setIncome(income);
		this.setSavings(savings);
		this.setWithdrawal(withdrawal);
	}
	
	/*/ Constructor
	public Counters(int id, Date date, double exp01, double exp02, double exp03, double exp04, double exp05,
			double amountSpent, double income, double savings, double withdrawal)
	{
		this.id = id;
		this.date = date;
		this.exp01 = exp01;
		this.exp02 = exp02;
		this.exp03 = exp03;
		this.exp04 = exp04;
		this.exp05 = exp05;
		this.setAmountSpent(amountSpent);
		this.setIncome(income);
		this.setSavings(savings);
		this.setWithdrawal(withdrawal);
	}*/
	
	// Constructor
	public Counters(int id, Date date, double[] exp)
	{
		this.id = id;
		this.date = date;
		int numExpTypes = exp.length-4;
		this.exp = new double[numExpTypes];
		for(int i=0; i<numExpTypes; i++)
		{
			this.exp[i] = exp[i];
		}
		this.amountSpent = exp[numExpTypes];
		this.income = exp[numExpTypes+1];
		this.savings = exp[numExpTypes+2];
		this.withdrawal = exp[numExpTypes+3];
	}
	
	public void increamentCounters(double[] exp)
	{
		int numExpTypes = exp.length-4;
		for(int i=0; i<numExpTypes; i++)
		{
			this.exp[i] += exp[i];
		}
		this.amountSpent += exp[numExpTypes];
		this.income += exp[numExpTypes+1];
		this.savings += exp[numExpTypes+2];
		this.withdrawal += exp[numExpTypes+3];
	}
	
	public void decreamentCounters(double[] exp)
	{
		int numExpTypes = exp.length-4;
		for(int i=0; i<numExpTypes; i++)
		{
			this.exp[i] -= exp[i];
		}
		this.amountSpent -= exp[numExpTypes];
		this.income -= exp[numExpTypes+1];
		this.savings -= exp[numExpTypes+2];
		this.withdrawal -= exp[numExpTypes+3];
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
	 * /
	public void setExp01(double exp01)
	{
		this.exp01 = exp01;
	}

	/**
	 * @return exp01
	 * /
	public double getExp01()
	{
		return exp01;
	}

	/**
	 * @param exp02
	 * /
	public void setExp02(double exp02)
	{
		this.exp02 = exp02;
	}

	/**
	 * @return exp02
	 * /
	public double getExp02()
	{
		return exp02;
	}

	/**
	 * @param exp03
	 * /
	public void setExp03(double exp03)
	{
		this.exp03 = exp03;
	}

	/**
	 * @return exp03
	 * /
	public double getExp03()
	{
		return exp03;
	}

	/**
	 * @param exp04
	 * /
	public void setExp04(double exp04)
	{
		this.exp04 = exp04;
	}

	/**
	 * @return exp04
	 * /
	public double getExp04()
	{
		return exp04;
	}

	/**
	 * @param exp05
	 * /
	public void setExp05(double exp05)
	{
		this.exp05 = exp05;
	}

	/**
	 * @return exp05
	 * /
	public double getExp05()
	{
		return exp05;
	}*/
	
	public void setExp(double[] exp)
	{
		int numExpTypes = exp.length-4;
		for(int i=0; i<numExpTypes; i++)
		{
			this.exp[i] = exp[i];
		}
	}
	
	public double[] getAllExpenditures()
	{
		int numExpTypes = exp.length;
		double[] exp1 = new double[numExpTypes];
		for(int i=0; i<numExpTypes; i++)
		{
			exp1[i] = exp[i];
		}
		return exp1;
	}

	/**
	 * @return the amountSpent
	 */
	public double getAmountSpent() {
		return amountSpent;
	}

	/**
	 * @param amountSpent the amountSpent to set
	 */
	public void setAmountSpent(double amountSpent) {
		this.amountSpent = amountSpent;
	}

	/**
	 * @return the income
	 */
	public double getIncome() {
		return income;
	}

	/**
	 * @param income the income to set
	 */
	public void setIncome(double income) {
		this.income = income;
	}

	/**
	 * @return the savings
	 */
	public double getSavings() {
		return savings;
	}

	/**
	 * @param savings the savings to set
	 */
	public void setSavings(double savings) {
		this.savings = savings;
	}

	/**
	 * @return the withdrawal
	 */
	public double getWithdrawal() {
		return withdrawal;
	}

	/**
	 * @param withdrawal the withdrawal to set
	 */
	public void setWithdrawal(double withdrawal) {
		this.withdrawal = withdrawal;
	}
	
	/**
	 * Adds new Exp Type Value
	 */
	public void addNewExpToCounters(int position)
	{
		double[] newExp = new double[exp.length+1];
		for(int i=0; i<exp.length; i++)
		{
			if(i<position)
			{
				newExp[i] = exp[i];
			}
			else
			{
				newExp[i+1] = exp[i];
			}
		}
		exp = newExp;
	}
}
