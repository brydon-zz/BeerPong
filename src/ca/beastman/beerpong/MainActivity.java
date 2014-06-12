package ca.beastman.beerpong;

import ca.beastman.beerpong.R;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	private Bundle sis;
	private static final String SCORES = "scores";
	private static final String PLAYER2LOC = "player2Loc";
	private static final String PLAYER1LOC = "payer1Loc";
	private static final String BALLLOC = "ballLoc";
	private static final String BALLVELOCITY = "ballVelocity";
	private static final String PAUSED = "paused";
	private static final String SETTINGS = "settings";
	
	protected GameView gv;
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("Main","onCreate");
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_main);
		
		gv = (GameView) findViewById(R.id.GameView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	

	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		Log.d("Main","restore");
		super.onRestoreInstanceState(inState);
	}
	
	@Override
	public void onBackPressed() {
		makeExitDialog();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle inState) {
		Log.d("Main","save");
		super.onSaveInstanceState(inState);
		inState.putIntArray(SCORES, gv.getThread().getGameState().getScores());
		inState.putIntArray(PLAYER1LOC, gv.getThread().getGameState().getPlayer1Loc());
		inState.putIntArray(PLAYER2LOC, gv.getThread().getGameState().getPlayer2Loc());
		inState.putIntArray(BALLLOC, gv.getThread().getGameState().getBallLoc());
		inState.putFloatArray(BALLVELOCITY, gv.getThread().getGameState().getBallV());
		inState.putBoolean(PAUSED, gv.getThread().getGameState().getPaused());
		inState.putBooleanArray(SETTINGS, gv.getSettings());
		sis = inState;
	}
	
	@Override
	public void finish() {
		Log.d("Main","Finish");
		super.finish();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(gv != null && gv.getThread() != null && gv.getThread().getGameState() != null) 
			gv.getThread().getGameState().setPaused(true);
		Log.d("Main","pause");
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d("Main","restart");
		super.onRestoreInstanceState(sis);
		gv.setValues(sis.getIntArray(PLAYER1LOC),sis.getIntArray(PLAYER2LOC),sis.getIntArray(SCORES), sis.getIntArray(BALLLOC), sis.getFloatArray(BALLVELOCITY), sis.getBoolean(PAUSED), sis.getBooleanArray(SETTINGS));
		/*Menu item = (Menu) findViewById(R.id.settingsGroup);
		if (item != null) item.findItem(R.id.submenu_keyboard).setChecked(gv.getSettings()[0]);
		//item = (MenuItem) findViewById(R.id.submenu_touch);
		if (item != null) item.findItem(R.id.submenu_touch).setChecked(gv.getSettings()[1]);
		//item = (MenuItem) findViewById(R.id.submenu_tilt);
		if (item != null) item.findItem(R.id.submenu_tilt).setChecked(gv.getSettings()[2]);
		//item = (MenuItem) findViewById(R.id.two_player);
		if (item != null) item.findItem(R.id.two_player).setChecked(gv.getSettings()[3]);*/
		
	}
	
	public boolean onPrepareOptionsMenu(Menu m) {
	    boolean result = super.onPrepareOptionsMenu(m);
		m.findItem(R.id.submenu_keyboard).setChecked(gv.getSettings()[0]);
		
		m.findItem(R.id.submenu_touch).setChecked(gv.getSettings()[1]);
		
		m.findItem(R.id.submenu_tilt).setChecked(gv.getSettings()[2]);
		
		m.findItem(R.id.two_player).setChecked(gv.getSettings()[3]);
		
		return result;
	}
	
	private void makeExitDialog() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            //Yes button clicked
		        	finish();
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            dialog.dismiss();
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to exit?").setPositiveButton("Yes", dialogClickListener)
		    .setNegativeButton("No", dialogClickListener).show();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case (R.id.exit):
				makeExitDialog();
				return true;
			case (R.id.new_game):			
				// ohhh buddy
				gv.getThread().getGameState().resetGame();
				return true;
			case (R.id.two_player):
				if (item.isChecked()) {
					item.setChecked(false);
					gv.getThread().getGameState().setTwoPlayer(false);
				} else {
					item.setChecked(true);
					gv.getThread().getGameState().setTwoPlayer(true);
				}
			case (R.id.submenu_keyboard): 
				if (item.isChecked()) {
					if (gv.numberOfControlsSelected() > 1) { // don't want to have 0 control options
						item.setChecked(false);
						gv.addInput(-gv.KEYBOARD);// a negative value means remove input
					}
				}
				else {
					item.setChecked(true);
					gv.addInput(gv.KEYBOARD);
				}
				return true;
			case (R.id.submenu_touch): 
				if (item.isChecked()) {
					if (gv.numberOfControlsSelected() > 1) {
						item.setChecked(false);
						gv.addInput(-gv.TOUCH);
					}
				}
				else {
					item.setChecked(true);
					gv.addInput(gv.TOUCH);
				}
				return true;
			case (R.id.submenu_tilt): 
				if (item.isChecked()) {
					if (gv.numberOfControlsSelected() > 1) {
						item.setChecked(false);
						gv.addInput(-gv.TILT);
					}
				}
				else {
					item.setChecked(true);
					gv.addInput(gv.TILT);
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


}
