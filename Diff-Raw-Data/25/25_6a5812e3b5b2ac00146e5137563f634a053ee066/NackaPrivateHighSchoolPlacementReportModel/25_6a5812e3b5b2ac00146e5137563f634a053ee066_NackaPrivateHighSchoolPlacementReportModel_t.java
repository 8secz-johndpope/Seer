 /*
 * $Id: NackaPrivateHighSchoolPlacementReportModel.java,v 1.3 2004/01/19 08:42:46 anders Exp $
  *
  * Copyright (C) 2003 Agura IT. All Rights Reserved.
  *
  * This software is the proprietary information of Agura IT AB.
  * Use is subject to license terms.
  *
  */
 package se.idega.idegaweb.commune.school.report.business;
 
 import java.rmi.RemoteException;
 import java.util.Collection;
 import java.util.Iterator;
 
 import com.idega.block.school.data.School;
 import com.idega.block.school.data.SchoolStudyPath;
 
 /** 
  * Report model for private high school placements in Nacka.
  * <p>
 * Last modified: $Date: 2004/01/19 08:42:46 $ by $Author: anders $
  *
  * @author Anders Lindman
 * @version $Revision: 1.3 $
  */
 public class NackaPrivateHighSchoolPlacementReportModel extends ReportModel {
 
 	private final static int COLUMN_SIZE = 18;
 	
 	private final static int ROW_METHOD_STUDY_PATH = 1;
 	private final static int ROW_METHOD_TOTAL = 2;
 
 	private final static int COLUMN_METHOD_SCHOOL_1 = 101;
 	private final static int COLUMN_METHOD_SCHOOL_2 = 102;
 	private final static int COLUMN_METHOD_SCHOOL_3 = 103;
 	private final static int COLUMN_METHOD_TOTAL = 104;
 	private final static int COLUMN_METHOD_OTHER_COMMUNE_CITIZENS = 105;
 	private final static int COLUMN_METHOD_NACKA_CITIZENS = 106;
 
 	private final static String QUERY_ALL = "all";
 	private final static String QUERY_OTHER_COMMUNES = "other_communes";
 	private final static String QUERY_NACKA_COMMUNE = "nacka_commune";
 
 	private final static String KEY_REPORT_TITLE = KP + "title_nacka_private_high_school_placements";
 	
 	private School[] _schools = null;
 
 	/**
 	 * Constructs this report model.
 	 * @param reportBusiness the report business instance for calculating cell values
 	 */
 	public NackaPrivateHighSchoolPlacementReportModel(ReportBusiness reportBusiness) {
 		super(reportBusiness);
 		try {
 			Collection studyPaths = reportBusiness.getAllStudyPaths();
 			int rowSize = 0;
			rowSize += studyPaths.size() + 2; 
 			setReportSize(rowSize, COLUMN_SIZE);
 		} catch (RemoteException e) {
 			log(e.getMessage());
 		}
 	}
 	
 	/**
 	 * @see se.idega.idegaweb.commune.school.report.business.ReportModel#buildRowHeaders()
 	 */
 	protected Header[] buildRowHeaders() {
 		Header[] headers = null;
 		
 		try {
 			ReportBusiness rb = getReportBusiness();
 			Collection studyPaths = rb.getAllStudyPaths();
 			headers = new Header[studyPaths.size() + 3];
 			Iterator iter = studyPaths.iterator();
 			int headerIndex = 0;
 			while (iter.hasNext()) {
 				SchoolStudyPath studyPath = (SchoolStudyPath) iter.next();
 				headers[headerIndex] = new Header(studyPath.getDescription(), Header.HEADERTYPE_ROW_NONLOCALIZED_HEADER, 1);
 				Header child = new Header(studyPath.getCode(), Header.HEADERTYPE_ROW_NONLOCALIZED_NORMAL);
 				headers[headerIndex].setChild(0, child);
 				headerIndex++;
 			}
 			Header header = new Header(KEY_COMPULSORY_HIGH_SCHOOLS, Header.HEADERTYPE_ROW_HEADER, 1);
 			Header child = new Header(KEY_STUDY_PATH_CODE_COMPULSORY_HIGH_SCHOOL, Header.HEADERTYPE_ROW_NORMAL);
 			header.setChild(0, child);
 			headers[headerIndex] = header;
 			headerIndex++;
 			
 			header = new Header(KEY_TOTAL, Header.HEADERTYPE_ROW_TOTAL);
 			headers[headerIndex] = header;
 			headerIndex++;
 		} catch (RemoteException e) {
 			log(e.getMessage());
 		}
 		
 		return headers;
 	}
 	
 	/**
 	 * @see se.idega.idegaweb.commune.school.report.business.ReportModel#buildColumnHeaders()
 	 */
 	protected Header[] buildColumnHeaders() {
 		Header[] headers = new Header[6];
 		int headerColumn = 0;
 		try {
 			ReportBusiness rb = getReportBusiness();
 			Collection schools = rb.getPrivateHighSchools();
 			_schools = new School[schools.size()];
 			int schoolIndex = 0;
 			Iterator iter = schools.iterator();
 			while (iter.hasNext()) {
 				School school = (School) iter.next();
 				_schools[schoolIndex++] = school;
 				Header h = new Header(school.getName(), Header.HEADERTYPE_COLUMN_NONLOCALIZED_HEADER, 4);
 				Header child0 = new Header(KEY_SCHOOL_YEAR_1, Header.HEADERTYPE_COLUMN_NORMAL);
 				Header child1 = new Header(KEY_SCHOOL_YEAR_2, Header.HEADERTYPE_COLUMN_NORMAL);
 				Header child2 = new Header(KEY_SCHOOL_YEAR_3, Header.HEADERTYPE_COLUMN_NORMAL);
 				Header child3 = new Header(KEY_SUM_1_3, Header.HEADERTYPE_COLUMN_NORMAL);
 				h.setChild(0, child0);
 				h.setChild(1, child1);
 				h.setChild(2, child2);
 				h.setChild(3, child3);
 				headers[headerColumn++] = h;
 			}
 		} catch (RemoteException e) {
 			log(e.getMessage());
 		}
 		
 		Header h = new Header(KEY_TOTAL, Header.HEADERTYPE_COLUMN_HEADER, 4);
 		Header child0 = new Header(KEY_SCHOOL_YEAR_1, Header.HEADERTYPE_COLUMN_NORMAL);
 		Header child1 = new Header(KEY_SCHOOL_YEAR_2, Header.HEADERTYPE_COLUMN_NORMAL);
 		Header child2 = new Header(KEY_SCHOOL_YEAR_3, Header.HEADERTYPE_COLUMN_NORMAL);
 		Header child3 = new Header(KEY_SUM_1_3, Header.HEADERTYPE_COLUMN_NORMAL);
 		h.setChild(0, child0);
 		h.setChild(1, child1);
 		h.setChild(2, child2);
 		h.setChild(3, child3);
 		headers[headerColumn++] = h;
 		
 		h = new Header(KEY_OTHER_COMMUNES, Header.HEADERTYPE_COLUMN_HEADER);
 		headers[headerColumn++] = h;
 		
 		h = new Header(KEY_NACKA_STUDENTS, Header.HEADERTYPE_COLUMN_HEADER);
 		headers[headerColumn++] = h;
 				
 		return headers;
 	}
 	
 	/**
 	 * @see se.idega.idegaweb.commune.school.report.business.ReportModel#buildCells()
 	 */
 	protected void buildCells() {
 		for (int column = 0; column < getColumnSize(); column++) {
 			int row = 0;
 			int columnMethod = 0;
 			String columnParameter = null;
 			switch (column) {
 				case 0:
 					columnMethod = COLUMN_METHOD_SCHOOL_1;
 					columnParameter = "G1";
 					break;
 				case 1:
 					columnMethod = COLUMN_METHOD_SCHOOL_1;
 					columnParameter = "G2";
 					break;
 				case 2:
 					columnMethod = COLUMN_METHOD_SCHOOL_1;
 					columnParameter = "G3";
 					break;
 				case 3:
 					columnMethod = COLUMN_METHOD_SCHOOL_1;
 					columnParameter = null;
 					break;
 				case 4:
 					columnMethod = COLUMN_METHOD_SCHOOL_2;
 					columnParameter = "G1";
 					break;
 				case 5:
 					columnMethod = COLUMN_METHOD_SCHOOL_2;
 					columnParameter = "G2";
 					break;
 				case 6:
 					columnMethod = COLUMN_METHOD_SCHOOL_2;
 					columnParameter = "G3";
 					break;
 				case 7:
 					columnMethod = COLUMN_METHOD_SCHOOL_2;
 					columnParameter = null;
 					break;
 				case 8:
 					columnMethod = COLUMN_METHOD_SCHOOL_3;
 					columnParameter = "G1";
 					break;
 				case 9:
 					columnMethod = COLUMN_METHOD_SCHOOL_3;
 					columnParameter = "G2";
 					break;
 				case 10:
 					columnMethod = COLUMN_METHOD_SCHOOL_3;
 					columnParameter = "G3";
 					break;
 				case 11:
 					columnMethod = COLUMN_METHOD_SCHOOL_3;
 					columnParameter = null;
 					break;
 				case 12:
 					columnMethod = COLUMN_METHOD_TOTAL;
 					columnParameter = "G1";
 					break;
 				case 13:
 					columnMethod = COLUMN_METHOD_TOTAL;
 					columnParameter = "G2";
 					break;
 				case 14:
 					columnMethod = COLUMN_METHOD_TOTAL;
 					columnParameter = "G3";
 					break;
 				case 15:
 					columnMethod = COLUMN_METHOD_TOTAL;
 					columnParameter = null;
 					break;
 				case 16:
 					columnMethod = COLUMN_METHOD_OTHER_COMMUNE_CITIZENS;
 					columnParameter = null;
 					break;
 				case 17:
 					columnMethod = COLUMN_METHOD_NACKA_CITIZENS;
 					columnParameter = null;
 					break;
 			}
 			
 			try {
 				ReportBusiness rb = getReportBusiness();
 				Collection studyPaths = rb.getAllStudyPaths();
 				Iterator iter = studyPaths.iterator();
 				while (iter.hasNext()) {
 					SchoolStudyPath studyPath = (SchoolStudyPath) iter.next();
 					Object rowParameter = studyPath.getCode();
 					Cell cell = new Cell(this, row, column, ROW_METHOD_STUDY_PATH,
 							columnMethod, rowParameter, columnParameter, Cell.CELLTYPE_NORMAL);
 					setCell(row, column, cell);
 					row++;
 				}
 				Cell cell = new Cell(this, row, column, ROW_METHOD_TOTAL,
 						columnMethod, null, columnParameter, Cell.CELLTYPE_TOTAL);
 				setCell(row, column, cell);
 				row++;
 			} catch (RemoteException e) {
 				log(e.getMessage());
 			}
 		}
 	}
 	
 	/**
 	 * @see se.idega.idegaweb.commune.school.report.business.ReportModel#calculate()
 	 */
 	protected float calculate(Cell cell) throws RemoteException {
 		float value = 0f;
 		String studyPathPrefix = null;
 		if (cell.getRowParameter() != null) {
 			studyPathPrefix = (String) cell.getRowParameter();
 		}
 		String schoolYearName = (String) cell.getColumnParameter();
 		int row = cell.getRow();
 		int column = cell.getColumn();
 		
 		switch (cell.getRowMethod()) {
 			case ROW_METHOD_STUDY_PATH:
 				switch (cell.getColumnMethod()) {
 					case COLUMN_METHOD_SCHOOL_1:
 						if (schoolYearName == null) {
 							value += getCell(row, 0).getFloatValue();
 							value += getCell(row, 1).getFloatValue();
 							value += getCell(row, 2).getFloatValue();
 						} else {
 							value = getHighSchoolNackaCommunePlacementCount(_schools[0], schoolYearName, studyPathPrefix);
 						}
 						break;
 					case COLUMN_METHOD_SCHOOL_2:
 						if (schoolYearName == null) {
 							value += getCell(row, 4).getFloatValue();
 							value += getCell(row, 5).getFloatValue();
 							value += getCell(row, 6).getFloatValue();
 						} else {
 							value = getHighSchoolNackaCommunePlacementCount(_schools[1], schoolYearName, studyPathPrefix);
 						}
 						break;
 					case COLUMN_METHOD_SCHOOL_3:
 						if (schoolYearName == null) {
 							value += getCell(row, 8).getFloatValue();
 							value += getCell(row, 9).getFloatValue();
 							value += getCell(row, 10).getFloatValue();
 						} else {
 							value = getHighSchoolNackaCommunePlacementCount(_schools[2], schoolYearName, studyPathPrefix);
 						}
 						break;
 					case COLUMN_METHOD_TOTAL:
 						for (int i = column % 4; i < 12; i += 4) {
 							value += getCell(row, i).getFloatValue();
 						}
 						break;
 					case COLUMN_METHOD_OTHER_COMMUNE_CITIZENS:
 						value = getHighSchoolOCCPlacementCount(studyPathPrefix);
 						break;
 					case COLUMN_METHOD_NACKA_CITIZENS:
 						value = getHighSchoolNackaCitizenPlacementCount(studyPathPrefix);
 						break;
 				}
 				break;
 			case ROW_METHOD_TOTAL:
 				for (int i = 0; i < row; i++) {
 					Cell c = getCell(i, column);
 					value += c.getFloatValue();
 				}
 				break;
 		}
 		
 		return value;
 	}
 
 	/**
 	 * @see se.idega.idegaweb.commune.school.report.business.ReportModel#getReportTitleLocalizationKey()
 	 */
 	public String getReportTitleLocalizationKey() {
 		return KEY_REPORT_TITLE;
 	}
 	
 	/**
 	 * Returns the number of student placements for high schools
 	 * in Nacka commune for the specified school year.
 	 */
 	protected int getHighSchoolNackaCommunePlacementCount(School school, String schoolYearName, String studyPathPrefix) throws RemoteException {
 		PreparedQuery query = null;
 		ReportBusiness rb = getReportBusiness();
 		query = getQuery(QUERY_ALL);
 		if (query == null) {
 			query = new PreparedQuery(getConnection());
 			query.setSelectCount();
 			query.setPlacements(rb.getSchoolSeasonId());
 			query.setSchoolTypeHighSchool();
 			query.setSchool(); // parameter 1
 			query.setSchoolYearName(); // parameter 2
 			query.setStudyPathPrefix(); // parameter 3
 			query.prepare();
 			setQuery(QUERY_ALL, query);
 		}
 		query.setInt(1, ((Integer) school.getPrimaryKey()).intValue());
 		query.setString(2, schoolYearName);
 		query.setString(3, studyPathPrefix + "%");
 		return query.execute();
 	}
 	
 	/**
 	 * Returns the number of student placements for private high schools
 	 * for the specified study path prefix.
 	 */
 	protected int getHighSchoolOCCPlacementCount(String studyPathPrefix) throws RemoteException{
 		PreparedQuery query = null;
 		ReportBusiness rb = getReportBusiness();
 		query = getQuery(QUERY_OTHER_COMMUNES);
 		if (query == null) {
 			query = new PreparedQuery(getConnection());
 			query.setSelectCount();
 			query.setPlacements(rb.getSchoolSeasonId());
 			query.setNotNackaCitizens();
 			query.setSchoolTypeHighSchool();
			query.setSchools(rb.getPrivateHighSchools());
 			query.setStudyPathPrefix(); // parameter 1
 			query.prepare();
 			setQuery(QUERY_OTHER_COMMUNES, query);
 		}
 		query.setString(1, studyPathPrefix + "%");
 		return query.execute();
 	}	
 	
 	/**
 	 * Returns the number of student placements for private high schools
 	 * in Nacka commune for the specified school year.
 	 * Only citizens in Nacka commune are counted. 
 	 */
 	protected int getHighSchoolNackaCitizenPlacementCount(String studyPathPrefix) throws RemoteException{
 		PreparedQuery query = null;
 		ReportBusiness rb = getReportBusiness();
 		query = getQuery(QUERY_NACKA_COMMUNE);
 		if (query == null) {
 			query = new PreparedQuery(getConnection());
 			query.setSelectCount();
 			query.setPlacements(rb.getSchoolSeasonId());
 			query.setOnlyNackaCitizens();
 			query.setSchoolTypeHighSchool();
			query.setSchools(rb.getPrivateHighSchools());
 			query.setStudyPathPrefix(); // parameter 1
 			query.prepare();
 			setQuery(QUERY_NACKA_COMMUNE, query);
 		}
 		query.setString(1, studyPathPrefix + "%");
 		return query.execute();
 	}	
 }
