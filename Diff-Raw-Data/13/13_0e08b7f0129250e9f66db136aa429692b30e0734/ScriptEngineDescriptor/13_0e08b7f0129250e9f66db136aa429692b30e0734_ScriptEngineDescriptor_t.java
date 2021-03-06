 package org.openstreetmap.josm.plugins.scripting.model;
 
 import static org.openstreetmap.josm.tools.I18n.tr;
 
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
import java.util.logging.Logger;
 
 import javax.script.ScriptEngineFactory;
 
 import org.openstreetmap.josm.Main;
 import org.openstreetmap.josm.data.Preferences;
 import org.openstreetmap.josm.plugins.scripting.util.Assert;
 
 /**
  * Describes a scripting engine used in the scripting plugin.
  *
  */
 public class ScriptEngineDescriptor implements PreferenceKeys {
	static private final Logger logger = Logger.getLogger(ScriptEngineDescriptor.class.getName());
 	
 	static public enum ScriptEngineType {
 		/**
 		 * an embedded scripting engine, i.e. the embedded Mozilla Rhino engine 
 		 */
 		EMBEDDED("embedded"),
 		/**
 		 * a scripting engine supplied as JSR233 compliant scripting engine
 		 */
 		PLUGGED("plugged");
 		
 		public final String preferencesValue;		
 		ScriptEngineType(String preferencesValue) {
 			this.preferencesValue = preferencesValue;
 		}
 		
 		/**
 		 * <p>Infers the script engine type from preference value. The value is a string
 		 * <code>type/engineId</code>. This methods decodes the component <code>type</code>.
 		 * Replies <code>null</code> if no type can be inferred.</p>
 		 * 
 		 * <strong>Examples</strong>
 		 * <pre>
 		 *    type = ScriptEngineType.fromPreferencesValue("embedded/rhino");
 		 *    type = ScriptEngineType.fromPreferencesValue("plugged/groovy");
 		 * </pre>
 		 * 
 		 * @param preferencesValue the preferences value
 		 * @return the type 
 		 */
 		static public ScriptEngineType fromPreferencesValue(String preferencesValue) {
 			if (preferencesValue == null) return null;
 			preferencesValue = preferencesValue.trim().toLowerCase();
 			int i = preferencesValue.indexOf("/");
 			if (i > 0) preferencesValue = preferencesValue.substring(0, i);
 			for (ScriptEngineType type: ScriptEngineType.values()){
 				if (type.preferencesValue.equals(preferencesValue)) return type; 
 			}
 			return null;			
 		}
 	}
 	
 	
 	private ScriptEngineType engineType;
 	private String engineId;
 	private String languageName = null;
 	private String engineName = null;
 	private final List<String> contentMimeTypes = new ArrayList<String>();
 
 	
 	/**
 	 * The default script engine descriptor. Refers to the embedded script engine based
 	 * on Mozilla Rhino.
 	 */
 	static public final ScriptEngineDescriptor DEFAULT_SCRIPT_ENGINE = new ScriptEngineDescriptor(
 			ScriptEngineType.EMBEDDED,
 			"rhino",
 			"JavaScript",
 			"Mozilla Rhino",
 			"text/javascript"
 	);
 	
 	/**
 	 * An unmodifiable map of embedded script engines (currently only one entry for the embedded
 	 * engine based on Mozilla Rhino)
 	 */
 	static public final Map<String, ScriptEngineDescriptor> EMBEDDED_SCRIPT_ENGINES;
 	static {
 		 HashMap<String, ScriptEngineDescriptor> m = new HashMap<String, ScriptEngineDescriptor>();
 		 m.put(DEFAULT_SCRIPT_ENGINE.getEngineId(), DEFAULT_SCRIPT_ENGINE);
 		 EMBEDDED_SCRIPT_ENGINES = Collections.unmodifiableMap(m);
 	}
 	
 	
 	/**
 	 * <p>Replies a script engine descriptor derived from a preference value <code>engineType/engineId</code> in
 	 * {@link Main#prefs}.<p>
 	 * 
 	 * @return the scripting engine descriptor 
 	 * @see #buildFromPreferences(Preferences)
 	 */	
 	static public ScriptEngineDescriptor buildFromPreferences(){
 		return buildFromPreferences(Main.pref);
 	}
 	
 	/**
 	 * <p>Replies a script engine descriptor derived from a preference value <code>engineType/engineId</code>.<p>
 	 * 
 	 * <p>It looks for  the preference value with key {@link PreferenceKeys#PREF_KEY_SCRIPTING_ENGINE}. If this
 	 * key doesn't exist or if it doesn't refer to a supported combination of <code>engineType</code> and
 	 * <code>engineId</code>, the default scripting engine descriptor {@link #DEFAULT_SCRIPT_ENGINE} is
 	 * replied.</p>
 	 * 
 	 * @param preferences the preferences 
 	 * @return the scripting engine descriptor 
 	 */
 	static public ScriptEngineDescriptor buildFromPreferences(Preferences preferences) {
 		if (preferences == null) return DEFAULT_SCRIPT_ENGINE;
 		String prefValue = preferences.get(PREF_KEY_SCRIPTING_ENGINE);
 		return buildFromPreferences(prefValue);
 	}
 	
 	/**
 	 * <p>Replies a script engine descriptor derived from a preference value <code>engineType/engineId</code>.<p>
 	 * 	 
 	 * @param preferenceValue the preference value. If null, replies the {@link #DEFAULT_SCRIPT_ENGINE} 
 	 * @return the scripting engine descriptor 
 	 */
 	static public ScriptEngineDescriptor buildFromPreferences(String preferenceValue){
 		if (preferenceValue == null) return DEFAULT_SCRIPT_ENGINE;
 		ScriptEngineType type = ScriptEngineType.fromPreferencesValue(preferenceValue);
 		if (type == null) {
 			//NOTE: might be a legal preferences value for former plugin versions. No attempt to recover from these
 			// values, when this code goes productive, former preference values are automatically reset to the
 			// to the current default scripting engine.
 			System.out.println(tr("Warning: preference with key ''{0}'' consist of an unsupported value. Expected pattern ''type/id'', got ''{1}''. Assuming default scripting engine.", PREF_KEY_SCRIPTING_ENGINE, preferenceValue));
 			return DEFAULT_SCRIPT_ENGINE;
 		}
 		int i = preferenceValue.indexOf("/");
 		if (i < 0) return DEFAULT_SCRIPT_ENGINE;
 		String engineId = preferenceValue.substring(i+1);
 		switch(type){
 			case EMBEDDED:				
 				if (engineId == null) return DEFAULT_SCRIPT_ENGINE;
 				engineId = engineId.trim().toLowerCase();
 				ScriptEngineDescriptor desc = EMBEDDED_SCRIPT_ENGINES.get(engineId);
 				if (desc == null) {
 					System.out.println(tr("Warning: preference with key ''{0}'' refers to an unsupported embedded scripting engine with id ''{1}''. Assuming default scripting engine.", PREF_KEY_SCRIPTING_ENGINE, engineId));
 					return DEFAULT_SCRIPT_ENGINE;
 				}
 				return desc;
 			case PLUGGED:
 				engineId = engineId.trim(); // don't lowercase. Lookup in ScriptEngineManager could be case sensitive
 				if (! JSR223ScriptEngineProvider.getInstance().hasEngineWithName(engineId)) {
 					System.out.println(tr("Warning: preference with key ''{0}'' refers to an unsupported JSR223 compatible scripting engine with id ''{1}''. Assuming default scripting engine.", PREF_KEY_SCRIPTING_ENGINE, engineId));
 					return DEFAULT_SCRIPT_ENGINE;
 				}
 				return new ScriptEngineDescriptor(ScriptEngineType.PLUGGED, engineId);
 		}		 
 		return DEFAULT_SCRIPT_ENGINE;
 	}
 
 	protected void initParametersForJSR223Engine(String engineId) {
 		ScriptEngineFactory factory = JSR223ScriptEngineProvider.getInstance().getScriptFactoryByName(engineId);
 		if (factory == null){
 			this.languageName = null;
 			this.engineName = null;
 			this.contentMimeTypes.clear();
 			return;
 		} 
 		initParametersForJSR223Engine(factory);
 	}
 	
 	protected void initParametersForJSR223Engine(ScriptEngineFactory factory) {
 		this.languageName = factory.getLanguageName();
 		this.engineName = factory.getEngineName();
 		this.contentMimeTypes.clear();
 		this.contentMimeTypes.addAll(factory.getMimeTypes());
 	}
 		
 	/**
 	 * Creates a new descriptor for the type {@link ScriptEngineType#PLUGGED}.
 	 * 
 	 * @param engineId the engine id. Must not be null. 
 	 */
 	public ScriptEngineDescriptor(String engineId){
 		Assert.assertArgNotNull(engineId, "id");
 		this.engineType = ScriptEngineType.PLUGGED;
 		this.engineId = engineId.trim();
 		initParametersForJSR223Engine(this.engineId);
 	}
 
 	/**
 	 * Creates a new descriptor.
 	 * 
 	 * @param engineType the engine type. Must not be null.
 	 * @param engineId the engine id. Must not be null. 
 	 */
 	public ScriptEngineDescriptor(ScriptEngineType engineType, String engineId) {
 		Assert.assertArgNotNull(engineType, "type");
 		Assert.assertArgNotNull(engineId, "id");
 		this.engineId = engineId;
 		this.engineType= engineType; 
 		if (this.engineType.equals(ScriptEngineType.PLUGGED)) {
 			initParametersForJSR223Engine(this.engineId);
 		}
 	}
 	
 	/**
 	 * Creates a new descriptor.
 	 * 
 	 * @param engineType the engine type. Must not be null.
 	 * @param engineId the engine id. Must not be null. 
 	 * @param languageName the name of the scripting language. May be null.
 	 * @param engineName the name of the scripting engine. May be null.
 	 * @param contentType the content type of the script sources. Ignored if null.
 	 */
 	public ScriptEngineDescriptor(ScriptEngineType engineType, String engineId, String languageName, String engineName, String contentType) {
 		Assert.assertArgNotNull(engineType, "type");
 		Assert.assertArgNotNull(engineId, "id");
 		this.engineId = engineId;
 		this.engineType= engineType;
 		this.languageName = languageName == null ? null : languageName.trim();
 		this.engineName = engineName == null ? null : engineName.trim();
 		if (contentType != null) {
 			this.contentMimeTypes.add(contentType.trim());
 		}
 	}
 	
 	/**
 	 * Creates a new descriptor given a factory for JSR223-compatible script
 	 * engines.
 	 * 
 	 * @param factory the factory. Must not be null. 
 	 */
 	public ScriptEngineDescriptor(ScriptEngineFactory factory) {
 		Assert.assertArgNotNull(factory, "factory");
 		this.engineType = ScriptEngineType.PLUGGED;
		List<String> engineNames = factory.getNames();
		if (engineNames == null || engineNames.isEmpty()) {
			logger.warning(MessageFormat.format("script engine factory ''{0}'' doesn''t provide an engine id. Using engine name ''{1}'' instead.", factory.getEngineName()));		
			this.engineId = factory.getEngineName();
		} else {
			// use the first of the provided names as ID
			this.engineId = engineNames.get(0);
		}
		this.engineId = factory.getNames().get(0);
 		initParametersForJSR223Engine(factory);
 	}
 	
 	/**
 	 * Replies true, if this is the descriptor for the default scripting engine.
 	 * @return
 	 */
 	public boolean isDefault() {
 		return this.equals(DEFAULT_SCRIPT_ENGINE);
 	}
 	
 	/**
 	 * Replies the engine id
 	 * 
 	 * @return
 	 */
 	public String getEngineId() {
 		return engineId;
 	}
 	
 	/**
 	 * Replies the engine type
 	 * 
 	 * @return
 	 */
 	public ScriptEngineType getEngineType() {
 		return engineType;
 	}
 	
 	/**
 	 * Replies a string representing the descriptor in the format <em>engineType/engineInfo</em>.
 	 *  
 	 * @return the preferences value
 	 * @see #buildFromPreferences()
 	 */
 	public String getPreferencesValue() {
 		return MessageFormat.format("{0}/{1}", engineType.preferencesValue, engineId); 
 	}
 	
 	/**
 	 * Replies the name of the scripting language supported by this scripting engine, or null,
 	 * if the name isn't known.
 	 * 
 	 * @return the name of the scripting language supported by this scripting engine.
 	 */
 	public String getLanguageName() {
 		return languageName;
 	}
 	
 	/**
 	 * Replies the name of the scripting engine , or null,
 	 * if the name isn't known.
 	 * 
 	 * @return the name of the scripting engine
 	 */	
 	public String getEngineName() {
 		return engineName;
 	}
 	
 	/**
 	 * Replies the content types of the script source 
 	 * 
 	 * @return the content types. An unmodifiable list. An empty list, if no content types are known. 
 	 */
 	public List<String> getContentMimeTypes() {
 		return Collections.unmodifiableList(contentMimeTypes);
 	}
 	
 	public String toString() {
 		return getPreferencesValue();
 	}
 	
 	@Override
 	public int hashCode() {
 		final int prime = 31;
 		int result = 1;
 		result = prime * result + ((engineId == null) ? 0 : engineId.hashCode());
 		result = prime * result + ((engineType == null) ? 0 : engineType.hashCode());
 		return result;
 	}
 
 	@Override
 	public boolean equals(Object obj) {
 		if (this == obj)
 			return true;
 		if (obj == null)
 			return false;
 		if (getClass() != obj.getClass())
 			return false;
 		ScriptEngineDescriptor other = (ScriptEngineDescriptor) obj;
 		if (engineId == null) {
 			if (other.engineId != null)
 				return false;
 		} else if (!engineId.equals(other.engineId))
 			return false;
 		if (engineType != other.engineType)
 			return false;
 		return true;
 	}
 }
