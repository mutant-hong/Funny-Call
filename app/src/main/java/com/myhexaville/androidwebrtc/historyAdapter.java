package com.myhexaville.androidwebrtc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class historyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    int pos;

    //ArrayList<Integer> checkIdx = new ArrayList<>();

    public static int[] checkIdx;

    public static class historyViewHolder extends RecyclerView.ViewHolder {

        TextView friendId;
        TextView date;
        ImageView info;
        ImageView sender;

        CheckBox checkBox;

        public final View mView;

        historyViewHolder(View view) {
            super(view);
            mView = view;

            friendId = view.findViewById(R.id.friendId);
            date = view.findViewById(R.id.date);
            info = view.findViewById(R.id.info);
            sender = view.findViewById(R.id.sender);

            checkBox = view.findViewById(R.id.checkbox);
        }

    }

    private ArrayList<history> historyList;

    public historyAdapter(ArrayList<history> historyList){
        this.historyList = historyList;

        checkIdx = new int[historyList.size()];
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_historyitem, parent, false);

        return new historyAdapter.historyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final historyAdapter.historyViewHolder myViewHolder = (historyAdapter.historyViewHolder) holder;

        //내가 발신자일 때
        if(historyList.get(position).caller.equals("true"))
            myViewHolder.sender.setImageResource(R.drawable.sender);
        else
            myViewHolder.sender.setImageResource(0);

        //상대방 이름
        //부재중일 때
        myViewHolder.friendId.setText(historyList.get(position).friendId);
        if(historyList.get(position).missied.equals("true"))
            myViewHolder.friendId.setTextColor(Color.RED);
        myViewHolder.friendId.setTag(holder.getAdapterPosition());

        //날짜
        myViewHolder.date.setText(historyList.get(position).date);

        myViewHolder.info.setTag(holder.getAdapterPosition());

        myViewHolder.checkBox.setTag(holder.getAdapterPosition());

        if(CallHistoryActivity.check){
            myViewHolder.checkBox.setVisibility(View.VISIBLE);

            if(CallHistoryActivity.all) {
                myViewHolder.checkBox.setChecked(true);
                checkIdx[position] = 1;
            }
            else{
                checkIdx[position] = 0;
            }
        }

        else
            myViewHolder.checkBox.setVisibility(View.GONE);

        myViewHolder.friendId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context = v.getContext();
                pos = (int)v.getTag();

                //Toast.makeText(context, myViewHolder.friendId.getText().toString(), Toast.LENGTH_SHORT).show();
/*
                Intent intent = new Intent(context, AppRTCMainActivity.class);
                intent.putExtra("friendId", myViewHolder.friendId.getText().toString());
                intent.putExtra("caller", "true");
                context.startActivity(intent);
*/
            }
        });

        //상세 보기 페이지
        myViewHolder.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                context = v.getContext();
                pos = (int)v.getTag();

//                Toast.makeText(context, myViewHolder.friendId.getText().toString() + " / "
//                        + myViewHolder.date.getText().toString() + " / "
//                        + historyList.get(pos).time + " / " + historyList.get(pos).caller, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(context, DetailHistoryActivity.class);
                intent.putExtra("friendId", myViewHolder.friendId.getText().toString());
                intent.putExtra("date", myViewHolder.date.getText().toString());
                intent.putExtra("time", historyList.get(pos).time);
                intent.putExtra("caller", historyList.get(pos).caller);
                intent.putExtra("missied", historyList.get(pos).missied);
                context.startActivity(intent);
            }
        });

        /*

        context = buttonView.getContext();
                pos = (int)buttonView.getTag();

                if(isChecked)
                    checkIdx[pos] = 1;
                else
                    checkIdx[pos] = 0;

         */
        myViewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                context = buttonView.getContext();
                pos = (int)buttonView.getTag();

                if(isChecked)
                    checkIdx[pos] = 1;
                else
                    checkIdx[pos] = 0;
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
}
