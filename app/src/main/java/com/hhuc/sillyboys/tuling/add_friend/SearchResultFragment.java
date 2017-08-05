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

public class SearchResultFragment extends Fragment {
    private static final String TAG = "search_result";

    private ImageView imageView;
    private TextView textView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_target_list, container, false);
        Log.d(TAG, "onCreatView");
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        init();
    }

    private void init(){
        imageView = (ImageView)getActivity().findViewById(R.id.target_list_image);
        Glide.with(getActivity())
                .load(R.drawable.friend_jinge)
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(new BitmapImageViewTarget(imageView){
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circleImage =
                                RoundedBitmapDrawableFactory.create(getActivity().getResources(),resource);
                        circleImage.setCircular(true);
                        imageView.setImageDrawable(circleImage);
                    }
                });
        textView = (TextView)getActivity().findViewById(R.id.target_list_add);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "已发送申请", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
