/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.hugo.android.scanner.camera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

import cn.hugo.android.scanner.R;
import cn.hugo.android.scanner.config.Config;

/**
 * Manages beeps and vibrations for {@link  }.
 */
public class BeepManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private static final String TAG = BeepManager.class.getSimpleName();
    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;
    private final Activity mActivity;
    private MediaPlayer mMediaPlayer;
    private boolean mPlayBeep;
    private boolean mVibrate;

    public BeepManager(Activity activity) {
        this.mActivity = activity;
        this.mMediaPlayer = null;
        updatePrefs();
    }

    public BeepManager(Context context) {
        this.mActivity = (Activity) context;
        this.mMediaPlayer = null;
        updatePrefs();
    }

    /**
     * 掃描成功後可以播放提示音並震動，這兩種功能都是用戶自定義的 在Barcode Scanner中點擊功能表鍵，點設置即可看到這兩項的設置
     */
    public synchronized void updatePrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mPlayBeep = shouldBeep(prefs, mActivity);
        mVibrate = prefs.getBoolean(Config.KEY_VIBRATE, false);
        if (mPlayBeep && mMediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mMediaPlayer = buildMediaPlayer(mActivity);
        }
    }

    /**
     * 根據配置播放提示音和震動
     */
    public synchronized void playBeepSoundAndVibrate() {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer.start();
        }
        if (mVibrate) {
            Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    private static boolean shouldBeep(SharedPreferences prefs, Context activity) {
        boolean shouldPlayBeep = prefs.getBoolean(Config.KEY_PLAY_BEEP, true);
        if (shouldPlayBeep) {
            // See if sound settings overrides this
            AudioManager audioService = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                shouldPlayBeep = false;
            }
        }
        return shouldPlayBeep;
    }

    private MediaPlayer buildMediaPlayer(Context activity) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.beep);
        try {
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            file.close();
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer = null;
        }
        return mediaPlayer;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0);
    }

    @Override
    public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required
            // and finish
            mActivity.finish();
        } else {
            // possibly media player error, so release and recreate
            mp.release();
            mMediaPlayer = null;
            updatePrefs();
        }
        return true;
    }

    /**
     * 新增
     * 關閉beep聲音
     */
    public synchronized void close() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
