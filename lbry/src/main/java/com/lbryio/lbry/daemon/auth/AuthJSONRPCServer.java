package com.lbryio.lbry.daemon.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by zhang on 7/10/17.
 */

public class AuthJSONRPCServer extends Service{

    public AuthJSONRPCServer(boolean use_auth_http) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
