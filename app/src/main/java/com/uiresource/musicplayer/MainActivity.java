package com.uiresource.musicplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import me.crosswall.lib.coverflow.CoverFlow;
import me.crosswall.lib.coverflow.core.PagerContainer;


public class MainActivity extends AppCompatActivity {

    // UI
    public int[] covers = {R.drawable.black_eye_peas, R.drawable.calum, R.drawable.charlie_puth, R.drawable.ed_sheeran, R.drawable.rihanna, R.drawable.shawn };
    public String[] song_title = {"The Time", "You Are The Reason", "Done For Me","Perfect", "Wild Thoughts", "In My Blood" };
    public String[] singers = {"Black Eye Peas", "Calum Scott", "Charlie Puth","Ed Sheeran", "Rihanna", "Shawn Mendes" };
    public boolean[] stars = {false,false,false,false,false,false};
    // END UI
    public static int[] MEDIAS_RES_ID =  {R.raw.the_time,R.raw.you_are_the_reason,R.raw.done_for_me,R.raw.perfect,R.raw.wild_thoughts,R.raw.in_my_blood};

    public static final String TAG = "MainActivity";
    private boolean VALID_FIRST_CLICK = false;


    public static boolean CONTINOUS_PLAY = false;

    public int index;
    private SeekBar mSeekbarAudio;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private TextView tv_current_time;
    private TextView tv_full_time;
    private TextView tv_artist;
    private TextView tv_song;
    private ToggleButton starButton;
    private ToggleButton continousPlayButton;
    private ImageView mPlayButton;
    private ImageView mShuffleButton;
    private UpdateTimer updateTimer;
    private ViewPager pager;
    private View root;

