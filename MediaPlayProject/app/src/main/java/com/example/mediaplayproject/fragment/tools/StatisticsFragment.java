package com.example.mediaplayproject.fragment.tools;

import static com.example.mediaplayproject.base.BasicApplication.getApplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.tools.StatisticsEditAdapter;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;
import com.example.mediaplayproject.view.StatisticsFloatView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description: java类作用描述
 * @author: wm
 */
public class StatisticsFragment extends Fragment implements StatisticsEditAdapter.StatisticsEditCallBack {

    private View myView;
    private Context mContext;
    private TextView tvPlayTotal, tvArtistTotal, tvPlayFromArtist, tvPlayTime;
    private ImageView ivBack, ivMore;
    private List<Map.Entry<String, Integer>> playTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> artistTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> mostPlayFromArtistTotalList = new ArrayList<>();
    private String musicName, artistName, artistMusic;
    private int musicPlayCount, artistPlayCount, artistMusicCount;

    private StatisticsFloatView mEditFloatLayout, mShareFloatLayout;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private RecyclerView rvStatisticsItem;
    private LinearLayout llCustomerEditInput;
    private EditText etText, etTime;
    private Button btnOk, btnCancel;
    private StatisticsEditAdapter mEditAdapter;
    private ArrayList<String> statisticsItemList;
    private CaptureCallback mCaptureCallback;

    private ImageView ivCapture;
    private Button btnSaveCapture, btnShareCapture, btnCancelCapture;
    private Bitmap mBitmap;

    /**
     * 用于记录编辑页面中选中的状态，并非全局使用的下标
     */
    private int statisticsTextSub = 0;

    public StatisticsFragment() {
    }

    public StatisticsFragment(Context context) {
        mContext = context;
    }

    @SuppressLint("StaticFieldLeak")
    private static StatisticsFragment instance;

    public static StatisticsFragment getInstance(Context context) {
        if (instance == null) {
            instance = new StatisticsFragment(context);
        }
        return instance;
    }

