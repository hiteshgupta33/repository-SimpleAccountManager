package accountmanager.com.hitesh.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import Utils.Constants;
import accountmanager.com.hitesh.R;
import java.io.IOException;

public class AuthenticatorActivity extends android.accounts.AccountAuthenticatorActivity {

    /** The Intent flag to confirm credentials. */
    public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

    /** The Intent extra to store password. */
    public static final String PARAM_PASSWORD = "password";

    /** The Intent extra to store username. */
    public static final String PARAM_USERNAME = "username";

    /** The Intent extra to store username. */
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    /** The tag used to log to adb console. */
    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    private AccountManager mAccountManager;

    /** Keep track of the progress dialog so we can dismiss it */
    private ProgressDialog mProgressDialog = null;

    /** Keep track of the login task so can cancel it if requested */
    private keyAuthTask mAuthTask = null;

    /**
     * If set we are just checking that the user knows their credentials; this
     * doesn't cause the user's password or authToken to be changed on the
     * device.
     */
    private Boolean mConfirmCredentials = false;

    /** for posting authentication attempts back to UI thread */
    private final Handler Handler = new Handler();

    private TextView mMessage;

    private String mPassword;

    private EditText mPasswordEdit;

    /** Was the original caller asking for an entirely new account? */
    protected boolean mRequestNewAccount = false;

    private String mUsername;

    private EditText mUsernameEdit;

