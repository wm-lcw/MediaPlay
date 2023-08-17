package com.example.mediaplayproject.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.activity.MusicPlayActivity;
import com.example.mediaplayproject.adapter.FavoriteMusicAdapter;
import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.service.MusicPlayService;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 */
public class FavoriteListFragment extends BaseFragment {
    private ImageView ivLocalList;
    private ListView mFavoriteListView;
    private List<MediaFileBean> mFavoriteList;
    private Context mContext;
    private View favoriteView;
    private FavoriteMusicAdapter favoriteListAdapter;
    private Handler mHandler;

    public int getCurrentListMode() {
        return currentListMode;
    }

    public void setCurrentListMode(int currentListMode) {
        this.currentListMode = currentListMode;
    }

    private int listMode, currentListMode, mPosition;

    public FavoriteListFragment(Context context, List<MediaFileBean> favoriteMusicList, Handler handler) {
        mContext = context;
        mFavoriteList = favoriteMusicList;
        mHandler = handler;
        listMode = Constant.LIST_MODE_FAVORITE;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        favoriteView = inflater.inflate(R.layout.fragment_favorite_list, container, false);
        init();
        return favoriteView;
    }

    @Override
    public void onResume() {
        super.onResume();
        favoriteView.setFocusable(true);
        if (favoriteListAdapter != null) {
            if (mFavoriteList.size()>0){
                favoriteListAdapter.notifyDataSetChanged();
            }
        }
    }

    private void init() {
        DebugLog.debug("");
        favoriteListAdapter = new FavoriteMusicAdapter(mContext, mFavoriteList);

        //定位当前播放歌曲
        ivLocalList = favoriteView.findViewById(R.id.iv_local_music);
        ivLocalList.setOnClickListener(mListener);

        mFavoriteListView = favoriteView.findViewById(R.id.lv_favoriteList);
        mFavoriteListView.setAdapter(favoriteListAdapter);
        mFavoriteListView.setOnItemClickListener((parent, view, position, id) -> {

            mPosition = position;
            //发送Message给MusicPlayActivity，
            Message msg = new Message();
            msg.what = MusicPlayActivity.HANDLER_MESSAGE_FROM_LIST_FRAGMENT;
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            bundle.putInt("musicListMode", 1);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            favoriteListAdapter.setSelectPosition(position);
        });
        favoriteListAdapter.notifyDataSetChanged();
        mFavoriteListView.setVisibility(View.VISIBLE);
    }


    private View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint({"ResourceType", "UseCompatLoadingForColorStateLists"})
        @Override
        public void onClick(View view) {
//            if (view == ivCloseListView) {
//                mWindowManager.removeView(mFloatLayout);
//            } else if (view == ivLocalList) {
//                initListHighLight();
//            }
        }
    };

    @Override
    public void setSelectPosition(int position) {
        if (favoriteListAdapter != null) {
            favoriteListAdapter.setSelectPosition(position);
        }
    }

    @Override
    public void setSelection(int position) {
        if (mFavoriteListView != null) {
            mFavoriteListView.setSelection(position);
        }
    }

    @Override
    public int checkRefreshPosition(int deletePosition) {
        int isRefreshResult = favoriteListAdapter.checkRefreshPosition(deletePosition);
        DebugLog.debug("isRefreshResult " + isRefreshResult);
        if (isRefreshResult == Constant.RESULT_BEFORE_CURRENT_POSITION) {
            // 删除的是小于当前播放下标的歌曲
            favoriteListAdapter.refreshSelectedPosition();
            mPosition--;
        } else if (isRefreshResult == Constant.RESULT_IS_CURRENT_POSITION) {
            // 删除的是当前的歌曲，需要先隐藏高亮坐标,等收到service的消息后再打开
            favoriteListAdapter.setSelectPosition(-1);
            favoriteListAdapter.notifyDataSetChanged();
        }
        return isRefreshResult;
    }

    @Override
    public void refreshListView() {
        if (favoriteListAdapter != null) {
            favoriteListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public int getListMode() {
        return listMode;
    }


}