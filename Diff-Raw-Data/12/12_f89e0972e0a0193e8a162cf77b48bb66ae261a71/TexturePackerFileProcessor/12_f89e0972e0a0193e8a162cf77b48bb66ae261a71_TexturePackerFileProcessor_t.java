 
 package com.badlogic.gdx.tools.imagepacker;
 
 import java.io.File;
 import java.io.FileReader;
 import java.util.ArrayList;
 
 import com.badlogic.gdx.tools.FileProcessor;
import com.badlogic.gdx.tools.FileProcessor.InputFile;
 import com.badlogic.gdx.tools.imagepacker.TexturePacker2.Settings;
 import com.badlogic.gdx.utils.Json;
 import com.badlogic.gdx.utils.JsonReader;
 import com.badlogic.gdx.utils.ObjectMap;
 
 /** @author Nathan Sweet */
 public class TexturePackerFileProcessor extends FileProcessor {
 	private final Settings defaultSettings;
 	private ObjectMap<File, Settings> dirToSettings = new ObjectMap();
 	private Json json = new Json();
 	private String packFileName;
	private File root;
 
 	public TexturePackerFileProcessor () {
 		this(new Settings(), "pack.atlas");
 	}
 
 	public TexturePackerFileProcessor (Settings defaultSettings, String packFileName) {
 		this.defaultSettings = defaultSettings;
 		this.packFileName = packFileName;
 
 		setFlattenOutput(true);
 		addInputSuffix(".png", ".jpg");
 	}
 
	public ArrayList<InputFile> process (File inputFile, File outputRoot) throws Exception {
		root = inputFile;
		return super.process(inputFile, outputRoot);
	}

 	public ArrayList<InputFile> process (File[] files, File outputRoot) throws Exception {
 		// Delete pack file and images.
 		new File(outputRoot, packFileName).delete();
 		FileProcessor deleteProcessor = new FileProcessor() {
 			protected void processFile (InputFile inputFile) throws Exception {
 				inputFile.inputFile.delete();
 			}
 		};
 		deleteProcessor.setRecursive(false);
 		deleteProcessor.addInputSuffix(".png", ".jpg");
 		deleteProcessor.process(outputRoot, null);
 		return super.process(files, outputRoot);
 	}
 
 	protected void processDir (InputFile inputDir, ArrayList<InputFile> files) throws Exception {
 		// Start with a copy of parent dir's settings or the default settings.
 		Settings settings = dirToSettings.get(inputDir.inputFile.getParentFile());
 		if (settings == null)
 			settings = new Settings(defaultSettings);
 		else
 			settings = new Settings(settings);
 		dirToSettings.put(inputDir.inputFile, settings);
 
 		// Merge settings from pack.json file.
 		File settingsFile = new File(inputDir.inputFile, "pack.json");
 		if (settingsFile.exists()) json.readFields(settings, new JsonReader().parse(new FileReader(settingsFile)));
 
 		// Pack.
		TexturePacker2 packer = new TexturePacker2(root, settings);
 		for (InputFile file : files)
 			packer.addImage(file.inputFile);
 		packer.pack(inputDir.outputDir, packFileName);
 	}
 }
