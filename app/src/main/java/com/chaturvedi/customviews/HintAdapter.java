// Shree KRISHNAya Namaha

package com.chaturvedi.customviews;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;

import java.util.Collection;
import java.util.List;

/**
 * An Adapter that automatically adds Hint to the list of options.
 * After setting the adapter to Spinner, set the hint as selection by
 * `spinner.setSelection(hintAdapter.getCount())`
 */
@SuppressWarnings("unused")
public class HintAdapter extends ArrayAdapter<String>
{
	public static final String HINT_TEXT = "Select";
	
	public HintAdapter(@NonNull Context context, int resource)
	{
		super(context, resource);
	}
	
	public HintAdapter(@NonNull Context context, int resource, int textViewResourceId)
	{
		super(context, resource, textViewResourceId);
	}
	
	public HintAdapter(@NonNull Context context, int resource, @NonNull String[] items)
	{
		super(context, resource, items);
		super.add(HINT_TEXT);
	}
	
	public HintAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull
			String[] items)
	{
		super(context, resource, textViewResourceId, items);
		super.add(HINT_TEXT);
	}
	
	public HintAdapter(@NonNull Context context, int resource, @NonNull List<String> items)
	{
		super(context, resource, items);
		super.add(HINT_TEXT);
	}
	
	public HintAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull
			List<String> items)
	{
		super(context, resource, textViewResourceId, items);
		super.add(HINT_TEXT);
	}
	
	@Override
	public void add(@Nullable String item)
	{
		super.remove(HINT_TEXT);
		super.add(item);
		super.add(HINT_TEXT);
	}
	
	@Override
	public void addAll(@NonNull Collection<? extends String> collection)
	{
		super.remove(HINT_TEXT);
		super.addAll(collection);
		super.add(HINT_TEXT);
	}
	
	@Override
	public void addAll(String... items)
	{
		super.remove(HINT_TEXT);
		super.addAll(items);
		super.add(HINT_TEXT);
	}
	
	@Override
	public int getCount()
	{
		// don't display last item. It is used as hint.
		int count = super.getCount();
		return count > 0 ? count - 1 : count;
	}
}
