 package com.taboozle;
 
 import android.app.*;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.view.View.OnClickListener;
 import android.view.View.OnTouchListener;
 import android.view.animation.Animation;
 import android.view.animation.TranslateAnimation;
 import android.media.AudioManager;
 import android.media.SoundPool;
 import android.os.Bundle;
 import android.os.CountDownTimer;
 import android.os.Vibrator;
 import android.preference.PreferenceManager;
 import android.view.KeyEvent;
 import android.view.Menu;
import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.View;
 import android.widget.ArrayAdapter;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.ViewFlipper;
 
 /**
  * This handles a single turn consisting of cards presented to a player for a
  * limited amount of time.
  *
  * @author The Taboozle Team
  */
 public class Turn extends Activity
 {
 
   /**
    * This is a reference to the current game manager
    */
   private GameManager curGameManager;
 
   /**
    * Boolean to track which views are currently active
    */
   private boolean AIsActive;
 
   /**
    * Sound pool for playing the buzz sound on a loop.
    */
   private SoundPool soundPool;
 
   /**
    * id of the buzz within android's sound-pool framework
    */
   private int buzzSoundId;
 
   /**
    * id of the buzz's stream within android's sound-pool framework
    */
   private int buzzStreamId;
 
   /**
    * vibrator object to vibrate on buzz click
    */
   private Vibrator buzzVibrator;
 
   /**
    * Unique IDs for Options menu
    */
   protected static final int MENU_ENDGAME = 0;
   protected static final int MENU_SCORE = 1;
   protected static final int MENU_RULES = 2;
 
   /**
    * Time for the Timer
    */
   protected static final int TURN_TIME = 5000;
 
   /**
    *  Creates the menu items for the options menu
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      menu.add(0, R.string.menu_EndGame, 0, "End Game");
      menu.add(0, R.string.menu_Score, 0, "Score");
      menu.add(0, R.string.menu_Rules, 0, "Rules");
 
       return true;
   }
 
   /**
   * Handle various menu clicks
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      TaboozleApplication application =
          (TaboozleApplication) Turn.this.getApplication();
        GameManager gm = application.GetGameManager();
	  
      // Handle item selection
      switch (item.getItemId()) {
      case R.string.menu_EndGame:
    	  gm.EndGame();
    	  startActivity(new Intent( this, GameEnd.class ));
          return true;
      case R.string.menu_Score:
          //quit();
          return true;
      default:
          return super.onOptionsItemSelected(item);
      }
  }  
  
  /**
    * Listener for the buzzer that plays on touch-down and stops playing on
    * touch-up.
    */
   private final OnTouchListener BuzzListener = new OnTouchListener()
   {	 	  
       public boolean onTouch(View v, MotionEvent event)
       {
     	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
         AudioManager mgr =
           (AudioManager) v.getContext().getSystemService( Context.AUDIO_SERVICE );
         float streamVolumeCurrent = mgr.getStreamVolume( AudioManager.STREAM_MUSIC );
         float streamVolumeMax = mgr.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
         float volume = streamVolumeCurrent / streamVolumeMax;
 
         //Show wrong controls once the buzzer is hit
         ImageButton confirm = (ImageButton) findViewById( R.id.ButtonConfirmWrong );
         ImageButton cancel = (ImageButton) findViewById( R.id.ButtonCancelWrong );
         ImageView wrongStamp = (ImageView) findViewById( R.id.WrongStamp );
         
         confirm.setVisibility( View.VISIBLE );
         cancel.setVisibility( View.VISIBLE );
         
         boolean ret;
         switch( event.getAction() )
         {
           case MotionEvent.ACTION_DOWN:
             buzzStreamId = soundPool.play( buzzSoundId, volume, volume, 1, -1, 1.0f );
             if (sp.getBoolean("vibrate_pref", true))
             {
               buzzVibrator.vibrate(1000);
             }
             wrongStamp.setVisibility( View.VISIBLE ); //Show stamp on down
             ret = true;
             break;
           case MotionEvent.ACTION_UP:
             soundPool.stop( buzzStreamId );
             if (sp.getBoolean("vibrate_pref", true))
             {
               buzzVibrator.cancel();
             }
             wrongStamp.setVisibility( View.INVISIBLE );	//Hide stamp on up
             ret = true;
             break;
           	default:
             ret = false;
         }
 
         return ret;
       }
 
   }; // End BuzzListener
 
   /**
    * Listener for the 'Correct' button. It deals with the flip to the next
    * card.
    */
   private final OnClickListener CorrectListener = new OnClickListener()
   {
       public void onClick(View v)
       {
         AIsActive = !AIsActive;
         ViewFlipper flipper = (ViewFlipper) findViewById( R.id.ViewFlipper0 );
         flipper.showNext();
         curGameManager.ProcessCard( 0 );
         ShowCard();
       }
   }; // End CorrectListener
 
   /**
    * Listener for the 'Skip' button. This deals with moving to the next card
    * via the ViewFlipper, but denotes that the card was skipped;
    */
   private final OnClickListener SkipListener = new OnClickListener()
   {
       public void onClick(View v)
       {
         AIsActive = !AIsActive;
         ViewFlipper flipper = (ViewFlipper) findViewById( R.id.ViewFlipper0 );
         flipper.showNext();
         curGameManager.ProcessCard( 2 );
         ShowCard();
       }
   }; // End SkipListener
 
   /**
    * Listener for the button that confirms a buzz.  This will cause the application
    * to move to the next card and record a wrong score.
    */
   private final OnClickListener ConfirmWrongListener = new OnClickListener()
   {
     public void onClick(View v)
 	{
       AIsActive = !AIsActive;
       ViewFlipper flipper = (ViewFlipper) findViewById( R.id.ViewFlipper0 );
       flipper.showNext();
       curGameManager.ProcessCard( 1 );
       ShowCard();
 	}
   }; // End ConfirmWrongListener
   
   /**
    * 
    */
   private final OnClickListener CancelWrongListener = new OnClickListener()
   {
     public void onClick(View v)
     {
       //Hide the wrong stamp and wrong controls on every new card
       ImageButton confirm = (ImageButton) findViewById( R.id.ButtonConfirmWrong );
       ImageButton cancel = (ImageButton) findViewById( R.id.ButtonCancelWrong );
       ImageView wrongStamp = (ImageView) findViewById( R.id.WrongStamp );
 
       confirm.setVisibility( View.INVISIBLE );
       cancel.setVisibility( View.INVISIBLE );
       wrongStamp.setVisibility( View.INVISIBLE );
     }
   }; // End CancelWrongListener
   
   /**
    * CountdownTimer - This initializes a timer during every turn that runs a
    * method when it completes as well as during update intervals.
   */
   private class TurnTimer extends CountDownTimer
   {
 
     public TurnTimer(long millisInFuture, long countDownInterval)
     {
       super(millisInFuture, countDownInterval);
     }
 
     @Override
       public void onFinish() {
    	
          OnTurnEnd();
       }
 
     @Override
       public void onTick(long millisUntilFinished)
       {
      TextView countdownTxt = (TextView) findViewById( R.id.Timer );
      countdownTxt.setText( ":" + Long.toString(( millisUntilFinished / 1000 ) + 1 ));
       }
   }; // End TurnTimer
 
   /**
    * @return The animation that brings cards into view from the right of the
    * screen
    */
   private Animation InFromRightAnimation ()
   {
 	Animation inFromRight = new TranslateAnimation(
 		  	Animation.RELATIVE_TO_PARENT,  1.0f, Animation.RELATIVE_TO_PARENT,  0.0f,
 		  	Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f );
   	inFromRight.setDuration(500);
   	return inFromRight;
   }
 
   /**
    * @return The animation that tosses the cards from the view out into the
    * ether at the left of the screen
    */
   private Animation OutToLeftAnimation ()
   {
 	Animation outToLeft = new TranslateAnimation(
 		  	Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,  -1.0f,
 		  	Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f );
 	outToLeft.setDuration(500);
   	return outToLeft;
   }
 
   /**
    * Function for changing the currently viewed card. It does a bit of bounds
    * checking.
    */
   protected void ShowCard()
   {
     int curTitle;
     int curWords;
     if( this.AIsActive )
     {
     	curTitle = R.id.CardTitleA;
     	curWords = R.id.CardWordsA;
     }
     else
     {
     	curTitle = R.id.CardTitleB;
     	curWords = R.id.CardWordsB;
     }
 
     TextView cardTitle = (TextView) this.findViewById( curTitle );
     ListView cardWords = (ListView) this.findViewById( curWords );
     
     //Hide the wrong stamp and wrong controls on every new card
     ImageButton confirm = (ImageButton) findViewById( R.id.ButtonConfirmWrong );
     ImageButton cancel = (ImageButton) findViewById( R.id.ButtonCancelWrong );
     ImageView wrongStamp = (ImageView) this.findViewById( R.id.WrongStamp );
     
     confirm.setVisibility( View.INVISIBLE );
     cancel.setVisibility( View.INVISIBLE );
     wrongStamp.setVisibility( View.INVISIBLE );
     
     
     // Disable the ListView to prevent its children from being click-able
     cardWords.setEnabled(false);
     ArrayAdapter<String> cardAdapter =
       new ArrayAdapter<String>( this, R.layout.word );
     Card curCard = this.curGameManager.GetNextCard();
     cardTitle.setText( curCard.getTitle() );
     for( int i = 0; i < curCard.getBadWords().size(); i++ )
     {
       cardAdapter.add( curCard.getBadWords().get( i ) );
     }
     cardWords.setAdapter( cardAdapter );
   }
 
 
 
   /**
    * Hands off the intent to the next turn summary activity.
    */
   protected void OnTurnEnd( )
   {
 	  //Stop the sound if someone had the buzzer held down
 	  soundPool.stop( buzzStreamId );
 	  buzzVibrator.cancel();
 	  Intent newintent = new Intent( this, TurnSummary.class);
 	  startActivity(newintent);
   }
 
   /**
    * onCreate - initializes the activity to display the word you have to cause
    * your team mates to say with the words you cannot say below.
    */
   @Override
   public void onCreate( Bundle savedInstanceState )
   {
     super.onCreate( savedInstanceState );
 
     this.AIsActive = true;
 
     this.soundPool = new SoundPool( 4, AudioManager.STREAM_MUSIC, 100 );
     this.buzzSoundId = this.soundPool.load( this, R.raw.buzzer, 1 );
     this.buzzVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);	
 
     TaboozleApplication application =
       (TaboozleApplication) this.getApplication();
     this.curGameManager = application.GetGameManager();
 
     // Setup the view
     this.setContentView(R.layout.turn );
 
     ViewFlipper flipper = (ViewFlipper) this.findViewById( R.id.ViewFlipper0 );
     flipper.setInAnimation(InFromRightAnimation());
     flipper.setOutAnimation(OutToLeftAnimation());
 
     this.ShowCard();
 
     TurnTimer counter = new TurnTimer( TURN_TIME, 200);
 
     counter.start();
 
     ImageButton buzzerButton = (ImageButton) this.findViewById( R.id.ButtonWrong );
     buzzerButton.setOnTouchListener( BuzzListener );
 
     ImageButton nextButton = (ImageButton) this.findViewById( R.id.ButtonCorrect );
     nextButton.setOnClickListener( CorrectListener );
 
     ImageButton skipButton = (ImageButton) this.findViewById( R.id.ButtonSkip );
     skipButton.setOnClickListener( SkipListener );
     
     ImageButton confirmWrongButton = (ImageButton )this.findViewById( R.id.ButtonConfirmWrong );
     confirmWrongButton.setOnClickListener( ConfirmWrongListener );
 
     ImageButton cancelWrongButton = (ImageButton) this.findViewById( R.id.ButtonCancelWrong );
     cancelWrongButton.setOnClickListener( CancelWrongListener );
     
   }
 
   /**
    * Handler for key down events
    */
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)
     {
 
     // Handle the back button
     if( keyCode == KeyEvent.KEYCODE_BACK
         && event.getRepeatCount() == 0 )
       {
         event.startTracking();
         return true;
       }
 
     return super.onKeyDown(keyCode, event);
    }
 
   /**
    * Handler for key up events
    */
   @Override
   public boolean onKeyUp(int keyCode, KeyEvent event)
     {
 
     // Make back do nothing on key-up instead of climb the action stack
     if( keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
         && !event.isCanceled() )
       {
       return true;
       }
 
     return super.onKeyUp(keyCode, event);
     }
 }