    public void setCaptureCallback(CaptureCallback captureCallback) {
        mCaptureCallback = captureCallback;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_statistics, container, false);
        initView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createFloatView();
        }
        return myView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDataFromService();
    }

    private void initView() {
        ivBack = myView.findViewById(R.id.iv_statistics_back);
        ivMore = myView.findViewById(R.id.iv_statistics_more);
        tvPlayTotal = myView.findViewById(R.id.tv_play_total);
        tvArtistTotal = myView.findViewById(R.id.tv_artist_total);
        tvPlayFromArtist = myView.findViewById(R.id.tv_play_from_artist_total);
        tvPlayTime = myView.findViewById(R.id.tv_play_time);

        ivBack.setOnClickListener(mListener);
        ivMore.setOnClickListener(mListener);
        registerForContextMenu(ivMore);

        statisticsItemList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.statistics_title_item)));
    }


    /**
     * 刷新统计信息
     *
     * @author wm
     * @createTime 2023/11/18 9:02
     */
    private void refreshDataFromService() {
        int flag = 0;
        playTotalList = DataRefreshService.getPlayTotalList();
        artistTotalList = DataRefreshService.getArtistTotalList();

        musicName = playTotalList.get(0).getKey();
        musicPlayCount = playTotalList.get(0).getValue();
        for (int i = 0; i < artistTotalList.size(); i++) {
            if ("<unknown>".equals(artistTotalList.get(i).getKey())) {
                continue;
            }
            artistName = artistTotalList.get(i).getKey();
            artistPlayCount = artistTotalList.get(i).getValue();
            flag = i;
            break;
        }

        mostPlayFromArtistTotalList = DataRefreshService.getMostPlayFromArtistTotalList(artistTotalList.get(flag));
        if (mostPlayFromArtistTotalList != null && mostPlayFromArtistTotalList.size() > 0) {
            artistMusic = mostPlayFromArtistTotalList.get(0).getKey();
            artistMusicCount = mostPlayFromArtistTotalList.get(0).getValue();
        }

        statisticsTextSub = (Integer) SharedPreferencesUtil.getData(Constant.STATISTICS_SUB, 0);
        // 使用Handler更新UI
        toolsViewHandler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_REFRESH_STATISTICS_DATA, 300);
    }

    /**
     * 创建悬浮窗
     *
     * @author wm
     * @createTime 2023/11/18 9:07
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        // 获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // 设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        // 设置背景为透明，否则滑动ListView会出现残影
        wmParams.format = PixelFormat.TRANSPARENT;
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口,不设置这个flag的话，home页的划屏会有问题
        // FLAG_WATCH_OUTSIDE_TOUCH : 使窗口可以接受窗口之外的触摸事件
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        // 调整悬浮窗显示的停靠位置为居中顶部
        wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;

        // 设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        initEditFloatView();
        initShareFloatView();
    }

    /**
     * 初始化Edit界面悬浮窗
     *
     * @author wm
     * @createTime 2023/11/18 9:06
     */
    private void initEditFloatView() {
        // 获取编辑页面浮动窗口视图所在布局
        mEditFloatLayout = new StatisticsFloatView(mContext, R.layout.statistics_edit_view);
        mEditFloatLayout.setFloatViewCallback(() -> {
            if (mWindowManager != null && mEditFloatLayout.isAttachedToWindow()) {
                mWindowManager.removeView(mEditFloatLayout);
            }
        });
        DebugLog.debug("list " + statisticsItemList.get(0));
        mEditAdapter = new StatisticsEditAdapter(mContext, statisticsItemList);
        mEditAdapter.setEditCallBack(this);
        rvStatisticsItem = mEditFloatLayout.findViewById(R.id.rv_statistics_edit);
        llCustomerEditInput = mEditFloatLayout.findViewById(R.id.ll_statistics_input);
        etText = mEditFloatLayout.findViewById(R.id.et_statistics_text);
        etTime = mEditFloatLayout.findViewById(R.id.et_statistics_time);
        btnOk = mEditFloatLayout.findViewById(R.id.btn_statistics_ok);
        btnCancel = mEditFloatLayout.findViewById(R.id.btn_statistics_cancel);
        btnOk.setOnClickListener(mListener);
        btnCancel.setOnClickListener(mListener);
        rvStatisticsItem.setAdapter(mEditAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        rvStatisticsItem.setLayoutManager(linearLayoutManager);
    }

    /**
     * 初始化分享页面的UI悬浮窗
     *
     * @author wm
     * @createTime 2023/11/24 14:30
     */
    private void initShareFloatView() {
        mShareFloatLayout = new StatisticsFloatView(mContext, R.layout.statistics_share_view);
        mShareFloatLayout.setFloatViewCallback(() -> {
            if (mWindowManager != null && mShareFloatLayout.isAttachedToWindow()) {
                mWindowManager.removeView(mShareFloatLayout);
            }
        });
        ivCapture = mShareFloatLayout.findViewById(R.id.iv_capture);
        btnSaveCapture = mShareFloatLayout.findViewById(R.id.btn_save_capture);
        btnShareCapture = mShareFloatLayout.findViewById(R.id.btn_share_capture);
        btnCancelCapture = mShareFloatLayout.findViewById(R.id.btn_cancel_capture);
        btnSaveCapture.setOnClickListener(mListener);
        btnShareCapture.setOnClickListener(mListener);
        btnCancelCapture.setOnClickListener(mListener);
    }

    final Handler toolsViewHandler = new Handler(Looper.myLooper()) {
        @SuppressLint({"ResourceAsColor", "SetTextI18n"})
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_REFRESH_STATISTICS_DATA) {
                tvPlayTotal.setText(mContext.getString(R.string.favorite_music, musicName, musicPlayCount));
                tvArtistTotal.setText(mContext.getString(R.string.favorite_artist, artistName, artistPlayCount));
                tvPlayFromArtist.setText(mContext.getString(R.string.favorite_artist_and_music, artistName, artistMusic, artistMusicCount));

                double totalTime = (double) DataRefreshService.getTotalPlayTime() / 3600;
                switch (statisticsTextSub) {
                    case 0:
                        tvPlayTime.setText(mContext.getString(R.string.immersive_meditation, totalTime));
                        break;
                    case 1:
                        tvPlayTime.setText(mContext.getString(R.string.time_traveler, totalTime));
                        break;
                    case 2:
                        tvPlayTime.setText(mContext.getString(R.string.music_marathon, totalTime));
                        break;
                    case 3:
                        tvPlayTime.setText(mContext.getString(R.string.dream_explorer, totalTime));
                        break;
                    case 4:
                        tvPlayTime.setText(mContext.getString(R.string.musical_genius, totalTime));
                        break;
                    case 5:
                        String text = (String) SharedPreferencesUtil.getData(Constant.STATISTICS_TEXT, "");
                        int time = (int) SharedPreferencesUtil.getData(Constant.STATISTICS_TIME, 0);
                        if ("".equals(text) || time == 0) {
                            long count = DataRefreshService.getTotalPlayTime() / 240L;
                            tvPlayTime.setText(mContext.getString(R.string.default_statistics_text1,
                                    "" + DataRefreshService.getTotalPlayTime(), count));
                        } else {
                            long count = DataRefreshService.getTotalPlayTime() / (time * 60L);
                            tvPlayTime.setText(mContext.getString(R.string.default_statistics_text2,
                                    "" + DataRefreshService.getTotalPlayTime(), "" + count, text));
                        }
                        break;
                    default:
                        tvPlayTime.setText("");
                        break;
                }
            }
        }
    };

    private final View.OnClickListener mListener = view -> {
        if (view == ivBack) {
            // 返回主页
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        } else if (view == ivMore) {
            // 更多功能
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 绘制的位置，以当前ivMore的位置偏移
                ivMore.showContextMenu(-130, 150);
            }

        } else if (view == btnOk) {
            SharedPreferencesUtil.putData(Constant.STATISTICS_SUB, statisticsTextSub);
            refreshDataFromService();

            if (statisticsTextSub == statisticsItemList.size() - 1) {
                try {
                    String text = etText.getText().toString().trim();
                    int min = Integer.parseInt(etTime.getText().toString().trim());
                    DebugLog.debug("text/time " + text + "/" + min);
                    if ("".equals(text) || min < 0) {
                        Toast.makeText(mContext, "please input text and time(min)!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SharedPreferencesUtil.putData(Constant.STATISTICS_TEXT, text);
                    SharedPreferencesUtil.putData(Constant.STATISTICS_TIME, min);
                } catch (Exception e) {
                    Toast.makeText(mContext, "please input text and time(min)!", Toast.LENGTH_LONG).show();
                    DebugLog.error("error " + e.getMessage());
                    return;
                }

            }

            if (mWindowManager != null && mEditFloatLayout.isAttachedToWindow()) {
                mWindowManager.removeView(mEditFloatLayout);
            }
        } else if (view == btnCancel) {
            if (mWindowManager != null && mEditFloatLayout.isAttachedToWindow()) {
                mWindowManager.removeView(mEditFloatLayout);
            }
        } else if (view == btnSaveCapture) {
            DebugLog.debug("save capture");
            if (mBitmap != null) {
                saveCapture(mBitmap);
                if (mWindowManager != null && mShareFloatLayout.isAttachedToWindow()) {
                    mWindowManager.removeView(mShareFloatLayout);
                }
            }
        } else if (view == btnShareCapture) {
            DebugLog.debug("share capture");
//            boolean isOpenLog = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_SWITCH,true);
//            DebugLog.debug("isOpenLog " + isOpenLog);
//            boolean result = SharedPreferencesUtil.putData(Constant.LOG_SWITCH, !isOpenLog);

            boolean writeToFile = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_WRITE,true);
            DebugLog.debug("isOpenLog " + writeToFile);
            boolean result = SharedPreferencesUtil.putData(Constant.LOG_WRITE, !writeToFile);
        } else if (view == btnCancelCapture) {
            DebugLog.debug("cancel save capture");
            if (mWindowManager != null && mShareFloatLayout.isAttachedToWindow()) {
                mWindowManager.removeView(mShareFloatLayout);
            }
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add("Edit").setOnMenuItemClickListener(item -> {

            mEditAdapter.setCheckItemSub(statisticsTextSub);
            if (statisticsTextSub == statisticsItemList.size() - 1) {
                int sub = (Integer) SharedPreferencesUtil.getData(Constant.STATISTICS_SUB, 0);
                if (sub == statisticsTextSub) {
                    etText.setText((String) SharedPreferencesUtil.getData(Constant.STATISTICS_TEXT, ""));
                    etTime.setText("" + (Integer) SharedPreferencesUtil.getData(Constant.STATISTICS_TIME, 0));
                }
                llCustomerEditInput.setVisibility(View.VISIBLE);
            } else {
                llCustomerEditInput.setVisibility(View.GONE);
            }

            wmParams.x = 0;
            wmParams.y = 300;
            mWindowManager.addView(mEditFloatLayout, wmParams);
            return false;
        });
        menu.add("Share").setOnMenuItemClickListener(item -> {
            DebugLog.debug("share");
            if (mCaptureCallback != null) {
                mCaptureCallback.startCapture();
            }
            return false;
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClickItem(int position) {
        DebugLog.debug("statistics check " + position
                + " : " + statisticsItemList.get(position));
        statisticsTextSub = position;
        if (position == statisticsItemList.size() - 1) {
            int sub = (Integer) SharedPreferencesUtil.getData(Constant.STATISTICS_SUB, 0);
            if (sub == statisticsTextSub) {
                etText.setText((String) SharedPreferencesUtil.getData(Constant.STATISTICS_TEXT, ""));
                etTime.setText("" + (Integer) SharedPreferencesUtil.getData(Constant.STATISTICS_TIME, 0));
            }
            llCustomerEditInput.setVisibility(View.VISIBLE);
        } else {
            llCustomerEditInput.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWindowManager != null && mEditFloatLayout.isAttachedToWindow()) {
            mWindowManager.removeView(mEditFloatLayout);
        }
        if (mWindowManager != null && mShareFloatLayout.isAttachedToWindow()) {
            mWindowManager.removeView(mShareFloatLayout);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public interface CaptureCallback {

        /**
         * 截图回调接口
         *
         * @author wm
         * @createTime 2023/11/24 14:37
         */
        void startCapture();
    }

    /**
     * Activity截图完成后调用，设置bitmap，启动分享页面
     *
     * @param bitmap:
     * @author wm
     * @createTime 2023/12/1 16:41
     */
    public void setCaptureImage(Bitmap bitmap) {
        DebugLog.debug("bitmap " + bitmap);
        if (ivCapture != null && bitmap != null) {
            ivCapture.setImageBitmap(bitmap);
            mBitmap = bitmap;
        }
        DebugLog.debug("width " + ivCapture.getWidth() + "; height " + ivCapture.getHeight());
        wmParams.x = 0;
        wmParams.y = 0;
        mWindowManager.addView(mShareFloatLayout, wmParams);

    }

    /**
     * 保存截图到本地，路径为/storage/emulated/0/MediaPlay/Capture/
     *
     * @param bitmap: 传进来的截图
     * @author wm
     * @createTime 2023/12/1 16:40
     */
    private void saveCapture(Bitmap bitmap) {
        DebugLog.debug("");
        // 保存到的文件路径
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {
            //创建文件夹
            File dir = new File(filePath, "MediaPlay");
            if (!dir.exists()) {
                dir.mkdir();
            }
            //创建文件夹
            File dir2 = new File(dir, "Capture");
            if (!dir2.exists()) {
                dir2.mkdir();
            }
            // 创建文件
            File file = new File(dir2, "mediaPlay_" + ToolsUtils.getCurrentTime2() + ".png");
            if (!file.exists()) {
                file.createNewFile();
            }
            DebugLog.debug("path " + file);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            // compress-压缩  将bitmap保存到本地文件中
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, fileOutputStream);
            // 存储完成后需要清除相关的进程
            fileOutputStream.flush();
            fileOutputStream.close();
            // 保存图片后发送广播通知更新数据库
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(uri);
            mContext.sendBroadcast(intent);
            Toast.makeText(mContext, file.getPath() + "保存成功", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对View控件里面的内容进行截图(这里说是可以对任意View截图，不清楚是否有用)
     *
     * @param view :
     */
    private Bitmap testViewSnapshot(View view) {
        //使控件可以进行缓存
        view.setDrawingCacheEnabled(true);
        //获取缓存的 Bitmap
        Bitmap drawingCache = view.getDrawingCache();
        //复制获取的 Bitmap
        drawingCache = Bitmap.createBitmap(drawingCache);
        //关闭视图的缓存
        view.setDrawingCacheEnabled(false);

        return drawingCache;
    }

}