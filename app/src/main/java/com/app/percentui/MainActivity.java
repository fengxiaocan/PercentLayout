package com.app.percentui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{

    private TextView mTvUser;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvUser=findViewById(R.id.tv_user);
        mTvUser.postDelayed(new Runnable(){
            @Override
            public void run(){
                mTvUser.setText("这是一个美好的时代");
            }
        },3000);
    }
}