    private Button getAuthToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);

        mAccountManager = AccountManager.get(this);

        Log.i(TAG, "loading data from Intent");

        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mRequestNewAccount = mUsername == null;
        mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false);

        Log.i(TAG, "    request new: " + mRequestNewAccount);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.activity_account_authenticator);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_alert);

        mMessage = (TextView) findViewById(R.id.message);
        mUsernameEdit = (EditText) findViewById(R.id.username_edit);
        mPasswordEdit = (EditText) findViewById(R.id.password_edit);

        if (!TextUtils.isEmpty(mUsername)) mUsernameEdit.setText(mUsername);
        mMessage.setText(getMessage());

        Button getAuthToken = (Button) findViewById(R.id.getAuthToken);
        Button deleteAll = (Button) findViewById(R.id.deleteAllAccounts);

        // Setup listeners
        getAuthToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
                if (availableAccounts.length > 0) {
                    String userName = mUsernameEdit.getText().toString().trim();

                    for (final Account account: availableAccounts) {
                        if(account.name.equalsIgnoreCase(userName)) {

                            final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, Constants.AUTHTOKEN_TYPE, null,
                                    AuthenticatorActivity.this, new
                                            AccountManagerCallback<Bundle>() {
                                        @Override
                                        public void run(AccountManagerFuture<Bundle> future) {
                                            try {
                                                Bundle bundle = future.getResult();
                                                String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                                                mAccountManager.invalidateAuthToken("com.google", authToken);
                                                authToken = mAccountManager.peekAuthToken(account, Constants.AUTHTOKEN_TYPE);

                                                Log.d(TAG, "Auth Retrevial: " + authToken);
                                            } catch (OperationCanceledException e) {
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            } catch (AuthenticatorException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    null);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Bundle bundle = future.getResult();

                                        final String authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                                        showMessage((authtoken != null) ? "SUCCESS!\ntoken: " + authtoken : "FAIL");
                                        Log.d("Resmed Acc Manager", "GetToken Bundle is " + bundle);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        showMessage(e.getMessage());
                                    }
                                }
                            }).start();
                            return;
                        }
                    }
                } else {
                    showMessage("No Account exists");
                }
            }
        });

        deleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
                if (availableAccounts.length > 0) {

                    for (Account account: availableAccounts) {
                        mAccountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                            @Override
                            public void run(AccountManagerFuture<Boolean> future) {
                                // This is the line that actually starts the call to remove the account.
                                try {
                                    boolean wasAccountDeleted = future.getResult();
                                    Log.d(TAG, "Accounts Deleted: " + wasAccountDeleted);
                                } catch (OperationCanceledException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (AuthenticatorException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, null);
                        //  Available for sdk > = 22
                        //  mAccountManager.removeAccountExplicitly(account);
                    }
                } else {
                    showMessage("No Account exists to delete");
                }
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getText(R.string.ui_activity_authenticating));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                Log.i(TAG, "user cancelling authentication");
                if (mAuthTask != null) {
                    mAuthTask.cancel(true);
                }
            }
        });
        // We save off the progress dialog in a field so that we can dismiss
        // it later. We can't just call dismissDialog(0) because the system
        // can lose track of our dialog if there's an orientation change.
        mProgressDialog = dialog;
        return dialog;
    }

    /**
     * Handles onClick event on the Submit button. Sends username/password to
     * the server for authentication. The button is configured to call
     * handleLogin() in the layout XML.
     *
     * @param view The Submit button for which this method is invoked
     */
    public void handleLogin(View view) {
        if (mRequestNewAccount) {
            mUsername = mUsernameEdit.getText().toString();
        }
        mPassword = mPasswordEdit.getText().toString();
        if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
            mMessage.setText(getMessage());
        } else {
            // Show a progress dialog, and kick off a background task to perform
            // the user login attempt.
            showProgress();
            mAuthTask = new keyAuthTask();
            mAuthTask.execute(mPassword);
        }
    }

    /**
     * Called when response is received from the server for confirm credentials
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller.
     *
     * @param result the confirmCredentials result.
     */
    private void finishConfirmCredentials(boolean result) {
        Log.i(TAG, "finishConfirmCredentials()");
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
        mAccountManager.setPassword(account, mPassword);
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
    }

    /**
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. We store the
     * authToken that's returned from the server as the 'password' for this
     * account - so we're never storing the user's actual password locally.
     *
     * @param authToken the confirmCredentials result.
     */
    private void finishLogin(String authToken) {

        Log.i(TAG, "finishLogin()");
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
        if (mRequestNewAccount) {
            mAccountManager.addAccountExplicitly(account, mPassword, null);
            // Set contacts sync for this account.
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
        } else {
            mAccountManager.setPassword(account, mPassword);
        }
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
    }

    /**
     * Called when the authentication process completes (see attemptLogin()).
     *
     * @param authToken the authentication token returned by the server, or NULL if
     *            authentication failed.
     */
    public void onAuthenticationResult(String authToken) {

        boolean success = ((authToken != null) && (authToken.length() > 0));
        Log.i(TAG, "onAuthenticationResult(" + success + ")");

        // Our task is complete, so clear it out
        mAuthTask = null;

        // Hide the progress dialog
        hideProgress();

        if (success) {
            if (!mConfirmCredentials) {
                finishLogin(authToken);
            } else {
                finishConfirmCredentials(success);
            }
            showMessage("Account Created for " + mUsername);
        } else {
            Log.e(TAG, "onAuthenticationResult: failed to authenticate");
            if (mRequestNewAccount) {
                // "Please enter a valid username/password.
                mMessage.setText(getText(R.string.login_activity_loginfail_text_both));
            } else {
                // "Please enter a valid password." (Used when the
                // account is already in the database but the password
                // doesn't work.)
                mMessage.setText(getText(R.string.login_activity_loginfail_text_pwonly));
            }
        }
    }

    public void onAuthenticationCancel() {
        Log.i(TAG, "onAuthenticationCancel()");

        // Our task is complete, so clear it out
        mAuthTask = null;

        // Hide the progress dialog
        hideProgress();
    }

    /**
     * Returns the message to be displayed at the top of the login dialog box.
     */
    private CharSequence getMessage() {
        getString(R.string.app_name);
        if (TextUtils.isEmpty(mUsername)) {
            // If no username, then we ask the user to log in using an
            // appropriate service.
            final CharSequence msg = getText(R.string.login_activity_newaccount_text);
            return msg;
        }
        if (TextUtils.isEmpty(mPassword)) {
            // We have an account but no password
            return getText(R.string.login_activity_loginfail_text_pwmissing);
        }
        return null;
    }

    /**
     * Shows the progress UI for a lengthy operation.
     */
    private void showProgress() {
        showDialog(0);
    }

    /**
     * Hides the progress UI for a lengthy operation.
     */
    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Represents an asynchronous task used to authenticate a user
     */
    public class keyAuthTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showMessage("Creating Account");
        }

        @Override
        protected String doInBackground(String... params) {
            // We do the actual work of authenticating the user
            // in the NetworkUtilities class.
            try {
                return params[0];
            } catch (Exception ex) {

                Log.e(TAG, "keyAuthTask.doInBackground: failed to authenticate");
                Log.i(TAG, ex.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String authToken) {
            // On a successful authentication, call back into the Activity to
            // communicate the authToken (or null for an error).
            onAuthenticationResult(authToken);
        }

        @Override
        protected void onCancelled() {
            // If the action was canceled (by the user clicking the cancel
            // button in the progress dialog), then call back into the
            // activity to let it know.
            onAuthenticationCancel();
        }
    }

    private void showMessage(final String msg) {
        if (TextUtils.isEmpty(msg))
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
