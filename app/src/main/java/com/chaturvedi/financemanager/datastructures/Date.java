package com.chaturvedi.financemanager.datastructures;

import com.chaturvedi.financemanager.functions.Constants;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
	
	public Date(Date date1)
	{
		this.year = date1.getYear();
		this.month = date1.getMonth();
		this.date = date1.getDate();
	}

	/**
	 *
	 * @return 01/11/2016
	 */
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
		DecimalFormat formatter = new DecimalFormat("00");
		String date = year + "/" + formatter.format(month) + "/" + formatter.format(this.date);
		return date;
	}
	
	public long getLongDate()
	{
		long date = year*10000 + month*100 + this.date;
		return date;
	}
	
	/*public boolean isGreaterThan(Date date2)
	{
		if(this.getLongDate()>date2.getLongDate())
			return true;
		else
			return false;
	}
	
	public boolean isLesserThan(Date date2)
	{
		if(this.getLongDate()<date2.getLongDate())
			return true;
		else
			return false;
	}
	
	public boolean isEqualTo(Date date2)
	{
		if(this.getLongDate()==date2.getLongDate())
			return true;
		else
			return false;
	}*/
	
	public boolean isNotEqualTo(Date date2)
	{
		if(this.getLongDate()!=date2.getLongDate())
			return true;
		else
			return false;
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
	
	/**
	 * @param monthNo
	 * 		1 for January
	 * 		2 for February and so on
	 * @return The month name (January, February,..)
	 */
	public static String getMonthName(int monthNo)
	{
		switch(monthNo)
		{
			case 1:
				return "January";
				
			case 2:
				return "February";
				
			case 3:
				return "March";
				
			case 4:
				return "April";
				
			case 5:
				return "May";
				
			case 6:
				return "June";
				
			case 7:
				return "July";
				
			case 8:
				return "August";
				
			case 9:
				return "September";
				
			case 10:
				return "October";
				
			case 11:
				return "November";
				
			case 12:
				return "December";
				
			default:
				return "";
		}
	}
	
	/**
	 * @param fullMonth Month and year in the format
	 *      January-2015
	 *      February-2015
	 * @return The month in the format 
	 * 		201501 (For January 2015)
	 * 		201502 (For February 2015) and so on
	 */
	public static long getLongMonth(String fullMonth)
	{
		StringTokenizer tokens = new StringTokenizer(fullMonth, "-");
		String month = tokens.nextToken();
		int month1;
		int year = Integer.parseInt(tokens.nextToken().trim());
		
		if(month.contains("January"))
		{
			month1 = 1;
		}
		else if(month.contains("February"))
		{
			month1 = 2;
		}
		else if(month.contains("March"))
		{
			month1 = 3;
		}
		else if(month.contains("April"))
		{
			month1 = 4;
		}
		else if(month.contains("May"))
		{
			month1 = 5;
		}
		else if(month.contains("June"))
		{
			month1 = 6;
		}
		else if(month.contains("July"))
		{
			month1 = 7;
		}
		else if(month.contains("August"))
		{
			month1 = 8;
		}
		else if(month.contains("September"))
		{
			month1 = 9;
		}
		else if(month.contains("October"))
		{
			month1 = 10;
		}
		else if(month.contains("November"))
		{
			month1 = 11;
		}
		else if(month.contains("December"))
		{
			month1 = 12;
		}
		else
		{
			month1 = 1;
		}
		long longMonth = year*100 + month1;
		return longMonth;
	}
	
	/**
	 * @param year Year
	 *             2015
	 * @return List of months in the given year
	 * January-2015
	 * February-2015
	 * .
	 * .
	 * December-2015
	 */
	public static ArrayList<String> getMonthsList(String year)
	{
		ArrayList<String> monthsList = new ArrayList<String>(12);
		for (String month : Constants.MONTHS_IN_YEAR_ARRAY)
		{
			monthsList.add(month + "-" + year);
		}
		return monthsList;
	}
}
