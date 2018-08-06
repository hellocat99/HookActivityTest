package com.example.testing.hookactivitytest;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
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
        hookActivity();
        findViewById(R.id.tv_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //注意这里我们的CarActivity是没有在配置文件中注册过的
                MainActivity.this.startActivity(new Intent(MainActivity.this, HoldActivity.class));
            }
        });
    }

    private void hookActivity() {
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
            }


        } catch (Exception e) {
            e.printStackTrace();
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
            return method.invoke(this.iActivityManagerObj, args);
        }
    }
}
