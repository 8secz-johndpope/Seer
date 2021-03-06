 package se.cubecon.bun24.viewpoint.data;
 
 import com.idega.data.*;
 import java.util.*;
 import java.rmi.RemoteException;
 import javax.ejb.FinderException;
 import com.idega.user.data.Group;
 
 /**
  * @author <a href="http://www.staffannoteberg.com">Staffan Nteberg</a>
  */
 public class SubCategoryBMPBean extends GenericEntity implements SubCategory {
 
     private static final String ENTITY_NAME = "vp_subcategory";
     private static final String COLUMN_ID = ENTITY_NAME + "_id";
 	private static final String COLUMN_NAME = "name";
     private static final String COLUMN_TOPCATEGORY_ID = "topcategory_id";
     private static final String COLUMN_HANDLERGROUP_ID = "handlergroup_id";
 
 	public String getEntityName() {
 		return ENTITY_NAME;
 	}
 
     public void insertStartData () throws Exception {
         super.insertStartData ();
        System.out.println (" Invoked " + ENTITY_NAME + ".insertStartData ()");
 
         final String [][] startData = {
             { "Anordnare", "Barnomsorg", "Myndighetsgruppen" },
             { "Betalningar", "Barnomsorg", "Ekonomi" },
             { "Ktid", "Barnomsorg", "Anordnare" },
             { "Regelverk", "Barnomsorg", "Kundvalsgruppen" },
             { "Taxan", "Barnomsorg", "Kundvalsgruppen" },
             { "vrigt", "Barnomsorg", "Kundvalsgruppen" },
             { "Fritids", "Skola", "Kundvalsgruppen" },
             { "Frskoleklass", "Skola", "Kundvalsgruppen" },
             { "Likvrdighetsgaranti", "Skola", "Myndighetsgruppen" },
             { "Modersml", "Skola", "Myndighetsgruppen" },
             { "Regelverk", "Skola", "Kundvalsgruppen" },
             { "Skolskjuts", "Skola", "Myndighetsgruppen" },
             { "Skolval", "Skola", "Anordnare" },
             { "vrigt", "Skola", "Kundvalsgruppen" },
             { "Elevvrd", "Gymnasieskola", "Myndighetsgruppen" },
             { "Inackorderingsbidrag", "Gymnasieskola", "Myndighetsgruppen" },
             { "Intagning", "Gymnasieskola", "Intagningsgruppen" },
             { "Programval", "Gymnasieskola", "Kundvalsgruppen" },
             { "Skolhlsovrd", "Gymnasieskola", "Myndighetsgruppen" },
             { "Studiebidrag", "Gymnasieskola", "Myndighetsgruppen" },
             { "vrigt", "Gymnasieskola", "Kundvalsgruppen" },
             { "Anordnare", "Komvux", "Myndighetsgruppen" },
             { "Kurser/utbud", "Komvux", "Kundvalsgruppen" },
             { "SFI", "Komvux", "Kundvalsgruppen" },
             { "Studiebidrag", "Komvux", "Myndighetsgruppen" },
             { "Studievgledning", "Komvux", "Kundvalsgruppen" },
             { "vrigt", "Komvux", "Kundvalsgruppen" },
             { "Beslut i nmnden", "Politiker", "Namndsekreterare" },
             { "Enskilt rende", "Politiker", "Myndighetsgruppen" },
             { "Frslag", "Politiker", "Namndsekreterare" },
             { "Kundvalet", "Politiker", "Kundvalsgruppen" },
             { "vrigt", "Politiker", "Namndsekreterare" },
             { "BUN24", "Myndigheten", "Kundvalsgruppen" },
             { "Barnomsorgscheck", "Myndigheten", "Finansgruppen" },
             { "Handikapp", "Myndigheten", "Myndighetsgruppen" },
             { "Likvrdighetsgaranti", "Myndigheten", "Myndighetsgruppen" },
             { "Service", "Myndigheten", "Kundvalsgruppen" },
             { "Skolpeng", "Myndigheten", "Finansgruppen" },
             { "vrigt", "Myndigheten", "Myndighetsgruppen" },
         };
        final TopCategoryHome topCategoryHome
                = (TopCategoryHome) IDOLookup.getHome (TopCategory.class);
        final TopCategory [] topCategories = topCategoryHome.findAll ();
        final Map topCategoriesMap = new HashMap ();
        for (int i = 0; i < topCategories.length; i++) {
            topCategoriesMap.put (topCategories [i].getName (),
                                  topCategories [i].getPrimaryKey ());
        }
        final Map groupsMap = new HashMap ();
        SubCategoryHome subCategoryHome
                = (SubCategoryHome) IDOLookup.getHome(SubCategory.class);
        for (int i = 0; i < startData.length; i++) {
            final String subCategoryName = startData [i][0];
            final String topCategoryName = startData [i][1];
            final String groupName = startData [i][2];
            final Integer topCategoryId
                    = (Integer) topCategoriesMap.get (topCategoryName);
            final Integer handlerGroupId = (Integer) groupsMap.get (groupName);
            if (topCategoryId != null && handlerGroupId != null) {
                final SubCategory subCategory = subCategoryHome.create ();
                subCategory.setName (subCategoryName);
                subCategory.setHandlerGroupId (handlerGroupId.intValue ());
                subCategory.setTopCategoryId (topCategoryId.intValue ());
                subCategory.store ();
            }
        }
     }
 
     public void initializeAttributes () {
         addAttribute(COLUMN_ID, "Id", Integer.class);
         setAsPrimaryKey (COLUMN_ID, true);
 		addAttribute (COLUMN_NAME, "Name", String.class);
         addManyToOneRelationship (COLUMN_TOPCATEGORY_ID, TopCategory.class);
         addManyToOneRelationship (COLUMN_HANDLERGROUP_ID, Group.class);
     }
 
     public String getName () {
 		return getStringColumnValue (COLUMN_NAME);
     }
 
     public Group getHandlerGroup () {
         return (Group) getColumnValue (COLUMN_HANDLERGROUP_ID);
     }
 
     public void setName (final String name) {
 		setColumn (COLUMN_NAME, name);
     }
 
     public void setTopCategoryId (final int id) {
 		setColumn (COLUMN_TOPCATEGORY_ID, id);
     }
 
     public void setHandlerGroupId (final int id) {
 		setColumn (COLUMN_HANDLERGROUP_ID, id);
     }
 
     public Collection ejbFindSubCategories (final int topCategoryId)
         throws FinderException, RemoteException {
        final String sql = "select * from " + ENTITY_NAME + " where "
                 + COLUMN_TOPCATEGORY_ID + " = '" + topCategoryId + "'";
         System.out.println ("sql: " + sql);
         return idoFindIDsBySQL (sql);
     }
 }
