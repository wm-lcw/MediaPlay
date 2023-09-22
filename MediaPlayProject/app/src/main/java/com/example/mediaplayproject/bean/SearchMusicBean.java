package com.example.mediaplayproject.bean;

/**
 * @author wm
 * @Classname SearchMusicBean
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/20 14:02
 * @Created by wm
 */
public class SearchMusicBean {

    private long musicId;
    private String musicTitle;
    private String sourceArtist;
    private String sourceListName;

    public SearchMusicBean() {
    }

    public SearchMusicBean(long musicId, String musicTitle, String sourceArtist, String sourceListName) {
        this.musicId = musicId;
        this.musicTitle = musicTitle;
        this.sourceArtist = sourceArtist;
        this.sourceListName = sourceListName;
    }


    public long getMusicId() {
        return musicId;
    }

    public void setMusicId(long musicId) {
        this.musicId = musicId;
    }

    public String getMusicTitle() {
        return musicTitle;
    }

    public void setMusicTitle(String musicTitle) {
        this.musicTitle = musicTitle;
    }

    public String getSourceArtist() {
        return sourceArtist;
    }

    public void setSourceArtist(String sourceArtist) {
        this.sourceArtist = sourceArtist;
    }

    public String getSourceListName() {
        return sourceListName;
    }

    public void setSourceListName(String sourceListName) {
        this.sourceListName = sourceListName;
    }
}
