package com.example.mediaplayproject.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.SharedPreferencesUtil;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.io.File;


/**
 * @author wm
 */
public class SettingsFragment extends Fragment {

    @SuppressLint("StaticFieldLeak")
    private static SettingsFragment mSettingsFragment;
    private Context mContext;
    private View mView;
    private ImageView ivBack;
    private LinearLayout llClearTitle, llClearView, llClearCapture, llClearLog;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch swLog, swLockScreen;
    private TextView tvLockScreenPermission;

    public SettingsFragment(Context context) {
        mContext = context;
    }

    public static SettingsFragment getInstance(Context context) {
        if (mSettingsFragment == null) {
            mSettingsFragment = new SettingsFragment(context);
        }
        return mSettingsFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_settings, container, false);
        initView();
        initData();
        return mView;
    }

    private void initView() {
        ivBack = mView.findViewById(R.id.iv_settings_back);
        swLog = mView.findViewById(R.id.sw_log);
        llClearTitle = mView.findViewById(R.id.ll_clear_storage);
        llClearView = mView.findViewById(R.id.ll_clear_view);
        llClearCapture = mView.findViewById(R.id.ll_clear_capture);
        llClearLog = mView.findViewById(R.id.ll_clear_log);

        swLockScreen = mView.findViewById(R.id.sw_lock_screen);
        tvLockScreenPermission = mView.findViewById(R.id.tv_lock_screen_permission);

        ivBack.setOnClickListener(mListener);
        llClearTitle.setOnClickListener(mListener);
        llClearCapture.setOnClickListener(mListener);
        llClearLog.setOnClickListener(mListener);
        swLog.setOnCheckedChangeListener(mCheckedChangeListener);
        swLockScreen.setOnCheckedChangeListener(mCheckedChangeListener);
        tvLockScreenPermission.setOnClickListener(mListener);
    }

    private void initData() {
        // 打印控制逻辑
        boolean isOpenLog = (Boolean) SharedPreferencesUtil.getData(Constant.LOG_SWITCH, true);
        DebugLog.debug("isOpenLog " + isOpenLog);
        swLog.setChecked(isOpenLog);

        // 锁屏开关
        boolean lockScreenSwitch = (Boolean) SharedPreferencesUtil.getData(Constant.LOCK_SCREEN_SWITCH, false);
        DebugLog.debug("lockScreenSwitch " + lockScreenSwitch);
        swLockScreen.setChecked(lockScreenSwitch);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivBack) {
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        } else if (view == llClearTitle) {
            llClearView.setVisibility(llClearView.getVisibility() == View.VISIBLE ? View.GONE:View.VISIBLE);
        } else if (view == llClearCapture) {
            showClearDialog(1);
        } else if (view == llClearLog) {
            showClearDialog(2);
        } else if (view == tvLockScreenPermission){
            // 启动锁屏权限申请界面

        }
    };

    private final CompoundButton.OnCheckedChangeListener mCheckedChangeListener = (buttonView, isChecked) -> {
        if (buttonView == swLog && buttonView.isPressed()) {
            boolean result = SharedPreferencesUtil.putData(Constant.LOG_SWITCH, isChecked);
            DebugLog.debug("set log switch " + isChecked + "; result: " + result);
            if (result) {
                Toast.makeText(mContext, "设置成功，重启生效！", Toast.LENGTH_LONG).show();
            }
        } else if (buttonView == swLockScreen && buttonView.isPressed()){
            boolean result = SharedPreferencesUtil.putData(Constant.LOCK_SCREEN_SWITCH, isChecked);
            DebugLog.debug("set lockScreen switch " + isChecked + "; result: " + result);
            if (result) {
                Toast.makeText(mContext, "设置成功！", Toast.LENGTH_LONG).show();
            }
        }
    };

    /**
     * 启动原生文件管理器
     *
     * @author wm
     * @createTime 2023/12/15 9:59
     */
    private void startFileManager() {
        ComponentName componentName = new ComponentName("com.google.android.documentsui",
                "com.android.documentsui.files.FilesActivity");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(componentName);
        startActivity(intent);
    }

    /**
     * 启动特定的文件夹（暂不起作用）
     *
     * @author wm
     * @createTime 2023/12/15 10:30
     */
    @SuppressLint("IntentReset")
    private void openSpecificFolder() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {
            //创建文件夹
            File dir = new File(filePath, "MediaPlay");
            if (!dir.exists()) {
                dir.mkdir();
            }
            // 使用 "file://" 方案打开文件夹
            Uri uri = Uri.parse("file://" + dir.getAbsolutePath());
            intent.setData(uri);
            // 设置 MIME 类型为目录
            intent.setType("resource/folder");

            // 启动文件管理器
            startActivity(intent);
        } catch (Exception exception) {
            DebugLog.error(exception.getMessage());
        }


    }

    /**
     * 删除相关文件夹
     *
     * @param fileType: 1--删除截图文件夹； 2--删除Log文件夹
     * @return true-操作成功；false-操作失败
     * @author wm
     * @createTime 2023/12/15 10:31
     */
    private boolean clearStore(int fileType) {
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {
            //创建文件夹
            File dir = new File(filePath, "MediaPlay");
            if (!dir.exists()) {
                DebugLog.debug("-MediaPlay no exists");
                return false;
            }
            if (fileType == 1) {
                // clear capture
                File dir2 = new File(dir, "Capture");
                if (!dir2.exists()) {
                    DebugLog.debug("-MediaPlay/Capture no exists");
                    return false;
                }
                boolean result = deleteDirectory(dir2.getPath());
                DebugLog.debug("-delete-MediaPlay/Capture " + result);
                return result;

            } else if (fileType == 2) {
                // clear log
                File dir2 = new File(dir, "Log");
                if (!dir2.exists()) {
                    DebugLog.debug("-MediaPlay/Log no exists");
                    return false;
                }
                boolean result = deleteDirectory(dir2.getPath());
                DebugLog.debug("-delete-MediaPlay/Log " + result);
                return result;
            } else {
                // other
            }

        } catch (Exception exception) {
            DebugLog.error(exception.getMessage());
        }
        return false;

    }


    /**
     * 删除单个文件
     *
     * @param filePath 被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除文件夹以及目录下的文件
     *
     * @param filePath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public boolean deleteDirectory(String filePath) {
        // 如果filePath不以文件分隔符结尾，自动添加文件分隔符：UNIX系统：“/”；Windows系统：“\\”
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        File[] files = dirFile.listFiles();
        if (files == null || files.length <= 0) {
            return false;
        }
        // 遍历删除文件夹下的所有文件(包括子目录)
        for (File file : files) {
            if (file.isFile()) {
                // 删除子文件
                flag = deleteFile(file.getAbsolutePath());
            } else {
                // 删除子目录
                flag = deleteDirectory(file.getAbsolutePath());
            }
            if (!flag) {
                break;
            }
        }
        if (flag) {
            // 删除当前空目录
            return dirFile.delete();
        }
        return false;
    }

    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     *
     * @param filePath 要删除的目录或文件
     * @return 删除成功返回 true，否则返回 false。
     */
    public boolean deleteFolder(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                // 为文件时调用删除文件方法
                return deleteFile(filePath);
            } else {
                // 为目录时调用删除目录方法
                return deleteDirectory(filePath);
            }
        }
    }

    /**
     *  清除缓存的Dialog
     *  @author wm
     *  @createTime 2023/12/18 15:57
     * @param clearType: 1--清除截图； 2--清除打印
     */
    private void showClearDialog(int clearType) {
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String capturePath = filePath + "/MediaPlay/Capture";
        String logPath = filePath + "/MediaPlay/Log";
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        String title = "";
        if (clearType == 1){
            builder.setTitle("Clear Capture");
            builder.setMessage("Delete all files in " + capturePath + " ? ");
        } else if (clearType == 2){
            builder.setTitle("Clear Log");
            builder.setMessage("Delete all files in " + logPath + " ? ");
        } else {
            return;
        }

        builder.setIcon(R.mipmap.ic_delete_select_pre);
        // 点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        // 设置正面按钮
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            try {
                clearStore(clearType);
                Toast.makeText(mContext, R.string.delete_success,Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception exception){
                Toast.makeText(mContext, R.string.delete_failed,Toast.LENGTH_SHORT).show();
                DebugLog.debug("error " + exception.getMessage());
            }
        });

        // 设置反面按钮
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        // 创建AlertDialog对象
        AlertDialog dialog = builder.create();
        // 显示对话框
        dialog.show();
    }
}