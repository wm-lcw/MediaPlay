package com.example.mediaplayproject.adapter.viewpager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mediaplayproject.fragment.PlayListFragment;

import java.util.List;

/**
 * @author wm
 * @Description 主页视图 Adapter
 * @Date 2023/8/4 14:48
 */
public class MainViewPagerAdapter extends FragmentStateAdapter {

    private List<Fragment> fragmentLists;
    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<Fragment> fragmentLists) {
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
