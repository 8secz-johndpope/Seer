 /*
  * SonarQube, open source software quality management tool.
  * Copyright (C) 2008-2013 SonarSource
  * mailto:contact AT sonarsource DOT com
  *
  * SonarQube is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * SonarQube is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package org.sonar.batch.components;
 
 import com.google.common.base.Strings;
 import com.google.common.collect.ImmutableList;
 import org.apache.commons.configuration.Configuration;
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.sonar.api.BatchExtension;
 import org.sonar.api.CoreProperties;
 import org.sonar.api.config.PropertyDefinition;
 import org.sonar.api.config.Settings;
 import org.sonar.api.database.model.Snapshot;
 import org.sonar.api.resources.Qualifiers;
 
import javax.annotation.Nullable;

 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.List;
 
 public class PastSnapshotFinder implements BatchExtension {
 
   private static final Logger LOG = LoggerFactory.getLogger(PastSnapshotFinder.class);
 
   private PastSnapshotFinderByDays finderByDays;
   private PastSnapshotFinderByVersion finderByVersion;
   private PastSnapshotFinderByDate finderByDate;
   private PastSnapshotFinderByPreviousAnalysis finderByPreviousAnalysis;
   private PastSnapshotFinderByPreviousVersion finderByPreviousVersion;
 
   public PastSnapshotFinder(PastSnapshotFinderByDays finderByDays, PastSnapshotFinderByVersion finderByVersion,
                             PastSnapshotFinderByDate finderByDate, PastSnapshotFinderByPreviousAnalysis finderByPreviousAnalysis,
                             PastSnapshotFinderByPreviousVersion finderByPreviousVersion) {
     this.finderByDays = finderByDays;
     this.finderByVersion = finderByVersion;
     this.finderByDate = finderByDate;
     this.finderByPreviousAnalysis = finderByPreviousAnalysis;
     this.finderByPreviousVersion = finderByPreviousVersion;
   }
 
   /**
    * @deprecated since 3.6. Replaced by {@link #find(Snapshot projectSnapshot, String rootQualifier, Settings settings, int index) }
    */
   @Deprecated
   public PastSnapshot find(Snapshot projectSnapshot, Configuration conf, int index) {
     String propertyValue = getPropertyValue(conf, index);
     PastSnapshot pastSnapshot = find(projectSnapshot, index, propertyValue);
     if (pastSnapshot == null && StringUtils.isNotBlank(propertyValue)) {
       LoggerFactory.getLogger(PastSnapshotFinder.class).debug("Property " + CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + " is not valid: " + propertyValue);
     }
     return pastSnapshot;
   }
 
   /**
    * @deprecated since 3.6. Replace by {@link #getPropertyValue(String rootQualifier, Settings settings, int index) }
    */
   @Deprecated
   static String getPropertyValue(Configuration conf, int index) {
     String defaultValue = null;
     switch (index) {
       case 1:
         defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_1;
         break;
       case 2:
         defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_2;
         break;
       case 3:
         defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_3;
         break;
       case 4:
         defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_4;
         break;
       case 5:
         defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_5;
         break;
     }
     return conf.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index, defaultValue);
   }
 
  public PastSnapshot find(Snapshot projectSnapshot, @Nullable String rootQualifier, Settings settings, int index) {
     String propertyValue = getPropertyValue(rootQualifier, settings, index);
     PastSnapshot pastSnapshot = find(projectSnapshot, index, propertyValue);
     if (pastSnapshot == null && StringUtils.isNotBlank(propertyValue)) {
       LOG.debug("Property " + CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + " is not valid: " + propertyValue);
     }
     return pastSnapshot;
   }
 
   public PastSnapshot find(Snapshot projectSnapshot, Settings settings, int index) {
     return find(projectSnapshot, null, settings, index);
   }
 
  static String getPropertyValue(@Nullable String rootQualifier, Settings settings, int index) {
     String value = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index);
     // For periods 4 and 5 we're searching for a property prefixed by the qualifier
     if (index > 3 && Strings.isNullOrEmpty(value)) {
       value = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + "." + rootQualifier);
     }
     return value;
   }
 
   public PastSnapshot findPreviousAnalysis(Snapshot projectSnapshot) {
     return finderByPreviousAnalysis.findByPreviousAnalysis(projectSnapshot);
   }
 
  @Nullable
   public PastSnapshot find(Snapshot projectSnapshot, int index, String property) {
     if (StringUtils.isBlank(property)) {
       return null;
     }
 
     PastSnapshot result = findByDays(projectSnapshot, property);
     if (result == null) {
       result = findByDate(projectSnapshot, property);
       if (result == null) {
         result = findByPreviousAnalysis(projectSnapshot, property);
         if (result == null) {
           result = findByPreviousVersion(projectSnapshot, property);
           if (result == null) {
             result = findByVersion(projectSnapshot, property);
           }
         }
       }
     }
 
     if (result != null) {
       result.setIndex(index);
     }
 
     return result;
   }
 
  @Nullable
   private PastSnapshot findByPreviousAnalysis(Snapshot projectSnapshot, String property) {
     PastSnapshot pastSnapshot = null;
     if (StringUtils.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, property)) {
       pastSnapshot = finderByPreviousAnalysis.findByPreviousAnalysis(projectSnapshot);
     }
     return pastSnapshot;
   }
 
  @Nullable
   private PastSnapshot findByPreviousVersion(Snapshot projectSnapshot, String property) {
     PastSnapshot pastSnapshot = null;
     if (StringUtils.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, property)) {
       pastSnapshot = finderByPreviousVersion.findByPreviousVersion(projectSnapshot);
     }
     return pastSnapshot;
   }
 
  @Nullable
   private PastSnapshot findByDate(Snapshot projectSnapshot, String property) {
     SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
     try {
       Date date = format.parse(property);
       return finderByDate.findByDate(projectSnapshot, date);
 
     } catch (ParseException e) {
       return null;
     }
   }
 
   private PastSnapshot findByVersion(Snapshot projectSnapshot, String property) {
     return finderByVersion.findByVersion(projectSnapshot, property);
   }
 
  @Nullable
   private PastSnapshot findByDays(Snapshot projectSnapshot, String property) {
     try {
       int days = Integer.parseInt(property);
       return finderByDays.findFromDays(projectSnapshot, days);
 
     } catch (NumberFormatException e) {
       return null;
     }
   }
 
   public static List<? extends PropertyDefinition> getPropertyDefinitions() {
     return ImmutableList.of(
       PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 1)
         .name("Period 1")
         .description("Period used to compare measures and track new violations. Values are : <ul class='bullet'><li>Number of days before " +
           "analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, for example 2010-12-25</li><li>'previous_analysis' to " +
           "compare to previous analysis</li><li>'previous_version' to compare to the previous version in the project history</li></ul>" +
           "<p>When specifying a number of days or a date, the snapshot selected for comparison is " +
           " the first one available inside the corresponding time range. </p>" +
           "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
         .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_1)
         .category(CoreProperties.CATEGORY_GENERAL)
         .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
         .build(),
 
       PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 2)
         .name("Period 2")
         .description("See the property 'Period 1'")
         .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_2)
         .category(CoreProperties.CATEGORY_GENERAL)
         .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
         .build(),
 
       PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 3)
         .name("Period 3")
         .description("See the property 'Period 1'")
         .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_3)
         .category(CoreProperties.CATEGORY_GENERAL)
         .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
         .build(),
 
       PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 4)
         .name("Period 4")
         .description("Period used to compare measures and track new violations. This property is specific to the project. Values are : " +
           "<ul class='bullet'><li>Number of days before analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, " +
           "for example 2010-12-25</li><li>'previous_analysis' to compare to previous analysis</li>" +
           "<li>'previous_version' to compare to the previous version in the project history</li><li>A version, for example 1.2</li></ul>" +
           "<p>When specifying a number of days or a date, the snapshot selected for comparison is the first one available inside the corresponding time range. </p>" +
           "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
         .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_4)
         .onlyOnQualifiers(Qualifiers.PROJECT)
         .category(CoreProperties.CATEGORY_GENERAL)
         .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
         .build(),
 
       PropertyDefinition.builder(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + 5)
         .name("Period 5")
         .description("See the property 'Period 4'")
         .defaultValue(CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_5)
         .onlyOnQualifiers(Qualifiers.PROJECT)
         .category(CoreProperties.CATEGORY_GENERAL)
         .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
         .build()
     );
   }
 
 }
