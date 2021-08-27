// Shree KRISHNAya Namaha

package com.chaturvedi.datastructures;

public enum Month
{
	JANUARY("January", 1),
	FEBRUARY("February", 2),
	MARCH("March", 3),
	APRIL("April", 4),
	MAY("May", 5),
	JUNE("June", 6),
	JULY("July", 7),
	AUGUST("August", 8),
	SEPTEMBER("September", 9),
	OCTOBER("October", 10),
	NOVEMBER("November", 11),
	DECEMBER("December", 12);
	
	private String value;
	private int monthNo;
	
	Month(String value, int monthNo)
	{
		this.value = value;
		this.monthNo = monthNo;
	}
	
	public static Month fromMonthName(String monthName)
	{
		for (Month month : values())
		{
			if (month.value.equals(monthName))
			{
				return month;
			}
		}
		throw new IllegalArgumentException("Illegal Month: " + monthName);
	}
	
	public static Month fromMonthNo(int monthNo)
	{
		for (Month month : values())
		{
			if (month.monthNo == monthNo)
			{
				return month;
			}
		}
		throw new IllegalArgumentException("Illegal Month Number: " + monthNo);
	}
	
	@Override
	public String toString()
	{
		return value;
	}
	
	public int getMonthNo()
	{
		return monthNo;
	}
	
	public int getLastDate(int year)
	{
		switch (monthNo)
		{
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				return 31;
			
			case 4:
			case 6:
			case 9:
			case 11:
				return 30;
			
			case 2:
				return Date.isLeapYear(year) ? 29 : 28;
			
			default:
				throw new IllegalArgumentException("Invalid Month Number: " + monthNo + " for " +
						"month: " + value);
		}
	}
}
