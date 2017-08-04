package com.hhuc.sillyboys.tuling.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hhuc.sillyboys.tuling.R;

import java.util.List;

public class MyMessageAdapter extends RecyclerView.Adapter<MyMessageAdapter.MyViewHolder>{

    private LayoutInflater mInflater;
    private Context mContext;
    protected List<String> mUser, mTime, mContent;
    protected List<String> mpicture;

    // 提供接口，用于实现item的点击事件
    public interface OnItemClickListener{
        void onItemClick(View view, int position);
        void onItemLongClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.mOnItemClickListener = listener;
    }

    public MyMessageAdapter(Context context, List<String> subject,List<String> pics,List<String> times,List<String> contents){
        this.mContext = context;
        this.mUser = subject;
        this.mpicture = pics;
        this.mTime = times;
        this.mContent = contents;
        mInflater = LayoutInflater.from(context);
    }


    public void setAllData(List<String> msubject,List<String> mpicture,List<String> times,List<String> contents) {
        this.mUser = msubject;
        this.mpicture = mpicture;
        this.mTime = times;
        this.mContent = contents;
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return mUser.size();
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.user.setText(mUser.get(position));
        holder.time.setText(mTime.get(position));
        holder.content.setText(mContent.get(position));
        Glide.with(mContext)
                .load(mpicture.get(position))
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .centerCrop()
                .into(holder.imageView);
        setUpItemEvent(holder);
    }

    // 设置为protected，供其子类使用
    protected void setUpItemEvent(final MyViewHolder holder) {
        // 设置点击事件
        if(mOnItemClickListener != null){
            // click
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int layoutPosition = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView,layoutPosition);
                }
            });
            // longClick
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int layoutPosition = holder.getLayoutPosition();
                    mOnItemClickListener.onItemLongClick(holder.itemView,layoutPosition);
                    return false;
                }
            });
        }
    }

    // 创建ViewHolder
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.navi_mymessage_single,parent,false);
        MyViewHolder viewHolder = new MyViewHolder(view);

        return viewHolder;
    }


    // 内部类
    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView user,time,content;
        ImageView imageView;
        public MyViewHolder(View arg0){
            super(arg0);
            user = (TextView)arg0.findViewById(R.id.message_fromuser);
            time = (TextView)arg0.findViewById(R.id.message_time);
            content = (TextView)arg0.findViewById(R.id.message_content);
            imageView = (ImageView)arg0.findViewById(R.id.message_img);
        }
    }

}


