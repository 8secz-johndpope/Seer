 package de.taimos.maven_redmine_plugin.model;
 
 import java.util.Date;
 
 import org.codehaus.jackson.annotate.JsonIgnore;
 import org.codehaus.jackson.map.annotate.JsonDeserialize;
 
 /**
  * @author hoegertn
  * 
  */
 public class Version implements Comparable<Version> {
 
 	@JsonDeserialize(using = DateDeserializer.class)
 	private Date created_on;
 
 	private String description;
 
 	private Integer id;
 
 	private String name;
 
 	private String status;
 
 	@JsonDeserialize(using = DateDeserializer.class)
 	private Date updated_on;
 
 	@JsonDeserialize(using = DateDeserializer.class)
 	private Date due_date;
 
 	/**
 	 * @return the created_on
 	 */
 	public Date getCreated_on() {
 		return this.created_on;
 	}
 
 	/**
 	 * @param created_on
 	 *            the created_on to set
 	 */
 	public void setCreated_on(final Date created_on) {
 		this.created_on = created_on;
 	}
 
 	/**
 	 * @return the description
 	 */
 	public String getDescription() {
 		return this.description;
 	}
 
 	/**
 	 * @param description
 	 *            the description to set
 	 */
 	public void setDescription(final String description) {
 		this.description = description;
 	}
 
 	/**
 	 * @return the id
 	 */
 	public Integer getId() {
 		return this.id;
 	}
 
 	/**
 	 * @param id
 	 *            the id to set
 	 */
 	public void setId(final Integer id) {
 		this.id = id;
 	}
 
 	/**
 	 * @return the name
 	 */
 	public String getName() {
 		return this.name;
 	}
 
 	/**
 	 * @param name
 	 *            the name to set
 	 */
 	public void setName(final String name) {
 		this.name = name;
 	}
 
 	/**
 	 * @return the status
 	 */
 	public String getStatus() {
 		return this.status;
 	}
 
 	/**
 	 * @param status
 	 *            the status to set
 	 */
 	public void setStatus(final String status) {
 		this.status = status;
 	}
 
 	/**
 	 * @return the updated_on
 	 */
 	public Date getUpdated_on() {
 		return this.updated_on;
 	}
 
 	/**
 	 * @param updated_on
 	 *            the updated_on to set
 	 */
 	public void setUpdated_on(final Date updated_on) {
 		this.updated_on = updated_on;
 	}
 
 	/**
 	 * @return the due_date
 	 */
 	public Date getDue_date() {
 		return this.due_date;
 	}
 
 	/**
 	 * @param due_date
 	 *            the due_date to set
 	 */
 	public void setDue_date(final Date due_date) {
 		this.due_date = due_date;
 	}
 
 	/**
 	 * @return the projectPrefix
 	 */
 	@JsonIgnore
 	public String getProjectPrefix() {
 		final int pos = this.name.indexOf("-");
 		if (pos == -1) {
 			return "";
 		}
 		return this.name.substring(0, pos);
 	}
 
 	/**
 	 * @return the numeric parts
 	 */
 	@JsonIgnore
 	public int[] getNumericParts() {
 		final int pos = this.name.indexOf("-");
 		if (pos == -1) {
 			return Version.splitVersion(this.name);
 		}
 		return Version.splitVersion(this.name.substring(pos + 1));
 	}
 
 	/**
 	 * @return the version as x.y.z without eventual prefix
 	 */
 	public String toVersionString() {
 		final int pos = this.name.indexOf("-");
 		if (pos == -1) {
 			return this.name;
 		}
 		return this.name.substring(pos + 1);
 	}
 
 	@Override
 	public int compareTo(final Version o) {
 		int comp = this.getProjectPrefix().compareTo(o.getProjectPrefix());
 		if (comp == 0) {
 			comp = Version.compareVersions(this.getNumericParts(), o.getNumericParts());
 		}
 		return comp;
 	}
 
 	private static int compareVersions(final String me, final String other) {
 		return Version.compareVersions(Version.splitVersion(me), Version.splitVersion(other));
 	}
 
 	private static int compareVersions(final int[] me, final int[] other) {
 		if (me[0] == other[0]) {
 			if (me[1] == other[1]) {
 				return me[2] - other[2];
 			}
 			return me[1] - other[1];
 		}
 		return me[0] - other[0];
 	}
 
 	private static int[] splitVersion(final String version) {
 		final String[] split = version.split("\\.");
		final int[] res = new int[3];
 		if (split.length > 3) {
 			throw new RuntimeException("Illegal version name");
 		}
 		switch (split.length) {
			case 3:
				res[2] = Integer.valueOf(split[2]);
			case 2:
				res[1] = Integer.valueOf(split[1]);
			case 1:
				res[0] = Integer.valueOf(split[0]);
 		}
 		return res;
 	}
 
 	/**
 	 * @param projectPrefix
 	 * @param version
 	 * @return [{projectPrefix}-]{version}
 	 */
 	public static String createName(final String projectPrefix, final String version) {
 		if ((projectPrefix != null) && !projectPrefix.isEmpty()) {
 			return projectPrefix + "-" + version;
 		}
 		return version;
 	}
 
 	/**
 	 * @param version
 	 * @return the version without -SNAPSHOT
 	 */
 	public static String cleanSnapshot(final String version) {
 		return version.replaceAll("-SNAPSHOT", "");
 	}
 
 	/**
 	 * @param args
 	 */
 	public static void main(final String[] args) {
 		System.out.println(Version.compareVersions("1.0.0", "1.1.0"));
 		System.out.println(Version.compareVersions("2.0.0", "1.1.0"));
 		System.out.println(Version.compareVersions("1.2.0", "1.1.0"));
 		System.out.println(Version.compareVersions("1.0.1", "1.1.0"));
		System.out.println(Version.compareVersions("1.1.1", "1.1.0"));
 	}
 
 }
