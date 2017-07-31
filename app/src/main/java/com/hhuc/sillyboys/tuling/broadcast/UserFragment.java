package com.hhuc.sillyboys.tuling.broadcast;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hhuc.sillyboys.tuling.R;

public class UserFragment extends Fragment {
    private static final String TAG = "userFragment";
    private int selfId = 0;
    private static SharedPreferences pref;

    private ImageView talkButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_broadcast_user, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        selfId = pref.getInt("selfid", 0);
        initComponent();

    }

    private void initComponent(){
        talkButton = (ImageView)getActivity().findViewById(R.id.broadcast_user_talk);
        talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "按住说话");
            }
        });
    }

}
