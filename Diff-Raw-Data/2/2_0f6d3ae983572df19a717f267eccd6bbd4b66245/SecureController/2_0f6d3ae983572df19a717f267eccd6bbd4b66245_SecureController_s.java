 package controllers.fap;
 
 import java.lang.reflect.Method;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.inject.Inject;
 
 import org.apache.ivy.util.Message;
 import org.apache.log4j.Logger;
 import org.apache.log4j.MDC;
 
 import config.InjectorConfig;
 
 
 import messages.Messages;
 import models.Agente;
 
 import org.apache.log4j.Logger;
 
 import platino.InfoCert;
 import play.Play;
 import play.cache.Cache;
 import play.data.validation.Required;
 import play.libs.Codec;
 import play.libs.Crypto;
 import play.mvc.Before;
 import play.mvc.Controller;
 import play.mvc.Http;
 import play.mvc.Scope;
 import play.mvc.Http.Request;
 import play.mvc.Scope.Params;
 import play.mvc.Scope.Session;
 import play.mvc.Util;
 import play.mvc.With;
 import properties.FapProperties;
 import security.Secure;
 import services.FirmaService;
 import services.FirmaServiceException;
 import ugot.recaptcha.Recaptcha;
 import ugot.recaptcha.RecaptchaCheck;
 import ugot.recaptcha.RecaptchaValidator;
 import utils.RoutesUtils;
 
 
 public class SecureController extends GenericController{
 
 	@Inject
 	private static FirmaService firmaService;
 	
 	private static Logger log = Logger.getLogger(SecureController.class);
 	
     // ~~~ Login
     public static void loginFap() {
     	//if (!buscarLoginOverwrite())
     		loginPorDefecto();
     }
     
 //    private static boolean buscarLoginOverwrite(){
 //    	Class invokedClass = getSecureClass();
 //    	Object object=null;
 //    	
 //    	if (invokedClass != null){
 //			Method method = null;
 //			try {
 //    			object = invokedClass.newInstance();
 //    			method = invokedClass.getDeclaredMethod("login");
 //    			if (method != null){
 //    				method.invoke(object);
 //    				return true;
 //    			}
 //    			else{
 //    				log.info("No existe el método login() en la clase "+invokedClass.getName());
 //    				return false;
 //    			}
 //			} catch (Exception e) {
 //				log.info("No se puede instanciar la clase propia de la aplicación que se encargará del login, por defecto se usará la autenticación de FAP");
 //				return false;
 //			}
 //    	} else {
 //    		log.info("No existe una clase en la aplicación que extienda de SecureController, por defecto se usará la autenticación de FAP");
 //    		return false;
 //    	}
 //    }
     
     @Util
     private static void loginPorDefecto(){
     	Http.Cookie remember = request.cookies.get("rememberme");
         if(remember != null && remember.value.indexOf("-") > 0) {
             String sign = remember.value.substring(0, remember.value.indexOf("-"));
             String username = remember.value.substring(remember.value.indexOf("-") + 1);
             if(Crypto.sign(username).equals(sign)) {
                 session.put("username", username);
                 redirectToOriginalURL();
             }
         }
         flash.keep("url");
         
         
         //Token para firmar y acceder por certificado
         if(FapProperties.getBoolean("fap.login.type.cert")){
         	String sessionid = Session.current().getId();
         	String token = Codec.UUID();
         	Cache.delete(sessionid + "login.cert.token");
         	Cache.add(sessionid + "login.cert.token",token, "5mn");
         	renderArgs.put("token", token);
         }
         //Messages.keep();
         renderTemplate("fap/Secure/login.html");
     }
     
     public static void authenticateCertificateFap(String certificado, String token, String firma){
     	checkAuthenticity();
     	if (!buscarAuthenticateCertificateOverwrite(certificado, token, firma))
     		authenticateCertificatePorDefecto(certificado, token, firma);
     }
     
     @Util
     private static boolean buscarAuthenticateCertificateOverwrite(String certificado, String token, String firma){
     	Class invokedClass = getSecureClass();
     	Object object=null;
     	
     	if (invokedClass != null){
 			Method method = null;
 			try {
     			object = invokedClass.newInstance();
     			method = invokedClass.getDeclaredMethod("authenticateCertificate", String.class, String.class, String.class);
     			if (method != null){
     				method.invoke(object, certificado, token, firma);
     				return true;
     			}
     			else{
     				log.info("No existe el método authenticateCertificate() en la clase "+invokedClass.getName());
     				return false;
     			}
 			} catch (Exception e) {
 				log.info("No se puede instanciar la clase propia de la aplicación que se encargará del authenticateCertificate, por defecto se usará la autenticación de FAP");
 				return false;
 			}
     	} else {
     		log.info("No existe una clase en la aplicación que extienda de SecureController, por defecto se usará la autenticación de FAP");
     		return false;
     	}
     }
    
     /**
      * Login con certificado electronico
      * @param certificado
      * @throws Throwable
      */
     @Util
     public static void authenticateCertificatePorDefecto(String certificado, String token, String firma) {
     	
     	if(!FapProperties.getBoolean("fap.login.type.cert")){
             flash.keep("url");
             Messages.error("El acceso a la aplicación mediante certificado electrónico está desactivado");
             Messages.keep();
             loginFap();   		
     	}
     	
     	String sessionid = Session.current().getId();
     	String serverToken = (String)Cache.get(sessionid + "login.cert.token");
     	
     	//Comprueba que el token firmado sea el correcto
     	if(!token.equals(serverToken)) validation.addError("login-certificado", "El token firmado no es correcto");
     	 
     	//Valida la firma
     	if(!validation.hasErrors()){
     	    try {
     	        boolean firmaCorrecta = firmaService.validarFirmaTexto(token.getBytes(), firma);
     	        if(!firmaCorrecta)
     	            validation.addError("login-certificado", "La firma no es válida");
     	    }catch(Exception e){
     	        validation.addError("login-certificado", "Error validando la firma");
     	    }
     	}
     	
     	//Obtiene información del certificado
         String username = null;
         String name = null;
         if (!validation.hasErrors()) {
             try {
                 InfoCert cert = firmaService.extraerCertificadoLogin(firma);
                 username = cert.getId();
                 name = cert.getNombreCompleto();
             } catch (FirmaServiceException e) {
                 log.error(e);
                 validation.addError("login-certificado", "El certificado no es válido");
             }
         }
 
     	
     	//Si hay errores redirige a la página de login
     	if(validation.hasErrors()){
             flash.keep("url");
             Messages.keep();
             loginFap();
     	}
     	
     	//Busca el agente en la base de datos, si no existe lo crea
 		Agente agente = Agente.find("byUsername", username).first();
 		if(agente == null){
 			log.debug("El agente no existe en la base de datos");
 			//El agente no existe, hay que crear uno nuevo
 			agente = new Agente();
 			agente.username = username;
 			agente.roles.add("usuario");
 			agente.rolActivo = "usuario";
 			agente.name = name;
 			
 		}else{
			if(agente.name == null || !agente.acceso.equals("certificado")){
 				agente.name = name;
 			}
 		}
 		
 		//Almacena el modo de acceso del agente
 		agente.acceso = "certificado";
 		agente.save();
 
 		//Almacena el usuario en la sesion
 		session.put("username", agente.username);
 		
 		redirectToOriginalURL();
     }    
 
     public static void authenticateFap(@Required String username, String password, boolean remember) throws Throwable {
     	checkAuthenticity();
     	if (!buscarAuthenticateOverwrite(username, password, remember))
     		authenticatePorDefecto(username, password, remember);
     }
     
     @Util
     private static boolean buscarAuthenticateOverwrite(String username, String password, boolean remember){
     	Class invokedClass = getSecureClass();
     	Object object=null;
     	
     	if (invokedClass != null){
 			Method method = null;
 			try {
     			object = invokedClass.newInstance();
     			method = invokedClass.getDeclaredMethod("authenticate", String.class, String.class, boolean.class);
     			if (method != null){
     				method.invoke(object, username, password, remember);
     				return true;
     			}
     			else{
     				log.info("No existe el método authenticate() en la clase "+invokedClass.getName());
     				return false;
     			}
 			} catch (Exception e) {
 				log.info("No se puede instanciar la clase propia de la aplicación que se encargará del authenticate, por defecto se usará la autenticación de FAP");
 				return false;
 			}
     	} else {
     		log.info("No existe una clase en la aplicación que extienda de SecureController, por defecto se usará la autenticación de FAP");
     		return false;
     	}
     }
     
     /**
      * Login con usuario y contraseña
      * @param username
      * @param password
      * @param remember
      * @throws Throwable
      */
     @Util
     public static void authenticatePorDefecto(String username, String password, boolean remember){
 
         int accesosFallidos = 0;
         if (session.get("accesoFallido") != null) {
         	accesosFallidos = new Integer(session.get("accesoFallido"));
         }
         
         if (accesosFallidos > 2) {
         	boolean valido = RecaptchaValidator.checkAnswer(Request.current(), Params.current());
         	if (valido == false){
         		flash.keep("url");
         		Messages.error(play.i18n.Messages.get("validation.recaptcha"));
         		Messages.keep();
         		loginFap();
         	}
     	}
 
         if(!FapProperties.getBoolean("fap.login.type.user")){
             flash.keep("url");
             Messages.error("El acceso a la aplicación mediante usuario y contraseña está desactivado");
             Messages.keep();
             loginFap();   		
     	}
     	    	
     	String cryptoPassword = Crypto.passwordHash(password);
     	log.info("Login con usuario y contraseña. User: " +  username + ", Pass: " + cryptoPassword);
     	// Check tokens
         Boolean allowed = false;
 
     	Agente agente = null;
     	if(username.contains("@")){
     		//Correo
     		agente = Agente.find("byEmail", username).first();
     	}else{
     		//Nip
     		agente = Agente.find("byUsername", username).first();
     	}
     	
     	log.debug("Agente encontrado " + agente);
     	
     	if(agente != null){
     		if(Play.mode.isDev()){
     			//En modo desarrollo se permite hacer login a cualquier usuario
     			allowed = true;
     		}else {
     	        /** Si uno de los passwords es vacío */
     			if (agente.password == null) {
     				allowed = false;
     	        	log.info("No se permite hacer password, porque en BBDD es vacío");
     			} else if ((password.trim().length() == 0)  || (agente.password.trim().length() == 0)) {
     	        	allowed = false;
     	        	log.info("Uno de los Passwords es vacío");
     	        } else {
     	        	log.info("Agente encontrado " + agente);
     				allowed = agente.password.equals(cryptoPassword);
     	        }
     		}
     	}else{
     		log.info("Agente no encontrado");
     	}
     	
     	log.debug("Allowed " + allowed);
     	
     	if(!allowed){
     		//Usuario no encontrado
     		log.warn("Intento de login fallido, user:"+ username+ ", pass:"+cryptoPassword+", IP:"+request.remoteAddress+", URL:"+request.url);
             accesosFallidos = 0;
             if (session.get("accesoFallido") != null) {
             	accesosFallidos = new Integer(session.get("accesoFallido"));
             }
             session.put("accesoFallido", accesosFallidos+1);
 
             flash.keep("url");
             Messages.error(play.i18n.Messages.get("fap.login.error.user"));
             Messages.keep();
             loginFap();
     	}
         
 		//Almacena el modo de acceso del agente
 		agente.acceso = "usuario";
 		agente.save();
 
         session.put("accesoFallido", 0);
 
         // Mark user as connected
         session.put("username", agente.username);
         // Remember if needed
         if(remember) {
             response.setCookie("rememberme", Crypto.sign(agente.username) + "-" + username, "30d");
         }
         
         // Redirect to the original URL (or /)
         redirectToOriginalURL();
     }
 
     public static void logoutFap() throws Throwable {
     	Cache.delete(session.getId());
         session.clear();
         response.removeCookie("rememberme");
         Messages.info(play.i18n.Messages.get("fap.logout.ok"));
         Messages.keep();
         redirect("fap.SecureController.loginFap");
     }
     
     @Util
     static void redirectToOriginalURL() {
         String url = flash.get("url");
         redirectToUrlOrOriginal(url);
         redirect(url);
     }
     
     static void redirectToUrlOrOriginal(String url) {
         if(url == null) {
             url = RoutesUtils.getDefaultRoute(); 
         }
         redirect(url);
     }
     
     private static Class getSecureClass() {
 		Class invokedClass = null;
 		//Busca una clase que herede del SecureController
         List<Class> assignableClasses = Play.classloader.getAssignableClasses(SecureController.class);
         if(assignableClasses.size() > 0){
             invokedClass = assignableClasses.get(0);
         }
 		return invokedClass;
 	}
 
     /**
      * Cambia el rol del usuario
      * Se comprueba que el usuario conectado tenga el rol que se quiera cambiar
      * @param url Dirección a la que redirigir
      * @param rol Rol nuevo
      */
     @Util
     public static void changeRol(String url, String rol){
     	checkAuthenticity();
     	AgenteController.getAgente().cambiarRolActivo(rol);
     	redirectToUrlOrOriginal(url);
     }
         
 }
