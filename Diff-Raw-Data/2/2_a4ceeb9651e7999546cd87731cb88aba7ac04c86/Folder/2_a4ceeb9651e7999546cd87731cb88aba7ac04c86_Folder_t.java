 /*****************************************************************************
  * All public interface based on Starteam API are a property of Borland, 
  * those interface are reproduced here only for testing purpose. You should
  * never use those interface to create a competitive product to the Starteam
  * Server. 
  * 
  * The implementation is given AS-IS and should not be considered a reference 
  * to the API. The behavior on a lots of method and class will not be the
  * same as the real API. The reproduction only seek to mimic some basic 
  * operation. You will not found anything here that can be deduced by using
  * the real API.
  * 
  * Fake-Starteam is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *****************************************************************************/
 package com.starbase.starteam;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Properties;
 
 import org.ossnoize.fakestarteam.FakeFolder;
 import org.ossnoize.fakestarteam.FileUtility;
 import org.ossnoize.fakestarteam.InternalPropertiesProvider;
 import org.ossnoize.fakestarteam.SimpleTypedResourceIDProvider;
 import org.ossnoize.fakestarteam.exception.InvalidOperationException;
 
 public class Folder extends Item {
 	private static final String FOLDER_PROPERTIES = "folder.properties";
 	private static final FilenameFilter FOLDER_TESTER = new FilenameFilter() {
 		@Override
 		public boolean accept(File dir, String name) {
 			return name.equalsIgnoreCase(FOLDER_PROPERTIES);
 		}
 	};
 
 	private static final FilenameFilter FILE_TESTER = new FilenameFilter() {
 		@Override
 		public boolean accept(File dir, String name) {
 			boolean valid = true;
 			for(char c : name.toCharArray()) {
 				if(!Character.isDigit(c)) {
 					valid = false;
 				}
 			}
 			return valid;
 		}
 	};
 
 	protected Folder() {
 	}
 	
 	public Folder(Server server) {
 		throw new UnsupportedOperationException("Unknown goal for this constructor");
 	}
 
 	public Folder(Folder parent, String name, String workingFolder) {
 		itemProperties = new Properties();
 		// initialize the basic properties of the folder.
 		itemProperties.setProperty(propertyKeys.OBJECT_ID, 
 				Integer.toString(SimpleTypedResourceIDProvider.getProvider().registerNew(this)));
 		this.parent = parent;
 		view = parent.getView();
 		setName(name);
 		try {
 			File storage = InternalPropertiesProvider.getInstance().getStorageLocation();
 			String folder = storage.getCanonicalPath() + File.separator + getObjectID();
 			holdingPlace = new File(folder);
 			validateHoldingPlace();
 			loadFolderProperties();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 		shareTo(parent);
 	}
 
 	protected void validateHoldingPlace() {
 		if(null == holdingPlace) {
 			throw new InvalidOperationException("Cannot create a folder without an holding place.");
 		}
 		if(holdingPlace.exists()) {
 			if(holdingPlace.isFile()) {
 				holdingPlace.delete();
 				holdingPlace.mkdirs();
 			}
 		} else {
 			holdingPlace.mkdirs();
 		}
 	}
 	
 	public void setName(java.lang.String name) {
 		if(itemProperties == null) {
 			throw new InvalidOperationException("The properties are not initialized");
 		}
 		itemProperties.setProperty(propertyKeys.FOLDER_NAME, name);
 	}
 	
 	public String getName() {
 		if(itemProperties == null) {
 			throw new InvalidOperationException("The properties are not initialized");
 		}
 		return itemProperties.getProperty(propertyKeys.FOLDER_NAME);
 	}
 	
 	public Folder[] getSubFolders() {
 		if(itemProperties == null)
 			throw new InvalidOperationException("The properties are not initialized");
 
 		String listOfFolder = itemProperties.getProperty(propertyKeys._CHILD_FOLDER);
 		List<Folder> generatedList = new ArrayList<Folder>();
 		if(listOfFolder != null && listOfFolder.length() > 0) {
 			for(String folderId : listOfFolder.split(";")) {
 				if(null != folderId && 0 < folderId.length()) {
 					try {
 						int id = Integer.parseInt(folderId);
 						SimpleTypedResource ressource =
 								SimpleTypedResourceIDProvider.getProvider().findExisting(id);
 						if(null != ressource && ressource instanceof Folder) {
 							generatedList.add((Folder)ressource);
 						} else {
 							Folder child = new FakeFolder(this.view, id, this);
 							generatedList.add(child);
 						}
 					} catch (NumberFormatException ne) {
 						throw new InvalidOperationException("Folder child id corrupted.");
 					}
 				}
 			}
 		}
 		Folder[] buffer = new Folder[generatedList.size()];
 		return generatedList.toArray(buffer);
 	}
 	
 	private com.starbase.starteam.File[] getFiles() {
 		List<com.starbase.starteam.File> generatedList = new ArrayList<com.starbase.starteam.File>();
 		for(File f : holdingPlace.listFiles()) {
 			if(f.isDirectory()) {
 				String[] fileRevision = f.list(FILE_TESTER);
 				if(null != fileRevision && fileRevision.length >= 1) {
 					generatedList.add(new com.starbase.starteam.File(f.getName(), this));
 				}
 			}
 		}
 		com.starbase.starteam.File[] buffer = new com.starbase.starteam.File[generatedList.size()];
 		return generatedList.toArray(buffer);
 	}
 	
 	public Item[] getItems(java.lang.String typeName) {
 		if(typeName.equalsIgnoreCase(getTypeNames().FOLDER))
 			return getSubFolders();
 		else if (typeName.equalsIgnoreCase(getTypeNames().FILE)) {
 			return getFiles();
 		}
 		return new Item[0];
 	}
 	
 	@Override
 	public Folder getParentFolder() {
 		return parent;
 	}
 	
 	@Override
 	public void update() {
 		if(itemProperties == null) {
 			throw new InvalidOperationException("Properties are not initialized yet!!!");
 		}
 		FileOutputStream fout = null;
 		try {
 			fout = new FileOutputStream(holdingPlace.getCanonicalPath() + File.separator + FOLDER_PROPERTIES);
 			itemProperties.store(fout, "Folders properties");
 		} catch (IOException e) {
 			e.printStackTrace();
 		} finally {
 			FileUtility.close(fout);
 		}
 	}
 	
 	@Override
 	public String toString() {
 		return getName();
 	}
 	
 	protected void loadFolderProperties() {
 		FileInputStream fin = null;
 		try {
 			File folderProperty = new File(holdingPlace.getCanonicalPath() + File.separator + FOLDER_PROPERTIES);
 			if(folderProperty.exists()) {
 				fin = new FileInputStream(folderProperty);
 				itemProperties.load(fin);
 				int id = Integer.parseInt(itemProperties.getProperty(propertyKeys.OBJECT_ID));
 				SimpleTypedResourceIDProvider.getProvider().registerExisting(id, this);
 			} else {
 				// initialize the basic properties of the folder.
 				if (null != parent)	{
 					itemProperties.setProperty(propertyKeys.PARENT_OBJECT_ID, Integer.toString(parent.getObjectID()));
 					itemProperties.setProperty(propertyKeys.FOLDER_PATH, 
 							parent.getParentFolderQualifiedName() + File.separatorChar + getName());
				} else {
					itemProperties.setProperty(propertyKeys.FOLDER_PATH, getName());
 				}
 				update();
 			}
 		} catch (IOException e) {
 			e.printStackTrace();
 		} finally {
 			FileUtility.close(fin);
 		}
 	}
 	
 	@Override
 	public Item shareTo(Folder folder) {
 		StringBuffer childIdList = null;
 		if(folder.itemProperties.containsKey(propertyKeys._CHILD_FOLDER)) {
 			childIdList = new StringBuffer(folder.itemProperties.getProperty(propertyKeys._CHILD_FOLDER)).append(";");
 		} else {
 			childIdList = new StringBuffer(25);
 		}
 		childIdList.append(getObjectID());
 		folder.itemProperties.setProperty(propertyKeys._CHILD_FOLDER, childIdList.toString());
 		folder.update();
 		return folder;
 	}
 }
