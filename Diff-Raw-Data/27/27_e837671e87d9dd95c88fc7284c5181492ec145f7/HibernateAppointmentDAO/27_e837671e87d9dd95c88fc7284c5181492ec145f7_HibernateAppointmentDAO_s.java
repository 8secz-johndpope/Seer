 /**
  * The contents of this file are subject to the OpenMRS Public License
  * Version 1.0 (the "License"); you may not use this file except in
  * compliance with the License. You may obtain a copy of the License at
  * http://license.openmrs.org
  *
  * Software distributed under the License is distributed on an "AS IS"
  * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
  * License for the specific language governing rights and limitations
  * under the License.
  *
  * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
  */
 package org.openmrs.module.appointment.api.db.hibernate;
 
 import java.util.List;
 
 import org.hibernate.criterion.Restrictions;
 import org.openmrs.module.appointment.Appointment;
 import org.openmrs.module.appointment.api.db.AppointmentDAO;
 import org.springframework.transaction.annotation.Transactional;
 
 public class HibernateAppointmentDAO extends HibernateSingleClassDAO implements AppointmentDAO {
 	
 	public HibernateAppointmentDAO() {
 		super(Appointment.class);
 	}
 	
 	@Override
 	@Transactional(readOnly = true)
 	public List<Appointment> getAppointmentsByPatient(Integer patientId) {
 		return super.sessionFactory.getCurrentSession().createCriteria(Appointment.class)
		        .add(Restrictions.like("patient_id", patientId)).list();
 	}
 	
 	@Override
 	public Appointment getAppointmentByVisit(Integer visitId) {
		return (Appointment)super.sessionFactory.getCurrentSession().createQuery("from " + mappedClass.getSimpleName() + " where at.visit_id = :visitId").setParameter(0, visitId).uniqueResult();
 	}
 }
