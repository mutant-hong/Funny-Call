package com.myhexaville.androidwebrtc;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.util.ArrayList;

public class CallHistoryActivity extends AppCompatActivity {

    ImageView list, friends;
    Button del, edit;
    CheckBox all_check;

    CallDataDB callDataDB;
    SQLiteDatabase database;
    String sql = "select * from " + callDataDB.tableName + " order by IDX DESC";

    Cursor cursor;

    //리사이클러뷰
    RecyclerView recycler;
    RecyclerView.LayoutManager layoutManager;

//    private AdapterItem mAdapter;

    public static boolean check = false, all = false;

    private ArrayList<history> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callhistory);

        historyList = new ArrayList<history>();

        list = (ImageView)findViewById(R.id.list);
        friends = (ImageView)findViewById(R.id.friends);

        del = (Button)findViewById(R.id.del);
        edit = (Button)findViewById(R.id.edit);

        all_check = (CheckBox)findViewById(R.id.all_check);

//        recycler = (RecyclerView)findViewById(R.id.recycler);
//        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
//        recycler.setLayoutManager(mLayoutManager);
//        mAdapter = new AdapterItem(this);
//        mAdapter.setLinearLayoutManager(mLayoutManager);
//        mAdapter.setRecyclerView(recycler);
//        recycler.setAdapter(mAdapter);


        recycler = (RecyclerView)findViewById(R.id.recycler);
        layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);


        callDataDB = new CallDataDB(CallHistoryActivity.this, CallDataDB.tableName, null, 1);
        database = callDataDB.getWritableDatabase();

//        select();
//
//        historyAdapter historyAdapter = new historyAdapter(historyList);
//        recycler.setAdapter(historyAdapter);



    }

    private void select(){
        cursor = database.rawQuery(sql, null);


        if (cursor != null)
        {

            for (int i = 0; i < cursor.getCount(); i++) {

                cursor.moveToNext();
                String friendId = cursor.getString(cursor.getColumnIndex("friendId"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                String time = cursor.getString(cursor.getColumnIndex("time"));
                String caller = cursor.getString(cursor.getColumnIndex("caller"));
                String missied = cursor.getString(cursor.getColumnIndex("missied"));
                historyList.add(new history(friendId, date, time, caller, missied));

                Log.d("auto", cursor.getString(cursor.getColumnIndex("IDX")));
            }

            cursor.close();
        }
    }

    @Override
    protected void onStart(){
        super.onStart();

        if(!historyList.isEmpty())
            historyList.clear();
        select();

        historyAdapter historyAdapter = new historyAdapter(historyList);
        recycler.setAdapter(historyAdapter);


        Log.d("CallHistoryActivity", "onStart");

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //완료 -> 편집
                if (check){
                    check = false;
                    del.setVisibility(View.INVISIBLE);
                    edit.setText("편집     ");
                    all_check.setVisibility(View.GONE);
                }

                //편집 -> 완료
                else{
                    check = true;
                    del.setVisibility(View.VISIBLE);
                    edit.setText("완료     ");
                    all_check.setVisibility(View.VISIBLE);
                }

                historyAdapter historyAdapter = new historyAdapter(historyList);
                recycler.setAdapter(historyAdapter);

            }
        });

        all_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    all = true;
                else
                    all = false;

                historyAdapter historyAdapter = new historyAdapter(historyList);
                recycler.setAdapter(historyAdapter);
            }
        });

        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String str = "";
                for(int i = 0; i < historyAdapter.checkIdx.length; i++){

                    if(historyAdapter.checkIdx[i] == 1) {
                        str += (i + 1) + " / ";
                        callDataDB.delete(database, historyAdapter.checkIdx.length - i);
                    }

                }

                historyList.clear();

                callDataDB.sort(database);
                select();

                if(all){
                    all_check.setChecked(false);
                    all = false;
                }

                historyAdapter historyAdapter = new historyAdapter(historyList);
                recycler.setAdapter(historyAdapter);

                //Toast.makeText(CallHistoryActivity.this, str, Toast.LENGTH_SHORT).show();

            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                finish();
            }
        });

        friends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FriendsActivity.class);
                startActivity(intent);
                overridePendingTransition(0,0);
                finish();
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        Log.d("CallHistoryActivity", "onResume");
    }

    @Override
    protected void onPause(){
        super.onPause();

        Log.d("CallHistoryActivity", "onPause");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        Log.d("CallHistoryActivity", "onDestroy");
    }
/*
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity_", "onStart");
        loadData();
    }

    @Override
    public void onLoadMore() {
        Log.d("MainActivity_", "onLoadMore");
        //mAdapter.setProgressMore(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                historyList.clear();
                //mAdapter.setProgressMore(false);

                ///////이부분에을 자신의 프로젝트에 맞게 설정하면 됨
                //다음 페이지? 내용을 불러오는 부분
                int start = mAdapter.getItemCount();
                Log.d("start", start + "");
                int end = start + 10;

                if(end <= cursor.getCount())
                    end = cursor.getCount();

                if(cursor != null){

                    for (int i = start + 1; i <= end; i++) {
                        cursor.moveToPosition(i-1);
                        String friendId = cursor.getString(cursor.getColumnIndex("friendId"));
                        String date = cursor.getString(cursor.getColumnIndex("date"));
                        String time = cursor.getString(cursor.getColumnIndex("time"));
                        String caller = cursor.getString(cursor.getColumnIndex("caller"));
                        String missied = cursor.getString(cursor.getColumnIndex("missied"));
                        historyList.add(new history(friendId, date, time, caller, missied));
                    }
                }

                //////////////////////////////////////////////////
                mAdapter.addItemMore(historyList);
                mAdapter.setMoreLoading(false);

            }
        }, 0);
    }

    private void loadData() {
        historyList.clear();

        cursor = database.rawQuery(sql, null);

        if (cursor != null)
        {
            if(cursor.getCount() >= 10) {

                for (int i = 1; i <= 10; i++) {

                    cursor.moveToNext();
                    String friendId = cursor.getString(cursor.getColumnIndex("friendId"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));
                    String time = cursor.getString(cursor.getColumnIndex("time"));
                    String caller = cursor.getString(cursor.getColumnIndex("caller"));
                    String missied = cursor.getString(cursor.getColumnIndex("missied"));
                    historyList.add(new history(friendId, date, time, caller, missied));
                }
            }
            else{
                for (int i = 1; i <= cursor.getCount(); i++) {

                    cursor.moveToNext();
                    String friendId = cursor.getString(cursor.getColumnIndex("friendId"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));
                    String time = cursor.getString(cursor.getColumnIndex("time"));
                    String caller = cursor.getString(cursor.getColumnIndex("caller"));
                    String missied = cursor.getString(cursor.getColumnIndex("missied"));
                    historyList.add(new history(friendId, date, time, caller, missied));
                }
            }
        }

        Log.d("cursor", cursor.getPosition() + "");

        mAdapter.addAll(historyList);
    }
*/
}
