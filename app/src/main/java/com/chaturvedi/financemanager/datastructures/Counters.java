package com.chaturvedi.financemanager.datastructures;

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
	
	/**
	 * Increament a single counter
	 * @param counterNo starts with 0
	 * @param amount amount to be added
	 */
	public void increamentCounter(int counterNo, double amount)
	{
		if(counterNo < exp.length)
		{
			exp[counterNo] += amount;
		}
		else
		{
			switch (counterNo-exp.length)
			{
				case 0:
					amountSpent += amount;
					break;
				
				case 1:
					income += amount;
					break;
				
				case 2:
					savings += amount;
					break;
				
				case 3:
					withdrawal += amount;
					break;
			}
		}
	}
	
	/**
	 * Decreament a single counter
	 * @param counterNo starts with 0
	 * @param amount amount to be subtracted
	 */
	public void decreamentCounter(int counterNo, double amount)
	{
		if(counterNo < exp.length)
		{
			exp[counterNo] -= amount;
		}
		else
		{
			switch (counterNo-exp.length)
			{
				case 0:
					amountSpent -= amount;
					break;
				
				case 1:
					income -= amount;
					break;
				
				case 2:
					savings -= amount;
					break;
				
				case 3:
					withdrawal -= amount;
					break;
			}
		}
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
	
	/**
	 * Delete an Existing Exp Type Value
	 */
	public void deleteExpTypeInCounters(int position)
	{
		double oldExp = exp[position];
		double[] newExp = new double[exp.length-1];
		for(int i=0; i<exp.length-1; i++)
		{
			if(i<position)
			{
				newExp[i] = exp[i];
			}
			else
			{
				newExp[i] = exp[i+1];
			}
		}
		newExp[newExp.length-1] += oldExp;
		exp = newExp;
	}
}
