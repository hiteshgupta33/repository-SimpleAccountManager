package authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import Utils.Constants;
import accountmanager.com.hitesh.activities.AuthenticatorActivity;


/**
 * Created by hiteshg on 10/13/15.
 */
public class ResmedAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = ResmedAuthenticator.class.getSimpleName().toString();

    private Context cxt;

    public ResmedAuthenticator(Context context) {
        super(context);
        cxt = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "Adding Account");

        final Intent intent = new Intent(cxt, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;

    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.v(TAG, "confirmCredentials");
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.v(TAG, "getAuthToken");

        Log.d("AM", TAG + "> getAuthToken");

        // If the caller requested an authToken type we don't support, then
        // return an error
        final AccountManager am = AccountManager.get(cxt);

        String authToken = am.peekAuthToken(account, authTokenType);

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                authToken = account + password;
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(cxt, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(Constants.ACCOUNT_TYPE, account.type);
        intent.putExtra(Constants.AUTHTOKEN_TYPE, authTokenType);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.v(TAG, "getAuthTokenLabel");
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.v(TAG, "updateCredentials");
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        Log.v(TAG, "hasFeatures()");
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
        Bundle result = new Bundle();
        boolean allowed = true; // or whatever logic you want here
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, allowed);
        return result;
    }
}
