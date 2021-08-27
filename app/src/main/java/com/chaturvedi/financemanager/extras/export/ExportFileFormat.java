// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.extras.export;

enum ExportFileFormat
{
	PDF("pdf"),
	DOC("doc"),
	HTML("html");
	
	private String fileFormat;
	
	ExportFileFormat(String fileFormat)
	{
		this.fileFormat = fileFormat;
	}
	
	public static ExportFileFormat fromString(String value)
	{
		for (ExportFileFormat ExportFileFormat : ExportFileFormat.values())
		{
			if (ExportFileFormat.toString().equals(value))
			{
				return ExportFileFormat;
			}
		}
		throw new RuntimeException("Invalid File Format: " + value);
	}
	
	@Override
	public String toString()
	{
		return fileFormat;
	}
}
