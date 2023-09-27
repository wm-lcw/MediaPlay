package com.example.mediaplayproject.bean;

/**
 * @author wm
 * @Classname LanguageBean
 * @Description TODO
 * @Version 1.0.0
 * @Date 2023/9/27 19:23
 * @Created by wm
 */
public class LanguageBean {
    private String language;
    private String languageTag;

    public LanguageBean(String language, String languageTag) {
        this.language = language;
        this.languageTag = languageTag;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
    }
}
