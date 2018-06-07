package com.devicestats.android;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static String filename = "devicestats.txt";

    private DeviceStats mDeviceStats;
    private String mDeviceStatsString;
    private ViewPager mViewPager;
    private WallpaperObserver mWallpaperReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initStats();
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mWallpaperReceiver);
    }

    private void initStats() {
        //TODO - move to background thread
        StatsGatherer gatherer = new StatsGatherer(this);
        mDeviceStatsString = gatherer.printStatsToString();
        mDeviceStats = gatherer.getDeviceStats();
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        DeviceStatsAdapter adapter = new DeviceStatsAdapter(this, getSupportFragmentManager(), mDeviceStats);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setAdapter(adapter);

        tabLayout.setTabsFromPagerAdapter(adapter);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        //TODO - maybe move this share back to action bar menu as does get in the way of data
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareAllDeviceStatsText();
            }
        });

        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        mWallpaperReceiver = new WallpaperObserver();
        registerReceiver(mWallpaperReceiver, filter);
    }

    private void shareAllDeviceStatsText() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mDeviceStatsString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.default_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long id = item.getItemId();
        if (R.id.menu_screenshot == id) {
            setScreenAsWallpaper();
            return true;
        } else if (R.id.menu_save_to_sd == id) {
            try {
                Utils.dumpDataToSD(filename, mDeviceStatsString);
                Toast.makeText(getApplicationContext(), "Saved to " + filename,
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Failed to save.",
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void setScreenAsWallpaper() {
        WallpaperTask wallpaperTask = new WallpaperTask(this);
        wallpaperTask.execute();
    }

    public Bitmap takeScreenShot(View view) {
        view = getWindow().getDecorView().getRootView();

        view.setDrawingCacheEnabled(true);
        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        view.buildDrawingCache();

        if(view.getDrawingCache() == null) return null;

        Bitmap snapshot = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        view.destroyDrawingCache();

        return snapshot;
    }

    private class WallpaperObserver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Wallpaper set", Toast.LENGTH_LONG).show();
        }
    }

    private class WallpaperTask extends AsyncTask<Void, Void, String> {

        private Context context;

        public WallpaperTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Bitmap bitmap = takeScreenShot(mViewPager);
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
            try {
                wallpaperManager.setBitmap(bitmap);
            } catch (IOException e) {
                return "Error setting Wallpaper";
            }
            return null;
        }

        @Override
        protected void onPostExecute(String errorString) {
            super.onPostExecute(errorString);
            if (errorString != null) {
                Toast.makeText(context, errorString, Toast.LENGTH_LONG).show();
            }
        }
    }
}
