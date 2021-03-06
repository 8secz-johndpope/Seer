 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package hu.sch.services;
 
 import hu.sch.domain.EntrantRequest;
 import hu.sch.domain.Group;
 import hu.sch.domain.Membership;
 import hu.sch.domain.User;
 import hu.sch.domain.PointRequest;
 import hu.sch.domain.SvieMembershipType;
 import hu.sch.domain.SvieStatus;
 import javax.ejb.Local;
 import java.util.Date;
 import java.util.List;
 
 /**
  * Felhasználó kezelés, lokális interfész
  * @author hege
  */
 @Local
 public interface UserManagerLocal {
 
     List<User> getAllUsers();
 
     void updateUserAttributes(User user);
 
     User findUserById(Long userId);
 
     User findUserWithCsoporttagsagokById(Long userId);
 
     void addUserToGroup(User user, Group group, Date membershipStart, Date membershipEnd);
 
     List<Group> getAllGroups();
 
     List<String> getEveryGroupName();
 
     Group getGroupHierarchy();
 
     List<Group> findGroupByName(String name);
 
     Group getGroupByName(String name);
 
     Group findGroupById(Long id);
 
     Group findGroupWithCsoporttagsagokById(Long id);
 
     void modifyMembership(User user, Group group, Date start, Date end);
 
     void deleteMembership(Membership ms);
 
     List<User> getCsoporttagok(Long csoportId);
 
     List<User> getCsoporttagokWithoutOregtagok(Long csoportId);
 
     List<EntrantRequest> getBelepoIgenyekForUser(User felhasznalo);
 
     List<PointRequest> getPontIgenyekForUser(User felhasznalo);
 
     void groupInfoUpdate(Group cs);
 
     Membership getCsoporttagsag(Long memberId);
 
     void setMemberToOldBoy(Membership user);
 
     void setOldBoyToActive(Membership cst);
 
     void updateUser(User user);
 }
