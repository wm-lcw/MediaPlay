package com.example.mediaplayproject.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author wm
 * @Classname BasicActivity
 * @Description 基础Activity
 * @Version 1.0.0
 * @Date 2023/2/9 11:08
 * @Created by wm
 */
public abstract class BasicActivity extends AppCompatActivity implements UiCallBack {
    /**
     * 快速点击的时间间隔
     */
    private static final int FAST_CLICK_DELAY_TIME = 500;
    /**
     * 最后点击的时间
     */
    private static long lastClickTime;
    /**
     * 上下文参数
     */
    protected Activity context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        // Activity布局加载前的处理
        initBeforeView(savedInstanceState);
        EventBus.getDefault().register(this);
        Intent dataRefreshService = new Intent(context, DataRefreshService.class);
        startService(dataRefreshService);
        // 添加继承这个BaseActivity的Activity
        BasicApplication.getActivityManager().addActivity(this);

        // 绑定布局id
        if (getLayoutId() > 0) {
            setContentView(getLayoutId());
        }
        // 初始化数据
        initBundleData(savedInstanceState);
    }

    /**
     *  初始化一些操作，该方法在setContentView之前被调用
     *  @author wm
     *  @createTime 2023/9/24 14:33
     * @param savedInstanceState:
     */
    @Override
    public void initBeforeView(Bundle savedInstanceState) {
        changeLanguage();
    }

    @Override
    public void initBundleData(Bundle savedInstanceState) {

    }

    /**
     * 返回
     * @param toolbar
     */
    protected void Back(Toolbar toolbar) {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.finish();
                if (!isFastClick()) {
                    context.finish();
                }
            }
        });
    }

    /**
     * 两次点击间隔不能少于500ms  防止多次点击
     * @return flag
     */
    protected static boolean isFastClick() {
        boolean flag = true;
        long currentClickTime = System.currentTimeMillis();
        if ((currentClickTime - lastClickTime) >= FAST_CLICK_DELAY_TIME) {
            flag = false;
        }
        lastClickTime = currentClickTime;

        return flag;
    }

    /**
     * 消息提示
     */
    protected void show(CharSequence charSequence) {
        Toast.makeText(context, charSequence, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DebugLog.debug("---");
        stopService(new Intent(context, DataRefreshService.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(String msg) {
        if (Constant.SWITCH_LANGUAGE.equals(msg)) {
            changeLanguage();
            recreate();//刷新界面
        }

    }

    /**
     *  切换语言
     *  @author wm
     *  @createTime 2023/9/24 21:12
     */
    public void changeLanguage() {
        try {
            String currentLanguage = (String) SharedPreferencesUtil.getData(Constant.CURRENT_LANGUAGE,"zh");
            String currentCountry = (String) SharedPreferencesUtil.getData(Constant.CURRENT_COUNTRY,"CN");
            ToolsUtils.getInstance().changeLanguage(context, currentLanguage, currentCountry);
            // 保存当前的语言
            SharedPreferencesUtil.putData(Constant.CURRENT_USE_LANGUAGE, currentLanguage);
        } catch (Exception exception) {
            DebugLog.debug(exception.getMessage());
        }

    }

}


