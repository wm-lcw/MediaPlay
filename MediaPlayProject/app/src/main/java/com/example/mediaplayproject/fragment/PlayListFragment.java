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
    private int mPosition;
    private String listName;

    public PlayListFragment(Context context, List<MediaFileBean> list, String listName, Handler handler) {
        this.mContext = context;
        this.musicList = list;
        this.listName = listName;
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
        musicListAdapter = new MusicListAdapter(mContext, listName, musicList);
        tvListTitle = fragmentView.findViewById(R.id.tv_list_title);
        refreshListTitle();

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
            bundle.putString("musicListName", listName);
            bundle.putInt("position", position);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            musicListAdapter.setSelectPosition(position);
        });
        musicListAdapter.notifyDataSetChanged();
    }

    private void refreshListTitle() {
        tvListTitle.setText(listName);
    }

    private View.OnClickListener mListener = view -> {
        if (view == ivLocalList) {
            setSelection(mPosition);
        }
    };

    /**
     *  检查删除歌曲的下标与当前播放歌曲的下标
     *  @author wm
     *  @createTime 2023/9/3 18:02
     * @param deletePosition: 要删除的歌曲下标
     * @return : int：1-小于当前播放的下标； 0-删除的是当前播放歌曲； 2-删除的是后面的歌曲，不受影响
     */
    public int checkRefreshPosition(int deletePosition) {
        int isRefreshResult = musicListAdapter.checkRefreshPosition(deletePosition);
        if (isRefreshResult == Constant.RESULT_BEFORE_CURRENT_POSITION) {
            // 删除的是小于当前播放下标的歌曲
            musicListAdapter.refreshSelectedPosition();
            mPosition--;
        } else if (isRefreshResult == Constant.RESULT_IS_CURRENT_POSITION) {
            // 删除的是当前的歌曲，需要先隐藏高亮坐标,等收到Activity的刷新消息后再打开
            musicListAdapter.setSelectPosition(-1);
            musicListAdapter.notifyDataSetChanged();
        }
        return isRefreshResult;
    }

    /**
     *  刷新列表
     *  @author wm
     *  @createTime 2023/9/3 18:05
     */
    public void refreshListView() {
        if (musicListAdapter != null) {
            musicListAdapter.notifyDataSetChanged();
        }
    }

    public String getListName() {
        return listName;
    }

    /**
     *  选中高亮效果
     *  @author wm
     *  @createTime 2023/9/3 18:06
     * @param position: 需要高亮的歌曲下标
     */
    public void setSelectPosition(int position) {
        if (musicListAdapter != null) {
            musicListAdapter.setSelectPosition(position);
        }
    }

    /**
     *  定位当前歌曲
     *  @author wm
     *  @createTime 2023/9/3 18:07
     * @param position: 歌曲下标
     */
    public void setSelection(int position) {
        if (musicListView != null) {
            musicListView.setSelection(position);
        }
    }

    /**
     * 更改播放列表
     * @param list:     音乐列表
     * @param listName: 播放模式
     * @param position: 播放的下标
     * @author wm
     * @createTime 2023/8/31 10:48
     */
    public void changePlayList(List<MediaFileBean> list, String listName, int position) {
        this.listName = listName;
        this.musicList = list;
        this.mPosition = position;
        refreshListTitle();
        musicListAdapter.changePlayList(list, listName);
        setSelection(mPosition);
        setSelectPosition(mPosition);
        refreshListView();
    }
}