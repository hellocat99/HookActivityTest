package com.example.testing.hookactivitytest;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

//占坑的activity
public class HoldActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hold);
    }
}
