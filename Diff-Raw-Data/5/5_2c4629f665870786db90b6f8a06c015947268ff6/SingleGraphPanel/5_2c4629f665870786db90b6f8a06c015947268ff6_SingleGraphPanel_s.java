 /* OpenLogViewer
  *
  * Copyright 2011
  *
  * This file is part of the OpenLogViewer project.
  *
  * OpenLogViewer software is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenLogViewer software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with any OpenLogViewer software.  If not, see http://www.gnu.org/licenses/
  *
  * I ask that if you make any changes to this file you fork the code on github.com!
  *
  */
 package org.diyefi.openlogviewer.graphing;
 
 import java.awt.Color;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.event.HierarchyBoundsListener;
 import java.awt.event.HierarchyEvent;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 
 import javax.swing.JPanel;
 import org.diyefi.openlogviewer.OpenLogViewer;
 import org.diyefi.openlogviewer.genericlog.GenericDataElement;
 import org.diyefi.openlogviewer.utils.MathUtils;
 
 /**
  * SingleGraphPanel is a JPanel that uses a transparent background.
  * The graph trace is drawn to this panel and used in conjunction with a JLayeredPane
  * to give the appearance of all the graph traces drawn together.
  *
  * This layer listens for window resizes and property changes.
  * @author Bryan Harris and Ben Fenner
  */
 public class SingleGraphPanel extends JPanel implements HierarchyBoundsListener, PropertyChangeListener {
 	private static final long serialVersionUID = 1L;
 
 	private static final double GRAPH_TRACE_SIZE_AS_PERCENTAGE_OF_TOTAL_GRAPH_SIZE = 0.95;
 	private GenericDataElement GDE;
 	private double[] dataPointsToDisplay;
 	private double[][] dataPointRangeInfo;
 	private int availableDataRecords;
 
 	public SingleGraphPanel() {
 		this.setOpaque(false);
 		this.setLayout(null);
 		this.GDE = null;
 		dataPointsToDisplay = null;
 		dataPointRangeInfo = null;
 	}
 
 	@Override
 	public void ancestorMoved(final HierarchyEvent e) {
 	}
 
 	@Override
 	public final void ancestorResized(final HierarchyEvent e) {
 		if (e.getID() == HierarchyEvent.ANCESTOR_RESIZED) {
 			sizeGraph();
 		}
 	}
 
 	@Override
 	public final void propertyChange(final PropertyChangeEvent evt) {
 		if (evt.getPropertyName().equalsIgnoreCase("Split")) {
 			sizeGraph();
 		}
 	}
 
 	@Override
 	public final void paint(final Graphics g) { // overridden paint because there will be no other painting other than this
 		boolean zoomedOut = OpenLogViewer.getInstance().getEntireGraphingPanel().isZoomedOutBeyondOneToOne();
 		if(zoomedOut){
 			initGraphZoomedOut();
 		} else{
 			initGraphZoomed();
 		}
 		if (hasDataPointToDisplay()) {
 			paintDataPointsAndTraces(g);
 		}
 	}
 
 	private void paintDataPointsAndTraces(final Graphics g) {
 		// Setup graphics stuff
 		final Graphics2D g2d = (Graphics2D) g;
 		g2d.setColor(GDE.getDisplayColor());
 
 		// Initialize current, previous and next graph trace data points
 		double leftOfTraceData = -Double.MAX_VALUE;
 		double traceData = -Double.MAX_VALUE;
 		double rightOfTraceData = dataPointsToDisplay[0];
 
 		// Initialize graph status markers
 		boolean atGraphBeginning = false;
 		boolean insideGraph = false;
 		boolean atGraphEnd = false;
 
 		// Initialize and setup data point screen location stuff
 		final boolean zoomedOut = OpenLogViewer.getInstance().getEntireGraphingPanel().isZoomedOutBeyondOneToOne();
 		int zoom = OpenLogViewer.getInstance().getEntireGraphingPanel().getZoom();
 		if(zoomedOut){
 			zoom = 1;
 		}
 		final double graphPosition = OpenLogViewer.getInstance().getEntireGraphingPanel().getGraphPosition();
 		final double offset = (graphPosition % 1) * zoom;
 		int screenPositionXCoord = -(int)Math.round(offset);  // Start with one point off-screen to the left
 		int screenPositionYCoord = Integer.MIN_VALUE;
 		int nextScreenPositionYCoord = getScreenPositionYCoord(rightOfTraceData, GDE.getDisplayMinValue(), GDE.getDisplayMaxValue());
 
 		// Draw data points and trace lines from left to right including one off screen to the right
 		for (int i = 0; i < dataPointsToDisplay.length; i++) {
 
 			// Setup current, previous and next graph trace data points
 			if (i > 0){
 				leftOfTraceData = dataPointsToDisplay[i - 1];
 			} else {
 				leftOfTraceData = -Double.MAX_VALUE;
 			}
 			traceData = dataPointsToDisplay[i];
 			if (i + 1 < dataPointsToDisplay.length){
 				rightOfTraceData = dataPointsToDisplay[i + 1];
 			} else {
 				rightOfTraceData = -Double.MAX_VALUE;
 			}
 
 			// Setup data point screen location stuff
 			screenPositionYCoord = nextScreenPositionYCoord;
 			nextScreenPositionYCoord = getScreenPositionYCoord(rightOfTraceData, GDE.getDisplayMinValue(), GDE.getDisplayMaxValue());
 
 			// Setup graph states and draw graph beginning and end markers
 			if(leftOfTraceData == -Double.MAX_VALUE && traceData != -Double.MAX_VALUE){
 				// At graph beginning
				g2d.drawRect(screenPositionXCoord - 2, screenPositionYCoord - 2, 4, 4);
 				atGraphBeginning = true;
 				insideGraph = true;
 			}
 			if(traceData != -Double.MAX_VALUE && rightOfTraceData == -Double.MAX_VALUE){
 				// At graph end
				g2d.drawRect(screenPositionXCoord - 2, screenPositionYCoord - 2, 4, 4);
 				atGraphEnd = true;
 			}
 
 			// Draw data point
 			if(!zoomedOut && zoom > 5){
 				// Draw fat data point
 				if (atGraphBeginning){
 					if (traceData != rightOfTraceData) {
 						// fillRect() is 95% faster than fillOval() for a 3x3 square on Ben's dev machine
 						g2d.fillRect(screenPositionXCoord - 1, screenPositionYCoord - 1, 3, 3);
 					}
 				} else if (atGraphEnd){
 					if (traceData != leftOfTraceData) {
 						// fillRect() is 95% faster than fillOval() for a 3x3 square on Ben's dev machine
 						g2d.fillRect(screenPositionXCoord - 1, screenPositionYCoord - 1, 3, 3);
 					}
 				} else if (insideGraph) {
 					if (traceData != leftOfTraceData || traceData != rightOfTraceData){
 						// fillRect() is 95% faster than fillOval() for a 3x3 square on Ben's dev machine
 						g2d.fillRect(screenPositionXCoord - 1, screenPositionYCoord - 1, 3, 3);
 					}
 				}
 			} else if (insideGraph) {
 				// Draw small data point
 				// drawLine() is 33% faster than fillRect() for a single pixel on Ben's dev machine
 				g2d.drawLine(screenPositionXCoord, screenPositionYCoord, screenPositionXCoord, screenPositionYCoord);
 			}
 
 			// Draw graph trace line
 			if (insideGraph && !atGraphEnd){
 				g2d.drawLine(screenPositionXCoord, screenPositionYCoord, screenPositionXCoord + zoom, nextScreenPositionYCoord);
 			}
 
 			// Reset graph states
 			if(atGraphEnd){
 				insideGraph = false;
 			}
 			atGraphBeginning = false;
 
 			// Move to the right in preparation of drawing more
 			screenPositionXCoord += zoom;
 		}
 	}
 
 	private int getScreenPositionYCoord(final Double traceData, final double minValue, final double maxValue) {
 		int point = 0;
 		final int height = (int) (this.getHeight() * GRAPH_TRACE_SIZE_AS_PERCENTAGE_OF_TOTAL_GRAPH_SIZE);
 		if (maxValue != minValue) {
 			point = (int) (height - (height * ((traceData - minValue) / (maxValue - minValue))));
 		}
 		return point;
 	}
 
 	private boolean hasDataPointToDisplay() {
 		boolean result = false;
 		if ((dataPointsToDisplay != null) && (dataPointsToDisplay.length > 0)) {
 			result = true;
 		}
 		return result;
 	}
 
 	/**
 	 * this is where the GDE is referenced and the graph gets initialized for the first time
 	 * @param GDE
 	 */
 	public final void setData(final GenericDataElement GDE) {
 		this.GDE = GDE;
 		this.availableDataRecords = GDE.size() + 1; // Size is currently position, this will need cleaning up later, leave it to me.
 		// The main thing is to take away 10 calls to the GDE per view on something that is fairly static and cache it internally
 		sizeGraph();
 	}
 
 	public final GenericDataElement getData() {
 		return GDE;
 	}
 
 	/**
 	 * Used for InfoLayer to get the data from the single graphs for data under the mouse
 	 *
 	 * @param pointerDistanceFromCenter
 	 * @return Double representation of info at the mouse cursor line which snaps to data points or null if no data under cursor
 	 */
 	public final String getMouseInfo(final int cursorPosition) {
 		boolean zoomedOut = OpenLogViewer.getInstance().getEntireGraphingPanel().isZoomedOutBeyondOneToOne();
 		String info = "-.-";
 		if(zoomedOut){
 			info = getMouseInfoZoomedOut(cursorPosition);
 		} else {
 			info = getMouseInfoZoomed(cursorPosition);
 		}
 
 		return info;
 	}
 
 	/**
 	 * Used for InfoLayer to get the data from the single graphs for data under the mouse when not zoomed out
 	 *
 	 * @param pointerDistanceFromCenter
 	 * @return Double representation of info at the mouse cursor line which snaps to data points or null if no data under cursor
 	 */
 	private final String getMouseInfoZoomed(final int cursorPosition){
 		String result = "-.-";
 		final double graphPosition = OpenLogViewer.getInstance().getEntireGraphingPanel().getGraphPosition();
 		final int zoom = OpenLogViewer.getInstance().getEntireGraphingPanel().getZoom();
 		final double offset = (graphPosition % 1) * zoom;
 		final int cursorPositionPlusOffset = cursorPosition + (int) offset;
 		double numSnapsFromCenter = ((double) cursorPositionPlusOffset / (double) zoom);
 		numSnapsFromCenter = Math.round(numSnapsFromCenter);
 		final int dataLocation = (int) graphPosition + (int) numSnapsFromCenter;
 		if ((dataLocation >= 0) && (dataLocation < availableDataRecords)) {
 			double data = GDE.get(dataLocation);
 			data = MathUtils.INSTANCE.roundToSignificantFigures(data, 6);
 			result = Double.toString(data);
 			if(result.length() > 8){
 				result = result.substring(0, 8);
 			}
 		}
 		return result;
 	}
 
 	/**
 	 * Used for InfoLayer to get the data from the single graphs for data under the mouse when zoomed out
 	 *
 	 * @param pointerDistanceFromCenter
 	 * @return Double representation of info at the mouse cursor line which snaps to data points or null if no data under cursor
 	 */
 	private final String getMouseInfoZoomedOut(int cursorPosition){
 		String result = "-.- | -.- | -.-";
 		if ((cursorPosition >= 0) && (cursorPosition < dataPointRangeInfo.length)) {
 			double minData = dataPointRangeInfo[cursorPosition][0];
 			double meanData = dataPointRangeInfo[cursorPosition][1];
 			double maxData = dataPointRangeInfo[cursorPosition][2];
 			if(minData != -Double.MAX_VALUE){
 				minData = MathUtils.INSTANCE.roundToSignificantFigures(minData, 6);
 				maxData = MathUtils.INSTANCE.roundToSignificantFigures(maxData, 6);
 				String resultMin = Double.toString(minData);
 				String resultMax = Double.toString(maxData);
 				if(resultMin.length() > 8){
 					resultMin = resultMin.substring(0, 8);
 				}
 				if(resultMax.length() > 8){
 					resultMax = resultMax.substring(0, 8);
 				}
 				meanData = MathUtils.INSTANCE.roundToSignificantFigures(meanData, 6);
 				String resultMean = Double.toString(meanData);
 				if(resultMin.length() > resultMax.length() && resultMin.length() < resultMean.length()){
 					meanData = MathUtils.INSTANCE.roundToSignificantFigures(meanData, resultMin.length() - 2);
 					resultMean = resultMean.substring(0, resultMin.length());
 				} else if (resultMax.length() < resultMean.length()){
 					meanData = MathUtils.INSTANCE.roundToSignificantFigures(meanData, resultMax.length() - 2);
 					resultMean = resultMean.substring(0, resultMax.length());
 				}
 
 				result = resultMin + " | " + resultMean + " | " + resultMax;
 			}
 		}
 		return result;
 	}
 
 	public final Color getColor() {
 		return GDE.getDisplayColor();
 	}
 
 	public final void setColor(final Color c) {
 		GDE.setDisplayColor(c);
 	}
 
 	/**
 	 * initialize the graph any time you need to paint
 	 */
 	public final void initGraphZoomed() {
 		if (GDE != null) {
 			final int graphPosition = (int)OpenLogViewer.getInstance().getEntireGraphingPanel().getGraphPosition();
 			int graphWindowWidth = OpenLogViewer.getInstance().getEntireGraphingPanel().getWidth();
 			final int zoom = OpenLogViewer.getInstance().getEntireGraphingPanel().getZoom();
 			int numberOfPointsThatFitInDisplay = graphWindowWidth / zoom;
 			numberOfPointsThatFitInDisplay += 3; // Add three for off-screen points to the right
 			dataPointsToDisplay = new double[numberOfPointsThatFitInDisplay];
 			int position = graphPosition;
 
 			// Setup data points.
 			for (int i = 0; i < numberOfPointsThatFitInDisplay; i++) {
 				if (position >= 0 && position < availableDataRecords) {
 					dataPointsToDisplay[i] = GDE.get(position);
 				} else {
 					dataPointsToDisplay[i] = -Double.MAX_VALUE;
 				}
 				position++;
 			}
 		}
 	}
 
 	/**
 	 * initialize the graph any time you need to paint
 	 */
 	public final void initGraphZoomedOut() {
 		if (GDE != null) {
 			final int graphPosition = (int)OpenLogViewer.getInstance().getEntireGraphingPanel().getGraphPosition();
 			int graphWindowWidth = OpenLogViewer.getInstance().getEntireGraphingPanel().getWidth();
 			final int zoom = OpenLogViewer.getInstance().getEntireGraphingPanel().getZoom();
 			dataPointsToDisplay = new double[graphWindowWidth + 1]; // Add one data point for off-screen to the right
 			dataPointRangeInfo = new double[graphWindowWidth + 1][3]; // Add one data point for off-screen to the right
 			final int numberOfRealPointsThatFitInDisplay = (graphWindowWidth * zoom) + zoom; // Add one data point for off-screen to the right
 			final int rightGraphPosition = graphPosition + numberOfRealPointsThatFitInDisplay;
 
 			/*
 			* Setup data points.
 			*
 			* The data point to display is calculated by taking the average of
 			* the data point spread and comparing it to the previous calculated
 			* data point. If the average is higher, then the highest value of
 			* the data spread is used. If the average is lower, then the lowest
 			* value of the data point spread is used.
 			*
 			* In other words, if the graph is trending upward, the peak is used.
 			* If the graph is trending downward, the valley is used.
 			* This keeps the peaks and valleys intact and the middle stuff is
 			* lost. This maintains the general shape of the graph, and assumes
 			* that local peaks and valleys are the most interesting parts of the
 			* graph to display.
 			*/
 			int nextAarrayIndex = 0;
 			double leftOfNewData = GDE.get(0);
 			for (int i = graphPosition; i < rightGraphPosition; i+=zoom) {
 
 				if (i >= 0 && i < availableDataRecords) {
 					double minData = Double.MAX_VALUE;
 					double maxData = -Double.MAX_VALUE;
 					double newData = 0.0;
 					double acummulateData = 0.0;
 					int divisor = 0;
 
 					for (int j = 0; j < zoom; j++){
 						if (i + j >= 0 && i + j < availableDataRecords) {
 							newData = GDE.get(i + j);
 							acummulateData += newData;
 							divisor++;
 							if (newData < minData){
 								minData = newData;
 							}
 							if (newData > maxData){
 								maxData = newData;
 							}
 						}
 					}
 					double averageData = acummulateData / divisor;
 					if (averageData > leftOfNewData){
 						dataPointsToDisplay[nextAarrayIndex] = maxData;
 						leftOfNewData = maxData;
 					} else if (averageData < leftOfNewData){
 						dataPointsToDisplay[nextAarrayIndex] = minData;
 						leftOfNewData = minData;
 					} else {
 						dataPointsToDisplay[nextAarrayIndex] = averageData;
 						leftOfNewData = averageData;
 					}
 					dataPointRangeInfo[nextAarrayIndex][0] = minData;
 					dataPointRangeInfo[nextAarrayIndex][1] = averageData;
 					dataPointRangeInfo[nextAarrayIndex][2] = maxData;
 					nextAarrayIndex++;
 				} else {
 					dataPointsToDisplay[nextAarrayIndex] = -Double.MAX_VALUE;
 					dataPointRangeInfo[nextAarrayIndex][0] = -Double.MAX_VALUE;
 					dataPointRangeInfo[nextAarrayIndex][1] = -Double.MAX_VALUE;
 					dataPointRangeInfo[nextAarrayIndex][2] = -Double.MAX_VALUE;
 					nextAarrayIndex++;
 				}
 			}
 		}
 	}
 
 	/**
 	 * maintains the size of the graph when applying divisions
 	 */
 	public final void sizeGraph() {
 		final MultiGraphLayeredPane lg = OpenLogViewer.getInstance().getMultiGraphLayeredPane();
 		int wherePixel = 0;
 		if (lg.getTotalSplits() > 1) {
 			if (GDE.getSplitNumber() <= lg.getTotalSplits()) {
 				wherePixel += lg.getHeight() / lg.getTotalSplits() * GDE.getSplitNumber() - (lg.getHeight() / lg.getTotalSplits());
 			} else {
 				wherePixel += lg.getHeight() / lg.getTotalSplits() * lg.getTotalSplits() - (lg.getHeight() / lg.getTotalSplits());
 			}
 		}
 
 		this.setBounds(0, wherePixel, lg.getWidth(), lg.getHeight() / (lg.getTotalSplits()));
 		final boolean zoomedOut = OpenLogViewer.getInstance().getEntireGraphingPanel().isZoomedOutBeyondOneToOne();
 		if(zoomedOut){
 			initGraphZoomedOut();
 		} else {
 			initGraphZoomed();
 		}
 	}
 
 	/**
 	 * Graph total size
 	 * @return GDE.size()
 	 */
 	public final int graphSize() {
 		return availableDataRecords;
 	}
 }
