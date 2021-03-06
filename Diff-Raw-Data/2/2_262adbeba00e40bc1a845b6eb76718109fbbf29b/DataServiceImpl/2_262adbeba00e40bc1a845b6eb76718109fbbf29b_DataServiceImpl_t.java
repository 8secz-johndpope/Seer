 package backend.DataService;
 
 import gui.TimeSlot;
 
 import java.io.IOException;
 import java.sql.Connection;
 import java.sql.Date;
 import java.sql.DriverManager;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.List;
 import java.util.logging.FileHandler;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.logging.SimpleFormatter;
 
 import backend.DataTransferObjects.AppointmentDto;
 import backend.DataTransferObjects.DayDto;
 import backend.DataTransferObjects.NoShowDto;
 import backend.DataTransferObjects.PatientDto;
 import backend.DataTransferObjects.PractitionerDto;
 import backend.DataTransferObjects.SchedulePractitionerDto;
 import backend.DataTransferObjects.TypeDto;
 import backend.DataTransferObjects.WaitlistDto;
 
 public class DataServiceImpl implements DataService {
 
 	// just for testing
 	public static void main(String[] args) {
 		//String url = "jdbc:mysql://localhost:3306/test";
 		String user = "testuser";
 		String password = "test623";
 
 		//DataService serv = DataServiceImpl.create("test", "192.168.0.13:3306", user, password);
 
 		//System.out.println(GLOBAL_DATA_INSTANCE.getFutureAppointmentsByPatId(1));
 		//		PatientDto newPatient = new PatientDto();
 		//		newPatient.setFirst("Dead").setLast("Bowie").setPhone(3215552314L).setNotes("ELE member");
 		//		serv.addPatient(newPatient);
 
 		//        serv.addNewPractitionerType("Homeopathy");
 		//        
 		//        PractitionerDto newPractitioner = new PractitionerDto();
 		//        newPractitioner.setApptLength(60);
 		//        newPractitioner.setFirst("Mitts");
 		//        newPractitioner.setLast("MaGee");
 		//        newPractitioner.setNotes("Not President");
 		//        newPractitioner.setPhone("123456789");
 		//        newPractitioner.setTypeID(1);
 		//        serv.addPractitioner(newPractitioner);
 
 		//for (PatientDto patient : serv.queryPatientByName("Dead", "Bowie")) {
 
 			//System.out.println(patient);
 		//}
 		//		TypeDto type = new TypeDto();
 		//		type.setField("TypeID", 1);
 		//		serv.addPatientToWaitlist(new PatientDto().setPatID(1), type);
 		//for (WaitlistDto entry : serv.getWaitlist()) {
 
 			//System.out.println(entry);
 			
 		//}
 		//System.out.println(serv.queryPatientByName("Dead", "Bowie").get(0));
 		//serv.close();
 	}
 
 	public static Logger lgr = Logger.getLogger(DataServiceImpl.class.getName());
 	public static DataService GLOBAL_DATA_INSTANCE = DataServiceImpl.create(
 			"ifc_db", "localhost:3306", "testuser", "test623");
 	
 	private final String url;
 	private final String user;
 	private final String password;
 
 	private Connection connection;
 
 	private DataServiceImpl(String url, String user, String password) {
 		// Instantiate this class with DataService.create(String dbName, String serverAddr);
 		this.url = url;
 		this.user = user;
 		this.password = password;
 	}
 
 	/**
 	 * Creates an instance of DataService connected to the specified database service.
 	 * Returns null if a connection cannot be made.
 	 * Don't forget to close the connection after you're done!
 	 * 
 	 * @param dbName example:test
 	 * @param serverAddr example:localhost:3306
 	 * @param username example:testuser
 	 * @param password example:test623
 	 * @return An instance of DataService with an active connection. null if can't connect
 	 */
 	public static DataService create(
 			String dbName, String serverAddr, String username, String password) {
 
 		DataServiceImpl service = new DataServiceImpl(
 				"jdbc:mysql://" + serverAddr + "/" + dbName, username, password);
 
 		try {
 			FileHandler fileHandler = new FileHandler("error.log", true);
 			fileHandler.setFormatter(new SimpleFormatter());
 			lgr.addHandler(fileHandler);
 
 			service.connection =
 				DriverManager.getConnection(service.url, service.user, service.password);
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 			return null;
 		} catch (SecurityException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 			return null;
 		} catch (IOException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 			return null;
 		}
 
 		GLOBAL_DATA_INSTANCE = service;
 		return service;
 	}
 
 	@Override
 	public void close() {
 		try {
 			if (connection != null) {
 				connection.close();
 			}
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, "DataService.close() failed.\n" + e.getMessage(), e);
 		}
 	}
 
 	@Override
 	public boolean addPatient(PatientDto patient) {
 		PreparedStatement st = null;
 
 		try {
 			if (patient.getPatID() == null) {
 				st = connection.prepareStatement(
 				"INSERT INTO Patient (FirstName, LastName, PhoneNumber, Notes, Active) VALUES (?, ?, ?, ?, 1)");
 			} else {
 				st = connection.prepareStatement(
 						"INSERT INTO Patient (FirstName, LastName, PhoneNumber, Notes, PatID, Active) " +
 				"VALUES (?, ?, ?, ?, ?, 1)");
 				st.setInt(5, patient.getPatID());
 			}
 			st.setString(1, patient.getFirst());
 			st.setString(2, patient.getLast());
 			st.setString(3, patient.getPhone());
 			st.setString(4, patient.getNotes());
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean updatePatient(String fieldName, Object value, PatientDto patient) {
 		PreparedStatement st = null;
 
 		try {
 			patient.setField(fieldName, value);
 			st = connection.prepareStatement(
 					"UPDATE Patient SET " + fieldName + "=? WHERE PatID=?");
 			st.setObject(1, value);
 			st.setInt(2, patient.getPatID());
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean removePatient(PatientDto patient) {
 		PreparedStatement st = null;
 
 		try {
 			if (patient.getPatID() == null) {
 				lgr.log(Level.WARNING, "Tried to delete patient without ID");
 				return false;
 			} else {
 				st = connection.prepareStatement(
 				"UPDATE Patient SET Patient.Active=0 WHERE PatID=?");
 				st.setInt(1, patient.getPatID());
 			}
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
         
         
 
 	@Override
 	public PatientDto getPatient(int PatID) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("Select Patient.PatID, " +
 					"Patient.FirstName, Patient.LastName, Patient.PhoneNumber, " +
 					"Patient.Notes, temp.NumberOfNoShows  From Patient LEFT JOIN " +
 					"(Select PatID, Count(NoShowID) as NumberOfNoShows from NoShow" +
 					" Group by PatID) as temp ON temp.PatID = Patient.PatID WHERE Patient.PatID = (?)");
 			st.setInt(1, PatID);
 			rs = st.executeQuery();
 			PatientDto patient = new PatientDto();
 			if (rs.next()) {
 				patient.setField(PatientDto.PATIENT_ID, rs.getInt(1));
 				patient.setField(PatientDto.FIRST, rs.getString(2));
 				patient.setField(PatientDto.LAST, rs.getString(3));
 				patient.setField(PatientDto.PHONE, rs.getString(4));
 				patient.setField(PatientDto.NOTES, rs.getString(5));
 				patient.setField(PatientDto.NO_SHOW, rs.getInt(6));
 				
 				return patient;
 			}
 
 			return null;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public List<PatientDto> getAllPatients() {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 
 			st = connection.prepareStatement("Select Patient.Active, Patient.PatID, Patient.FirstName, " +
 					"Patient.LastName, Patient.PhoneNumber, Patient.Notes, temp.NumberOfNoShows  " +
 					"From Patient LEFT JOIN (Select PatID, Count(NoShowID) as " +
 					"NumberOfNoShows from NoShow Group by PatID) as temp ON temp.PatID = Patient.PatID"); 
                        
 			rs = st.executeQuery();
 			List<PatientDto> results = new ArrayList<PatientDto>();
 			PatientDto patient = new PatientDto();
 			while (rs.next()) {
                             if(rs.getInt("Active") != 0){
 				patient.setField(PatientDto.PATIENT_ID, rs.getInt(PatientDto.PATIENT_ID));
 				patient.setField(PatientDto.FIRST, rs.getString(PatientDto.FIRST));
 				patient.setField(PatientDto.LAST, rs.getString(PatientDto.LAST));
 				patient.setField(PatientDto.PHONE, rs.getString(PatientDto.PHONE));
 				patient.setField(PatientDto.NOTES, rs.getString(PatientDto.NOTES));
 				//TODO set to 0 if null
 				patient.setField(PatientDto.NO_SHOW, rs.getInt(PatientDto.NO_SHOW));
 				results.add(patient);
 				patient = new PatientDto();
                             }
 			}
 			return results;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public List<PatientDto> queryPatientByName(String first, String last) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
                         if (first != null && !first.equals("")){
                             if (last != null && !last.equals("")){
                                 st = connection.prepareStatement("Select Patient.Active, Patient.PatID, Patient.FirstName, Patient.LastName, " +
 					"Patient.PhoneNumber, Patient.Notes, " +
 					"temp.NumberOfNoShows  From Patient LEFT JOIN " +
 					"(Select PatID, Count(NoShowID) as 	NumberOfNoShows from " +
 					"NoShow Group by PatID) as temp ON temp.PatID = Patient.PatID " +
 					"WHERE Patient.FirstName = ? AND Patient.LastName = ?");
                                 st.setString(1, first);
                                 st.setString(2, last);
                             }
                             else {
                                 st = connection.prepareStatement("Select Patient.Active, Patient.PatID, Patient.FirstName, Patient.LastName, " +
 					"Patient.PhoneNumber, Patient.Notes, " +
 					"temp.NumberOfNoShows  From Patient LEFT JOIN " +
 					"(Select PatID, Count(NoShowID) as 	NumberOfNoShows from " +
 					"NoShow Group by PatID) as temp ON temp.PatID = Patient.PatID " +
 					"WHERE Patient.FirstName = ?");
                                 st.setString(1, first);
                             }
                         }
                         else if (last != null && !last.equals("")){
                             st = connection.prepareStatement("Select Patient.Active, Patient.PatID, Patient.FirstName, Patient.LastName, " +
 					"Patient.PhoneNumber, Patient.Notes, " +
 					"temp.NumberOfNoShows  From Patient LEFT JOIN " +
 					"(Select PatID, Count(NoShowID) as 	NumberOfNoShows from " +
 					"NoShow Group by PatID) as temp ON temp.PatID = Patient.PatID " +
 					"WHERE Patient.LastName = ?");
                             st.setString(1, last);
                         }
                         else {
                             return new ArrayList<PatientDto>();
                         }
 			rs = st.executeQuery();
 			List<PatientDto> results = new ArrayList<PatientDto>();
 			PatientDto patient = new PatientDto();
 			while (rs.next()) {
                             if (rs.getInt("Patient.Active") != 0){
 				patient.setField(PatientDto.PATIENT_ID, rs.getInt(PatientDto.PATIENT_ID));
 				patient.setField(PatientDto.FIRST, rs.getString(PatientDto.FIRST));
 				patient.setField(PatientDto.LAST, rs.getString(PatientDto.LAST));
 				patient.setField(PatientDto.PHONE, rs.getString(PatientDto.PHONE));
 				patient.setField(PatientDto.NOTES, rs.getString(PatientDto.NOTES));
 				// TODO: change to 0 if null
 				patient.setField(PatientDto.NO_SHOW, rs.getInt(PatientDto.NO_SHOW));
 				results.add(patient);
 				patient = new PatientDto();
                             }
 			}
 			return results;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public List<NoShowDto> getNoShowsByPatient(int patID) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("SELECT * FROM NoShows WHERE PatID=?");
 			st.setInt(1, patID);
 			rs = st.executeQuery();
 			List<NoShowDto> results = new ArrayList<NoShowDto>();
 			NoShowDto noShow = new NoShowDto();
 			while (rs.next()) {
 				noShow.setField(NoShowDto.NOSHOW_ID, rs.getInt(NoShowDto.NOSHOW_ID));
 				noShow.setField(NoShowDto.PATIENT_ID, rs.getString(NoShowDto.PATIENT_ID));
 				noShow.setField(NoShowDto.DATE, rs.getString(NoShowDto.DATE));
 				results.add(noShow);
 				noShow = new NoShowDto();
 			}
 			return results;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
         
         
 
 	@Override
 	public TypeDto addNewPractitionerType(String serviceType) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("INSERT INTO ServiceType (TypeName) VALUES (?)");
 			st.setString(1, serviceType);
 			st.executeUpdate();
 			return this.getType(serviceType);
                         
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public boolean removePractitionerType(String serviceType) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("DELETE FROM ServiceType WHERE (TypeName=?)");
 			st.setString(1, serviceType);
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public List<TypeDto> getAllPractitionerTypes() {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("SELECT * FROM ServiceType");
 			rs = st.executeQuery();
 			List<TypeDto> results = new ArrayList<TypeDto>();
 			while (rs.next()) {
 				TypeDto type = new TypeDto();
 				type.setField(TypeDto.TYPE_NAME, rs.getString(TypeDto.TYPE_NAME));
 				type.setField(TypeDto.TYPE_ID, rs.getInt(TypeDto.TYPE_ID));
 				results.add(type);
 			}
 			return results;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public List<PractitionerDto> getAllPractitioners() {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("SELECT * FROM Practitioner " +
 					"INNER JOIN ServiceType ON Practitioner.`TypeID` = ServiceType.TypeID");
 			rs = st.executeQuery();
 			List<PractitionerDto> results = new ArrayList<PractitionerDto>();
 			PractitionerDto practitioner;
 			while (rs.next()) {
                             if (rs.getInt("Active") != 0){
 				practitioner = new PractitionerDto();
 				practitioner.setField(
 						PractitionerDto.PRACT_ID, rs.getInt(PractitionerDto.PRACT_ID));
 				TypeDto type = new TypeDto();
 				type.setField(TypeDto.TYPE_ID, rs.getInt(TypeDto.TYPE_ID));
 				type.setField(TypeDto.TYPE_NAME, rs.getString(TypeDto.TYPE_NAME));
 				practitioner.setField(
 						PractitionerDto.TYPE, type);
 				practitioner.setField(
 						PractitionerDto.FIRST, rs.getString(PractitionerDto.FIRST));
 				practitioner.setField(
 						PractitionerDto.LAST, rs.getString(PractitionerDto.LAST));
 				practitioner.setField(
 						PractitionerDto.APPT_LENGTH, rs.getInt(PractitionerDto.APPT_LENGTH));
 				practitioner.setField(
 						PractitionerDto.PHONE, rs.getString(PractitionerDto.PHONE));
 				practitioner.setField(
 						PractitionerDto.NOTES, rs.getString(PractitionerDto.NOTES));
 				results.add(practitioner);
                             }
 			}
 			return results;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public PractitionerDto addPractitioner(int typeID, String first, String last, int appLength, String phone, String notes) {
 		PreparedStatement st = null;
                 ResultSet rs = null;
 
 		try {
 			
 			st = connection.prepareStatement("INSERT INTO Practitioner " +
 					"(TypeID, FirstName, LastName, ApptLength, PhoneNumber, Active, Notes) " +
 			"VALUES (?, ?, ?, ?, ?, ?, ?)");
 			
 			st.setInt(1, typeID);
 			st.setString(2, first);
 			st.setString(3, last); 
 			st.setInt(4, appLength);
 			st.setString(5, phone);
                         st.setInt(6, 7);
 			st.setString(7, notes);
 			st.executeUpdate();
                         
                         st = connection.prepareStatement(
                         "SELECT Max(PractID) FROM Practitioner"); 
 		
                         rs = st.executeQuery();
                         
                         rs.next();
                         
                         int id = rs.getInt(1);
                         
                         st = connection.prepareStatement(
                         "SELECT * FROM Practitioner INNER JOIN ServiceType ON Practitioner.TypeID = " +
 					"ServiceType.TypeID WHERE Practitioner.PractID=?");
                         
                         st.setInt(1, id);
                         
                         rs = st.executeQuery();
                         
                         if(rs.next()){
                             PractitionerDto returnPract = new PractitionerDto();
 
                             returnPract.setField(PractitionerDto.APPT_LENGTH, rs.getInt(PractitionerDto.APPT_LENGTH));
                             returnPract.setField(PractitionerDto.FIRST, rs.getString(PractitionerDto.FIRST));
                             returnPract.setField(PractitionerDto.LAST, rs.getString(PractitionerDto.LAST));
                             returnPract.setField(PractitionerDto.NOTES, rs.getString(PractitionerDto.NOTES));
                             returnPract.setField(PractitionerDto.PHONE, rs.getString(PractitionerDto.PHONE));
                             returnPract.setField(PractitionerDto.PRACT_ID, rs.getInt(PractitionerDto.PRACT_ID));
                             TypeDto type = new TypeDto();
                             type.setField(TypeDto.TYPE_ID, rs.getInt(TypeDto.TYPE_ID));
                             type.setField(TypeDto.TYPE_NAME, rs.getString(TypeDto.TYPE_NAME));
                             returnPract.setField(PractitionerDto.TYPE, type);
 
                             return returnPract;
                         }
 			
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	// TODO: Remove appointments with this practitioner
 	@Override
 	public boolean removePractitioner(PractitionerDto practitioner) {
 		PreparedStatement st = null;
 
 		try {
 			if (practitioner.getPractID() == null) {
 				lgr.log(Level.WARNING, "Tried to delete practitioner without ID\n");
 				return false;
 			} else {
 				st = connection.prepareStatement(
 				"UPDATE Practitioner SET Practitioner.Active=0 WHERE PractID=?");
 				st.setInt(1, practitioner.getPractID());
 			}
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean updatePractitionerInfo(PractitionerDto practitioner) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE Practitioner " +
 				"SET TypeID=?, FirstName=?, LastName=?, ApptLength=?, PhoneNumber=?, Notes=? " +
 				"WHERE PractID=?" );
 		st.setInt(1, practitioner.getTypeID());
 		st.setString(2, practitioner.getFirst());
 		st.setString(3,practitioner.getLast());
 		st.setInt(4,practitioner.getApptLength());
 		st.setString(5,practitioner.getPhone());
 		st.setString(6,  practitioner.getNotes());
 		st.setInt(7, practitioner.getPractID());
 		
 		int updated = st.executeUpdate();
 		return updated != 0;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
 
 	@Override
 	public List<SchedulePractitionerDto> getAllPractitionersForDay(DayDto day) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
                         //TODO: single query
 			st = connection.prepareStatement(
 					"SELECT * FROM PractitionerScheduled WHERE ScheduleDate =?");
 			st.setDate(1, day.getDate());
 			rs = st.executeQuery();
 			List<SchedulePractitionerDto> retList = new ArrayList<SchedulePractitionerDto>();
 			SchedulePractitionerDto newPract;
 
 			while(rs.next()){
 				newPract = new SchedulePractitionerDto();
 				newPract.setEnd(rs.getInt(SchedulePractitionerDto.END));
 				newPract.setStart(rs.getInt(SchedulePractitionerDto.START));
 				newPract.setField(SchedulePractitionerDto.PRACT_SCHED_ID, 
 						rs.getInt(SchedulePractitionerDto.PRACT_SCHED_ID));
 				newPract.setField(SchedulePractitionerDto.PRACT, 
 						this.getPractitioner(rs.getInt("PractID")));
 				newPract.setField(SchedulePractitionerDto.APPOINTMENTS, 
 						this.getAllAppointments(newPract.getPractSchedID()));
 				retList.add(newPract);
 			}
 			return retList;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public boolean removePractitionerFromDay(int practSchedId, DayDto day) {
 		PreparedStatement st = null;
 		try {
 			st = connection.prepareStatement("DELETE FROM PractitionerScheduled WHERE PractSchID=?");
 			st.setInt(1, practSchedId);
 			st.executeUpdate();
 			st = connection.prepareStatement("DELETE FROM Appointment WHERE PractSchedID=?");
 			st.setInt(1, practSchedId);
 			st.executeUpdate();
 			return true;
 		}
 		catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e + " : appointment without patient being" +
 			" checked as no show");
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 
 		}
 		return false;
 	}
 
 	@Override
 	public boolean changePractitionerHoursForDay(SchedulePractitionerDto pract,
 			DayDto day, int start, int end) {
 		PreparedStatement st = null;
 
 		try {
 			//delete previous appointments
 			st = connection.prepareStatement("DELETE FROM Appointment WHERE PractSchedID = ? AND " +
 					"(Appointment.StartTime>? OR Appointment.EndTime<?)");
 			st.setInt(1, pract.getPractSchedID());
 			st.setInt(2, end - pract.getPractitioner().getApptLength());
 			st.setInt(3, start + pract.getPractitioner().getApptLength());
 			st.executeUpdate();
 
 			int newStart = pract.getStart();
 			int newEnd = pract.getEnd();
 			if (newStart < start) { 
 				newStart = start;
 			}
 			if (newEnd > end) {
 				newEnd = end;
 			}
 			st = connection.prepareStatement("UPDATE PractitionerScheduled SET " +
 					"StartTime=?,EndTime=?");
 			st.setInt(1, newStart);
 			st.setInt(2, newEnd);
 			st.executeUpdate();
 			//set hours
 			pract.setStart(newStart);
 			pract.setEnd(newEnd);
 
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;    
 	}
 	
 	@Override
 	public boolean resetPractitionerHoursForDay(SchedulePractitionerDto pract,
 			DayDto day, int start, int end) {
 		PreparedStatement st = null;
 
 		try {
 			//delete previous appointments
 			st = connection.prepareStatement("DELETE FROM Appointment WHERE PractSchedID = ?");
 			st.setInt(1, pract.getPractSchedID());
 			st.setInt(2, end - pract.getPractitioner().getApptLength());
 			st.setInt(3, start + pract.getPractitioner().getApptLength());
 			st.executeQuery();
 
 			//set hours
 			pract.setStart(start);
 			pract.setEnd(end);
 
 			AppointmentDto newApt = new AppointmentDto();
 
 			List<AppointmentDto> appointments = new ArrayList<AppointmentDto>();
 
 			pract.setField(SchedulePractitionerDto.APPOINTMENTS, appointments);;
 
 			st = connection.prepareStatement(
 					"INSERT INTO Appointment (PractSchedID, StartTime, EndTime, ApptDate) VALUES (?, ?, ?, ?)");
 
 			for (int i = start; i < end; i+=pract.getPractitioner().getApptLength()){
 				newApt = new AppointmentDto();
 				newApt.setEnd(i + pract.getPractitioner().getApptLength());
 				st.setInt(3, i + pract.getPractitioner().getApptLength());
 				newApt.setStart(i);
 				st.setInt(2, i);
 				newApt.setField(AppointmentDto.APPT_DATE, day.getDate());
 				st.setDate(4, day.getDate());
 				newApt.setField(AppointmentDto.PRACT_SCHED_ID, pract.getPractSchedID());
 				st.setInt(1, pract.getPractSchedID());
 				appointments.add(newApt);
 				newApt.setField(AppointmentDto.PRACTITIONER_NAME, pract.getPractitioner().getFirst() + " " + pract.getPractitioner().getLast());
 				st.executeQuery();
 			}
 
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;    
 	}
 
 	@Override
 	public boolean addPatientToAppointment(int patID, AppointmentDto appointment) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE Appointment " +
 				"SET Appointment.PatID=? WHERE Appointment.ApptID=?" );
 		st.setInt(1, patID);
 		st.setInt(2, appointment.getApptID());
 		
 		st.executeUpdate();
                 appointment.setPatient(DataServiceImpl.GLOBAL_DATA_INSTANCE.getPatient(patID));
 		return true;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
 	
 	
 	public boolean addNotesToAppointment(AppointmentDto appointment) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE Appointment " +
 				"SET Appointment.Note=? WHERE Appointment.ApptID=?" );
 		st.setString(1, appointment.getNote());
 		st.setInt(2, appointment.getApptID());
 		
 		st.executeUpdate();
 		return true;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
 
 	@Override
 	public boolean removePatientFromAppointment(AppointmentDto appointment) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE Appointment " +
 				"SET Appointment.PatID=NULL WHERE Appointment.ApptID=?" );
 		st.setInt(1, appointment.getApptID());
 		
                 appointment.setPatient(null);
                 appointment.setPatientID(null);
 		st.executeUpdate();
 		return true;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
 
 	@Override
 	public boolean checkAsNoShow(AppointmentDto appointment) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			int patID = appointment.getPatientID();
 			Date date = appointment.getApptDate();
 			st = connection.prepareStatement("INSERT INTO NoShow " +
 					"(PatID, NoShowDate) " +
 			"VALUES (?, ?)");
 			st.setInt(1, patID);
 			st.setDate(2, date);
 			st.executeUpdate();
 			
 			st = null;
 			
 			// TODO: is this a safe way to get the noshow ID back?
 			st = connection.prepareStatement("SELECT MAX(NoShowID) as ID From NoShow");
 			rs = st.executeQuery();
 			if (rs.next()){
 				appointment.setNoShowID(rs.getInt("ID"));
 			}
 			st = null;
 			rs = null;
 			
 			
 			st = connection.prepareStatement("UPDATE Appointment " +
 				"SET Appointment.NoShowID = ? WHERE Appointment.ApptID=? ");
 			st.setInt(1, appointment.getNoShowID());
 			st.setInt(2, appointment.getApptID());
 			st.executeUpdate();
 			
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} catch (NullPointerException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e + " : appointment without patient being" +
 			" checked as no show");
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean uncheckAsNoShow(AppointmentDto appointment) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("DELETE FROM NoShow WHERE NoShowID=?");
 			System.out.println(appointment.getNoShowID());
 			st.setInt(1, appointment.getNoShowID());
 			st.executeUpdate();
 			
 			st = connection.prepareStatement("UPDATE Appointment " +
 				"SET Appointment.NoShowID = NULL WHERE Appointment.ApptID=? ");
 			st.setInt(1, appointment.getApptID());
 			st.executeUpdate();
 			appointment.setNoShowID(null);
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} catch (NullPointerException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e + " : appointment without patient being" +
 			" checked as no show");
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public WaitlistDto addPatientToWaitlist(PatientDto patient, TypeDto type, String comments) {
 		PreparedStatement st = null;
 		try {
 			st = connection.prepareStatement("INSERT INTO Waitlist " +
 					"(PatID, TypeID, DatetimeEntered, Comments) " +
 			"VALUES (?, ?, ?, ?)");
 			st.setInt(1, patient.getPatID());
 			st.setInt(2, type.getTypeID());
 			st.setTimestamp(3, new Timestamp(new java.util.Date().getTime()));
                         st.setString(4, comments);
 			st.executeUpdate();
 			return null; //Todo: changge return type
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public boolean removePatientFromWaitlist(PatientDto patient, TypeDto type) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("DELETE FROM Waitlist WHERE " +
 			"PatID=? AND TypeID=?");
 			st.setInt(1, patient.getPatID());
 			st.setInt(2, type.getTypeID());
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean commentWaitlist(WaitlistDto entry, String comment) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("UPDATE Waitlist SET Comments=? " +
 			"WHERE WaitlistID=?");
 			st.setString(1, comment);
 			st.setInt(2, entry.getWaitlistID());
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 	
 	@Override
 	public Timestamp getOldestWaitlistTime() {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("SELECT MIN(DatetimeEntered) FROM Waitlist");
 			rs = st.executeQuery();
 			if (rs.next()) {
 				return rs.getTimestamp(1);
 			}
 			return null;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 	
 	@Override
 	public boolean updateWaitlistTime(WaitlistDto entry, Timestamp time) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("UPDATE Waitlist SET DatetimeEntered=? " +
 			"WHERE WaitlistID=?");
 			st.setTimestamp(1, time);
 			st.setInt(2, entry.getWaitlistID());
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public List<WaitlistDto> getWaitlist() {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("SELECT * FROM Waitlist INNER JOIN Patient " +
 					"ON Waitlist.PatID=Patient.PatID INNER JOIN ServiceType ON " +
 					"Waitlist.TypeID = ServiceType.TypeID ORDER BY Waitlist.DatetimeEntered");
 			rs = st.executeQuery();
 			List<WaitlistDto> results = new ArrayList<WaitlistDto>();
 			while (rs.next()) {
 				WaitlistDto entry = new WaitlistDto();
 				PatientDto patient = new PatientDto();
 				entry.setField(WaitlistDto.WAITLIST_ID, rs.getInt(WaitlistDto.WAITLIST_ID));
 				TypeDto type = new TypeDto();
 				type.setField(TypeDto.TYPE_ID, rs.getInt(TypeDto.TYPE_ID));
 				type.setField(TypeDto.TYPE_NAME, rs.getString(TypeDto.TYPE_NAME));
 				entry.setField(WaitlistDto.TYPE, type);
 				entry.setField(WaitlistDto.DATE, rs.getDate(WaitlistDto.DATE));
 				entry.setField(WaitlistDto.COMMENTS, rs.getString(WaitlistDto.COMMENTS));
 				patient.setField(PatientDto.PATIENT_ID, rs.getInt(PatientDto.PATIENT_ID));
 				patient.setField(PatientDto.FIRST, rs.getString(PatientDto.FIRST));
 				patient.setField(PatientDto.LAST, rs.getString(PatientDto.LAST));
 				patient.setField(PatientDto.PHONE, rs.getString(PatientDto.PHONE));
 				patient.setField(PatientDto.NOTES, rs.getString(PatientDto.NOTES));
 				entry.setPatient(patient);
 				results.add(entry);
 			}
 			return results;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	//TODO set hours for inputs
 	public boolean setHoursForDay(DayDto day, int start, int end) {
 		PreparedStatement st = null;
 
 		try {
 			st = connection.prepareStatement("UPDATE Day " +
 					"SET StartTime=?, EndTime=? " +
 					"WHERE DayDate=?");
 			st.setInt(1, start);
 			st.setInt(2, end);
 			st.setDate(3, day.getDate());
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
 	@Override
 	public boolean setStatus(DayDto day) {
 		// TODO there's no status field for day... we could set the hours to null?
 		return false;
 	}
 
 	@Override
 	public SchedulePractitionerDto addPractitionerToDay(PractitionerDto pract, DayDto day, 
 			int start, int end){
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement(
 					"INSERT INTO PractitionerScheduled (PractID, ScheduleDate, StartTime, EndTime) VALUES (?, ?, ?, ?)");
 			st.setInt(1, pract.getPractID());
 			st.setDate(2, day.getDate());
 			st.setInt(3, start);
 			st.setInt(4, end);
 			st.executeUpdate();
                         st = connection.prepareStatement("SELECT Max(PractSchID) FROM PractitionerScheduled");
 			rs = st.executeQuery();
 			rs.next();
 
 			int pract_id = rs.getInt(1);
 
 			AppointmentDto newApt = new AppointmentDto();
 			SchedulePractitionerDto returnDto = new SchedulePractitionerDto();
 
 			List<AppointmentDto> appointments = new ArrayList<AppointmentDto>();
 
 			returnDto.setField(SchedulePractitionerDto.DATE, day.getDate());
 			returnDto.setField(SchedulePractitionerDto.APPOINTMENTS, appointments);
 			returnDto.setField(SchedulePractitionerDto.PRACT, pract);
 			returnDto.setField(SchedulePractitionerDto.END, end);
 			returnDto.setField(SchedulePractitionerDto.START, start);
 			returnDto.setField(SchedulePractitionerDto.PRACT_SCHED_ID, pract_id);
 
 			
 			PreparedStatement new_st = connection.prepareStatement("Select MAX(ApptID) as ID From Appointment");
 			rs = new_st.executeQuery();
 			rs.next();
 			int lastID = rs.getInt("ID");
 		
 			st = connection.prepareStatement("INSERT INTO Appointment (PractSchedID, StartTime, EndTime, ApptDate) VALUES (?, ?, ?, ?)");
 			int j = 0;
			for (int i = start; i <= end + pract.getApptLength(); i+=pract.getApptLength()){
 				newApt = new AppointmentDto();
 				newApt.setField(AppointmentDto.APPT_ID, lastID+j);
 				newApt.setEnd(i + pract.getApptLength());
 				st.setInt(3, i + pract.getApptLength());
 				newApt.setStart(i);
 				st.setInt(2, i);
 				newApt.setField(AppointmentDto.APPT_DATE, day.getDate());
 				st.setDate(+4, day.getDate());
 				newApt.setField(AppointmentDto.PRACT_SCHED_ID, pract_id);
 				st.setInt(1, pract_id);
 				appointments.add(newApt);
 				st.executeUpdate();
 				j++;
 			}
 			
 			return returnDto;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;    
 	}
 
 	@Override
 	public DayDto getOrCreateDay(Date date) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 		DayDto retDay = new DayDto(); 
 
 		try {
 			st = connection.prepareStatement(
 					"SELECT * FROM Day WHERE DayDate=?");
 			st.setDate(1, date);
 			rs = st.executeQuery();
 
 			if (rs.next()){
 				retDay.setField(DayDto.DATE, date);
 				retDay.setStart(rs.getInt(DayDto.START));
 				retDay.setEnd(rs.getInt(DayDto.END));
 				return retDay;
 			}
 			else{
 				st = connection.prepareStatement("INSERT INTO Day (DayDate, StartTime, EndTime) VALUES (?, ?, ?)");
 				retDay.setField(DayDto.DATE, date);
 				st.setDate(1, date);
                                 Calendar cal = Calendar.getInstance();
                                 cal.setTime(date);
                                 TimeSlot times;
                                 switch (cal.get(Calendar.DAY_OF_WEEK)) {
                                     case (Calendar.MONDAY):
                                         times = this.getDayTimeslot(Day.MONDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                     case (Calendar.TUESDAY):
                                         times = this.getDayTimeslot(Day.TUESDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                     case (Calendar.WEDNESDAY):
                                         times = this.getDayTimeslot(Day.WEDNESDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                     case (Calendar.THURSDAY):
                                         times = this.getDayTimeslot(Day.THURSDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                     case (Calendar.FRIDAY):
                                         times = this.getDayTimeslot(Day.FRIDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                     case (Calendar.SATURDAY):
                                         times = this.getDayTimeslot(Day.SATURDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                     case (Calendar.SUNDAY):
                                         times = this.getDayTimeslot(Day.SUNDAY); 
                                         retDay.setStart(times.getStartTime());
                                         st.setInt(2, times.getStartTime());
                                         retDay.setEnd(times.getEndTime());
                                         st.setInt(3, times.getEndTime());
                                         break;
                                 }   
 				st.executeUpdate();
 				return retDay;
 			}
 
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return retDay;
 	}
 
 	@Override
 	public PractitionerDto getPractitioner(int practID) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 
 		try {
 			st = connection.prepareStatement("SELECT * FROM Practitioner " +
 					"INNER JOIN ServiceType ON Practitioner.TypeID = " +
 					"ServiceType.TypeID WHERE Practitioner.PractID=(?)");
 			st.setInt(1, practID);
 			rs = st.executeQuery();
 			PractitionerDto pract = new PractitionerDto();
 
 			if (rs.next()) {
 				pract.setField(PractitionerDto.FIRST, rs.getString(PractitionerDto.FIRST));
 				pract.setField(PractitionerDto.LAST, rs.getString(PractitionerDto.LAST));
 				pract.setField(PractitionerDto.APPT_LENGTH, 
 						rs.getInt(PractitionerDto.APPT_LENGTH));
 				pract.setField(PractitionerDto.NOTES, rs.getString(PractitionerDto.NOTES));
 				pract.setField(PractitionerDto.PHONE, rs.getString(PractitionerDto.PHONE));
 				pract.setField(PractitionerDto.PRACT_ID, rs.getInt(PractitionerDto.PRACT_ID));
 				TypeDto type = new TypeDto();
 				type.setField(TypeDto.TYPE_ID, rs.getInt(TypeDto.TYPE_ID));
 				type.setField(TypeDto.TYPE_NAME, rs.getString(TypeDto.TYPE_NAME));
 				pract.setField(PractitionerDto.TYPE, type);
                                 return pract;
 			}
 			return null;
 
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public List<AppointmentDto> getAllAppointments(int schedPractId) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 		String name = null;
 
 		try {
 			st = connection.prepareStatement("Select FirstName, LastName FROM PractitionerScheduled INNER JOIN Practitioner ON PractitionerScheduled.PractID = Practitioner.PractID WHERE PractitionerScheduled.PractSchID = ?");
 			st.setInt(1, schedPractId);
 			rs = st.executeQuery();
 				if (rs.next()){
 					name = rs.getString("FirstName") + ' '+ rs.getString("LastName");
 				}
 			rs = null;
 			st = null;
 			
 			st = connection.prepareStatement(
 					"SELECT * FROM Appointment WHERE PractSchedID = ?");
 			st.setInt(1, schedPractId);
 			rs = st.executeQuery();
 
 			List<AppointmentDto> retList = new ArrayList<AppointmentDto>();
 			AppointmentDto newAppointment;
 
 			while(rs.next()){
 				newAppointment = new AppointmentDto();
 				newAppointment.setField(AppointmentDto.APPT_DATE, 
 						rs.getDate(AppointmentDto.APPT_DATE));
 				newAppointment.setField(AppointmentDto.APPT_ID, 
 						rs.getInt(AppointmentDto.APPT_ID));
 				newAppointment.setField(AppointmentDto.END, 
 						rs.getInt(AppointmentDto.END));
 				newAppointment.setField(AppointmentDto.NOTE, 
 						rs.getString(AppointmentDto.NOTE));
 				newAppointment.setField(AppointmentDto.NO_SHOW_ID,
 						rs.getInt(AppointmentDto.NO_SHOW_ID));
 				newAppointment.setField(AppointmentDto.PAT_ID,
 						rs.getInt(AppointmentDto.PAT_ID));
 				newAppointment.setField(AppointmentDto.PRACT_SCHED_ID, 
 						rs.getInt(AppointmentDto.PRACT_SCHED_ID));
 				newAppointment.setField(AppointmentDto.START, 
 						rs.getInt(AppointmentDto.START));
 				newAppointment.setField(AppointmentDto.CONFIRMATION,
 						rs.getInt(AppointmentDto.CONFIRMATION)!=0);
 				newAppointment.setField(AppointmentDto.PRACTITIONER_NAME, name);
 				retList.add(newAppointment);
 			}
 			return retList;
 
 		} catch (SQLException e) {
 			//Logger lgr = Logger.getLogger(DataServiceImpl.class.getName());
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 	
 	@Override
 	public List<AppointmentDto> getFutureAppointmentsByPatId(int patID) {
 		PreparedStatement st = null;
 		ResultSet rs = null;
 		String name = null;
 
 		try {
 			st = connection.prepareStatement(
 					"SELECT * FROM Appointment,PractitionerScheduled,Practitioner WHERE " +
 					"Appointment.PatID=? AND " +
 					"Appointment.PractSchedID = PractitionerScheduled.PractSchID AND " +
 					"PractitionerScheduled.PractID = Practitioner.PractID AND " +
 					"Appointment.ApptDate>=?");
 			st.setInt(1, patID);
 			st.setDate(2, new Date(Calendar.getInstance().getTime().getTime()));
 			rs = st.executeQuery();
 
 			List<AppointmentDto> retList = new ArrayList<AppointmentDto>();
 			AppointmentDto newAppointment;
 
 			while(rs.next()){
 				newAppointment = new AppointmentDto();
 				newAppointment.setField(AppointmentDto.APPT_DATE, 
 						rs.getDate(AppointmentDto.APPT_DATE));
 				newAppointment.setField(AppointmentDto.APPT_ID, 
 						rs.getInt(AppointmentDto.APPT_ID));
 				newAppointment.setField(AppointmentDto.END, 
 						rs.getInt(AppointmentDto.END));
 				newAppointment.setField(AppointmentDto.NOTE, 
 						rs.getString(AppointmentDto.NOTE));
 				newAppointment.setField(AppointmentDto.NO_SHOW_ID,
 						rs.getInt(AppointmentDto.NO_SHOW_ID));
 				newAppointment.setField(AppointmentDto.PAT_ID,
 						rs.getInt(AppointmentDto.PAT_ID));
 				newAppointment.setField(AppointmentDto.PRACT_SCHED_ID, 
 						rs.getInt(AppointmentDto.PRACT_SCHED_ID));
 				newAppointment.setField(AppointmentDto.START, 
 						rs.getInt(AppointmentDto.START));
 				newAppointment.setField(AppointmentDto.CONFIRMATION,
 						rs.getInt(AppointmentDto.CONFIRMATION)!=0);
 				newAppointment.setField(AppointmentDto.PRACTITIONER_NAME,
 						rs.getString(PractitionerDto.FIRST) + " " +
 						rs.getString(PractitionerDto.LAST));
 				retList.add(newAppointment);
 			}
 			return retList;
 
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return null;
 	}
 
 	@Override
 	public boolean confirmAppointment(AppointmentDto appointment) {
 		PreparedStatement st = null;
 
 		try {
 			appointment.setConfirmation(true);
 			st = connection.prepareStatement(
 					"UPDATE Appointment SET Confirmation=1 " +
 					"WHERE ApptID=?");
 			st.setInt(1, appointment.getApptID());
                         appointment.setConfirmation(true);
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
         
         @Override
 	public boolean unConfirmAppointment(AppointmentDto appointment) {
 		PreparedStatement st = null;
 
 		try {
 			appointment.setConfirmation(true);
 			st = connection.prepareStatement(
 					"UPDATE Appointment SET Confirmation=0 " +
 					"WHERE ApptID=?");
 			st.setInt(1, appointment.getApptID());
                         appointment.setConfirmation(false);
 			st.executeUpdate();
 			return true;
 		} catch (SQLException e) {
 			lgr.log(Level.SEVERE, e.getMessage(), e);
 		} finally {
 			try {
 				if (st != null) {
 					st.close();
 				}
 			} catch (SQLException ex) {
 				lgr.log(Level.WARNING, ex.getMessage(), ex);
 			}
 		}
 		return false;
 	}
 
     @Override
     public PatientDto addPatient(String phone, String first, String last, String notes) {
         PreparedStatement st = null;
         ResultSet rs = null;
 	try {
 		
 		st = connection.prepareStatement(
 			"INSERT INTO Patient (FirstName, LastName, PhoneNumber, Notes, Active) VALUES (?, ?, ?, ?, 1)");
 		
 		st.setString(1, first);
 		st.setString(2, last);
 		st.setString(3, phone);
 		st.setString(4, notes);
 			st.executeUpdate();
                         
                 st = connection.prepareStatement(
                         "SELECT Max(PatID) FROM Patient");
 		rs = st.executeQuery();
                 rs.next();
                 
                 int newId = rs.getInt(1);
                 
                 st = connection.prepareStatement("SELECT * FROM Patient WHERE PatID=?");
                 
                 st.setInt(1, newId);
                 
                 rs = st.executeQuery();
                 
                 if(rs.next()){
                 PatientDto returnPatient = new PatientDto();
                     returnPatient.setField(PatientDto.PATIENT_ID, rs.getInt(PatientDto.PATIENT_ID));
                     returnPatient.setFirst(first);
                     returnPatient.setLast(last);
                     returnPatient.setNotes(notes);
                     returnPatient.setPhone(phone);
                     returnPatient.setField(PatientDto.NO_SHOW, 0);
 
                     return returnPatient;
                 }
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 	return null;
     }
 
     @Override
     public TypeDto getType(String type) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("SELECT * FROM ServiceType WHERE TypeName=?");
 		
 		st.setString(1, type);
                 rs = st.executeQuery();
                         
                 if (rs.next()){
                     TypeDto returnType = new TypeDto();
                     returnType.setField(TypeDto.TYPE_ID, rs.getInt(TypeDto.TYPE_ID));
                     returnType.setField(TypeDto.TYPE_NAME, type);
                     return returnType;
                 }
                 else {
                     return null;
                 }
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
         return null;
     }
 
     @Override
     public boolean updatePatient(PatientDto patient) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE Patient " +
 				"SET Patient.FirstName=?, Patient.LastName=?, Patient.PhoneNumber=?, Patient.Notes=?" +
 				" WHERE PatID = ?");
 		st.setString(1, patient.getFirst());
 		st.setString(2,patient.getLast());
 		st.setString(3,patient.getPhone());
 		st.setString(4,patient.getNotes());
 		st.setInt(5,patient.getPatID());
 		
 		st.executeUpdate();
 		
 		return true;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
 
     @Override
     public boolean updateWaitlist(WaitlistDto wl) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE Waitlist " +
 				"SET Waitlist.PatID=?, Waitlist.TypeID=?, Waitlist.DatetimeEntered=?, Waitlist.Comments=?" +
 				" WHERE Waitlist.WaitlistID = ?");
 		st.setInt(1, wl.getPatientID());
 		st.setInt(2,wl.getTypeID());
 		// This seems worrisome? will date work this way?
 		st.setDate(3,wl.getDate());
 		st.setString(4,wl.getComments());
 		st.setInt(5,wl.getWaitlistID());
 		
 		st.executeUpdate();
 		return true;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
     
     @Override
     public boolean removePatientFromWaitlist(WaitlistDto patient) {
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("DELETE FROM Waitlist WHERE PatID = ?");
 		
 		st.setInt(1, patient.getPatientID());
                 st.executeUpdate();
                 return true;
                 
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 		return false;
     }
 
     @Override
     public ArrayList<AppointmentDto> searchForAppointments(int typeId) {
     	//TODO: Claire
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
         	if (typeId != -1) {
         		st = connection.prepareStatement("SELECT * FROM Appointment,Practitioner," +
         				"PractitionerScheduled WHERE Appointment.PractSchedID = " +
         				"PractitionerScheduled.PractSchID AND PractitionerScheduled.PractID = " +
         				"Practitioner.PractID AND Practitioner.TypeID=? AND Appointment.ApptDate>=? " +
         		"AND Appointment.PatID IS NULL ORDER BY Appointment.ApptDate, Appointment.StartTime");
         		st.setInt(1,typeId);
         		st.setDate(2, new Date(new java.util.Date().getTime()));
         	} else {
         		st = connection.prepareStatement("SELECT * FROM Appointment,Practitioner," +
         				"PractitionerScheduled WHERE Appointment.PractSchedID = " +
         				"PractitionerScheduled.PractSchID AND PractitionerScheduled.PractID = " +
         				"Practitioner.PractID AND Appointment.ApptDate>=? " +
         		"AND Appointment.PatID IS NULL ORDER BY Appointment.ApptDate, Appointment.StartTime");
         		st.setDate(1, new Date(new java.util.Date().getTime()));
         	}
         	
         	rs = st.executeQuery();
         	ArrayList<AppointmentDto> aptList = new ArrayList<AppointmentDto>();
 			AppointmentDto newAppt;
 
         	Calendar c = Calendar.getInstance();
         	int currentMinutes = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
 			while(rs.next()){
 				newAppt = new AppointmentDto();
 				
 				newAppt.setField(AppointmentDto.APPT_DATE, rs.getDate(AppointmentDto.APPT_DATE));
 				newAppt.setField(AppointmentDto.APPT_ID, rs.getInt(AppointmentDto.APPT_ID));
 				newAppt.setField(AppointmentDto.PRACT_SCHED_ID, rs.getInt(AppointmentDto.PRACT_SCHED_ID));
 				newAppt.setField(AppointmentDto.START, rs.getInt(AppointmentDto.START));
 				newAppt.setField(AppointmentDto.END, rs.getInt(AppointmentDto.END));
 				newAppt.setField(AppointmentDto.NOTE, rs.getString(AppointmentDto.NOTE));
 				newAppt.setField(AppointmentDto.PRACTITIONER_NAME, rs.getString(PractitionerDto.FIRST) + " "+ rs.getString(PractitionerDto.LAST));
 				
 				// manual filter of starttime
 	        	Calendar apptCal = Calendar.getInstance();
 	        	apptCal.setTime(newAppt.getApptDate());
 	        	if (apptCal.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) &&
 	        			apptCal.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
 	        		if (newAppt.getStart() >= currentMinutes) {
 	        			aptList.add(newAppt);
 	        		}
 	        	} else {
 	        		aptList.add(newAppt);
 	        	}
 			}
 			return aptList;
 
         } catch (SQLException e) {
         	lgr.log(Level.SEVERE, e.getMessage(), e);
         } finally {
         	try {
         		if (st != null) {
         			st.close();
         		}
         	} catch (SQLException ex) {
         		lgr.log(Level.WARNING, ex.getMessage(), ex);
         	}
         }
         return null;
     }
 
     @Override
     public ArrayList<AppointmentDto> getAllPatientsForDay(Date day){
     	PreparedStatement st = null;
     	ResultSet rs = null;
     	try {
 
     		st = connection.prepareStatement("Select * From Patient INNER JOIN Appointment ON Appointment.PatID = Patient.PatID LEFT JOIN (Select PatID, Count(NoShowID) as NumberOfNoShows from NoShow Group by PatID) as temp ON temp.PatID = Patient.PatID WHERE Appointment.ApptDate = ? AND Appointment.PatID IS NOT NULL");
 
     		st.setDate(1, day);
     		rs = st.executeQuery();
 
     		ArrayList<AppointmentDto> returnList = new ArrayList<AppointmentDto>();
     		PatientDto newPat;
     		AppointmentDto newAppt;
 
     		while (rs.next()){
     			newPat = new PatientDto();
     			newPat.setField(PatientDto.FIRST, rs.getString(PatientDto.FIRST));
     			newPat.setField(PatientDto.LAST, rs.getString(PatientDto.LAST));
     			newPat.setField(PatientDto.NO_SHOW, rs.getInt(PatientDto.NO_SHOW));
     			newPat.setField(PatientDto.NOTES, rs.getString(PatientDto.NOTES));
     			newPat.setField(PatientDto.PATIENT_ID, rs.getInt(PatientDto.PATIENT_ID));
     			newPat.setField(PatientDto.PHONE, rs.getString(PatientDto.PHONE));
     			
     			newAppt = new AppointmentDto();
     			newAppt.setPatient(newPat);
     			newAppt.setField(AppointmentDto.APPT_ID, rs.getInt(AppointmentDto.APPT_ID));
     			newAppt.setField(AppointmentDto.NO_SHOW_ID, rs.getInt(AppointmentDto.NO_SHOW_ID));
     			newAppt.setField(AppointmentDto.START, rs.getInt(AppointmentDto.START));
     			newAppt.setField(AppointmentDto.END, rs.getInt(AppointmentDto.END));
     			newAppt.setField(AppointmentDto.CONFIRMATION, rs.getInt(AppointmentDto.CONFIRMATION)!=0);
     			newAppt.setField(AppointmentDto.NOTE, rs.getString(AppointmentDto.NOTE));
     			newAppt.setField(AppointmentDto.APPT_DATE, rs.getDate(AppointmentDto.APPT_DATE));
     			newAppt.setField(AppointmentDto.PAT_ID, rs.getInt(AppointmentDto.PAT_ID));
     			newAppt.setField(AppointmentDto.PRACT_SCHED_ID, rs.getInt(AppointmentDto.PRACT_SCHED_ID));
     			
     			returnList.add(newAppt);
     		}
     		return returnList;
 
     	} catch (SQLException e) {
     		lgr.log(Level.SEVERE, e.getMessage(), e);
     	} finally {
     		try {
     			if (st != null) {
     				st.close();
     			}
     		} catch (SQLException ex) {
     			lgr.log(Level.WARNING, ex.getMessage(), ex);
     		}
     	}
     	return null;
     }
 
     @Override
     public TimeSlot getDayTimeslot(Day day) {
         String dayname = "";
         if (day == Day.SUNDAY){
             dayname = "Sunday";
         }
         else if (day == Day.MONDAY){
             dayname = "Monday";
         }
         else if (day == Day.TUESDAY){
             dayname = "Tuesday";
         }
         else if (day == Day.WEDNESDAY){
             dayname = "Wednesday";
         }
         else if (day == Day.THURSDAY){
             dayname = "Thursday";
         }
         else if (day == Day.FRIDAY){
             dayname = "Friday";
         }
         else if (day == Day.SATURDAY){
             dayname = "Saturday";
         }
         PreparedStatement st = null;
         ResultSet rs = null;
         try {
 		
 		st = connection.prepareStatement("SELECT * FROM DefaultHours WHERE Day=?");
 		st.setString(1, dayname); 
 		
 		rs = st.executeQuery();
                 if (rs.next()){
                     return new TimeSlot(rs.getInt("StartTime"), rs.getInt("EndTime"));
                 }
 		return null;
 		
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 	return null;
     }
 
     @Override
     public boolean setTimeSlot(Day day, TimeSlot newtimes) {
         String dayname = "";
         if (day == Day.SUNDAY){
             dayname = "Sunday";
         }
         else if (day == Day.MONDAY){
             dayname = "Monday";
         }
         else if (day == Day.TUESDAY){
             dayname = "Tuesday";
         }
         else if (day == Day.WEDNESDAY){
             dayname = "Wednesday";
         }
         else if (day == Day.THURSDAY){
             dayname = "Thursday";
         }
         else if (day == Day.FRIDAY){
             dayname = "Friday";
         }
         else if (day == Day.SATURDAY){
             dayname = "Saturday";
         }
         PreparedStatement st = null;
         try {
 		
 		st = connection.prepareStatement("UPDATE DefaultHours SET " +
                         "DefaultHours.StartTime=?, DefaultHours.EndTime=?" +
 				" WHERE Day=?");
 		st.setString(3, dayname);
                 st.setInt(1, newtimes.getStartTime());
                 st.setInt(2, newtimes.getEndTime());
                 
                 st.executeUpdate();
 		return true;
 	} catch (SQLException e) {
 		lgr.log(Level.SEVERE, e.getMessage(), e);
 	} finally {
 		try {
 			if (st != null) {
 				st.close();
 			}
 		} catch (SQLException ex) {
 			lgr.log(Level.WARNING, ex.getMessage(), ex);
 		}
 	}
 	return false;
     }
 
 
 }
