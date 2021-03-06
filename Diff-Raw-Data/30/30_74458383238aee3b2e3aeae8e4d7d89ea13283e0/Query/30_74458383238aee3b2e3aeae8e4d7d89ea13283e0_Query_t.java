 package org.apache.stanbol.factstore.model;
 
 import java.util.HashSet;
import java.util.List;
import java.util.Map;
 import java.util.Set;
 
 import org.apache.stanbol.commons.jsonld.JsonLd;
 import org.apache.stanbol.commons.jsonld.JsonLdIRI;
 import org.apache.stanbol.commons.jsonld.JsonLdResource;
 
 public class Query {
 
     public static final String SELECT = "SELECT";
     public static final String FROM = "FROM";
     public static final String WHERE = "WHERE";
 
     private Set<String> roles = new HashSet<String>();
     private String fromSchemaURN;
     private Set<WhereClause> whereClauses = new HashSet<WhereClause>();
 
     public Set<String> getRoles() {
         return roles;
     }
 
     public String getFromSchemaURN() {
         return fromSchemaURN;
     }
 
     
     public Set<WhereClause> getWhereClauses() {
         return whereClauses;
     }
 
     public static Query toQueryFromJsonLd(JsonLd jsonLdQuery) throws Exception {
         Query query = null;
         if (jsonLdQuery != null) {
             if (jsonLdQuery.getResourceSubjects() != null) {
                 if (jsonLdQuery.getResourceSubjects().size() == 1) {
                     JsonLdResource subject = jsonLdQuery.getResource(jsonLdQuery.getResourceSubjects()
                             .iterator().next());
                     if (subject.hasPropertyIgnorecase(SELECT)) {
                         if (subject.hasPropertyIgnorecase(FROM)) {
                             if (subject.hasPropertyIgnorecase(WHERE)) {
                                 query = new Query();
                                 handleSelect(subject, query);
                                 handleFrom(jsonLdQuery, subject, query);
                                 handleWhere(jsonLdQuery, subject, query);
                             }
                             else {
                                 throw new Exception("Query does not define a WHERE");
                             }
                         }
                         else {
                             throw new Exception("Query does not define a FROM");
                         }
                     }
                     else {
                         throw new Exception("Query does not define a SELECT");
                     }
                 }
                 else {
                     throw new Exception("Query does not consist of exactly 1 JSON-LD subject");
                 }
             }
         }
         return query;
     }
 
     private static void handleSelect(JsonLdResource subject, Query query) throws Exception {
        List<String> selects = (List<String>)subject.getPropertyValueIgnoreCase(SELECT);
        for (String role : selects) {
             query.roles.add(role);
         }
     }
 
     private static void handleFrom(JsonLd jsonLd, JsonLdResource subject, Query query) {
         query.fromSchemaURN = (String)subject.getPropertyValueIgnoreCase(FROM);
         query.fromSchemaURN = jsonLd.unCURIE(query.fromSchemaURN);
     }
 
     private static void handleWhere(JsonLd jsonLd, JsonLdResource subject, Query query) throws Exception {
        List<Map<String,Map<String,Object>>> wheres = (List)subject.getPropertyValueIgnoreCase(WHERE);
        for (Map<String,Map<String,Object>> whereObj : wheres) {
            for (String operator : whereObj.keySet()) {
                 if (operator.equalsIgnoreCase(CompareOperator.EQ.getLiteral())) {
                    Map<String,Object> compareValues = whereObj.get(operator);
                    if (compareValues.keySet().size() == 1) {
                        String comparedRole = (String)compareValues.keySet().iterator().next();
                         String searchedValue = null;
                         
                         Object searchedValueObj = compareValues.get(comparedRole);
                         if (searchedValueObj instanceof JsonLdIRI) {
                             JsonLdIRI iri = (JsonLdIRI) searchedValueObj;
                             searchedValue = jsonLd.unCURIE(iri.getIRI());
                         }
                         else {
                             searchedValue = searchedValueObj.toString();
                         }
                         
                         WhereClause wc = new WhereClause();
                         wc.setCompareOperator(CompareOperator.EQ);
                         wc.setComparedRole(comparedRole);
                         wc.setSearchedValue(searchedValue);
                         query.whereClauses.add(wc);
                     }
                 }
                 else {
                     throw new Exception("Unknown compare operator '" + operator + "' in WHERE clause");
                 }
             }
         }
     }
 
 }
