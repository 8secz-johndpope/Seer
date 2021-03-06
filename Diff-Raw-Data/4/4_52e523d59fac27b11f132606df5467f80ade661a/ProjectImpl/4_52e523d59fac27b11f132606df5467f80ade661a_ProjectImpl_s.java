 /**
  * <copyright>
  * </copyright>
  *
  * $Id$
  */
 package de.jutzig.jabylon.properties.impl;
 
 import java.io.File;
 import java.util.Collection;
 import java.util.Locale;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.eclipse.emf.common.notify.NotificationChain;
 import org.eclipse.emf.common.util.EList;
 import org.eclipse.emf.common.util.URI;
 import org.eclipse.emf.ecore.EClass;
 import org.eclipse.emf.ecore.InternalEObject;
 import org.eclipse.emf.ecore.util.InternalEList;
 import org.eclipse.emf.internal.cdo.CDOObjectImpl;
 
 import de.jutzig.jabylon.properties.Project;
 import de.jutzig.jabylon.properties.ProjectStats;
 import de.jutzig.jabylon.properties.PropertiesFactory;
 import de.jutzig.jabylon.properties.PropertiesPackage;
 import de.jutzig.jabylon.properties.PropertyBag;
 import de.jutzig.jabylon.properties.PropertyFileDescriptor;
 import de.jutzig.jabylon.properties.Workspace;
 import de.jutzig.jabylon.properties.util.scanner.PropertyFileAcceptor;
 import de.jutzig.jabylon.properties.util.scanner.WorkspaceScanner;
 
 /**
  * <!-- begin-user-doc -->
  * An implementation of the model object '<em><b>Project</b></em>'.
  * <!-- end-user-doc -->
  * <p>
  * The following features are implemented:
  * <ul>
  *   <li>{@link de.jutzig.jabylon.properties.impl.ProjectImpl#getName <em>Name</em>}</li>
  *   <li>{@link de.jutzig.jabylon.properties.impl.ProjectImpl#getPropertyBags <em>Property Bags</em>}</li>
  *   <li>{@link de.jutzig.jabylon.properties.impl.ProjectImpl#getWorkspace <em>Workspace</em>}</li>
  *   <li>{@link de.jutzig.jabylon.properties.impl.ProjectImpl#getBase <em>Base</em>}</li>
  *   <li>{@link de.jutzig.jabylon.properties.impl.ProjectImpl#getStats <em>Stats</em>}</li>
  * </ul>
  * </p>
  *
  * @generated
  */
 public class ProjectImpl extends CDOObjectImpl implements Project {
 	/**
 	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @see #getName()
 	 * @generated
 	 * @ordered
 	 */
 	protected static final String NAME_EDEFAULT = null;
 	
 
 	/**
 	 * The default value of the '{@link #getBase() <em>Base</em>}' attribute.
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @see #getBase()
 	 * @generated
 	 * @ordered
 	 */
 	protected static final URI BASE_EDEFAULT = null;
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	protected ProjectImpl() {
 		super();
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	protected EClass eStaticClass() {
 		return PropertiesPackage.Literals.PROJECT;
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	protected int eStaticFeatureCount() {
 		return 0;
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public String getName() {
 		return (String)eDynamicGet(PropertiesPackage.PROJECT__NAME, PropertiesPackage.Literals.PROJECT__NAME, true, true);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public void setName(String newName) {
 		eDynamicSet(PropertiesPackage.PROJECT__NAME, PropertiesPackage.Literals.PROJECT__NAME, newName);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@SuppressWarnings("unchecked")
 	public EList<PropertyBag> getPropertyBags() {
 		return (EList<PropertyBag>)eDynamicGet(PropertiesPackage.PROJECT__PROPERTY_BAGS, PropertiesPackage.Literals.PROJECT__PROPERTY_BAGS, true, true);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public Workspace getWorkspace() {
 		return (Workspace)eDynamicGet(PropertiesPackage.PROJECT__WORKSPACE, PropertiesPackage.Literals.PROJECT__WORKSPACE, true, true);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public NotificationChain basicSetWorkspace(Workspace newWorkspace, NotificationChain msgs) {
 		msgs = eBasicSetContainer((InternalEObject)newWorkspace, PropertiesPackage.PROJECT__WORKSPACE, msgs);
 		return msgs;
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public void setWorkspace(Workspace newWorkspace) {
 		eDynamicSet(PropertiesPackage.PROJECT__WORKSPACE, PropertiesPackage.Literals.PROJECT__WORKSPACE, newWorkspace);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated NOT
 	 */
 	public URI getBase() {
 		return getWorkspace().getRoot().appendSegment(getName());
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public ProjectStats getStats() {
 		return (ProjectStats)eDynamicGet(PropertiesPackage.PROJECT__STATS, PropertiesPackage.Literals.PROJECT__STATS, true, true);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public NotificationChain basicSetStats(ProjectStats newStats, NotificationChain msgs) {
 		msgs = eDynamicInverseAdd((InternalEObject)newStats, PropertiesPackage.PROJECT__STATS, msgs);
 		return msgs;
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	public void setStats(ProjectStats newStats) {
 		eDynamicSet(PropertiesPackage.PROJECT__STATS, PropertiesPackage.Literals.PROJECT__STATS, newStats);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated NOT
 	 */
 	public void fullScan() {
 		getPropertyBags().clear();
 		WorkspaceScanner scanner = new WorkspaceScanner();
 		scanner.fullScan(new FileAcceptor(), this);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@SuppressWarnings("unchecked")
 	@Override
 	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
 		switch (featureID) {
 			case PropertiesPackage.PROJECT__PROPERTY_BAGS:
 				return ((InternalEList<InternalEObject>)(InternalEList<?>)getPropertyBags()).basicAdd(otherEnd, msgs);
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				if (eInternalContainer() != null)
 					msgs = eBasicRemoveFromContainer(msgs);
 				return basicSetWorkspace((Workspace)otherEnd, msgs);
 		}
 		return super.eInverseAdd(otherEnd, featureID, msgs);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
 		switch (featureID) {
 			case PropertiesPackage.PROJECT__PROPERTY_BAGS:
 				return ((InternalEList<?>)getPropertyBags()).basicRemove(otherEnd, msgs);
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				return basicSetWorkspace(null, msgs);
 			case PropertiesPackage.PROJECT__STATS:
 				return basicSetStats(null, msgs);
 		}
 		return super.eInverseRemove(otherEnd, featureID, msgs);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	public NotificationChain eBasicRemoveFromContainerFeature(NotificationChain msgs) {
 		switch (eContainerFeatureID()) {
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				return eInternalContainer().eInverseRemove(this, PropertiesPackage.WORKSPACE__PROJECTS, Workspace.class, msgs);
 		}
 		return super.eBasicRemoveFromContainerFeature(msgs);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	public Object eGet(int featureID, boolean resolve, boolean coreType) {
 		switch (featureID) {
 			case PropertiesPackage.PROJECT__NAME:
 				return getName();
 			case PropertiesPackage.PROJECT__PROPERTY_BAGS:
 				return getPropertyBags();
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				return getWorkspace();
 			case PropertiesPackage.PROJECT__BASE:
 				return getBase();
 			case PropertiesPackage.PROJECT__STATS:
 				return getStats();
 		}
 		return super.eGet(featureID, resolve, coreType);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@SuppressWarnings("unchecked")
 	@Override
 	public void eSet(int featureID, Object newValue) {
 		switch (featureID) {
 			case PropertiesPackage.PROJECT__NAME:
 				setName((String)newValue);
 				return;
 			case PropertiesPackage.PROJECT__PROPERTY_BAGS:
 				getPropertyBags().clear();
 				getPropertyBags().addAll((Collection<? extends PropertyBag>)newValue);
 				return;
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				setWorkspace((Workspace)newValue);
 				return;
 			case PropertiesPackage.PROJECT__STATS:
 				setStats((ProjectStats)newValue);
 				return;
 		}
 		super.eSet(featureID, newValue);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	public void eUnset(int featureID) {
 		switch (featureID) {
 			case PropertiesPackage.PROJECT__NAME:
 				setName(NAME_EDEFAULT);
 				return;
 			case PropertiesPackage.PROJECT__PROPERTY_BAGS:
 				getPropertyBags().clear();
 				return;
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				setWorkspace((Workspace)null);
 				return;
 			case PropertiesPackage.PROJECT__STATS:
 				setStats((ProjectStats)null);
 				return;
 		}
 		super.eUnset(featureID);
 	}
 
 	/**
 	 * <!-- begin-user-doc -->
 	 * <!-- end-user-doc -->
 	 * @generated
 	 */
 	@Override
 	public boolean eIsSet(int featureID) {
 		switch (featureID) {
 			case PropertiesPackage.PROJECT__NAME:
 				return NAME_EDEFAULT == null ? getName() != null : !NAME_EDEFAULT.equals(getName());
 			case PropertiesPackage.PROJECT__PROPERTY_BAGS:
 				return !getPropertyBags().isEmpty();
 			case PropertiesPackage.PROJECT__WORKSPACE:
 				return getWorkspace() != null;
 			case PropertiesPackage.PROJECT__BASE:
 				return BASE_EDEFAULT == null ? getBase() != null : !BASE_EDEFAULT.equals(getBase());
 			case PropertiesPackage.PROJECT__STATS:
 				return getStats() != null;
 		}
 		return super.eIsSet(featureID);
 	}
 
 	class FileAcceptor implements PropertyFileAcceptor
 	{
 
 		@Override
 		public void newMatch(File file) {
 			PropertyBag propertyBag = PropertiesFactory.eINSTANCE.createPropertyBag();
 			PropertyFileDescriptor descriptor = PropertiesFactory.eINSTANCE.createPropertyFileDescriptor();
 			descriptor.setName(file.getName());
 			propertyBag.getDescriptors().add(descriptor);
 			String absolutePath = file.getParentFile().getAbsolutePath();
 			URI bagURI = URI.createFileURI(absolutePath);
 			bagURI = bagURI.deresolve(getBase());
 			propertyBag.setPath(bagURI);
 			Pattern pattern = buildPatternFrom(file);
 			File folder = file.getParentFile();
 			String[] childNames = folder.list();
 			for (String child : childNames) {
 				if(child.equals(file.getName()))
 					continue;
 				Matcher matcher = pattern.matcher(child);
 				if(matcher.matches())
 				{
 					PropertyFileDescriptor fileDescriptor = PropertiesFactory.eINSTANCE.createPropertyFileDescriptor();
 					fileDescriptor.setBag(propertyBag);
 					fileDescriptor.setName(child);
 					Locale locale = createVariant(matcher.group(1).substring(1));
 					fileDescriptor.setVariant(locale);
 				}
 			}
 			getPropertyBags().add(propertyBag);			
 		}
 
 		private Locale createVariant(String localeString) {
 			return (Locale) PropertiesFactory.eINSTANCE.createFromString(PropertiesPackage.Literals.LOCALE, localeString);
 		}
 
 		private Pattern buildPatternFrom(File file) {
 			int separator = file.getName().lastIndexOf(".");
 			String prefix = file.getName().substring(0,separator);
 			String suffix = file.getName().substring(separator);
 			return Pattern.compile(Pattern.quote(prefix) + "((_\\w\\w){1,3})"+Pattern.quote(suffix)); //messages.properties => messages_de_DE.properties
 		}
 		
 	}
 
 } //ProjectImpl
