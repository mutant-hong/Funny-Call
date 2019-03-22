package com.myhexaville.androidwebrtc;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

public class CallDataDB extends SQLiteOpenHelper {

    public static final String tableName = "test3";

    public CallDataDB(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
        //db.execSQL("DROP TABLE Calldata");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void createTable(SQLiteDatabase db)
    {
        String sql = "CREATE TABLE " + tableName + "(IDX INTEGER PRIMARY KEY AUTOINCREMENT, friendId VARCHAR(20), date VARCHAR(20), time INTEGER, caller VARCHAR(20), missied VARCHAR(20))";
        try
        {
            db.execSQL(sql);
        }
        catch (SQLException e)
        {

        }
    }

    public void insertData(SQLiteDatabase db, String friendId, String date, long time, String caller, String missied)
    {
        db.beginTransaction();
        try
        {

            String sql = "insert into " + tableName + "(friendId, date, time, caller, missied)" + " values('" + friendId + "', '"+ date+"', '" + time + "', '" + caller + "', '" + missied +"')";
            db.execSQL(sql);
            db.setTransactionSuccessful();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.endTransaction();
        }
    }

    public void delete(SQLiteDatabase db, int index){
        db.beginTransaction();
        try
        {
            String sql = "delete from " + tableName + " where IDX = " + index;
            db.execSQL(sql);
            db.setTransactionSuccessful();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.endTransaction();
        }
    }

    public void sort(SQLiteDatabase db){
        db.beginTransaction();
        try
        {
            //빈 테이블 생성
            String sql = "CREATE TABLE tmp (IDX INTEGER PRIMARY KEY AUTOINCREMENT, friendId VARCHAR(20), date VARCHAR(20), time INTEGER, caller VARCHAR(20), missied VARCHAR(20))";
            db.execSQL(sql);

            //빈 테이블에 데이터 복사
            sql = "insert into tmp (friendId, date, time, caller, missied) select friendId, date, time, caller, missied from " + tableName;
            db.execSQL(sql);

            //원래 테이블 삭제
            sql = "drop table " + tableName;
            db.execSQL(sql);

            //다시 생성
            createTable(db);

            //데이터 다시 복사
            sql = "insert into " + tableName + "(friendId, date, time, caller, missied) select friendId, date, time, caller, missied from tmp";
            db.execSQL(sql);

            //빈테이블 삭제
            sql = "drop table tmp";
            db.execSQL(sql);

            db.setTransactionSuccessful();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.endTransaction();
        }
    }


    //
//    public CallDataDB(String friendId, String date, String time){
//        this.friendId = friendId;
//        this.date = date;
//        this.time = time;
//    }
//
//    public void saveCallData(){
//
//        try {
//            CallDB = CallDataDB.openOrCreateDatabase(dbName, MODE_PRIVATE, null);
//        }
//    }

}
