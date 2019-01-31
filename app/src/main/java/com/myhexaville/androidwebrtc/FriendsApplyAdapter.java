package com.myhexaville.androidwebrtc;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class FriendsApplyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class friendsApplyViewHolder extends RecyclerView.ViewHolder {

        TextView friendId;
        public final View mView;

        friendsApplyViewHolder(View view) {
            super(view);
            mView = view;

            friendId = view.findViewById(R.id.friendId_apply);
        }

    }

    private ArrayList<String> friendslist;

    FriendsApplyAdapter(ArrayList<String> friendslist){
        this.friendslist = friendslist;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_friends_apply_item, parent, false);

        return new friendsApplyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final friendsApplyViewHolder myViewHolder = (friendsApplyViewHolder) holder;

        myViewHolder.friendId.setText(friendslist.get(position) + "   수락 중...");

    }

    @Override
    public int getItemCount() {
        return friendslist.size();
    }
}
