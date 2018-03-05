package com.chaturvedi.datastructures;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

@SuppressWarnings("WeakerAccess")
public class Date
{
	private int year;
	private int month;    // 1 -> January
	private int date;    // 1-31
	
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
	
	@SuppressWarnings("unused")
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
				validDate = date > 0 && date <= 31;
			}
			else if(month==4 || month==6 || month==9 || month==11) // Months having 30 days
			{
				validDate = date > 0 && date <= 30;
			}
			else if(month==2) // February
			{
				if (isLeapYear(year)) // Leap Year
				{
					validDate = date > 0 && date <= 29;
				}
				else
				{
					validDate = date > 0 && date <= 28;
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
	 * Deprecated: Use Month Enum instead
	 * @param monthNo
	 * 		1 for January
	 * 		2 for February and so on
	 * @return The month name (January, February,..)
	 */
	@Deprecated
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
	 * Deprecated: Use Month Enum instead
	 * @param fullMonth Month and year in the format
	 *      January-2015
	 *      February-2015
	 * @return The month in the format
	 * 		201501 (For January 2015)
	 * 		201502 (For February 2015) and so on
	 */
	@Deprecated
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
		return (long) (year * 100 + month1);
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
		ArrayList<String> monthsList = new ArrayList<>(12);
		for (Month month : Month.values())
		{
			monthsList.add(month.toString() + "-" + year);
		}
		return monthsList;
	}
	
	public static boolean isLeapYear(int year)
	{
		return ((GregorianCalendar) GregorianCalendar.getInstance()).isLeapYear(year);
	}
	
	/**
	 * Deprecated: Use getDisplayDate("/") instead
	 * Returns a String for this Date object in the form of DD/MM/YYYY
	 *
	 * @return 01/11/2016
	 */
	@Deprecated
	public String getDisplayDate()
	{
		return getDisplayDate("/");
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
	
	/**
	 * Returns a String representing this Date Object in the format DDMMYYYY separated by separator
	 *
	 * @param separator Separator used to separate date, month and year. Eg: "/", "-"
	 * @return String
	 */
	public String getDisplayDate(String separator)
	{
		return this.date + separator + this.month + separator + this.year;
	}
	
	/**
	 * Deprecated: Use getShortDate("/") instead.
	 *
	 * @return a String representing this Date object in the format DD/MM
	 */
	@Deprecated
	public String getShortDate()
	{
		return getShortDate("/");
	}
	
	@SuppressWarnings("SameParameterValue")
	public String getShortDate(String separator)
	{
		return this.date + separator + this.month;
	}
	
	public String getSavableDate()
	{
		DecimalFormat formatter = new DecimalFormat("00");
		return year + "/" + formatter.format(month) + "/" + formatter.format(this.date);
	}
	
	public long getLongDate()
	{
		return (long) (year * 10000 + month * 100 + this.date);
	}
	
	public boolean isNotEqualTo(Date date2)
	{
		return this.getLongDate() != date2.getLongDate();
	}
	
	public int getYear()
	{
		return year;
	}
	
	public void setYear(int year)
	{
		this.year = year;
	}
	
	public int getMonth()
	{
		return month;
	}
	
	public void setMonth(int month)
	{
		this.month = month;
	}
	
	public int getDate()
	{
		return date;
	}
	
	public void setDate(int date)
	{
		this.date = date;
	}
}
