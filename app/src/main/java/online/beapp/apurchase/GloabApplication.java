package online.beapp.apurchase;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.LogLevel;

public class GloabApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        String env = AdjustConfig.ENVIRONMENT_PRODUCTION;
        String appToken = getResources().getString(R.string.adjust_token);
        AdjustConfig config = new AdjustConfig(this, appToken, env);
        config.setLogLevel(LogLevel.VERBOSE);
        config.setOnDeeplinkResponseListener(deeplink -> {
            Intent intent = new Intent("deeplinkReceiver.intent.org");
            intent.setData(deeplink);
            sendBroadcast(intent);
            return true;
        });

        Adjust.onCreate(config);
        registerActivityLifecycleCallbacks(new AdjustLifeCycleCallbacks());
    }

    private static final class AdjustLifeCycleCallbacks implements ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }
    }
}

