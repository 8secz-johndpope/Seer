 package devopsdistilled.operp.server.context.party;
 
 import javax.inject.Inject;
 
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.remoting.rmi.RmiServiceExporter;
 
 import devopsdistilled.operp.server.data.service.party.VendorService;
 
 @Configuration
 public class PartyRmiContext {
 
 	@Inject
 	private VendorService vendorService;
 
 	@Bean
 	public RmiServiceExporter rmiVendorServiceExporter() {
 		RmiServiceExporter rmiServiceExportor = new RmiServiceExporter();
 		rmiServiceExportor.setServiceName("VendorService");
 		rmiServiceExportor.setServiceInterface(VendorService.class);
 		rmiServiceExportor.setService(vendorService);
 		rmiServiceExportor.setRegistryPort(1099);
 		return rmiServiceExportor;
 
 	}
}
