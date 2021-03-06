 package edu.iastate.music.marching.attendance.util;
 
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.PrintWriter;
 
 import edu.iastate.music.marching.attendance.controllers.DataTrain;
 import edu.iastate.music.marching.attendance.model.User;
 import edu.iastate.music.marching.attendance.model.User.Grade;
 
 public class GradeExport {
 
 	public static final String CONTENT_TYPE_CSV = "text/csv";
 
 	public static void exportCSV(DataTrain train, OutputStream out)
 			throws IOException {
 		PrintWriter writer = new PrintWriter(out);
 
 		// Header
		writer.println("Email" + "\t" + "UniversityID" + "\t" + "Grade");
 
 		for (User student : train.getUsersController().get(User.Type.Student)) {
			
 			String line = "";
			line += student.getPrimaryEmail().getEmail();
 			line += "\t";
 			line += student.getUniversityID();
 			line += "\t";
 			line += sanitizeGrade(student.getGrade());
 
 			writer.println(line);
 		}
 
 		writer.println("");
 		writer.flush();
 		writer.close();
 		out.flush();
 	}
 
 	private static String sanitizeGrade(Grade grade) {
 		if (grade == null) {
 			return "Undetermined";
 		} else {
 			return grade.getDisplayName();
 		}
 	}
 }
