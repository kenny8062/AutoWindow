package com.example.iot_autowindow;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Context context=this;
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
        Button button_open=findViewById(R.id.button2);
        Button button_close=findViewById(R.id.button);
        text_temp.setText("온도 : " + temp);
        text_humid.setText("습도 : " + humid);
        text_window.setText("창문 : " +Window);
        button_open.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                downloadWithTransferUtility();
            }
        });
        button_close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });

    }

    private void downloadWithTransferUtility(){
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-2:f80fe4d0-e4d3-4e09-be94-f5d452722201", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );
        TransferNetworkLossHandler.getInstance(getApplicationContext());
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .defaultBucket("sensor-esp32")
                        .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                        .build();

        File file = new File(getApplicationContext().getFilesDir(), "/test.txt");
        String string="test.txt";
        TransferObserver downloadObserver =
                transferUtility.download(
                        string,
                        file);
        downloadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                try {
                    BufferedReader buf = new BufferedReader(new FileReader(file));
                    String line=null;
                    ArrayList<String> tv = new ArrayList<>();
                    while ((line=buf.readLine())!=null){
                        tv.add(line);
                    }
                    for(int i=0;i<tv.size();i++){
                        System.out.println(tv.get(i));
                    }
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d("AWS", "DOWNLOAD - - ID: $id, percent done = $done");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d("AWS", "DOWNLOAD ERROR - - ID: $id - - EX: ${ex.message.toString()}");
            }
        });
    }
    public void uploadWithTransferUtility(String filename,File file){
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-2:f80fe4d0-e4d3-4e09-be94-f5d452722201", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );
        TransferNetworkLossHandler.getInstance(getApplicationContext());
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .defaultBucket("sensor-esp32")
                        .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                        .build();
        TransferObserver uploadObserver =
                transferUtility.upload(
                        "BUKET_PATH/${filename}",
                        file, CannedAccessControlList.PublicRead);
        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if(state ==TransferState.COMPLETED){
                    System.out.println("upload완료");
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d("MYTAG", "UPLOAD - - ID: $id, percent done = $done");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d("MYTAG", "UPLOAD ERROR - - ID: $id - - EX: ${ex.message.toString()}");
            }
        });

    }
}