 package controllers;
 
 import java.io.File;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.OutputStreamWriter;
 import java.net.HttpURLConnection;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.UUID;
 
 import jobs.ClipboardExporter;
 import jobs.Downloader;
 import jobs.Downloader.Format;
 import models.Instance;
 import models.Person;
 import models.Project;
 import models.ProjectAssociation;
 import models.Series;
 import models.Study;
 import notifiers.Mail;
 
 import org.apache.commons.lang.StringUtils;
 import org.dcm4che.data.Dataset;
 
 import play.Invoker;
 import play.Logger;
 import play.Play;
 import play.cache.Cache;
 import play.libs.Files;
 import play.libs.IO;
 import play.mvc.Before;
 import play.mvc.Finally;
 import util.Clipboard;
 import util.Dicom;
 import util.Medcon;
 import util.PersistentLogger;
 import util.Properties;
 import au.com.bytecode.opencsv.CSVReader;
 import au.com.bytecode.opencsv.CSVWriter;
 import controllers.Secure.Security;
 
 public class Application extends SecureController {
 	//session variables
 	public static final String CLIPBOARD = "clipboard";
 	public static final String EXPORTS = "exports";
 
 	@Before
 	static void before() {
 		if (Security.isConnected()) {
 			renderArgs.put(CLIPBOARD, new Clipboard(getUser().clipboard));
 			renderArgs.put(EXPORTS, ClipboardExporter.getExports(session));
 		}
 	}
 
 	public static void index() {
 		render();
 	}
 
 	public static void recent() {
 		render();
 	}
 
 	public static void help() {
 		render();
 	}
 
 	@Check("admin")
 	public static void audit(File spreadsheet) throws IOException, ParseException {
 		if (spreadsheet == null) {
 			render();
 		}
 		response.contentType = "text/csv";
 		response.setHeader("Content-Disposition", "attachment; filename='audit.csv'");
 		CSVReader reader = new CSVReader(new FileReader(spreadsheet));
 		CSVWriter writer = new CSVWriter(new OutputStreamWriter(response.out));
 		String[] headers = reader.readNext();
 		writer.writeNext(headers);
 		String[] line = null;
 		while ((line = reader.readNext()) != null) {
 			String pat_id = line[3].toLowerCase();
 			Date study_datetime = new SimpleDateFormat("dd/MM/yyyy").parse(line[9]);
 			Study study = Study.find("lower(patient.pat_id) = ? and cast(study_datetime as date) = ?", pat_id, study_datetime).first();
 			if (study != null) {
 				line[10] = "Yes";
 				Project project = Project.find("byName", line[1]).first();
 				if (project == null) {
 					project = new Project(line[1]).save();
 				}
 				associate(study, project, line[2]);
 			} else {
 				line[10] = "No";
 			}
 			Boolean singleFrames = null;
 			for (int i = 12; i < headers.length; i++) {
 				String series_desc = headers[i].toLowerCase();
 				Series series = Series.find("from Series where lower(series_desc) = ? and lower(study.patient.pat_id) = ? and cast(study.study_datetime as date) = ?", series_desc, pat_id, study_datetime).first();
 				if (series != null) {
 					line[i] = "Yes";
 					singleFrames = true;
 					singleFrames &= Dicom.singleFrames(series).size() > 0;
 				} else {
 					line[i] = "No";
 				}
 				line[i] = series != null ? "Yes" : "No";
 			}
 			line[11] = Boolean.TRUE.equals(singleFrames) ? "Yes" : "No";
 			writer.writeNext(line);
 		}
 		reader.close();
 		writer.close();
 	}
 
 	private static Map<String, String> comparators = new HashMap<String, String>() {{
 		put("before", "<=");
 		put("on", "=");
 		put("after", ">");
 		put("since", ">");
 	}};
 	public static void studies(String name, String id, Integer age, Character sex, String protocol, String acquisition, String study, int page, String order, String sort, Long project, String participationID) throws Exception {
 		List<String> from = new ArrayList<String>();
 		from.add("Study study");
 
 		List<String> where = new ArrayList<String>();
 		List<Object> args = new ArrayList<Object>();
 
 		if (!name.isEmpty()) {
 			where.add("lower(study.patient.pat_name) like ?");
 			args.add("%" + name.toLowerCase() + "%");
 		}
 		if (!id.isEmpty()) {
 			where.add("(lower(study.patient.pat_id) like ? or lower(study.study_custom1) like ?)");
 			args.add("%" + id.toLowerCase() + "%");
 			args.add("%" + id.toLowerCase() + "%");
 		}
 		if (age != null) {
 			where.add("cast(study.study_datetime as date) - cast(study.patient.pat_birthdate as date) >= ? and cast(study.study_datetime as date) - cast(study.patient.pat_birthdate as date) < ?");
 			args.add(365D * age);
 			args.add(365D * (age + 1));
 		}
 		if (sex != null) {
 			where.add("study.patient.pat_sex = ?");
 			args.add(sex);
 		}
 		if (!protocol.isEmpty()) {
 			from.add("in(study.series) series");
 			where.add("lower(series.series_custom1) like ?");
 			args.add("%" + protocol.toLowerCase() + "%");
 		}
 		if (!study.isEmpty()) {
 			where.add("lower(study.study_desc) like ?");
 			args.add("%" + study.toLowerCase() + "%");
 		}
 		if (!acquisition.isEmpty()) {
 			//where.add(String.format("(study_datetime is null or cast(study_datetime as date) %s ?)", comparators.get(acquisition)));
 			where.add(String.format("cast(study.study_datetime as date) %s ?", comparators.get(acquisition)));
 			args.add(params.get(acquisition, Date.class));
 		}
 		if (project != null || !participationID.isEmpty()) {
 			from.add("in (study.projectAssociations) association");
 			if (project != null) {
 				where.add("association.project.id = ?");
 				args.add(project);
 			}
 			if (!participationID.isEmpty()) {
 				where.add("association.participationID = ?");
 				args.add(participationID);
 			}
 		}
 
 		String query = "select study from " + StringUtils.join(from, ", ");
 		if (!where.isEmpty()) {
 			query += " where " + StringUtils.join(where, " and ");
 		}
 		query += " order by " + "study." + (order.isEmpty() ? "patient.pk" : order) + " " + ("desc".equals(sort) ? "desc" : "asc");
 		List<Study> studies = Study.find(query, args.toArray()).fetch(page + 1, Properties.pageSize());
 		int studyCount = Study.find(query, args.toArray()).fetch().size();
 		render(studies, studyCount, page);
 	}
 
 	public static void study(long pk) {
		long time = System.currentTimeMillis();;
		System.out.println("0 " + (System.currentTimeMillis() - time));
 		Study study = Study.findById(pk);
		System.out.println("1 " + (System.currentTimeMillis() - time));
 		ProjectAssociation projectAssociation = study.getProjectAssociation();
		System.out.println("2 " + (System.currentTimeMillis() - time));
		Set<Series> serieses = new HashSet();
		for (Series series : study.series) {
			if (Dicom.downloadable(series)) {
				serieses.add(series);
			}
		}
		System.out.println("3 " + (System.currentTimeMillis() - time));
		render(study, projectAssociation, serieses);
 	}
 
 	public static void series(long pk) throws IOException {
 		Series series = Series.findById(pk);
 		Instance instance = Dicom.multiFrame(series);
 		if (instance == null) {
 			Collection instances = Dicom.singleFrames(series);
 			instance = instances.size() > 0 ? (Instance) instances.iterator().next() : Dicom.spectrogram(series);
 		}
 		Dataset dataset = Dicom.dataset(Dicom.file(instance));
 		Set<String> echoes = Dicom.echoes(dataset);
 		render(series, dataset, echoes);
 	}
 
 	public static void image(long pk, Integer columns) throws MalformedURLException, IOException {
 		Series series = Series.findById(pk);
 		if (!Dicom.renderable(series)) {
 			renderBinary(new File(Play.applicationPath, "public/images/spectrogram.png"));
 		}
 		int frameNumber;
 		String objectUID = null;
 		Instance instance = Dicom.multiFrame(series);
 		if (instance != null) {
 			frameNumber = Dicom.numberOfFrames(series) / 2 + 1;
 			objectUID = instance.sop_iuid;
 		} else {
 			Object[] instances = Dicom.singleFrames(series).toArray(new Instance[0]);
 			if (instances.length == 0) {
 				renderBinary(new File(Play.applicationPath, "public/images/128x128.gif"));
 			}
 			frameNumber = 1;
 			Arrays.sort(instances, new Comparator() {
 				@Override
 				public int compare(Object o1, Object o2) {
 					return Integer.valueOf(((Instance) o1).inst_no).compareTo(Integer.valueOf(((Instance) o2).inst_no));
 				}
 			});
 			objectUID = ((Instance) instances[instances.length / 2]).sop_iuid;
 		}
 		//columns=256 matches prefetch configuration
 		String url = String.format("http://%s:8080/wado?requestType=WADO&studyUID=&seriesUID=&objectUID=%s&frameNumber=%s&columns=%s", request.domain, objectUID, frameNumber, columns == null ? 256 : columns);
 		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
 		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
 			Logger.warn("Image not found for %s", series);
 			renderBinary(new File(Play.applicationPath, "public/images/missing.png"));
 		}
 		IO.copy(new URL(url).openConnection().getInputStream(), response.out);
 	}
 
 	public static void download(long[] pk, Format format) throws InterruptedException, IOException {
 		PersistentLogger.log("downloaded series %s", Arrays.toString(pk));
 		File tmpDir = new File(Properties.getDownloads(), UUID.randomUUID().toString());
 		tmpDir.mkdir();
 		await(new Downloader(pk, format == null ? Format.dcm : format, tmpDir).now());
 		File zip = new File(tmpDir, String.format("%s.zip", tmpDir.listFiles()[0].getName()));
 		Files.zip(tmpDir.listFiles()[0], zip);
 		renderBinary(zip);
 	}
 
 	public static void export(String password) throws InterruptedException, IOException, ClassNotFoundException {
 		PersistentLogger.log("exported clipboard %s", getUser().clipboard);
 		Clipboard clipboard = (Clipboard) renderArgs.get(CLIPBOARD);
 		File tmpDir = new File(Properties.getDownloads(), UUID.randomUUID().toString());
 		tmpDir.mkdir();
 		new ClipboardExporter(clipboard, tmpDir, password, session, getUser().username).now();
 		clipboard(null, null, null);
 	}
 
 	public static void retrieve(String filename) {
 		File download = ClipboardExporter.getExport(filename, session);
 		notFoundIfNull(download);
 		renderBinary(download);
 	}
 
 	public enum ClipboardOp { ADD, REMOVE, CLEAR }
 	public static void clipboard(ClipboardOp op, String type, Long pk) throws ClassNotFoundException {
 		Clipboard clipboard = (Clipboard) renderArgs.get(CLIPBOARD);
 		if (op != null) {
 			switch (op) {
 			case ADD: clipboard.add(type, pk); break;
 			case REMOVE: clipboard.remove(type, pk); break;
 			case CLEAR: clipboard.clear(); break;
 			}
 			Person person = getUser();
 			person.clipboard = clipboard.toString();
 			person.merge()._save();
 			Cache.delete(Security.connected().toLowerCase());
 		}
 		render();
 	}
 
 	public static void imagej(long pk) throws InterruptedException, IOException {
 		Series series = Series.findById(pk);
 
 		File tmpDir = new File(Properties.getDownloads(), UUID.randomUUID().toString());
 		tmpDir.mkdir();
 
 		File dcm;
 		Instance instance = Dicom.multiFrame(series);
 		if (instance != null) {
 			await(new Downloader(new long[] { pk }, Format.dcm, tmpDir).now());
 			dcm = tmpDir.listFiles()[0].listFiles()[0].listFiles()[0];
 		} else {
 			dcm = new File(tmpDir, String.format("%s.dcm", series.pk));
 			Medcon.convert(Dicom.collate(series), Format.dcm, dcm);
 		}
 		renderBinary(dcm);
 	}
 
 	public static void associate(Study study, Long projectID, String participationID, String projectName) {
 		Project project = null;
 		if (!projectName.isEmpty()) {
 			project = new Project(projectName).save();
 		} else if (projectID != null) {
 			project = Project.findById(projectID);
 		}
 		associate(study, project, participationID);
 		PersistentLogger.log("study %s linked to project: %s (%s)", study.pk, project, participationID);
 		redirect(request.headers.get("referer").value());
 	}
 
 	static void associate(Study study, Project project, String participationID) {
 		ProjectAssociation association = ProjectAssociation.find("byStudy", study).first();
 		if (project == null) {
 			if (association != null) {
 				association.delete();
 			}
 		} else {
 			if (association != null) {
 				association.project = project;
 			} else {
 				association = new ProjectAssociation(project, study);
 			}
 			association.participationID = participationID;
 			association.save();
 		}
 	}
 
 	@Finally
 	static void log(Throwable e) {
 		if (e != null && !(e instanceof Invoker.Suspend) && Properties.getString("mail.from") != null) {
 			Mail.exception(request, session, e);
 		}
 	}
 }
