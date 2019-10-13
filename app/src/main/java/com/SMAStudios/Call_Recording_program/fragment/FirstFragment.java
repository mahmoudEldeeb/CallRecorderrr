package com.SMAStudios.Call_Recording_program.fragment;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.SMAStudios.Call_Recording_program.R;

import static android.content.Context.MODE_PRIVATE;

public class FirstFragment extends Fragment {


    public FirstFragment() {
        // Required empty public constructor
    }
    Button next;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_first, container, false);
        TextView mlink=view.findViewById(R.id.mlink);
        next=view.findViewById(R.id.next);
        mlink.setMovementMethod(LinkMovementMethod.getInstance());
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences=getActivity().getSharedPreferences("com.SMAStudios.Call_Recording_program",MODE_PRIVATE);
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putInt("time",1).commit();
                ChangePage changePage= (ChangePage) getActivity();
                changePage.change();

            }
        });
        return view;
    }

    public interface ChangePage{
        public void change();
    }

}
