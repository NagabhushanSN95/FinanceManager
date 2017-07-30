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
	private double amountSpent;
	private double income;
	private double savings;
	private double withdrawal;
	
	// Constructor
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
		//this.amountSpent = exp[0] + exp[1] + exp[2] + exp[3] + exp[4];
		this.setAmountSpent(exp[5]);
		this.setIncome(exp[6]);
		this.setSavings(exp[7]);
		this.setWithdrawal(exp[8]);
	}
	
	public void increamentCounters(double[] exp)
	{
		this.exp01 += exp[0];
		this.exp02 += exp[1];
		this.exp03 += exp[2];
		this.exp04 += exp[3];
		this.exp05 += exp[4];
		this.setAmountSpent(this.getAmountSpent() + exp[5]);
		this.setIncome(this.getIncome() + exp[6]);
		this.setSavings(this.getSavings() + exp[7]);
		this.setWithdrawal(this.getWithdrawal() + exp[8]);
	}
	
	public void decreamentCounters(double[] exp)
	{
		this.exp01 -= exp[0];
		this.exp02 -= exp[1];
		this.exp03 -= exp[2];
		this.exp04 -= exp[3];
		this.exp05 -= exp[4];
		this.setAmountSpent(this.getAmountSpent() - exp[5]);
		this.setIncome(this.getIncome() - exp[6]);
		this.setSavings(this.getSavings() - exp[7]);
		this.setWithdrawal(this.getWithdrawal() - exp[8]);
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
}