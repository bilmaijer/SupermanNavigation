package com.movesense.mds.sampleapp.example_app_using_mds_api.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.hanks.htextview.evaporate.EvaporateTextView;
import com.movesense.mds.sampleapp.MainActivity;
import com.movesense.mds.sampleapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SplashActivity extends AppCompatActivity {

    private final String[] sentences = new String[]{"Suunto", "Movesense", "Android", "Try it   !!!"};

    @BindView(R.id.evaporate_htextView)
    EvaporateTextView evaporateHtextView;

    private int mSentenceCounter = 0;
    private final Handler mHandler = new Handler();
    private final int ANIMATION_TIME = 1500;
    private final int DELAY_START_ANIMATION_TIME = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                evaporateHtextView.animateText(sentences[mSentenceCounter]);
                mSentenceCounter++;
                if (mSentenceCounter == 4) {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        }
                    }, ANIMATION_TIME);

                } else {
                    mHandler.postDelayed(this, ANIMATION_TIME);
                }
            }
        }, DELAY_START_ANIMATION_TIME);

    }
}
