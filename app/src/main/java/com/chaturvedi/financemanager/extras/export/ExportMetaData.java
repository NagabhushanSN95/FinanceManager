package com.chaturvedi.financemanager.extras.export;

import com.chaturvedi.customviews.IntervalSelector;
import com.chaturvedi.datastructures.Date;

class ExportMetaData
{
	private String exportFileName;
	private ExportFileFormat exportFileFormat;
	private IntervalSelector.IntervalType intervalType;
	private Date startDate;
	private Date endDate;
	private boolean includeTransactionType;
	private boolean includeRateQuantity;
	private boolean includeCurrentWalletBankBalances;
	
	ExportMetaData()
	{
	}
	
	ExportMetaData(String exportFileName, ExportFileFormat exportFileFormat,
				   IntervalSelector.IntervalType intervalType, Date startDate, Date
						   endDate, boolean includeTransactionType, boolean
						   includeRateQuantity, boolean includeCurrentWalletBankBalances)
	{
		this.exportFileName = exportFileName;
		this.exportFileFormat = exportFileFormat;
		this.intervalType = intervalType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.includeTransactionType = includeTransactionType;
		this.includeRateQuantity = includeRateQuantity;
		this.includeCurrentWalletBankBalances = includeCurrentWalletBankBalances;
	}
	
	String getExportFileName()
	{
		return exportFileName;
	}
	
	public void setExportFileName(String exportFileName)
	{
		this.exportFileName = exportFileName;
	}
	
	ExportFileFormat getExportFileFormat()
	{
		return exportFileFormat;
	}
	
	public void setExportFileFormat(ExportFileFormat exportFileFormat)
	{
		this.exportFileFormat = exportFileFormat;
	}
	
	IntervalSelector.IntervalType getIntervalType()
	{
		return intervalType;
	}
	
	public void setIntervalType(IntervalSelector.IntervalType intervalType)
	{
		this.intervalType = intervalType;
	}
	
	Date getStartDate()
	{
		return startDate;
	}
	
	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}
	
	Date getEndDate()
	{
		return endDate;
	}
	
	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}
	
	boolean isIncludeTransactionType()
	{
		return includeTransactionType;
	}
	
	public void setIncludeTransactionType(boolean includeTransactionType)
	{
		this.includeTransactionType = includeTransactionType;
	}
	
	boolean isIncludeRateQuantity()
	{
		return includeRateQuantity;
	}
	
	public void setIncludeRateQuantity(boolean includeRateQuantity)
	{
		this.includeRateQuantity = includeRateQuantity;
	}
	
	boolean isIncludeCurrentWalletBankBalances()
	{
		return includeCurrentWalletBankBalances;
	}
	
	public void setIncludeCurrentWalletBankBalances(boolean includeCurrentWalletBankBalances)
	{
		this.includeCurrentWalletBankBalances = includeCurrentWalletBankBalances;
	}
}
