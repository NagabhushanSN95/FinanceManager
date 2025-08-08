package com.chaturvedi.financemanager.extras.zerodha;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import com.chaturvedi.customviews.IndefiniteWaitDialogBuilder;
import com.chaturvedi.financemanager.R;
import com.chaturvedi.financemanager.database.DatabaseManager;
import com.chaturvedi.financemanager.datastructures.Transaction;

public class ZerodhaImportActivity extends Activity {
    private static final int CODE_FILE_CHOOSER_COIN = 101;
    private static final int CODE_FILE_CHOOSER_KITE_TRADEBOOK = 102;
    private static final int CODE_FILE_CHOOSER_KITE_DIVIDENDS = 103;
    private static final int PERMISSION_COIN = 201;
    private static final int PERMISSION_KITE_TRADEBOOK = 202;
    private static final int PERMISSION_KITE_DIVIDENDS = 203;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zerodha_import);
        if (getActionBar() != null) {
            // Provide Up Button in Action Bar
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        buildLayout();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(ZerodhaImportActivity.this);
                return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean permissionGranted = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case PERMISSION_COIN:
                if (permissionGranted) {
                    chooseZerodhaCoinFile();
                } else {
                    Toast.makeText(ZerodhaImportActivity.this, "Please provide Read permission to " +
                            "read Zerodha Coin Statement", Toast.LENGTH_LONG).show();
                }
                break;

            case PERMISSION_KITE_TRADEBOOK:
                if (permissionGranted) {
                    chooseKiteTradebookFile();
                } else {
                    Toast.makeText(ZerodhaImportActivity.this, "Please provide Read permission to " +
                            "read Zerodha Kite Tradebook", Toast.LENGTH_LONG).show();
                }
                break;

            case PERMISSION_KITE_DIVIDENDS:
                if (permissionGranted) {
                    chooseKiteDividendsFile();
                } else {
                    Toast.makeText(ZerodhaImportActivity.this, "Please provide Read permission to " +
                            "read Zerodha Kite Dividends", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK && intent != null && intent.getData() != null) {
            Uri fileUri = intent.getData();

            switch (requestCode) {
                case CODE_FILE_CHOOSER_COIN:
                    importZerodhaCoin(fileUri);
                    break;

                case CODE_FILE_CHOOSER_KITE_TRADEBOOK:
                    importKiteTradebook(fileUri);
                    break;

                case CODE_FILE_CHOOSER_KITE_DIVIDENDS:
                    importKiteDividends(fileUri);
                    break;

                default:
                    Log.e("onActivityResult", "Unknown request code: " + requestCode);
            }
        } else {
            Log.e("onActivityResult", "No file selected or invalid result");
        }
    }

    protected void buildLayout() {
        // If Release Version, Make Krishna TextView Invisible
        if (0 == (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            TextView krishna = (TextView) findViewById(R.id.krishna);
            krishna.setVisibility(View.INVISIBLE);
        }

        LinearLayout importCoinLayout = (LinearLayout) findViewById(R.id.layout_import_coin);
        importCoinLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCoinImportPermissions();
            }
        });

        LinearLayout importKiteTradebookLayout = (LinearLayout) findViewById(R.id.layout_import_kite_tradebook);
        importKiteTradebookLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkKiteTradebookImportPermissions();
            }
        });

        LinearLayout importKiteDividendsLayout = (LinearLayout) findViewById(R.id.layout_import_kite_dividends);
        importKiteDividendsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkKiteDividendsImportPermissions();
            }
        });
    }

    private void checkCoinImportPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(ZerodhaImportActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_COIN);
        } else {
            // Permission Granted
            chooseZerodhaCoinFile();
        }
    }

    private void checkKiteTradebookImportPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(ZerodhaImportActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_KITE_TRADEBOOK);
        } else {
            // Permission Granted
            chooseKiteTradebookFile();
        }
    }

    private void checkKiteDividendsImportPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(ZerodhaImportActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_KITE_DIVIDENDS);
        } else {
            // Permission Granted
            chooseKiteDividendsFile();
        }
    }

    private void chooseZerodhaCoinFile() {
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        startActivityForResult(fileIntent, CODE_FILE_CHOOSER_COIN);
    }

    private void chooseKiteTradebookFile() {
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        startActivityForResult(fileIntent, CODE_FILE_CHOOSER_KITE_TRADEBOOK);
    }

    private void chooseKiteDividendsFile() {
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        startActivityForResult(fileIntent, CODE_FILE_CHOOSER_KITE_DIVIDENDS);
    }

    private void importZerodhaCoin(final Uri fileUri) {
        IndefiniteWaitDialogBuilder restoreDialogBuilder = new IndefiniteWaitDialogBuilder(this);
        restoreDialogBuilder.setTitle("Importing Data from Zerodha Coin");
        restoreDialogBuilder.setWaitText("This may take few seconds depending on the Size of your Data");
        restoreDialogBuilder.setCancelable(false);
        final AlertDialog restoreDialog = restoreDialogBuilder.show();

        Thread importThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Import Data
                ImportZerodhaCoinManager importManager = new ImportZerodhaCoinManager(ZerodhaImportActivity.this, fileUri);
                int result = importManager.getResult();
                if (result == 0) {
                    for (Transaction transaction : importManager.getAllTransactions()) {
                        DatabaseManager.addTransaction(ZerodhaImportActivity.this, transaction, true);
                    }
                }
                restoreDialog.dismiss();

                // Show result in a dialog on the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == 0) {
                            showImportCompleteDialog(result, importManager.getAllTransactions().size(), "Coin");
                        } else {
                            showImportCompleteDialog(result, 0, "Coin");
                        }
                    }
                });
            }
        });
        importThread.start();
    }

    private void importKiteTradebook(final Uri fileUri) {
        IndefiniteWaitDialogBuilder restoreDialogBuilder = new IndefiniteWaitDialogBuilder(this);
        restoreDialogBuilder.setTitle("Importing Data from Zerodha Kite Tradebook");
        restoreDialogBuilder.setWaitText("This may take few seconds depending on the Size of your Data");
        restoreDialogBuilder.setCancelable(false);
        final AlertDialog restoreDialog = restoreDialogBuilder.show();

        Thread importThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Import Data
                ImportZerodhaKiteTradebookManager importManager = new ImportZerodhaKiteTradebookManager(ZerodhaImportActivity.this, fileUri);
                int result = importManager.getResult();
                if (result == 0) {
                    for (Transaction transaction : importManager.getAllTransactions()) {
                        DatabaseManager.addTransaction(ZerodhaImportActivity.this, transaction, true);
                    }
                }
                restoreDialog.dismiss();

                // Show result in a dialog on the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == 0) {
                            showImportCompleteDialog(result, importManager.getAllTransactions().size(), "Kite Tradebook");
                        } else {
                            showImportCompleteDialog(result, 0, "Kite Tradebook");
                        }
                    }
                });
            }
        });
        importThread.start();
    }

    private void importKiteDividends(final Uri fileUri) {
        IndefiniteWaitDialogBuilder restoreDialogBuilder = new IndefiniteWaitDialogBuilder(this);
        restoreDialogBuilder.setTitle("Importing Data from Zerodha Kite Dividends");
        restoreDialogBuilder.setWaitText("This may take few seconds depending on the Size of your Data");
        restoreDialogBuilder.setCancelable(false);
        final AlertDialog restoreDialog = restoreDialogBuilder.show();

        Thread importThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Import Data
                ImportZerodhaKiteDividendsManager importManager = new ImportZerodhaKiteDividendsManager(ZerodhaImportActivity.this, fileUri);
                int result = importManager.getResult();
                if (result == 0) {
                    for (Transaction transaction : importManager.getAllTransactions()) {
                        DatabaseManager.addTransaction(ZerodhaImportActivity.this, transaction, true);
                    }
                }
                restoreDialog.dismiss();

                // Show result in a dialog on the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == 0) {
                            showImportCompleteDialog(result, importManager.getAllTransactions().size(), "Kite Dividends");
                        } else {
                            showImportCompleteDialog(result, 0, "Kite Dividends");
                        }
                    }
                });
            }
        });
        importThread.start();
    }

    // Method to show the import complete dialog
    private void showImportCompleteDialog(int result, int transactionCount, String importType) {
        String message = "";
        if (result == 0) {
            message = "Successfully imported " + transactionCount + " transactions from Zerodha " + importType + ".";
            if (importType.equals("Kite Dividends")) {
                message += "\nPlease update the transaction date based on your netbanking. Currently the date is set to Ex-Dividend date.";
            }
        } else if (result == 1) {
            Log.d("import Zerodha " + importType, "No Zerodha " + importType + " Statements were found.\nPlease select a file exported using Zerodha " + importType);
            message = "No Zerodha " + importType + " Statements were found.\nPlease select a file exported using Zerodha " + importType;
        } else if (result == 2) {
            Log.d("import Zerodha " + importType, "Error in Importing Zerodha " + importType + " Statement\nControl Entered Catch Block");
            message = "Error in Importing Zerodha " + importType + " Statement";
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Import Complete");
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
    }
}
