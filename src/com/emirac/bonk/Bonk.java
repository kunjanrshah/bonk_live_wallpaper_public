package com.emirac.bonk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import com.emirac.bonk.bubbles.Bubbles;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

public class Bonk {

	public static final int        NO_COLLISION       = 0x00;
	public static final int        START_COLLISION    = 0x01;
	public static final int        IN_COLLISION       = 0x02;

	private static int             mFgColor          = BonkWallpaper.FG_COLOR;
	private static int             mBgColor          = BonkWallpaper.BG_COLOR;
	private static int             mFlashColor       = BonkWallpaper.WAVE_COLOR;
	private static int             mStyle            = 0;
	private static float[]         pos               = {0.6f, 1.0f};
	private static int[]           colors            = {0, 0};
	
	private Set<Bonk>              collisions        = new HashSet<Bonk>();
	private List<FlashPoint>       flashes           = new ArrayList<FlashPoint>();
	private List<FlashPoint>       doneFlashes       = new ArrayList<FlashPoint>();
	private int                    age               = 0;
	private double                 centerX           = 0; 
	private double                 centerY           = 0;
	private double                 vX                = 0;
	private double                 vY                = 0;
	private double                 radius            = 0;
	private double                 mass              = 0;
	private	int                    mDrawCounter      = 0;
		
	public Bonk(int x, int y){
		this(x, y, 10);
		return;
	}
	public Bonk(int x, int y, int r){
		this(x, y, r, 1);
		return;
	}
	public Bonk(int x, int y, int r, int m){
		vX = 0;
		vY = 0;
		centerX = x;
		centerY = y;
		radius = r;
		mass = m;
		age = 0;
		return;
	}
		
	public static void setFgColor(int c){
		mFgColor = c;
		colors[0] = mFgColor; 
		return;
	}
	
	public static void setBgColor(int c){
		mBgColor = c;
		colors[1] = mBgColor; 
		return;
	}
	
	public static void setFlashColor(int c){
		mFlashColor = c;
		return;
	}
	
	public static void setStyle(int s){
		mStyle = s;
	}

	public void setV(double x, double y){
		vX = x;
		vY = y;
		return;
	}
	
	public int getAge(){
		return(age);
	}
	
	public double getVX(){
		return(vX);
	}
	
	public double getVY(){
		return(vY);
	}
	
	public double getR() {
		return(radius);
	}

	public double getMass() {
		return(mass);
	}
	
	// returns one of {NO_COLLISION, START_COLLISION, IN_COLLISION}
	//  NO_COLLISION ==> this bonk doesnt overlap input bonk
	//  START_COLLISION ==> this bonk didn't overlap input bonk last time
	//   we checked, but it does now
	//  IN_COLLISION ==> this bonk overlaps input bonk, and it did the
	//   last time we checked
	public int collide(Bonk b){
		int state = Bonk.NO_COLLISION;
		double x = (centerX - b.centerX);
		double y = (centerY - b. centerY);
		double br = b.getR();
		double sqd = (x * x) + (y * y);  // distance between centers
		double sqc = (radius + br) * (radius + br);  // total distance of both radii
		// if they collide then...
		if((sqd) <= (sqc)){
			if(collisions.add(b)){
				state = Bonk.START_COLLISION;
				b.addCollision(this);
				flashes.add(new FlashPoint(this, mFgColor, mFlashColor));
				b.addFlash();
				// do the conservation-of-momentum thing
				double bvX = b.getVX();
				double bvY = b.getVY();
				double bmass = b.getMass();
				double tmpX0;
				double tmpY0;
				double tmpX1;
				double tmpY1;
				tmpX0 = ((vX * (mass - bmass)) + (2 * bmass * bvX)) / (mass + bmass);
				tmpY0 = ((vY * (mass - bmass)) + (2 * bmass * bvY)) / (mass + bmass);
				tmpX1 = ((bvX * (bmass - mass)) + (2 * mass * vX)) / (mass + bmass);
				tmpY1 = ((bvY * (bmass - mass)) + (2 * mass * vY)) / (mass + bmass);
				setV(tmpX0, tmpY0);
				b.setV(tmpX1, tmpY1);
			}
			else{
				state = Bonk.IN_COLLISION;
			}
		}
		else{
			state = Bonk.NO_COLLISION;
			collisions.remove(b);
			b.removeCollision(this);
		}
		return(state);
	}
	
	// returns true if this bonk hits the edge of the given rectangle
	public boolean collide(int w, int h){
		boolean hit = false;
		if((centerX + radius) >= w){
			centerX = w - radius;
			setV(-vX, vY);
			hit = true;
		}
		else if ((centerX - radius) <= 0){
			centerX = radius;
			setV(-vX, vY);
			hit = true;
		}
		if((centerY + radius) >= h){
			centerY = h - radius;
			setV(vX, -vY);
			hit = true;
		}
		else if((centerY - radius ) <= 0){
			centerY = radius;
			setV(vX, -vY);
			hit = true;
		}
		return(hit);
	}

	public void draw(Canvas c, Paint p, Bitmap bgBitmap){
		int r = (int)radius;
		// pulse the radius
		if(flashes.size() > 0){
			switch(mDrawCounter){
				case 0:
					radius -= 2;
					mDrawCounter++;
					break;
				case 1:
					radius += 2;
					mDrawCounter = 0;
					break;
			}
		}
		// draw depending on style
		switch(mStyle){
			case 0: // outline
				p.setColor(mFgColor);
				c.drawCircle((int)centerX, (int)centerY, (int)r, p);
				break;
			case 1: // solid
				p.setShader(new RadialGradient((int)centerX, (int)centerY, (int)r, colors, pos, Shader.TileMode.CLAMP));
				p.setStyle(Paint.Style.FILL);
				p.setColor(mFgColor);
				c.drawCircle((int)centerX, (int)centerY, (int)r, p);
				p.setStyle(Paint.Style.STROKE);
				p.setShader(null);
				break;
//			case 2: // glass
//				Bubbles.draw(centerX, centerY, radius, c, p, bgBitmap);
//				break;
		}
		return;
	}
		
	public void drawFlashes(Canvas c, Paint p, int fcolor){
		if(flashes.size() > 0){
			float sw = p.getStrokeWidth();
			Paint.Style s = p.getStyle();
			int cp = p.getColor();
			for(FlashPoint fp : flashes){
				if(fp.draw(c, p, fcolor)){
					doneFlashes.add(fp);
				}
			}
			flashes.removeAll(doneFlashes);
			doneFlashes.clear();
			p.setStrokeWidth(sw);
			p.setColor(cp);
			p.setStyle(s);
		}
		return;
	}
	
	public boolean addCollision(Bonk b){
		return(collisions.add(b));
	}
	
	public boolean collidesWith(Bonk b){
		return(collisions.contains(b));
	}

	public boolean removeCollision(Bonk b){
		return(collisions.remove(b));
	}

	public void addFlash(){
		flashes.add(new FlashPoint(this, mFgColor, mFlashColor));
		return;
	}
	
	public double centerSqDistance(Bonk b){
		double dx = centerX - b.getCenterX();
		double dy = centerY - b.getCenterY();
		return((dx * dx) + (dy * dy));
	}
	
	public double getCenterY() {
		return(centerY);
	}
	
	public double getCenterX() {
		return(centerX);
	}
	
	public void update() {
		age++;
		centerX += vX;
		centerY += vY;
		return;
	}
}
