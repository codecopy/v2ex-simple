package im.fdx.v2ex;

import android.app.Application;

/**
 * Created by fdx on 2015/8/16.
 */
public class MyApplication extends Application {
    private static MyApplication instance;
    public static MyApplication getInstance() {
        return instance;
    }
    @Override
    public void onCreate() {
        //  Auto-generated method stub
        super.onCreate();
        instance = this;
    }
}
