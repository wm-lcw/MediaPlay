package com.example.mediaplayproject.fragment.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description: java类作用描述
 * @author: wm
 */
public class StatisticsFragment extends Fragment {

    private View myView;
    private Context mContext;
    private TextView tvPlayTotal, tvArtistTotal, tvPlayFromArtist;
    private List<Map.Entry<String, Integer>> playTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> artistTotalList = new ArrayList<>();
    private List<Map.Entry<String, Integer>> mostPlayFromArtistTotalList = new ArrayList<>();

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.fragment_statistics, container, false);;
        initView();
        return myView;
    }

    private void initView() {
        tvPlayTotal = myView.findViewById(R.id.tv_play_total);
        tvArtistTotal = myView.findViewById(R.id.tv_artist_total);
        tvPlayFromArtist = myView.findViewById(R.id.tv_play_from_artist_total);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDataFromService();
    }

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
                tvPlayTotal.setText("您播放最多的歌曲是 " + playTotalList.get(0).getKey() + " , 共播放了 " + playTotalList.get(0).getValue() + "次");
                tvArtistTotal.setText("您最喜欢的歌手是 " + artistTotalList.get(0).getKey() + " , 共播放了 " + artistTotalList.get(0).getValue() + "首TA的音乐");
                if (mostPlayFromArtistTotalList != null && mostPlayFromArtistTotalList.size() > 0) {

                }
                tvPlayFromArtist.setText("在" + artistTotalList.get(0).getKey() + "的歌曲中你最喜欢 " + mostPlayFromArtistTotalList.get(0).getKey() + " , 共播放了 " + mostPlayFromArtistTotalList.get(0).getValue() + "次");
                DebugLog.debug("time " + DataRefreshService.getTotalPlayTime());
//                for (int i = 0; i < 3 ; i++){
//                    DebugLog.debug("play total " + playTotalList.get(i).getKey()
//                            + ";  " + playTotalList.get(i).getValue());
//                }
//
//                for (int i = 0; i < 3 ; i++){
//                    DebugLog.debug("play total " + artistTotalList.get(i).getKey()
//                            + ";  " + artistTotalList.get(i).getValue());
//                }
//
//                if (mostPlayFromArtistTotalList != null && mostPlayFromArtistTotalList.size() > 0){
//                    for (int i = 0 ; i < mostPlayFromArtistTotalList.size() ; i++){
//                        DebugLog.debug("" + mostPlayFromArtistTotalList.get(i).getKey() +
//                                "; " + mostPlayFromArtistTotalList.get(i).getValue());
//                    }
//
//                }


            }
        }
    };
}