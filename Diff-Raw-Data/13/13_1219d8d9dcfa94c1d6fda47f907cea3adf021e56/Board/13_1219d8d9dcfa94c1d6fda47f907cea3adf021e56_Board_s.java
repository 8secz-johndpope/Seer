 package it.alcacoop.gnubackgammon.actors;
 
 import it.alcacoop.gnubackgammon.GnuBackgammon;
 import it.alcacoop.gnubackgammon.fsm.BaseFSM;
 import it.alcacoop.gnubackgammon.fsm.BaseFSM.Events;
 import it.alcacoop.gnubackgammon.fsm.GameFSM.States;
 import it.alcacoop.gnubackgammon.layers.GameScreen;
 import it.alcacoop.gnubackgammon.logic.AvailableMoves;
 import it.alcacoop.gnubackgammon.logic.GnubgAPI;
 import it.alcacoop.gnubackgammon.logic.MatchState;
 import it.alcacoop.gnubackgammon.logic.Move;
 import it.alcacoop.gnubackgammon.utils.JSONProperties;
 import com.badlogic.gdx.Gdx;
 import com.badlogic.gdx.graphics.g2d.TextureRegion;
 import com.badlogic.gdx.math.Vector2;
 import com.badlogic.gdx.scenes.scene2d.Group;
 import com.badlogic.gdx.scenes.scene2d.InputEvent;
 import com.badlogic.gdx.scenes.scene2d.actions.Actions;
 import com.badlogic.gdx.scenes.scene2d.ui.Image;
 import com.badlogic.gdx.scenes.scene2d.ui.Label;
 import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
 import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
 import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
 
 import java.util.Stack;
 
 
 
 public class Board extends Group {
   
   private Label thinking;
   private DoublingCube doublingCube;
   private BaseFSM fsm;
   
   public int[][] _board;
   public int[] bearedOff = {0,0};
   public Stack<Move> moves;
   public Stack<Move> playedMoves;
 
   public Vector2 pos[];
   public Checker checkers[][];
   private Checker lastMoved = null;
   public Checker selected = null;
   
   public Points points;
   public Dices dices;
   public AvailableMoves availableMoves;
   
   private Image boardbg;
   public BoardImage board;
   public JSONProperties jp;
 
   public TextButton rollBtn;
   public TextButton doubleBtn;
   
   public Board() {
     
     jp = new JSONProperties(Gdx.files.internal("data/"+GnuBackgammon.Instance.getResName()+"/pos.json"));
     _board = new int[2][25];
     
     moves = new Stack<Move>();
     playedMoves = new Stack<Move>();
     availableMoves = new AvailableMoves(this);
     checkers = new Checker[2][15]; //[0]=WHITE [1]=BLACK
     
     board = new BoardImage(0,0);
     
     TextureRegion r1 = GnuBackgammon.atlas.findRegion("boardbg");
     boardbg = new Image(r1);
     boardbg.setX(0);
     boardbg.setY(0);
     addActor(boardbg);
 
     pos = new Vector2[25];
     for (int i=0; i<24;i++) {
       pos[i] = new Vector2();
       pos[i].x = board.getX() + GnuBackgammon.Instance.jp.asFloat("pos"+i, 0) + GnuBackgammon.Instance.jp.asFloat("pos", 0)/2;
       if (i<12)
         pos[i].y = board.getY() + GnuBackgammon.Instance.jp.asFloat("down", 0);
       else
         pos[i].y = board.getY() + GnuBackgammon.Instance.jp.asFloat("up", 0);
     }
     pos[24] = new Vector2();  //HITTED
     pos[24].x = board.getX() + GnuBackgammon.Instance.jp.asFloat("pos24", 0) + GnuBackgammon.Instance.jp.asFloat("pos", 0);
     pos[24].y = board.getY();
 
     for (int i = 0; i<15; i++) {
       checkers[0][i] = new Checker(this, 0); //BLACK
       checkers[1][i] = new Checker(this, 1); //WHITE
     }
     
     points = new Points(this);
     addActor(points);
     addActor(board);
     
     for (int i = 0; i<15; i++) {
       addActor(checkers[0][i]);
       addActor(checkers[1][i]);
     }
     
     dices = new Dices(this);
     addActor(dices);
     
     thinking = new Label("... Thinking ...", GnuBackgammon.skin);
     thinking.setX(getX()+(boardbg.getWidth()-thinking.getWidth())/2);
     thinking.setY(getY()+(boardbg.getHeight()-thinking.getHeight())/2);
     thinking.addAction(Actions.forever(Actions.sequence(Actions.alpha(0.7f, 0.4f), Actions.alpha(1, 0.5f))));
     thinking.setVisible(false);
     addActor(thinking);
 
     TextButtonStyle ts = GnuBackgammon.skin2.get("button", TextButtonStyle.class);
     rollBtn = new TextButton("Roll", ts);
     rollBtn.addListener(new ClickListener(){
       @Override
       public void clicked(InputEvent event, float x, float y) {
         Board.this.rollBtn.remove();
         Board.this.doubleBtn.remove();
         Board.this.rollDices();
       }
     });
     rollBtn.setWidth(boardbg.getWidth()/5);
     rollBtn.setHeight(boardbg.getHeight()/9);
     rollBtn.setX(board.getX() + GnuBackgammon.Instance.jp.asFloat("dice0", 0)-rollBtn.getWidth()/2);
     rollBtn.setY(board.getY() + boardbg.getHeight()/2-rollBtn.getHeight()/2);
 
 
     doubleBtn = new TextButton("Double", ts);
     doubleBtn.addListener(new ClickListener(){
       @Override
       public void clicked(InputEvent event, float x, float y) {
         Board.this.doubleBtn.remove();
         GnuBackgammon.fsm.state(States.DIALOG_HANDLER);
         GnuBackgammon.fsm.processEvent(Events.DOUBLE_REQUEST, null);
       }
     });
     doubleBtn.setWidth(boardbg.getWidth()/5);
     doubleBtn.setHeight(boardbg.getHeight()/9);
     doubleBtn.setX(board.getX() + jp.asFloat("dice1", 0)-doubleBtn.getWidth()/2);
     doubleBtn.setY(board.getY() + boardbg.getHeight()/2-doubleBtn.getHeight()/2);
     
     doublingCube = new DoublingCube(this);
     addActor(doublingCube);
   }
 
 
   public Vector2 getBoardCoord(int color, int x, int y){
     if ((y>4)&&(x!=-1)) y=4;
     float cdim = checkers[0][0].getHeight()-checkers[0][0].getHeight()/18;
     Vector2 ret = new Vector2();
 
     switch (x) {
     
       case -1: //BEAR OFF
         ret.x = board.getX() + GnuBackgammon.Instance.jp.asFloat("pos_bo", 0) + GnuBackgammon.Instance.jp.asFloat("pos", 0)/2;
         if (color==0)
           ret.y = board.getY() + GnuBackgammon.Instance.jp.asFloat("down", 0) + GnuBackgammon.Instance.jp.asFloat("pos", 0)*y;
         else
           ret.y = board.getY() + GnuBackgammon.Instance.jp.asFloat("up", 0) - cdim - GnuBackgammon.Instance.jp.asFloat("pos", 0)*y;
         break;
         
       case 24: //BAR
         ret.x = pos[x].x - GnuBackgammon.Instance.jp.asFloat("pos", 0)/2;
         if (color==0)
           ret.y = board.getY() + GnuBackgammon.Instance.jp.asFloat("up", 0) - cdim*y - cdim*3/2;
         else
           ret.y = board.getY() + GnuBackgammon.Instance.jp.asFloat("down", 0) + cdim*y + cdim/2;
         break;
         
       default: //ON THE TABLE
         if (color==0) { //BLACK
           ret.x = pos[x].x;
           if (x<12) ret.y = board.getY() + pos[x].y + cdim*y;
           else ret.y = board.getY() + pos[x].y - cdim*(y+1);
         } else { //WHITE
           ret.x = pos[23-x].x;
           if (x<12) ret.y = board.getY() + pos[23-x].y - cdim*(y+1);
           else ret.y = board.getY() + pos[23-x].y + cdim*y;
         }
     }
     return ret;
   }
 
   public void initBoard(int type) {
     MatchState.SetGameVariant(type);
     initBoard();
   }
   public void initBoard() {
     abandon();
     doublingCube.reset();
     MatchState.resignValue = 0;
     MatchState.fMove = 0;
     MatchState.fTurn = 0;
     
     if (MatchState.fCubeUse==0) { //NOT DOUBLING
       doublingCube.setVisible(false);
     } else {
       doublingCube.setVisible(true);
       if ((MatchState.fCrawford==1)&&(MatchState.fCrafwordGame))
         doublingCube.setVisible(false);
     }
     
     int i0 = 0;
     int i1 = 0;
 
     for (int i=0; i<=24; i++) {
       _board[0][i] = MatchState.board[MatchState.bgv*2][i];
       i0+=MatchState.board[MatchState.bgv*2][i]; //TOT CHECKERS ON BOARD
       _board[1][i] = MatchState.board[MatchState.bgv*2+1][i];
       i1+=MatchState.board[MatchState.bgv*2+1][i]; //TOT CHECKERS ON BOARD
     }
     bearedOff[0] = 0;
     bearedOff[1] = 0;
     
     int nchecker = 0;
     //POSITIONING BLACK CHECKERS
     for (int i=0; i<25; i++) {
       for (int j=0;j<_board[0][i];j++) {
         checkers[0][nchecker].reset(i,j);
         addActor(checkers[0][nchecker]);
         nchecker++;
       }
     }
     for (int i=0;i<15-i0;i++) {
       checkers[0][nchecker].reset(-1,i);
       addActor(checkers[0][nchecker]);
       bearedOff[0]++;
       nchecker++;
     }
     
     //POSITIONING WHITE CHECKERS
     nchecker = 0;
     for (int i=0; i<25; i++) {
       for (int j=0;j<_board[1][i];j++) {
         checkers[1][nchecker].reset(i, j);
         addActor(checkers[1][nchecker]);
         nchecker++;
       }
     }
     for (int i=0;i<15-i1;i++) {
       checkers[1][nchecker].reset(-1,i);
       addActor(checkers[1][nchecker]);
       bearedOff[1]++;
       nchecker++;
     }
 
     MatchState.UpdateMSCubeInfo(1, -1);
   }
 
   
   public Checker getChecker(int color, int x) {
     Checker _c = null;
     int y = 0;
     if (x>=0)
       y = _board[color][x]-1;
     else
       y = bearedOff[color]-1;
     
     for (int i = 0; i<15; i++) {
       Checker c = checkers[color][i];
       if ((c.boardX==x)&&(c.boardY==y))
         _c = c;
     }
     return _c;
   }
 
 
   public void setMoves(int _moves[]) {
     fsm = GnuBackgammon.fsm;
     moves.clear();
     
     if (_moves.length<8) return;
     int ms = 0;
     for (int i=3;i>=0;i--) {
       if (_moves[2*i]!=-1) {
         Move m = new Move(this, _moves[2*i], _moves[2*i+1]);
         moves.push(m);
         ms++;
       }
     }
 
     if (ms==1) performNextMove(true);
     else performNextMove();
   }
 
 
   public void performNextMove() { 
     performNextMove(false);
   }
   public void performNextMove(boolean nodelay) {
     try {
       Move m = moves.pop();
      
       
       if (m!=null) {
         playedMoves.push(m);
         m.setRemovedMoves(availableMoves.removeMoves(m.from, m.to));
         Checker c = getChecker(MatchState.fMove, m.from);
         if (nodelay) c.moveTo(m.to);
         else c.moveToDelayed(m.to, 0.2f);
         lastMoved = c;
       }  
     } catch (Exception e) {
       if (fsm == GnuBackgammon.fsm) //SAME FSM FROM MOVING START...
         fsm.processEvent(Events.NO_MORE_MOVES, null);
     }
   }
 
   
   public boolean checkHit() {
     if (lastMoved!=null) {
       if (lastMoved.boardX==-1) return false; //BEARED OFF
       
       int c = lastMoved.getSpecularColor();
       int p = lastMoved.getSpecularPosition();
       if (_board[c][p]==1) {
         //CHECKER HITTED
         Checker ch = getChecker(c, p);
         ch.moveTo(24);
         playedMoves.lastElement().hitted = true;
         return true;
       }
     }
     return false;
   }
 
   
   public int getAutoDestination(int x) {
     int ps[] = availableMoves.getPoints(x);
     if ((ps!=null)&&(ps.length==1))
       return ps[0];
     else if ((ps!=null)&&(ps.length==2))
       return ps[0]<=ps[1]?ps[0]:ps[1];
     else return -2;
   }
   
   
   public void selectChecker(int x) {
     if (_board[MatchState.fMove][x]>0) {
 
       points.reset();
       if (selected!=null) selected.highlight(false);
       
       Checker c = getChecker(MatchState.fMove, x);
       
       if ((selected!=null)&&(c.boardX==selected.boardX)) {
         selected = null;
         return;
       }
       
       int ps[] = availableMoves.getPoints(x);
       if ((ps==null)||(ps.length==0)) { //NO MOVES FROM HERE!
         c.highlight(false);
         selected = null;        
       } else {
         c.highlight(true);
         selected = c;
         for (int i=0; i<ps.length;i++)
           points.get(ps[i]).highlight();
       }
         
     }
   }
   
   
   public int bearingOff() {
     int count = 0;
     for(int i=6;i<25;i++){
       if(_board[MatchState.fMove][i] > 0){
         count+=_board[MatchState.fMove][i];
       }
     }//if count = 0 here, we're in bear off
 
     int max_point = 0;
     if(count==0) { //bear off
       for(int i=0;i<6;i++){
         if(_board[MatchState.fMove][i] > 0){
           max_point = i;
         }
       }//max_point here is the bigger point with checkers 
     }
     if(count!=0)
       return -1;
     else 
       return max_point;
   }
 
   public int getPIPS() {
     return getPIPS(MatchState.fMove);
   }
   public int getPIPS(int color) {
     int pips = 0;
     for (int i=0;i<_board[color].length;i++) {
       pips += _board[color][i]*(i+1);
     }
     return pips;
   }
   
   public boolean gameFinished() {
     if (getPIPS()>0) return false;
     else return true;
   }
   
   public boolean specularPointFree(int nPoint) {
     if (MatchState.fMove==1) return (_board[0][23-nPoint]<=1);
     else return (_board[1][23-nPoint]<=1);
   }
   
   public void undoMove() {
     if (playedMoves.size()>0) {
       playedMoves.pop().undo();
       updatePInfo();
     }
   }
   
   public void switchTurn() {
     playedMoves.clear();
     MatchState.SwitchTurn();
   }
   
   private void abandon() {
     for (int i=0; i<15; i++) {
       checkers[0][i].resetActions();
       checkers[1][i].resetActions();
     }
     if (dices!=null) dices.clear();
     if (moves!=null) moves.clear();
     if (playedMoves!=null) playedMoves.clear();
     points.reset();
     rollBtn.remove();
     if(MatchState.fCubeUse == 1)
       doubleBtn.remove();    
   }
   
   public void animate(float t) {
     int i0 = 0;
     int i1 = 0;
 
     for (int i=0; i<=24; i++) {
       _board[0][i] = MatchState.board[0][i];
       i0+=MatchState.board[0][i];
       _board[1][i] = MatchState.board[1][i];
       i1+=MatchState.board[1][i];
     }
     bearedOff[0] = 0;
     bearedOff[1] = 0;
     
     int nchecker = 0;
     //POSITIONING BLACK CHECKERS
     for (int i=0; i<25; i++) {
       for (int j=0;j<_board[0][i];j++) {
         checkers[0][nchecker].reset(i,j, t);
         nchecker++;
       }
     }
     for (int i=0;i<15-i0;i++) {
       checkers[0][nchecker].reset(-1,i, t);
       bearedOff[0]++;
       nchecker++;
     }
     
     //POSITIONING WHITE CHECKERS
     nchecker = 0;
     for (int i=0; i<25; i++) {
       for (int j=0;j<_board[1][i];j++) {
         checkers[1][nchecker].reset(i, j, t);
         nchecker++;
       }
     }
     for (int i=0;i<15-i1;i++) {
       checkers[1][nchecker].reset(-1,i, t);
       bearedOff[1]++;
       nchecker++;
     }
   }
   
   public void updatePInfo() {
     GameScreen s = (GameScreen) GnuBackgammon.Instance.currentScreen;
     s.updatePInfo();
   }
   
   @Override
   public float getHeight() {
     return boardbg.getHeight();
   }
   
   @Override
   public float getWidth() {
     return boardbg.getWidth();
   }
   
   @Override
   public void setWidth(float width) {
     setScaleX(width/boardbg.getWidth());
   }
   
   @Override
   public void setHeight(float height) {
     setScaleY(height/boardbg.getHeight());
   }
   
   public void thinking(boolean v) {
     thinking.setVisible(v);
   }
   
   public int gameScore(int loser) {
     if ((bearedOff[0]!=15)&&(bearedOff[1]!=15)) return 1;//CUBE NOT ACCEPTED
     if (bearedOff[loser]>0) return 1;
     else {
       boolean backgammon = false;
       for(int i=18;i<25;i++){
         backgammon |= (_board[loser][i] > 0);
       }//if count = 0 here, we're in bear off
       if (backgammon) return 3;
       else return 2;
     }
   }
   
   public void doubleCube() {
     doublingCube.setValue(MatchState.nCube);
   }
   
   public void rollDices() {
     GnuBackgammon.Instance.snd.playRoll();
     dices.clear();
     int[] ds = {0,0};
     GnubgAPI.RollDice(ds);
    dices.animate(ds[0], ds[1]);
   }
   
   public void rollDices(int d1, int d2) {
     dices.clear();
    dices.show(d1, d2, false);
   }
   
 } //END CLASS
