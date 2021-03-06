 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.
  */
 package org.apache.shindig.social.samplecontainer;
 
 import org.apache.shindig.social.ResponseItem;
 import org.apache.shindig.social.opensocial.PeopleService;
 import org.apache.shindig.social.opensocial.model.IdSpec;
 import org.apache.shindig.social.opensocial.model.Person;
 import org.apache.shindig.social.opensocial.model.ApiCollection;
 import org.apache.shindig.gadgets.GadgetToken;
 import org.json.JSONException;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Collections;
 import java.util.Comparator;
 
 public class BasicPeopleService implements PeopleService {
   private static final Comparator<Person> NAME_COMPARATOR
       = new Comparator<Person>() {
     public int compare(Person person, Person person1) {
       String name = person.getName().getUnstructured();
       String name1 = person1.getName().getUnstructured();
       return name.compareTo(name1);
     }
   };
 
   public ResponseItem<ApiCollection<Person>> getPeople(List<String> ids,
       SortOrder sortOrder, FilterType filter, int first, int max, GadgetToken token) {
     Map<String, Person> allPeople = XmlStateFileFetcher.get().getAllPeople();
 
     List<Person> people = new ArrayList<Person>();
     for (String id : ids) {
       Person person = allPeople.get(id);
       if (id.equals(token.getViewerId())) {
         person.setIsViewer(true);
       }
       if (id.equals(token.getOwnerId())) {
         person.setIsOwner(true);
       }
       people.add(person);
     }
 
     // We can pretend that by default the people are in top friends order
     if (sortOrder.equals(SortOrder.name)) {
       Collections.sort(people, NAME_COMPARATOR);
     }
 
     // TODO: The samplecontainer doesn't really have the concept of HAS_APP so
     // we can't support any filters yet. We should fix this.
 
     int totalSize = people.size();
     int last = first + max;
     people = people.subList(first, Math.min(last, totalSize));
 
     ApiCollection<Person> collection = new ApiCollection<Person>(people, first,
         totalSize);
     return new ResponseItem<ApiCollection<Person>>(collection);
   }
 
   public List<String> getIds(IdSpec idSpec, GadgetToken token)
       throws JSONException {
     Map<String, List<String>> friendIds
         = XmlStateFileFetcher.get().getFriendIds();
 
     List<String> ids = new ArrayList<String>();
     switch(idSpec.getType()) {
       case OWNER:
         ids.add(token.getOwnerId());
         break;
       case VIEWER:
         ids.add(token.getViewerId());
         break;
       case OWNER_FRIENDS:
         ids.addAll(friendIds.get(token.getOwnerId()));
         break;
       case VIEWER_FRIENDS:
        ids.addAll(friendIds.get(token.getOwnerId()));
         break;
       case USER_IDS:
         ids.addAll(idSpec.fetchUserIds());
         break;
     }
     return ids;
   }
 }
