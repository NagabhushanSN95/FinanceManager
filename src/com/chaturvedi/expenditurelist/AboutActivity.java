package com.chaturvedi.expenditurelist;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AboutActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(VERSION.SDK_INT<=10)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.activity_about);
		}
		else
		{
			setContentView(R.layout.activity_about);
			RelativeLayout actionBar=(RelativeLayout)findViewById(R.id.action_bar);
			actionBar.setVisibility(View.GONE);
		}
		
		TextView devotionView = (TextView)findViewById(R.id.devotion);
		devotionView.setText("Devoted To Lord KRISHNA, My Parents, Teachers, Relatives, Friends And All Those Who Helped Me Reach This Position");
		
		TextView nagabhushanView = (TextView)findViewById(R.id.textView_nagabhushan);
		SpannableString nagabhushanString = new SpannableString("NAGABHUSHAN S N, currently doing B.E. In PESIT, Bengaluru, India.");
		nagabhushanString.setSpan(new RelativeSizeSpan(1.5f), 0, 15, 0);
		nagabhushanString.setSpan(new ForegroundColorSpan(Color.parseColor("#FF00FF")), 0, 15, 0);
		nagabhushanString.setSpan(new StyleSpan(2), 0, 15, 0);
		nagabhushanString.setSpan(new UnderlineSpan(), 0, 15, 0);
		nagabhushanView.setText(nagabhushanString);
		
		TextView contactView = (TextView)findViewById(R.id.contact);
		contactView.setText("For Any Queries, Drop Me An Email At");
		
		TextView emailView = (TextView)findViewById(R.id.email);
		emailView.setText("nagabhushansn.android@gmail.com");
		emailView.setPaintFlags(emailView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		emailView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent mailIntent = new Intent(android.content.Intent.ACTION_SEND);
				mailIntent.setType("plain/text");
				mailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"nagabhushansn.android@gmail.com"});
				mailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Query About ExpenditureList App");
				startActivity(Intent.createChooser(mailIntent, "Send Mail Using"));
			}
		});
	}
}
