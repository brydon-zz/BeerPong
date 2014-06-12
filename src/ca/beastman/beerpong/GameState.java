package ca.beastman.beerpong;

import ca.beastman.beerpong.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.SensorEvent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GameState {

	public final int KEYBOARD = 1;
	public final int TOUCH = 2;
	public final int TILT= 4;

	private Path tri = new Path();
	private Drawable paddle1Image;
	private Drawable paddle2Image;
	private boolean paused = true;
	private boolean started = false;

	private final int MAX_TILT = 3;

	private boolean player1TouchDown = false;
	private int player1TouchDir = 0;
	private boolean player2TouchDown = false;
	private int player2TouchDir = 0;
	private boolean tiltDown = false;
	private float tiltDir = 0;

	private int _screenWidth;
	private int _screenHeight;
	//The ball
	private final int BALLSIZE = 10;
	private int _ballY;
	private int _ballX;
	private final float DEFAULTBALLVELOCITY = 3;
	private float _ballVelocityY;
	private float _ballVelocityX;
	//The bats
	private final int PADDLEHEIGHT = 76;
	private final int PADDLEWIDTH = 56;
	private final int PADDLE_EFFECTIVE_WIDTH = 10; // only first 10 pixels can "bounce" the ball.
	private int player1PaddleY ;
	private final int PADDLE_X_OFFSET = 20;
	private int player1PaddleX = PADDLE_X_OFFSET;
	private int player2PaddleY;	
	private int player2PaddleX;
	private float paddleSpeedRatio;
	private final float PADDLESPEED = (float) 4;
	private int paddleSmackCount=0;
	private final int PADDLE_COUNT_MAX = 3;

	private int player1Score;
	private int player2Score;

	private boolean twoPlayer = false;
	private int lastPointerCount = 0;

	public GameState(Context context) {
		Log.d("State","constructing");
		//get the screen width and height from the context
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		_screenHeight = metrics.heightPixels;
		_screenWidth = metrics.widthPixels;

		resetGame();
		//set up the pause triangle
		tri.moveTo(_screenWidth-80,20);
		tri.lineTo(_screenWidth-80,60);
		tri.lineTo(_screenWidth-40,40);
		tri.close();

		paddle1Image=context.getResources().getDrawable(R.drawable.beer_left);
		paddle2Image=context.getResources().getDrawable(R.drawable.beer_right);

	}

	public void resetGame() {
		//and set those parameters that rely on width and height
		paddleSpeedRatio = (float) 0.9;
		player1PaddleY = (_screenHeight/2) - (PADDLEWIDTH / 2);
		player2PaddleY = (_screenHeight/2) - (PADDLEWIDTH / 2);
		player2PaddleX = _screenWidth-PADDLE_X_OFFSET-PADDLEHEIGHT;
		_ballY = (_screenHeight - BALLSIZE)/2;
		_ballX = (_screenWidth - BALLSIZE)/2;

		_ballVelocityY = DEFAULTBALLVELOCITY;
		_ballVelocityX = DEFAULTBALLVELOCITY;

		player1Score=player2Score=0;

		started=false;
		paused = true;
	}

	//The update method
	public void update() {
		if (!paused) {
			//move the ball
			_ballY += _ballVelocityY;
			_ballX += _ballVelocityX;

			//DEATH!
			if(_ballX+BALLSIZE > _screenWidth) { // right side of ball out of screen
				player1Score++;
				resetBall();
			}  
			if (_ballX < 0) { // left side of ball out of screen
				player2Score++;
				resetBall();
			}

			if(_ballY+BALLSIZE > _screenHeight || _ballY < 0)
				//if bottom of ball out of screen or top of ball
				_ballVelocityY *= -1.1; 	//Hitting walls  	

			if (paddleSmackCount>0) paddleSmackCount--; // decrement paddleSmackCount
			//read comment below for paddleSC logic

			if(paddleSmackCount == 0 && _ballY+BALLSIZE > player1PaddleY && _ballY < player1PaddleY+PADDLEHEIGHT && _ballX < player1PaddleX+PADDLEWIDTH && _ballX > PADDLE_X_OFFSET+PADDLEWIDTH-PADDLE_EFFECTIVE_WIDTH) {
				// if it's 	x	is 	in	the	right	range					and the y is too				
				_ballVelocityX *= -1.1;  //Collisions with the top bat
				_ballX  = player1PaddleX + PADDLEWIDTH;
				paddleSmackCount = PADDLE_COUNT_MAX;
				// the idea with paddleSmackCount is to prevent it from when (at high speeds) the ball "enters" the bat.
				// and so multiple "smacks" by the paddle are reported (when it should just be one) thereby speeding the ball up
				// in an arbitrary direction.  This combats that situation by waiting a couple of game ticks after a paddle collision
				// before a new collision can be "reported" (the ball has to get alll the way across the court, after all.
			}

			if(paddleSmackCount == 0 && _ballY+BALLSIZE > player2PaddleY && _ballY < player2PaddleY+PADDLEHEIGHT && _ballX+BALLSIZE > player2PaddleX && _ballX < _screenWidth-PADDLE_X_OFFSET-PADDLEWIDTH+PADDLE_EFFECTIVE_WIDTH) {
				// if paddleSmackCount is 0 (so it's okay to bounce)
				// and the balls int the right place, bounce it!
				_ballVelocityX *= -1.1; // Bottom bat
				_ballX  = player2PaddleX - BALLSIZE;
				paddleSmackCount = PADDLE_COUNT_MAX; // and reset the pSC
			}

			if (player1TouchDown) { // the boolean for touch input
				if (player1TouchDir < 0) { // move the paddle up
					if (player1PaddleY - PADDLESPEED >= 0) player1PaddleY += player1TouchDir*PADDLESPEED; 
					// check to make sure we don't go out of top screen
				} else {
					//check to make sure we don't go out of bottom screen
					if (player1PaddleY + PADDLEHEIGHT + PADDLESPEED <= _screenHeight) player1PaddleY += player1TouchDir*PADDLESPEED;
				}
			} else if (tiltDown && !twoPlayer) { // allow touches to supercede tilt importance
				if (tiltDir < 0) { // same logic as above
					if (player1PaddleY + PADDLESPEED*tiltDir >= 0) player1PaddleY += tiltDir*PADDLESPEED;
				} else {
					if (player1PaddleY + PADDLEHEIGHT + PADDLESPEED*tiltDir <= _screenHeight) player1PaddleY += tiltDir*PADDLESPEED;
				}
			}

			//AI
			if (!twoPlayer) {
				if (player1Score-player2Score > 5 || _ballVelocityX > 0) { // difficulty settings basically
					if (_ballVelocityX < 0){
						if (player1Score-player2Score > 10) {
							paddleSpeedRatio = (float) 1.0;
						} else { 
							paddleSpeedRatio = (float) ((player1Score-player2Score)/10.);
						}
					} else {
						paddleSpeedRatio = (float) 1.0;
					}
					if (player2PaddleY > _ballY) {  // basically it just chases the y component of the ball
						// but only if the ball is either coming at us, or the other player is sufficiently winning
						if (player2PaddleY - PADDLESPEED*paddleSpeedRatio >= 0) // stay in screen! 
							player2PaddleY -= PADDLESPEED*paddleSpeedRatio;
					} else if (player2PaddleY < _ballY) {
						if (player2PaddleY + PADDLEHEIGHT + PADDLESPEED*paddleSpeedRatio <= _screenHeight)
							player2PaddleY += PADDLESPEED*paddleSpeedRatio;
					}
				}
			} else if (player2TouchDown) { // the boolean for touch input
				if (player2TouchDir < 0) { // move the paddle up
					if (player2PaddleY - PADDLESPEED >= 0) player2PaddleY += player2TouchDir*PADDLESPEED; 
					// check to make sure we don't go out of top screen
				} else {
					//check to make sure we don't go out of bottom screen
					if (player2PaddleY + PADDLEHEIGHT + PADDLESPEED <= _screenHeight) player2PaddleY += player2TouchDir*PADDLESPEED;
				}
			}
		}
	}

	public boolean keyPressed(int keyCode, KeyEvent msg) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			player1PaddleY += PADDLESPEED;
		}

		if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			player1PaddleY -= PADDLESPEED;
		}

		return true;
	}

	public void tilt(SensorEvent event) {
		if (event.values[0]<0) {
			tiltDown = true;
			tiltDir = (event.values[0]>-MAX_TILT) ? event.values[0] : -MAX_TILT;
		} else if (event.values[0]>0) {
			tiltDown = true;
			tiltDir = (event.values[0]<MAX_TILT) ? event.values[0] : MAX_TILT;
		} else {
			tiltDown = false;
			tiltDir = 0;
		}		
	}

	public void resetBall() {
		_ballY = (_screenHeight - BALLSIZE)/2; //place it in the middle
		_ballX = (_screenWidth - BALLSIZE)/2;

		_ballVelocityX = DEFAULTBALLVELOCITY; // undo the speed increases
		_ballVelocityY = DEFAULTBALLVELOCITY;

		_ballVelocityY *= (int) Math.round(Math.random())*2-1; // generate a random dir
		_ballVelocityX *= (int) Math.round(Math.random())*2-1;
	}

	public boolean touch(MotionEvent event,Boolean touchEnabled) {
		for (int i=0;i<event.getPointerCount();i++) {
			Log.d("Touch",i+": "+event.getX(i)+", "+event.getY(i)+"| "+((event.getAction() == MotionEvent.ACTION_UP)));
		}
		Log.d("Touch"," "+(event.getAction())+": "+MotionEvent.ACTION_MOVE);
		if (!started) {
			started = true;
			paused=false;
			return true;
		}
		if (twoPlayer) {
			if (event.getPointerCount() == 0) {
				player1TouchDir = 0;
				player2TouchDir = 0;
				player1TouchDown = false; 
				player2TouchDown = false;
			}
		}
		//pause click
		if (event.getAction() == MotionEvent.ACTION_DOWN && (event.getY()>10 && event.getY()<70 && event.getX() > _screenWidth-90 && event.getX()< _screenWidth-30))
			paused = !paused;
		//player touch
		if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
			if (touchEnabled) {
				if (twoPlayer) {
					for (int i=0;i<event.getPointerCount();i++) {
						if (event.getX(i)<_screenWidth/2) {
							player1TouchDown = true;
							player1TouchDir = (event.getY(i)>_screenHeight/2)? 1 : -1;
							if (event.getPointerCount()<lastPointerCount) {
								player2TouchDown = false;
								player2TouchDir = 0;
							}
						} else {
							player2TouchDown = true;
							player2TouchDir = (event.getY(i)>_screenHeight/2)? 1 : -1;
							if (event.getPointerCount()<lastPointerCount) {
								player1TouchDown = false;
								player1TouchDir = 0;
							}
						}
					}
				} else {
					player1TouchDown = true;
					player1TouchDir = (event.getY()>_screenHeight/2)? 1 : -1;	
				}
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (!twoPlayer) {
				player1TouchDown = false;
				player1TouchDir = 0;	
			} else {
				player1TouchDir = player2TouchDir = 0;
				player1TouchDown = player2TouchDown = false;
			}
		}
		lastPointerCount = event.getPointerCount();
		return true;
	}

	//the draw method
	public void draw(Canvas canvas, Paint paint) {


		//Clear the screen
		canvas.drawRGB(0, 0, 0);

		//	set the colour
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.WHITE);

		//Align the paddles
		paddle1Image.setBounds(new Rect(player1PaddleX, player1PaddleY, player1PaddleX + PADDLEWIDTH,
				player1PaddleY + PADDLEHEIGHT));
		paddle2Image.setBounds(new Rect(player2PaddleX, player2PaddleY, player2PaddleX + PADDLEWIDTH, 
				player2PaddleY + PADDLEHEIGHT));
		//Draw the paddles
		paddle1Image.draw(canvas);
		paddle2Image.draw(canvas);

		if (started) {  // bool that let's us know whether to draw a welcome screen or a game.
			//centre line
			canvas.drawRect(new Rect(_screenWidth/2-2,0,_screenWidth/2+2,_screenHeight), paint);
			//	draw the ball
			canvas.drawRect(new Rect(_ballX,_ballY,_ballX + BALLSIZE,_ballY + BALLSIZE),
					paint);

			if (!paused){ //draw the "pause" rectangles in the top right
				canvas.drawRect(new Rect(_screenWidth-80,20,_screenWidth-67,60),paint);
				canvas.drawRect(new Rect(_screenWidth-53,20,_screenWidth-40,60),paint);
			} else { // draw the "play" triangle in the top right
				canvas.drawPath(tri,paint);
			}
			// draw the scores
			canvas.drawText(""+player1Score,_screenWidth/2-27,20,paint);
			canvas.drawText(""+player2Score,_screenWidth/2+7,20,paint);
		} else { // draw the welcome screen
			paint.setTextSize(20);
			paint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText("Touch anywhere to start!",_screenWidth/2,(float)(0.9)*_screenHeight,paint);
			//the preceding 3 lines draw the welcome screen text 
			paint.setTextAlign(Paint.Align.LEFT);
			paint.setTextSize(40);
			canvas.drawText("Beer Pong", 50,50,paint);
			//these 3 lines draw the beer pong text in the top left
			paint.setTextSize(16);
			//just reset the paint to be used in the game.
		}	
	}

	/**
	 * Returns the current scores [topScore,botScore]
	 * @return [topScore,botScore]
	 */
	public int[] getScores() {
		return new int[] {player1Score,player2Score};
	}

	/**
	 * Returns player 1's location
	 * @return [x,y]
	 */
	public int[] getPlayer1Loc() {
		return new int[] {player1PaddleX,player1PaddleY};
	}

	/**
	 * Returns player 2's location
	 * @return [x,y]
	 */
	public int[] getPlayer2Loc() {
		return new int[] {player2PaddleX,player2PaddleY};
	}

	/**
	 * Returns ball's location
	 * @return [x,y]
	 */
	public int[] getBallLoc() {
		return new int[] {_ballX,_ballY};
	}

	/**
	 * Returns the balls velocity
	 * @return [vx,vy]
	 */
	public float[] getBallV() {
		return new float[] {_ballVelocityX,_ballVelocityY}; 
	}

	/**
	 * Returns whether or not the game has been paused
	 * @return paused
	 */
	public boolean getPaused() {
		return paused;
	}

	/**
	 * Sets the balls velocity
	 * @param [vx,vy]
	 */
	public void setBallV(float[] bv) {
		_ballVelocityX = bv[0];
		_ballVelocityY = bv[1];
	}

	/**
	 * Set's the paused to the value of p
	 * @param p
	 */
	public void setPaused(boolean p) {
		paused = p;
		started = true;
	}

	/**
	 * set's the balls location
	 * @param [x,y]
	 */
	public void setBallLoc(int[] loc) {
		_ballX = loc[0];
		_ballY = loc[1];
	}

	/**
	 * Set's the scores
	 * @param [topScore,bottomScore]
	 */
	public void setScores(int[] scores){
		player1Score = scores[0];
		player2Score = scores[1];
	}

	/**
	 * Set's player i's location 
	 * @param i,[x,y]
	 */
	public void setPlayerLoc(int i, int[] loc) {
		if (i == 1) {
			player1PaddleX = loc[0];
			player1PaddleY = loc[1];
		}
		else if (i == 2) {
			player2PaddleX = loc[0];
			player2PaddleY = loc[1];
		}
	}

	/**
	 * Stop's touch, tilt, or keyboard controls,
	 * the argument i coincides with constants in both the GameState and GameThread classes.
	 * @param i
	 */
	public void stopControl(int i) {
		if (i == TILT) {
			tiltDir =0;
			tiltDown = false;
		} else if (i == TOUCH || i == KEYBOARD) {
			player1TouchDown = false;
			player1TouchDir = 0;
		}
	}

	/**
	 * Set's the two player boolean to the value of tp
	 * @param tp
	 */
	public void setTwoPlayer(boolean tp) {
		twoPlayer = tp;
	}

	public boolean getTwoPlayer() {
		return twoPlayer;
	}
}