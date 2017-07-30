package com.chaturvedi.financemanager.database;

public class ExpenditureTypes
{
	private int id;
	private String expenditureTypeName;
	
	public ExpenditureTypes(int id, String expenditureTypeName)
	{
		this.id=id;
		this.expenditureTypeName=expenditureTypeName;
	}
	
	public ExpenditureTypes(String id, String expenditureTypeName)
	{
		this.id=Integer.parseInt(id);
		this.expenditureTypeName=expenditureTypeName;
	}
	
	public void setId(int id)
	{
		this.id=id;
	}
	
	public int getID()
	{
		return this.id;
	}
	
	public void setExpenditureTypeName(String expenditureTypeName)
	{
		this.expenditureTypeName=expenditureTypeName;
	}
	
	public String getExpenditureTypeName()
	{
		return this.expenditureTypeName;
	}
}
