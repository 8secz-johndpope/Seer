 package com.topsy.jmxproxy;
 
 import com.fasterxml.jackson.annotation.JsonProperty;
 
 import com.yammer.dropwizard.json.JsonSnakeCase;
 
 import javax.validation.constraints.Min;
 
 @JsonSnakeCase
 public class JMXProxyServiceConfiguration {
     @Min(1)
     @JsonProperty
     private int cleanInterval = 1;
 
     @Min(0)
     @JsonProperty
     private int cacheDuration = 5;
 
     @Min(1)
     @JsonProperty
     private int accessDuration = 30;
 
     public int getCleanInterval() {
         return cleanInterval;
     }
 
     public int getCacheDuration() {
         return cacheDuration;
     }
 
     public int getAccessDuration() {
         return accessDuration;
     }
 }
