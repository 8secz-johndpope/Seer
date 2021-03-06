 /****************************************************************************************
  * Copyright (c) 2009 Daniel Svärd <daniel.svard@gmail.com>                             *
  * Copyright (c) 2010 Rick Gruber-Riemer <rick@vanosten.net>                            *
  * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
  *                                                                                      *
  * This program is free software; you can redistribute it and/or modify it under        *
  * the terms of the GNU General Public License as published by the Free Software        *
  * Foundation; either version 3 of the License, or (at your option) any later           *
  * version.                                                                             *
  *                                                                                      *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
  * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
  *                                                                                      *
  * You should have received a copy of the GNU General Public License along with         *
  * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
  ****************************************************************************************/
 
 package com.ichi2.libanki;
 
 import android.content.ContentValues;
 import android.util.Log;
 
 import com.ichi2.anki.AnkiDroidApp;
 import com.mindprod.common11.StringTools;
 import com.samskivert.mustache.Mustache;
 import com.samskivert.mustache.Template;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 public class Models {
 
 	public static final String defaultModel = 
 		"{'sortf': 0, " +
 		"'did': 1, " +
 		"'latexPre': \"" +
 		"\\\\documentclass[12pt]{article} " +
 		"\\\\special{papersize=3in,5in} " +
 		"\\\\usepackage[utf8]{inputenc} " +
 		"\\\\usepackage{amssymb,amsmath} " +
 		"\\\\pagestyle{empty} " +
 		"\\\\setlength{\\\\parindent}{0in} " +
 		"\\\\begin{document} " +
 		"\", " +
 		"'latexPost': \"\\\\end{document}\", " +
 		"'mod': 9, " +
 		"'usn': 9, " +
 		"'vers': [], " +
 		"'type': " + Sched.MODEL_STD + ", " +
 		"'css': \" .card {" +
 		"font-familiy: arial; " +
 		"font-size: 20px; " +
 		"text-align: center; " +
 		"color:black; " +
 		"background-color: white; }\"" +
 		"}";
 
 	private static final String defaultField = 
 		"{'name': \"\", " +
 		"'ord': None, " +
 		"'sticky': False, " +
 		// the following alter editing, and are used as defaults for the template wizard
 		"'rtl': False, " +
 		"'font': \"Arial\", " +
 		"'size': 20, " +
 		// reserved for future use
 		"'media': [] }";
 
 	private static final String defaultTemplate = 
 		"{'name': \"\", " +
 		"'ord': None, " +
 		"'qfmt': \"\", " +
 		"'afmt': \"\", " +
 		"'did': None }";
 	
 //    /** Regex pattern used in removing tags from text before diff */
 //    private static final Pattern sFactPattern = Pattern.compile("%\\([tT]ags\\)s");
 //    private static final Pattern sModelPattern = Pattern.compile("%\\(modelTags\\)s");
 //    private static final Pattern sTemplPattern = Pattern.compile("%\\(cardModel\\)s");
 
 	private Collection mCol;
 	private boolean mChanged;
 	private HashMap<Long, JSONObject> mModels;
 
     // BEGIN SQL table entries
     private int mId;
     private String mName = "";
     private long mCrt = Utils.intNow();
     private long mMod = Utils.intNow();
     private JSONObject mConf;
     private String mCss = "";
     private JSONArray mFields;
     private JSONArray mTemplates;
     // BEGIN SQL table entries
 
 //    private Decks mDeck;
 //    private AnkiDb mDb;
 //
     /** Map for compiled Mustache Templates */
     private HashMap<Long, HashMap<Integer, Template[]>> mCmpldTemplateMap = new HashMap<Long, HashMap<Integer, Template[]>>();
 //
 //    /** Map for convenience and speed which contains FieldNames from current model */
 //    private TreeMap<String, Integer> mFieldMap = new TreeMap<String, Integer>();
 //
 //    /** Map for convenience and speed which contains Templates from current model */
 //    private TreeMap<Integer, JSONObject> mTemplateMap = new TreeMap<Integer, JSONObject>();
 //
 //    /** Map for convenience and speed which contains the CSS code related to a Template */
 //    private HashMap<Integer, String> mCssTemplateMap = new HashMap<Integer, String>();
 //
 //    /**
 //     * The percentage chosen in preferences for font sizing at the time when the css for the CardModels related to this
 //     * Model was calculated in prepareCSSForCardModels.
 //     */
 //    private transient int mDisplayPercentage = 0;
 //    private boolean mNightMode = false;
 
     
     
     
     
     /**
      * Saving/loading registry
      * ***********************************************************************************************
      */
 
     public Models(Collection col) {
     	mCol = col;
     }
 
 
     /**
      * Load registry from JSON.
      */
     public void load(String json) {
     	mChanged = false;
     	mModels = new HashMap<Long, JSONObject>();
         try {
         	JSONObject modelarray = new JSONObject(json);
         	JSONArray ids = modelarray.names();
         	if (ids != null) {
             	for (int i = 0; i < ids.length(); i++) {
             		String id = ids.getString(i);
             		JSONObject o = modelarray.getJSONObject(id);
             		mModels.put(o.getLong("id"), o);
             	}
         	}
  		} catch (JSONException e) {
  			throw new RuntimeException(e);
  		}
     }
 
 
     /**
      * Mark M modified if provided, and schedule registry flush.
      */
     public void save() {
     	save(null, false);
     }
     public void save(JSONObject m) {
     	save(m, false);
     }
     public void save(JSONObject m, boolean templates) {
     	if (m != null && m.has("id")) {
     		try {
 				m.put("mod", Utils.intNow());
 	    		m.put("usn", mCol.usn());
 	    		// TODO: fix empty id problem on _updaterequired (needed for model adding)
 	    		if (m.getLong("id") != 0) {
 		    		_updateRequired(m);	    			
 	    		}
 	    		if (templates) {
 	    			_syncTemplates(m);
 	    		}
 			} catch (JSONException e) {
 				throw new RuntimeException(e);
 			}
     	}
     	mChanged = true;
     }
 
     
     /**
      * Flush the registry if any models were changed.
      */
     public void flush() {
     	if (mChanged) {
     		JSONObject array = new JSONObject();
 			try {
 	    		for (Map.Entry<Long, JSONObject> o : mModels.entrySet()) {
 					array.put(Long.toString(o.getKey()), o.getValue());
 	    		}
 			} catch (JSONException e) {
 				throw new RuntimeException(e);
 			}
     		ContentValues val = new ContentValues();  		
     		val.put("models", array.toString());
     		mCol.getDb().update("col", val);
     		mChanged = false;
     	}
     }
 
     /**
      * Retrieving and creating models
      * ***********************************************************************************************
      */
     
     /**
      * Get current model.
      */
     public JSONObject current() {
     	JSONObject m;
 		try {
			m = get(mCol.getDecks().current().getLong("mid"));
	    	if (m == null) {
 				m = get(mCol.getConf().getLong("curModel"));
 	    	} 
 	    	if (m == null) {
 	    		if (!mModels.isEmpty()) {
 	    			m = mModels.values().iterator().next();
 	    		}
 	    	}
 	    	return m;
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
 
     public void setCurrent(JSONObject m) {
     	try {
 			mCol.getConf().put("curModel", m.get("id"));
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	mCol.setMod();
     }
 
 
     /** get model with ID, or none. */
     public JSONObject get(long id) {
     	if (mModels.containsKey(id)) {
     		return mModels.get(id);
     	} else {
     		return null;
     	}
     }
 
 
     /** get all models */
     public ArrayList<JSONObject> all() {
 		ArrayList<JSONObject> models = new ArrayList<JSONObject>();
 		Iterator<JSONObject> it = mModels.values().iterator();
 		while(it.hasNext()) {
 			models.add(it.next());
 		}
 		return models;
     }
 
 
     public ArrayList<String> allNAmes() {
 		ArrayList<String> names = new ArrayList<String>();
 		Iterator<JSONObject> it = mModels.values().iterator();
 		while(it.hasNext()) {
 			try {
 				names.add(it.next().getString("name"));
 			} catch (JSONException e) {
 				throw new RuntimeException(e);
 			}
 		}
 		return names;
     }
 
 
     /** get model with NAME. */
     public JSONObject byName(String name) {
     	for (JSONObject m : mModels.values()) {
     		try {
 				if (m.getString("name").equalsIgnoreCase(name)) {
 					return m;
 				}
 			} catch (JSONException e) {
 				throw new RuntimeException(e);
 			}
     	}
     	return null;
     }
 
     /** Create a new model, save it in the registry, and return it. */
     public JSONObject newModel(String name) {
     	// caller should call save() after modifying
     	JSONObject m;
 		try {
 			m = new JSONObject(defaultModel);
 	    	m.put("name", name);
 	    	m.put("mod", Utils.intNow());
 	    	m.put("flds", new JSONArray());
 	    	m.put("tmpls", new JSONArray());
 	    	m.put("tags", new JSONArray());
 	    	m.put("id", 0);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	return m;
     }
 
     /** Delete model, and all its cards/notes. */
     public void rem(JSONObject m) {
     	mCol.modSchema();
     	try {
     		long id = m.getLong("id");
 			boolean current = current().getLong("id") == id;
 			// delete notes/cards
 			mCol.remCards(Utils.arrayList2array(mCol.getDb().queryColumn(Long.class, "SELECT id FROM cards WHERE nid IN (SELECT id FROM notes WHERE mid = " + id + ")", 0)));
 			// then the model
 			mModels.remove(id);
 			save();
 			// GUI should ensure last model is not deleted
 			if (current) {
 				setCurrent(mModels.values().iterator().next());
 			}
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
 	public void add(JSONObject m) {
     	_setID(m);
     	update(m);
     	setCurrent(m);    		
     	save(m);
     }
 
     /** Add or update an existing model. Used for syncing and merging. */
     public void update(JSONObject m) {
     	try {
 			mModels.put(m.getLong("id"), m);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	// mark registry changed, but don't bump mod time
     	save();
     }
 
     private void _setID(JSONObject m) {
     	long id = Utils.intNow();
     	while (mModels.containsKey(id)) {
     		id = Utils.intNow();
     	}
     	try {
 			m.put("id", id);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
     public boolean have(long id) {
     	return mModels.containsKey(id);
     }
 
     /**
      * Tools
      * ***********************************************************************************************
      */
 
     /** Note ids for M */
     public ArrayList<Long> nids(JSONObject m) {
     	try {
 			return mCol.getDb().queryColumn(Long.class, "SELECT id FROM notes WHERE mid = " + m.getLong("id"), 0);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
     
     // usecounts
     
     /**
      * Copying
      * ***********************************************************************************************
      */
     
     // copy
 
     /**
      * Fields
      * ***********************************************************************************************
      */
 
     public JSONObject newField(String name) {
     	JSONObject f;
 		try {
 			f = new JSONObject(defaultField);
 	    	f.put("name", name);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	return f;
     }
 
 
 //    /** Mapping of field name --> (ord, field). */
 //    public TreeMap<String, Object[]> fieldMap(JSONObject m) {
 //    	JSONArray ja;
 //		try {
 //			ja = m.getJSONArray("flds");
 //			TreeMap<String, Object[]> map = new TreeMap<String, Object[]>();
 //	    	for (int i = 0; i < ja.length(); i++) {
 //	    		JSONObject f = ja.getJSONObject(i);
 //	    		map.put(f.getString("name"), new Object[]{f.getInt("ord"), f});
 //	    	}
 //	    	return map;
 //		} catch (JSONException e) {
 //			throw new RuntimeException(e);
 //		}
 //    }
 
 
     public Map<String, Integer> fieldMap(JSONObject m) {
     	JSONArray ja;
 		try {
 			ja = m.getJSONArray("flds");
 			TreeMap<Integer, String> map = new TreeMap<Integer, String>();
 	    	for (int i = 0; i < ja.length(); i++) {
 	    		JSONObject f = ja.getJSONObject(i);
 	    		map.put(f.getInt("ord"), f.getString("name"));
 	    	}
 	    	Map<String, Integer> result = new HashMap<String, Integer>();
 	    	for (int i = 0; i < map.size(); i++) {
 	    		result.put(map.get(i), i);
 	    	}
 	    	return result;
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
 
     public ArrayList<String> fieldNames(JSONObject m) {
     	JSONArray ja;
 		try {
 			ja = m.getJSONArray("flds");
 			ArrayList<String> names = new ArrayList<String>();
 	    	for (int i = 0; i < ja.length(); i++) {
 	    		names.add(ja.getJSONObject(i).getString("name"));
 	    	}
 	    	return names;
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	
     }
 
 
     public int sortIdx(JSONObject m) {
     	try {
 			return m.getInt("sortf");
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
 
 //    public int setSortIdx(JSONObject m, int idx) {
 //    	try {
 //    		mCol.modSchema();
 //    		m.put("sortf", idx);
 //    		mCol.updateFieldCache(nids(m));
 //    		save(m);
 //		} catch (JSONException e) {
 //			throw new RuntimeException(e);
 //		}
 //    }
 
 
     public void addField(JSONObject m, JSONObject field) {
     	// only mod schema if model isn't new
     	try {
 			if (m.getLong("id") != 0) {
 				mCol.modSchema();
 			}
 			JSONArray ja = m.getJSONArray("flds");
 			ja.put(field);
 			m.put("flds", ja);
 			_updateFieldOrds(m);
 			save(m);
 			_transformFields(m); //, Method add);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	
     }
 
     //remfield
     //movefield
     //renamefield
 
     public void _updateFieldOrds(JSONObject m) {
     	JSONArray ja;
 		try {
 			ja = m.getJSONArray("flds");
 	    	for (int i = 0; i < ja.length(); i++) {
 	    		JSONObject f = ja.getJSONObject(i);
 	    		f.put("ord", i);
 	    	}
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
     public void _transformFields(JSONObject m) { // Method fn) {
     	// model hasn't been added yet?
     	try {
 			if (m.getLong("id") == 0) {
 				return;
 			}
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	// TODO
     }
 
     /**
      * Templates
      * ***********************************************************************************************
      */
 
     public JSONObject newTemplate(String name) {
     	JSONObject t;
 		try {
 			t = new JSONObject(defaultTemplate);
 	    	t.put("name", name);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	return t;
     }
 
     /** Note: should col.genCards() afterwards. */
     public void addTemplate(JSONObject m, JSONObject template) {
     	try {
 			if (m.getLong("id") != 0) {
 				mCol.modSchema();
 			}
 	    	JSONArray ja = m.getJSONArray("tmpls");
 	    	ja.put(template);
 	    	m.put("tmpls", ja);
 	    	_updateTemplOrds(m);
 	    	save(m);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
     public void remTemplate(JSONObject m, JSONObject template) {
     	// TODO
     }
 
     public void _updateTemplOrds(JSONObject m) {
     	JSONArray ja;
 		try {
 			ja = m.getJSONArray("tmpls");
 	    	for (int i = 0; i < ja.length(); i++) {
 	    		JSONObject f = ja.getJSONObject(i);
 	    		f.put("ord", i);
 	    	}
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
     //movetemplate
 
     private void _syncTemplates(JSONObject m) {
     	ArrayList<Long> rem = mCol.genCards(Utils.arrayList2array(nids(m)));
     }
 
     
 //    public TreeMap<Integer, JSONObject> getTemplates() {
 //    	return mTemplateMap;
 //    }
 //
 //
 //    public JSONObject getTemplate(int ord) {
 //		return mTemplateMap.get(ord);
 //    }
 
 
     // not in libanki
     public Template[] getCmpldTemplate(long modelId, int ord) {
     	if (!mCmpldTemplateMap.containsKey(modelId)) {
     		mCmpldTemplateMap.put(modelId, new HashMap<Integer, Template[]>());
     	}
 		if (!mCmpldTemplateMap.get(modelId).containsKey(ord)) {
     		mCmpldTemplateMap.get(modelId).put(ord, compileTemplate(modelId, ord));
 		}
     	return mCmpldTemplateMap.get(modelId).get(ord);
     }
 
 
     // not in libanki
     private Template[] compileTemplate(long modelId, int ord) {
     	JSONObject model = mModels.get(modelId);
         JSONObject template;
         Template[] t = new Template[2];
 		try {
 			template = model.getJSONArray("tmpls").getJSONObject(ord);
 			String format = template.getString("qfmt").replace("{{cloze:", "{{cq:" + (ord+1) + ":");
             Log.i(AnkiDroidApp.TAG, "Compiling question template \"" + format + "\"");
             t[0] = Mustache.compiler().compile(format);
             format = template.getString("afmt").replace("{{cloze:", "{{ca:" + (ord+1) + ":");
             Log.i(AnkiDroidApp.TAG, "Compiling answer template \"" + format + "\"");
             t[1] = Mustache.compiler().compile(format);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
 		return t;
     }
 
     // not in libanki
     // Handle fields fetched from templates and any anki-specific formatting
     protected static class fieldParser implements Mustache.VariableFetcher {
         private Map <String, String> _fields;
         private String rubyr = " ?([^ ]+?)\\[(.+?)\\]";
         public fieldParser (Map<String, String> fields) {
             _fields = fields;
         }
 
         public Object get (Object ctx, String name) throws Exception {
             if (name.length() == 0) {
                 return null;
             }
             String txt = _fields.get(name);
             if (txt != null) {
                 return txt;
             }
             // field modifier handling as taken from template.py
             String[] parts = name.split(":", 3);
             String mod = null, extra = null, tag = null;
             if (parts.length == 1 || parts[0].length() == 0) {
                 return null;
             } else if (parts.length == 2) {
                 mod = parts[0];
                 tag = parts[1];
             } else if (parts.length == 3) {
                 mod = parts[0];
                 extra = parts[1];
                 tag = parts[2];
             }
 
             txt = _fields.get(tag);
 
             Log.d(AnkiDroidApp.TAG, "Processing field modifier " + mod + ": extra = " + extra + ", field " + tag + " = " + txt);
 
             // built-in modifiers
             // including furigana/ruby text handling
             if (mod.equals("text")) {
                 // strip html
                 if (txt != null) {
                     return Utils.stripHTML(txt);
                 }
                 return "";
             } else if (mod.equals("type")) {
                 // TODO: handle type field modifier
                 Log.e(AnkiDroidApp.TAG, "Unimplemented field modifier: " + mod);
                 return null;
             } else if (mod.equals("cq") || mod.equals("ca")) {
                 // cloze handling
                 if (txt == null || extra == null) return "";
                 int ord;
                 try {
                     ord = Integer.parseInt(extra);
                 } catch (NumberFormatException e) {
                     return "";
                 }
                 if (ord < 0) return "";
                 String rx = "\\{\\{c"+ord+"::(.*?)(?:::(.*?))?\\}\\}";
                 Matcher m = Pattern.compile(rx).matcher(txt);
                 String clozetxt = null;
                 if (mod.equals("ca")) {
                     // in answer
                     clozetxt = m.replaceAll("<span class=\"cloze\">$1</span>");
                 } else {
                     // in question
                     // unfortunately, Android's java implementation replaces
                     // non-matching captures with "null", requiring this ugly little loop
                     StringBuffer sb = new StringBuffer();
                     while (m.find()) {
                         if (m.group(2) != null) {
                             m.appendReplacement(sb, "<span class=\"cloze\">[...$2]</span>");
                         } else {
                             m.appendReplacement(sb, "<span class=\"cloze\">[...]</span>");
                         }
                     }
                     m.appendTail(sb);
                     clozetxt = sb.toString();
                 }
                 if (clozetxt.equals(txt)) {
                     // cloze wasn't found; return empty
                     return "";
                 }
                 // display any other clozes normally
                 clozetxt = clozetxt.replaceAll("\\{\\{c[0-9]+::(.*?)(?:::(.*?))?\\}\\}", "$1");
                 Log.d(AnkiDroidApp.TAG, "Cloze: ord=" + ord + ", txt=" + clozetxt);
                 return clozetxt;
             } else if (mod.equals("kanjionly")) {
                 if (txt == null) return txt;
                 return txt.replaceAll(rubyr, "$1");
             } else if (mod.equals("readingonly")) {
                 if (txt == null) return txt;
                 return txt.replaceAll(rubyr, "$2");
             } else if (mod.equals("furigana")) {
                 if (txt == null) return txt;
                 return txt.replaceAll(rubyr, "<ruby><rb>$1</rb><rt>$2</rt></ruby>");
             } else {
                 Log.w(AnkiDroidApp.TAG, "Unknown field modifier: " + mod);
                 return txt;
             }
         }
     }
 
 //    /**
 //     * This function recompiles the templates for question and answer. It should be called everytime we change mQformat
 //     * or mAformat, so if in the future we create set(Q|A)Format setters, we should include a call to this.
 //     */
 //    private void refreshTemplates(int ord) {
 //        // Question template
 //        StringBuffer sb = new StringBuffer();
 //        Matcher m = sOldStylePattern.matcher(mQformat);
 //        while (m.find()) {
 //            // Convert old style
 //            m.appendReplacement(sb, "{{" + m.group(1) + "}}");
 //        }
 //        m.appendTail(sb);
 //        Log.i(AnkiDroidApp.TAG, "Compiling question template \"" + sb.toString() + "\"");
 //        mQTemplate = Mustache.compiler().compile(sb.toString());
 //
 //        // Answer template
 //        sb = new StringBuffer();
 //        m = sOldStylePattern.matcher(mAformat);
 //        while (m.find()) {
 //            // Convert old style
 //            m.appendReplacement(sb, "{{" + m.group(1) + "}}");
 //        }
 //        m.appendTail(sb);
 //        Log.i(AnkiDroidApp.TAG, "Compiling answer template \"" + sb.toString() + "\"");
 //        mATemplate = Mustache.compiler().compile(sb.toString());
 //    }
     
     
     /**
      * Model changing
      * ***********************************************************************************************
      */
     
     // change
     //_changeNotes
     //_changeCards
 
     /**
      * Schema hash
      * ***********************************************************************************************
      */
 
     //scmhash
 
     /**
      * Required field/text cache
      * ***********************************************************************************************
      */
 
     private void _updateRequired(JSONObject m) {
 		try {
 	    	if (m.getInt("type") == Sched.MODEL_CLOZE) {
 	    		// nothing to do
 	    		return;
 	    	}
 	    	JSONArray req = new JSONArray();
 	    	ArrayList<String> flds = new ArrayList<String>();
 	    	JSONArray fields;
 			fields = m.getJSONArray("flds");
 	    	for (int i = 0; i < fields.length(); i++) {
 	    		flds.add(fields.getJSONObject(i).getString("name"));
 	    	}
 	    	JSONArray templates = m.getJSONArray("tmpls");
 	    	for (int i = 0; i < templates.length(); i++) {
 	    		JSONObject t = templates.getJSONObject(i);
 	    		Object[] ret = _reqForTemplate(m, flds, t);
 	    		JSONArray r = new JSONArray();
 	    		r.put(t.getInt("ord"));
 	    		r.put(ret[0]);
 	    		r.put(ret[1]);
 	    		req.put(r);
 	    	}
 	    	m.put("req", req);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
     private Object[] _reqForTemplate(JSONObject m, ArrayList<String> flds, JSONObject t) {
 		try {
 	    	ArrayList<String> a = new ArrayList<String> ();
 	    	ArrayList<String> b = new ArrayList<String> ();
 	    	for (String f : flds) {
 	    		a.add("1");
 	    		b.add("");
 	    	}
 	    	Object[] data;
 			data = new Object[]{1l, 1l, m.getLong("id"), 1l, t.getInt("ord"), "", Utils.joinFields(a.toArray(new String[a.size()]))};
 	    	String full = mCol._renderQA(data).get("q");
 	    	data = new Object[]{1l, 1l, m.getLong("id"), 1l, t.getInt("ord"), "", Utils.joinFields(b.toArray(new String[b.size()]))};
 	    	String empty = mCol._renderQA(data).get("q");
 	    	// if full and empty are the same, the template is invalid and there is no way to satisfy it
 	    	if (full.equals(empty)) {
 	    		return new Object[] {"none", new JSONArray(), new JSONArray()};
 	    	}
 	    	String type = "all";
 	    	JSONArray req = new JSONArray();
 	    	ArrayList<String> tmp = new ArrayList<String>();
 	    	for (int i = 0; i < flds.size(); i++) {
 	    		tmp.clear();
 	    		tmp.addAll(a);
 	    		tmp.remove(i);
 	    		tmp.add(i, "");
 	    		data[6] = Utils.joinFields(tmp.toArray(new String[tmp.size()]));
 	    		// if the result is same as empty, field is required
 	    		if (mCol._renderQA(data).get("q").equals(empty)) {
 	    			req.put(i);
 	    		}
 	    	}
 	    	if (req.length() > 0) {
 	    		return new Object[] {type, req};
 	    	}
 	    	// if there are no required fields, switch to any mode
 	    	type = "any";
 	    	req = new JSONArray();
 	    	for (int i = 0; i < flds.size(); i++) {
 	    		tmp.clear();
 	    		tmp.addAll(b);
 	    		tmp.remove(i);
 	    		tmp.add(i, "1");
 	    		data[6] = Utils.joinFields(tmp.toArray(new String[tmp.size()]));
 	    		// if not the same as empty, this field can make the card non-blank
 	    		if (mCol._renderQA(data).get("q").equals(empty)) {
 	    			req.put(i);
 	    		}
 	    	}
 	    	return new Object[]{ type, req };
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
 
     /** Given a joined field string, return available template ordinals */
     public ArrayList<Integer> availOrds(JSONObject m, String flds) {
     	try {
         	if (m.getInt("type") == Sched.MODEL_CLOZE) {
         		return _availClozeOrds(m, flds);
         	}
         	String[] fields = Utils.splitFields(flds);
         	for (String f : fields) {
         		f = f.trim();
         	}
         	ArrayList<Integer> avail = new ArrayList<Integer>();
 			JSONArray reqArray = m.getJSONArray("req");
 			for (int i = 0; i < reqArray.length(); i++) {
 				JSONArray sr = reqArray.getJSONArray(i);
 
 				int ord = sr.getInt(0);
 				String type = sr.getString(1);				
 				JSONArray req = sr.getJSONArray(2);
 
 				if (type.equals("none")) {
 					// unsatisfiable template
 					continue;
 				} else if (type.equals("all")) {
 					// AND requirement?
 					boolean ok = true;
 					for (int j = 0; j < req.length(); j++) {
 						if (fields.length <= j || fields[j] == null || fields[j].length() == 0) {
 							// missing and was required
 							ok = false;
 							break;
 						}
 					}
 					if (!ok) {
 						continue;
 					}
 				} else if (type.equals("any")) {
 					// OR requirement?
 					boolean ok = false;
 					for (int j = 0; j < req.length(); j++) {
 						if (fields.length <= j || fields[j] == null || fields[j].length() == 0) {
 							// missing and was required
 							ok = true;
 							break;
 						}
 					}
 					if (!ok) {
 						continue;
 					}	
 				}
 				avail.add(ord);
 			}
 	    	return avail;
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     }
 
     private ArrayList<Integer> _availClozeOrds(JSONObject m, String flds) {
     	// TODO
     	return null;
     }
     
     /**
      * Sync handling
      * ***********************************************************************************************
      */
 
     public void beforeUpload() {
 		try {
 	    	for (JSONObject m : all()) {
 				m.put("usn", 0);
 	    	}
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	save();
     }
 
     /**
      * Routines from Stdmodels.py
      * ***********************************************************************************************
      */
 
     public static JSONObject addBasicModel(Collection col) {
     	return addBasicModel(col, "Basic");
     }
     public static JSONObject addBasicModel(Collection col, String name) {
     	Models mm = col.getModels();
     	JSONObject m = mm.newModel(name);
     	JSONObject fm = mm.newField("Front");
     	mm.addField(m, fm);
     	fm = mm.newField("Back");
     	mm.addField(m, fm);
     	JSONObject t = mm.newTemplate("Card 1");
     	try {
 			t.put("qfmt", "{{Front}}");
 	    	t.put("afmt", "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}");
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	mm.addTemplate(m, t);
     	mm.add(m);
     	return m;
     }
 
     public static JSONObject addClozeModel(Collection col) {
     	Models mm = col.getModels();
     	JSONObject m = mm.newModel("Cloze");
     	try {
 			m.put("type", Sched.MODEL_CLOZE);
 			String txt = "Text";
 			JSONObject fm = mm.newField(txt);
 			mm.addField(m,  fm);
 			fm = mm.newField("Extra");
 			mm.addField(m,  fm);
 	    	JSONObject t = mm.newTemplate("Cloze");
 	    	String fmt = "{{cloze:" + txt + "}}";
 	    	m.put("css", m.getString("css") + ".cloze {"+
 	    			"font-weight: bold;"+
 	    			"color: blue;"+
 	    			"}");
 			t.put("qfmt", fmt);
 			t.put("afmt", fmt + "<br>\n{{Extra}}");
 			mm.addTemplate(m,  t);
 			mm.add(m);
 		} catch (JSONException e) {
 			throw new RuntimeException(e);
 		}
     	return m;
     }
     
     /**
      * Other stuff
      * NOT IN LIBANKI
      * ***********************************************************************************************
      */
 
     public void setChanged() {
     	mChanged = true;
     }
 
     /**
      * Returns a string where all colors have been inverted.
      * It applies to anything that is in a tag and looks like #FFFFFF
      * 
      * Example: Here only #000000 will be replaced (#777777 is content)
      * <span style="color: #000000;">Code #777777 is the grey color</span>
      * 
      * This is done with a state machine with 2 states:
      *  - 0: within content
      *  - 1: within a tag
      */
     public static String invertColors(String text) {
         final String[] colors = {"color=\"white\"", "color=\"black\""};
         final String[] htmlColors = {"color=\"#000000\"", "color=\"#ffffff\""};
         for (int i = 0; i < colors.length; i++) {
         	text = text.replace(colors[i], htmlColors[i]);
         }
         int state = 0;
         StringBuffer inverted = new StringBuffer(text.length());
         for(int i=0; i<text.length(); i++) {
             char character = text.charAt(i);
             if (state == 1 && character == '#') {
             	// TODO: handle shorter html-colors too (e.g. #0000)
                 inverted.append(invertColor(text.substring(i+1, i+7)));
             }
             else {
                 if (character == '<') {
                     state = 1;
                 }
                 if (character == '>') {
                     state = 0;
                 }
                 inverted.append(character);
             }
         }
         return inverted.toString();
     }
 
     private static String invertColor(String color) {
 	    if (color.length() != 0) {
 	        color = StringTools.toUpperCase(color);
 	    }
         final char[] items = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
         final char[] tmpItems = {'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v'};
         for (int i = 0; i < 16; i++) {
             color = color.replace(items[i], tmpItems[15-i]);
         }
         for (int i = 0; i < 16; i++) {
             color = color.replace(tmpItems[i], items[i]);
         }
     	return color;		
     }
 
 
     public HashMap<Long, HashMap<Integer, String>> getTemplateNames() {
     	HashMap<Long, HashMap<Integer, String>> result = new HashMap<Long, HashMap<Integer, String>>();
     	for (JSONObject m : mModels.values()) {
     		JSONArray templates;
 			try {
 				templates = m.getJSONArray("tmpls");
 	    		HashMap<Integer, String> names = new HashMap<Integer, String>();
 	    		for (int i = 0; i < templates.length(); i++) {
 	    			JSONObject t = templates.getJSONObject(i);
 	    			names.put(t.getInt("ord"), t.getString("name"));
 	    		}
 	    		result.put(m.getLong("id"), names);
 			} catch (JSONException e) {
 				throw new RuntimeException(e);
 			}
     	}
     	return result;
     }
 
 
     /**
      * @return the ID
      */
     public int getId() {
         return mId;
     }
 
     
     /**
      * @return the name
      */
     public String getName() {
         return mName;
     }
 
 }
