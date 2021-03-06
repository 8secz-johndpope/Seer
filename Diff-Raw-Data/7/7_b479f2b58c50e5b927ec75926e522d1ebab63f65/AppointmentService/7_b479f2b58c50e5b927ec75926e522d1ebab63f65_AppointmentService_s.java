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
 package org.openmrs.module.appointment.api;
 
 import java.util.Date;
 import java.util.List;
 import java.util.Set;
 
 import org.openmrs.Patient;
 import org.openmrs.Visit;
 import org.openmrs.api.APIException;
 import org.openmrs.api.OpenmrsService;
 import org.openmrs.module.appointment.Appointment;
 import org.openmrs.module.appointment.AppointmentBlock;
 import org.openmrs.module.appointment.AppointmentStatusHistory;
 import org.openmrs.module.appointment.AppointmentType;
 import org.openmrs.module.appointment.TimeSlot;
 import org.springframework.transaction.annotation.Transactional;
 
 /**
  * This service exposes module's core functionality. It is a Spring managed bean which is configured
  * in moduleApplicationContext.xml.
  * <p>
  * It can be accessed only via Context:<br>
  * <code>
  * Context.getService(AppointmentService.class).someMethod();
  * </code>
  * 
  * @see org.openmrs.api.context.Context
  */
 @Transactional
 public interface AppointmentService extends OpenmrsService {
 	
 	/**
 	 * Gets all appointment types.
 	 * 
 	 * @return a list of appointment type objects.
 	 * @should get all appointment types
 	 */
 	@Transactional(readOnly = true)
 	Set<AppointmentType> getAllAppointmentTypes();
 	
 	/**
 	 * Get all appointment types based on includeRetired flag
 	 * 
 	 * @param includeRetired
 	 * @return List of all appointment types
 	 * @should get all appointment types based on include retired flag.
 	 */
 	@Transactional(readOnly = true)
 	public List<AppointmentType> getAllAppointmentTypes(boolean includeRetired);
 	
 	/**
 	 * Gets an appointment type by its appointment type id.
 	 * 
 	 * @param appointmentTypeId the appointment type id.
 	 * @return the appointment type object found with the given id, else null.
 	 * @should get correct appointment type
 	 */
 	@Transactional(readOnly = true)
 	AppointmentType getAppointmentType(Integer appointmentTypeId);
 	
 	/**
 	 * Gets an appointment type by its UUID.
 	 * 
 	 * @param uuid the appointment type UUID.
 	 * @return the appointment type object found with the given uuid, else null.
 	 * @should get correct appointment type
 	 */
 	@Transactional(readOnly = true)
 	AppointmentType getAppointmentTypeByUuid(String uuid);
 	
 	/**
 	 * Gets all appointment types whose names are similar to or contain the given search phrase.
 	 * 
 	 * @param fuzzySearchPhrase the search phrase to use.
 	 * @return a list of all appointment types with names similar to or containing the given phrase
 	 * @should get correct appointment types
 	 */
 	@Transactional(readOnly = true)
 	List<AppointmentType> getAppointmentTypes(String fuzzySearchPhrase);
 	
 	/**
 	 * Creates or updates the given appointment type in the database.
 	 * 
 	 * @param appointmentType the appointment type to create or update.
 	 * @return the created or updated appointment type.
 	 * @should save new appointment type
 	 * @should save edited appointment type
 	 * @should throw error when name is null
 	 * @should throw error when name is empty string
 	 */
 	AppointmentType saveAppointmentType(AppointmentType appointmentType) throws APIException;
 	
 	/**
 	 * Retires a given appointment type.
 	 * 
 	 * @param appointmentType the appointment type to retire.
 	 * @param reason the reason why the appointment type is retired.
 	 * @return the appointment type that has been retired.
 	 * @should retire given appointment type
 	 */
 	AppointmentType retireAppointmentType(AppointmentType appointmentType, String reason);
 	
 	/**
 	 * Unretires an appointment type.
 	 * 
 	 * @param appointmentType the appointment type to unretire.
 	 * @return the unretired appointment type
 	 * @should unretire given appointment type
 	 */
 	AppointmentType unretireAppointmentType(AppointmentType appointmentType);
 	
 	/**
 	 * Completely removes an appointment type from the database. This is not reversible.
 	 * 
 	 * @param appointmentType the appointment type to delete from the database.
 	 * @should delete given appointment type
 	 */
 	void purgeAppointmentType(AppointmentType appointmentType);
 	
 	//AppointmentBlock	
 	/**
 	 * Gets all appointment blocks.
 	 * 
 	 * @return a list of appointment block objects.
 	 * @should get all appointment blocks
 	 */
 	@Transactional(readOnly = true)
 	List<AppointmentBlock> getAllAppointmentBlocks();
 	
 	/**
 	 * Get all appointment blocks based on includeVoided flag
 	 * 
 	 * @param includeVoided
 	 * @return List of all appointment blocks
 	 * @should get all appointment blocks based on include voided flag.
 	 */
 	@Transactional(readOnly = true)
 	public List<AppointmentBlock> getAllAppointmentBlocks(boolean includeVoided);
 	
 	/**
 	 * Gets an appointment block by its appointment block id.
 	 * 
 	 * @param appointmentBlockId the appointment block id.
 	 * @return the appointment block object found with the given id, else null.
 	 * @should get correct appointment block
 	 */
 	@Transactional(readOnly = true)
 	AppointmentBlock getAppointmentBlock(Integer appointmentBlockId);
 	
 	/**
 	 * Gets an appointment block by its UUID.
 	 * 
 	 * @param uuid the appointment block UUID.
 	 * @return the appointment block object found with the given uuid, else null.
 	 * @should get correct appointment block
 	 */
 	@Transactional(readOnly = true)
 	AppointmentBlock getAppointmentBlockByUuid(String uuid);
 	
 	/**
 	 * Creates or updates the given appointment block in the database.
 	 * 
 	 * @param appointmentBlock the appointment block to create or update.
 	 * @return the created or updated appointment block.
 	 * @should save new appointment block
 	 * @should save edited appointment block
 	 * @should throw error when name is null
 	 * @should throw error when name is empty string
 	 */
 	AppointmentBlock saveAppointmentBlock(AppointmentBlock appointmentBlock) throws APIException;
 	
 	/**
 	 * Voids a given appointment block.
 	 * 
 	 * @param appointmentBlock the appointment block to void.
 	 * @param reason the reason why the appointment block is voided.
 	 * @return the appointment block that has been voided.
 	 * @should void given appointment block
 	 */
 	AppointmentBlock voidAppointmentBlock(AppointmentBlock appointmentBlock, String reason);
 	
 	/**
 	 * Unvoids an appointment block.
 	 * 
 	 * @param appointmentBlock the appointment block to unvoid.
 	 * @return the unvoided appointment block
 	 * @should unvoided given appointment block
 	 */
 	AppointmentBlock unvoidAppointmentBlock(AppointmentBlock appointmentBlock);
 	
 	/**
 	 * Completely removes an appointment block from the database. This is not reversible.
 	 * 
 	 * @param appointmentBlock the appointment block to delete from the database.
 	 * @should delete given appointment block
 	 */
 	void purgeAppointmentBlock(AppointmentBlock appointmentBlock);
 	
 	/**
 	 * Gets appointment blocks which have a given date and location.
 	 * 
 	 * @return a list of appointment block objects.
 	 * @should get all appointment blocks which have a given date and location.
 	 */
 	@Transactional(readOnly = true)
 	List<AppointmentBlock> getAppointmentBlocks(Date fromDate, Date toDate, Location location);
 	
 	//Appointment
 	/**
 	 * Gets all appointments.
 	 * 
 	 * @return a list of appointment objects.
 	 * @should get all appointment
 	 */
 	@Transactional(readOnly = true)
 	List<Appointment> getAllAppointments();
 	
 	/**
 	 * Get all appointments based on includeVoided flag
 	 * 
 	 * @param includeVoided
 	 * @return List of all appointments
 	 * @should get all appointments based on include voided flag.
 	 */
 	@Transactional(readOnly = true)
 	public List<Appointment> getAllAppointments(boolean includeVoided);
 	
 	/**
 	 * Gets an appointment by its appointment id.
 	 * 
 	 * @param appointmentId the appointment id.
 	 * @return the appointment object found with the given id, else null.
 	 * @should get correct appointment
 	 */
 	@Transactional(readOnly = true)
 	Appointment getAppointment(Integer appointmentId);
 	
 	/**
 	 * Gets an appointment by its UUID.
 	 * 
 	 * @param uuid the appointment UUID.
 	 * @return the appointment object found with the given uuid, else null.
 	 * @should get correct appointment
 	 */
 	@Transactional(readOnly = true)
 	Appointment getAppointmentByUuid(String uuid);
 	
 	/**
 	 * Creates or updates the given appointment in the database.
 	 * 
 	 * @param appointment the appointment to create or update.
 	 * @return the created or updated appointment.
 	 * @should save new appointment
 	 * @should save edited appointment
 	 */
 	Appointment saveAppointment(Appointment appointment) throws APIException;
 	
 	/**
 	 * Voids a given appointment.
 	 * 
 	 * @param appointment the appointment to void.
 	 * @param reason the reason why the appointment is voided.
 	 * @return the appointment that has been voided.
 	 * @should void given appointment
 	 */
 	Appointment voidAppointment(Appointment appointment, String reason);
 	
 	/**
 	 * Unvoids an appointment.
 	 * 
 	 * @param appointment the appointment to unvoid.
 	 * @return the unvoid appointment
 	 * @should unvoid given appointment
 	 */
 	Appointment unvoidAppointment(Appointment appointment);
 	
 	/**
 	 * Completely removes an appointment from the database. This is not reversible.
 	 * 
 	 * @param appointment the appointment to delete from the database.
 	 * @should delete given appointment
 	 */
 	void purgeAppointment(Appointment appointment);
 	
 	/**
 	 * Returns all Appointments for a given Patient
 	 * 
 	 * @param patientId the patient id to search by.
 	 * @return all the appointments for the given patient id.
 	 * @should return all of the appointments for the given patient.
 	 */
 	List<Appointment> getAppointmentsOfPatient(Patient patient);
 	
 	/**
 	 * Returns the appointment corresponding to the given visit.
 	 * 
 	 * @param visitId the visit id to search by.
 	 * @return the appointment that is related to this visit, null if there isnt any.
 	 */
 	Appointment getAppointmentByVisit(Visit visit);
 	
 	/*
 	 * TODO: add status to appointment.
 	 */
 
 	//TimeSlot
 	
 	/**
 	 * Gets all time slots.
 	 * 
 	 * @return a list of time slot objects.
 	 * @should get all time slots
 	 */
 	@Transactional(readOnly = true)
 	List<TimeSlot> getAllTimeSlots();
 	
 	/**
 	 * Get all time slots based on includeVoided flag
 	 * 
 	 * @param includeVoided
 	 * @return List of all time slots
 	 * @should get all time slots based on include voided flag.
 	 */
 	@Transactional(readOnly = true)
 	public List<TimeSlot> getAllTimeSlots(boolean includeVoided);
 	
 	/**
 	 * Creates or updates the given time slot in the database.
 	 * 
 	 * @param timeSlot the time slot to create or update.
 	 * @return the created or updated time slot.
 	 * @should save new time slot
 	 * @should save edited time slot
 	 */
 	TimeSlot saveTimeSlot(TimeSlot timeSlot) throws APIException;
 	
 	/**
 	 * Gets a a time slot by its id.
 	 * 
 	 * @param timeSlotId the time slot id.
 	 * @return the time slot object found with the given id, else null.
 	 * @should get correct time slot
 	 */
 	@Transactional(readOnly = true)
 	TimeSlot getTimeSlot(Integer timeSlotId);
 	
 	/**
 	 * Gets a time slot by its UUID.
 	 * 
 	 * @param uuid the time slot UUID.
 	 * @return the time slot object found with the given uuid, else null.
 	 * @should get correct time slot
 	 */
 	@Transactional(readOnly = true)
 	TimeSlot getTimeSlotByUuid(String uuid);
 	
 	/**
 	 * Voids a given time slot.
 	 * 
 	 * @param timeSlot the time slot to void.
 	 * @param reason the reason why the time slot is voided.
 	 * @return the time slot that has been voided.
 	 * @should void given time slot
 	 */
 	TimeSlot voidTimeSlot(TimeSlot timeSlot, String reason);
 	
 	/**
 	 * Unvoids a time slot.
 	 * 
 	 * @param timeSlot the time slot to unvoid.
 	 * @return the unvoided time slot
 	 * @should unvoid given time slot
 	 */
 	TimeSlot unvoidTimeSlot(TimeSlot timeSlot);
 	
 	/**
 	 * Completely removes a time slot from the database. This is not reversible.
 	 * 
 	 * @param timeSlot the time slot to delete from the database.
 	 * @should delete given time slot
 	 */
 	void purgeTimeSlot(TimeSlot timeSlot);
 	
 	/**
 	 * Should retrieve all appointments in the given time slot.
 	 * 
 	 * @param timeSlot the time slot to search by.
 	 * @return the appointments in the given time slot.
 	 */
 	List<Appointment> getAppointmentsInTimeSlot(TimeSlot timeSlot);
 	
 	//Appointment Status History
 	/**
 	 * Gets all appointment status histories.
 	 * 
 	 * @return a list of appointment status history objects.
 	 * @should get all appointment status histories
 	 */
 	@Transactional(readOnly = true)
 	List<AppointmentStatusHistory> getAllAppointmentStatusHistories();
 	
 	/**
 	 * Gets an appointment status by its appointment status history id.
 	 * 
 	 * @param appointmentStatusHistoryId the appointment status history id.
 	 * @return the appointment status history object found with the given id, else null.
 	 * @should get correct appointment status history
 	 */
 	@Transactional(readOnly = true)
 	AppointmentStatusHistory getAppointmentStatusHistory(Integer appointmentStatusHistoryId);
 	
 	/**
 	 * Gets all appointment status histories whose statuses are similar to or contain the given
 	 * status.
 	 * 
 	 * @param status the search phrase to use.
 	 * @return a list of all appointment status histories with names identical to or containing the given status
 	 * @should get correct appointment status histories
 	 */
 	@Transactional(readOnly = true)
 	List<AppointmentStatusHistory> getAppointmentStatusHistories(String status);
 	
 	/**
 	 * Creates or updates the given appointment status history in the database.
 	 * 
 	 * @param AppointmentStatusHistory the appointment status history to create or update.
 	 * @return the created or updated appointment status history.
 	 * @should save new appointment status history
 	 * @should save edited appointment status history
 	 */
 	AppointmentStatusHistory saveAppointmentStatusHistory(AppointmentStatusHistory appointmentStatusHistory)
 	        throws APIException;
 	
 	/**
 	 * 
 	 * Retrieves the most recent appointment for a given patient.
 	 * 
 	 * @param patient the patient for which we are retrieving.
 	 * @return The most recent appointment for the given patient, null if no appointments were set.
 	 */
 	@Transactional(readOnly = true)
 	Appointment getLastAppointment(Patient patient);
 }
