 package gr.sch.ira.minoas.seam.components.home;
 
 import gr.sch.ira.minoas.model.employee.Employee;
 import gr.sch.ira.minoas.model.employee.EmployeeInfo;
 import gr.sch.ira.minoas.model.employee.EmployeeType;
 import gr.sch.ira.minoas.model.employee.RegularEmployeeInfo;
 import gr.sch.ira.minoas.model.employement.Employment;
 import gr.sch.ira.minoas.model.employement.EmploymentType;
 import gr.sch.ira.minoas.model.employement.NonRegularEmploymentInfo;
 import gr.sch.ira.minoas.model.employement.Secondment;
 import gr.sch.ira.minoas.model.employement.ServiceAllocation;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 
 import org.jboss.seam.ScopeType;
 import org.jboss.seam.annotations.Factory;
 import org.jboss.seam.annotations.In;
 import org.jboss.seam.annotations.Name;
 import org.jboss.seam.annotations.Scope;
 import org.jboss.seam.annotations.Transactional;
 import org.jboss.seam.annotations.datamodel.DataModel;
 import org.jboss.seam.annotations.security.Restrict;
 import org.jboss.seam.international.StatusMessage.Severity;
 
 /**
  * @author <a href="mailto:filippos@slavik.gr">Filippos Slavik</a>
  * @version $Id$
  */
 @Name("employeeHome")
 @Scope(ScopeType.CONVERSATION)
 public class EmployeeHome extends MinoasEntityHome<Employee> {
 
 	/**
 	 * Comment for <code>serialVersionUID</code>
 	 */
 	private static final long serialVersionUID = 1L;
 
 	@In(create = true)
 	private NonRegularEmploymentInfoHome nonRegularEmploymentInfoHome;
 
 	@In(create = true)
 	private EmploymentHome employmentHome;
 	
 	@In(create = true)
 	private EmployeeInfoHome employeeInfoHome;
 	
 	@DataModel(value = "hourlyBasedEmployments")
 	private Collection<Employment> hourlyBasedEmployments = new ArrayList<Employment>();
 
 	@In(create = true)
 	private RegularEmployeeInfoHome regularEmployeeInfoHome;
 
 	@In(create = true)
 	private SecondmentHome secondmentHome;
 
 	@In(create = true)
 	private ServiceAllocationHome serviceAllocationHome;
 
 	
 	protected String tempValueHolder1; /* used as a holder value in forms */ 
 	
 	
 	
 
 	@Transactional
 	public String addNewEmployeeInLocalPYSDE() {
 
 		if (isManaged() || employmentHome.isManaged() || regularEmployeeInfoHome.isManaged()
 				|| nonRegularEmploymentInfoHome.isManaged()) {
 			throw new RuntimeException(
 					"employee home or employment home or employeeRegularInfo or nonRegularEmploymentInfoHome is managed.");
 		}
 
 		RegularEmployeeInfo info = null;
 		NonRegularEmploymentInfo nonRegularEmploymentInfo = null;
 		Employee new_employee = getInstance();
 		Employment employment = employmentHome.getInstance();
 		EmployeeInfo eInfo = employeeInfoHome.getInstance(); 
 
 		Employee test_employee = getCoreSearching().getEmployeeOfTypeByVatNumber(getEntityManager(),
 				new_employee.getType(), new_employee.getVatNumber());
 		if (test_employee != null) {
 			getFacesMessages().add(
 					Severity.ERROR,
 					"Το ΑΦΜ '" + new_employee.getVatNumber()
 							+ "' που εισάγατε είναι ήδη σε χρήση απο τον εκπαιδευτικό '"
 							+ test_employee.toPrettyString()+"').");
 			return null;
 		}
 
 		new_employee.setActive(Boolean.TRUE);
 		new_employee.setCurrentPYSDE(getCoreSearching().getLocalPYSDE(getEntityManager()));
 		switch (new_employee.getType()) {
 		case DEPUTY:
 			employment.setType(EmploymentType.DEPUTY);
 			break;
 		case HOURLYPAID:
 			employment.setType(EmploymentType.HOURLYBASED);
 			break;
 		case REGULAR:
 			employment.setType(EmploymentType.REGULAR);
 			break;
 		}
 
 		getEntityManager().persist(new_employee);
 		
 		
 		//
 		//	Add an EmployeeInfo row in the database for all types of Employees
 		//	EmployeeInfo will be later filled using the 'Διαχείριση Στοιχείων Μισθοδοσίας' UI
 		//
 		eInfo.setEmployee(new_employee);
 		eInfo.setInsertedBy(getPrincipal());
 		eInfo.setInsertedOn(new Date());
 		getEntityManager().persist(eInfo);
 		new_employee.setEmployeeInfo(eInfo);
 		
 		employment.setEmployee(new_employee);
 		employment.setActive(Boolean.TRUE);
 		employment.setInsertedBy(getPrincipal());
 		employment.setSchoolYear(getCoreSearching().getActiveSchoolYear(getEntityManager()));
 		employment.setSpecialization(new_employee.getLastSpecialization());
 
 		if (new_employee.getType() == EmployeeType.REGULAR) {
 			new_employee.setCurrentEmployment(employment);
 			getEntityManager().persist(employment);
 
 			info = regularEmployeeInfoHome.getInstance();
 			info.setInsertedBy(getPrincipal());
 			info.setEmployee(new_employee);
 			new_employee.setRegularDetail(info);
 			getEntityManager().persist(info);
 		}
 
 		if (new_employee.getType() == EmployeeType.DEPUTY) {
 			nonRegularEmploymentInfo = nonRegularEmploymentInfoHome.getInstance();
 			nonRegularEmploymentInfo.setEmployment(employment);
 			nonRegularEmploymentInfo.setInsertedBy(getPrincipal());
 			getEntityManager().persist(nonRegularEmploymentInfo);
 			
			employment.setNonRegularEmploymentInfo(nonRegularEmploymentInfo);
			
 			new_employee.setCurrentEmployment(employment);
 			getEntityManager().persist(employment);
 
 
 		}
 
 		if (new_employee.getType() == EmployeeType.HOURLYPAID) {
 			for (Employment hemployment : getHourlyBasedEmployments()) {
 				//
 				//	Copy Mandatory working hours to Final working hours
 				//	This will be deprecated in the future
 				//
 				hemployment.setFinalWorkingHours(hemployment.getMandatoryWorkingHours());
 				getEntityManager().persist(hemployment.getNonRegularEmploymentInfo());
 				
 				hemployment.setEmployee(new_employee);
 				hemployment.setActive(Boolean.TRUE);
 				hemployment.setInsertedBy(getPrincipal());
 				hemployment.setSchoolYear(getCoreSearching().getActiveSchoolYear(getEntityManager()));
 				hemployment.setSpecialization(new_employee.getLastSpecialization());
 				new_employee.setCurrentEmployment(hemployment);
 				getEntityManager().persist(hemployment);
 			}
 		}
 		return persist();
 	}
 
 	public void addNewHourlyBasedEmploymentItem() {
 		Employment e = new Employment();
 		e.setEmployee(getInstance());
 		e.setActive(Boolean.TRUE);
 		e.setSchoolYear(getCoreSearching().getActiveSchoolYear(getEntityManager()));
 		e.setInsertedBy(getPrincipal());
 		e.setType(EmploymentType.HOURLYBASED);
 		e.setEstablished(new Date());
 		e.setFinalWorkingHours(11);
 		//e.setMandatoryWorkingHours(21);
 		
 		NonRegularEmploymentInfo nrei = new NonRegularEmploymentInfo();
 		nrei.setInsertedBy(getPrincipal());
 		
 		e.setNonRegularEmploymentInfo(nrei);
 		
 		getHourlyBasedEmployments().add(e);
 	}
 
 	/**
 	 * @see org.jboss.seam.framework.Home#createInstance()
 	 */
 	@Override
 	protected Employee createInstance() {
 		Employee new_instance = new Employee();
 		new_instance.setType(EmployeeType.REGULAR);
 		return new_instance;
 	}
 
 	/**
 	 * @return the nonRegularEmploymentInfoHome
 	 */
 	public NonRegularEmploymentInfoHome getNonRegularEmploymentInfoHome() {
 		return nonRegularEmploymentInfoHome;
 	}
 
 	/**
 	 * @return the hourlyBasedEmployments
 	 */
 	public Collection<Employment> getHourlyBasedEmployments() {
 		return hourlyBasedEmployments;
 	}
 
 	/**
 	 * @see org.jboss.seam.framework.Home#getInstance()
 	 */
 	@Override
 	@Factory(value = "employee", scope = ScopeType.PAGE)
 	public Employee getInstance() {
 		return (Employee) super.getInstance();
 
 	}
 
 	/**
 	 * @return the regularEmployeeInfoHome
 	 */
 	public RegularEmployeeInfoHome getRegularEmployeeInfoHome() {
 		return regularEmployeeInfoHome;
 	}
 
 	public boolean hasDeputyEmployment() {
 		return hasEmployment() && getInstance().getCurrentEmployment().getType().equals(EmploymentType.DEPUTY);
 	}
 
 	public boolean hasEmployment() {
 		return getInstance().getCurrentEmployment() != null;
 	}
 
 	public boolean hasRegularEmployment() {
 		return hasEmployment() && getInstance().getCurrentEmployment().getType().equals(EmploymentType.REGULAR);
 	}
 
 	/**
 	 * @see gr.sch.ira.minoas.seam.components.home.MinoasEntityHome#persist()
 	 */
 	@Override
 	@Transactional
 	public String persist() {
 		Employee employee = getInstance();
 		employee.setInsertedBy(getPrincipal());
 		return super.persist();
 	}
 
 	public void prepareForNewEmployee() {
 		this.clearInstance();
 		hourlyBasedEmployments.clear();
 		regularEmployeeInfoHome.clearInstance();
 		employmentHome.clearInstance();
 		nonRegularEmploymentInfoHome.clearInstance();
 		employmentHome.getInstance().setEstablished(
 				getCoreSearching().getActiveSchoolYear(getEntityManager()).getSchoolYearStart());
 		employmentHome.getInstance().setFinalWorkingHours(21);
 		employmentHome.getInstance().setMandatoryWorkingHours(21);
 		//employmentHome.getInstance().setNonRegularEmploymentInfo(nonRegularEmploymentInfoHome.getInstance());
 		addNewHourlyBasedEmploymentItem();
 	}
 
     
 	/**
      * @see gr.sch.ira.minoas.seam.components.home.MinoasEntityHome#clearInstance()
      */
     @Override
     public void clearInstance() {
         super.clearInstance();
         setTempValueHolder1(null);
     }
 	
 	
 	/**
 	 * @see gr.sch.ira.minoas.seam.components.home.MinoasEntityHome#remove()
 	 */
 	@Override
 	@Transactional
 	@Restrict("#{s:hasRole('ADMIN')}")
 	public String remove() {
 		if (isManaged()) {
 			Employee employee = getInstance();
 			info("principal #0 is trying to remove employee #1", getPrincipalName(), employee);
 			if (employee.getType() == EmployeeType.REGULAR) {
 				getEntityManager().remove(employee.getRegularDetail());
 			}
 			
 			getEntityManager().remove(employee.getEmployeeInfo());
 			return super.remove();
 		}
 		return null;
 	}
 
 	public void removeNewHourlyBasedEmploymentItem(Employment newEmployment) {
 		getHourlyBasedEmployments().remove(newEmployment);
 	}
 
 	/**
 	 * @param nonRegularEmploymentInfoHome the nonRegularEmploymentInfoHome to set
 	 */
 	public void setNonRegularEmploymentInfoHome(NonRegularEmploymentInfoHome nonRegularEmploymentInfoHome) {
 		this.nonRegularEmploymentInfoHome = nonRegularEmploymentInfoHome;
 	}
 
 	/**
 	 * @param hourlyBasedEmployments the hourlyBasedEmployments to set
 	 */
 	public void setHourlyBasedEmployments(Collection<Employment> hourlyBasedEmployments) {
 		this.hourlyBasedEmployments = hourlyBasedEmployments;
 	}
 
 	/**
 	 * @param regularEmployeeInfoHome
 	 *            the regularEmployeeInfoHome to set
 	 */
 	public void setRegularEmployeeInfoHome(RegularEmployeeInfoHome regularEmployeeInfoHome) {
 		this.regularEmployeeInfoHome = regularEmployeeInfoHome;
 	}
 
 	/**
 	 * @see gr.sch.ira.minoas.seam.components.home.MinoasEntityHome#update()
 	 */
 	@Override
 	@Transactional
 	public String update() {
 		return super.update();
 	}
 	
 	@Transactional
     public String revert() {
         info("principal #0 is reverting updates to employee #1", getPrincipalName(), getInstance());
         getEntityManager().refresh(getInstance());
         return "reverted";
     }
 
 	@Transactional
 	public boolean wire() {
 		if (!secondmentHome.isManaged()) {
 			Employee employee = getInstance();
 			Secondment newSecondment = secondmentHome.getInstance();
 			Employment currentEmployment = employee.getCurrentEmployment();
 			if (currentEmployment != null) {
 				newSecondment.setSourceUnit(currentEmployment.getSchool());
 				newSecondment.setMandatoryWorkingHours(currentEmployment.getMandatoryWorkingHours());
 				newSecondment.setFinalWorkingHours(currentEmployment.getFinalWorkingHours());
 			} else {
 				newSecondment.setSourceUnit(employee.getCurrentPYSDE().getRepresentedByUnit());
 			}
 		}
 		if (!serviceAllocationHome.isManaged()) {
 			Employee employee = getInstance();
 			ServiceAllocation newServiceAllocation = serviceAllocationHome.getInstance();
 			Employment currentEmployment = employee != null ? employee.getCurrentEmployment() : null;
 			if (currentEmployment != null) {
 				newServiceAllocation.setSourceUnit(currentEmployment.getSchool());
 			} else {
 				newServiceAllocation.setSourceUnit(employee.getCurrentPYSDE().getRepresentedByUnit());
 			}
 		}
 		return true;
 	}
 	
 	public Boolean isInExclusionList() {
 		return getInstance().getExclusion()!=null;
 	}
 	
 	public Boolean isRegularEmployee() {
         return getInstance().isRegularEmployee();
     }
     
     public Boolean isDeputyEmployee() {
        return getInstance().isDeputyEmployee();
     }
     
     public Boolean isHourlyPaidEmployee() {
         return getInstance().isHourlyPaidEmployee();
     }
 
     /**
      * @return the tempValueHolder1
      */
     public String getTempValueHolder1() {
         return tempValueHolder1;
     }
 
     /**
      * @param tempValueHolder1 the tempValueHolder1 to set
      */
     public void setTempValueHolder1(String tempValueHolder1) {
         this.tempValueHolder1 = tempValueHolder1;
     }
 }
