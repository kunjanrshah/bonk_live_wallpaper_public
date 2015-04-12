package com.emirac.bonk;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class BonkWallpaper extends WallpaperService {
	
	public static final String SHARED_PREFS_NAME="bonksettings";

	private static final int MIN_NBONKS         = 2;
	private static final int MAX_AGE            = 4000;
	// after this many we start killing them off whenever
	//  we create a new one
	private static final int MAX_NBONKS         = 32;
	private static final int MAX_NBONKS_GLASS   = 8;
	// colors
	public static final int  BG_COLOR           = 0x000037;
	public static final int  FG_COLOR           = 0xc8c800;
	public static final int  WAVE_COLOR         = 0x7700aa;
	// radius limits
	private static final int MIN_RADIUS         = 12;
	private static final int MAX_RADIUS         = 50;	
	private static final int MIN_RADIUS_GLASS   = 20;
	private static final int MAX_RADIUS_GLASS   = 60;	
	// time between frames (msec)
	private static final int FRAME_INTERVAL     = 20;
	// fader alpha value (0 - 255)
	private static final int DEFAULT_ALPHA      = 70;
	

	private final Handler    mHandler = new Handler();
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		return new BonkEngine(this);
	}

	class BonkEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

	private final List<Bonk>    mBonks = new ArrayList<Bonk>();
	private final Paint         mPaint = new Paint();

	private Canvas              mCanvas          = null;
	public Bitmap               mBitmap          = null;
	private SoundManager        mSounds          = null;;
	private boolean             mVisible         = true;
	private float               mTouchDownY      = 0;
	private float               mTouchDownX      = 0;
	private float               mTouchUpX        = 0;
	private float               mTouchUpY        = 0;
	private int                 mHeight          = 0;
	private int                 mWidth           = 0;
	private float               mXOff            = 0f;
	private float               mYOff            = 0f;
	private int                 mXOffPix         = 0;
	private int                 mYOffPix         = 0;
	private WallpaperService    mContext         = null;;
	private SharedPreferences   mPrefs           = null;
	private int                 mMaxRadius       = MAX_RADIUS;
	private int                 mMinRadius       = MIN_RADIUS;
	private int                 mMaxNumBonks     = MAX_NBONKS;
	private int                 mMinNumBonks     = MIN_NBONKS;
	private int                 mBgColor         = BG_COLOR;
	private int                 mFgColor         = FG_COLOR;
	private int                 mWaveColor       = WAVE_COLOR;
	private boolean             mUseSound        = true;
	private boolean             mTrails          = false;    	
	private boolean             mShowBonks       = true;
	private int                 mBonkStyle       = 0;
	private String              mBgImageString   = null;
	private Bitmap              mBgBitmap        = null;
	private boolean             mUseBgImage      = false;

	private final Runnable mDoNextFrame = new Runnable() {
		public void run() {
			doNextFrame();
		}
	};

	BonkEngine(WallpaperService ws) {
//		android.os.Debug.waitForDebugger();
		final Paint paint = mPaint; 
		paint.setColor(0xffffffff);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(2);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStyle(Paint.Style.STROKE);
		mContext = ws;
		mSounds = new SoundManager(mContext);

		mPrefs = BonkWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(mPrefs, null);
	}

	@Override
	public void onCreate(SurfaceHolder surfaceHolder) {
		super.onCreate(surfaceHolder);
		setTouchEventsEnabled(true);
	}
	
	public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested){
		return (super.onCommand(action, x, y, z, extras, resultRequested));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSounds.off();
		mHandler.removeCallbacks(mDoNextFrame);
	}

	@Override
	public void onVisibilityChanged(boolean visible) {
		mVisible = visible;
		if (visible) {
			mSounds.on();
			doNextFrame();
		} 
		else {
			mSounds.off();
			mHandler.removeCallbacks(mDoNextFrame);
		}
	}

	@Override
	public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		super.onSurfaceChanged(holder, format, width, height);
		mWidth = width;
		mHeight = height;
		mCanvas = createCanvasBuffer(format, width, height);
		// make 3 bonks...so people don't freakout that "it doesn't do anything!"
		if(mBonks.size() == 0){
			mBonks.add(generateBonk(mWidth/2, mHeight/2, mWidth/2, mHeight/2));
			mBonks.add(generateBonk(mWidth/2, mHeight/2, mWidth/2, mHeight/2));
			mBonks.add(generateBonk(mWidth/2, mHeight/2, mWidth/2, mHeight/2));
		}
		loadBgBitmap();
		return;
	}

	@Override
	public void onSurfaceCreated(SurfaceHolder holder) {
		super.onSurfaceCreated(holder);
		return;
	}

	@Override
	public void onSurfaceDestroyed(SurfaceHolder holder) {
		super.onSurfaceDestroyed(holder);
		mVisible = false;
		mHandler.removeCallbacks(mDoNextFrame);
		return;
	}

	@Override
	public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
		mXOff = xOffset;
		mYOff = yOffset;
		if(mBgBitmap != null){
			mXOffPix = (int) (mXOff * (mWidth - mBgBitmap.getWidth()));
			mYOffPix = (int) (mYOff * (mHeight - mBgBitmap.getHeight()));
			if(mCanvas != null){
				mPaint.setAlpha(0xff);
				mCanvas.drawBitmap(mBgBitmap, mXOffPix, mYOffPix, mPaint);
			}
		}
		doNextFrame();
		return;
	}

	@Override
	public void onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			mTouchDownX = event.getX();
			mTouchDownY = event.getY();
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			mTouchUpX = event.getX();
			mTouchUpY = event.getY();
			Bonk b = generateBonk(mTouchDownX, mTouchDownY, mTouchUpX, mTouchUpY);
			mBonks.add(b);
		}
		super.onTouchEvent(event);
		return;
	}

	private Bonk generateBonk(float x1, float y1, float x2, float y2){
		Random rn = new Random();
		int r = (int) (mMaxRadius * rn.nextDouble());
		if(r <= mMinRadius){
			r = mMinRadius;
		}
		int m = r / 2;
		int vx = (int) (x2 - x1);
		int vy = (int) (y2 - y1);
		Bonk b = new Bonk((int)x1, (int)y1, r, m);
		setV(b, vx, vy, rn);
		return(b);
	}

	private void setV(Bonk b, int vx, int vy, Random r) {
		int[] signs = {-1, 1};
		vx /= (mWidth / 20);
		vy /= (mHeight / 20);
		if(vx == 0){
			vx = signs[r.nextInt(2)];
		}
		if((vx < 5) && (vx > -5)){
			vx *= 5;
		}
		if((vy < 5) && (vy > -5)){
			vy = (int) ((mHeight / 60) * r.nextDouble());
			vy *= signs[r.nextInt(2)];
		}
		b.setV(vx, vy);
		return;
	}

	Canvas createCanvasBuffer(int fmt, int w, int h){
		mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas newCanvas = new Canvas(mBitmap);
		return(newCanvas);
	}

	// do one frame of the animation
	void doNextFrame() {
		final SurfaceHolder holder = getSurfaceHolder();

		Canvas c = null;
		// check for collisions
		List<Bonk> collisionBonks = new ArrayList<Bonk>();
		List<Bonk> bonks2 = new ArrayList<Bonk>(mBonks);
		for(Bonk b: mBonks){
			b.collide(mWidth, mHeight); // bounce off the edges
			bonks2.remove(b);
			for(Bonk b2 : bonks2){
				int state = b2.collide(b);
				if((state == Bonk.START_COLLISION) && mUseSound){
					mSounds.play(b, b2, mMaxRadius, mMinRadius);
				}
				if(state != Bonk.NO_COLLISION){
					collisionBonks.add(b2);
				}
			}
		}
		// draw into the buffer
		//  if we need a bg image and haven't loaded it, then try to load it
		if(mUseBgImage  && (mBgImageString != null)){
			if(mBgBitmap == null){
				loadBgBitmap();
			}
		}
		// fade-erase
		if(mUseBgImage && (mBgBitmap != null)){
			mPaint.setAlpha(DEFAULT_ALPHA);
			mCanvas.drawBitmap(mBgBitmap, mXOffPix, mYOffPix, mPaint);
			mPaint.setAlpha(0xff);
		}
		else {
			mCanvas.drawARGB(DEFAULT_ALPHA,0,0,mBgColor);
			mPaint.setAlpha(0xff);
		}
		if(mCanvas != null){
			// update positions and draw
			//  flashes first
			for(Bonk b : mBonks){
				b.update();
				b.drawFlashes(mCanvas, mPaint, mWaveColor);
			}
			//  then draw bonks
			if(mTrails && mShowBonks){
				for(Bonk b : mBonks){
					b.draw(mCanvas, mPaint, mBitmap);
				}
			}
		}
		// copy the buffer to the screen canvas
		try {
			c = holder.lockCanvas();
			if (c != null) {
				c.drawBitmap(mBitmap, 0, 0, mPaint);
				if(mShowBonks && !mTrails){
					for(Bonk b : mBonks){
						b.draw(c, mPaint, mBitmap);
					}
				}
			}
		} 
		finally {
			if (c != null) holder.unlockCanvasAndPost(c);
		}
		// delete the bonks that are too old
		int nbonks = mBonks.size();
		if(nbonks > mMinNumBonks){
			int nToDelete = nbonks - mMinNumBonks;
			List<Bonk> toDeleteList = new ArrayList<Bonk>();			
			int ndeleted = 0;
			for(Bonk b: mBonks){
				if(b.getAge() > MAX_AGE){
					if(collisionBonks.contains(b)){
						toDeleteList.add(b);
						ndeleted++;
						if(ndeleted >= nToDelete){
							break;
						}
					}
				}
			}
			mBonks.removeAll(toDeleteList);
		}
		// delete the bonks that are too many
		nbonks = mBonks.size();
		if(nbonks > mMaxNumBonks){
			List<Bonk> toDeleteList = new ArrayList<Bonk>();
			int nToDelete = nbonks - mMaxNumBonks;
			Bonk[] bonks = mBonks.toArray(new Bonk[1]);
			int ndeleted = 0;
			for(int i = 0; i < nbonks; i++){
				if(collisionBonks.contains(bonks[i])){
					toDeleteList.add(bonks[i]);
					ndeleted++;
					if(ndeleted >= nToDelete){
						break;
					}
				}
			}
			mBonks.removeAll(toDeleteList);
		}
		// schedule the next frame
		mHandler.removeCallbacks(mDoNextFrame);
		if (mVisible) {
			mHandler.postDelayed(mDoNextFrame, FRAME_INTERVAL);
		}
		return;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(key != null){
			if(key.equals("soundOnOffPref")){
				changeSoundPref(prefs.getBoolean("soundOnOffPref", true));
			}
			else if(key.equals("trailsPref")){
				changeTrailsPref(prefs.getBoolean("trailsPref", false));
			}
			else if(key.equals("showCirclesPref")){
				changeShowBonksPref(prefs.getBoolean("showCirclesPref", false));
			}
			else if(key.equals("circleStylePref")){
				changeBonkStylePref(prefs.getString("circleStylePref", "0"));
			}
			else if(key.equals("bgColorPref")){
				changeBackgroundPref(prefs.getInt("bgColorPref", BG_COLOR));
			}
			else if(key.equals("fgColorPref")){
				changeForegroundPref(prefs.getInt("fgColorPref", FG_COLOR));
			}
			else if(key.equals("waveColorPref")){
				changeWavePref(prefs.getInt("waveColorPref", WAVE_COLOR));
			}
			else if(key.equals("soundStylePref")){
				changeSoundsPref(prefs.getString("soundStylePref", "0"));
			}
			else if(key.equals(BonkSettings.BG_IMAGE_KEY)){
				changeBgImagePref(prefs.getString(BonkSettings.BG_IMAGE_KEY, null));
			}
			else if(key.equals("useImagePref")){
				changeUseImagePref(prefs.getBoolean("useImagePref", false));
			}
		}
		else {
			changeSoundPref(prefs.getBoolean("soundOnOffPref", true));
			changeTrailsPref(prefs.getBoolean("trailsPref", false));
			changeShowBonksPref(prefs.getBoolean("showCirclesPref", true));
			changeBonkStylePref(prefs.getString("circleStylePref", "0"));
			changeBackgroundPref(prefs.getInt("bgColorPref", BG_COLOR));				
			changeForegroundPref(prefs.getInt("fgColorPref", FG_COLOR));
			changeWavePref(prefs.getInt("waveColorPref", WAVE_COLOR));
			changeSoundsPref(prefs.getString("soundStylePref", "0"));
			changeBgImagePref(prefs.getString(BonkSettings.BG_IMAGE_KEY, null));
			changeUseImagePref(prefs.getBoolean("useImagePref", false));
		}
		return;
	}

	private void changeWavePref(int value) {
		mWaveColor = value;
		Bonk.setFlashColor(mWaveColor);
		return;
	}

	private void changeBackgroundPref(int value) {
		mBgColor = value;
		Bonk.setBgColor(mBgColor);
		return;
	}

	private void changeForegroundPref(int value) {
		mFgColor = 0xff000000 | value;
		Bonk.setFgColor(mFgColor);
		return;
	}
	
	private void changeTrailsPref(boolean value){
		mTrails = value;
		return;
	}

	private void changeBonkStylePref(String value){
		mBonkStyle = Integer.parseInt(value);
		if(mBonkStyle == 2){ // glass
			mMinRadius = MIN_RADIUS_GLASS;
			mMaxRadius = MAX_RADIUS_GLASS;
			mMaxNumBonks = MAX_NBONKS_GLASS;
		}
		else{ // outline or solid
			mMinRadius = MIN_RADIUS;
			mMaxRadius = MAX_RADIUS;
			mMaxNumBonks = MAX_NBONKS;			
		}
		Bonk.setStyle(mBonkStyle);
		return;
	}

	private void changeSoundsPref(String value){
		mSounds.setScheme(value);
		return;
	}

	private void changeShowBonksPref(boolean value){
		mShowBonks = value;
		return;
	}

	private void changeSoundPref(boolean value){
		mUseSound = value;
		if(mUseSound){
			mSounds.on();
		}
		else{
			mSounds.off();
		}
		return;
	}
	
	private void changeBgImagePref(String value){
		mBgImageString = value;
		if(mUseBgImage){
			loadBgBitmap();
		}
		return;
	}

	private void loadBgBitmap() {
		Bitmap bmp = null;
		if((mBgImageString != null) && (mWidth > 0) && (mHeight > 0)){
			bmp = BonkUtil.imageFilePathToBitmap(mContext, mBgImageString, Math.max(mWidth, mHeight));
			if(bmp != null){
				mBgBitmap = scaleBgBitmap(bmp);
			}
			if(mBgBitmap != null){
				mXOffPix = (int) (mXOff * (mWidth - mBgBitmap.getWidth()));
				mYOffPix = (int) (mYOff * (mHeight - mBgBitmap.getHeight()));
			}
		}
		return;
	}

	private Bitmap scaleBgBitmap(Bitmap b) {
		Bitmap result = null;
		int bw = b.getWidth();
		int bh = b.getHeight();
		double s = (double)mHeight / (double)bh;
		int newW = (int)(bw * s);
		if(newW < mWidth){
			newW = mWidth;
		}
		result = Bitmap.createScaledBitmap(b, newW, mHeight, false);
		return(result);
	}

	private void changeUseImagePref(boolean value){
		mUseBgImage = value;
		if(!mUseBgImage){
			mBgBitmap = null;
		}
		return;
	}
}
}
