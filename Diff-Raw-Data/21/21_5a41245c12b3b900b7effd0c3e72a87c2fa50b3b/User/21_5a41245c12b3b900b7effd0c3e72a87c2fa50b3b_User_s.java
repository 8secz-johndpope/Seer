 package models.casino;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 
 import play.modules.siena.EnhancedModel;
 import siena.Generator;
 import siena.Id;
 import siena.embed.Embedded;
 import casino.BCrypt;
 
 public class User extends EnhancedModel {
 
 	@Id(Generator.AUTO_INCREMENT)
 	public Long id;
 
 	public String email;
 
 	public String pwHash;
 
 	public String confirmationCode;
 
 	public String recoverPasswordCode;
 
 
 	/**
 	 * A simple String based role checking.
 	 * use methods hasRole / addRole / removeRole to manage roles.
 	 * 
 	 * You can use your own roles in your domain.
 	 * 
 	 * Examples would be: admin or superadmin
 	 * 
 	 * Siena saves roles as embeded Json
 	 */
 	@Embedded
 	private List<String> roles;
 	
	
 
 	public User(String email, String passwordHash, String confirmationCode) {
 
 		this.email = email;
 		this.pwHash = passwordHash;
 		this.confirmationCode = confirmationCode;
 
 		this.roles = new ArrayList<String>();
 	}
 
 	@Deprecated
 	public void setPasswordHash(String password) {
 
 		int saltFactor = Integer.parseInt(play.Play.configuration.getProperty(
 				"registration.salt_factor", "10"));
 		this.pwHash = BCrypt.hashpw(password, BCrypt.gensalt(saltFactor));
 
 	}
 
 	@Deprecated
 	public boolean isThisCorrectUserPassword(String plainTextPassword) {
 
 		return BCrypt.checkpw(plainTextPassword, pwHash);
 
 	}
 
 	public boolean hasRole(String role) {
 		
 		HashSet<String> rolesSet = new HashSet<String>(getRoles());
 
 		return rolesSet.contains(role);
 
 	}
 
 	public void addRole(String role) {
 
 		HashSet<String> rolesSet = new HashSet<String>(getRoles());
 		rolesSet.add(role);
 
 		getRoles().clear();
 		getRoles().addAll(new  ArrayList<String>(rolesSet));
 		
 
 	}
 
 	public void removeRole(String role) {
 
 		HashSet<String> rolesSet = new HashSet<String>(getRoles());
 		rolesSet.remove(role);
 
 		getRoles().clear();
 		getRoles().addAll(new  ArrayList<String>(rolesSet));
 
 	}
 	
 	private List<String> getRoles() {
 	
 		if (roles == null) {
 			roles = new ArrayList<String>();
 		}
 		
 		return roles;
 		
 	}
 
 
 
 }
