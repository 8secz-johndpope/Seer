 package controllers;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import javax.inject.Inject;
 
 import reports.Report;
 import services.FirmaService;
 import services.GestorDocumentalService;
 import services.GestorDocumentalServiceException;
 import services.NotificacionService;
 import services.RegistroService;
 
 import org.joda.time.DateTime;
 
 import platino.FirmaUtils;
 import play.mvc.Util;
 import properties.FapProperties;
 
 import tags.ComboItem;
 import utils.CalcularFirmantes;
 import utils.NotificacionUtils;
 import validation.CustomValidation;
 import verificacion.VerificacionUtils;
 
 import messages.Messages;
 import models.Agente;
 import models.Documento;
 import models.DocumentoNotificacion;
 import models.Firma;
 import models.Firmante;
 import models.Firmantes;
 import models.Notificacion;
 import models.Requerimiento;
 import models.SolicitudGenerica;
 import models.TipoDocumento;
 import models.Tramite;
 import models.Verificacion;
 import models.VerificacionDocumento;
 
 import controllers.fap.AgenteController;
 import controllers.fap.VerificacionFapController;
 import controllers.gen.PaginaVerificacionControllerGen;
 import emails.Mails;
 import enumerado.fap.gen.EstadoNotificacionEnum;
 import enumerado.fap.gen.EstadosDocumentoVerificacionEnum;
 import enumerado.fap.gen.EstadosSolicitudEnum;
 import enumerado.fap.gen.EstadosVerificacionEnum;
 import es.gobcan.eadmon.verificacion.ws.dominio.DocumentoVerificacion;
 import es.gobcan.platino.servicios.enotificacion.notificacion.NotificacionException;
 
 public class PaginaVerificacionController extends PaginaVerificacionControllerGen {
 	
     @Inject
     static FirmaService firmaService;
 
     @Inject
     static RegistroService registroService;
     
     @Inject
     static NotificacionService notificacionService;
     
 	@Inject
 	static GestorDocumentalService gestorDocumentalService;
 	
 	public static void index(String accion, Long idSolicitud, Long idVerificacion) {
 		if (accion == null)
 			accion = getAccion();
 		if (!permiso(accion)) {
 			Messages.fatal("No tiene suficientes privilegios para acceder a esta solicitud");
 			renderTemplate("gen/PaginaVerificacion/PaginaVerificacion.html");
 		}
 		
 		SolicitudGenerica solicitud = PaginaVerificacionController.getSolicitudGenerica(idSolicitud);
 
 		Verificacion verificacion = null;
 		if ("crear".equals(accion))
 			verificacion = PaginaVerificacionController.getVerificacion();
 		else if (!"borrado".equals(accion))
 			verificacion = PaginaVerificacionController.getVerificacion(idSolicitud, idVerificacion);
 
         if ((solicitud != null) && (solicitud.verificacion != null) && (solicitud.verificacion.estado != null)){
         	log.info("Visitando página: " + "gen/PaginaVerificacion/PaginaVerificacion.html");
         	renderTemplate("gen/PaginaVerificacion/PaginaVerificacion.html", accion, idSolicitud, idVerificacion, solicitud, verificacion);
         } else
         	redirect("AccesoVerificacionesController.index", accion, idSolicitud);
 		
 	}
 	
 	//Métodos en el controlador manual
 	public static List<ComboItem> getTramitesCombo () {
 		List<ComboItem> result = new ArrayList<ComboItem>();
 		List<Tramite> lTrams = Tramite.findAll();
 		for (Tramite t: lTrams) {
 			result.add(new ComboItem(t.uri, t.nombre));
 		}
 		return result;
 	}
 	
 	public static void iniciarVerificacion(Long idSolicitud, Long idVerificacion, Verificacion verificacion, String botonIniciarVerificacion) {
 		checkAuthenticity();
 		if (!permisoIniciarVerificacion("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 		Verificacion dbVerificacion = PaginaVerificacionController.getVerificacion(idSolicitud, idVerificacion);
 
 		PaginaVerificacionController.iniciarVerificacionBindReferences(verificacion);
 
 		if (!Messages.hasErrors()) {
 			dbVerificacion.uriTramite = verificacion.tramiteNombre.uri;
 			PaginaVerificacionController.iniciarVerificacionValidateCopy("editar", dbVerificacion, verificacion);
 		}
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.iniciarVerificacionValidateRules(dbVerificacion, verificacion);
 		}
 		if (!Messages.hasErrors()) {
 			dbVerificacion.estado = EstadosVerificacionEnum.verificandoTipos.name();
 			dbVerificacion.fechaUltimaActualizacion = new DateTime();
 			dbVerificacion.verificacionTiposDocumentos = VerificacionUtils.existDocumentosNuevos(dbVerificacion, idSolicitud);
 			SolicitudGenerica dbSolicitud = getSolicitudGenerica(idSolicitud);
 			dbSolicitud.estado=EstadosSolicitudEnum.enVerificacion.name();
 			dbVerificacion.save();
 			dbSolicitud.save();
 			log.info("Acción sobre iniciar verificacion de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción sobre iniciar verificacion de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.iniciarVerificacionRender(idSolicitud, idVerificacion);
 	}
 	
 	public static void tablaverificacionTipos(Long idSolicitud) {
 
 		java.util.List<Documento> rows = Documento.find("select documento from SolicitudGenerica solicitud join solicitud.verificacion.verificacionTiposDocumentos documento where solicitud.id=? and (documento.verificado is null or documento.verificado = false)",idSolicitud).fetch();
 
 		Map<String, Long> ids = (Map<String, Long>) tags.TagMapStack.top("idParams");
 		List<Documento> rowsFiltered = rows; //Tabla sin permisos, no filtra
 
 		tables.TableRenderResponse<Documento> response = new tables.TableRenderResponse<Documento>(rowsFiltered, false, false, false, "", "", "", getAccion(), ids);
 
 		renderJSON(response.toJSON("fechaSubida", "fechaRegistro", "tipo", "descripcionVisible", "verificado", "urlDescarga", "id"));
 	}
 	
 	public static void verificaTipos(Long idSolicitud, Long idVerificacion, String finalizaVerificarTipos) {
 		checkAuthenticity();
 		if (!permisoVerificaTipos("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.verificaTiposValidateRules();
 		}
 		if (!Messages.hasErrors()) {
 			SolicitudGenerica dbSolicitud = getSolicitudGenerica(idSolicitud);
 			
 			try {
 				dbSolicitud.verificacion.documentos = VerificacionUtils.getVerificacionDocumentosFromNewDocumentos((List<Documento>)VerificacionFapController.invoke("getNuevosDocumentosVerificar", dbSolicitud.verificacion.id, idSolicitud), dbSolicitud.verificacion.uriTramite, dbSolicitud.verificaciones, idSolicitud);
 			} catch (Throwable e) {
 				play.Logger.error("Error recuperando los documentos nuevos a verificar", e.getMessage());
 			}
 
 			dbSolicitud.verificacion.estado = EstadosVerificacionEnum.enVerificacion.name();
 			dbSolicitud.verificacion.nuevosDocumentos.clear();
 			dbSolicitud.verificacion.verificacionTiposDocumentos.clear();
 			dbSolicitud.verificacion.fechaUltimaActualizacion = new DateTime();
 			dbSolicitud.save();
 			Messages.ok("Finaliza la verificación de tipos");
 			log.info("Acción verificacion de tipos de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción verificacion de tipos de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.verificaTiposRender(idSolicitud, idVerificacion);
 	}
 	
 	public static void verificaTiposNuevosDoc(Long idSolicitud, Long idVerificacion, String finalizaVerificarTipos) {
 		checkAuthenticity();
 		if (!permisoVerificaTiposNuevosDoc("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.verificaTiposNuevosDocValidateRules();
 		}
 		if (!Messages.hasErrors()) {
 			SolicitudGenerica dbSolicitud = getSolicitudGenerica(idSolicitud);
 			for (Documento doc: dbSolicitud.verificacion.nuevosDocumentos){
 				VerificacionDocumento vDoc= new VerificacionDocumento(doc);
 				TipoDocumento tipo = TipoDocumento.find("select tipo from TipoDocumento tipo where tipo.tramitePertenece=? and tipo.uri=?", dbSolicitud.verificacion.uriTramite, doc.tipo).first();
 				if (tipo != null)
 					vDoc.identificadorMultiple = tipo.cardinalidad;
 				else
 					log.error("Tipo no encontrado al verificar los tipos de documentos nuevos: "+doc.tipo);
 				vDoc.existe = true;
 				vDoc.estadoDocumentoVerificacion = EstadosDocumentoVerificacionEnum.noVerificado.name();
 				vDoc.save();
 				dbSolicitud.verificacion.documentos.add(vDoc);
 			}
 			dbSolicitud.verificacion.estado = EstadosVerificacionEnum.enVerificacion.name();
 			dbSolicitud.verificacion.nuevosDocumentos.clear();
 			dbSolicitud.verificacion.verificacionTiposDocumentos.clear();
 			dbSolicitud.verificacion.fechaUltimaActualizacion = new DateTime();
 			dbSolicitud.save();
 			log.info("Acción sobre verificacion de tipos de nuevos documentos de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción sobre verificacion de tipos de nuevos documentos de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.verificaTiposNuevosDocRender(idSolicitud, idVerificacion);
 	}
 	
 	public static void nuevosDocumentos(Long idSolicitud, Long idVerificacion, String adjuntarNuevosDocumentos) {
 		checkAuthenticity();
 		if (!permisoNuevosDocumentos("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.nuevosDocumentosValidateRules();
 		}
 		if (!Messages.hasErrors()) {
 			SolicitudGenerica dbSolicitud = getSolicitudGenerica(idSolicitud);
 			List<Documento> documentosNuevos = VerificacionUtils.existDocumentosNuevos(dbSolicitud.verificacion, idSolicitud);
 			// Compruebo que no existen documentos nuevos aportados por el solicitante y que no esten incluidos en la verificacion actual
 			if (!documentosNuevos.isEmpty()){
 				dbSolicitud.verificacion.nuevosDocumentos.addAll(documentosNuevos);
 				dbSolicitud.verificacion.estado=EstadosVerificacionEnum.enVerificacionNuevosDoc.name();
 				dbSolicitud.verificacion.verificacionTiposDocumentos = VerificacionUtils.existDocumentosNuevos(dbSolicitud.verificacion, idSolicitud);
 				dbSolicitud.save();
 				Messages.info("Nuevos documentos aportados por el solicitante añadidos a la verificación actual. Verifique los tipos de estos documentos para proseguir con la verificación en curso.");
 			}
 			log.info("Acción sobre adjuntar Nuevos Documentos en página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción sobre adjuntar Nuevos Documentos en página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.nuevosDocumentosRender(idSolicitud, idVerificacion);
 	}
 	
 	public static void gnuevoRequerimientoBorradorPreliminar(Long idSolicitud, Long idVerificacion, String obtenerBorradorPreliminar) {
 		checkAuthenticity();
 		if (!permisoGnuevoRequerimientoBorradorPreliminar("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.gnuevoRequerimientoBorradorPreliminarValidateRules();
 		}
 		if (!Messages.hasErrors()) {
 			try {
 				SolicitudGenerica dbSolicitud = SolicitudGenerica.findById(idSolicitud);
 				new Report("reports/requerimiento.html").header("reports/header.html").footer("reports/footer-borrador.html").renderResponse(dbSolicitud);
 			} catch (Exception e) {
 				play.Logger.error("Error generando el borrador", e.getMessage());
 				Messages.error("Error generando el borrador");
 			} catch (Throwable e) {
 				play.Logger.error("Error generando el borrador", e.getMessage());
 				Messages.error("Error generando el borrador");
 			}
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.gnuevoRequerimientoBorradorPreliminarRender(idSolicitud, idVerificacion);
 	}
 	
 	@Util
 	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
 	public static void finalizarVerificacion(Long idSolicitud, Long idVerificacion, String btnFinalizarVerificacion) {
 		checkAuthenticity();
 		if (!permisoFinalizarVerificacion("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 
 		if (!Messages.hasErrors()) {
 			SolicitudGenerica dbSolicitud = getSolicitudGenerica(idSolicitud);
 			// Comprobamos que esten todos los documentos verificados
 			if (!VerificacionUtils.existsDocumentoNoVerificado(dbSolicitud.verificacion)){
 				// Si hay cosas que requerir, la verificación tiene causas subsanables
 				if (((dbSolicitud.verificacion.requerimiento.motivo != null) && (!dbSolicitud.verificacion.requerimiento.motivo.trim().isEmpty())) || (VerificacionUtils.documentosIncorrectos(dbSolicitud.verificacion))){
 					log.info("Hay que requerir y notificar, existe un motivo general de requerimiento o documentos en estado noValidos o noPresentados (Solicitud "+dbSolicitud.id+")");
 					Requerimiento requerimiento = dbSolicitud.verificacion.requerimiento;
 					if(!Messages.hasErrors()){
 						try {
 							String tipoDocumentoRequerimiento = FapProperties.get("fap.aed.tiposdocumentos.requerimiento");
 													
 							if((requerimiento.oficial != null) && (requerimiento.oficial.uri != null) && (!requerimiento.oficial.uri.trim().equals(""))){
 							    Documento oficialOld = requerimiento.oficial;
 							    requerimiento.oficial = null;
 							    requerimiento.save();
 							    gestorDocumentalService.deleteDocumento(oficialOld);
 							}						
 
 							//Genera el documento oficial
 							SolicitudGenerica solicitud = dbSolicitud;
 							File oficial =  new Report("reports/requerimiento.html").header("reports/header.html").registroSize().renderTmpFile(solicitud);
 							requerimiento.oficial = new Documento();
 							requerimiento.oficial.tipo = tipoDocumentoRequerimiento;
 							requerimiento.oficial.descripcion = "Requerimiento";
 							requerimiento.oficial.clasificado=false;
 							
 							gestorDocumentalService.saveDocumentoTemporal(requerimiento.oficial, new FileInputStream(oficial), oficial.getName());
 							
 							requerimiento.estado = "borrador";
 							requerimiento.save();
 							
 							
 							// Actualizamos los datos de la verificacion para verificaciones posteriores, en este caso el estado.
 							dbSolicitud.verificacion.estado = EstadosVerificacionEnum.enRequerimiento.name();
 							Messages.ok("Se deberá realizar un Requerimiento");
 						}catch(Exception e){
 							Messages.error("Se produjo un error generando el documento de requerimiento.");
 							play.Logger.error(e, "Error al generar el documento de requerimiento: " + e.getMessage());
 							e.printStackTrace();
 						}
 					}
 
 				} else { // Si la verificación ha ido correcta, no hay ninguna causa subsanable
 					log.info("La verificación se ha podido finalizar con éxito, todo es correcto");
 					Messages.ok("La verificación no tiene ningun requerimiento, finalizada correctamente y con éxito");
 					
 					// Ponemos todos los documentos de la verificacion como verificados, para que no se incluyan en sucesivas verificaciones
 					VerificacionUtils.setVerificadoDocumentos(dbSolicitud.verificacion.documentos, dbSolicitud.documentacion.documentos);
 					// Actualizamos los datos de la verificacion para verificaciones posteriores, en este caso el estado.
 					dbSolicitud.verificacion.estado = EstadosVerificacionEnum.verificacionPositiva.name();
 					
 					// Actualizamos los datos de la verificacion para verificaciones posteriores. Copiamos la verificacionActual a las verificaciones Anteriores para poder empezar una nueva verificación.
 					dbSolicitud.verificaciones.add(dbSolicitud.verificacion);
 					// Según el estado anterior de la Solicitud cambiamos a nuevo estado
 					if (dbSolicitud.estado.equals(EstadosSolicitudEnum.enVerifAceptadoRSLPROV.name()))
 						dbSolicitud.estado = EstadosSolicitudEnum.concedidoRSLPROV.name();
 					else if (dbSolicitud.estado.equals(EstadosSolicitudEnum.enVerifAceptadoRSLDEF.name()))
 						dbSolicitud.estado = EstadosSolicitudEnum.concedidoRSLDEF.name();
 					else
 						dbSolicitud.estado = EstadosSolicitudEnum.verificado.name();
 					dbSolicitud.save();
 				}
 
 				dbSolicitud.save();
 			} else {
 				Messages.error("Existen documentos aún por verificar, compruebe y verifiquelos para finalizar la Verificación Actual");
 			}
 		}
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.finalizarVerificacionValidateRules();
 		}
 		if (!Messages.hasErrors()) {
 
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.finalizarVerificacionRender(idSolicitud, idVerificacion);
 	}
 	
 	/**
 	 * Lista los gestores que pueden firmar el requerimiento
 	 * @return
 	 */
 	public static List<ComboItem> gestorAFirmar() {
 		List<ComboItem> result = new ArrayList<ComboItem>();
 		List<Agente> listaAgentes = Agente.findAll();
 		for (Agente ag : listaAgentes) {
 			List<String> roles = ag.getSortRoles();
 			for(String rol : roles){
 				if (rol.equals("gestor")){
 					result.add(new ComboItem(ag.username, ag.username +" - "+ag.name));
 				}
 			}
 		}
 		return result;
 	}
 
 	
 	@Util
 	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
 	public static void gRequerirFirmaRequerimiento(Long idSolicitud, Long idVerificacion, SolicitudGenerica solicitud, String requerirFirma) {
 		checkAuthenticity();
 		if (!permisoGRequerirFirmaRequerimiento("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 		SolicitudGenerica dbSolicitud = PaginaVerificacionController.getSolicitudGenerica(idSolicitud);
 		PaginaVerificacionController.gRequerirFirmaRequerimientoBindReferences(solicitud);
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.gRequerirFirmaRequerimientoValidateCopy("editar", dbSolicitud, solicitud);
 		}
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.gRequerirFirmaRequerimientoValidateRules(dbSolicitud, solicitud);
 			Messages.ok("Se estableció correctamente el firmante del Requerimiento");
 			dbSolicitud.verificacion.estado = EstadosVerificacionEnum.enRequerimientoFirmaSolicitada.name();
 			
 			// Se debe enviar el mail de "solicitarFirmaRequerimiento"
 			String mailRevisor = null;
 			String mailGestor = null;
 			try {
 				Agente revisor = AgenteController.getAgente();
 				mailRevisor = revisor.email;
				mailGestor = ((Agente) Agente.find("select agente from Agente agente where agente.username=?", solicitud.verificacion.requerimiento.firmante).first()).email;
 				play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("solicitud", solicitud);
 				play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("mailGestor", mailGestor);
 				play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("mailRevisor", mailRevisor);
 				Mails.enviar("solicitarFirmaRequerimiento", solicitud, mailGestor, mailRevisor);
 			} catch (Exception e) {
 				play.Logger.error("No se pudo enviar el mail solicitarFirmaRequerimiento a los mails: "+mailGestor+", "+mailRevisor+". Error: "+e.getMessage());
 			}
 		}
 		if (!Messages.hasErrors()) {
 			dbSolicitud.save();
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.gRequerirFirmaRequerimientoRender(idSolicitud, idVerificacion);
 	}
 	
 	@Util
 	public static void gRequerirFirmaRequerimientoValidateCopy(String accion, SolicitudGenerica dbSolicitud, SolicitudGenerica solicitud) {
 		CustomValidation.clearValidadas();
 		if (secure.checkGrafico("requerimientoRequerirFirma", "editable", accion, (Map<String, Long>) tags.TagMapStack.top("idParams"), null)) {
 			CustomValidation.valid("solicitud.verificacion.requerimiento", solicitud.verificacion.requerimiento);
 			CustomValidation.valid("solicitud.verificacion", solicitud.verificacion);
 			CustomValidation.valid("solicitud", solicitud);
 			CustomValidation.validValueFromTable("solicitud.verificacion.requerimiento.firmante", solicitud.verificacion.requerimiento.firmante);
 			dbSolicitud.verificacion.requerimiento.firmante = solicitud.verificacion.requerimiento.firmante;
 			
 			dbSolicitud.verificacion.requerimiento.registro.firmantes.todos = CalcularFirmantes.getGestorComoFirmante(solicitud.verificacion.requerimiento.firmante);
 			dbSolicitud.verificacion.requerimiento.registro.firmantes.save();
 			dbSolicitud.save();
 		}
 	}
 	
 	@Util
 	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
 	public static void gFirmarRequerimiento(Long idSolicitud, Long idVerificacion, String firma, String firmaRequerimiento) {
 		checkAuthenticity();
 		if (!permisoGFirmarRequerimiento("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 		SolicitudGenerica dbSolicitud = PaginaVerificacionController.getSolicitudGenerica(idSolicitud);
 
 		if (firmaRequerimiento != null) {
 			PaginaVerificacionController.firmaRequerimientoGFirmarRequerimiento(idSolicitud, idVerificacion, firma);
 			
 			// Si ya fue firmada y no ha sido registrada
 			if (dbSolicitud.verificacion.requerimiento.registro.fasesRegistro.firmada
 					&& !dbSolicitud.verificacion.requerimiento.registro.fasesRegistro.registro) {
 				try {
 
 					models.JustificanteRegistro justificanteSalida = registroService.registroDeSalida(dbSolicitud.solicitante, dbSolicitud.verificacion.requerimiento.oficial, dbSolicitud.expedientePlatino, "Requerimiento");
 					
 					// ----- Hecho por Paco ------------------------
 					dbSolicitud.verificacion.requerimiento.registro.informacionRegistro.setDataFromJustificante(justificanteSalida);
 					
 					Documento documento = dbSolicitud.verificacion.requerimiento.justificante;
 			        documento.tipo = FapProperties.get("fap.aed.tiposdocumentos.justificanteRegistroSalida");
 			        String aplicacionJ = "la aplicación";
 			        if ((FapProperties.get("fap.app.name.requerimiento.justificante.descripcion") != null) && (!"undefined".equals(FapProperties.get("fap.app.name.requerimiento.justificante.descripcion"))))
 			        	aplicacionJ = FapProperties.get("fap.app.name.requerimiento.justificante.descripcion");
 			        documento.descripcion = "Justificante de registro de requerimientos de la solicitud de "+aplicacionJ;
 			        documento.save();
 
 			        InputStream is = justificanteSalida.getDocumento().contenido.getInputStream();
 			        gestorDocumentalService.saveDocumentoTemporal(documento, is, "JustificanteRequerimiento" + dbSolicitud.verificacion.requerimiento.id + ".pdf");
 			        play.Logger.info("Justificante del Requerimiento almacenado en el AED");
 			        
 			        List<Documento> documentos = new ArrayList<Documento>();
 			        
 			        documentos.add(dbSolicitud.verificacion.requerimiento.justificante);
 			        
 			        try { // Sin registro
 		                gestorDocumentalService.clasificarDocumentos(dbSolicitud, documentos, true);
 		            } catch (Exception e) {
 		                play.Logger.error("No se ha podido clasificar el justificante del requerimiento: "+e.getMessage());
 		            }
 			        
 			        documentos.clear();
 			        documentos.add(dbSolicitud.verificacion.requerimiento.oficial);
 			        
 			        try { // Con registro
 		                gestorDocumentalService.clasificarDocumentos(dbSolicitud, documentos, dbSolicitud.verificacion.requerimiento.registro.informacionRegistro, true);
 		            } catch (Exception e) {
 		            	play.Logger.error("No se ha podido clasificar el requerimiento oficial: "+e.getMessage());
 		            }
 					
 			        // ------------------------------------------
 			        
 					play.Logger.info("Se ha registrado de Salida el documento del requerimiento de la solicitud "+dbSolicitud.id);
 					Messages.ok("Se ha registrado el Requerimiento correctamente.");
 					dbSolicitud.verificacion.requerimiento.registro.fasesRegistro.registro = true;
 					dbSolicitud.save();
 					
 				} catch (Exception e) {
 					Messages.error("No se ha podido registrar el requerimiento de la solicitud "+dbSolicitud.id);
 					play.Logger.error("No se ha podido registrar el requerimiento de la solicitud "+dbSolicitud.id+": "+e.getMessage());
 				}
 			}
 			
 			// Si ya fue registrada
 			if (dbSolicitud.verificacion.requerimiento.registro.fasesRegistro.registro) {
 				Notificacion notificacion = dbSolicitud.verificacion.requerimiento.notificacion;
 				if (notificacion.estado == null || notificacion.estado.isEmpty()) {
 					//La notificación no ha sido creada
 					DocumentoNotificacion docANotificar = new DocumentoNotificacion(dbSolicitud.verificacion.requerimiento.justificante.uri);
 					notificacion.documentosANotificar.add(docANotificar);
 					notificacion.interesados.addAll(dbSolicitud.solicitante.getAllInteresados());
 					notificacion.descripcion = "Notificación";
 					notificacion.plazoAcceso = FapProperties.getInt("fap.notificacion.plazoacceso");
 					notificacion.plazoRespuesta = FapProperties.getInt("fap.notificacion.plazorespuesta");
 					notificacion.frecuenciaRecordatorioAcceso = FapProperties.getInt("fap.notificacion.frecuenciarecordatorioacceso");
 					notificacion.frecuenciaRecordatorioRespuesta = FapProperties.getInt("fap.notificacion.frecuenciarecordatoriorespuesta");
 					notificacion.estado = EstadoNotificacionEnum.creada.name();
 					notificacion.idExpedienteAed = dbSolicitud.expedienteAed.idAed;
 					notificacion.asunto = "Notificación por Requerimiento";
 					notificacion.save();
 					dbSolicitud.save();
 				}
 
 			}
 			
 			PaginaVerificacionController.gFirmarRequerimientoRender(idSolicitud, idVerificacion);
 		}
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.gFirmarRequerimientoValidateRules(firma);
 		}
 		if (!Messages.hasErrors()) {
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.gFirmarRequerimientoRender(idSolicitud, idVerificacion);
 	}
 	
 	@Util
 	public static void firmaRequerimientoGFirmarRequerimiento(Long idSolicitud, Long idVerificacion, String firma) {
 		SolicitudGenerica solicitud = PaginaVerificacionController.getSolicitudGenerica(idSolicitud);
 
 		if (solicitud.verificacion.requerimiento.registro.firmantes.todos == null || solicitud.verificacion.requerimiento.registro.firmantes.todos.size() == 0) {
 			solicitud.verificacion.requerimiento.registro.firmantes.todos = CalcularFirmantes.getGestoresComoFirmantes();
 			solicitud.verificacion.requerimiento.registro.firmantes.save();
 		}
 		FirmaUtils.firmar(solicitud.verificacion.requerimiento.oficial, solicitud.verificacion.requerimiento.registro.firmantes.todos, firma, solicitud.verificacion.requerimiento.firmante);
 		
 		if (!Messages.hasErrors()) {
 			Messages.ok("El requerimiento se ha firmado correctamente");
 			
 			solicitud.verificacion.requerimiento.registro.fasesRegistro.firmada = true;
 			solicitud.save();
 		}
 	}
 	
 	@Util
 	// Este @Util es necesario porque en determinadas circunstancias crear(..) llama a editar(..).
 	public static void gPonerADisposicion(Long idSolicitud, Long idVerificacion, String ponerADisposicion) {
 		checkAuthenticity();
 		if (!permisoGPonerADisposicion("editar")) {
 			Messages.error("No tiene permisos suficientes para realizar la acción");
 		}
 
 		if (!Messages.hasErrors()) {
 			Agente gestor = AgenteController.getAgente();
 			SolicitudGenerica solicitud = getSolicitudGenerica(idSolicitud);
 			Notificacion notificacion = solicitud.verificacion.requerimiento.notificacion;
 		
 			if (notificacion.estado.equals(EstadoNotificacionEnum.creada.name()) &&
 					!notificacion.estado.equals(EstadoNotificacionEnum.enviada.name())) {
 				// TODO: Está en estado creada, debo notificarla
 				try {
 					notificacionService.enviarNotificaciones(notificacion, AgenteController.getAgente());
 					play.Logger.info("Se ha enviado correctamente la notificación "+notificacion.id);
 					// Los demás cambios en la notificación los hace el Servicio
 					notificacion.estado = EstadoNotificacionEnum.enviada.name();
 					notificacion.fechaPuestaADisposicion = new DateTime();
 					notificacion.save();
 					
 					solicitud.verificacion.estado = EstadosVerificacionEnum.enRequerido.name();
 					// Ponemos todos los documentos de la verificacion como verificados, para que no se incluyan en sucesivas verificaciones
 					VerificacionUtils.setVerificadoDocumentos(solicitud.verificacion.documentos, solicitud.documentacion.documentos);
 					// Actualizamos los datos de la verificacion para verificaciones posteriores. Copiamos la verificacionActual a las verificaciones Anteriores para poder empezar una nueva verificación.
 					solicitud.verificaciones.add(solicitud.verificacion);
 					
 					solicitud.save();
 					
 					try {			
 						play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("solicitud", solicitud);
 						
 						Mails.enviar("emitirRequerimiento", solicitud);
 					} catch (Exception e) {
 						play.Logger.error("No se pudo enviar el mail emitirRequerimiento: "+e.getMessage());
 					}
 					
 					if ((FapProperties.get("fap.notificacion.activa") != null) && (FapProperties.getBoolean("fap.notificacion.activa")) && (FapProperties.get("fap.notificacion.procedimiento") != null) && (!(FapProperties.get("fap.notificacion.procedimiento").trim().isEmpty())))
 			    		NotificacionUtils.recargarNotificacionesFromWS(FapProperties.get("fap.notificacion.procedimiento"));
 					
 				} catch (NotificacionException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 					play.Logger.error("No se ha podido enviar la notificación "+notificacion.id+": "+e.getMessage());
 					Messages.error("No se envío la notificación por problemas con la llamada al Servicio Web");
 				}
 			}
 		}
 
 		if (!Messages.hasErrors()) {
 			PaginaVerificacionController.gPonerADisposicionValidateRules();
 		}
 		if (!Messages.hasErrors()) {
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada con éxito");
 		} else
 			log.info("Acción Editar de página: " + "gen/PaginaVerificacion/PaginaVerificacion.html" + " , intentada sin éxito (Problemas de Validación)");
 		PaginaVerificacionController.gPonerADisposicionRender(idSolicitud, idVerificacion);
 	}
 	
 }
