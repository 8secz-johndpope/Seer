 package com.kh.beatbot.view;
 
 import java.nio.FloatBuffer;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.microedition.khronos.opengles.GL10;
 
 import android.content.Context;
 import android.os.Bundle;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.MotionEvent;
 import android.view.SurfaceHolder;
 
 import com.kh.beatbot.manager.MidiManager;
 import com.kh.beatbot.manager.PlaybackManager;
 import com.kh.beatbot.manager.RecordManager;
 import com.kh.beatbot.midi.MidiNote;
 import com.kh.beatbot.view.bean.MidiViewBean;
 import com.kh.beatbot.view.helper.LevelsViewHelper;
 import com.kh.beatbot.view.helper.TickWindowHelper;
 import com.kh.beatbot.view.helper.WaveformHelper;
 
 public class MidiView extends SurfaceViewBase {
 
 	private MidiViewBean bean = new MidiViewBean();
 
 	private static final int[] V_LINE_WIDTHS = new int[] { 5, 3, 2 };
 	private static final float[] V_LINE_COLORS = new float[] { 0, .2f, .3f };
 	// NIO Buffers
 	private FloatBuffer[] vLineVB = new FloatBuffer[3];
 	private FloatBuffer hLineVB = null;
 	private FloatBuffer tickFillVB = null;
 	private FloatBuffer selectRegionVB = null;
 	private FloatBuffer loopMarkerVB = null;
 	private FloatBuffer loopMarkerLineVB = null;
 	private FloatBuffer loopSquareVB = null;
 
 	private int[] textures = new int[1];
 
 	private MidiManager midiManager;
 	private RecordManager recordManager;
 	private PlaybackManager playbackManager;
 
 	// map of pointerIds to the notes they are selecting
 	private Map<Integer, MidiNote> touchedNotes = new HashMap<Integer, MidiNote>();
 
 	// map of pointerIds to the original on-ticks of the notes they are touching
 	// (before dragging)
 	private Map<Integer, Long> startOnTicks = new HashMap<Integer, Long>();
 
 	public enum State {
 		LEVELS_VIEW, NORMAL_VIEW, TO_LEVELS_VIEW, TO_NORMAL_VIEW
 	};
 
 	private TickWindowHelper tickWindow;
 	private LevelsViewHelper levelsHelper;
 	private WaveformHelper waveformHelper;
 
 	public MidiView(Context context, AttributeSet attrs) {
 		super(context, attrs);
 		bean.setHeight(height);
 		bean.setWidth(width);
 		for (int i = 0; i < 5; i++) {
 			bean.setDragOffsetTick(i, 0);
 		}
 	}
 
 	public void setMidiManager(MidiManager midiManager) {
 		this.midiManager = midiManager;
 		bean.setAllTicks(midiManager.RESOLUTION * 4);
 		bean.setYOffset(21);
 		tickWindow = new TickWindowHelper(bean, 0, bean.getAllTicks() - 1);
 	}
 
 	public MidiManager getMidiManager() {
 		return midiManager;
 	}
 
 	public MidiViewBean getBean() {
 		return bean;
 	}
 
 	public GL10 getGL10() {
 		return gl;
 	}
 
 	public TickWindowHelper getTickWindow() {
 		return tickWindow;
 	}
 
 	public void setRecordManager(RecordManager recorder) {
 		this.recordManager = recorder;
 	}
 
 	public void setPlaybackManager(PlaybackManager playbackManager) {
 		this.playbackManager = playbackManager;
 	}
 
 	public State getViewState() {
 		return bean.getViewState();
 	}
 
 	public void handleUndo() {
 		levelsHelper.resetSelected();
 	}
 
 	public void setViewState(State viewState) {
 		if (viewState == State.TO_LEVELS_VIEW
 				|| viewState == State.TO_NORMAL_VIEW)
 			return;
 		bean.setViewState(viewState);
 		if (viewState == State.LEVELS_VIEW)
 			bean.setBgColor(0);
 		else
 			bean.setBgColor(.3f);
 	}
 
 	public void setLevelMode(LevelsViewHelper.LevelMode levelMode) {
 		levelsHelper.setLevelMode(levelMode);
 	}
 
 	public void reset() {
 		tickWindow.setTickOffset(0);
 	}
 
 	public void drawWaveform(byte[] bytes) {
 		waveformHelper.addBytesToQueue(bytes);
 	}
 
 	public void endWaveform() {
 		waveformHelper.endWaveform();
 	}
 
 	private void selectRegion(float x, float y) {
 		long tick = xToTick(x);
 		long leftTick = Math.min(tick, bean.getSelectRegionStartTick());
 		long rightTick = Math.max(tick, bean.getSelectRegionStartTick());
 		float topY = Math.min(y, bean.getSelectRegionStartY());
 		float bottomY = Math.max(y, bean.getSelectRegionStartY());
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.selectRegion(leftTick, rightTick, topY, bottomY);
 		} else {
 			int topNote = yToNote(Math.min(y, bean.getSelectRegionStartY()));
 			int bottomNote = yToNote(Math.max(y, bean.getSelectRegionStartY()));
 			midiManager.selectRegion(leftTick, rightTick, topNote, bottomNote);
 			// for normal view, round the drawn rectangle to nearest notes
 			topY = noteToY(topNote);
 			bottomY = noteToY(bottomNote + 1);
 		}
 		// make room in the view window if we are dragging out of the view
 		tickWindow.updateView(leftTick, rightTick);
 		initSelectRegionVB(leftTick, rightTick, topY, bottomY);
 	}
 
 	private void selectMidiNote(float x, float y, int pointerId) {
 		long tick = xToTick(x);
 		long note = yToNote(y);
 
 		for (int i = 0; i < midiManager.getMidiNotes().size(); i++) {
 			MidiNote midiNote = midiManager.getMidiNotes().get(i);
 			if (midiNote.getNoteValue() == note && midiNote.getOnTick() <= tick
 					&& midiNote.getOffTick() >= tick) {
 				if (!touchedNotes.containsValue(midiNote)) {
 					startOnTicks.put(pointerId, midiNote.getOnTick());
 					long leftOffset = tick - midiNote.getOnTick();
 					bean.setDragOffsetTick(pointerId, leftOffset);
 					// don't need right offset for simple drag (one finger
 					// select)
 
 					// If this is the only touched midi note, and it hasn't yet
 					// been selected, make it the only selected note.
 					// If we are multi-selecting, add it to the selected list
 					if (!midiNote.isSelected()) {
 						if (touchedNotes.isEmpty())
 							midiManager.deselectAllNotes();
 						midiNote.setSelected(true);
 					}
 					touchedNotes.put(pointerId, midiNote);
 				}
 				return;
 			}
 		}
 	}
 
 	private void selectLoopMarker(int pointerId, float x) {
 		float loopBeginX = tickToX(midiManager.getLoopBeginTick());
 		float loopEndX = tickToX(midiManager.getLoopEndTick());
 		if (Math.abs(x - loopBeginX) <= 20) {
 			bean.setLoopPointerId(0, pointerId);
 		} else if (x > loopBeginX && x < loopEndX) {
 			bean.setLoopPointerId(1, pointerId);
 			bean.setLoopSelectionOffset(x - loopBeginX);
 		} else if (Math.abs(x - loopEndX) <= 20) {
 			bean.setLoopPointerId(2, pointerId);			
 		}
 	}
 
 	private void drawHorizontalLines() {
 		gl.glColor4f(0, 0, 0, 1);
 		gl.glLineWidth(2);
 		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, hLineVB);
 		gl.glDrawArrays(GL10.GL_LINES, 0, hLineVB.capacity() / 2);
 	}
 
 	private void drawVerticalLines() {
 		// distance between one primary (LONG) tick to the next
 		float translateDist = tickWindow.getMajorTickSpacing() * 4f * width
 				/ tickWindow.getNumTicks();
 		// start at the first primary tick before display start
 		float startX = tickToX(tickWindow.getPrimaryTickToLeftOf(tickWindow
 				.getTickOffset()));
 		// end at the first primary tick after display end
 		float endX = tickToX(tickWindow.getPrimaryTickToLeftOf(tickWindow
 				.getTickOffset() + tickWindow.getNumTicks()))
 				+ translateDist;
 
 		gl.glPushMatrix();
 		gl.glTranslatef(startX, 0, 0);
 		for (int i = 0; i < 3; i++) {
 			float color = V_LINE_COLORS[i];
 			gl.glColor4f(color, color, color, 1); // appropriate line color
 			gl.glLineWidth(V_LINE_WIDTHS[i]); // appropriate line width
 			gl.glPushMatrix();
 			for (float x = startX; x < endX; x += translateDist) {
 				gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vLineVB[i]);
 				gl.glDrawArrays(GL10.GL_LINES, 0, 2);
 				gl.glTranslatef(translateDist, 0, 0);
 			}
 			gl.glPopMatrix();
 			if (i == 0) {
 				gl.glTranslatef(translateDist / 2, 0, 0);
 			} else if (i == 1) {
 				translateDist /= 2;
 				gl.glTranslatef(-translateDist / 2, 0, 0);
 			}
 		}
 		gl.glPopMatrix();
 	}
 
 	private void drawCurrentTick() {
 		float xLoc = tickToX(midiManager.getCurrTick());
 		float[] vertLine = new float[] { xLoc, width, xLoc, 0 };
 		FloatBuffer lineBuff = makeFloatBuffer(vertLine);
 		gl.glColor4f(1, 1, 1, 0.5f);
 		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, lineBuff);
 		gl.glDrawArrays(GL10.GL_LINES, 0, 2);
 	}
 
 	private void drawLoopMarker() {
 		float[][] color = new float[2][3];
 		color[0] = bean.getLoopPointerIds()[0] != -1 ? MidiViewBean.TICK_SELECTED_COLOR
 				: MidiViewBean.TICK_MARKER_COLOR;
 		color[1] = bean.getLoopPointerIds()[2] != -1 ? MidiViewBean.TICK_SELECTED_COLOR
 				: MidiViewBean.TICK_MARKER_COLOR;
 		gl.glLineWidth(6);
 		float[] loopMarkerLocs = { tickToX(midiManager.getLoopBeginTick()),
 				tickToX(midiManager.getLoopEndTick()) };
 		for (int i = 0; i < 2; i++) {
 			float loopMarkerLoc = loopMarkerLocs[i];
 			gl.glColor4f(color[i][0], color[i][1], color[i][2], 1);
 			gl.glPushMatrix();
 			gl.glTranslatef(loopMarkerLoc, 0, 0);
 			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, loopMarkerVB);
 			gl.glDrawArrays(GL10.GL_TRIANGLES, i * 3, 3);
 			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, loopMarkerLineVB);
 			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
 			gl.glPopMatrix();
 		}
 	}
 
 	private void drawTickFill() {
 		drawTriangleStrip(tickFillVB, MidiViewBean.TICK_FILL_COLOR);
 		drawLoopBar();
 	}
 
 	private void drawLoopBar() {
 		float[] color = bean.getLoopPointerIds()[1] == -1 ? MidiViewBean.TICKBAR_COLOR
 				: MidiViewBean.TICK_SELECTED_COLOR;
 		// entire loop bar is selected. draw darker square
 		drawRectangle(tickToX(midiManager.getLoopBeginTick()), 0,
 				tickToX(midiManager.getLoopEndTick()), bean.getYOffset(), color);
 	}
 
 	private void drawLoopSquare() {
 		float gray = 1.3f * bean.getBgColor();
 		float[] color = new float[] { gray, gray, gray, .6f };
 		drawTriangleStrip(loopSquareVB, color);
 	}
 
 	private void drawRecordingWaveforms() {
 		ArrayList<FloatBuffer> waveformVBs = waveformHelper
 				.getCurrentWaveformVBs();
 		if (recordManager.isRecording() && !waveformVBs.isEmpty()) {
 			FloatBuffer last = waveformVBs.get(waveformVBs.size() - 1);
 			float waveWidth = last.get(last.capacity() - 2);
 			float noteWidth = tickToX(midiManager.getCurrTick()
 					- recordManager.getRecordStartTick());
 			gl.glPushMatrix();
 			gl.glTranslatef(tickToX(recordManager.getRecordStartTick()), 0, 0);
 			// scale drawing so the entire waveform exactly fits in the note
 			// width
 			gl.glScalef(noteWidth / waveWidth, 1, 1);
 			for (int i = 0; i < waveformVBs.size(); i++) {
 				drawLines(waveformVBs.get(i), MidiViewBean.WAVEFORM_COLOR, 1,
 						GL10.GL_LINE_STRIP);
 			}
 			gl.glPopMatrix();
 		}
 	}
 
 	public void initSelectRegionVB(long leftTick, long rightTick, float topY,
 			float bottomY) {
 		selectRegionVB = makeRectFloatBuffer(tickToX(leftTick), topY,
 				tickToX(rightTick), bottomY);
 	}
 
 	private void drawSelectRegion() {
 		if (!bean.isSelectRegion() || selectRegionVB == null)
 			return;
 		drawTriangleStrip(selectRegionVB, new float[] { .6f, .6f, 1, .7f });
 	}
 
 	private void drawAllMidiNotes() {
 		// not using for-each to avoid concurrent modification
 		for (int i = 0; i < midiManager.getMidiNotes().size(); i++) {
 			if (midiManager.getMidiNotes().size() <= i)
 				break;
 			MidiNote midiNote = midiManager.getMidiNote(i);
 			if (midiNote != null) {
 				drawMidiNote(
 						midiNote,
 						midiNote.isSelected() ? MidiViewBean.NOTE_SELECTED_COLOR
 								: MidiViewBean.NOTE_COLOR);
 			}
 		}
 	}
 
 	public void drawMidiNote(MidiNote midiNote, float[] color) {
 		// midi note rectangle coordinates
 		float x1 = tickToX(midiNote.getOnTick());
 		float y1 = noteToY(midiNote.getNoteValue());
 		float x2 = tickToX(midiNote.getOffTick());
 		float y2 = y1 + bean.getNoteHeight();
 		// fade outline from black to white
 		float baseColor = (1 - bean.getBgColor() * 2);
 		drawRectangle(x1, y1, x2, y2, color);
 		drawRectangleOutline(x1, y1, x2, y2, new float[] { baseColor,
 				baseColor, baseColor, 1 }, 4);
 	}
 
 	private void initTickFillVB() {
 		tickFillVB = makeRectFloatBuffer(0, 0, width, bean.getYOffset());
 	}
 
 	private void initLoopSquareVB() {
 		loopSquareVB = makeRectFloatBuffer(
 				tickToX(midiManager.getLoopBeginTick()), bean.getYOffset(),
 				tickToX(midiManager.getLoopEndTick()), height);
 	}
 
 	private void initHLineVB() {
 		float[] hLines = new float[(midiManager.getNumSamples() + 2) * 4];
 		hLines[0] = 0;
 		hLines[1] = 0;
 		hLines[2] = width;
 		hLines[3] = 0;
 		float y = bean.getYOffset();
 		for (int i = 1; i < midiManager.getNumSamples() + 2; i++) {
 			hLines[i * 4] = 0;
 			hLines[i * 4 + 1] = y;
 			hLines[i * 4 + 2] = width;
 			hLines[i * 4 + 3] = y;
 			y += bean.getNoteHeight();
 		}
 		hLineVB = makeFloatBuffer(hLines);
 	}
 
 	private void initVLineVBs() {
 		// height of the bottom of the record row
 		float y1 = bean.getYOffset();
 
 		for (int i = 0; i < 3; i++) {
 			// 4 vertices per line
 			float[] line = new float[4];
 			line[0] = 0;
 			line[1] = y1 - y1 / (i + 1.5f);
 			line[2] = 0;
 			line[3] = bean.getHeight();
 			vLineVB[i] = makeFloatBuffer(line);
 		}
 	}
 
 	private void initLoopMarkerVBs() {
 		float h = bean.getYOffset();
 		float[] loopMarkerLine = new float[] { 0, 0, 0, bean.getHeight() };
 		float[] loopMarkerTriangles = new float[] { 0, 0, 0, h, h, h / 2, // loop
 																			// begin
 																			// triangle,
 																			// pointing
 																			// right
 				0, 0, 0, h, -h, h / 2 }; // loop end triangle, pointing left
 		loopMarkerLineVB = makeFloatBuffer(loopMarkerLine);
 		loopMarkerVB = makeFloatBuffer(loopMarkerTriangles);
 	}
 
 	public float tickToX(long tick) {
 		return (float) (tick - tickWindow.getTickOffset())
 				/ tickWindow.getNumTicks() * bean.getWidth();
 	}
 
 	public long xToTick(float x) {
 		return (long) (tickWindow.getNumTicks() * x / bean.getWidth() + tickWindow
 				.getTickOffset());
 	}
 
 	public int yToNote(float y) {
 		if (y >= 0 && y < bean.getYOffset())
 			return -1;
 		return (int) (midiManager.getNumSamples() * (y - bean.getYOffset()) / (bean
 				.getHeight() - bean.getYOffset()));
 	}
 
 	public float noteToY(int note) {
 		return note * bean.getNoteHeight() + bean.getYOffset();
 	}
 
 	public void signalRecording() {
 		waveformHelper.start();
 	}
 
 	private void renderBackgroundTexture() {
 		float vertices[] = { -width, -height, -width, height, width, -height,
 				width, height };
 
 		float texture[] = { 0, 1, 0, 0, 1, 1, 1, 0 };
 
 		FloatBuffer vertexBuffer = makeFloatBuffer(vertices);
 		FloatBuffer textureBuffer = makeFloatBuffer(texture);
 		// bind the previously generated texture
 		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
 
 		// Point to our buffers
 		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
 		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
 
 		// Set the face rotation
 		gl.glFrontFace(GL10.GL_CW);
 
 		// Point to our vertex buffer
 		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
 		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
 
 		// Draw the vertices as triangle strip
 		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertexBuffer.capacity() / 2);
 	}
 
 	protected void init() {
 		levelsHelper = new LevelsViewHelper(this);
 		// waveformHelper constructor: yPos, height
 		waveformHelper = new WaveformHelper();
 		tickWindow.updateGranularity();
 		float color = bean.getBgColor();
 		gl.glClearColor(color, color, color, 1.0f);
 		gl.glEnable(GL10.GL_POINT_SMOOTH);
 		// gl.glEnable(GL10.GL_TEXTURE_2D);
 		// Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
 		// R.drawable.background);
 		// loadTexture(gl, bitmap, textures);
 		initHLineVB();
 		initVLineVBs();
 		initLoopMarkerVBs();
 		initLoopSquareVB();
 		initTickFillVB();
 	}
 
 	@Override
 	protected void drawFrame() {
 		// renderBackgroundTexture();
 		boolean recording = recordManager.getState() == RecordManager.State.LISTENING
 				|| recordManager.getState() == RecordManager.State.RECORDING;
 		if (bean.getViewState() == State.TO_LEVELS_VIEW
 				|| bean.getViewState() == State.TO_NORMAL_VIEW) { // transitioning
 			float amt = .5f / 30;
 			bean.setBgColor(bean.getViewState() == State.TO_LEVELS_VIEW ? bean
 					.getBgColor() - amt : bean.getBgColor() + amt);
 			gl.glClearColor(bean.getBgColor(), bean.getBgColor(),
 					bean.getBgColor(), 1);
 			if (bean.getBgColor() >= .5f || bean.getBgColor() <= 0) {
 				bean.setViewState(bean.getBgColor() >= .5f ? State.NORMAL_VIEW
 						: State.LEVELS_VIEW);
 			}
 		}
 		tickWindow.scroll();
 		initLoopSquareVB();
 		drawTickFill();
 		drawLoopSquare();
 		if (recording
 				|| playbackManager.getState() == PlaybackManager.State.PLAYING) {
 			// if we're recording, keep the current recording tick in view.
 			if (recording
 					&& midiManager.getCurrTick() > tickWindow.getTickOffset()
 							+ tickWindow.getNumTicks())
 				tickWindow.setNumTicks(midiManager.getCurrTick()
 						- tickWindow.getTickOffset());
 			drawCurrentTick();
 		}
 		if (bean.getViewState() != State.LEVELS_VIEW) {
 			// normal or transitioning view. draw lines
 			drawHorizontalLines();
 			drawVerticalLines();
 		}
 		if (bean.getViewState() != State.NORMAL_VIEW) {
 			levelsHelper.drawFrame();
 		} else {
 			drawLoopMarker();
 			drawAllMidiNotes();
 		}
 		drawSelectRegion();
 		if (bean.isScrolling()
 				|| bean.getScrollVelocity() != 0
 				|| Math.abs(System.currentTimeMillis()
 						- bean.getScrollViewEndTime()) <= MidiViewBean.DOUBLE_TAP_TIME * 2) {
 			drawScrollView();
 		}
 		drawRecordingWaveforms();
 	}
 
 	private void drawScrollView() {
 		// if scrolling is still in progress, elapsed time is relative to the
 		// time of scroll start,
 		// otherwise, elapsed time is relative to scroll end time
 		boolean scrollingEnded = bean.getScrollViewStartTime() < bean
 				.getScrollViewEndTime();
 		long elapsedTime = scrollingEnded ? System.currentTimeMillis()
 				- bean.getScrollViewEndTime() : System.currentTimeMillis()
 				- bean.getScrollViewStartTime();
 
 		float alpha = .8f;
 		if (!scrollingEnded && elapsedTime <= MidiViewBean.DOUBLE_TAP_TIME)
 			alpha *= elapsedTime / (float) MidiViewBean.DOUBLE_TAP_TIME;
 		else if (scrollingEnded && elapsedTime > MidiViewBean.DOUBLE_TAP_TIME)
 			alpha *= (MidiViewBean.DOUBLE_TAP_TIME * 2 - elapsedTime)
 					/ (float) MidiViewBean.DOUBLE_TAP_TIME;
 
 		float x1 = tickWindow.getTickOffset() * width
 				/ tickWindow.getMaxTicks();
 		float x2 = (tickWindow.getTickOffset() + tickWindow.getNumTicks())
 				* width / tickWindow.getMaxTicks();
 		drawRectangle(x1, bean.getHeight() - 20, x2, bean.getHeight(),
 				new float[] { 1, 1, 1, alpha });
 	}
 
 	private long getAdjustedTickDiff(long tickDiff, int pointerId,
 			MidiNote singleNote) {
 		if (tickDiff == 0)
 			return 0;
 		long adjustedTickDiff = tickDiff;
 		for (MidiNote selectedNote : midiManager.getSelectedNotes()) {
 			if (singleNote != null && !selectedNote.equals(singleNote))
 				continue;
 			if (Math.abs(startOnTicks.get(pointerId) - selectedNote.getOnTick())
 					+ Math.abs(tickDiff) <= 10) {
 				// inside threshold distance - set to original position
 				return startOnTicks.get(pointerId) - selectedNote.getOnTick();
 			}
 			if (selectedNote.getOnTick() + tickDiff < 0) {
 				if (selectedNote.getOnTick() > adjustedTickDiff)
 					adjustedTickDiff = -selectedNote.getOnTick();
 			} else if (selectedNote.getOffTick() + tickDiff > tickWindow
 					.getMaxTicks()) {
 				if (tickWindow.getMaxTicks() - selectedNote.getOffTick() < adjustedTickDiff)
 					adjustedTickDiff = tickWindow.getMaxTicks()
 							- selectedNote.getOffTick();
 			}
 		}
 		return adjustedTickDiff;
 	}
 
 	private int getAdjustedNoteDiff(int noteDiff) {
 		if (noteDiff == 0)
 			return 0;
 		int adjustedNoteDiff = noteDiff;
 		for (MidiNote selectedNote : midiManager.getSelectedNotes()) {
 			if (selectedNote.getNoteValue() + noteDiff < 0
 					&& selectedNote.getNoteValue() > adjustedNoteDiff)
 				adjustedNoteDiff = -selectedNote.getNoteValue();
 			else if (selectedNote.getNoteValue() + noteDiff > midiManager
 					.getNumSamples() - 1
 					&& midiManager.getNumSamples() - 1
 							- selectedNote.getNoteValue() < adjustedNoteDiff)
 				adjustedNoteDiff = midiManager.getNumSamples() - 1
 						- selectedNote.getNoteValue();
 		}
 		return adjustedNoteDiff;
 	}
 
 	private void pinchNote(MidiNote midiNote, long onTickDiff, long offTickDiff) {
 		long newOnTick = midiNote.getOnTick() + onTickDiff;
 		long newOffTick = midiNote.getOffTick();
 		if (midiNote.getOffTick() + offTickDiff <= tickWindow.getMaxTicks())
 			newOffTick = midiNote.getOffTick() + offTickDiff;
 		midiManager.setNoteTicks(midiNote, newOnTick, newOffTick,
 				bean.isSnapToGrid());
 		bean.setStateChanged(true);
 	}
 
 	private void singleTap(float x, float y, MidiNote touchedNote) {
 		bean.setLastTapX(x);
 		bean.setLastTapY(y);
 		bean.setLastTapTime(System.currentTimeMillis());
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.selectLevelNote(x, y);
 		} else {
 			if (touchedNote != null) {
 				// single tapping a note always makes it the only selected note
 				if (touchedNote.isSelected())
 					midiManager.deselectAllNotes();
 				touchedNote.setSelected(true);
 			} else {
 				int note = yToNote(y);
 				long tick = xToTick(x);
 				// if no note is touched, than this tap deselects all notes
 				if (midiManager.anyNoteSelected()) {
 					midiManager.deselectAllNotes();
 				} else { // add a note based on the current tick granularity
 					if (note >= 0) {
 						addMidiNote(tick, note);
 						bean.setStateChanged(true);
 					}
 				}
 			}
 		}
 	}
 
 	private void startSelectRegion(float x, float y) {
 		bean.setSelectRegionStartTick(xToTick(x));
 		if (bean.getViewState() == State.LEVELS_VIEW)
 			bean.setSelectRegionStartY(y);
 		else
 			bean.setSelectRegionStartY(noteToY(yToNote(y)));
 		selectRegionVB = null;
 		bean.setSelectRegion(true);
 	}
 
 	private void doubleTap(MidiNote touchedNote) {
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.doubleTap();
 			return;
 		}
 		if (touchedNote != null) {
 			touchedNote.setSelected(false);
 			midiManager.removeNote(touchedNote);
 			bean.setStateChanged(true);
 		}
 		// reset tap time so that a third tap doesn't register as
 		// another double tap
 		bean.setLastTapTime(0);
 	}
 
 	// adds a note starting at the nearest major tick (nearest displayed
 	// grid line) to the left and ending one tick before the nearest major
 	// tick to the right of the given tick
 	private void addMidiNote(long tick, int note) {
 		long spacing = tickWindow.getMajorTickSpacing();
 		long onTick = tick - tick % spacing;
 		long offTick = onTick + spacing - 1;
 		addMidiNote(onTick, offTick, note);
 	}
 
 	public void addMidiNote(long onTick, long offTick, int note) {
 		MidiNote noteToAdd = midiManager.addNote(onTick, offTick, note, .75f,
 				.5f, .5f);
 		noteToAdd.setSelected(true);
 		handleMidiCollisions();
 		midiManager.mergeTempNotes();
 		noteToAdd.setSelected(false);
 	}
 
 	public void handleMidiCollisions() {
 		midiManager.clearTempNotes();
 		for (MidiNote selected : midiManager.getSelectedNotes()) {
 			for (int i = 0; i < midiManager.getMidiNotes().size(); i++) {
 				MidiNote note = midiManager.getMidiNote(i);
 				if (note == null || selected.equals(note)
 						|| selected.getNoteValue() != note.getNoteValue()) {
 					continue;
 				}
 				// if a selected note begins in the middle of another note,
 				// clip the covered note
 				if (selected.getOnTick() > note.getOnTick()
 						&& selected.getOnTick() <= note.getOffTick()) {
 					MidiNote copy = note.getCopy();
 					copy.setOffTick(selected.getOnTick() - 1);
 					// update the native midi events
 					midiManager.moveMidiNoteTicks(note.getNoteValue(),
 							note.getOnTick(), copy.getOnTick(),
 							note.getOffTick(), copy.getOffTick());
 					midiManager.putTempNote(i, copy);
 					// if the selected note ends after the beginning
 					// of the other note, or if the selected note completely
 					// covers the other note, delete the covered note
 				} else if (selected.getOffTick() >= note.getOnTick()
 						&& selected.getOffTick() <= note.getOffTick()
 						|| selected.getOnTick() <= note.getOnTick()
 						&& selected.getOffTick() >= note.getOffTick()) {
 					midiManager.setNoteMute(note.getNoteValue(),
 							note.getOnTick(), true);
 					midiManager.putTempNote(i, null);
 				}
 			}
 		}
 	}
 
 	public boolean toggleSnapToGrid() {
 		return bean.toggleSnapToGrid();
 	}
 
 	private void startScrollView() {
 		long now = System.currentTimeMillis();
 		if (now - bean.getScrollViewEndTime() > MidiViewBean.DOUBLE_TAP_TIME * 2)
 			bean.setScrollViewStartTime(now);
 		else
 			bean.setScrollViewEndTime(Long.MAX_VALUE);
 		bean.setScrolling(true);
 	}
 
 	public void toggleLevelsView() {
 		if (bean.getViewState() == State.NORMAL_VIEW
 				|| bean.getViewState() == State.TO_NORMAL_VIEW) {
 			levelsHelper.resetSelected();
 			bean.setViewState(State.TO_LEVELS_VIEW);
 		} else {
 			bean.setViewState(State.TO_NORMAL_VIEW);
 		}
 	}
 
 	private void dragNotes(boolean dragAllSelected, int pointerId,
 			long currTick, int currNote) {
 		MidiNote touchedNote = touchedNotes.get(pointerId);
 		if (touchedNote == null)
 			return;
 		int noteDiff = currNote - touchedNote.getNoteValue();
 		long tickDiff = currTick - bean.getDragOffsetTick(pointerId)
 				- touchedNote.getOnTick();
 		tickDiff = getAdjustedTickDiff(tickDiff, pointerId, null);
 		noteDiff = getAdjustedNoteDiff(noteDiff);
 		List<MidiNote> notesToDrag = dragAllSelected ? midiManager
 				.getSelectedNotes() : Arrays.asList(touchedNote);
 		// dragging one note - drag all selected notes together
 		for (MidiNote midiNote : notesToDrag) {
 			midiManager.setNoteTicks(midiNote, midiNote.getOnTick() + tickDiff,
 					midiNote.getOffTick() + tickDiff, bean.isSnapToGrid());
 			midiManager.setNoteValue(midiNote, midiNote.getNoteValue()
 					+ noteDiff);
 		}
 		if (noteDiff > 0 || tickDiff > 0)
 			bean.setStateChanged(true);
 	}
 
 	private void pinchSelectedNotes(long currLeftTick, long currRightTick) {
 		MidiNote touchedNote = touchedNotes.values().iterator().next();
 		long onTickDiff = currLeftTick - touchedNote.getOnTick()
 				- bean.getPinchLeftOffset();
 		long offTickDiff = currRightTick - touchedNote.getOffTick()
 				+ bean.getPinchRightOffset();
 		for (MidiNote midiNote : midiManager.getSelectedNotes()) {
 			pinchNote(midiNote, onTickDiff, offTickDiff);
 		}
 	}
 
 	private void updateLoopMarkers(MotionEvent e) {
 		for (int i = 0; i < 3; i++) {			
 			if (bean.getLoopPointerIds()[i] != -1) {
 				float x = e.getX(e.findPointerIndex(bean.getLoopPointerIds()[i]));
 				long majorTick = tickWindow
 						.getMajorTickToLeftOf(xToTick(x));				
 				if (i == 0) { // begin loop marker selected
 					midiManager.setLoopBeginTick(majorTick);
 				} else if (i == 1) { // end loop marker selected
 					long newOnTick = tickWindow.getMajorTickToLeftOf(xToTick(x -
 							bean.getLoopSelectionOffset()));
 					long newOffTick = midiManager.getLoopEndTick() + newOnTick
 							- midiManager.getLoopBeginTick();
 					if (newOnTick >= 0
 							&& newOffTick <= tickWindow.getMaxTicks()) {
 						midiManager.setLoopBeginTick(newOnTick);
 						midiManager.setLoopEndTick(newOffTick);
 					}
 				} else { // middle selected. move begin and end
 					midiManager.setLoopEndTick(majorTick);
 				}
 				tickWindow.updateView(midiManager.getLoopBeginTick(),
 						midiManager.getLoopEndTick());				
 			}
 		}
 	}
 
 	public void noMidiMove(MotionEvent e) {
 		if (e.getPointerCount()  - bean.getNumLoopMarkersSelected() == 1) {
 			if (bean.isSelectRegion()) { // update select region
 				selectRegion(e.getX(0), e.getY(0));
 			} else {
 				// one finger scroll
 				bean.setScrollVelocity(tickWindow.scroll(e.getX(e.findPointerIndex(bean.getScrollPointerId()))));
 			}
 		} else if (e.getPointerCount()  - bean.getNumLoopMarkersSelected() == 2) {
 			// two finger zoom
 			float leftX = Math.min(e.getX(0), e.getX(1));
 			float rightX = Math.max(e.getX(0), e.getX(1));
 			tickWindow.zoom(leftX, rightX);
 		}
 	}
 
 	public void writeToBundle(Bundle out) {
 		out.putInt("viewState", bean.getViewState().ordinal());
 	}
 
 	// use constructor first, and set the deets with this method
 	public void readFromBundle(Bundle in) {
 		setViewState(State.values()[in.getInt("viewState")]);
 	}
 
 	@Override
 	public void surfaceChanged(SurfaceHolder holder, int format, int width,
 			int height) {
 		super.surfaceChanged(holder, format, width, height);
 		bean.setWidth(width);
 		bean.setHeight(height);
 		bean.setMidiHeight(bean.getHeight() - bean.getYOffset());
 		bean.setNoteHeight(bean.getMidiHeight() / midiManager.getNumSamples());
 		bean.setLevelsHeight(bean.getMidiHeight()
 				- MidiViewBean.LEVEL_POINT_SIZE);
 	}
 
 	@Override
 	protected void handleActionDown(int id, float x, float y) {
 		bean.setLastDownTime(System.currentTimeMillis());
 		bean.setLastTapX(x);
 		bean.setLastTapY(y);
 		startScrollView();
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.selectLevel(x, y, id);
 		} else {
 			selectMidiNote(x, y, id);
 		}
 		if (touchedNotes.get(id) == null) {
 			// no note selected.
 			// check if loop marker selected
 			if (yToNote(y) == -1) {
 				selectLoopMarker(id, x);
 			} else {
 				// otherwise, enable scrolling
 				bean.setScrollAnchorTick(xToTick(x));
 				bean.setScrollPointerId(id);
 			}
 		}
 	}
 
 	@Override
 	protected void handleActionPointerDown(MotionEvent e, int id, float x,
 			float y) {
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.handleActionPointerDown(e, id, x, y);
 			return;
 		}
 		boolean noteAlreadySelected = !touchedNotes.isEmpty();
 		selectMidiNote(x, y, id);
 		if (e.getPointerCount() > 2)
 			return;
 		if (touchedNotes.get(id) == null) {
 			if (yToNote(y) == -1) {
 				selectLoopMarker(id, x);
 			} else {
 				long leftTick = xToTick(Math.min(e.getX(0), e.getX(1)));
 				long rightTick = xToTick(Math.max(e.getX(0), e.getX(1)));
 				if (noteAlreadySelected) {
 					// note is selected with one pointer, but this pointer
 					// did not select a note. start pinching all selected notes.
 					MidiNote touchedNote = touchedNotes.values().iterator()
 							.next();
 					int leftId = e.getX(e.findPointerIndex(0)) <= e.getX(e.findPointerIndex(1)) ? 0 : 1;
 					int rightId = (leftId + 1) % 2;
 					bean.setPinchLeftPointerId(leftId);
 					bean.setPinchRightPointerId(rightId);
 					bean.setPinchLeftOffset(leftTick - touchedNote.getOnTick());
 					bean.setPinchRightOffset(touchedNote.getOffTick()
 							- rightTick);
 					bean.setPinch(true);
 				} else if (bean.getNumLoopMarkersSelected() != 0) {
 					// otherwise, enable scrolling
 					bean.setScrollAnchorTick(xToTick(x));
 					bean.setScrollPointerId(id);
 				} else {
 					// init zoom anchors (the same ticks should be under the
 					// fingers at all times)
 					bean.setZoomLeftAnchorTick(leftTick);
 					bean.setZoomRightAnchorTick(rightTick);
 				}
 			}
 		}
 	}
 
 	@Override
 	protected void handleActionMove(MotionEvent e) {
 		if (Math.abs(e.getX() - bean.getLastTapX()) < 25
 				&& yToNote(e.getY()) == yToNote(bean.getLastTapY())) {
 			if (System.currentTimeMillis() - bean.getLastDownTime() > 500) {
 				startSelectRegion(e.getX(), e.getY());
 			}
 		} else {
 			bean.setLastDownTime(Long.MAX_VALUE);
 		}
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.handleActionMove(e);
 			return;
 		}
 		if (bean.isPinch()) {
 			long leftTick = xToTick(e.getX(e.findPointerIndex(bean.getPinchLeftPointerId())));
 			long rightTick = xToTick(e.getX(e.findPointerIndex(bean.getPinchRightPointerId())));
 			pinchSelectedNotes(leftTick, rightTick);
 		} else if (!touchedNotes.isEmpty()) { // at least one midi selected
			if (e.getPointerCount() - bean.getNumLoopMarkersSelected() == 1) {
 				// drag all selected notes together
 				dragNotes(true, e.getPointerId(0), xToTick(e.getX(0)),
 						yToNote(e.getY(0)));
 			} else {
 				// drag each touched note separately
 				for (int i = 0; i < e.getPointerCount(); i++) {
 					dragNotes(false, e.getPointerId(i), xToTick(e.getX(i)),
 							yToNote(e.getY(i)));
 				}
 			}
 			// make room in the view window if we are dragging out of the view
 			tickWindow.updateView(midiManager.getLeftMostSelectedTick(),
 					midiManager.getRightMostSelectedTick());
 			// handle any overlapping notes (clip or delete notes as
 			// appropriate)
 			handleMidiCollisions();
 		} else { // no midi selected. scroll, zoom, or update select region
 			noMidiMove(e);
 		}
 		updateLoopMarkers(e);
 	}
 
 	@Override
 	protected void handleActionPointerUp(MotionEvent e, int id, float x, float y) {
 		if (bean.getViewState() == State.LEVELS_VIEW) {
 			levelsHelper.handleActionPointerUp(e, id);
 			return;
 		}
 		touchedNotes.remove(id);
 		if (bean.getScrollPointerId() == id)
 			bean.setScrollPointerId(-1);
 		for (int i = 0; i < 3; i++)
 			if (bean.getLoopPointerIds()[i] == id)
 				bean.setLoopPointerId(i, -1);
 		int index = e.getActionIndex() == 0 ? 1 : 0;
 		if (e.getPointerCount() == 2) {
 			bean.setPinch(false);
 			bean.setScrollAnchorTick(xToTick(e.getX(index)));
 			bean.setScrollPointerId(e.getPointerId(index));
 		}
 	}
 
 	@Override
 	protected void handleActionUp(int id, float x, float y) {
 		bean.setScrolling(false);
 		for (int i = 0; i < 3; i++)
 			bean.setLoopPointerId(i, -1);
 		if (bean.getScrollVelocity() == 0)
 			bean.setScrollViewEndTime(System.currentTimeMillis());
 		bean.setSelectRegion(false);
 		midiManager.mergeTempNotes();
 		long time = System.currentTimeMillis();
 		if (Math.abs(time - bean.getLastDownTime()) < 200) {
 			// if the second tap is not in the same location as the first tap,
 			// no double tap :(
 			if (time - bean.getLastTapTime() < MidiViewBean.DOUBLE_TAP_TIME
 					&& Math.abs(x - bean.getLastTapX()) <= 25
 					&& yToNote(y) == yToNote(bean.getLastTapY())) {
 				doubleTap(touchedNotes.get(id));
 			} else {
 				singleTap(x, y, touchedNotes.get(id));
 			}
 		}
 		if (bean.isStateChanged())
 			midiManager.saveState();
 		bean.setStateChanged(false);
 		if (bean.getViewState() == State.LEVELS_VIEW)
 			levelsHelper.clearTouchedNotes();
 		else {
 			startOnTicks.clear();
 			touchedNotes.clear();
 		}
 		bean.setLastDownTime(Long.MAX_VALUE);
 	}
 }
