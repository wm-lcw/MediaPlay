package com.example.mediaplayproject.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wm
 * @Classname MusicListBean
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/8/28 18:32
 * @Created by wm
 */
public class MusicListBean {
    private String listName;
    private long listId;
    /**
    * 注意：这里的size并不能同步的获取list的音乐数量需要手动的获取list.size()
    * */
    private int listSize;
    private List<MediaFileBean> musicList;

    public MusicListBean(String listName) {
        this.listName = listName;
        musicList = new ArrayList<>();
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public long getListId() {
        return listId;
    }

    public void setListId(long listId) {
        this.listId = listId;
    }

    public int getListSize() {
        return musicList.size();
    }

    public void setListSize(int listSize) {
        this.listSize = listSize;
    }

    public List<MediaFileBean> getMusicList() {
        return musicList;
    }

    public void setMusicList(List<MediaFileBean> musicList) {
        this.musicList = musicList;
    }

    @Override
    public String toString() {
        return "MusicListBean{" +
                "listName='" + listName + '\'' +
                ", listSize=" + musicList.size() +
                ", musicList=" + musicList +
                '}';
    }
}
