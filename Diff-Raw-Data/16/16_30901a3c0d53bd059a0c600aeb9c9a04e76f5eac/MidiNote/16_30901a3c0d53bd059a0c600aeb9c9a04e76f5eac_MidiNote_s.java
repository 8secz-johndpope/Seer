 package com.kh.beatbot.midi;
 
 import com.kh.beatbot.global.GlobalVars;
 import com.kh.beatbot.midi.event.MidiEvent;
 import com.kh.beatbot.midi.event.NoteOff;
 import com.kh.beatbot.midi.event.NoteOn;
 import com.kh.beatbot.view.helper.LevelsViewHelper;
 
 public class MidiNote {
 	NoteOn noteOn;
 	NoteOff noteOff;
 
 	public MidiNote(NoteOn noteOn, NoteOff noteOff) {
 		this.noteOn = noteOn;
 		this.noteOff = noteOff;
 	}
 	
 	public MidiNote getCopy() {
 		NoteOn noteOnCopy = new NoteOn(noteOn.getTick(), 0, noteOn.getNoteValue(), noteOn.getVelocity(), noteOn.getPan(), noteOn.getPitch());
 		NoteOff noteOffCopy = new NoteOff(noteOff.getTick(), 0, noteOff.getNoteValue(), noteOff.getVelocity(), noteOff.getPan(), noteOn.getPitch());
 		return new MidiNote(noteOnCopy, noteOffCopy);
 	}
 	
 	public MidiEvent getOnEvent() {
 		return noteOn;
 	}
 	
 	public MidiEvent getOffEvent() {
 		return noteOff;
 	}
 	
 	public long getOnTick() {
 		return noteOn.getTick();
 	}
 	
 	public long getOffTick() {
 		return noteOff.getTick();
 	}
 
 	public int getNoteValue() {
 		return noteOn.getNoteValue();
 	}
 	
 	public float getVelocity() {
 		return noteOn.getVelocity();
 	}
 	
 	public float getPan() {
 		return noteOn.getPan();
 	}
 	
 	public float getPitch() {
 		return noteOn.getPitch();
 	}
 	
 	public void setVelocity(float velocity) {
 		velocity = clipLevel(velocity);
 		noteOn.setVelocity(velocity);
 		noteOff.setVelocity(velocity);
 	}
 	
 	public void setPan(float pan) {
 		pan = clipLevel(pan);
 		noteOn.setPan(pan);
 		noteOff.setPan(pan);
 	}
 	
 	public void setPitch(float pitch) {
 		pitch = clipLevel(pitch);
 		noteOn.setPitch(pitch);
 		noteOff.setPitch(pitch);
 	}
 	
 	// clip the level to be within the min/max allowed
 	// (0-1)
 	private float clipLevel(float level) {
 		if (level < 0)
 			return 0;
 		if (level > 1)
 			return 1;
 		return level;
 	}
 	
 	public void setOnTick(long onTick) {
 		if (onTick >= 0)
 			noteOn.setTick(onTick);
 		else
 			noteOn.setTick(0);
 	}
 	
 	public void setOffTick(long offTick) {
 		if (offTick > getOnTick())
 			this.noteOff.setTick(offTick);
 	}
 	
 	public void setNote(int note) {
 		if (note < 0)
 			return;
 		this.noteOn.setNoteValue(note);
 		this.noteOff.setNoteValue(note);
 	}
 	
 	public long getNoteLength() {
 		return noteOff.getTick() - noteOn.getTick();
 	}
 	
 	public float getLevel(LevelsViewHelper.LevelMode levelMode) {
 		if (levelMode == LevelsViewHelper.LevelMode.VOLUME)
 			return noteOn.getVelocity();
 		else if (levelMode == LevelsViewHelper.LevelMode.PAN)
 			return noteOn.getPan();
 		else if (levelMode == LevelsViewHelper.LevelMode.PITCH)
 			return noteOn.getPitch();
 		
 		return 0;
 	}
 	
 	public void setLevel(LevelsViewHelper.LevelMode levelMode, float level) {
 		if (levelMode == LevelsViewHelper.LevelMode.VOLUME) {
			setVelocity(level);
			setVolume(getNoteValue(), getOnTick(), level);
 		}
 		else if (levelMode == LevelsViewHelper.LevelMode.PAN) {
			setPan(level);
			setPan(getNoteValue(), getOnTick(), level);
 		}
 		else if (levelMode == LevelsViewHelper.LevelMode.PITCH) {
			setPitch(level);
			setPitch(getNoteValue(), getOnTick(), level);
 		}
 	}
 	
 	public native void setVolume(int track, long onTick, float volume);
 	public native void setPan(int track, long onTick, float pan);
 	public native void setPitch(int track, long onTick, float pitch);	
 }
