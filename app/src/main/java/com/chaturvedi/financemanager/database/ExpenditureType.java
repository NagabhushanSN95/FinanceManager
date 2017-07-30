// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.database;

public class ExpenditureType
{
	private int id;
	private String name;

	public ExpenditureType(int id, String name)
	{
		this.id = id;
		this.name = name;
	}

	public ExpenditureType(String id, String name)
	{
		this.id = Integer.parseInt(id);
		this.name = name;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
}
