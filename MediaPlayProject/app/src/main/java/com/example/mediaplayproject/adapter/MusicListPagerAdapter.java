package com.example.mediaplayproject.adapter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mediaplayproject.base.BaseFragment;
import com.example.mediaplayproject.utils.DebugLog;

import java.util.List;

/**
 * @author wm
 * @Description 音乐列表悬浮窗 Adapter
 * @Date 2023/8/4 14:48
 */
public class MusicListPagerAdapter extends FragmentStateAdapter {

    private List<BaseFragment> fragmentLists;
    public MusicListPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<BaseFragment> fragmentLists) {
        super(fragmentActivity);
        this.fragmentLists = fragmentLists;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentLists.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentLists.size();
    }

}
