package com.example.iot_autowindow;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String temp = "3";
        String humid = "3";
        String Window = "open";

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FirebaseSettingEx", "getInstanceId failed", task.getException());
                return;
            }
            else{
                String token = task.getResult().getToken();
                System.out.println(token);
            }
        });
        TextView text_temp=findViewById(R.id.textView);
        TextView text_humid=findViewById(R.id.textView2);
        TextView text_window=findViewById(R.id.textView3);
        Button button_open=findViewById(R.id.button);
        Button button_close=findViewById(R.id.button2);
        text_temp.setText("온도 : " + temp);
        text_humid.setText("습도 : " + humid);
        text_window.setText("창문 : " +Window);
        button_open.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });
        button_close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });

    }
}