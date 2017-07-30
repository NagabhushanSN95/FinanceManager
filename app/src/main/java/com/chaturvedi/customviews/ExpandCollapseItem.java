// Shree KRISHNAya Namaha

package com.chaturvedi.customviews;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.chaturvedi.financemanager.R;

import java.util.ArrayList;

public class ExpandCollapseItem extends RelativeLayout
{
	private int ANIMATION_DURATION = 300;
	private int CHILD_HEIGHT = 0;

	private boolean isCollapsed;
	private ImageButton expandCollapseButton;
	private LinearLayout titleLayout;
	private LinearLayout childLayout;

	public ExpandCollapseItem(Context context)
	{
		super(context);
		buildLayout();
	}

	public ExpandCollapseItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		buildLayout();
	}

	public ExpandCollapseItem(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		buildLayout();
	}

	private void buildLayout()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.layout_expand_collapse_item, this);

		expandCollapseButton = (ImageButton) findViewById(R.id.imageButton_expandCollapse);
		isCollapsed = true;
		titleLayout = (LinearLayout) findViewById(R.id.layout_title);
		childLayout = (LinearLayout) findViewById(R.id.layout_child);

		childLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				CHILD_HEIGHT = childLayout.getHeight();
				if(Build.VERSION.SDK_INT < 16)
				{
					childLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				else
				{
					childLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}

				childLayout.setVisibility(View.GONE);
			}
		});

		ExpandCollapseListener listener = new ExpandCollapseListener();
		expandCollapseButton.setOnClickListener(listener);
		titleLayout.setOnClickListener(listener);
	}

	public void setViews(View titleView, ArrayList<View> childViews)
	{
		titleLayout.addView(titleView);
		if((childViews == null) || (childViews.size() == 0))
		{
			expandCollapseButton.setVisibility(View.INVISIBLE);
			return;
		}
		for (View childView : childViews)
		{
			childLayout.addView(childView);
		}
	}

	private class ExpandCollapseListener implements View.OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			if (isCollapsed)
			{
				// Rotate the Expand/Collapse ImageButton clockwise 90deg with animation
				RotateAnimation rotateAnimation = new RotateAnimation(0, 90,
						Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				rotateAnimation.setDuration(ANIMATION_DURATION);
				rotateAnimation.setFillAfter(true);
				expandCollapseButton.startAnimation(rotateAnimation);

				// Increase the height from 0 to max
//				childLayout.setVisibility(View.INVISIBLE);;
				ResizeAnimation resizeAnimation = new ResizeAnimation(childLayout, 0, CHILD_HEIGHT);
				resizeAnimation.setDuration(ANIMATION_DURATION);
				childLayout.startAnimation(resizeAnimation);
				new Handler().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						childLayout.setVisibility(View.VISIBLE);
					}
				}, ANIMATION_DURATION/10);
//				childLayout.invalidate();
//				childLayout.setVisibility(View.VISIBLE);
			}
			else
			{
				// Rotate the Expand/Collapse ImageButton counter-clockwise 90deg with animation
				RotateAnimation rotateAnimation = new RotateAnimation(90, 0,
						Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				rotateAnimation.setDuration(ANIMATION_DURATION);
				rotateAnimation.setFillAfter(true);
				expandCollapseButton.startAnimation(rotateAnimation);

				// Decrease the height from max to 0
				ResizeAnimation resizeAnimation = new ResizeAnimation(childLayout, CHILD_HEIGHT, 0);
				resizeAnimation.setDuration(ANIMATION_DURATION);
				resizeAnimation.setAnimationListener(new Animation.AnimationListener()
				{
					@Override
					public void onAnimationStart(Animation animation)
					{

					}

					@Override
					public void onAnimationEnd(Animation animation)
					{
						childLayout.setVisibility(View.GONE);
					}

					@Override
					public void onAnimationRepeat(Animation animation)
					{

					}
				});
				childLayout.startAnimation(resizeAnimation);

			}
			isCollapsed = !isCollapsed;
		}
	}

	public void setAnimationDuration(int animationDuration)
	{
		ANIMATION_DURATION = animationDuration;
	}

	public class ResizeAnimation extends Animation
	{
		View view;
		int startHeight;
		int endHeight;

		public ResizeAnimation(View view, int startHeight, int endHeight)
		{
			this.view = view;
			this.startHeight = startHeight;
			this.endHeight = endHeight;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			int newHeight = (int) (startHeight + (endHeight-startHeight) * interpolatedTime);
			//to support decent animation, change new heigt as Nico S. recommended in comments
			//int newHeight = (int) (startHeight+(endHeight - startHeight) * interpolatedTime);
			view.getLayoutParams().height = newHeight;
			view.requestLayout();
		}

		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight)
		{
			super.initialize(width, height, parentWidth, parentHeight);
		}

		@Override
		public boolean willChangeBounds()
		{
			return true;
		}
	}
}
