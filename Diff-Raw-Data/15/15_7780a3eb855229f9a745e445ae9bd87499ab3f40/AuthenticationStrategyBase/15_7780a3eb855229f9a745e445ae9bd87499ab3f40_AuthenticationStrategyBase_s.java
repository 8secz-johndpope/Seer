 package net.ripe.db.whois.update.authentication.strategy;
 
 import net.ripe.db.whois.common.rpsl.ObjectType;
 
 import java.util.Collections;
 import java.util.Set;
 
abstract class AuthenticationStrategyBase implements AuthenticationStrategy {
     @Override
     public String getName() {
         return getClass().getSimpleName();
     }
 
     @Override
     public Set<ObjectType> getTypesWithPendingAuthenticationSupport() {
         return Collections.emptySet();
     }
 }
