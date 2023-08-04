package com.example.mediaplayproject.bean;

import java.util.Objects;

/**
 * @author wm
 */
public class MediaFileBean {

    private long id;
    /**音乐名称*/
    private String title;
    /**音乐文件*/
    private String data;
    /**专辑*/
    private String album;
    /**艺人*/
    private String artist;
    /**音乐时长*/
    private int duration;
    /**音乐文件大小*/
    private long size;
    private boolean isPlaying;
    private boolean isLike;

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        MediaFileBean that = (MediaFileBean) o;
        return id == that.id && duration == that.duration && size == that.size && isPlaying == that.isPlaying && isLike == that.isLike && Objects.equals(title, that.title) && Objects.equals(data, that.data) && Objects.equals(album, that.album) && Objects.equals(artist, that.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, data, album, artist, duration, size, isPlaying, isLike);
    }

    public boolean isLike() {
        return isLike;
    }

    public void setLike(boolean like) {
        isLike = like;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }


}
