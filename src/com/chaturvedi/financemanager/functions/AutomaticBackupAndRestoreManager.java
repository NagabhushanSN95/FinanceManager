// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.functions;

/**
 * 0 - No Backup And Restore
 * 1 - Automatic Backup Only
 * 2 - Automatic Backup. No Restore. But check and inform
 * 3 - Automatic Backup. No Restore. But check and ask
 * 4 - Automatic Backup And Restore.
 * @author Nagabhushan
 *
 */
public class AutomaticBackupAndRestoreManager
{
	private int value = 3;
	
	public AutomaticBackupAndRestoreManager(int val)
	{
		value = val;
	}
	
	public int getValue()
	{
		return value;
	}
	
	public boolean isAutomaticBackup()
	{
		return (value!=0);
	}
	
	public boolean isAutomaticRestore()
	{
		return (value == 4);
	}
}
