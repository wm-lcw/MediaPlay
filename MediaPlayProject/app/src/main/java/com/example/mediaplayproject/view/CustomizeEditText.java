package com.example.mediaplayproject.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.mediaplayproject.R;

/**
 * @author wm
 * @Description 自定义的EditText
 * @Date 2023/9/16 16:09
 */
public class CustomizeEditText extends LinearLayout implements View.OnClickListener, TextWatcher {
    private EditText editText;
    private Button clearButton;
    private ImageView searchImageView;

    /**
     *  设置editText的提示文字
     *  @author wm
     *  @createTime 2023/9/16 16:29
     * @param hint: 提示文字
     */
    public void setEditTextHint(String hint) {
        if (editText != null) {
            editText.setHint(hint);
        }
    }

    /**
     *  获得EditText的文字
     *  @author wm
     *  @createTime 2023/9/16 16:30
     * @return : java.lang.String
     */
    public String getText(){
        return editText.getText().toString();
    }

    public CustomizeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_edittext,this,true);
        editText = findViewById(R.id.et_view);
        searchImageView =findViewById(R.id.iv_search);
        editText.addTextChangedListener(this);
        clearButton = findViewById(R.id.bt_clear);
        clearButton.setVisibility(GONE);
        clearButton.setOnClickListener(this);

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    /**
     *  当输入框文字有改变时
     *  @author wm
     *  @createTime 2023/9/16 16:30
     * @param editable:
     * @return : void
     */
    @Override
    public void afterTextChanged(Editable editable) {
        String input = editText.getText().toString();
        if(input.isEmpty()){
            clearButton.setVisibility(GONE);
        }else {
            clearButton.setVisibility(VISIBLE);
        }
    }


    @Override
    public void onClick(View v) {
        // 当点击了清空按钮，则清空editText的内容
        if (v.getId() == R.id.bt_clear) {
            editText.setText("");
        }
    }

    /**
     *  返回editText对象
     *  @author wm
     *  @createTime 2023/9/16 17:02
     * @return : android.widget.EditText
     */
    public EditText getEditText(){
        return editText;
    }

    /**
     *  获取搜索图标
     *  @author wm
     *  @createTime 2023/9/21 19:37
     * @return : android.widget.ImageView
     */
    public ImageView getSearchImageView() {
        return searchImageView;
    }
}
