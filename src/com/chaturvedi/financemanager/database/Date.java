package com.chaturvedi.financemanager.database;

import java.util.Calendar;
import java.util.StringTokenizer;

public class Date
{
	private int year;
	private int month;
	private int date;
	
	public Date(int year, int month, int date)
	{
		this.setYear(year);
		this.setMonth(month);
		this.setDate(date);
	}
	
	public Date(String date)
	{
		StringTokenizer tokens = new StringTokenizer(date,"/");
		this.year = Integer.parseInt(tokens.nextToken());
		this.month = Integer.parseInt(tokens.nextToken());
		this.date = Integer.parseInt(tokens.nextToken());
		
		if(this.year<this.date)
		{
			int temp = this.date;
			this.date = this.year;
			this.year = temp;
		}
	}
	
	public Date(Calendar calendar)
	{
		this.year = calendar.get(Calendar.YEAR);
		this.month = calendar.get(Calendar.MONTH) + 1;
		this.date = calendar.get(Calendar.DATE);
	}
	
	public String getDisplayDate()
	{
		String date = this.date + "/" + month + "/" + year;
		return date;
	}
	
	public String getShortDate()
	{
		String date = this.date + "/" + month;
		return date;
	}
	
	public String getSavableDate()
	{
		String date = year + "/" + month + "/" + this.date;
		return date;
	}
	
	public long getLongDate()
	{
		long date = year*10000 + month*100 + this.date;
		return date;
	}
	
	public boolean isGreaterThan(Date date1, Date date2)
	{
		if(date1.getLongDate()>date2.getLongDate())
			return true;
		else
			return false;
	}
	
	public boolean isLesserThan(Date date1, Date date2)
	{
		if(date1.getLongDate()<date2.getLongDate())
			return true;
		else
			return false;
	}
	
	public boolean isEqualTo(Date date1, Date date2)
	{
		if(date1.getLongDate()==date2.getLongDate())
			return true;
		else
			return false;
	}
	
	public static boolean isValidDate(String dateString)
	{
		boolean validDate = true;
		try
		{
			StringTokenizer tokens = new StringTokenizer(dateString, "/-.,");
			int date = Integer.parseInt(tokens.nextToken());
			int month = Integer.parseInt(tokens.nextToken());
			int year = Integer.parseInt(tokens.nextToken());
			
			if(month==1 || month==3 || month==5 || month==7 || month==8 || month==10 || month==12) // Months Having 31 days
			{
				if(date>0 && date<=31)
					validDate = true;
				else
					validDate = false;
			}
			else if(month==4 || month==6 || month==9 || month==11) // Months having 30 days
			{
				if(date>0 && date<=30)
					validDate = true;
				else
					validDate = false;
			}
			else if(month==2) // February
			{
				if(year%4==0) // Leap Year
				{
					if(date>0 && date<=29)
						validDate = true;
					else
						validDate = false;
				}
				else
				{
					if(date>0 && date<=28)
						validDate = true;
					else
						validDate = false;
				}
			}
		}
		catch(Exception e)
		{
			validDate = false;
		}
		return validDate;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDate() {
		return date;
	}

	public void setDate(int date) {
		this.date = date;
	}
}
