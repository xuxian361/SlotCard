package com.sundy.slotcarddemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.acs.audiojack.*;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";
    public static final String DEFAULT_MASTER_KEY_STRING = "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
    public static final String DEFAULT_AES_KEY_STRING = "4E 61 74 68 61 6E 2E 4C 69 20 54 65 64 64 79 20";

    private TextView txtCardNum;
    private TextView txtMsg;
    private LinearLayout linear_Reset;
    private Button btnReset;
    private ImageButton btnQuestion;

    private AudioManager mAudioManager;
    private AudioJackReader mReader;
    private byte[] mAesKey = new byte[16];
    private byte[] mIksn = new byte[10];
    private DukptReceiver mDukptReceiver = new DukptReceiver();
    private byte[] mIpek = new byte[16];
    private byte[] mMasterKey = new byte[16];
    private byte[] mNewMasterKey = new byte[16];


    private final BroadcastReceiver mHeadsetPlugReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                boolean plugged = (intent.getIntExtra("state", 0) == 1);
                /* Mute the audio output if the reader is unplugged. */
                mReader.setMute(!plugged);
                if (plugged) {//插入设备
                    txtMsg.setText(R.string.check_light);
                    linear_Reset.setVisibility(View.VISIBLE);
                    mReader.start();
                    mReader.reset();
                } else {//拔出设备
                    txtMsg.setText(R.string.insertion_device);
                    linear_Reset.setVisibility(View.INVISIBLE);
                    mReader.stop();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        txtCardNum = (TextView) findViewById(R.id.txtCardNum);
        txtMsg = (TextView) findViewById(R.id.txtMsg);
        linear_Reset = (LinearLayout) findViewById(R.id.linear_Reset);
        btnReset = (Button) findViewById(R.id.btnReset);
        btnReset.setOnClickListener(onClick);
        btnQuestion = (ImageButton) findViewById(R.id.btnQuestion);
        btnQuestion.setOnClickListener(onClick);


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mReader = new AudioJackReader(mAudioManager, true);

         /* Register the headset plug receiver. */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetPlugReceiver, filter);

        MyUtils.toByteArray(DEFAULT_MASTER_KEY_STRING, mNewMasterKey);
        MyUtils.toByteArray(DEFAULT_MASTER_KEY_STRING, mMasterKey);
        MyUtils.toByteArray(DEFAULT_AES_KEY_STRING, mAesKey);

        mReader.setOnTrackDataAvailableListener(new OnTrackDataAvailableListener());

        mReader.start();

        /* Set the key serial number. */
        mDukptReceiver.setKeySerialNumber(mIksn);

        /* Load the initial key. */
        mDukptReceiver.loadInitialKey(mIpek);

    }

    private class OnTrackDataAvailableListener implements
            AudioJackReader.OnTrackDataAvailableListener {

        private Track1Data mTrack1Data;
        private Track2Data mTrack2Data;
        private Track1Data mTrack1MaskedData;
        private Track2Data mTrack2MaskedData;

        @Override
        public void onTrackDataAvailable(AudioJackReader reader,
                                         TrackData trackData) {
            mTrack1Data = new Track1Data();
            mTrack2Data = new Track2Data();
            mTrack1MaskedData = new Track1Data();
            mTrack2MaskedData = new Track2Data();

            if ((trackData.getTrack1ErrorCode() != TrackData.TRACK_ERROR_SUCCESS)
                    && (trackData.getTrack2ErrorCode() != TrackData.TRACK_ERROR_SUCCESS)) {
                Log.e(TAG, "------->no data");
            } else if (trackData.getTrack1ErrorCode() != TrackData.TRACK_ERROR_SUCCESS) {
                Log.e(TAG, "------->no data 1");
            } else if (trackData.getTrack2ErrorCode() != TrackData.TRACK_ERROR_SUCCESS) {
                Log.e(TAG, "------->no data 2");
            }

            /* Show the track data. */
            if (trackData instanceof AesTrackData) {
                Log.e(TAG, "------->AesTrackData");
                showAesTrackData((AesTrackData) trackData);
            } else if (trackData instanceof DukptTrackData) {
                Log.e(TAG, "------->DukptTrackData");
                showDukptTrackData((DukptTrackData) trackData);
            }
        }

        /**
         * Shows the AES track data.
         *
         * @param trackData the AES track data.
         */
        private void showAesTrackData(AesTrackData trackData) {

            byte[] decryptedTrackData = null;

            /* Decrypt the track data. */
            try {
                decryptedTrackData = MyUtils.aesDecrypt(mAesKey,
                        trackData.getTrackData());
            } catch (GeneralSecurityException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        MyUtils.showMessageDialog(MainActivity.this,
                                R.string.message_track_data_error_decrypted);
                    }
                });
                /* Show the track data. */
                showTrackDataAES();
                return;
            }

            /* Verify the track data. */
            if (!mReader.verifyData(decryptedTrackData)) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        MyUtils.showMessageDialog(MainActivity.this,
                                R.string.message_track_data_error_checksum);
                    }
                });
                /* Show the track data. */
                showTrackDataAES();
                return;
            }

            /* Decode the track data. */
            mTrack1Data.fromByteArray(decryptedTrackData, 0,
                    trackData.getTrack1Length());
            mTrack2Data.fromByteArray(decryptedTrackData, 79,
                    trackData.getTrack2Length());

            /* Show the track data. */
            showTrackDataAES();
        }

        /**
         * Shows the DUKPT track data.
         *
         * @param trackData the DUKPT track data.
         */
        private void showDukptTrackData(DukptTrackData trackData) {

            int ec = 0;
            int ec2 = 0;
            byte[] track1Data = null;
            byte[] track2Data = null;
            String track1DataString = null;
            String track2DataString = null;
            byte[] key = null;
            byte[] dek = null;
            byte[] macKey = null;
            byte[] dek3des = null;

            mTrack2MaskedData.fromString(trackData.getTrack2MaskedData());

            /* Compare the key serial number. */
            if (!DukptReceiver.compareKeySerialNumber(mIksn,
                    trackData.getKeySerialNumber())) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        MyUtils.showMessageDialog(MainActivity.this,
                                R.string.message_track_data_error_ksn);
                    }
                });

                /* Show the track data. */
                showTrackDataDUKPT();
                return;
            }

            /* Get the encryption counter from KSN. */
            ec = DukptReceiver.getEncryptionCounter(trackData
                    .getKeySerialNumber());

            /* Get the encryption counter from DUKPT receiver. */
            ec2 = mDukptReceiver.getEncryptionCounter();

            /*
             * Load the initial key if the encryption counter from KSN is less
             * than the encryption counter from DUKPT receiver.
             */
            if (ec < ec2) {

                mDukptReceiver.loadInitialKey(mIpek);
                ec2 = mDukptReceiver.getEncryptionCounter();
            }

            /*
             * Synchronize the key if the encryption counter from KSN is greater
             * than the encryption counter from DUKPT receiver.
             */
            while (ec > ec2) {

                mDukptReceiver.getKey();
                ec2 = mDukptReceiver.getEncryptionCounter();
            }

            if (ec != ec2) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        MyUtils.showMessageDialog(MainActivity.this,
                                R.string.message_track_data_error_ec);
                    }
                });

                /* Show the track data. */
                showTrackDataDUKPT();
                return;
            }

            key = mDukptReceiver.getKey();
            if (key == null) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        /* Show the timeout. */
                        Toast.makeText(
                                MainActivity.this,
                                "The maximum encryption count had been reached.",
                                Toast.LENGTH_LONG).show();
                    }
                });

                /* Show the track data. */
                showTrackDataDUKPT();
                return;
            }

            dek = DukptReceiver.generateDataEncryptionRequestKey(key);
            macKey = DukptReceiver.generateMacRequestKey(key);
            dek3des = new byte[24];

            /* Generate 3DES key (K1 = K3) */
            System.arraycopy(dek, 0, dek3des, 0, dek.length);
            System.arraycopy(dek, 0, dek3des, 16, 8);

            try {

                if (trackData.getTrack1Data() != null) {

                    /* Decrypt the track 1 data. */
                    track1Data = MyUtils.tripleDesDecrypt(dek3des,
                            trackData.getTrack1Data());


                    /* Get the track 1 data as string. */
                    track1DataString = new String(track1Data, 1,
                            trackData.getTrack1Length(), "US-ASCII");

                    /* Divide the track 1 data into fields. */
                    mTrack1Data.fromString(track1DataString);
                }

                if (trackData.getTrack2Data() != null) {

                    /* Decrypt the track 2 data. */
                    track2Data = MyUtils.tripleDesDecrypt(dek3des,
                            trackData.getTrack2Data());


                    /* Get the track 2 data as string. */
                    track2DataString = new String(track2Data, 1,
                            trackData.getTrack2Length(), "US-ASCII");

                    /* Divide the track 2 data into fields. */
                    mTrack2Data.fromString(track2DataString);
                }

            } catch (GeneralSecurityException e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        MyUtils.showMessageDialog(MainActivity.this,
                                R.string.message_track_data_error_decrypted);
                    }
                });

            } catch (UnsupportedEncodingException e) {
            }

            /* Show the track data. */
            showTrackDataDUKPT();
        }

        /**
         * Shows the track data.
         */
        private void showTrackDataAES() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String cardNum = MyUtils.concatString(
                            mTrack2Data.getPrimaryAccountNumber(),
                            mTrack2MaskedData.getPrimaryAccountNumber());
                    if (cardNum.length() >= 16) {
                        cardNum = cardNum.substring(0, 16);
                        cardNum = MyUtils.formatCard(cardNum);
                    } else {
                        cardNum = "0000 0000 0000 0000";
                    }
                    txtCardNum.setText(cardNum);
                }
            });
        }

        /**
         * Shows the track data.
         */
        private void showTrackDataDUKPT() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    String cardNum = MyUtils.concatString(
                            mTrack1Data.getPrimaryAccountNumber(),
                            mTrack1MaskedData.getPrimaryAccountNumber());
                    if (cardNum.length() >= 16) {
                        cardNum = cardNum.substring(0, 16);
                        cardNum = MyUtils.formatCard(cardNum);
                    } else {
                        cardNum = "0000 0000 0000 0000";
                    }
                    txtCardNum.setText(cardNum);

                }
            });
        }
    }

    private View.OnClickListener onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnReset:
                    if (mReader != null) {
                        mReader.start();
                        mReader.reset();
                    }
                    break;
                case R.id.btnQuestion:
                    Intent intent = new Intent(MainActivity.this, QuestionActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mReader.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReader.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mReader.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mReader.stop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mHeadsetPlugReceiver);
        super.onDestroy();
    }


}
