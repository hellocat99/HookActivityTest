package com.example.testing.hookactivitytest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hookStartActivity();
        hookLauchActivity();

        findViewById(R.id.tv_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //注意这里我们的CarActivity是没有在配置文件中注册过的
                MainActivity.this.startActivity(
                        new Intent(MainActivity.this, CarActivity.class));
            }
        });
    }

    private void hookLauchActivity() {
        try {
            Class<?> amClass = Class.forName("android.app.ActivityThread");
            Method cAThreadMethod = amClass.getDeclaredMethod("currentActivityThread");
            cAThreadMethod.setAccessible(true);
            //获取到了ActivityManager中的sCurrentActivityThread属性
            Object sCurrentActivityThread = cAThreadMethod.invoke(null);

            Field mHField = amClass.getDeclaredField("mH");
            mHField.setAccessible(true);

            //获取到了ActivityManager中的mH属性
            Object mH = mHField.get(sCurrentActivityThread);

            Class<?> handlerClass = Class.forName("android.os.Handler");
            Field mCallbackField = handlerClass.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mH, new ActivityThreadHandlerCallback(mH));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookStartActivity() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Class<?> aClass = Class.forName("android.app.ActivityManager");
                Field getService = aClass.getDeclaredField("IActivityManagerSingleton");
                getService.setAccessible(true);
                //获取到了ActivityManager中的IActivityManagerSingleton属性
                Object IActivityManagerSingleton = getService.get(null);

                Class<?> singletonClass = Class.forName("android.util.Singleton");
                Field mInstance = singletonClass.getDeclaredField("mInstance");
                mInstance.setAccessible(true);
                //获取到了ActivityManager单例对象
                Object IActivityManagerObj = mInstance.get(IActivityManagerSingleton);

                Class<?> IActivityManagerClass = Class.forName("android.app.IActivityManager");
                ActivityInvocationHandler invocationHandler = new ActivityInvocationHandler(IActivityManagerObj);
                // 生成IActivityManagerObj的代理对象
                Object proxyInstance = Proxy.newProxyInstance(getBaseContext().getClassLoader(),
                        new Class<?>[]{IActivityManagerClass}, invocationHandler);

                mInstance.set(IActivityManagerSingleton, proxyInstance);
            } else {
                Class<?> amnClass = Class.forName("android.app.ActivityManagerNative");
                Field gDefaultField = amnClass.getDeclaredField("gDefault");
                gDefaultField.setAccessible(true);
                //获取到了ActivityManagerNative中的gDefault属性
                Object gDefault = gDefaultField.get(null);

                Class<?> singletonClass = Class.forName("android.util.Singleton");
                Field mInstanceField = singletonClass.getDeclaredField("mInstance");
                mInstanceField.setAccessible(true);
                //获取到了ActivityManager单例对象
                Object IActivityManagerObj = mInstanceField.get(gDefault);

                Class<?> IActivityManagerClass = Class.forName("android.app.IActivityManager");
                ActivityInvocationHandler invocationHandler = new ActivityInvocationHandler(IActivityManagerObj);
                // 生成IActivityManagerObj的代理对象
                Object proxyInstance = Proxy.newProxyInstance(getBaseContext().getClassLoader(),
                        new Class<?>[]{IActivityManagerClass}, invocationHandler);

                mInstanceField.set(gDefault, proxyInstance);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ActivityThreadHandlerCallback implements Handler.Callback {

        Object mH;

        public ActivityThreadHandlerCallback(Object mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 100: {
                    try {
                        //100对应LAUNCH_ACTIVITY
                        //r对应的是ActivityClientRecord类型
                        final Object r = msg.obj;
                        Field intentField = r.getClass().getDeclaredField("intent");
                        intentField.setAccessible(true);
                        //获取到了intent了
                        Intent intent = (Intent) intentField.get(r);
                        Intent realIntent = intent.getParcelableExtra("realIntent");
                        if (realIntent != null) {
                            intentField.set(r, realIntent);
                        }
                        Log.d("wkl", "bbbb");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            return false;
        }
    }

    private class ActivityInvocationHandler implements InvocationHandler {

        Object iActivityManagerObj;

        public ActivityInvocationHandler(Object iActivityManagerObj) {
            this.iActivityManagerObj = iActivityManagerObj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.d("wkl", "aaaaa");
            if ("startActivity".equals(method.getName())) {
                int intentIndex = -1;
                Intent intent = null;
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Intent) {
                            intentIndex = i;
                            intent = (Intent) args[i];
                            break;
                        }
                    }
                }
                if (intentIndex >= 0) {
                    Intent newIntent = new Intent(MainActivity.this, HoldActivity.class);
                    newIntent.putExtra("realIntent", intent);
                    args[intentIndex] = newIntent;
                }
            }
            return method.invoke(this.iActivityManagerObj, args);
        }
    }
}
