// Shree KRISHNAya Namaha

package com.chaturvedi.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

public class MyAutoCompleteTextView extends AutoCompleteTextView
{
	public MyAutoCompleteTextView(Context context) {
		super(context);
	}

	public MyAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction()==1)) {
			InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

			if(inputManager.hideSoftInputFromWindow(this.getApplicationWindowToken(), 0)){
				return true;
			}
		}

		return super.onKeyPreIme(keyCode, event);
	}
}
