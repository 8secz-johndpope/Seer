 package com.github.dakusui.symfonion.song;
 
 import static com.github.dakusui.symfonion.core.SymfonionIllegalFormatException.NOTELENGTH_EXAMPLE;
 import static com.github.dakusui.symfonion.core.SymfonionTypeMismatchException.PRIMITIVE;
 
 import java.util.LinkedList;
 import java.util.List;
 import java.util.regex.Matcher;
 
 import javax.sound.midi.InvalidMidiDataException;
 import javax.sound.midi.MidiEvent;
 import javax.sound.midi.Track;
 
 import com.github.dakusui.symfonion.MidiCompiler;
 import com.github.dakusui.symfonion.MidiCompiler.CompilerContext;
 import com.github.dakusui.symfonion.core.ExceptionThrower;
 import com.github.dakusui.symfonion.core.Fraction;
 import com.github.dakusui.symfonion.core.JsonUtil;
 import com.github.dakusui.symfonion.core.SymfonionException;
 import com.github.dakusui.symfonion.core.Util;
 import com.github.dakusui.symfonion.song.Pattern.Parameters;
 import com.google.gson.JsonArray;
 import com.google.gson.JsonElement;
 import com.google.gson.JsonObject;
 
 
 
 public class Stroke {
 	private static final int UNDEFINED_NUM = -1;
 	private Fraction length;
 	static java.util.regex.Pattern notesPattern = java.util.regex.Pattern.compile("([A-Zac-z])([#b]*)([><]*)([\\+\\-]*)?");
 	List<NoteSet> notes = new LinkedList<NoteSet>();
 	private double gate;
 	private NoteMap noteMap;
 	private int[] volume = null;
 	private int[] pan = null;
 	private int[] reverb = null;
 	private int[] chorus;
 	private int[] pitch = null;
 	private int[] modulation = null;
 	private int pgno = UNDEFINED_NUM;
 	private String bkno = null;
 	private int tempo = UNDEFINED_NUM;
 	private JsonArray sysex;
 	private int[] aftertouch;
 	private JsonElement strokeJson;
 	public Stroke(
 			JsonElement cur,
 			Parameters params,
 			NoteMap noteMap
 			) throws SymfonionException {
 		String notes = null;
 		Fraction len = params.length();
 		double gate = params.gate();
 		this.strokeJson = cur;
 		if (cur.isJsonPrimitive()) {
 			notes = cur.getAsString();
 		} else if (cur.isJsonArray()) {
 			JsonArray arr = cur.getAsJsonArray();
 			int elems = arr.size();
 			if (elems > 0) {
 				notes = arr.get(0).getAsString();
 				if (elems > 1) {
 					len = Util.parseNoteLength(arr.get(1).getAsString());
 					if (len == null) {
 						ExceptionThrower.throwIllegalFormatException(arr.get(1), NOTELENGTH_EXAMPLE);
 					}
 				}
 			}
 		} else if (cur.isJsonObject()) {
 			JsonObject obj = cur.getAsJsonObject();
 			notes = JsonUtil.asString(obj, Keyword.$notes);
 			if (JsonUtil.hasPath(obj, Keyword.$length)) {
 				JsonElement lenJSON = JsonUtil.asJson(obj, Keyword.$length);
 				if (lenJSON.isJsonPrimitive()) {
 					len = Util.parseNoteLength(lenJSON.getAsString());
 					if (len == null) {
 						ExceptionThrower.throwIllegalFormatException(lenJSON, NOTELENGTH_EXAMPLE);
 					}
 				} else {
 					ExceptionThrower.throwTypeMismatchException(lenJSON, PRIMITIVE);
 				}
 			}
 			if (JsonUtil.hasPath(obj, Keyword.$gate)) {
 				gate = JsonUtil.asDouble(obj, Keyword.$gate);
 			}
 			this.tempo      = JsonUtil.hasPath(obj, Keyword.$tempo) ? JsonUtil.asInt(obj, Keyword.$tempo) : UNDEFINED_NUM;
 			this.pgno       = JsonUtil.hasPath(obj, Keyword.$program) ? JsonUtil.asInt(obj, Keyword.$program) : UNDEFINED_NUM;
 			if (JsonUtil.hasPath(obj, Keyword.$bank)) {
 				this.bkno       = JsonUtil.asString(obj, Keyword.$bank);
 				// Checks if this.bkno can be parsed as a double value.
 				Double.parseDouble(this.bkno);
 			}
 			this.volume     = getIntArray(obj, Keyword.$volume);
 			this.pan        = getIntArray(obj, Keyword.$pan);
 			this.reverb     = getIntArray(obj, Keyword.$reverb);
 			this.chorus     = getIntArray(obj, Keyword.$chorus);
 			this.pitch      = getIntArray(obj, Keyword.$pitch);
 			this.modulation = getIntArray(obj, Keyword.$modulation);
 			this.aftertouch = getIntArray(obj, Keyword.$aftertouch);
 			this.sysex      = JsonUtil.asJsonArray(obj, Keyword.$sysex);
 		} else {
 			// unsupported
 		}
 		this.noteMap = noteMap;
 		this.gate = gate;
 		Fraction strokeLen = Fraction.zero;
 		if (notes != null) {
 			for (String nn : notes.split(";")) {
 				NoteSet ns = new NoteSet();
 				Fraction nsLen = null;
 				String l;
				if ((l = parseNotes(nn, ns)) != null) {
 					nsLen = Util.parseNoteLength(l);
 				} else {
 					nsLen = len;
 				}
 				ns.setLength(nsLen);
				this.notes.add(ns);
 				strokeLen = Fraction.add(strokeLen, nsLen);
 			}
 		}
 		if (Fraction.zero.equals(strokeLen)) {
 			strokeLen = len;
 		}
 		this.length = strokeLen;
 	} 
 	
 	private int[] getIntArray(JsonObject cur, Keyword kw) throws SymfonionException {
 		int[] ret = null;
 		if (!JsonUtil.hasPath(cur, kw)) {
 			return null;
 		}
 		JsonElement json = JsonUtil.asJson(cur, kw);
 		if (json.isJsonArray()) {
 			JsonArray arr = json.getAsJsonArray();
 			Integer[] tmp = new Integer[arr.size()];
 			for (int i = 0; i < tmp.length; i++) {
 				if (arr.get(i) == null || arr.get(i).isJsonNull()) {
 					tmp[i] = null;
 				} else {
 					tmp[i] = arr.get(i).getAsInt();
 				}
 			}
 			ret = new int[arr.size()];
 			int start = 0;
 			int end = 0;
 			for (int i =0; i < tmp.length; i++) {
 				if (tmp[i] != null) {
 					start = ret[i] = tmp[i].intValue();
 				} else {
 					int j = i + 1;
 					while (j < tmp.length) {
 						if (tmp[j] != null) {
 							end = tmp[j].intValue();
 							ret[j] = end;
 							break;
 						}
 						j++;
 					}
 					int step = (end - start) / (j-i);
 					int curval = start;
 					for (int k = i; k < j; k++) {
 						curval += step;
 						ret[k] = curval;
 					}
 					i = j;
 				}
 			}
 		} else {
 			ret = new int[1];
 			ret[0] = JsonUtil.asInt(cur, kw);
 		}
 		return ret;
 	}
 
 	public Fraction length() {
 		return length;
 	}
 	
 	public double gate() {
 		return this.gate;
 	}
 
 	public List<NoteSet> notes() {
 		return this.notes;
 	}
 	
 	/*
 	 * Returns the 'length' portion of the string <code>s</code>.
 	 */
 	private String parseNotes(String s, List<Note> notes) throws SymfonionException {
 		Matcher m = notesPattern.matcher(s);
 		int i;
 		for (i = 0; m.find(i); i = m.end()) {
 			if (i != m.start()) {
 				throw new SymfonionException("Error:" + s.substring(0, i) + "[" + s.substring(i, m.start()) + "]" + s.substring(m.start()));
 			}
 			int n_ = this.noteMap.note(m.group(1), this.strokeJson); 
 			if (n_ >= 0) {
 				int n = 
 						n_ + 
 						Util.count('#', m.group(2)) - Util.count('b', m.group(2)) +
 						Util.count('>', m.group(3)) * 12 - Util.count('<', m.group(3)) *12;
 				int a = Util.count('+', m.group(4)) - Util.count('-', m.group(4));
 				Note nn = new Note(n, a);
 				notes.add(nn);
 			}
 		}
 		Matcher n = Util.lengthPattern.matcher(s);
 		String ret = null;
 		if (n.find(i)) {
 			ret = s.substring(n.start(), n.end());
 			i = n.end();
 		}
 		if (i != s.length()) {
 			String msg = s.substring(0, i) + "`" + s.substring(i) + "' isn't a valid note expression. Notes must be like 'C', 'CEG8.', and so on.";
 			ExceptionThrower.throwIllegalFormatException(this.strokeJson, msg);
 		}
 		return ret;
 	}
 	
 	static interface EventCreator {
 		void createEvent(int v, long pos) throws InvalidMidiDataException;
 	};
 	
 	private void renderValues(int[] values, long pos, long strokeLen, MidiCompiler compiler, EventCreator creator) throws InvalidMidiDataException {
 		if (values == null) {
 			return;
 		}
 		long step = strokeLen / values.length;
 		for (int i = 0; i < values.length; i++) {
 			creator.createEvent(values[i], pos + step * i);
 			compiler.controlEventProcessed();
 		}
 	}
 	
 	/**
 	 * 
 	 * @param compiler
 	 * @param context
 	 * @return
 	 * @throws InvalidMidiDataException
 	 */
 	public void compile(final MidiCompiler compiler, CompilerContext context) throws InvalidMidiDataException {
 		final Track track = context.getTrack();
 		final int ch = context.getChannel();
 		long absolutePosition = context.convertRelativePositionInStrokeToAbsolutePosition(Fraction.zero);
 		long strokeLen = context.getStrokeLengthInTicks(this);
 		if (tempo != UNDEFINED_NUM) {
 			track.add(compiler.createTempoEvent(this.tempo, absolutePosition));
 			compiler.controlEventProcessed();
 		}
 		if (bkno != null) {
 			int msb = Integer.parseInt(bkno.substring(0, this.bkno.indexOf('.')));
 			track.add(compiler.createBankSelectMSBEvent(ch, msb, absolutePosition));
 			if (this.bkno.indexOf('.') != -1) {
 				int lsb = Integer.parseInt(bkno.substring(this.bkno.indexOf('.') + 1));
 				track.add(compiler.createBankSelectLSBEvent(ch, lsb, absolutePosition));
 			}
 			compiler.controlEventProcessed();
 		}
 		if (pgno != UNDEFINED_NUM) {
 			track.add(compiler.createProgramChangeEvent(ch, this.pgno, absolutePosition));
 			compiler.controlEventProcessed();
 		}
 		if (sysex != null) {
 			MidiEvent ev = compiler.createSysexEvent(ch, sysex, absolutePosition);
 			if (ev != null) {
 				track.add(ev);
 				compiler.sysexEventProcessed();
 			}
 		}
 		renderValues(volume, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createVolumeChangeEvent(ch, v, pos));
 			}
 		});
 		renderValues(pan, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createPanChangeEvent(ch, v, pos));
 			}
 		});
 		renderValues(reverb, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createReverbEvent(ch, v, pos));
 			}
 		});
 		renderValues(chorus, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createChorusEvent(ch, v, pos));
 			}
 		});
 		renderValues(pitch, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createPitchBendEvent(ch, v, pos));
 			}
 		});
 		renderValues(modulation, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createModulationEvent(ch, v, pos));
 			}
 		});
 		renderValues(aftertouch, absolutePosition, strokeLen, compiler, new EventCreator() {
 			@Override public void createEvent(int v, long pos) throws InvalidMidiDataException {
 				track.add(compiler.createAfterTouchChangeEvent(ch, v, pos));
 			}
 		});
 		int transpose = context.getParams().transpose();
 		int arpegiodelay = context.getParams().arpegio();
 		int delta = 0;
 		Fraction relPosInStroke = Fraction.zero;
 		for (NoteSet noteSet : this.notes()) {
 			for (Note note : noteSet) {
 				absolutePosition = context.convertRelativePositionInStrokeToAbsolutePosition(relPosInStroke);
 				int key = note.key() + transpose;
 				int velocity = Math.max(
 						0, 
 						Math.min(
 								127, 
 								context.getParams().velocitybase() + 
 								note.accent() * context.getParams().velocitydelta() + 
 								context.getGrooveAccent(relPosInStroke)
 								)
 						);
 				track.add(compiler.createNoteOnEvent(ch, key, velocity, absolutePosition + delta));
 				track.add(compiler.createNoteOffEvent(ch, key, (long)(absolutePosition + delta + strokeLen * this.gate())));
 				compiler.noteProcessed();
 				delta += arpegiodelay;
 			}
 			relPosInStroke = Fraction.add(relPosInStroke, noteSet.getLength());
 		}
 	}
 }
