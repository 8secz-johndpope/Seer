 package Game.Sprite;
 
 import Game.Sprite.Player;
 import Game.MapData.MapData;
 import Game.Common.Data;
 
 import java.awt.Graphics;
 
 public class LeftUpSlope extends Sprite{
 
 	public LeftUpSlope(int x, int y){
 		super(x,y);
 		image = Data.image.leftUpSlopeImage;
 		width = Data.CHIP_SIZE;
 		height = Data.CHIP_SIZE;
 	}
 	// XvCgupdate
 	public void update(MapData mapData){}
 	// vC[XvCgɐGꂽƂ̊֐
 	public void touch(Sprite s, int dir, int[] dest){
 		int px = s.getX()+s.getWidth()-Data.CD_DIFF;
 		int py = s.getY()+s.getHeight()-Data.CD_DIFF;
 		// Gꂽꍇ
 		if(s.getVy() < 0 && s.getY()+Data.CD_DIFF >= y+height){
 			s.setVy(y+height-Data.CD_DIFF-s.getY());
 			return;
 		}
 		// Gꂽꍇ
 		if(px <= x && py > y){
 			s.setVx(x-px);
 			return;
 		}
 		// _̏ꍇ
 		if(s.getX()+Data.CD_DIFF < x){
 			s.setVy(y-py);
 			s.land();
 			return;
 		}
 		if((s.getX()+Data.CD_DIFF+s.getVx()-x) > (py+s.getVy()-y)) return;
		if(s.jumping() && s.getY() < 0) return;
 		s.setVy(s.getX()+Data.CD_DIFF+s.getVx()-x-py+y);
 		s.land();
 	}
 }
