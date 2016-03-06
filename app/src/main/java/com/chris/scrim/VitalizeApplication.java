package com.chris.scrim;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by chris on 2/21/2016.
 */
public class VitalizeApplication extends Application {
    private static int MAX_ID = 1000;
    //events should be removed 1 hour after start time
    private static final int HOUR_LIMIT = 1;
    private static DBHelper dbHelper;
    private static Map<String, Integer> typeToMarkerImage;
    private static Map<String, Integer> typeToTypeImage;
    private static  final String[] types = {"Basketball", "Football", "Frisbee", "Soccer", "Tennis", "Volleyball"};
    private static List<ScrimArea> allAreas;
    //locally
    public static synchronized int getUniqueId() {
        Random random = new Random();
        int n = random.nextInt(MAX_ID);
        while(!isUniqueId(n)) {
              random.nextInt(MAX_ID);
        }
        return n;
    }

    private static boolean isUniqueId(int id) {
        for(ScrimArea ariana: allAreas) {
            if(ariana.getId() == id) {
                return false;
            }
        }
        return true;
    }

    public static List<ScrimArea> getAllAreas() {
        return allAreas;
    }
    public static String[] getTypes() {
        return types;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeMaps();
        dbHelper = new DBHelper(this);
        allAreas = dbHelper.getAllScrimAreas();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity,
                                          Bundle savedInstanceState) {
                // new activity created; force its orientation to portrait
                activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }
        });
    }

    public static int getMarkerImage(String type) {
        return typeToMarkerImage.get(type);
    }
    public static int getTypeImage(String type) {
        return typeToTypeImage.get(type);
    }
    private static void initializeMaps() {
        final int[] markerImages = {R.drawable.basketball_marker, R.drawable.football_marker, R.drawable.frisbee_marker,
                R.drawable.soccer_marker, R.drawable.tennis_marker, R.drawable.volleyball_marker};
        final int[] typeImages = {R.drawable.basketball, R.drawable.football,
                R.drawable.frisbee, R.drawable.soccer, R.drawable.tennis,
                R.drawable.volleyball};
        typeToMarkerImage = new HashMap<>();
        typeToTypeImage = new HashMap<>();
        for(int k=0; k<types.length; k++) {
            typeToMarkerImage.put(types[k], markerImages[k]);
            typeToTypeImage.put(types[k], typeImages[k]);
        }
    }
    public static void removeAreaPassTimeLimit() {
//        Log.d("vitalize", String.valueOf(allAreas.size()));
//        SimpleDateFormat format = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a");
        int i = 0;
        while(i < allAreas.size()) {
            Calendar exist = allAreas.get(i).getDate();
            exist.add(Calendar.HOUR_OF_DAY, HOUR_LIMIT);
            Calendar temp = Calendar.getInstance();
            if(temp.after(exist)) {
                dbHelper.removeScrimAreaDB(allAreas.get(i).getId());
                allAreas.remove(i);
            } else  {
                i ++;
            }
        }
    }
}
