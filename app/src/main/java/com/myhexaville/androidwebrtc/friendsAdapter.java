package com.myhexaville.androidwebrtc;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class friendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    int pos;

    boolean accept = false;

    public static class friendsViewHolder extends RecyclerView.ViewHolder {

        TextView friendId;
        Button acceptBtn;
        public final View mView;

        friendsViewHolder(View view) {
            super(view);
            mView = view;

            friendId = view.findViewById(R.id.friendId);
            acceptBtn = view.findViewById(R.id.acceptBtn);

        }

    }

    private ArrayList<String> friendslist;

    friendsAdapter(ArrayList<String> friendslist){
        this.friendslist = friendslist;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_friendsitem, parent, false);

        return new friendsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final friendsViewHolder myViewHolder = (friendsViewHolder) holder;

        myViewHolder.friendId.setText(friendslist.get(position));
        myViewHolder.acceptBtn.setTag(holder.getAdapterPosition());

        //다이어로그 yes or no 추가해야됨
        myViewHolder.acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context = v.getContext();
                pos = (int)v.getTag();
                //Toast.makeText(context, myViewHolder.item.getText().toString(), Toast.LENGTH_SHORT).show();

                String friendId = myViewHolder.friendId.getText().toString();
                String IP_ADDRESS = "115.68.216.237";

                acceptFriends task = new acceptFriends();
                task.execute("http://" + IP_ADDRESS + "/faceToface/acceptFriends.php", FriendsActivity.myId,friendId);

                Log.d("tsts", String.valueOf(accept));

                if(accept){

                    //notifyDataSetChanged();
                }

                /*
                Intent intent = new Intent(context, ProductlistActivity.class);
                intent.putExtra("category", myViewHolder.item.getText());
                context.startActivity(intent);
                */
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendslist.size();
    }

    //친구 수락
    class acceptFriends extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            //Log.d(TAG, "POST response  - " + result);

            if(result.equals("true")){
                Toast.makeText(context, "친구 수락 성공", Toast.LENGTH_SHORT).show();
                //accept = true;

                friendslist.remove(pos);
                notifyItemRemoved(pos);

            }
            else if(result.equals("false")){
                Toast.makeText(context, "친구 수락 실패", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String myId = (String)params[1];
            String friendId = (String)params[2];

            String serverURL = (String)params[0];
            String postParameters = "myId=" + myId + "&friendId=" + friendId;


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                //Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString();

            } catch (Exception e) {

                //Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }
}
