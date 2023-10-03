package com.example.mediaplayproject.fragment.tools;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.utils.ToolsUtils;


/**
 * @author wm
 */
public class AboutFragment extends Fragment {

    private Context mContext;
    private View myView;
    private TextView tvVersion;
    private ImageView ivBack;

    public AboutFragment() {
    }

    public AboutFragment(Context mContext) {
        this.mContext = mContext;
    }

    private static AboutFragment aboutFragment;
    public static AboutFragment getInstance(Context context) {
        if (aboutFragment == null){
            aboutFragment = new AboutFragment(context);
        }
        return aboutFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_about, container, false);;
        initView();
        return myView;
    }

    private void initView() {
        tvVersion = myView.findViewById(R.id.tv_about_version);
        String versionString = getString(R.string.about_version_tip) + " " +
                ToolsUtils.getInstance().getVerName(mContext);
        tvVersion.setText(versionString);

        ivBack = myView.findViewById(R.id.iv_about_view_back);
        ivBack.setOnClickListener(mListener);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == ivBack) {
            // 返回主页
            ToolsUtils.getInstance().backToMainViewFragment(mContext);
        }
    };
}