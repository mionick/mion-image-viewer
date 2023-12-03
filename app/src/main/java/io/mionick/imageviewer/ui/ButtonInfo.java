package io.mionick.imageviewer.ui;

import android.view.View;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ButtonInfo {
    public ButtonInfo(int resId, String label, View.OnClickListener listener) {
        this.resId = resId;
        this.label = label;
        this.listener = listener;
    }

    private int resId;
    private String label;
    private View.OnClickListener listener;
    private View view;

}
