package com.adv.videoplayerlib;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CopyProgressDialog extends Dialog {
    private TextView tv_progress;
    private ProgressBar pb_progress;

    public CopyProgressDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_progrees);
        setCanceledOnTouchOutside(false);

        pb_progress = (ProgressBar) findViewById(R.id.pb_progress_num);
        tv_progress = (TextView) findViewById(R.id.tv_progress_num);
    }

    public void setProgress(int progress) {
        pb_progress.setProgress(progress);
    }

    public void setProgressText(String progress) {
        tv_progress.setText(progress);
    }
}
