 package br.gov.frameworkdemoiselle.fuselage.view.edit;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.inject.Inject;
 
 import br.gov.frameworkdemoiselle.fuselage.business.ProfileBC;
 import br.gov.frameworkdemoiselle.fuselage.domain.SecurityProfile;
 import br.gov.frameworkdemoiselle.fuselage.domain.SecurityResource;
 import br.gov.frameworkdemoiselle.fuselage.domain.SecurityRole;
 import br.gov.frameworkdemoiselle.message.SeverityType;
 import br.gov.frameworkdemoiselle.stereotype.ViewController;
 import br.gov.frameworkdemoiselle.template.contrib.AbstractEditPageBean;
 import br.gov.frameworkdemoiselle.util.contrib.Faces;
 
 @ViewController
 public class ProfileEditMB extends AbstractEditPageBean<SecurityProfile, Long> {
 
 	private static final long serialVersionUID = 1L;
 
 	@Inject
 	private ProfileBC bc;
 
 	private List<SecurityRole> roles;
 
 	private List<SecurityRole> selectedRoles = new ArrayList<SecurityRole>();
 
 	@Override
 	public String insert() {
 		try {
 			bc.insert(getBean());
 			Faces.addI18nMessage("fuselage.profile.insert.success", getBean().getName());
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.profile.insert.failed", SeverityType.ERROR);
 		}
 		return null;
 	}
 
 	@Override
 	public String update() {
 		try {
 			bc.update(getBean());
 			Faces.addI18nMessage("fuselage.profile.update.success", getBean().getName());
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.profile.update.failed", SeverityType.ERROR);
 		}
 		return null;
 	}
 
 	@Override
 	public String delete() {
 		try {
 			bc.delete(getBean().getId());
 			Faces.addI18nMessage("fuselage.profile.delete.success", getBean().getName());
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.profile.delete.failed", SeverityType.ERROR);
 		}
 		return null;
 	}
 
 	@Override
 	public SecurityProfile load(Long id) {
 		try {
 			return bc.load(id);
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.profile.load.failed", SeverityType.ERROR);
 		}
 		return new SecurityProfile();
 	}
 
 	public List<Long> getResourcePriorities() {
 		List<Long> priorities = new ArrayList<Long>();
 		try {
 			List<Long> usedPriorities = bc.getUsedPrioritiesExceptMyself(getBean());
 			for (int i = 1; i < 100; i++)
 				if (!usedPriorities.contains(new Long(i)))
 					priorities.add(new Long(i));
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.generic.business.error", SeverityType.ERROR);
 		}
 		return priorities;
 	}
 
 	/**
 	 * Get all SecurityResources for selectOneMenu to select welcome page
 	 * 
 	 * @return list of all SecurityResources
 	 */
 	public List<SecurityResource> getResourceList() {
 		try {
 			return bc.getResources();
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.generic.business.error", SeverityType.ERROR);
 		}
 		return new ArrayList<SecurityResource>();
 	}
 
 	/**
 	 * Get all roles except already in bean
 	 */
 	public List<SecurityRole> getRoleList() {
 		try {
 			if (roles == null)
 				roles = bc.getRolesExceptList(getBean().getRoles());
 			return roles;
 		} catch (RuntimeException e) {
 			Faces.validationFailed();
 			Faces.addI18nMessage("fuselage.generic.business.error", SeverityType.ERROR);
 		}
 		return new ArrayList<SecurityRole>();
 	}
 
 	public void clearRoleList() {
 		roles = null;
 		selectedRoles.clear();
 	}
 
 	public void unselectRole(SecurityRole securityRole) {
 		getBean().getRoles().remove(securityRole);
 	}
 
 	public void selectRoles() {
 		if (getBean().getRoles() == null)
 			getBean().setRoles(new ArrayList<SecurityRole>(selectedRoles));
 		else
 			getBean().getRoles().addAll(selectedRoles);
 	}
 
 	/**
 	 * Get SecurityRoles from current bean as array for datatable selection
 	 * 
 	 * @return array of bean SecurityRoles
 	 */
 	public SecurityRole[] getRoleArray() {
 		return selectedRoles.toArray(new SecurityRole[0]);
 	}
 
 	/**
 	 * Set SecurityRoles on current bean from datatable selection array
 	 * 
	 * @param selectedRoles
 	 *            array of SecurityRoles to set current bean
 	 */
 	public void setRoleArray(SecurityRole[] selectedRolesArray) {
 		for (SecurityRole role : selectedRolesArray)
 			if (!selectedRoles.contains(role))
 				selectedRoles.add(role);
 	}
 
 }
