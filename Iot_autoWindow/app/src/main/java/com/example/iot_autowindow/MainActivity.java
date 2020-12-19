package com.example.iot_autowindow;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    static Context context;
    TextView home_temp;
    TextView home_humid;
    TextView window;
    TextView out_temp;
    TextView out_humid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FirebaseSettingEx", "getInstanceId failed", task.getException());
                return;
            }
            else{
                String token = task.getResult().getToken();
                Log.w("FirebaseSettingEx", token);
                System.out.println(token);
            }
        });

        downloadWithTransferUtility();
        Button button_open=findViewById(R.id.button2);
        Button button_close=findViewById(R.id.button);
        button_open.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    uploadWithTransferUtility("열림");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        button_close.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    uploadWithTransferUtility("닫힘");
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

        File file = new File(getFilesDir(),"text.txt");
        File file2 = new File(getFilesDir(),"text2.txt");
        String string="text.txt";
        String string2="text2.txt";
        TransferObserver downloadObserver =
                transferUtility.download(
                        string,
                        file);

        TransferObserver downloadObserver2 =
                transferUtility.download(
                        string2,
                        file2);

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
                    String[] str=tv.get(0).split(", ");
                    str[0]=Double.toString(Math.round(Double.parseDouble(str[0])*10)/10.0);
                    str[1]=Double.toString(Math.round(Double.parseDouble(str[1])*10)/10.0);
                    home_temp =findViewById(R.id.home_temp);
                    home_humid =findViewById(R.id.home_humid);
                    window =findViewById(R.id.home_door);
                    home_temp.setText("실내온도 : "+str[0]+ "도");
                    home_humid.setText("실내습도 : "+str[1] + "%");
                    if(str[2].equals("1")) window.setText("현재 문이 열려있습니다.");
                    else window.setText("현재 문이 닫혀있습니다.");
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

        downloadObserver2.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                try {
                    BufferedReader buf2 = new BufferedReader(new FileReader(file2));
                    String line2;
                    ArrayList<String> tv2 = new ArrayList<>();
                    while ((line2=buf2.readLine())!=null){
                        tv2.add(line2);
                    }
                    for(int i=0;i<tv2.size();i++){
                        System.out.println(tv2.get(i));
                    }
                    out_temp =findViewById(R.id.out_temp);
                    out_humid =findViewById(R.id.out_humid);
                    out_temp.setText("외부온도 : 20도");
                    out_humid.setText("외부습도 : 40%");
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
    public void uploadWithTransferUtility(String str) throws IOException {
        String filename="door.txt";
        FileOutputStream fos = null ;
        BufferedOutputStream bufos = null ;
        File file = new File(getFilesDir(),"text.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        String dummy="";
        ArrayList<String> tv = new ArrayList<>();

        while ((line=br.readLine())!=null){
            tv.add(line);
        }

        for(int i=0;i<tv.size();i++){
            System.out.println(tv.get(i));
        }

        String[] str2=tv.get(0).split(", ");
        System.out.println(str2[2]);
        if(str.equals("열림") &&str2[2].equals("0")) dummy+="1";
        else if(str.equals("닫힘")&&str2[2].equals("1")) dummy +="0";
        else return;
        br.close();

        FileOutputStream fileOutputStream=new FileOutputStream(getFilesDir() + "door.txt",false);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        bw.write(dummy);
        bw.flush();
        bw.close();
        fileOutputStream.close();
        File file2=new File(getFilesDir()+"door.txt");
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-2:f80fe4d0-e4d3-4e09-be94-f5d452722201", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );
        TransferNetworkLossHandler.getInstance(getApplicationContext());
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .defaultBucket("android-to-esp32")
                        .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                        .build();
        TransferObserver uploadObserver =
                transferUtility.upload(
                        "android-to-esp32",
                        "door.txt", file2);
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