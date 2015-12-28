package com.sundy.slotcarddemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by sundy on 15/12/28.
 */
public class QuestionActivity extends Activity {

    private final String TAG = "QuestionActivity";
    private TextView btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question);

        btnClose = (TextView) findViewById(R.id.btnClose);
        btnClose.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnClose:
                    finish();
                    break;
            }
        }
    };

}
