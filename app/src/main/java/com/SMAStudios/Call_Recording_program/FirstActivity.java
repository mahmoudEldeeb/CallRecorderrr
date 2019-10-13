package com.SMAStudios.Call_Recording_program;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.SMAStudios.Call_Recording_program.fragment.FirstFragment;
import com.SMAStudios.Call_Recording_program.fragment.SecondFragment;


public class FirstActivity extends AppCompatActivity implements FirstFragment.ChangePage{
    Fragment newFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        if(getIntent().getIntExtra("page",0)==0) {

            newFragment= new FirstFragment();
        }
        else {
            newFragment= new SecondFragment();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fram, newFragment);
        transaction.commit();

    }

    @Override
    public void change() {
        newFragment= new SecondFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fram, newFragment);
        transaction.commit();
    }
}
