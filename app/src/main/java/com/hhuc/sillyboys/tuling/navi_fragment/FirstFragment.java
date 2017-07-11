package com.hhuc.sillyboys.tuling.navi_fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.R;

public class FirstFragment extends Fragment {

    private Button button;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_first, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView toolbarText = (TextView)getActivity().findViewById(R.id.toolbar_text);
        toolbarText.setText("第一个");
    }
}