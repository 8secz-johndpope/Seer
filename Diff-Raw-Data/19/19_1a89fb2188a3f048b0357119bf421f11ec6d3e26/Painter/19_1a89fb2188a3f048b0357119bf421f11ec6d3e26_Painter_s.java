 import java.awt.Graphics2D;
 import java.awt.geom.AffineTransform;
 import java.awt.image.AffineTransformOp;
 import java.awt.image.BufferedImage;
 
 import javax.swing.ImageIcon;
 
 
 public class Painter 
 {
 	static void draw(Graphics2D g, ImageIcon image, long currentTime, Movement movement)
 	{
 		// Convert the ImageIcon to BufferedImage to rotate and scale
 		int w = image.getIconWidth();
 		int h = image.getIconHeight();
 		BufferedImage buffImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
 		Graphics2D g2 = (Graphics2D)buffImg.getGraphics();
 		image.paintIcon(null, g2, 0, 0);
 		g2.dispose();
 		
 		AffineTransform tx = new AffineTransform();
 
 		int imgWidth = buffImg.getWidth();
 		int imgHeight = buffImg.getHeight();
 

 		// Rotate
 		tx.translate(imgWidth, imgHeight);
 		tx.rotate(movement.calcRot(currentTime));
 		tx.translate(-imgWidth/2, -imgHeight/2);
		
 
 		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
 		try {
 			buffImg = op.filter(buffImg, null);
 			g.drawImage(buffImg, (int)(movement.calcPos(currentTime).x - imgWidth/2), (int)(movement.calcPos(currentTime).y - imgHeight/2), null);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 	
 	
 	/** Draw and scale. Set either desiredWidth or desiredHeight to -1 to set automatic scaling for that dimension */
 	static void draw(Graphics2D g, ImageIcon image, int desiredWidth, int desiredHeight, long currentTime, Movement movement)
 	{
 		// Convert the ImageIcon to BufferedImage to rotate and scale
 		int w = image.getIconWidth();
 		int h = image.getIconHeight();
 		BufferedImage buffImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
 		Graphics2D g2 = (Graphics2D)buffImg.getGraphics();
 		image.paintIcon(null, g2, 0, 0);
 		g2.dispose();
 		
		AffineTransform tx = new AffineTransform();
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
 		
 		// Scale
 		double xScaleFactor = (desiredWidth)*1.0/buffImg.getWidth();
 		double yScaleFactor = (desiredHeight)*1.0/buffImg.getHeight();
 		
 		if (desiredWidth == -1)
 			xScaleFactor = yScaleFactor;
 		if (desiredHeight == -1)
 			yScaleFactor = xScaleFactor;
		
		tx.scale(xScaleFactor*1.0, yScaleFactor*1.0);
				
 		BufferedImage scaledImg = new BufferedImage((int)(buffImg.getWidth()*xScaleFactor), (int)(buffImg.getHeight()*yScaleFactor), buffImg.getType());
 		Graphics2D gfx = scaledImg.createGraphics();
 		gfx.drawImage(buffImg, 0, 0, (int)(buffImg.getWidth()*xScaleFactor), (int)(buffImg.getHeight()*yScaleFactor),
 							   0, 0, buffImg.getWidth(), buffImg.getHeight(), null);
 		gfx.dispose();
 		
 
 		int imgWidth = scaledImg.getWidth();
 		int imgHeight = scaledImg.getHeight();
 
 		// Rotate
 		tx.translate(imgWidth, imgHeight);
 		tx.rotate(movement.calcRot(currentTime));
 		tx.translate(-imgWidth/2, -imgHeight/2);
 		
 		try {
 			scaledImg = op.filter(scaledImg, null);
 			g.drawImage(scaledImg, (int)(movement.calcPos(currentTime).x - imgWidth/2), (int)(movement.calcPos(currentTime).y - imgHeight/2), null);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 	}
 }
