 package fr.ybo.transportsbordeaux.tbc;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.FileNotFoundException;
 import java.io.InputStreamReader;
 import java.io.Serializable;
 import java.net.HttpURLConnection;
 import java.net.SocketException;
 import java.net.SocketTimeoutException;
 import java.net.URL;
 import java.net.UnknownHostException;
 import java.util.Date;
 import java.util.List;
 
 import javax.xml.parsers.SAXParser;
 import javax.xml.parsers.SAXParserFactory;
 
 import org.xml.sax.SAXException;
 
 import fr.ybo.transportsbordeaux.modele.ArretFavori;
 import fr.ybo.transportsbordeaux.util.GestionnaireHoraires.CleHoraires;
 import fr.ybo.transportsbordeaux.util.LogYbo;
 
 @SuppressWarnings("serial")
 public class Horaire implements Serializable {
 
 	public String arretId;
 	public String ligneId;
 	public Integer horaire;
 	public String url;
 
 	private static final LogYbo LOG_YBO = new LogYbo(Horaire.class);
 
 	public static List<Horaire> getHoraires(CleHoraires cleHoraires) throws TbcErreurReseaux {
 		return getHoraires(cleHoraires.date, TcbConstantes.getUrlHoraire(cleHoraires.ligneId, cleHoraires.arretId,
 				cleHoraires.macroDirection == 0, cleHoraires.date), new GetHorairesHandler(cleHoraires.ligneId,
 				cleHoraires.arretId));
 	}
 
 	public static List<Horaire> getHoraires(Date date, ArretFavori favori) throws TbcErreurReseaux {
 		return getHoraires(date, TcbConstantes.getUrlHoraire(favori.ligneId, favori.arretId,
 				favori.macroDirection.intValue() == 0, date), new GetHorairesHandler(favori.ligneId, favori.arretId));
 	}
 
 	private static List<Horaire> getHoraires(Date date, String url, GetHorairesHandler handler) throws TbcErreurReseaux {
 		StringBuilder stringBuilder = new StringBuilder();
 		try {
 			// Récupération sur la page internet du table d'horaire.
 			LOG_YBO.debug(url);
 			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
 			connection.setRequestMethod("GET");
 			connection.setDoOutput(true);
 			connection.setConnectTimeout(60000);
 			connection.connect();
 			BufferedReader bufReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
 			stringBuilder = new StringBuilder();
 			boolean tableEnCours = false;
 			try {
 				String ligne = bufReader.readLine();
 				while (ligne != null) {
 					if (ligne.contains("navitia-timetable-detail")) {
 						tableEnCours = true;
 					}
 					if (tableEnCours) {
 						stringBuilder.append(ligne.replaceAll("&", "&#38;"));
 						if (ligne.contains("</table>")) {
 							break;
 						}
 					}
 					ligne = bufReader.readLine();
 				}
 			} finally {
 				bufReader.close();
 			}
			
			if (stringBuilder.length() == 0) {
				throw new TbcErreurReseaux("Aucun contenu trouvé...");
			}
 
 			// Parsing SAX du tableau d'horaires.
 			SAXParserFactory factory = SAXParserFactory.newInstance();
 			SAXParser parser = factory.newSAXParser();
 			parser.parse(new ByteArrayInputStream(stringBuilder.toString().getBytes()), handler);
 			return handler.getHoraires();
 		} catch (SocketTimeoutException socketException) {
 			throw new TbcErreurReseaux(socketException);
 		} catch (SocketException socketException) {
 			throw new TbcErreurReseaux(socketException);
 		} catch (SAXException saxException) {
 			throw new TbcErreurReseaux(saxException);
 		} catch (FileNotFoundException erreurReseau) {
 			throw new TbcErreurReseaux(erreurReseau);
 		} catch (UnknownHostException erreurReseau) {
 			throw new TbcErreurReseaux(erreurReseau);
		} catch (TbcErreurReseaux erreurReseau) {
			throw erreurReseau;
 		} catch (Exception exception) {
 			throw new TcbException("Erreur lors de la récupération des horaires pour l'url " + url
 					+ ", html récupéré : " + stringBuilder.toString(), exception);
 		}
 	}
 
 	public List<PortionTrajet> getTrajet() throws TbcErreurReseaux {
 		String urlTbc = TcbConstantes.URL_INFOS_TBC + url;
 		StringBuilder stringBuilder = new StringBuilder();
 		try {
 			// Récupération sur la page internet du table d'horaire.
 			LOG_YBO.debug(urlTbc);
 			HttpURLConnection connection = (HttpURLConnection) new URL(urlTbc).openConnection();
 			connection.setRequestMethod("GET");
 			connection.setDoOutput(true);
 			connection.setConnectTimeout(60000);
 			connection.connect();
 			BufferedReader bufReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
 			stringBuilder = new StringBuilder();
 			boolean tbodyEnCours = false;
 			try {
 				String ligne = bufReader.readLine();
 				while (ligne != null) {
 					LOG_YBO.debug(ligne);
 					if (ligne.contains("tbody")) {
 						tbodyEnCours = true;
 					}
 					if (tbodyEnCours) {
 						stringBuilder.append(ligne.replaceAll("&", "&#38;"));
 						if (ligne.contains("</tbody>")) {
 							break;
 						}
 					}
 					ligne = bufReader.readLine();
 				}
 			} finally {
 				bufReader.close();
 			}
 
 			// Parsing SAX du tableau d'horaires.
 			GetTrajetHandler handler = new GetTrajetHandler();
 			SAXParserFactory factory = SAXParserFactory.newInstance();
 			SAXParser parser = factory.newSAXParser();
 			parser.parse(new ByteArrayInputStream(stringBuilder.toString().getBytes()), handler);
 			return handler.getTrajet();
 		} catch (FileNotFoundException erreurReseau) {
 			throw new TbcErreurReseaux(erreurReseau);
 		} catch (UnknownHostException erreurReseau) {
 			throw new TbcErreurReseaux(erreurReseau);
 		} catch (SocketException erreurReseau) {
 			throw new TbcErreurReseaux(erreurReseau);
 		} catch (SocketTimeoutException erreurReseau) {
 			throw new TbcErreurReseaux(erreurReseau);
 		} catch (Exception exception) {
 			throw new TcbException("Erreur lors de la récupération des trajets pour l'url " + url
 					+ ", html récupéré : " + stringBuilder.toString(), exception);
 		}
 	}
 
 }
