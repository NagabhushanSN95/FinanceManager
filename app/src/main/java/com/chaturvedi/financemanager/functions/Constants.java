// Shree KRISHNAya Namaha
package com.chaturvedi.financemanager.functions;

@SuppressWarnings("WeakerAccess")
public class Constants
{
	public static final int APP_VERSION_88 = 88;
	public static final int APP_VERSION_89 = 89;
	public static final int APP_VERSION_96 = 96;
	public static final int APP_VERSION_107 = 107;
	public static final int APP_VERSION_110 = 110;
	public static final int APP_VERSION_111 = 111;
	// In version 124, includeInCounters field was added for transactions
	public static final int APP_VERSION_124 = 124;
	// In version 125, daily backup setting was added
	public static final int APP_VERSION_125 = 125;
	public static final int APP_VERSION_131 = 131;
	public static final int APP_VERSION_134 = 134;
	public static final int APP_VERSION_135 = 135;
	
	public static final int CURRENT_APP_VERSION_NO = 125;
	
	// Settings and Shared Preferences
	public static final String ALL_PREFERENCES = "AllPreferences";
	public static final String KEY_APP_VERSION = "AppVersionNo";
	public static final String KEY_DATABASE_INITIALIZED = "DatabaseInitialized";
	public static final String KEY_SPLASH_DURATION = "SplashDuration";
	public static final String KEY_QUOTE_NO = "QuoteNo";
	public static final String KEY_TRANSACTIONS_DISPLAY_INTERVAL = "TransactionsDisplayInterval";
	public static final String KEY_CURRENCY_SYMBOL = "CurrencySymbol";
	public static final String KEY_RESPOND_BANK_SMS = "RespondToBankSms";
	public static final String KEY_BANK_SMS_ARRIVED = "HasNewBankSmsArrived";
	public static final String KEY_AUTOMATIC_BACKUP_RESTORE = "AutomaticBackupAndRestore";
	public static final String KEY_DAILY_BACKUP = "DailyBackup";
    public static final String KEY_SORT_TRANSACTIONS = "SortTransactions";
	public static final String KEY_EXPORT_INTERVAL_TYPE = "ExportIntervalType";
	public static final String KEY_EXPORT_INCLUDE_TRANSACTION_TYPE =
			"ExportIncludeTransactionType";
	public static final String KEY_EXPORT_INCLUDE_RATE_QUANTITY = "ExportIncludeRateQuantity";
	public static final String KEY_EXPORT_INCLUDE_CURRENT_BALANCES =
			"ExportIncludeCurrentWalletBankBalances";
	public static final String KEY_EXPORT_FILE_FORMAT = "ExportFileFormat";
	
	public static final int REQUEST_CODE_ADD_TRANSACTION = 101;
	public static final int REQUEST_CODE_EDIT_TRANSACTION = 102;
	public static final int REQUEST_CODE_TRANSACTIONS_ACTIVITY = 103;
	public static final int REQUEST_CODE_EDIT_ACTIVITY = 104;
	public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 105;
	public static final int REQUEST_CODE_EXTRAS_ACTIVITY = 106;
	public static final int REQUEST_CODE_FILTERS = 107;
	
	public static final int ACTION_BACKUP_SUCCESSFUL = 201;
	public static final int ACTION_BACKUP_FAILURE = 202;
	
	public static final String ACTION = "Action";
	public static final String ACTION_ADD = "ActionAdd";
	public static final String ACTION_EDIT = "ActionEdit";
	public static final String ACTION_BANK_SMS = "BankSms";
	
	public static final String TRANSACTION = "Transaction";
	public static final String TRANSACTION_TYPE = "TransactionType";
	public static final String TRANSACTION_INCOME = "Income";
	public static final String TRANSACTION_EXPENSE = "Expense";
	public static final String TRANSACTION_TRANSFER = "Transfer";
	public static final String TRANSFER_TYPE = "TransferType";
	public static final String TRANSFER_PAY_IN = "Pay_In";
	public static final String TRANSFER_WITHDRAW = "Withdraw";
	
	public static final String KEY_BANK_ID = "BankID";
	public static final String KEY_AMOUNT = "Amount";
	
	public static final int MIN_TRANSACTIONS_TO_DISPLAY = 50;
	
	// Used in Filters
	public static final String KEY_CREDIT_TRANSACTIONS = "Credit Transactions";
	public static final String KEY_DEBIT_TRANSACTIONS = "Debit Transactions";
	public static final String KEY_TRANSFER_TRANSACTIONS = "Transfer Transactions";
	public static final String KEY_WALLETS = "Wallets";
	public static final String KEY_BANKS = "Banks";
	public static final String KEY_INTERVAL_TYPE = "IntervalType";
	public static final String VALUE_ALL = "All";
	public static final String KEY_INTERVAL_TYPE_MONTH = "Month";
	public static final String VALUE_MONTH = "Month";
	public static final String KEY_INTERVAL_TYPE_YEAR = "Year";
	public static final String VALUE_YEAR = "Year";
	public static final String VALUE_CUSTOM = "Custom";
	public static final String KEY_START_DATE = "StartDate";
	public static final String KEY_END_DATE = "EndDate";
	public static final String KEY_ALLOWED_TRANSACTION_TYPES = "AllowedTransactionTypes";
	public static final String KEY_SEARCH_KEYWORD = "SearchKeyword";
	
	public static final String VALUE_CREDIT = "Credit";
	public static final String VALUE_DEBIT = "Debit";
	public static final String VALUE_TRANSFER = "Transfer";
	public static final String VALUE_WALLET = "Wallet";
	public static final String VALUE_BANK = "Bank";
	public static final String VALUE_DAILY_BACKUP_ENABLED = "Back-up";
	public static final String VALUE_DAILY_BACKUP_DISABLED = "Do not back-up";
	public static final boolean FLOW_LOGS_ENABLED = true;
	public static final String VALUE_FLOW_LOGS = "Flow Logs";
	
	public static final int YEAR_MINIMUM_VALUE = 2015;
    public static final String VALUE_SORT_TRANSACTIONS_DATE = "Date";
    public static final String VALUE_SORT_TRANSACTIONS_CREATED = "Created Time";
    public static final String VALUE_SORT_TRANSACTIONS_MODIFIED = "Modified Time";
}
