 package com.github.triplesolitaire;
 
 import java.util.List;
 
 import android.content.ClipData;
 import android.content.Context;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.DragEvent;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.View.OnDragListener;
 import android.widget.RelativeLayout;
 
 public class Lane extends RelativeLayout implements OnDragListener
 {
 	private class OnStartDragListener implements OnTouchListener
 	{
 		private final int cascadeIndex;
 
 		public OnStartDragListener(final int cascadeIndex)
 		{
 			this.cascadeIndex = cascadeIndex;
 		}
 
 		@Override
 		public boolean onTouch(final View v, final MotionEvent event)
 		{
 			if (event.getAction() != MotionEvent.ACTION_DOWN)
 				return false;
 			final String cascadeData = gameState.buildCascadeString(laneId - 1,
 					cascadeSize - cascadeIndex);
 			final ClipData dragData = ClipData.newPlainText(
 					(cascadeIndex + 1 != cascadeSize ? "MULTI" : "")
 							+ cascadeData, cascadeData);
 			currentDragIndex = cascadeIndex;
 			cascadeSizeOnStartDrag = cascadeSize;
 			v.startDrag(dragData, new View.DragShadowBuilder(v), laneId, 0);
 			return true;
 		}
 	}
 
 	/**
 	 * Logging tag
 	 */
 	private static final String TAG = "TripleSolitaireActivity";
 	private int cascadeSize;
 	private int cascadeSizeOnStartDrag = 0;
 	private int currentDragIndex = -1;
 	private GameState gameState;
 	private int laneId;
 	private OnClickListener onCardFlipListener;
 	private int stackSize;
 
 	public Lane(final Context context, final AttributeSet attrs)
 	{
 		super(context, attrs);
 		final Card laneBase = new Card(context, R.drawable.lane);
 		laneBase.setId(0);
 		laneBase.setOnDragListener(this);
 		addView(laneBase);
 	}
 
 	public void addCascade(final List<String> cascadeToAdd)
 	{
 		final int card_vert_overlap_dim = getResources().getDimensionPixelSize(
 				R.dimen.card_vert_overlap_dim);
 		// Create the cascade
 		for (int h = 0; h < cascadeToAdd.size(); h++)
 		{
 			final int cascadeId = h + cascadeSize + stackSize + 1;
 			final Card cascadeCard = new Card(getContext(), getResources()
 					.getIdentifier(cascadeToAdd.get(h), "drawable",
 							getContext().getPackageName()));
 			cascadeCard.setId(cascadeId);
 			final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
 					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
 					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
 			lp.addRule(RelativeLayout.ALIGN_TOP, cascadeId - 1);
 			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
 			if (stackSize + cascadeSize + h != 0)
 				lp.setMargins(0, card_vert_overlap_dim, 0, 0);
 			cascadeCard.setOnTouchListener(new OnStartDragListener(h
 					+ cascadeSize));
 			addView(cascadeCard, lp);
 		}
 		if (cascadeSize == 0 && !cascadeToAdd.isEmpty())
 			if (stackSize > 0)
 			{
 				// Remove the onCardFlipListener from the top card on the stack
 				// if there is a cascade now
 				final Card topStack = (Card) findViewById(stackSize);
 				topStack.setOnClickListener(null);
 			}
 			else
 			{
 				// Remove the onDragListener from the base of the stack if there
 				// is a cascade now
 				final Card laneBase = (Card) findViewById(0);
 				laneBase.setOnDragListener(null);
 			}
 		if (cascadeSize > 0)
 		{
 			final Card oldTopCascade = (Card) findViewById(stackSize
 					+ cascadeSize);
 			oldTopCascade.setOnDragListener(null);
 		}
 		if (!cascadeToAdd.isEmpty())
 		{
 			final Card newTopCascade = (Card) findViewById(getChildCount() - 1);
 			newTopCascade.setOnDragListener(this);
 		}
 		cascadeSize += cascadeToAdd.size();
 	}
 
 	public void decrementCascadeSize(final int removeCount)
 	{
 		for (int h = 0; h < removeCount; h++)
 		{
 			removeViewAt(getChildCount() - 1);
 			cascadeSize -= 1;
 		}
 		if (stackSize + cascadeSize == 0)
 		{
 			final Card laneBase = (Card) findViewById(0);
 			laneBase.setOnDragListener(this);
 		}
 		else if (cascadeSize == 0)
 		{
 			final Card topStack = (Card) findViewById(stackSize);
 			topStack.setOnClickListener(onCardFlipListener);
 		}
 		else
 		{
 			final Card topCascade = (Card) findViewById(stackSize + cascadeSize);
 			topCascade.setOnDragListener(this);
 		}
 	}
 
 	public void flipOverTopStack(final String card)
 	{
 		final Card toFlip = (Card) findViewById(stackSize);
 		toFlip.setBackgroundResource(getResources().getIdentifier(card,
 				"drawable", getContext().getPackageName()));
 		toFlip.invalidate();
 		toFlip.setOnClickListener(null);
 		toFlip.setOnDragListener(this);
 		toFlip.setOnTouchListener(new OnStartDragListener(0));
 		stackSize -= 1;
 		cascadeSize += 1;
 	}
 
 	public Card getTopCascadeCard()
 	{
 		return (Card) findViewById(getChildCount() - 1);
 	}
 
 	@Override
 	public boolean onDrag(final View v, final DragEvent event)
 	{
 		final boolean isMyCascade = laneId == (Integer) event.getLocalState();
 		if (event.getAction() == DragEvent.ACTION_DRAG_STARTED)
 		{
 			String card = event.getClipDescription().getLabel().toString();
 			if (isMyCascade)
 			{
 				Log.d(TAG, "Drag " + laneId + ": Started of " + card);
 				return false;
 			}
 			// Take off MULTI prefix - we accept all cascades based on the top
 			// card alone
 			if (card.startsWith("MULTI"))
 				card = card.substring(5, card.indexOf(';'));
 			return cascadeSize == 0 ? gameState
 					.acceptLaneDrop(laneId - 1, card) : gameState
 					.acceptCascadeDrop(laneId - 1, card);
 		}
 		else if (event.getAction() == DragEvent.ACTION_DROP)
 		{
 			final String card = event.getClipData().getItemAt(0).getText()
 					.toString();
 			final int from = (Integer) event.getLocalState();
 			if (from == 0)
 				gameState.dropFromWasteToCascade(laneId - 1);
 			else if (from < 0)
 				gameState
 						.dropFromFoundationToCascade(laneId - 1, -1 * from - 1);
 			else
 				gameState.dropFromCascadeToCascade(laneId - 1, from - 1, card);
 		}
 		else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED
 				&& isMyCascade)
 		{
 			Log.d(TAG, "Drag " + laneId + ": Ended with "
 					+ (event.getResult() ? "success" : "failure"));
 			if (!event.getResult() && cascadeSizeOnStartDrag == cascadeSize
 					&& currentDragIndex + 1 == cascadeSize)
				postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						gameState.attemptAutoMoveFromCascadeToFoundation(laneId - 1);
					}
				}, 10);
 			currentDragIndex = -1;
 		}
 		return true;
 	}
 
 	public void setGameState(final GameState gameState)
 	{
 		this.gameState = gameState;
 	}
 
 	public void setLaneId(final int laneId)
 	{
 		this.laneId = laneId;
 	}
 
 	public void setOnCardFlipListener(final OnClickListener onCardFlipListener)
 	{
 		this.onCardFlipListener = onCardFlipListener;
 	}
 
 	public void setStackSize(final int newStackSize)
 	{
 		// Remove the existing views, including the cascade
 		removeViews(1, getChildCount() - 1);
 		cascadeSize = 0;
 		if (stackSize == 0 && newStackSize > 0)
 		{
 			final Card laneBase = (Card) findViewById(0);
 			laneBase.setOnDragListener(null);
 		}
 		stackSize = newStackSize;
 		final int card_vert_overlap_dim = getResources().getDimensionPixelSize(
 				R.dimen.card_vert_overlap_dim);
 		// Create the stack
 		for (int stackId = 1; stackId <= stackSize; stackId++)
 		{
 			final Card stackCard = new Card(getContext(), R.drawable.back);
 			stackCard.setId(stackId);
 			final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
 					android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
 					android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
 			lp.addRule(RelativeLayout.ALIGN_TOP, stackId - 1);
 			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
 			if (stackId != 1)
 				lp.setMargins(0, card_vert_overlap_dim, 0, 0);
 			if (cascadeSize == 0 && stackId == stackSize)
 				stackCard.setOnClickListener(onCardFlipListener);
 			addView(stackCard, stackId, lp);
 		}
 	}
 }
