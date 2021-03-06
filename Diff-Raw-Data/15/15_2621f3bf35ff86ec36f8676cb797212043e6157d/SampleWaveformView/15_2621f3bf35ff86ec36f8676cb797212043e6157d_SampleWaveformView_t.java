 package com.kh.beatbot.view;
 
 import java.nio.FloatBuffer;
 import java.util.ArrayList;
 
 import javax.microedition.khronos.opengles.GL10;
 
 import android.content.Context;
 import android.util.AttributeSet;
 import android.view.MotionEvent;
 
 import com.kh.beatbot.manager.PlaybackManager;
 import com.kh.beatbot.view.bean.MidiViewBean;
 import com.kh.beatbot.view.helper.WaveformHelper;
 
 public class SampleWaveformView extends SurfaceViewBase {
 
 	private final float[][] ADSR_COLORS = new float[][] {{0, 1, 0, .7f},
 													    {1, .5f, .5f, .7f}, 
 													    {0, 0, 1, .7f},
 													    {1, 0, 1, .7f}}; 
 	private FloatBuffer waveformVB = null;
 	private FloatBuffer loopMarkerVB = null;
 	private FloatBuffer adsrPointVB = null;
 	private FloatBuffer[] adsrCurveVB = new FloatBuffer[4];
 	private PlaybackManager playbackManager = null;
 	
 	private final float[][] adsrPoints = new float[5][2];
 	// keep track of which pointer ids are selecting which ADSR points
 	// init to -1 to indicate no pointer is selecting
 	private int[] adsrSelected = new int[] {-1, -1, -1, -1, -1};
 	
 	private boolean showAdsr = true; // show ADSR points?
 	
 	// min distance for pointer to select loop markers
 	private final int SNAP_DIST = 35;
 	
 	private final int MIN_LOOP_WINDOW = 32;
 	// the left of this view is a preview button
 	private float previewButtonWidth, waveformWidth;
 
 	private int trackNum;
 	
 	private int numSamples = 0;
 
 	// which pointer id is touching which marker (-1 means no pointer)
 	private int beginLoopMarkerTouched = -1;
 	private int endLoopMarkerTouched = -1;
 	
 	// keep track of which finger is touching the preview button,
 	// so we can handle pointer-up/ move events outside the button
 	private int previewPointerId = -1;
 
 	private float sampleLoopBegin = 0;
 	private float sampleLoopEnd = 0; // to be set with the sample byte-length
 	
 	private float zoomLeftAnchorSample = -1;
 	private float zoomRightAnchorSample = -1;
 
 	private float scrollAnchorSample = -1;
 
 	private float[] samples = null;
 	// zooming/scrolling will change the view window of the samples
 	// keep track of that with offset and width
 	private float sampleOffset = 0;
 	private float sampleWidth;
 	
 	public SampleWaveformView(Context c, AttributeSet as) {
 		super(c, as);
 	}
 
 	public void setPlaybackManager(PlaybackManager playbackManager) {
 		this.playbackManager = playbackManager;
 	}
 
 	public void setTrackNum(int trackNum) {
 		this.trackNum = trackNum;
 	}
 
 	public void setSamples(float[] samples) {
 		this.samples = samples;
 		numSamples = samples.length;
 		sampleLoopEnd = numSamples;
 		sampleWidth = numSamples;	
 		if (height != 0) {
 			waveformVB = WaveformHelper.floatsToFloatBuffer(samples, height, 0);
 		}
 	}
 	
 	private void initDefaultAdsrPoints() {
 		for (int i = 0; i < 5; i++) {
 			// x coords
 			adsrPoints[i][0] = i/4f;
 		}
 		// y coords
 		adsrPoints[0][1] = 0;
 		adsrPoints[1][1] = .75f;
 		adsrPoints[2][1] = .60f;
 		adsrPoints[3][1] = .60f;
 		adsrPoints[4][1] = 0;
 	}
 	
 	private void initAdsrVB() {
 		float[] pointVertices = new float[10];
 		for (int i = 0; i < 5; i++) {			
 			for (int j = 0; j < 2; j++) {
 				pointVertices[i*2 + j] = j == 1 ? height - adsrPoints[i][j]*height : adsrToX(adsrPoints[i][j]);
 			}
 		}
 		adsrPointVB = makeFloatBuffer(pointVertices);
 		for (int i = 0; i < 4; i++) {
 			ArrayList<Float> curveVertices = new ArrayList<Float>();
 			curveVertices.addAll(makeExponentialCurveVertices(pointVertices[i*2],
 							     pointVertices[i*2 + 1], pointVertices[(i + 1)*2], pointVertices[(i + 1)*2 + 1]));
 			float[] converted = new float[curveVertices.size()];
 			for (int j = 0; j < curveVertices.size(); j++) {
 				converted[j] = curveVertices.get(j);
 			}
 			adsrCurveVB[i] = makeFloatBuffer(converted);			
 		}
 	}
 	
 	private ArrayList<Float> makeExponentialCurveVertices(float x1, float y1, float x2, float y2) {
 		ArrayList<Float> vertices = new ArrayList<Float>();
 		// fake it w/ Bezier curve
 		for (float t=0; t <= 1; t += 0.05) {
 			float bezierX = x1;
 			float bezierY = y2;
 			vertices.add((1-t)*(1-t)*x1 + 2*(1-t)*t*bezierX + t*t*x2);
 			vertices.add((1-t)*(1-t)*y1 + 2*(1-t)*t*bezierY + t*t*y2);
 		}
 		vertices.add(x2);
 		vertices.add(y2);
 		return vertices;
 	}
 	
 	private void drawWaveform() {
 		if (waveformVB == null)
 			return;
 		float scale = (waveformWidth*WaveformHelper.DEFAULT_SPP)/((float)sampleWidth);
 		float translate = -sampleOffset/WaveformHelper.DEFAULT_SPP;
 		float[] color = MidiViewBean.VOLUME_COLOR;		
 		gl.glLineWidth(10);
 		gl.glEnable(GL10.GL_LINE_SMOOTH);
 		gl.glColor4f(color[0], color[1], color[2], .9f);		
 		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, waveformVB);		
 		gl.glPushMatrix();
 		// scale drawing so the entire waveform exactly fits in the view
 		gl.glTranslatef(previewButtonWidth, 0, 0);
 		gl.glScalef(scale, 1, 1);
 		gl.glTranslatef(translate, 0, 0);		
 		gl.glDrawArrays(GL10.GL_LINE_STRIP, (int)(sampleOffset/WaveformHelper.DEFAULT_SPP),
 				(int)(sampleWidth/WaveformHelper.DEFAULT_SPP));
 		gl.glPopMatrix();
 	}
 
 	private void drawLoopMarkers() {
 		if (loopMarkerVB == null)
 			return;
 		gl.glColor4f(1, 1, 1, 1); // white for now
 		gl.glLineWidth(10); // width of 10 for now
 		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, loopMarkerVB);
 		gl.glDrawArrays(GL10.GL_LINES, 0, loopMarkerVB.capacity() / 2);
 		gl.glColor4f(1, .64706f, 0, .4f);
 		gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, loopMarkerVB.capacity()/2);
 	}
 	
 	private void drawAdsr() {
 		if (!showAdsr) return;
 		gl.glColor4f(0, 1, 0, 1); // green points for now
 		gl.glPointSize(10);
 		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, adsrPointVB);
		gl.glDrawArrays(GL10.GL_POINTS, 0, 5);
 		gl.glLineWidth(3);
 		for (int i = 0; i < adsrCurveVB.length; i++) {
 			gl.glColor4f(ADSR_COLORS[i][0], ADSR_COLORS[i][1], ADSR_COLORS[i][2],
 						 ADSR_COLORS[i][3]);
 			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, adsrCurveVB[i]);
 			gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, adsrCurveVB[i].capacity() / 2);
 		}
 	}
 
 	@Override
 	protected void init() {
 		// preview button is 80dpX80dp, but that is not the same as a height of '80'.  80dp will be the height		
 		previewButtonWidth = height;
 		waveformWidth = width - height;
 		while (samples == null)
 			; // wait until we're sure the sample bytes have been set
 		waveformVB = WaveformHelper.floatsToFloatBuffer(samples, height, 0);
 		gl.glEnable(GL10.GL_POINT_SMOOTH);
 		initLoopMarkerVB();
 		initDefaultAdsrPoints();
 		initAdsrVB();
 	}
 
 	private void initLoopMarkerVB() {
 		float xLoopBegin = sampleToX(sampleLoopBegin);
 		float xLoopEnd = sampleToX(sampleLoopEnd);
 		float[] loopMarkerVertices = { xLoopBegin, 0, xLoopBegin, height,
 				xLoopEnd, height, xLoopEnd, 0 };
 
 		loopMarkerVB = makeFloatBuffer(loopMarkerVertices);
 	}
 
 	float sampleToX(float sample) {
 		return (sample - sampleOffset)*waveformWidth/sampleWidth + previewButtonWidth;
 	}
 
 	float xToSample(float x) {
 		return (x - previewButtonWidth)*sampleWidth / waveformWidth + sampleOffset;
 	}
 
 	float adsrToX(float adsr) {
 		return sampleToX(sampleLoopBegin)*(1 - adsr) + adsr*sampleToX(sampleLoopEnd); 
 	}
 	
 	float xToAdsr(float x) {
 		return  (x - sampleToX(sampleLoopBegin))/(sampleToX(sampleLoopEnd) - sampleToX(sampleLoopBegin));
 	}
 	
 	float yToAdsr(float y) {
 		return 1 - y/height;	
 	}
 	
 	void updateSampleOffset(float scrollX) {
 		// set sampleOffset such that the scroll anchor sample stays under scrollX
 		float newSampleOffset = scrollAnchorSample - (scrollX - previewButtonWidth)*sampleWidth/waveformWidth;
 		sampleOffset = newSampleOffset < 0 ?
 				0 : (newSampleOffset + sampleWidth > numSamples ?
 						numSamples - sampleWidth : newSampleOffset);
 	}
 	
 	void updateZoom(float x1, float x2) {
 		// set sampleOffset and sampleWidth such that the zoom
 		// anchor samples stay under x1 and x2
 		float newSampleWidth = waveformWidth*(zoomRightAnchorSample - zoomLeftAnchorSample)/(x2 - x1);
 		float newSampleOffset = zoomRightAnchorSample - newSampleWidth*(x2 - previewButtonWidth)/waveformWidth;
 		if (newSampleOffset < 0 && newSampleOffset + newSampleWidth > numSamples ||
 				newSampleWidth < MIN_LOOP_WINDOW) {
 			return;
 		}
 		if (newSampleOffset >= 0 && newSampleOffset + newSampleWidth <= numSamples) {
 			sampleOffset = newSampleOffset;
 			sampleWidth = newSampleWidth;
 		} else if (newSampleOffset < 0) {
 			sampleOffset = 0;
 			sampleWidth = zoomRightAnchorSample*waveformWidth/(x2 - previewButtonWidth);
 		} else if (newSampleOffset + newSampleWidth > numSamples) {
 			sampleWidth = waveformWidth*(zoomLeftAnchorSample - numSamples)/(x1 - previewButtonWidth - waveformWidth);
 			sampleOffset = numSamples - sampleWidth;
 		}
 	}
 	
 	void updateLoopMarkers() {
 		float diff = 0;
 		if (sampleLoopBegin < sampleOffset && sampleLoopEnd > sampleOffset + sampleWidth) {
 			clipLoopToWindow();
 		} else if (sampleLoopBegin < sampleOffset)
 			diff = sampleOffset - sampleLoopBegin;
 		else if (sampleLoopEnd >= sampleOffset + sampleWidth)
 			diff = sampleOffset + sampleWidth - sampleLoopEnd;
 		if (diff != 0) {			
 			sampleLoopBegin += diff;
 			sampleLoopEnd += diff;
 			clipLoopToWindow();
 		}
 		playbackManager.setLoopWindow(trackNum, (int)sampleLoopBegin, (int)sampleLoopEnd);
 		// update the display location of the loop markers
 		initLoopMarkerVB();
 		initAdsrVB();
 	}
 	
 	@Override
 	protected void drawFrame() {
 		gl.glClearColor(.2f, .2f, .2f, 1);
 		drawWaveform();
 		drawLoopMarkers();
 		drawAdsr();
 	}
 
 	private boolean selectAdsrPoint(int id, float x, float y) {
 		for (int i = 0; i < 5; i++) {
 			if (Math.abs(adsrToX(adsrPoints[i][0]) - x) < SNAP_DIST &&
 					Math.abs((1 - adsrPoints[i][1])*height - y) < SNAP_DIST) {
 				adsrSelected[i] = id;
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	private boolean selectLoopMarker(int id, float x) {
 		if (Math.abs(x - sampleToX(sampleLoopBegin)) < SNAP_DIST) {
 			// begin loop marker touched
 			beginLoopMarkerTouched = id;
 			return true;
 		} else if (Math.abs(x - sampleToX(sampleLoopEnd)) < SNAP_DIST) {
 			// end loop marker touched
 			endLoopMarkerTouched = id;
 			return true;
 		}
 		return false;
 	}
 
 	private void deselectLoopMarker(int id) {
 		if (beginLoopMarkerTouched == id) {
 			beginLoopMarkerTouched = -1;
 		} else if (endLoopMarkerTouched == id) {
 			endLoopMarkerTouched = -1;
 		}
 	}
 
 	private void deselectAdsrPoint(int id) {
 		for (int i = 0; i < adsrSelected.length; i++) {
 			if (adsrSelected[i] == id)
 				adsrSelected[i] = -1;
 		}
 	}
 	
 	private void clearAdsrSelected() {
 		for (int i = 0; i < adsrSelected.length; i++) {
 			adsrSelected[i] = -1;
 		}
 	}
 	
 	private boolean moveAdsrPoint(int id, float x, float y) {
 		for (int i = 0; i < adsrSelected.length; i++) {
 			if (adsrSelected[i] == id) {
 				float adsrX = xToAdsr(x);
 				float adsrY = yToAdsr(y);
 				float prevX = i >= 2 ? adsrPoints[i - 1][0] : 0;
				float nextX = i <= 3 ? adsrPoints[i + 1][0] : 1;
 				if (i == 0) adsrX = 0;
 				// ADSR samples cannot go past the next ADSR sample or before the previous sample 
 				adsrPoints[i][0] = adsrX > prevX ? (adsrX < nextX ? adsrX : nextX) : prevX;
				if (i != 3) // can only change the x coord of the cutoff point
					adsrPoints[i][1] = adsrY > 0 ? (adsrY < 1 ? adsrY : 1) : 0;
 				// points 2 and 3 must have the same y value, since these are the two ends
 				// of the sustain level, which must be linear.
 				// ie.  adjusting either 2 or 3 will adjust both points' y values
 				if (i == 2)
 					adsrPoints[3][1] = adsrPoints[2][1];
 				initAdsrVB();
 				setAdsrPoint(trackNum, i, adsrPoints[i][0], adsrPoints[i][1]);
 				return true;
 			}
 		}
 		return false;
 	}
 	
 	private boolean moveLoopMarker(int id, float x) {
 		if (beginLoopMarkerTouched == id) {
 			float newLoopBegin = xToSample(x);
 			sampleLoopBegin = newLoopBegin < 0 ? 0
 					: (newLoopBegin >= sampleLoopEnd - MIN_LOOP_WINDOW ? sampleLoopEnd - MIN_LOOP_WINDOW
 							: newLoopBegin);
 		} else if (endLoopMarkerTouched == id) {
 			float newLoopEnd = xToSample(x);
 			sampleLoopEnd = newLoopEnd >= numSamples ? numSamples - 1
 					: (newLoopEnd <= sampleLoopBegin + MIN_LOOP_WINDOW ? sampleLoopBegin + MIN_LOOP_WINDOW
 							: newLoopEnd);
 		} else {
 			return false;
 		}
 		if (sampleLoopBegin < sampleOffset) {
 			sampleOffset = sampleLoopBegin;
 			if (sampleLoopEnd > sampleOffset + sampleWidth)
 				sampleWidth = sampleLoopEnd - sampleLoopBegin;
 		} else if (sampleLoopEnd > sampleOffset + sampleWidth) {
 			sampleWidth = sampleLoopEnd - sampleOffset;
 			if (sampleWidth + sampleOffset > numSamples)
 				sampleWidth = numSamples - sampleOffset;
 		}
 		// update UI
 		initLoopMarkerVB();
 		initAdsrVB();
 		// update sample playback.
 		playbackManager.setLoopWindow(trackNum, (int)sampleLoopBegin, (int)sampleLoopEnd);
 		return true;
 	}
 
 	private boolean setZoomAnchor(float x) {
 		if (scrollAnchorSample != -1) {
 			// one pointer is already scrolling.
 			zoomLeftAnchorSample = Math.min(scrollAnchorSample, xToSample(x));
 			zoomRightAnchorSample = Math.max(scrollAnchorSample, xToSample(x));
 			scrollAnchorSample = -1; // not scrolling anymore
 			return true;
 		}
 		return false;
 	}
 	
 	private void setScrollAnchor(float x) {
 		scrollAnchorSample = xToSample(x);
 	}
 	
 	private boolean scroll(float scrollX) {
 		if (scrollAnchorSample == -1)
 			return false; // not scrolling
 		updateSampleOffset(scrollX);
 		return true;
 	}
 	
 	private boolean zoom(float x1, float x2) {
 		if (zoomLeftAnchorSample == -1 || zoomRightAnchorSample == -1)
 			return false; // not zooming
 		float leftX = Math.min(x1, x2);
 		float rightX = Math.max(x1, x2);
 		updateZoom(leftX, rightX);
 		return true;
 	}
 	
 	private void clipLoopToWindow() {
 		if (sampleLoopBegin < sampleOffset)
 			sampleLoopBegin = sampleOffset;
 		if (sampleLoopEnd > sampleOffset + sampleWidth)
 			sampleLoopEnd = sampleOffset + sampleWidth;
 	}
 	
 	private void handleWaveActionDown(int id, float x, float y) {
 		if (!selectAdsrPoint(id, x, y) && !selectLoopMarker(id, x)) {
 			// no ADSR points are close enough, and
 			// loop marker not close enough to select. start scroll
 			// (we know it's the first pointer down, so we're not zooming)
 			setScrollAnchor(x);
 		}			
 	}
 
 	private void handleWaveActionPointerDown(int id, float x, float y) {
 		if (!selectAdsrPoint(id, x, y) && !selectLoopMarker(id, x) && !setZoomAnchor(x)) {
 			// loop marker not close enough to select, and first pointer down.
 			// start scrolling
 			setScrollAnchor(x);
 		}
 	}
 
 	private void handleWaveActionPointerUp(MotionEvent e, int id) {
 		deselectLoopMarker(id);
 		deselectAdsrPoint(id);
 		scrollAnchorSample = -1;
 		// stop zooming
 		if (zoomLeftAnchorSample != -1 && zoomRightAnchorSample != -1) {
 			int otherId = (id + 1)%2;
 			if (otherId != previewPointerId)
 				setScrollAnchor(e.getX(otherId));
 		}
 		zoomLeftAnchorSample = zoomRightAnchorSample = -1;
 	}
 	
 	public void handlePreviewActionDown(int id) {
 		playbackManager.playTrack(trackNum);
 		previewPointerId = id;		
 	}
 	
 	public void handlePreviewActionPointerDown(int id) {
 		handlePreviewActionDown(id);
 	}
 
 	@Override
 	protected void handleActionDown(int id, float x, float y) {
 		if (x > previewButtonWidth)
 			handleWaveActionDown(id, x, y);
 		else
 			handlePreviewActionDown(id);
 	}
 	
 	@Override
 	protected void handleActionPointerDown(MotionEvent e, int id, float x, float y) {
 		if (x >previewButtonWidth)
 			handleWaveActionPointerDown(id, x, y);
 		else
 			handlePreviewActionPointerDown(id);
 	}
 
 	@Override
 	protected void handleActionMove(MotionEvent e) {
 		if (zoomLeftAnchorSample != -1 && zoomRightAnchorSample != -1
 				&& previewPointerId == -1)
 			zoom(e.getX(0), e.getX(1));
 		for (int i = 0; i < e.getPointerCount(); i++) {
 			int id = e.getPointerId(i);
 			if (id == previewPointerId) {
 				if (e.getX(i) > previewButtonWidth) {
 					// if a click goes down on the preview button, 
 					// then moves out of it into wave, stop previewing
 					previewPointerId = -1;
 					playbackManager.stopTrack(trackNum);
 				}
 			} else if (!moveAdsrPoint(id, e.getX(i), e.getY(i)) && !moveLoopMarker(id, e.getX(i)))
 				scroll(e.getX(i));
 		}
 		updateLoopMarkers();
 	}
 
 	@Override
 	protected void handleActionPointerUp(MotionEvent e, int id, float x, float y) {
 		if (x > previewButtonWidth)
 			handleWaveActionPointerUp(e, id);
 		else
 			playbackManager.stopTrack(trackNum);		
 	}
 
 	@Override
 	protected void handleActionUp(int id, float x, float y) {
 		beginLoopMarkerTouched = endLoopMarkerTouched = -1;			
 		playbackManager.stopTrack(trackNum);			
 		scrollAnchorSample = zoomLeftAnchorSample = zoomRightAnchorSample = -1;
 		clearAdsrSelected();
 	}
 	
 	// set the native adsr point.  x and y range from 0 to 1
 	public native void setAdsrPoint(int trackNum, int adsrPointNum, float x, float y);
 }
