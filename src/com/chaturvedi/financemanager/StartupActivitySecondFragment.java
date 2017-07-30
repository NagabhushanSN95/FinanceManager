package com.chaturvedi.financemanager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StartupActivitySecondFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.activity_setup_expenditure, container, false);
		return v;
	}
	
	public static StartupActivitySecondFragment newInstance(String txt)
	{
		StartupActivitySecondFragment f = new StartupActivitySecondFragment();
		Bundle b = new Bundle();
		b.putString("msg", txt);
		f.setArguments(b);
		return f;
	}
}
