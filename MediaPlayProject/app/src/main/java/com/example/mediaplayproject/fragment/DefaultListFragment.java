package com.example.mediaplayproject.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.example.mediaplayproject.adapter.MusicAdapter;
import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.bean.MediaFileBean;
import com.example.mediaplayproject.utils.Constant;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 */
public class DefaultListFragment extends BaseFragment {
    private ImageView ivLocalList;
    private ListView mDefaultListView;
    private List<MediaFileBean> mDefaultMusicList;
    private Context mContext;
    private View defaultView;
    private MusicAdapter defaultListAdapter;
    private Handler mHandler;
    private int listMode, currentListMode, mPosition;

    public int getCurrentListMode() {
        return currentListMode;
    }

    public void setCurrentListMode(int currentListMode) {
        this.currentListMode = currentListMode;
    }

    public DefaultListFragment(Context context, List<MediaFileBean> defaultMusicList, Handler handler) {
        mContext = context;
        mDefaultMusicList = defaultMusicList;
        mHandler = handler;
        listMode = Constant.LIST_MODE_DEFAULT;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        defaultView = inflater.inflate(R.layout.fragment_default_list, container, false);
        init();
        return defaultView;
    }

    @Override
    public void onResume() {
        super.onResume();
        defaultView.setFocusable(true);
        if (defaultListAdapter != null) {
            if (mDefaultMusicList.size()>0){
                defaultListAdapter.notifyDataSetChanged();
            }
        }

    }

    private void init() {
        DebugLog.debug("");
        defaultListAdapter = new MusicAdapter(mContext, mDefaultMusicList);

        //定位当前播放歌曲
        ivLocalList = defaultView.findViewById(R.id.iv_local_music);
        ivLocalList.setOnClickListener(mListener);

        mDefaultListView = defaultView.findViewById(R.id.lv_musicList);
        mDefaultListView.setAdapter(defaultListAdapter);
        mDefaultListView.setOnItemClickListener((parent, view, position, id) -> {
            mPosition = position;
            //发送Message给MusicPlayActivity，
            Message msg = new Message();
            msg.what = MusicPlayActivity.HANDLER_MESSAGE_FROM_LIST_FRAGMENT;
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            bundle.putInt("musicListMode", 0);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            defaultListAdapter.setSelectPosition(position);
        });
        defaultListAdapter.notifyDataSetChanged();
        mDefaultListView.setVisibility(View.VISIBLE);
    }


    private View.OnClickListener mListener = new View.OnClickListener() {
        @SuppressLint({"ResourceType", "UseCompatLoadingForColorStateLists"})
        @Override
        public void onClick(View view) {
//            if (view == ivLocalList) {
//                initListHighLight();
//            }
        }
    };

    @Override
    public void setSelectPosition(int position) {
        if (defaultListAdapter != null) {
            defaultListAdapter.setSelectPosition(position);
        }
    }

    @Override
    public void setSelection(int position) {
        if (mDefaultListView != null) {
            mDefaultListView.setSelection(position);
        }
    }

    @Override
    public int checkRefreshPosition(int deletePosition) {
        return deletePosition;
    }

    @Override
    public void refreshListView() {
        if (defaultListAdapter != null) {
            defaultListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public int getListMode() {
        return listMode;
    }
}