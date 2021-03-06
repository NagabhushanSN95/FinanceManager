package com.chaturvedi.financemanager.setup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.chaturvedi.customviews.IndefiniteWaitDialogBuilder;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseAdapter;
import com.chaturvedi.financemanager.datastructures.ExpenditureType;
import com.chaturvedi.financemanager.datastructures.Wallet;
import com.chaturvedi.financemanager.extras.RestoreManager;
import com.chaturvedi.financemanager.main.SummaryActivity;

import java.util.ArrayList;

public class StartupActivity extends FragmentActivity
{
	private static final int CODE_SETUP_ACTIVITY = 101;
	private static final int CODE_FILE_CHOOSER = 102;
	private static final int RESTORE_REQUEST_PERMISSION = 203;

	private static final String ALL_PREFERENCES = "AllPreferences";
	private static final String KEY_APP_VERSION = "AppVersionNo";
	private static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	private static final String KEY_SPLASH_DURATION = "SplashDuration";
	private static final String KEY_QUOTE_NO = "QuoteNo";
	private static final String KEY_NUM_EXP_TYPES = "NumExpTypes";
	private static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	private static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	private static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	private static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
	private static final String KEY_AUTOMATIC_BACKUP_RESTORE = "AutomaticBackupAndRestore";
	private static final String setupInfoString = "You can setup your Finance Manager Account by " +
			"adding the\n" +
			"1) Amount in your wallet\n" +
			"2) Setup Bank Accounts if any\n" +
			"3) Configure Major types of Expenditures you make\n" +
													"4) Setup your Preferences\n";
	private static final String restoreInfoString = "If you have Finance Manager before and backed up your data,"+
			"you can restore the data here.\n" +
			"All your Transactions, Bank Details will be restored";
	private static final String skipInfoString = "You can skip the setup and straight away start using the App\n" +
			"1) Your Wallet Balance will be set to zero\n" +
			"2) No Bank Accounts will be set up\n" +
			"3) The Expenditure Types will be set to default\n" +
			"4) All Preferences will be set to default\n";
	private final int NUM_EXP_TYPES = 5;
	private SharedPreferences preferences;
	private int CURRENT_APP_VERSION_NO;
	private final int splashDuration = 5000;
	private DisplayMetrics displayMetrics;
	private int screenWidth;
	private int screenHeight;
	private int WIDTH_BUTTON;
	private int HEIGHT_BUTTON;
	private Intent setupIntent;
	private Intent summaryIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_startup);

		CURRENT_APP_VERSION_NO = Integer.parseInt(getResources().getString(R.string.currentAppVersion));
		preferences = this.getSharedPreferences(ALL_PREFERENCES, Context.MODE_PRIVATE);
		calculateDimensions();
		buildLayout();
		buildInfoButtons();
		setAnimation();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults)
	{
		boolean permissionGranted =
				(grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
		//noinspection SwitchStatementWithTooFewBranches
		switch (requestCode)
		{
			case RESTORE_REQUEST_PERMISSION:
				if (permissionGranted)
				{
					chooseRestoreFile();
				}
				else
				{
					Toast.makeText(StartupActivity.this, "Please provide Read permission to " +
							"restore data from SD card", Toast.LENGTH_LONG).show();
				}
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		super.onActivityResult(requestCode, resultCode, intent);

		switch (requestCode)
		{
			case CODE_SETUP_ACTIVITY:
				if(preferences.contains(KEY_DATABASE_INITIALIZED))
				{
					if(preferences.getBoolean(KEY_DATABASE_INITIALIZED, false))
					{
						// Finish This Activity
						finish();
					}
				}
				break;

			case CODE_FILE_CHOOSER:
				if(resultCode == RESULT_OK)
				{
					// Get the Uri of the selected file
					Uri uri = intent.getData();
					restoreData(uri);
				}
				break;
		}
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

		WIDTH_BUTTON = screenWidth*60/100;
		HEIGHT_BUTTON = screenHeight*10/100;

		setupIntent = new Intent(StartupActivity.this, SetupActivity.class);
		summaryIntent = new Intent(StartupActivity.this, SummaryActivity.class);

	}

	private void buildLayout()
	{
		// If Release Version, Make Krishna TextView Invisible
		if((this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
		{
			TextView krishna = (TextView) findViewById(R.id.krishna);
			krishna.setVisibility(View.INVISIBLE);
		}

		TextView financeManager = (TextView) findViewById(R.id.textView_FinanceManager);
		RelativeLayout.LayoutParams financeManagerTextParams = (LayoutParams) financeManager.getLayoutParams();
		financeManagerTextParams.topMargin = screenHeight * 10/100;

		TextView welcomeText = (TextView) findViewById(R.id.textView_welcome);
		RelativeLayout.LayoutParams welcomeTextParams = (LayoutParams) welcomeText.getLayoutParams();
		welcomeTextParams.topMargin = screenHeight * 3/100;

		Button setupButton = (Button) findViewById(R.id.button_setup);
		RelativeLayout.LayoutParams setupButtonParams = (LayoutParams) setupButton.getLayoutParams();
		setupButtonParams.width = WIDTH_BUTTON;
		setupButtonParams.height = HEIGHT_BUTTON;
		setupButtonParams.topMargin = screenHeight*10/100;
		//setupButton.setLayoutParams(setupButtonParams);
		setupButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivityForResult(setupIntent, CODE_SETUP_ACTIVITY);
			}
		});

		Button restoreButton = (Button) findViewById(R.id.button_restore);
		RelativeLayout.LayoutParams restoreButtonParams = (LayoutParams) restoreButton.getLayoutParams();
		restoreButtonParams.width = WIDTH_BUTTON;
		restoreButtonParams.height = HEIGHT_BUTTON;
		restoreButtonParams.topMargin = screenHeight*3/100;
		restoreButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				checkRestorePermissions();
			}
		});

		Button skipButton = (Button) findViewById(R.id.button_skip);
		RelativeLayout.LayoutParams skipButtonParams = (LayoutParams) skipButton.getLayoutParams();
		skipButtonParams.width = WIDTH_BUTTON;
		skipButtonParams.height = HEIGHT_BUTTON;
		skipButtonParams.topMargin = screenHeight*3/100;
		skipButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				skipSetup();
			}
		});
	}

	private void buildInfoButtons()
	{
		TranslateAnimation dialogEnterAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -2.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		dialogEnterAnimation.setDuration(1000);
		dialogEnterAnimation.setFillAfter(true);
		dialogEnterAnimation.setFillEnabled(true);

		TranslateAnimation dialogExitAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -2.0f);
		dialogExitAnimation.setDuration(1000);
		dialogExitAnimation.setFillAfter(true);
		dialogExitAnimation.setFillEnabled(true);

		// Info (Information) Buttons
		ImageButton setupInfoButton = (ImageButton) findViewById(R.id.infoButton_setup);
		setupInfoButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder setupInfoBuilder = new AlertDialog.Builder(StartupActivity.this);
				setupInfoBuilder.setTitle("Setup Finance Manager Account");
				setupInfoBuilder.setMessage(setupInfoString);
				AlertDialog setupInfoDialog = setupInfoBuilder.create();
				setupInfoDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
				setupInfoDialog.show();
			}
		});

		ImageButton restoreInfoButton = (ImageButton) findViewById(R.id.infoButton_restore);
		restoreInfoButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder restoreInfoBuilder = new AlertDialog.Builder(StartupActivity.this);
				restoreInfoBuilder.setTitle("Restore Previous Data");
				restoreInfoBuilder.setMessage(restoreInfoString);
				AlertDialog restoreInfoDialog = restoreInfoBuilder.create();
				restoreInfoDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
				restoreInfoDialog.show();
			}
		});

		ImageButton skipInfoButton = (ImageButton) findViewById(R.id.infoButton_skip);
		skipInfoButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder skipInfoBuilder = new AlertDialog.Builder(StartupActivity.this);
				skipInfoBuilder.setTitle("Skip Setup");
				skipInfoBuilder.setMessage(skipInfoString);
				AlertDialog skipInfoDialog = skipInfoBuilder.create();
				skipInfoDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
				skipInfoDialog.show();
			}
		});
	}

	private void setAnimation()
	{
		final TextView appNameView = (TextView) findViewById(R.id.textView_FinanceManager);

		final TranslateAnimation anim1 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_PARENT, -1f, Animation.RELATIVE_TO_SELF, 0.5f);
		//anim1.setRepeatCount(0);
		anim1.setDuration(800);

		final TranslateAnimation anim2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, -0.3f);
		//anim2.setRepeatCount(1);
		//anim2.setRepeatMode(Animation.REVERSE);
		anim2.setDuration(300);

		final TranslateAnimation anim3 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -0.3f, Animation.RELATIVE_TO_SELF, 0);
		//anim3.setRepeatCount(1);
		//anim3.setRepeatMode(Animation.REVERSE);
		anim3.setDuration(200);

		appNameView.setAnimation(anim1);
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				appNameView.setAnimation(anim2);
			}
		}, 800);
		new Handler().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				appNameView.setAnimation(anim3);
			}
		}, 1100);
		/*anim1.setAnimationListener(new Animation.AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				appNameView.setAnimation(anim2);
			}
		});
		
		anim2.setAnimationListener(new Animation.AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				appNameView.setAnimation(anim3);
			}
		});
		
		/*String appName = getResources().getString(R.string.app_name);
		for(int i=1; i<appName.length(); i++)
		{
			TextView letter = new TextView(StartupActivity.this);
			letter.setText(appName.charAt(i) + "");
			LayoutParams letterParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			letterParams.alignWithParent = true;
			letterParams.topMargin = 50;
			letterParams.leftMargin = 20 + i*5;
			letter.setLayoutParams(letterParams);
		}*/
	}

	private void checkRestorePermissions()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			// Permission is not granted
			ActivityCompat.requestPermissions(StartupActivity.this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					RESTORE_REQUEST_PERMISSION);
		}
		else
		{
			// Permission Granted
			chooseRestoreFile();
		}
	}

	private void chooseRestoreFile()
	{
		Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		fileIntent.setType("*/*");
		startActivityForResult(fileIntent, CODE_FILE_CHOOSER);
	}

	private void restoreData(final Uri fileUri)
	{
		// TODO: CREATE a new Activity for Restoring. Give 2 options there. To select Data Backup file
		// TODO: and settings backup file. Restore both

		IndefiniteWaitDialogBuilder restoreDialogBuilder = new IndefiniteWaitDialogBuilder(this);
		restoreDialogBuilder.setWaitText("Restoring Data. This may take few minutes depending on the Size of your Data");
		restoreDialogBuilder.setCancelable(false);
		final AlertDialog restoreDialog = restoreDialogBuilder.show();

		/* Restore in a separate (non-ui) thread */
		Thread restoreThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// Restore Data
				RestoreManager restoreManager = new RestoreManager(StartupActivity.this, fileUri, true);
				int result = restoreManager.getResult();
				if(result == 0)
				{
					DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(StartupActivity.this);
					databaseAdapter.addAllWallets(restoreManager.getAllWallets());
					databaseAdapter.addAllBanks(restoreManager.getAllBanks());
					databaseAdapter.addAllTransactions(restoreManager.getAllTransactions());
					databaseAdapter.addAllExpenditureTypes(restoreManager.getAllExpTypes());
					// Initially, in DatabaseAdapter, Counters Table is configured to have 5 Exp Types By Deafult
					// If Number of Exp Types is not 5, then readjust it to have more columns
					if(restoreManager.getNumExpTypes() != 5)
					{
						Log.d("restoreData()","Readjusting Counters Table");
						databaseAdapter.readjustCountersTable();
					}

					databaseAdapter.addAllCountersRows(restoreManager.getAllCounters());
					databaseAdapter.addAllTemplates(restoreManager.getAllTemplates());

					// Store Default Preferences
					SharedPreferences.Editor editor = preferences.edit();
					int appVersionNo;
					try
					{
						appVersionNo = StartupActivity.this.getPackageManager().
								getPackageInfo(StartupActivity.this.getPackageName(), 0).versionCode;
					}
					catch (NameNotFoundException e)
					{
						appVersionNo = CURRENT_APP_VERSION_NO;
						Toast.makeText(getApplicationContext(), "Error In Retrieving Version No In " +
								"StartupActivity/restoreData\n" + e.getMessage(), Toast.LENGTH_LONG).show();
					}
					editor.putInt(KEY_APP_VERSION, appVersionNo);
					editor.putBoolean(KEY_DATABASE_INITIALIZED, true);
					editor.putInt(KEY_SPLASH_DURATION, splashDuration);
					editor.putInt(KEY_QUOTE_NO, 0);
					editor.putInt(KEY_NUM_EXP_TYPES, NUM_EXP_TYPES);
					editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
					editor.putString(KEY_CURRENCY_SYMBOL, " ");
					editor.putString(KEY_RESPOND_BANK_SMS, "Popup");
					editor.putBoolean(KEY_BANK_SMS_ARRIVED, false);
					editor.putInt(KEY_AUTOMATIC_BACKUP_RESTORE, 1);
					editor.apply();

					//Toast.makeText(getApplicationContext(), "Data Restored Successfully", Toast.LENGTH_LONG).show();
					startActivity(summaryIntent);
					restoreDialog.dismiss();
					StartupActivity.this.finish();
				}
				else if(result == 1)
				{
					// TODO: Send these messages using a handler. Display Toast in that handler
					/*Toast.makeText(getApplicationContext(), "No Backups Were Found.\nMake sure the Backup Files " +
							"are located in\nChaturvedi/Finance Manager Folder", Toast.LENGTH_LONG).show();*/
					Log.d("restoreData()", "No Backups Were Found.\nPlease select a backup file created by Finance Manager App only");
					restoreDialog.dismiss();
				}
				else if(result == 2)
				{
					/*Toast.makeText(getApplicationContext(), "Old Data. Cannot be Restored. Sorry!",
							Toast.LENGTH_LONG).show();*/
					Log.d("restoreData()", "Old Data. Cannot be Restored. Sorry!");
					restoreDialog.dismiss();
				}
				else if(result == 3)
				{
					/*Toast.makeText(getApplicationContext(), "Error in Restoring Data\nControl Entered Catch Block",
							Toast.LENGTH_LONG).show();*/
					Log.d("restoreData()", "Error in Restoring Data\nControl Entered Catch Block");
					restoreDialog.dismiss();
				}
			}
		});
		restoreThread.start();


	}

	private void skipSetup()
	{
		DatabaseAdapter databaseAdapter = DatabaseAdapter.getInstance(StartupActivity.this);

		Wallet wallet = new Wallet(1, "Wallet01", 0, false);
		databaseAdapter.addWallet(wallet);

		ArrayList<ExpenditureType> expTypes = new ArrayList<ExpenditureType>(NUM_EXP_TYPES);
		expTypes.add(new ExpenditureType(1, getResources().getString(R.string.hint_exp01), false));
		expTypes.add(new ExpenditureType(2, getResources().getString(R.string.hint_exp02), false));
		expTypes.add(new ExpenditureType(3, getResources().getString(R.string.hint_exp03), false));
		expTypes.add(new ExpenditureType(4, getResources().getString(R.string.hint_exp04), false));
		expTypes.add(new ExpenditureType(5, getResources().getString(R.string.hint_exp05), false));
		databaseAdapter.addAllExpenditureTypes(expTypes);

		// Store Default Preferences
		SharedPreferences.Editor editor = preferences.edit();
		int appVersionNo;
		try
		{
			appVersionNo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			appVersionNo = CURRENT_APP_VERSION_NO;
			Toast.makeText(getApplicationContext(), "Error In Retrieving Version No In " +
					"StartupActivity/skipSetup\n" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		editor.putInt(KEY_APP_VERSION, appVersionNo);
		editor.putBoolean(KEY_DATABASE_INITIALIZED, true);
		editor.putInt(KEY_SPLASH_DURATION, splashDuration);
		editor.putInt(KEY_QUOTE_NO, 0);
		editor.putInt(KEY_NUM_EXP_TYPES, NUM_EXP_TYPES);
		editor.putString(KEY_TRANSACTIONS_DISPLAY_INTERVAL, "Month");
		editor.putString(KEY_CURRENCY_SYMBOL, " ");
		editor.putString(KEY_RESPOND_BANK_SMS, "Popup");
		editor.putBoolean(KEY_BANK_SMS_ARRIVED, false);
		editor.putInt(KEY_AUTOMATIC_BACKUP_RESTORE, 3);
		editor.apply();

		startActivityForResult(summaryIntent, 0);
		super.onBackPressed();
	}
}
