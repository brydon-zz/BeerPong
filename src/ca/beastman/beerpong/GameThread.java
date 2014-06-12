package ca.beastman.beerpong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

	/** Handle to the surface manager object we interact with */
	private SurfaceHolder _surfaceHolder;
	private Paint _paint;
	private GameState _state;
	private boolean running;
	private boolean started = false;
	
	private static final String TAG = "GameThread";
	private final int FPS = 50;
	private final int SKIP_FRAMES = 1000/FPS;
	private final int MAX_FRAMESKIP = 10;
	
	
	private final long startUp = System.currentTimeMillis();
	private long nextGameTick = System.currentTimeMillis()-startUp;
	private int loops;
	
	public GameThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
		_surfaceHolder = surfaceHolder;
		_paint = new Paint();
		_state = new GameState(context);
	}

	@Override
	public void run() {
		started = true;
		while(running) {
			loops = 0;
			Canvas canvas = _surfaceHolder.lockCanvas();
			if (canvas != null) {
				while (System.currentTimeMillis()-startUp > nextGameTick && loops < MAX_FRAMESKIP) {
						_state.update();
						nextGameTick += SKIP_FRAMES;
						loops++;
				}
				_state.draw(canvas,_paint);
				_surfaceHolder.unlockCanvasAndPost(canvas);
			} else Log.d(TAG,"NULLCANVAS");
		}
	}

	public GameState getGameState() {
		return _state;
	}
	
	public void setRunning(boolean cont) {
		running = cont;
	}
	
	public boolean isStarted() {
		return started;
	}
	
}