package com.example.mediaplayproject.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.adapter.musiclist.MusicListAdapter;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.DataRefreshService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;


/**
 * @author wm
 */
public class PlayListFragment extends Fragment {
    private ImageView ivLocalList;
    private ListView musicListView;
    private TextView tvListTitle;
    private List<MediaFileBean> musicList;
    private Context mContext;
    private View fragmentView;
    private MusicListAdapter musicListAdapter;
    private int mPosition;
    private String listName;
    private boolean isInitSuccess = false;
    /**
     * listMode用来判断列表的类型，三个固定类型的列表
     *     public static final int LIST_SHOW_MODE_CURRENT = 1;
     *     public static final int LIST_SHOW_MODE_FAVORITE = 2;
     *     public static final int LIST_SHOW_MODE_HISTORY = 3;
     * */
    private int listMode;

    public PlayListFragment(Context context, List<MediaFileBean> list, String listName, int listMode) {
        DebugLog.debug("PlayListFragment " + listName);
        this.mContext = context;
        this.musicList = list;
        this.listName = listName;
        this.listMode = listMode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_play_list, container, false);
        init();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLog.debug("PlayListFragment " + listName);
        fragmentView.setFocusable(true);
        if (musicListAdapter != null && musicList.size() > 0) {
            if (DataRefreshService.getLastPlayListName().equalsIgnoreCase(listName)){
                mPosition = DataRefreshService.getLastPosition();
            } else {
                mPosition = -1;
            }
            setSelection(mPosition);
            musicListAdapter.notifyDataSetChanged();
        }
    }

    private void init() {
        DebugLog.debug("PlayListFragment " + listName);
        if (listMode == Constant.LIST_SHOW_MODE_CURRENT){
            listName = DataRefreshService.getLastPlayListName();
            musicList = DataRefreshService.getMusicListByName(listName);
        } else if (listMode == Constant.LIST_SHOW_MODE_FAVORITE){
            listName = Constant.LIST_MODE_FAVORITE_NAME;
            musicList = DataRefreshService.getFavoriteList();
        } else if (listMode == Constant.LIST_SHOW_MODE_HISTORY){
            listName = Constant.LIST_MODE_HISTORY_NAME;
            musicList = DataRefreshService.getHistoryList();
        }
        if (DataRefreshService.getLastPlayListName().equalsIgnoreCase(listName)){
            mPosition = DataRefreshService.getLastPosition();
        } else {
            mPosition = -1;
        }
        musicListAdapter = new MusicListAdapter(mContext, listName, musicList, mPosition);
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
            Intent intent = new Intent(Constant.CHANGE_MUSIC_ACTION);
            Bundle bundle = new Bundle();
            bundle.putInt("position", mPosition);
            bundle.putString("musicListName", listName);
            intent.putExtras(bundle);
            mContext.sendBroadcast(intent);
            musicListAdapter.setSelectPosition(position);
        });
        isInitSuccess  = true;
        musicListAdapter.notifyDataSetChanged();
    }

    /**
     *  刷新列表名称
     *  @author wm
     *  @createTime 2023/9/24 13:11
     */
    private void refreshListTitle() {
        // 固定的几个列表需要动态设定列表名称
        if (Constant.LIST_MODE_DEFAULT_NAME.equalsIgnoreCase(listName)){
            tvListTitle.setText(R.string.default_list_name);
        } else if (Constant.LIST_MODE_FAVORITE_NAME.equalsIgnoreCase(listName)){
            tvListTitle.setText(R.string.favorite_list_name);
        } else if (Constant.LIST_MODE_HISTORY_NAME.equalsIgnoreCase(listName)){
            tvListTitle.setText(R.string.history_list_name);
        } else {
            tvListTitle.setText(listName);
        }
    }

    private final View.OnClickListener mListener = view -> {
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

    public boolean isInitSuccess() {
        return isInitSuccess;
    }
}