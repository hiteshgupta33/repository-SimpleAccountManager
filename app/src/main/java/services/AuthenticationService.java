package services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import authenticator.ResmedAuthenticator;

public class AuthenticationService extends Service {

    private static final String TAG = AuthenticationService.class.getSimpleName();

    private ResmedAuthenticator resAuthenticator;

    public AuthenticationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Creating Authentication Service");
        resAuthenticator = new ResmedAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "getBinder()...  returning the AccountAuthenticator binder for intent "
                    + intent);
        }
        return resAuthenticator.getIBinder();
    }
}
