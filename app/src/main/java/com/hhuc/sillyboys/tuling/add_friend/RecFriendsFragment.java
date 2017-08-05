package com.hhuc.sillyboys.tuling.add_friend;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.hhuc.sillyboys.tuling.R;

public class RecFriendsFragment extends Fragment {
    private static final String TAG = "recommend_friends";

    private ImageView imageView1,imageView2,imageView3,imageView4;
    private TextView textView1,textView2,textView3,textView4;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_rec_friends, container, false);
        Log.d(TAG, "onCreatView");
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        setImgs();
        addListeners();
    }


    private void setImgs(){
        imageView1 = (ImageView) getActivity().findViewById(R.id.add_friend_tip1_img);
        Glide.with(getActivity())
                .load(R.drawable.friend_dashi)
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(new BitmapImageViewTarget(imageView1){
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circleImage =
                                RoundedBitmapDrawableFactory.create(getActivity().getResources(),resource);
                        circleImage.setCircular(true);
                        imageView1.setImageDrawable(circleImage);
                    }
                });
        imageView2 = (ImageView) getActivity().findViewById(R.id.add_friend_tip2_img);
        Glide.with(getActivity())
                .load(R.drawable.friend_hongge)
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(new BitmapImageViewTarget(imageView2){
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circleImage =
                                RoundedBitmapDrawableFactory.create(getActivity().getResources(),resource);
                        circleImage.setCircular(true);
                        imageView2.setImageDrawable(circleImage);
                    }
                });
        imageView3 = (ImageView) getActivity().findViewById(R.id.add_friend_tip3_img);
        Glide.with(getActivity())
                .load(R.drawable.friend_doudou)
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(new BitmapImageViewTarget(imageView3){
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circleImage =
                                RoundedBitmapDrawableFactory.create(getActivity().getResources(),resource);
                        circleImage.setCircular(true);
                        imageView3.setImageDrawable(circleImage);
                    }
                });
        imageView4 = (ImageView) getActivity().findViewById(R.id.add_friend_tip4_img);
        Glide.with(getActivity())
                .load(R.drawable.friend_xiaoke)
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(new BitmapImageViewTarget(imageView4){
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circleImage =
                                RoundedBitmapDrawableFactory.create(getActivity().getResources(),resource);
                        circleImage.setCircular(true);
                        imageView4.setImageDrawable(circleImage);
                    }
                });
    }


    private void addListeners(){
        textView1 = (TextView)getActivity().findViewById(R.id.add_friend_tip1_add);
        textView2 = (TextView)getActivity().findViewById(R.id.add_friend_tip2_add);
        textView3 = (TextView)getActivity().findViewById(R.id.add_friend_tip3_add);
        textView4 = (TextView)getActivity().findViewById(R.id.add_friend_tip4_add);
        textView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "已发送申请", Toast.LENGTH_SHORT).show();
            }
        });
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "已发送申请", Toast.LENGTH_SHORT).show();
            }
        });
        textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "已发送申请", Toast.LENGTH_SHORT).show();
            }
        });
        textView4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "已发送申请", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
