// Shree KRISHNAya Namaha

package com.chaturvedi.customviews;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.chaturvedi.datastructures.Date;
import com.chaturvedi.datastructures.Month;
import com.chaturvedi.financemanager.R;

import java.util.Calendar;

/**
 * A Custom Widget for selecting interval (Date range) based on either month/year/all/custom date
 * range
 * By default, all will be selected
 */
public class IntervalSelector extends LinearLayout
{
	private TextView titleTextView;
	private Spinner intervalTypeSelector;
	private Spinner monthSelector;
	private Spinner yearSelector;
	private TextView fromDateTextView;
	private TextView toDateTextTextView;
	private TextView toDateTextView;
	
	private int yearMinValue;
	
	private OnIntervalChangeListener intervalChangeListener;
	
	public IntervalSelector(Context context)
	{
		super(context);
		yearMinValue = 2001;
		buildLayout();
	}
	
	/**
	 * Sets the minimum
	 *
	 * @param context
	 * @param attrs
	 */
	@SuppressWarnings("JavaDoc")
	public IntervalSelector(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable
				.IntervalSelector);
		String minYear = typedArray.getString(R.styleable.IntervalSelector_minYear);
		if (minYear != null)
		{
			yearMinValue = Integer.parseInt(minYear);
			Log.d("SNB", "YearMinValue set to: " + yearMinValue);
		}
		else
		{
			yearMinValue = 2001;
		}
		typedArray.recycle();
		buildLayout();
	}
	
	public void buildLayout()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.layout_interval_selector, this);
		
		titleTextView = (TextView) findViewById(R.id.title);
		intervalTypeSelector = (Spinner) findViewById(R.id.spinner_intervalType);
		monthSelector = (Spinner) findViewById(R.id.spinner_monthSelector);
		yearSelector = (Spinner) findViewById(R.id.spinner_yearSelector);
		fromDateTextView = (TextView) findViewById(R.id.textView_fromDate);
		toDateTextTextView = (TextView) findViewById(R.id.textView_toDateText);
		toDateTextView = (TextView) findViewById(R.id.textView_toDate);
		
		ArrayAdapter<IntervalType> intervalTypesAdapter = new ArrayAdapter<>(getContext(), android
				.R.layout.simple_spinner_item, IntervalType.values());
		intervalTypeSelector.setAdapter(intervalTypesAdapter);
		intervalTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				switch (position)
				{
					case 0:
						// Month Selector
						monthSelector.setVisibility(View.VISIBLE);
						yearSelector.setVisibility(View.VISIBLE);
						fromDateTextView.setVisibility(View.GONE);
						toDateTextTextView.setVisibility(View.GONE);
						toDateTextView.setVisibility(View.GONE);
						break;
					
					case 1:
						// Year Selector
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.VISIBLE);
						fromDateTextView.setVisibility(View.GONE);
						toDateTextTextView.setVisibility(View.GONE);
						toDateTextView.setVisibility(View.GONE);
						break;
					
					case 2:
						// All
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.GONE);
						fromDateTextView.setVisibility(View.GONE);
						toDateTextTextView.setVisibility(View.GONE);
						toDateTextView.setVisibility(View.GONE);
						break;
					
					case 3:
						// Custom Interval Selector
						monthSelector.setVisibility(View.GONE);
						yearSelector.setVisibility(View.GONE);
						fromDateTextView.setVisibility(View.VISIBLE);
						toDateTextTextView.setVisibility(View.VISIBLE);
						toDateTextView.setVisibility(View.VISIBLE);
						break;
				}
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
		});
		intervalTypeSelector.setSelection(2);
		
		Calendar today = Calendar.getInstance();
		ArrayAdapter<Month> monthAdapter = new ArrayAdapter<>(getContext(), android.R.layout
				.simple_spinner_item, Month.values());
		monthSelector.setAdapter(monthAdapter);
		monthSelector.setSelection(new Date(today).getMonth() - 1);    // Date.getMonth() starts
		// monthNo at 1
		
		int currentYear = today.get(Calendar.YEAR);
		String[] years = new String[currentYear - yearMinValue + 1];
		for (int i = yearMinValue; i <= currentYear; i++)
		{
			years[i - yearMinValue] = Integer.toString(i);
		}
		ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout
				.simple_spinner_item, years);
		yearSelector.setAdapter(yearAdapter);
		Log.d("SNB", "Length: " + years.length);
		yearSelector.setSelection(years.length - 1);
		
		fromDateTextView.setText(new Date(yearMinValue, 1, 1).getDisplayDate());
		toDateTextView.setText(new Date(today).getDisplayDate());
		fromDateTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Date fromDate = new Date(fromDateTextView.getText().toString());
				DatePickerDialog fromDatePicker = new DatePickerDialog(getContext(), new
						DatePickerDialog.OnDateSetListener()
				{
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int
							dayOfMonth)
					{
						fromDateTextView.setText(new Date(year, monthOfYear + 1, dayOfMonth)
								.getDisplayDate());
					}
				}, fromDate.getYear(), fromDate.getMonth() - 1, fromDate.getDate());
				fromDatePicker.show();
			}
		});
		toDateTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Date toDate = new Date(toDateTextView.getText().toString());
				DatePickerDialog toDatePicker = new DatePickerDialog(getContext(), new
						DatePickerDialog.OnDateSetListener()
				{
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int
							dayOfMonth)
					{
						// +1 is added to month because, DatePickerDialog represents Jan by 0, Feb
						// by 1 and so on
						toDateTextView.setText(new Date(year, monthOfYear + 1, dayOfMonth)
								.getDisplayDate());
					}
				}, toDate.getYear(), toDate.getMonth() - 1, toDate.getDate());
				toDatePicker.show();
			}
		});
		
		// Set Listener for Interval Change
		setListeners();
	}
	
	@SuppressWarnings("unused")
	public void setTitle(String title)
	{
		titleTextView.setText(title);
	}
	
	@SuppressWarnings("unused")
	public void setMinimumYear(int year)
	{
		yearMinValue = year;
		
		// Update in Year Selector
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		String[] years = new String[currentYear - yearMinValue + 1];
		for (int i = yearMinValue; i <= currentYear; i++)
		{
			years[i - yearMinValue] = Integer.toString(i);
		}
		ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout
				.simple_spinner_item, years);
		yearSelector.setAdapter(yearAdapter);
		
		// Update in fromDate and toDate TextViews
		if (getFromDate().getYear() < yearMinValue)
		{
			fromDateTextView.setText(new Date(yearMinValue, 1, 1).getDisplayDate());
		}
		if (getToDate().getYear() < yearMinValue)
		{
			toDateTextView.setText(new Date(yearMinValue, 1, 1).getDisplayDate());
		}
	}
	
	private void setListeners()
	{
		// For IntervalTypeSelector, this is already set in buildLayout
		
		monthSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
		});
		
		yearSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> adapterView)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
		});
		
		fromDateTextView.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
			
			}
			
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
			
			@Override
			public void afterTextChanged(Editable editable)
			{
			
			}
		});
		
		toDateTextView.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
			
			}
			
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
				if (intervalChangeListener != null)
				{
					intervalChangeListener.onIntervalChange();
				}
			}
			
			@Override
			public void afterTextChanged(Editable editable)
			{
			
			}
		});
	}
	
	@SuppressWarnings("unused")
	public IntervalType getSelectedIntervalType()
	{
		return (IntervalType) intervalTypeSelector.getSelectedItem();
	}
	
	@SuppressWarnings("unused")
	public Month getSelectedMonth()
	{
		return (Month) monthSelector.getSelectedItem();
	}
	
	@SuppressWarnings("unused")
	public int getSelectedYear()
	{
		return Integer.parseInt((String) yearSelector.getSelectedItem());
	}
	
	@SuppressWarnings("unused")
	public Date getFromDate()
	{
		return new Date(fromDateTextView.getText().toString());
	}
	
	@SuppressWarnings("unused")
	public Date getToDate()
	{
		return new Date(toDateTextView.getText().toString());
	}
	
	public void setOnIntervalChangeListener(OnIntervalChangeListener listener)
	{
		intervalChangeListener = listener;
	}
	
	public enum IntervalType
	{
		INTERVAL_MONTH("Month"),
		INTERVAL_YEAR("Year"),
		INTERVAL_ALL("All"),
		INTERVAL_CUSTOM("Custom");
		
		private String value;
		
		IntervalType(String value)
		{
			this.value = value;
		}
		
		@Override
		public String toString()
		{
			return value;
		}
	}
	
	public interface OnIntervalChangeListener
	{
		void onIntervalChange();
	}
}
