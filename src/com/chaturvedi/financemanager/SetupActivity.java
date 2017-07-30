package com.chaturvedi.financemanager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class SetupActivity extends FragmentActivity
{
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_NEXT_BUTTON;
	private int HEIGHT_NEXT_BUTTON;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);
		
		ViewPager pager = (ViewPager) findViewById(R.id.viewPager);
		pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
		
		calculateDimensions();
		buildLayout();
		
	}
	
	/**
	 * Calculate the values of various Dimension Fields
	 */
	private void calculateDimensions()
	{
		displayMetrics=new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		screenWidth=displayMetrics.widthPixels;
		screenHeight=displayMetrics.heightPixels;

		WIDTH_NEXT_BUTTON=screenWidth*60/100;
		HEIGHT_NEXT_BUTTON=screenHeight*10/100;
	}
	
	private void buildLayout()
	{
		Button nextButton = (Button) findViewById(R.id.button_next);
		RelativeLayout.LayoutParams nextButtonParams = (LayoutParams) nextButton.getLayoutParams();
		nextButtonParams.width = WIDTH_NEXT_BUTTON;
		nextButtonParams.height = HEIGHT_NEXT_BUTTON;
		//nextButton.setLayoutParams(nextButtonParams);
	}
	
	private class MyPagerAdapter extends FragmentPagerAdapter
	{
		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}
		
		@Override
		public Fragment getItem(int pos)
		{
			switch(pos)
			{
			case 0: return WalletSetupFragment.newInstance("Krishna");
			case 1: return StartupActivitySecondFragment.newInstance("Rama");
			default: return WalletSetupFragment.newInstance("Ramakrishna");
			}
		}
		
		@Override
		public int getCount()
		{
			return 2;
		}
	}
}
