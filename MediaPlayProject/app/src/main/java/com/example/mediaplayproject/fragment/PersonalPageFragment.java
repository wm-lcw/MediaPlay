package com.example.mediaplayproject.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mediaplayproject.R;
import com.example.mediaplayproject.base.BaseFragment;


/**
 * @author wm
 */
public class PersonalPageFragment extends BaseFragment {

    public static PersonalPageFragment newInstance(String param1, String param2) {
        PersonalPageFragment fragment = new PersonalPageFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_personal_page, container, false);
    }

    @Override
    public int getListMode() {
        return 0;
    }

    @Override
    public void setSelectPosition(int position) {

    }

    @Override
    public void setSelection(int position) {

    }

    @Override
    public int checkRefreshPosition(int deletePosition) {
        return 0;
    }

    @Override
    public void refreshListView() {

    }
}