 package wars.dragon.engine;
 
 public class Position {
     
     private Pair<Integer, Integer> pair;
 
     public Position(Integer x, Integer y) {
 	this.pair = new Pair<Integer, Integer>(x, y);
     }
 
    public Integer getX() { return pair.getLeft(); }
    public Integer getY() { return pair.getRight(); )
 
 }
