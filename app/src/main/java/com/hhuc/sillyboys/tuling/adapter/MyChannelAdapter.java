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

public class MyChannelAdapter extends RecyclerView.Adapter<MyChannelAdapter.MyViewHolder>{

    private LayoutInflater mInflater;
    private Context mContext;
    protected List<String> msubject;
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

    public MyChannelAdapter(Context context, List<String> subject,List<String> pics){
        this.mContext = context;
        this.msubject = subject;
        this.mpicture = pics;
        mInflater = LayoutInflater.from(context);
    }


    public void setAllData(List<String> msubject,List<String> mpicture) {
        this.msubject = msubject;
        this.mpicture = mpicture;
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return msubject.size();
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.tv1.setText(msubject.get(position));
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
        View view = mInflater.inflate(R.layout.navi_mychannel_single,parent,false);
        MyViewHolder viewHolder = new MyViewHolder(view);

        return viewHolder;
    }


    // 内部类
    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tv1;
        ImageView imageView;
        public MyViewHolder(View arg0){
            super(arg0);
            tv1 = (TextView)arg0.findViewById(R.id.mychannel_name);
            imageView = (ImageView)arg0.findViewById(R.id.mychannel_img);
        }
    }

}


