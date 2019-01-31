package com.myhexaville.androidwebrtc;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.myhexaville.androidwebrtc.app_rtc_sample.main.AppRTCMainActivity;

import java.util.ArrayList;

public class mainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    int pos;

    public static class mainViewHolder extends RecyclerView.ViewHolder {

        TextView friendId;
        ImageView call;
        public final View mView;

        mainViewHolder(View view) {
            super(view);
            mView = view;

            friendId = view.findViewById(R.id.friendId);
            call = view.findViewById(R.id.call);

        }

    }

    private ArrayList<String> friendslist;

    public mainAdapter(ArrayList<String> friendslist){
        this.friendslist = friendslist;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_mainitem, parent, false);

        return new mainViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final mainViewHolder myViewHolder = (mainViewHolder) holder;

        myViewHolder.friendId.setText(friendslist.get(position));
        myViewHolder.call.setTag(holder.getAdapterPosition());

        myViewHolder.call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context = v.getContext();
                pos = (int)v.getTag();

                //Toast.makeText(context, myViewHolder.friendId.getText().toString(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(context, AppRTCMainActivity.class);
                intent.putExtra("friendId", myViewHolder.friendId.getText().toString());
                intent.putExtra("caller", "true");
                context.startActivity(intent);

                //AppRTCMainActivity appRTCMainActivity = new AppRTCMainActivity();
                //appRTCMainActivity.connect();
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendslist.size();
    }
}
