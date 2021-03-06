 /*
  *   This file is part of Paintroid, a software part of the Catroid project.
  *   Copyright (C) 2010  Catroid development team
  *   <http://code.google.com/p/catroid/wiki/Credits>
  *
  *   Paintroid is free software: you can redistribute it and/or modify it
  *   under the terms of the GNU Affero General Public License as published
  *   by the Free Software Foundation, either version 3 of the License, or
  *   at your option) any later version.
  *
  *   This program is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *   GNU Affero General Public License for more details.
  *
  *   You should have received a copy of the GNU Affero General Public License
  *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package at.tugraz.ist.paintroid.tools.implementation;
 
 import android.content.Context;
 import android.graphics.Canvas;
 import android.graphics.Path;
 import android.graphics.PointF;
 import at.tugraz.ist.paintroid.MainActivity.ToolType;
 import at.tugraz.ist.paintroid.PaintroidApplication;
 import at.tugraz.ist.paintroid.R;
 import at.tugraz.ist.paintroid.commandmanagement.Command;
 import at.tugraz.ist.paintroid.commandmanagement.implementation.PathCommand;
 import at.tugraz.ist.paintroid.commandmanagement.implementation.PointCommand;
 
 public class DrawTool extends BaseTool {
	protected Path pathToDraw;
	protected PointF previousEventCoordinate = null;
	protected PointF initialEventCoordinate = null;
	protected PointF movedDistance = new PointF(0, 0);
 
 	public DrawTool(Context context, ToolType toolType) {
 		super(context, toolType);
 		pathToDraw = new Path();
 		pathToDraw.incReserve(1);
 	}
 
 	@Override
 	public void draw(Canvas canvas) {
 		canvas.drawPath(pathToDraw, drawPaint);
 	}
 
 	@Override
 	public boolean handleDown(PointF coordinate) {
 		initialEventCoordinate = new PointF(coordinate.x, coordinate.y);
 		previousEventCoordinate = new PointF(coordinate.x, coordinate.y);
 		pathToDraw.moveTo(coordinate.x, coordinate.y);
 		movedDistance.set(0, 0);
 		return true;
 	}
 
 	@Override
 	public boolean handleMove(PointF coordinate) {
		if (initialEventCoordinate == null || previousEventCoordinate == null) {
 			return false;
 		}
 		final float cx = (previousEventCoordinate.x + coordinate.x) / 2;
 		final float cy = (previousEventCoordinate.y + coordinate.y) / 2;
 		pathToDraw.quadTo(previousEventCoordinate.x, previousEventCoordinate.y, cx, cy);
 		pathToDraw.incReserve(1);
 		// movedDistance.set(movedDistance.x + Math.abs(coordinate.x - previousEventCoordinate.x),
 		// Math.abs(movedDistance.y - previousEventCoordinate.y));
 		movedDistance.set(movedDistance.x + Math.abs(coordinate.x - previousEventCoordinate.x),
 				movedDistance.y + Math.abs(coordinate.y - previousEventCoordinate.y));
 		previousEventCoordinate.set(coordinate.x, coordinate.y);
 		return true;
 	}
 
 	@Override
 	public boolean handleUp(PointF coordinate) {
		if (initialEventCoordinate == null || previousEventCoordinate == null) {
 			return false;
 		}
 		movedDistance.set(movedDistance.x + Math.abs(coordinate.x - previousEventCoordinate.x),
 				movedDistance.y + Math.abs(coordinate.y - previousEventCoordinate.y));
 		boolean returnValue;
 		if (PaintroidApplication.MOVE_TOLLERANCE < movedDistance.x
 				|| PaintroidApplication.MOVE_TOLLERANCE < movedDistance.y) {
 			returnValue = addPathCommand(coordinate);
 		} else {
 			returnValue = addPointCommand(initialEventCoordinate);
 		}
 		return returnValue;
 	}
 
 	protected boolean addPathCommand(PointF coordinate) {
 		// if (commandHandler == null) {
 		// Log.e(PaintroidApplication.TAG, "DrawTool null: " + commandHandler + " " + coordinate);
 		// return false;
 		// }
 		pathToDraw.lineTo(coordinate.x, coordinate.y);
 		Command command = new PathCommand(drawPaint, pathToDraw);
 		PaintroidApplication.COMMAND_HANDLER.commitCommand(command);
 		return true;
 	}
 
 	protected boolean addPointCommand(PointF coordinate) {
 		// if (commandHandler == null) {
 		// Log.e(PaintroidApplication.TAG, "DrawTool null: " + commandHandler + " " + coordinate);
 		// return false;
 		// }
 		Command command = new PointCommand(drawPaint, coordinate);
 		PaintroidApplication.COMMAND_HANDLER.commitCommand(command);
 		return true;
 	}
 
 	@Override
 	public int getAttributeButtonResource(int buttonNumber) {
 		if (buttonNumber == 0) {
 			return R.drawable.ic_menu_more_brush_64;
 		}
 		return super.getAttributeButtonResource(buttonNumber);
 	}
 
 	@Override
 	public void resetInternalState() {
 		pathToDraw.rewind();
 		initialEventCoordinate = null;
 		previousEventCoordinate = null;
 	}
 }
