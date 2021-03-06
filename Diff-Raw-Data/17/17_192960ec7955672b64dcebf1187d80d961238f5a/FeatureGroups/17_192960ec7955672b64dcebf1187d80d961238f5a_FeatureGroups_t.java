
 package de.cismet.cismap.commons.features;
 
 import com.vividsolutions.jts.geom.Geometry;
 import de.cismet.tools.collections.TypeSafeCollections;
 import java.util.Collection;
 
 /**
  *
  * @author srichter
  */
 public final class FeatureGroups {
 
     private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FeatureGroups.class);
     public static final boolean SHOW_GROUPING_ENABLED = false;
     public static final boolean SHOW_GROUPS_AS_ENVELOPES = false;
 
     private FeatureGroups() {
         throw new AssertionError();
     }
 
     public static boolean hasSubFeatures(FeatureGroup fg) {
         final Collection<? extends Feature> subFeatures = fg.getFeatures();
         if (subFeatures == null || subFeatures.size() == 0) {
             return false;
         } else {
             return true;
         }
     }
 
     public static Collection<? extends Feature> expand(FeatureGroup featureGroup, boolean includeGroups) {
         final Collection<Feature> result = TypeSafeCollections.newArrayList();
         final Collection<? extends Feature> subFeatures = featureGroup.getFeatures();
         if (subFeatures != null) {
             for (Feature f : featureGroup) {
                 if (f instanceof FeatureGroup) {
                     result.addAll(expand((FeatureGroup) f, includeGroups));
                 } else {
                     result.add(f);
                 }
             }
         }
         if (includeGroups || result.size() == 0) {
             result.add(featureGroup);
         }
         return result;
     }
 
     public static Collection<? extends Feature> expandAll(FeatureGroup fg) {
         return expand(fg, true);
     }
 
     public static Collection<? extends Feature> expandToLeafs(FeatureGroup fg) {
         return expand(fg, false);
     }
 
     public static Geometry getEnclosingGeometry(FeatureGroup group) {
         return getEnclosingGeometry(group.getFeatures());
     }
 
     public static Geometry getEnclosingGeometry(Collection<? extends Feature> featureCollection) {
         Geometry g = null;
         for (Feature f : featureCollection) {
             Geometry newGeom = f.getGeometry();
             if (newGeom != null) {
                 if (g == null) {
                     g = newGeom;
                     if (FeatureGroups.SHOW_GROUPS_AS_ENVELOPES) {
                         g = g.getEnvelope();
                     }
                 } else {
                     if (FeatureGroups.SHOW_GROUPS_AS_ENVELOPES) {
                         g = g.union(newGeom.getEnvelope()).getEnvelope();
 
                     } else {
                         g = g.union(newGeom);
                     }
                 }
             }
         }
         return g;
     }
 
     public static FeatureGroup getRootFeature(FeatureGroup hierarchyMember) {
         while (hierarchyMember.getParentFeature() != null) {
             hierarchyMember = hierarchyMember.getParentFeature();
         }
         return hierarchyMember;
     }
 
     public static Feature getRootFeature(SubFeature hierarchyMember) {
         while (hierarchyMember.getParentFeature() != null) {
             hierarchyMember = hierarchyMember.getParentFeature();
         }
         return hierarchyMember;
     }
 }
