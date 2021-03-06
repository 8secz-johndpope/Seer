 package fr.cg95.cvq.business.request.ecitizen;
 
 import java.io.Serializable;
 import java.util.HashMap;
 import java.util.Map;
 
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

 import fr.cg95.cvq.business.request.RequestData;
 import fr.cg95.cvq.service.request.condition.IConditionChecker;
 
 /**
  * @author bor@zenexity.fr
  */
 @Deprecated
@Entity
@Table(name="vo_card_request")
 public class VoCardRequestData implements Serializable {
 
     private static final long serialVersionUID = 1L;
 
     public static final Map<String, IConditionChecker> conditions =
         new HashMap<String, IConditionChecker>(RequestData.conditions);
 
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
     private Long id;
 
     public final void setId(final Long id) {
         this.id = id;
     }
 
     public final Long getId() {
         return this.id;
     }
 }
