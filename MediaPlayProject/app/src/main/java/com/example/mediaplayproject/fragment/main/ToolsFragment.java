package com.example.mediaplayproject.fragment.main;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;
import com.example.mediaplayproject.utils.ToolsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author wm
 */
public class ToolsFragment extends Fragment {


    private View myView;
    private Button btnTest;
    private List<Map.Entry<String, Integer>> playTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> artistTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> mostPlayFromArtistTotalList = new ArrayList<>();

    private static ToolsFragment instance;
    public static ToolsFragment getInstance() {
        if (instance == null) {
            instance = new ToolsFragment();
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_tools, container, false);

        myView.setOnTouchListener((v, event) -> {
            ToolsUtils.getInstance().hideKeyboard(myView);
            return false;
        });
        initView();
        return myView;
    }

    private void initView() {
        btnTest = myView.findViewById(R.id.btn_test);
        btnTest.setOnClickListener(mListener);
    }

    private final View.OnClickListener mListener = view -> {
        if (view == btnTest) {
            refreshDataFromService();
        }
    };


    private void refreshDataFromService(){
        playTotalList = DataRefreshService.getPlayTotalList();
        artistTotalList = DataRefreshService.getArtistTotalList();
        mostPlayFromArtistTotalList = DataRefreshService.getMostPlayFromArtistTotalList(artistTotalList.get(0));
        toolsViewHandler.sendEmptyMessageDelayed(Constant.HANDLER_MESSAGE_DELAY_REFRESH_PLAY_TOTAL_DATA,300);
    }

    final Handler toolsViewHandler = new Handler(Looper.myLooper()) {
        @SuppressLint("ResourceAsColor")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == Constant.HANDLER_MESSAGE_DELAY_REFRESH_PLAY_TOTAL_DATA) {
                for (int i = 0; i < 3 ; i++){
                    DebugLog.debug("play total " + playTotalList.get(i).getKey()
                            + ";  " + playTotalList.get(i).getValue());
                }

                for (int i = 0; i < 3 ; i++){
                    DebugLog.debug("play total " + artistTotalList.get(i).getKey()
                            + ";  " + artistTotalList.get(i).getValue());
                }

                if (mostPlayFromArtistTotalList != null && mostPlayFromArtistTotalList.size() > 0){
                    for (int i = 0 ; i < mostPlayFromArtistTotalList.size() ; i++){
                        DebugLog.debug("" + mostPlayFromArtistTotalList.get(i).getKey() +
                                "; " + mostPlayFromArtistTotalList.get(i).getValue());
                    }

                }


            }
        }
    };

}