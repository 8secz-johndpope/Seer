 package org.powertac.server.module.accountingService;
 
 import org.powertac.common.commands.*;
import org.powertac.common.enumerations.CustomerType;
 import org.powertac.common.interfaces.AccountingService;
 
 import java.util.ArrayList;
import java.util.HashSet;
 import java.util.List;
import java.util.Set;
 
 public class AccountingServiceImpl implements AccountingService {
 
     @Override
     public List<MeterReadingBalanceCommand> processMeterReadings(List<MeterReadingCommand> meterReadingCommandList) {
         return null;  //To change body of implemented methods use File | Settings | File Templates.
     }
 
     @Override
     public DepotChangedCommand processDepotUpdate(DepotUpdateCommand depotUpdateCommand) {
         return null;  //To change body of implemented methods use File | Settings | File Templates.
     }
 
     @Override
     public CashChangedCommand processCashUpdate(CashUpdateCommand cashUpdateCommand) {
         return null;  //To change body of implemented methods use File | Settings | File Templates.
     }
 
     @Override
    public List<TariffPublishCommand> publishTariffList() {
        // Demo implementation. This should return a list of all currently stored tariffs.
        ArrayList<TariffPublishCommand> tariffList = new ArrayList<TariffPublishCommand>();
        Set<CustomerType> permittedCustomerTypes = new HashSet<CustomerType>();
        permittedCustomerTypes.add(CustomerType.ConsumerOffice);
        TariffPublishCommand tariffPublishedCommand = new TariffPublishCommand(permittedCustomerTypes, "testToken", 1l,1.0, 1.0, new Double[] {1.0, 1.0}, new Double[] {0.1, 0.1}, new org.joda.time.LocalDateTime(), new org.joda.time.LocalDateTime(), 1, 2, 1.0, 2.0, 3.0, 4.0);
         tariffList.add(tariffPublishedCommand);
         return(tariffList);
     }
 
     @Override
     public TariffReplyCommand processTariffReply(TariffReplyCommand tariffReplyCommand) {
         // Demo implementation. Should return same object, which is fine.
         return tariffReplyCommand;
     }
 
     @Override
     public List<CustomerInfoCommand> processCustomerInfo(List<CustomerInfoCommand> customerInfoCommands) {
         return null;  //To change body of implemented methods use File | Settings | File Templates.
     }
 }
