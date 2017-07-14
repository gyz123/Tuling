package com.hhuc.sillyboys.tuling.navi_fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bartoszlipinski.recyclerviewheader.RecyclerViewHeader;
import com.dinuscxj.itemdecoration.ShaderItemDecoration;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.adapter.RoundImgAdapter;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirstFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private RoundImgAdapter mAdapter;
    private List<String> subject,description,pictures;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_first, container, false);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.first_recyclerView);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView toolbarText = (TextView)getActivity().findViewById(R.id.toolbar_text);
        toolbarText.setText("校园广播");
        initDatas();

        // 设置布局
        mAdapter = new RoundImgAdapter(getActivity(),subject,description,pictures);
        mRecyclerView.setAdapter(mAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView.getContext(),LinearLayoutManager.VERTICAL,false);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // 设置Item之间的分割线
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL_LIST));
        ShaderItemDecoration shaderItemDecoration = new ShaderItemDecoration(getActivity(),
                ShaderItemDecoration.SHADER_BOTTOM | ShaderItemDecoration.SHADER_TOP);
        shaderItemDecoration.setShaderTopDistance(1);
        shaderItemDecoration.setShaderBottomDistance(1);
        mRecyclerView.addItemDecoration(shaderItemDecoration);

        // 设置增加删除的动画效果
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // 接口回调，设置点击事件
        mAdapter.setOnItemClickListener(new RoundImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getActivity(),  "click : " + position , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemLongClick(View view, int position) {
                Toast.makeText(getActivity(),  "long click : " + position , Toast.LENGTH_SHORT).show();
            }
        });

        // 加载顶部搜索按钮
        RecyclerViewHeader header = RecyclerViewHeader.fromXml(getActivity(),R.layout.navi_header);
        header.attachTo(mRecyclerView);
        Button searchInfo = (Button)getActivity().findViewById(R.id.search_info);
        searchInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "搜索", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 测试数据
    private void initDatas(){
        subject = new ArrayList<String>(Arrays.asList("歪歪☆纳绘昕","滋油饼","qxk",
                "天才帅帅","神眷之樱花","李佳栋","夏雨"));

        description = new ArrayList<String>(Arrays.asList("真实姓名：耿元哲","真实姓名：马哲燚","真实姓名：屈肖柯",
                "真实姓名：陶宇","真实姓名：丁翰雯","真实姓名：李佳栋",
                "真实姓名：陈明华"));

        pictures = new ArrayList<String>(
                Arrays.asList("http://wx.qlogo.cn/mmopen/DZtibRDXICYabayGEnDE945eS02pbcBP53kI6LjyLODJqt59NpHVdXf1MHU1CwzRKNXcXt3cEdshTHTEIXsibNh4dVuIMyGfM5/0",
                        "http://wx.qlogo.cn/mmopen/6klL4b65U1MPibPtbQ0N4nPLtPSa45uA501oSBwM34Obvl104c4AONMNVDrmAg8kbpQqjwaT5qic1AN1bNyH63tL8jAZx52bnI/0",
                        "http://wx.qlogo.cn/mmopen/yFGN6Nl8WAzDiaMENnsGRHayiby5jCvPuK08ibF1LBzAku1tQ4icliceNraFL2iaILzcPWBibDHTYFkkkyH8woMjca9XEiaYtUtYm4hia/0",
                        "http://wx.qlogo.cn/mmopen/2sTJHesZN78BxgFpicXd7bf11I2d2OvIDg2Oia20B0lrC9oFERib8chVicFj9LOmEXTicbYD8VvjoAIwyRbWnpTUNs7s2t5fqHk4h/0",
                        "http://wx.qlogo.cn/mmopen/DZtibRDXICYYpbf4ZJZlWKL1RdZEm4RzV0eD72xHBnA9iadXWI2H3FHFBXR82NopvpXCzXiamld6UeEsbedZRzuAN354ibDCUPvz/0",
                        "http://wx.qlogo.cn/mmopen/6klL4b65U1MPibPtbQ0N4nPwj5u6Q2cicibb1FnXcNTm2yEuWj6icy8wb4OzL4xbXSyA6NSop5g8StTqwaqtmwxYHGeGrVPZNXf6/0",
                        "http://wx.qlogo.cn/mmopen/2sTJHesZN78BxgFpicXd7beeghbibiamLumTmpOdt9qXevDvy43sKoBewzUE72acNKD37iaibcIgYuxy7REYiaUm54x99w7IhxBA1y/0"
                ));
    }

}