package ca.beastman.beerpong;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class GameView extends SurfaceView  implements SurfaceHolder.Callback, SensorEventListener
{
   private GameThread _thread;
   
   private SensorManager mSensorManager;

   private Sensor mAccelerometer;
   
   private boolean resetValues = false;
   private int[] player1;
   private int[] player2;
   private int[] ball;
   private int[] score;
   private float[] ballV; 
   private boolean paused;
   private boolean twoPlayer;

   public final int KEYBOARD = 1;
   public final int TOUCH = 2;
   public final int TILT= 4;
	
   private boolean keyBoard = false;
   private boolean touch = true;
   private boolean tilt = false;
   
   Context appContext;
   public GameView(Context context, AttributeSet attrs) {
       super(context, attrs);
       appContext = context;
   
       SurfaceHolder holder = getHolder();	//So we can listen for events...
       holder.addCallback(this);
       setFocusable(true); 

       
       //sensor
   		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

   		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

   		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
   }  

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent msg) {
       if (keyBoard) return _thread.getGameState().keyPressed(keyCode, msg);
       else return true;
   }
   
   @Override
   public boolean onTouchEvent(MotionEvent event) {
       return _thread.getGameState().touch(event,touch);
       
   }
   
   public void qToast(String text) {
		Toast.makeText(appContext,text, Toast.LENGTH_SHORT).show();
	}
   
   public void setValues(int[] p1, int[] p2, int[] s, int[] b, float[] bv, boolean pause, boolean[] settings) {
	   resetValues = true;
	   player1 = p1;
	   player2 = p2;
	   score = s;
	   ball = b;
	   ballV = bv;
	   paused = pause;
	   keyBoard = settings[0];
	   touch = settings[1];
	   tilt = settings[2];
	   twoPlayer = settings[3];
   }
   
   //Implemented as part of the SurfaceHolder.Callback interface
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

   //Implemented as part of the SurfaceHolder.Callback interface
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//and instantiate the thread
		Log.d("View","SurfaceCreated");
	    _thread = new GameThread(holder, getContext(), new Handler());
	    if (resetValues) {
	    	_thread.getGameState().setBallLoc(ball);
	    	_thread.getGameState().setScores(score);
	    	_thread.getGameState().setPlayerLoc(1,player1);
	    	_thread.getGameState().setPlayerLoc(2,player2);
	    	_thread.getGameState().setBallV(ballV);
	    	_thread.getGameState().setPaused(paused);
	    	_thread.getGameState().setTwoPlayer(twoPlayer);
	    	resetValues = false;
	    }
		_thread.setRunning(true);
		_thread.start();
	}

   //Implemented as part of the SurfaceHolder.Callback interface
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
       _thread.setRunning(false);
       
	}

	/*
	 * Sensor fucking shit
	 */
	
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent arg0) {
		// TODO Auto-generated method stub
		if (_thread != null && tilt) _thread.getGameState().tilt(arg0);
	}
	
	public GameThread getThread() {
		return _thread;
	}
	
	public void addInput(int i) {
		Log.d("View",""+i);
		if (i == KEYBOARD) {
			keyBoard = true;
		} else if (i == TOUCH) {
			touch = true;
		} else if (i == TILT) {
			tilt = true;
		} else if (i == -KEYBOARD) {
			keyBoard = false;
			getThread().getGameState().stopControl(KEYBOARD);
		} else if (i == -TOUCH) {
			touch = false;
			getThread().getGameState().stopControl(TOUCH);
		} else if (i == -TILT) {
			tilt = false;
			getThread().getGameState().stopControl(TILT);
		}
	}
	
	public int numberOfControlsSelected() {
		return (keyBoard?1:0)+(tilt?1:0)+(touch?1:0); 
	}
	
	public boolean[] getSettings() {
		return new boolean[] {keyBoard,touch,tilt,_thread.getGameState().getTwoPlayer()};
	}
}
