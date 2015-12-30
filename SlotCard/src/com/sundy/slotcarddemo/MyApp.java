package com.sundy.slotcarddemo;

import android.app.Application;
import com.bugtags.library.Bugtags;

/**
 * Created by sundy on 15/12/30.
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //在这里初始化
        Bugtags.start("0c92e22a1f5a7950b72ca605923ec241", this, Bugtags.BTGInvocationEventBubble);
    }
}