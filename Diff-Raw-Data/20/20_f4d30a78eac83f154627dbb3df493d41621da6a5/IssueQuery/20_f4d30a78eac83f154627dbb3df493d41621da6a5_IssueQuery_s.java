 /*
  * Sonar, open source software quality management tool.
  * Copyright (C) 2008-2012 SonarSource
  * mailto:contact AT sonarsource DOT com
  *
  * Sonar is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * Sonar is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with Sonar; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
  */
 
 package org.sonar.api.issue;
 
 import com.google.common.base.Preconditions;
 import org.apache.commons.lang.builder.ReflectionToStringBuilder;
 import org.sonar.api.rule.RuleKey;
 
import java.util.Date;
 import java.util.Collection;
 
 /**
  * TODO add javadoc
  *
  * @since 3.6
  */
 public class IssueQuery {
 
   private final Collection<String> keys;
   private final Collection<String> severities;
   private final Collection<String> statuses;
   private final Collection<String> resolutions;
   private final Collection<String> components;
   private final Collection<String> componentRoots;
   private final Collection<RuleKey> rules;
   private final Collection<String> userLogins;
   private final Collection<String> assigneeLogins;
   private final Date createdAfter;
   private final Date createdBefore;
   private final int limit, offset;
 
   private IssueQuery(Builder builder) {
     this.keys = builder.keys;
     this.severities = builder.severities;
     this.statuses = builder.statuses;
     this.resolutions = builder.resolutions;
     this.components = builder.components;
     this.componentRoots = builder.componentRoots;
     this.rules = builder.rules;
     this.userLogins = builder.userLogins;
     this.assigneeLogins = builder.assigneeLogins;
     this.createdAfter = builder.createdAfter;
     this.createdBefore = builder.createdBefore;
     this.limit = builder.limit;
     this.offset = builder.offset;
   }
 
   public Collection<String> keys() {
     return keys;
   }
 
   public Collection<String> severities() {
     return severities;
   }
 
   public Collection<String> statuses() {
     return statuses;
   }
 
   public Collection<String> resolutions() {
     return resolutions;
   }
 
   public Collection<String> components() {
     return components;
   }
 
   public Collection<String> componentRoots() {
     return componentRoots;
   }
 
   public Collection<RuleKey> rules() {
     return rules;
   }
 
   public Collection<String> userLogins() {
     return userLogins;
   }
 
   public Collection<String> assigneeLogins() {
     return assigneeLogins;
   }
 
   public Date createdAfter() {
     return createdAfter;
   }
 
   public Date createdBefore() {
     return createdBefore;
   }
 
   public int limit() {
     return limit;
   }
 
   public int offset() {
     return offset;
   }
 
   @Override
   public String toString() {
     return ReflectionToStringBuilder.toString(this);
   }
 
   public static Builder builder() {
     return new Builder();
   }
 
 
   /**
    * @since 3.6
    */
   public static class Builder {
     private static final int DEFAULT_LIMIT = 100;
     private static final int MAX_LIMIT = 5000;
     private static final int DEFAULT_OFFSET = 0;
 
     private Collection<String> keys;
     private Collection<String> severities;
     private Collection<String> statuses;
     private Collection<String> resolutions;
     private Collection<String> components;
     private Collection<String> componentRoots;
     private Collection<RuleKey> rules;
     private Collection<String> userLogins;
     private Collection<String> assigneeLogins;
     private Date createdAfter;
     private Date createdBefore;
     private int limit = DEFAULT_LIMIT;
     private int offset = DEFAULT_OFFSET;
 
     private Builder() {
     }
 
     public Builder keys(Collection<String> l) {
       this.keys = l;
       return this;
     }
 
     public Builder severities(Collection<String> l) {
       this.severities = l;
       return this;
     }
 
     public Builder statuses(Collection<String> l) {
       this.statuses = l;
       return this;
     }
 
     public Builder resolutions(Collection<String> l) {
       this.resolutions = l;
       return this;
     }
 
     public Builder components(Collection<String> l) {
       this.components = l;
       return this;
     }
 
     public Builder componentRoots(Collection<String> l) {
       this.componentRoots = l;
       return this;
     }
 
     public Builder rules(Collection<RuleKey> rules) {
       this.rules = rules;
       return this;
     }
 
     public Builder userLogins(Collection<String> l) {
       this.userLogins = l;
       return this;
     }
 
     public Builder assigneeLogins(Collection<String> l) {
       this.assigneeLogins = l;
       return this;
     }
 
     public Builder createdAfter(Date createdAfter) {
       this.createdAfter = createdAfter;
       return this;
     }
 
     public Builder createdBefore(Date createdBefore) {
       this.createdBefore = createdBefore;
       return this;
     }
 
    public Builder limit(Integer i) {
       Preconditions.checkArgument(i == null || i.intValue() > 0, "Limit must be greater than 0 (got " + i + ")");
       Preconditions.checkArgument(i == null || i.intValue() < MAX_LIMIT, "Limit must be less than " + MAX_LIMIT + " (got " + i + ")");
       this.limit = (i == null ? DEFAULT_LIMIT : i.intValue());
       return this;
     }
 
    public Builder offset(Integer i) {
       Preconditions.checkArgument(i == null || i.intValue() >= 0, "Offset must be greater than or equal to 0 (got " + i + ")");
       this.offset = (i == null ? DEFAULT_OFFSET : i.intValue());
       return this;
     }
 
     public IssueQuery build() {
       return new IssueQuery(this);
     }
   }
 }
