package com.example.iot_autowindow;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    TextView home_region;
    String window_state="";
    String region="";
    ArrayList<String> all_location_data = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        region="";
        context=this;
        //Firebase 토큰값 확인
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
        //s3데이터 레이아웃 적용
        downloadWithTransferUtility("text.txt","sensor-esp32");

        Button button_open=findViewById(R.id.button2);
        Button button_close=findViewById(R.id.button);
        Button button_location=findViewById(R.id.button3);
        ImageView button_update=findViewById(R.id.bt_update);
        button_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=getIntent();
                finish();
                startActivity(intent);
            }
        });
        //창문열기 버튼
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
        //창문닫기 버튼
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
        //지역 선택 액티비티 이동
        button_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),Location_select.class);
                startActivity(intent);
            }
        });
    }
    //S3 파일 다운로드
    private void downloadWithTransferUtility(String filename,String bucketname){
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
        TransferUtility transferUtility2=TransferUtility.builder()
                .context(getApplicationContext())
                .defaultBucket("all-region-data")
                .s3Client(new AmazonS3Client(credentialsProvider,Region.getRegion(Regions.AP_NORTHEAST_2)))
                .build();
        File file = new File(getFilesDir(),"text.txt");
        File file2 = new File(getFilesDir(),"location_data.txt");
        File file3 = new File(getFilesDir(),"all_location_data.txt");
        TransferObserver downloadObserver = transferUtility.download("text.txt", file);
        TransferObserver downloadObserver2 = transferUtility.download("location_data.txt",file2);
        TransferObserver downloadObserver3 = transferUtility2.download("all_location_data.txt",file3);

        //내부 온도 및 습도 파일 다운
        downloadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                try {
                    BufferedReader buf = new BufferedReader(new FileReader(file));
                    String line = null;
                    ArrayList<String> tv = new ArrayList<>();
                    while ((line = buf.readLine()) != null) {
                        tv.add(line);
                    }
                    for (int i = 0; i < tv.size(); i++) {
                        System.out.println(tv.get(i));
                    }
                    String[] str = tv.get(0).split(", ");
                    str[0] = Double.toString(Math.round(Double.parseDouble(str[0]) * 10) / 10.0);
                    str[1] = Double.toString(Math.round(Double.parseDouble(str[1]) * 10) / 10.0);
                    window_state=str[2];
                    home_temp = findViewById(R.id.home_temp);
                    home_humid = findViewById(R.id.home_humid);
                    window = findViewById(R.id.home_door);
                    home_temp.setText("실내온도 : " + str[0] + "도");
                    home_humid.setText("실내습도 : " + str[1] + "%");
                    if (str[2].equals("1")) window.setText("현재 문이 열려있습니다.");
                    else window.setText("현재 문이 닫혀있습니다.");
                    buf.close();
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
        //이전에 선택했던 지역 저장한 파일 다운
        downloadObserver2.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                try {
                    BufferedReader buf = new BufferedReader(new FileReader(file2));
                    String line = null;
                    ArrayList<String> tv = new ArrayList<>();
                    while ((line = buf.readLine()) != null) {
                        tv.add(line);
                    }
                    for (int i = 0; i < tv.size(); i++) {
                        System.out.println(tv.get(i));
                    }
                    region=tv.get(0);
                    region=region.substring(1);
                    buf.close();
                    System.out.println(region);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {

            }
        });
        //S3에서 크롤링한 데이터를 가져와서 선택한 지역의 온도 및 습도를 찾아서 적용
        downloadObserver3.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                try {
                    BufferedReader buf = new BufferedReader(new FileReader(file3));
                    String line = null;
                    ArrayList<String> tv = new ArrayList<>();
                    while ((line = buf.readLine()) != null) {
                        tv.add(line);
                    }
                    for (int i = 0; i < tv.size(); i++) {
                        System.out.println(tv.get(i));
                    }
                    int i=tv.get(0).indexOf(region);
                    int j=tv.get(0).indexOf("]",i);
                    System.out.println(region);
                    String region_str="";
                    for(int k=i-1;k<j;k++){
                        region_str +=tv.get(0).charAt(k);
                    }
                    System.out.println(region_str);
                    String[] str = region_str.split(", ");
                    if(region.equals("")){
                        region="서울";
                    }
                    System.out.println(str.length);
                    System.out.println(str[0]);
                    for (int q=0;q<str.length;q++){
                        str[q]=str[q].substring(1);
                        str[q]=str[q].substring(0,str[q].length()-1);
                        System.out.println(str[q]);
                    }
                    out_temp =findViewById(R.id.out_temp);
                    out_humid =findViewById(R.id.out_humid);
                    home_region=findViewById(R.id.home_address);
                    out_temp.setText("외부온도 : " + str[1]+"도");
                    out_humid.setText("외부습도 : "+str[2]+"%");
                    home_region.setText(region);
                    buf.close();
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {

            }
        });



    }
    //S3에 창문 제어를 위한 업로드
    public void uploadWithTransferUtility(String str) throws IOException {
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
        if(str.equals("열림") &&str2[2].equals("0")) dummy+="{ \"state\" : {\"order\" : \"OPEN\"}}";
        else if(str.equals("닫힘")&&str2[2].equals("1")) dummy +="{ \"state\" : {\"order\" : \"CLOSE\"}}";
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
                        .defaultBucket("onoff")
                        .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                        .build();
        TransferObserver uploadObserver =
                transferUtility.upload(
                        "onoff",
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