    private void UpdateCoverFlow(){
        PagerContainer container = (PagerContainer) findViewById(R.id.pager_container);
        pager = container.getViewPager();
        pager.setAdapter(new MainActivity.MyPagerAdapter());
        pager.setClipChildren(false);
        pager.setOffscreenPageLimit(15);

        boolean showTransformer = getIntent().getBooleanExtra("showTransformer",true);

        if(showTransformer){
            new CoverFlow.Builder()
                    .with(pager)
                    .scale(0.3f)
                    .pagerMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin))
                    .spaceSize(0f)
                    .build();

        }else{
            pager.setPageMargin(30);
        }
        OnNextClick(null);
        OnPreviousClick(null);
        if (CONTINOUS_PLAY){
            mPlayButton.setImageResource(R.drawable.ic_pause);
            if(updateTimer == null){
                updateTimer = new UpdateTimer();
                updateTimer.start();
            }else if (!updateTimer.isAlive()){
                updateTimer = new UpdateTimer();
                updateTimer.start();
            }
        }
    }

    private void SetCorrectFormatTime(TextView textView, int duration){
        String time_in_format = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
        textView.setText(time_in_format);
    }

    private void Shuffle(){
        ArrayList<ArrayList<Object>> arrayLists = new ArrayList<ArrayList<Object>>();
        for (int i = 0; i < covers.length; i++){
            ArrayList<Object> objectArrayList = new ArrayList<Object>();
            objectArrayList.add(covers[i]);
            objectArrayList.add(song_title[i]);
            objectArrayList.add(singers[i]);
            objectArrayList.add(stars[i]);
            objectArrayList.add(MEDIAS_RES_ID[i]);
            arrayLists.add(objectArrayList);
        }
        Collections.shuffle(arrayLists);
        int[] new_covers = new int[covers.length];
        String[] new_song_title = new String[song_title.length];
        String[] new_singers = new String[singers.length];
        boolean[] new_stars = new boolean[stars.length];
        int[] new_medias = new int[MEDIAS_RES_ID.length];
        for (int i = 0; i < covers.length; i++){
            ArrayList<Object> objectArrayList = arrayLists.get(i);
            new_covers[i] = (int)objectArrayList.get(0);
            new_song_title[i] = (String) objectArrayList.get(1);
            new_singers[i] = (String) objectArrayList.get(2);
            new_stars[i] = (boolean) objectArrayList.get(3);
            new_medias[i] = (int) objectArrayList.get(4);
        }
        Log.i(TAG,"Just to see how shuffle gone!");
        if(mPlayerAdapter.isPlaying()){
            mPlayerAdapter.reset();
            mPlayerAdapter.release();
            mPlayButton.setImageResource(R.drawable.ic_play);
        }
        covers = new_covers;
        song_title = new_song_title;
        singers = new_singers;
        stars = new_stars;
        MEDIAS_RES_ID = new_medias;
        UpdateCoverFlow();
    }

    public void OnNextClick(View view){
        int current = pager.getCurrentItem();
        if (current == (covers.length - 1)){
            if(CONTINOUS_PLAY){
                CONTINOUS_PLAY = false;
                mPlayButton.setImageResource(R.drawable.ic_play);
                pager.setCurrentItem(0, true);
                continousPlayButton.setChecked(false);
            }

        }else{
            int next = current + 1;
            pager.setCurrentItem(next, true);
        }
    }

    public void OnPreviousClick(View view){
        int current = pager.getCurrentItem();
        int previous = current - 1;
        pager.setCurrentItem(previous, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        index = 0;
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        Log.d(TAG, "onCreate: finished");

        // UI
        initializeCoverFlow();
    }

    class UpdateTimer extends Thread {

        public void run() {
           while(mPlayerAdapter.isPlaying()){
               try{
                   sleep(1000);
               }catch (Exception e){
                   Log.e(TAG,"catch clause in thread");
               }
               String sDate = (String) tv_current_time.getText();
               SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
               try {
                   String myTime = (String)tv_current_time.getText();
                   String myFullTime = (String)tv_full_time.getText();

                   SimpleDateFormat df = new SimpleDateFormat("mm:ss");

                   Date d = df.parse(myTime);
                   Date full_time = df.parse(myFullTime);

                   Calendar cal = Calendar.getInstance();
                   Calendar cal_full = Calendar.getInstance();

                   cal.setTime(d);
                   cal_full.setTime(full_time);

                   int result = cal.compareTo(cal_full);
                   if (result < 0){
                       cal.add(Calendar.SECOND, 1);
                       String newTime = df.format(cal.getTime());
                       if(mPlayerAdapter.isPlaying())
                           tv_current_time.setText(newTime);
                   }
               }catch (Exception e){
                e.getStackTrace();
               }
           }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlayerAdapter.loadMedia(MEDIAS_RES_ID[0]);
        Log.d(TAG, "onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        View view = findViewById(R.id.activity_main);
        root = view.getRootView();
        Palette palette = Palette.from(drawableToBitmap(covers[0])).generate();
        setStatusBarAndBackground(palette);
        starButton = (ToggleButton) findViewById(R.id.star);
        starButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    stars[index] = true;
                } else {
                    stars[index] = false;
                }
            }
        });

        mShuffleButton = (ImageView) findViewById(R.id.shuffle);
        mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Shuffle();
            }
        });

        continousPlayButton = (ToggleButton) findViewById(R.id.continous_play);
        continousPlayButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    CONTINOUS_PLAY = true;
                } else {
                    CONTINOUS_PLAY = false;
                }
            }
        });

        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mPlayButton = (ImageView) findViewById(R.id.play_pause);
        tv_current_time = (TextView) findViewById(R.id.tv_current_time);
        tv_full_time = (TextView) findViewById(R.id.tv_full_time);

        tv_artist = (TextView) findViewById(R.id.tv_artist);
        tv_song = (TextView) findViewById(R.id.tv_song);

        tv_artist.setText(singers[0]);
        tv_song.setText(song_title[0]);

        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        OnStartAndPauseClick();
                    }
                });
    }

    private void OnStartAndPauseClick(){
        if(mPlayerAdapter.isPlaying()){
            mPlayerAdapter.pause();
            try{
                updateTimer.interrupt();
            }
            catch (Exception e){
                e.getStackTrace();
            }
            mPlayButton.setImageResource(R.drawable.ic_play);
        }else{
            mPlayerAdapter.play();
            updateTimer = new UpdateTimer();
            updateTimer.start();
            mPlayButton.setImageResource(R.drawable.ic_pause);
        }
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                            SetCorrectFormatTime(tv_current_time,userSelectedPosition);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                        SetCorrectFormatTime(tv_current_time,userSelectedPosition);
                    }
                });
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            SetCorrectFormatTime(tv_full_time,duration);
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
            mPlayerAdapter.reset();
            tv_current_time.setText("00:00");
            if (CONTINOUS_PLAY){
                OnNextClick(null);
            }else{
                mPlayButton.setImageResource(R.drawable.ic_play);
            }
        }
    }


    ////////////////////////////  UI SCENE  /////////////////////////////////

    private void initializeCoverFlow(){
        PagerContainer container = (PagerContainer) findViewById(R.id.pager_container);
        pager = container.getViewPager();
        pager.setAdapter(new MainActivity.MyPagerAdapter());
        pager.setClipChildren(false);
        //
        pager.setOffscreenPageLimit(15);

        boolean showTransformer = getIntent().getBooleanExtra("showTransformer",true);

        if(showTransformer){

            new CoverFlow.Builder()
                    .with(pager)
                    .scale(0.3f)
                    .pagerMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin))
                    .spaceSize(0f)
                    .build();

        }else{
            pager.setPageMargin(30);
        }

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tv_song.setText(song_title[position]);
                tv_artist.setText(singers[position]);
                index = position;
                starButton.setChecked(stars[index]);

                mPlayerAdapter.release();
                mPlayerAdapter.loadMedia(MEDIAS_RES_ID[position]);
                if(!CONTINOUS_PLAY)
                    mPlayButton.setImageResource(R.drawable.ic_play);
                else{
                    mPlayerAdapter.play();
                    mPlayButton.setImageResource(R.drawable.ic_pause);
                }

                try{
                    updateTimer.interrupt();
                }
                catch (Exception e){
                    e.getStackTrace();
                }

                RelativeLayout relativeLayout = (RelativeLayout) pager.getAdapter().instantiateItem(pager, 0);
                ViewCompat.setElevation(relativeLayout.getRootView(), 8.0f);
                Palette palette = Palette.from(drawableToBitmap(covers[position])).generate();
                setStatusBarAndBackground(palette);
                tv_current_time.setText("00:00");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private class MyPagerAdapter extends PagerAdapter {

        private class OnFirstClick extends Thread {

            public void run() {
                try {
                    sleep(700);
                } catch (Exception e) {
                    Log.e(TAG, "catch clause in thread");
                }
                VALID_FIRST_CLICK = false;
            }
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_cover,null);
            SquareImageView imageView = (SquareImageView) view.findViewById(R.id.image_cover);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(VALID_FIRST_CLICK){
                        Log.i(TAG,"Second Click valid");
                        OnStartAndPauseClick();
                    }else{
                        OnFirstClick onDoubleClick = new OnFirstClick();
                        VALID_FIRST_CLICK = true;
                        onDoubleClick.start();
                        Log.i(TAG,"FIRST CLICK");
                    }
                }
            });
            imageView.setImageDrawable(getResources().getDrawable(covers[position]));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public int getCount() {
            return covers.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return (view == object);
        }
    }

    public void setStatusBarAndBackground(Palette palette){
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Palette.Swatch vibrant = palette.getDominantSwatch();
            if (vibrant != null) {
                window.setStatusBarColor(vibrant.getRgb());
                root.setBackgroundColor(vibrant.getRgb());
            }
        }
    }

    public Bitmap drawableToBitmap(int id) {
        Bitmap bitmap = null;
        Drawable drawable = getResources().getDrawable(id);
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
