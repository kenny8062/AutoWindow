package com.example.iot_autowindow;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Location_select extends AppCompatActivity {
    private List<String> list;
    private ListView listView;
    private EditText editSearch;
    private LocationAdapter adapter;
    private ArrayList<String> arrayList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_select);
        editSearch=findViewById(R.id.editSearch);
        listView=(ListView) findViewById(R.id.listView);
        Button back_button = findViewById(R.id.button_exit);
        list=new ArrayList<String>();
        settingList();

        arrayList=new ArrayList<String>();
        arrayList.addAll(list);

        adapter=new LocationAdapter(list,this);

        listView.setAdapter(adapter);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
            }
        });
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = editSearch.getText().toString();
                search(text);
            }
        });



        listView.setOnItemClickListener(listener);


    }
    AdapterView.OnItemClickListener listener= new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                System.out.println(list.get(position));
                uploadWithTransferUtility(list.get(position),"location_data.txt","sensor-esp32");
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    public void search(String charText){
        list.clear();

        if(charText.length()==0){
            list.addAll(arrayList);
        }
        else{
            for (int i=0;i<arrayList.size();i++){
                if (arrayList.get(i).toLowerCase().contains(charText)){
                    list.add(arrayList.get(i));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
    //s3에 선택한 지역 업로드
    public void uploadWithTransferUtility(String dummy,String filename,String bucketname) throws IOException {
        FileOutputStream fileOutputStream=new FileOutputStream(getFilesDir() + filename,false);
        byte []tempByte = dummy.getBytes();
        int length = dummy.length();
        fileOutputStream.write(length);
        fileOutputStream.write(tempByte);
        fileOutputStream.close();
        File file2=new File(getFilesDir()+filename);
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-2:f80fe4d0-e4d3-4e09-be94-f5d452722201", // 자격 증명 풀 ID
                Regions.AP_NORTHEAST_2 // 리전
        );
        TransferNetworkLossHandler.getInstance(getApplicationContext());
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .defaultBucket(bucketname)
                        .s3Client(new AmazonS3Client(credentialsProvider, Region.getRegion(Regions.AP_NORTHEAST_2)))
                        .build();
        TransferObserver uploadObserver =
                transferUtility.upload(
                        bucketname,
                        filename, file2);

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
    //크롤링한 데이터들의 지역들
    private void settingList(){
        list.add("강릉");
        list.add("강진군");
        list.add("강화");
        list.add("거제");
        list.add("거창");
        list.add("경주시");
        list.add("고산");
        list.add("고창");
        list.add("고흥");
        list.add("광양시");
        list.add("광주");
        list.add("구미");
        list.add("군산");
        list.add("금산");
        list.add("김해시");
        list.add("남원");
        list.add("남해");
        list.add("대관령");
        list.add("대구");
        list.add("대전");
        list.add("동두천");
        list.add("동해");
        list.add("목포");
        list.add("문경");
        list.add("밀양");
        list.add("백령도");
        list.add("보령");
        list.add("보성군");
        list.add("보은");
        list.add("봉화");
        list.add("부산");
        list.add("부안");
        list.add("부여");
        list.add("북강릉");
        list.add("북창원");
        list.add("북춘천");
        list.add("산청");
        list.add("상주");
        list.add("서귀포");
        list.add("서산");
        list.add("서울");
        list.add("성산");
        list.add("세종");
        list.add("속초");
        list.add("수원");
        list.add("순창군");
        list.add("안동");
        list.add("양산시");
        list.add("양평");
        list.add("여수");
        list.add("영광군");
        list.add("영덕");
        list.add("영월");
        list.add("영주");
        list.add("영천");
        list.add("완도");
        list.add("울릉도");
        list.add("울산");
        list.add("울진");
        list.add("원주");
        list.add("의령군");
        list.add("의성");
        list.add("이천");
        list.add("인제");
        list.add("인천");
        list.add("임실");
        list.add("장수");
        list.add("장흥");
        list.add("전주");
        list.add("정선군");
        list.add("정읍");
        list.add("제주");
        list.add("제천");
        list.add("진도군");
        list.add("진주");
        list.add("창원");
        list.add("천안");
        list.add("철원");
        list.add("청송군");
        list.add("청주");
        list.add("추풍령");
        list.add("춘천");
        list.add("충주");
        list.add("태백");
        list.add("통영");
        list.add("파주");
        list.add("포항");
        list.add("함양군");
        list.add("합천");
        list.add("해남");
        list.add("홍성");
        list.add("홍천");
        list.add("흑산도");
    }
}