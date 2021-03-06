 package pt.go2.application;
 
 import java.io.BufferedWriter;
 import java.io.FileWriter;
 import java.io.IOException;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 
 import pt.go2.fileio.Configuration;
import pt.go2.fileio.Statistics;

 import com.sun.net.httpserver.BasicAuthenticator;
 import com.sun.net.httpserver.HttpHandler;
 import com.sun.net.httpserver.HttpServer;
 
 class Server {
 
 	private static final Logger LOG = LogManager.getLogger(Server.class);
 
 	/**
 	 * Process initial method
 	 */
 	public static void main(final String[] args) {
 
 		final Configuration config = new Configuration();
 
 		LOG.trace("Starting server...");
 
 		// log server version
 
 		LOG.trace("Preparing to run " + config.VERSION + ".");
 
 		LOG.trace("Resuming DB from folder: " + config.DATABASE_FOLDER);
 
 		// create listener
 
 		LOG.trace("Creating listener.");
 
 		final HttpServer listener;
 		try {
 
 			listener = HttpServer.create(config.HOST, config.BACKLOG);
 
 		} catch (IOException e) {
 			LOG.fatal("Could not create listener.");
 			return;
 		}
 
 		LOG.trace("Appending to access log.");
 
 		LOG.trace("Starting virtual file system.");
 
 		// Generate VFS
 
 		final Resources vfs = new Resources();
 
 		if (!vfs.start(config)) {
 			return;
 		}
 
		final Statistics statistics;
 		try {
			statistics = new Statistics(config);
 		} catch (IOException e1) {
 			LOG.fatal("Can't collect statistics.");
 			return;
 		}
 
 		BufferedWriter accessLog = null;
 
 		try {
 
 			// start access log
 
 			try {
 
 				final FileWriter file = new FileWriter(config.ACCESS_LOG, true);
 				accessLog = new BufferedWriter(file);
 			} catch (IOException e) {
 				System.out.println("Access log redirected to console.");
 			}
 
 			// RequestHandler
 
 			final BasicAuthenticator ba = new BasicAuthenticator("Statistics") {
 
 				@Override
 				public boolean checkCredentials(final String user,
 						final String pass) {
 
 					LOG.info("login: [" + user + "] | [" + pass + "]");
 
 					LOG.info("required: [" + config.STATISTICS_USERNAME
 							+ "] | [" + config.STATISTICS_PASSWORD
 							+ "]");
 
 					return user.equals(config.STATISTICS_USERNAME)
 							&& pass.equals(config.STATISTICS_PASSWORD
 									.trim());
 				}
 			};
 			
 			final HttpHandler root = new StaticPages(config, vfs, statistics,
 					accessLog);
 			final HttpHandler novo = new UrlHashing(config, vfs, accessLog);
 
 			final HttpHandler stats = new Analytics(config, vfs, statistics,
 					accessLog);
 
 			final HttpHandler browse = new Browse(config, vfs, accessLog);
 
 			listener.createContext("/", root);
 
 			listener.createContext("/new", novo);
 
 			listener.createContext("/stats", stats).setAuthenticator(ba);
 			listener.createContext("/browse", browse).setAuthenticator(ba);
 
 			listener.setExecutor(null);
 
 			// start server
 
 			listener.start();
 
 			LOG.trace("Listener is Started.");
 
 			System.out.println("Server Running. Press [k] to kill listener.");
 			boolean running = true;
 			do {
 
 				try {
 					running = System.in.read() == 'k';
 				} catch (IOException e) {
 				}
 
 			} while (running);
 
 			LOG.trace("Server stopping.");
 
 			listener.stop(1);
 
 		} finally {
 			try {
 				if (accessLog != null) {
 					accessLog.close();
 				}
 			} catch (IOException e) {
 			}
 			LOG.trace("Server stopped.");
 		}
 	}
 
 	/**
 	 * Private c'tor to forbid instantiation of utility class
 	 */
 	private Server() {
 	}
 }
