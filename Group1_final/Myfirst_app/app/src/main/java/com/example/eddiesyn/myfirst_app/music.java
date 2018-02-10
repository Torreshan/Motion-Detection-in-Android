package com.example.eddiesyn.myfirst_app;

/**
 * Created by fengzehan on 2018/2/7.
 */

import android.content.Context;
import android.media.MediaPlayer;

/** music play  */
public class music {

    private static MediaPlayer mp =null;
    public static void play(Context context, int resource){
        stop(context);
        mp = MediaPlayer.create(context, resource);
        mp.setLooping(true);
        mp.start();
    }
    public static void stop(Context context) {

        if(mp!= null){
            mp.stop();
            mp.release();
            mp = null;
        }
    }
}