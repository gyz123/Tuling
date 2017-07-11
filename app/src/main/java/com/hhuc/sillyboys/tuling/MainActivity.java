package com.hhuc.sillyboys.tuling;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.hhuc.sillyboys.tuling.navi_fragment.FirstFragment;
import com.hhuc.sillyboys.tuling.navi_fragment.FourthFragment;
import com.hhuc.sillyboys.tuling.navi_fragment.SecondFragment;
import com.hhuc.sillyboys.tuling.navi_fragment.ThirdFragment;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;
import com.hhuc.sillyboys.tuling.zxing.activity.CaptureActivity;
import com.luseen.luseenbottomnavigation.BottomNavigation.BottomNavigationItem;
import com.luseen.luseenbottomnavigation.BottomNavigation.BottomNavigationView;
import com.luseen.luseenbottomnavigation.BottomNavigation.OnBottomNavigationItemClickListener;
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.MenuParams;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

    private BottomNavigationView bottomNavigationView;
    private android.support.v4.app.FragmentManager manager;
    private ContextMenuDialogFragment mMenuDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        // 工具栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        // 菜单栏
        manager = getSupportFragmentManager();
        setContextMenu();
        // 导航栏
        setBottomNavi();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 菜单选项点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // 点击"+"后的操作（显示下拉菜单）
            case R.id.context_menu:
                if (manager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                    mMenuDialogFragment.show(manager, ContextMenuDialogFragment.TAG);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    // 加载菜单栏1
    private void setContextMenu() {
        MenuParams menuParams = new MenuParams();
        menuParams.setAnimationDelay(200);  // 菜单显示延迟时间
        menuParams.setAnimationDuration(20);  // 子项显示间隔时间
        menuParams.setActionBarSize((int) getResources().getDimension(R.dimen.tool_bar_height));  // 子项高度
        menuParams.setMenuObjects(getMenuObjects());
        menuParams.setClosableOutside(false);
        mMenuDialogFragment = ContextMenuDialogFragment.newInstance(menuParams);  // 创建下拉菜单
        mMenuDialogFragment.setItemClickListener(this);  // 点击事件
    }


    // 加载菜单栏2
    private List<MenuObject> getMenuObjects() {
        List<MenuObject> menuObjects = new ArrayList<>();

        MenuObject close = new MenuObject();
        close.setResource(R.drawable.icn_close);
        close.setDividerColor(R.color.sub_subject_color);

        // 微信端推送消息
        MenuObject send = new MenuObject("每日推送");
        send.setResource(R.drawable.icn_1);

        // 微信端推荐好书
        MenuObject like = new MenuObject("好书推荐");
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.icn_2);
        like.setBitmap(b);
        like.setDividerColor(R.color.sub_subject_color);

        // 添加新用户
        MenuObject addFr = new MenuObject("添加用户");
        BitmapDrawable bd = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.icn_3));
        addFr.setDrawable(bd);

        // 添加新书籍
        MenuObject addFav = new MenuObject("添加书籍");
        addFav.setResource(R.drawable.icn_4);
        addFav.setDividerColor(R.color.sub_subject_color);

        // 扫一扫
        MenuObject block = new MenuObject("扫一扫");
        block.setResource(R.drawable.icn_5);

        menuObjects.add(close);
        menuObjects.add(send);
        menuObjects.add(like);
        menuObjects.add(addFr);
        menuObjects.add(addFav);
        menuObjects.add(block);
        return menuObjects;
    }


    // 菜单栏点击事件
    @Override
    public void onMenuItemClick(View clickedView, int position) {
        switch(position){
            case 1: Toast.makeText(this, "服务器将进行每日推送 ", Toast.LENGTH_SHORT).show();
                break;
            case 2: Toast.makeText(this, "服务器将进行好书推荐 ", Toast.LENGTH_SHORT).show();
                break;
            case 3: Toast.makeText(this, "Clicked on position: " + position, Toast.LENGTH_SHORT).show();
                break;
            case 4: Toast.makeText(this, "Clicked on position: " + position, Toast.LENGTH_SHORT).show();
                break;
            case 5: Log.d("test","开启扫码事件");
                Intent scanCodeIntent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(scanCodeIntent);
                break;
            default:
                break;
        }
    }


    // 导航栏
    private void setBottomNavi() {
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation);

        // 从xml文件中获取图标与颜色信息
        int[] image = {R.drawable.ic_mic_black_24dp, R.drawable.ic_favorite_black_24dp,
                R.drawable.ic_book_black_24dp, R.drawable.ic_github_circle};
        int[] color = {ContextCompat.getColor(this, R.color.firstColor), ContextCompat.getColor(this, R.color.secondColor),
                ContextCompat.getColor(this, R.color.thirdColor), ContextCompat.getColor(this, R.color.fourthColor)};

        if (bottomNavigationView != null) {
            // 设置显示提示文字
            bottomNavigationView.isWithText(true);
            // 设置使用背景颜色
            bottomNavigationView.isColoredBackground(false);
            // 设置动态字体大小
            bottomNavigationView.setTextActiveSize(getResources().getDimension(R.dimen.text_active));
            bottomNavigationView.setTextInactiveSize(getResources().getDimension(R.dimen.text_inactive));
            // 设置点击图标时，图标与文字的颜色变化 （需要未使用背景颜色才会生效）
            bottomNavigationView.setItemActiveColorWithoutColoredBackground(ContextCompat.getColor(this, R.color.firstColor));
            // 取消滑动效果
            //bottomNavigationView.disableViewPagerSlide();
            // 不新生成activity界面
            bottomNavigationView.willNotRecreate(true);
        }

        // 设置导航栏各组件信息
        BottomNavigationItem bottomNavigationItem = new BottomNavigationItem
                ("First", color[0], image[0]);
        BottomNavigationItem bottomNavigationItem1 = new BottomNavigationItem
                ("Second", color[1], image[1]);
        BottomNavigationItem bottomNavigationItem2 = new BottomNavigationItem
                ("Third", color[2], image[2]);
        BottomNavigationItem bottomNavigationItem3 = new BottomNavigationItem
                ("Fourth", color[3], image[3]);

        // 将组件添加到底部导航栏中
        bottomNavigationView.addTab(bottomNavigationItem);
        bottomNavigationView.addTab(bottomNavigationItem1);
        bottomNavigationView.addTab(bottomNavigationItem2);
        bottomNavigationView.addTab(bottomNavigationItem3);

        // 设置响应事件
        bottomNavigationView.setOnBottomNavigationItemClickListener(new OnBottomNavigationItemClickListener() {
            @Override
            public void onNavigationItemClick(int index) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                switch (index) {
                    case 0:
                        FirstFragment firstFragment = new FirstFragment();
                        transaction.replace(R.id.main_fragment,firstFragment);
                        transaction.commit();
                        break;
                    case 1:
                        SecondFragment secondFragment = new SecondFragment();
                        transaction.replace(R.id.main_fragment,secondFragment);
                        transaction.commit();
                        break;
                    case 2:
                        ThirdFragment thirdFragment = new ThirdFragment();
                        transaction.replace(R.id.main_fragment,thirdFragment);
                        transaction.commit();
                        break;
                    case 3:
                        FourthFragment fourthFragment = new FourthFragment();
                        transaction.replace(R.id.main_fragment,fourthFragment);
                        transaction.commit();
                        break;
                }
            }
        });

        setDefaultFragment();
    }


    // 初始化碎片
    private void setDefaultFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        FirstFragment firstFragment = new FirstFragment();
        transaction.replace(R.id.main_fragment,firstFragment);
        transaction.commit();
    }

}
