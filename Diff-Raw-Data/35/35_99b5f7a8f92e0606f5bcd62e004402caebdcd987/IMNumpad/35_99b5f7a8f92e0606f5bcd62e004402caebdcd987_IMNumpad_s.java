 package cz.romario.opensudoku.gui.inputmethod;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import android.content.Context;
 import android.os.Bundle;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 import android.widget.ImageButton;
 import cz.romario.opensudoku.R;
 import cz.romario.opensudoku.game.SudokuCell;
 
 public class IMNumpad extends InputMethod {
 
 	private static final int MODE_EDIT_VALUE = 0;
 	private static final int MODE_EDIT_NOTE = 1;
 	
 	private SudokuCell mSelectedCell;
 	private ImageButton mSwitchNumNoteButton;
 
 	private int mEditMode = MODE_EDIT_VALUE;
 	
 	private Map<Integer,Button> mNumberButtons;
 	
 	@Override
 	protected View createControlPanel() {
 		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 		View controlPanel = inflater.inflate(R.layout.im_numpad, null);
 		
 		mNumberButtons = new HashMap<Integer, Button>(); 
 		mNumberButtons.put(1, (Button)controlPanel.findViewById(R.id.button_1));
 		mNumberButtons.put(2, (Button)controlPanel.findViewById(R.id.button_2));
 		mNumberButtons.put(3, (Button)controlPanel.findViewById(R.id.button_3));
 		mNumberButtons.put(4, (Button)controlPanel.findViewById(R.id.button_4));
 		mNumberButtons.put(5, (Button)controlPanel.findViewById(R.id.button_5));
 		mNumberButtons.put(6, (Button)controlPanel.findViewById(R.id.button_6));
 		mNumberButtons.put(7, (Button)controlPanel.findViewById(R.id.button_7));
 		mNumberButtons.put(8, (Button)controlPanel.findViewById(R.id.button_8));
 		mNumberButtons.put(9, (Button)controlPanel.findViewById(R.id.button_9));
 		mNumberButtons.put(0, (Button)controlPanel.findViewById(R.id.button_clear));
 		
 		for (Integer num : mNumberButtons.keySet()) {
 			Button b = mNumberButtons.get(num);
 			b.setTag(num);
 			b.setOnClickListener(mNumberButtonClick);
 		}
 		
 		mSwitchNumNoteButton = (ImageButton)controlPanel.findViewById(R.id.switch_num_note);
 		mSwitchNumNoteButton.setOnClickListener(new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
 				mEditMode = mEditMode == MODE_EDIT_VALUE ? MODE_EDIT_NOTE : MODE_EDIT_VALUE;
 				update();
 			}
 			
 		});
 		
 		return controlPanel;
 		
 	}
 
 	@Override
 	public int getNameResID() {
 		return R.string.numpad;
 	}
 
 	@Override
 	public int getHelpResID() {
 		return R.string.im_numpad_hint;
 	}
 	
 	@Override
 	public String getAbbrName() {
 		return mContext.getString(R.string.numpad_abbr);
 	}
 	
 	@Override
 	protected void onActivated() {
 		update();
 		
 		mSelectedCell = mBoard.getSelectedCell();
 	}
 	
 	@Override
 	protected void onCellSelected(SudokuCell cell) {
 		mSelectedCell = cell;
 	}
 	
 	private OnClickListener mNumberButtonClick = new OnClickListener() {
 
 		@Override
 		public void onClick(View v) {
 			int selNumber = (Integer)v.getTag();
 			SudokuCell selCell = mSelectedCell;
 			
			switch (mEditMode) {
			case MODE_EDIT_NOTE:
				if (selNumber == 0) {
					mGame.setCellNote(selCell, null);
					mBoard.postInvalidate();
				} else if (selNumber > 0 && selNumber <= 9) {
					mGame.setCellNote(selCell, selCell.toggleNoteNumber(selNumber));
					mBoard.postInvalidate();
 				}
				break;
			case MODE_EDIT_VALUE:
				if (selNumber >= 0 && selNumber <= 9) {
					mGame.setCellValue(selCell, selNumber);
					mBoard.moveCellSelectionRight();
					mBoard.postInvalidate();
				}
				break;
 			}
 		}
 		
 	};
 	
 	private void update() {
 		switch (mEditMode) {
 		case MODE_EDIT_NOTE:
 			mSwitchNumNoteButton.setImageResource(R.drawable.pencil);
 			break;
 		case MODE_EDIT_VALUE:
 			mSwitchNumNoteButton.setImageResource(R.drawable.pencil_disabled);
 			break;
 		}
 	}
 	
 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		
 		outState.putInt(getInputMethodName() + ".edit_mode", mEditMode);
 	}
 	
 	@Override
 	protected void onRestoreInstanceState(Bundle savedInstanceState) {
 		// TODO Auto-generated method stub
 		super.onRestoreInstanceState(savedInstanceState);
 		
 		mEditMode = savedInstanceState.getInt(getInputMethodName() + ".edit_mode");
 		if (isControlPanelCreated()) {
 			update();
 		}
 	}
 }
