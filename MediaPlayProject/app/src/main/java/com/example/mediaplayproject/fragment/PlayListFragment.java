package com.example.mediaplayproject.fragment;

import android.content.Context;
import android.os.Bundle;


import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.MusicListAdapter;
import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;


/**
 * @author wm
 */
public class PlayListFragment extends BaseFragment {
    private ImageView ivLocalList;
    private ListView musicListView;
    private TextView tvListTitle;
    private List<MediaFileBean> musicList;
    private Context mContext;
    private View fragmentView;
    private MusicListAdapter musicListAdapter;
    private Handler mHandler;
    private int listMode, currentListMode, mPosition;

    public PlayListFragment(Context context, List<MediaFileBean> list, int listMode, Handler handler) {
        this.mContext = context;
        this.musicList = list;
        this.listMode = listMode;
        this.mHandler = handler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_play_list, container, false);
        init();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentView.setFocusable(true);
        if (musicListAdapter != null) {
            if (musicList.size() > 0) {
                musicListAdapter.notifyDataSetChanged();
            }
        }
    }

    private void init() {
        DebugLog.debug("");
        musicListAdapter = new MusicListAdapter(mContext, listMode, musicList);
        tvListTitle = fragmentView.findViewById(R.id.tv_list_title);

        if (listMode == Constant.LIST_MODE_DEFAULT){
            tvListTitle.setText("默认列表");
        } else if (listMode == Constant.LIST_MODE_FAVORITE){
            tvListTitle.setText("收藏列表");
        }


        // 定位当前播放歌曲
        ivLocalList = fragmentView.findViewById(R.id.iv_local_music);
        ivLocalList.setOnClickListener(mListener);

        // 绑定listView
        musicListView = fragmentView.findViewById(R.id.lv_musicList);
        musicListView.setAdapter(musicListAdapter);
        musicListView.setOnItemClickListener((parent, view, position, id) -> {
            mPosition = position;
            Message msg = new Message();
            msg.what = Constant.HANDLER_MESSAGE_FROM_LIST_FRAGMENT;
            Bundle bundle = new Bundle();
            bundle.putInt("musicListMode",listMode);
            bundle.putInt("position", position);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            musicListAdapter.setSelectPosition(position);
        });
        musicListAdapter.notifyDataSetChanged();
        musicListView.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener mListener = view -> {
            if (view == ivLocalList) {
                setSelection(mPosition);
            }
    };

    @Override
    public int checkRefreshPosition(int deletePosition) {
        int isRefreshResult = musicListAdapter.checkRefreshPosition(deletePosition);
        DebugLog.debug("isRefreshResult " + isRefreshResult);
        if (isRefreshResult == Constant.RESULT_BEFORE_CURRENT_POSITION) {
            // 删除的是小于当前播放下标的歌曲
            musicListAdapter.refreshSelectedPosition();
            mPosition--;
        } else if (isRefreshResult == Constant.RESULT_IS_CURRENT_POSITION) {
            // 删除的是当前的歌曲，需要先隐藏高亮坐标,等收到service的消息后再打开
            musicListAdapter.setSelectPosition(-1);
            musicListAdapter.notifyDataSetChanged();
        }
        return isRefreshResult;
    }

    @Override
    public void refreshListView() {
        if (musicListAdapter != null) {
            musicListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public int getListMode() {
        return listMode;
    }

    @Override
    public void setSelectPosition(int position) {
        if (musicListAdapter != null) {
            musicListAdapter.setSelectPosition(position);
        }
    }

    @Override
    public void setSelection(int position) {
        if (musicListView != null) {
            musicListView.setSelection(position);
        }
    }
}