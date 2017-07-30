// Shree KRISHNAya Namaha

package com.chaturvedi.financemanager.database;

public interface MoneyStorage
{
	void setID(int id);
	int getID();
	void setName(String name);
	String getName();
	void setBalance(double balance);
	double getBalance();
	void incrementBalance(double amount);
	void decrementBalance(double amount);
}
