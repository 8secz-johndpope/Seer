 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package Persistence.JPA;
 
 import Model.IncomeType;
 import Persistence.Interfaces.IncomeTypeRepository;
import eapli.exception.EmptyList;
import java.util.List;
 
 /**
  *
  * @autor 1110186 & 1110590
  */
 public class IncomeTypeRepositoryImp implements IncomeTypeRepository{
 
     @Override
     public void save(IncomeType intType) {
         throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
     }

    @Override
    public List<IncomeType> getAllIncomeType() throws EmptyList {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
     
 }
