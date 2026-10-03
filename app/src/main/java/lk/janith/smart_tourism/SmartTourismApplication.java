package lk.janith.smart_tourism;

import android.app.Application;

public class SmartTourismApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppAppearance.applySavedTheme(this);
    }
}
