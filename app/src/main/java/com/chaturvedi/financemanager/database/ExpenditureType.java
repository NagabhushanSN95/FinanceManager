// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.database;

public class ExpenditureType
{
	private int id;
	private String name;
	private boolean deleted;

	public ExpenditureType(int id, String name, boolean deleted)
	{
		this.id = id;
		this.name = name;
		this.deleted = deleted;
	}

	public ExpenditureType(String id, String name, String deleted)
	{
		this.id = Integer.parseInt(id);
		this.name = name;
		this.deleted = Boolean.parseBoolean(deleted);
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public boolean isDeleted()
	{

		return deleted;
	}
}
