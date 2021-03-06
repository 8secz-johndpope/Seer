 package uk.ac.starlink.topcat;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Map;
 import javax.swing.ComboBoxModel;
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.event.ListDataEvent;
 import javax.swing.event.ListDataListener;
 import javax.swing.table.TableColumnModel;
 import uk.ac.starlink.table.ColumnData;
 import uk.ac.starlink.table.ColumnInfo;
 import uk.ac.starlink.table.StarTable;
 import uk.ac.starlink.table.Tables;
 import uk.ac.starlink.table.ValueInfo;
 import uk.ac.starlink.table.gui.StarTableColumn;
 
 /**
  * Model for a {@link ColumnSelector}.  Contains information about
  * how you get a value of a given type (such as Right Ascension)
  * from a table.
  *
  * @author   Mark Taylor (Starlink)
  * @since    6 Oct 2004
  */
 public class ColumnSelectorModel implements ListDataListener {
 
     private final TopcatModel tcModel_;
     private final ValueInfo info_;
     private final ComboBoxModel colChooser_;
     private final ComboBoxModel convChooser_;
     private final ColumnConverter converter0_;
     private final Map convMap_ = new HashMap();
 
     /**
      * Constructs a new model for a given table and value type.
      *
      * @param  tcModel  table model
      * @param  info   description of the kind of column which is required
      */
     public ColumnSelectorModel( TopcatModel tcModel, ValueInfo info ) {
         tcModel_ = tcModel;
         info_ = info;
 
         /* Get a suitable model for selection of the base column from the 
          * table. */
         colChooser_ = makeColumnModel( tcModel_, info );
         colChooser_.addListDataListener( this );
 
         /* Get a suitable model for selection of the unit converter, if
          * appropriate. */
         String units = info.getUnitString();
         ColumnConverter[] converters = ColumnConverter.getConverters( info );
         if ( converters.length > 1 ) {
             converter0_ = null;
             convChooser_ = new DefaultComboBoxModel( converters );
             convChooser_.addListDataListener( this );
         }
         else {
             convChooser_ = null;
             converter0_ = converters[ 0 ];
         }
     }
 
     /**
      * Returns the currently selected column converter.
      *
      * @return  converter
      */
     public ColumnConverter getConverter() {
         if ( converter0_ != null ) {
             return converter0_;
         }
         else {
             return (ColumnConverter) convChooser_.getSelectedItem();
         }
     }
 
     /**
      * Returns this model's value description.
      *
      * @return  value info
      */
     public ValueInfo getValueInfo() {
         return info_;
     }
 
     /**
      * Returns the model used for choosing columns.
      *
      * @return  columns combo box model
      */
     public ComboBoxModel getColumnModel() {
         return colChooser_;
     }
 
     /**
      * Returns the model used for choosing converters.  May be null if
      * there is no choice.
      *
      * @return  converter combo box model, or null
      */
     public ComboBoxModel getConverterModel() {
         return convChooser_;
     }
 
     /**
      * Returns the (effective) column currently selected by the user.
      * It takes into account the column and (if any) conversion selected
      * by the user.
      *
      * @return  ColumnData representing the currently-selected column,
      *          or null if none is selected
      */
     public ColumnData getColumnData() {
        StarTableColumn tcol = (StarTableColumn) colChooser_.getSelectedItem();
         if ( tcol == null ) {
             return null;
         }
         final int icol = tcol.getModelIndex();
         final ColumnConverter colConverter = getConverter();
         final StarTable table = tcModel_.getDataModel();
         assert colConverter != null;
         return new ColumnData( tcol.getColumnInfo() ) {
             public Object readValue( long irow ) throws IOException {
                 return colConverter.convertValue( table.getCell( irow, icol ) );
             }
         };
     }
 
     /**
      * Called when the column selection is changed.
      *
      * @param  col  new column (not null)
      */
     private void columnSelected( StarTableColumn tcol ) {
         if ( convChooser_ != null ) {
             ColumnConverter storedConverter =
                (ColumnConverter) convMap_.get( tcol );
 
             /* If we've used this column before, set the converter type
              * to the one that was in effect last time. */
             if ( storedConverter != null ) {
                 convChooser_.setSelectedItem( storedConverter );
             }
 
             /* Otherwise, try to guess the converter type on the basis
              * of the selected column. */
             else {
                 convChooser_
                .setSelectedItem( guessConverter( tcol.getColumnInfo() ) );
             }
         }
     }
 
     /**
      * Called when the converter selection is changed.
      *
      * @param  conv  new converter (not null)
      */
     private void converterSelected( ColumnConverter conv ) {
 
         /* Remember what converter was chosen for the current column. */
         convMap_.put( colChooser_.getSelectedItem(), conv );
     }
 
     /**
      * Returns a best guess for the conversion to use for a given selected
      * column.  This will be one of the ones associated with this selector
      * (i.e. one of the ones in the conversion selector or the sole one
      * if there is no conversion selector).
      *
      * @param  cinfo  column description
      * @return  suitable column converter
      */
     private ColumnConverter guessConverter( ColumnInfo cinfo ) {
 
         /* If there is only one permissible converter, return that. */
         if ( converter0_ != null ) {
             return converter0_;
         }
 
         /* Otherwise, try to get clever.  This is currently done on a
          * case-by-case basis rather than using an extensible framework
          * because there's a small number (1) of conversions that we know
          * about.  If there were many, a redesign might be in order. */
         String units = info_.getUnitString();
         String cunits = cinfo.getUnitString();
         if ( units != null && cunits != null ) {
             units = units.toLowerCase();
             cunits = cunits.toLowerCase();
             int nconv = convChooser_.getSize();
 
             /* Known converters for radians are radian or degree. */
             if ( units.equals( "radian" ) || units.equals( "radians" ) ) {
                 if ( cunits.startsWith( "rad" ) ) {
                     for ( int i = 0; i < nconv; i++ ) {
                         ColumnConverter conv =
                             (ColumnConverter) convChooser_.getElementAt( i );
                         if ( conv.toString().toLowerCase()
                                             .startsWith( "rad" ) ) {
                             return conv;
                         }
                     }
                 }
                 else if ( cunits.startsWith( "deg" ) ) {
                     for ( int i = 0; i < nconv; i++ ) {
                         ColumnConverter conv =
                             (ColumnConverter) convChooser_.getElementAt( i );
                         if ( conv.toString().toLowerCase()
                                             .startsWith( "deg" ) ) {
                             return conv;
                         }
                     }
                 }
                 else if ( cunits.startsWith( "hour" ) ||
                           cunits.equals( "hr" ) || cunits.equals( "hrs" ) ) {
                     for ( int i = 0; i < nconv; i++ ) {
                         ColumnConverter conv =
                             (ColumnConverter) convChooser_.getElementAt( i );
                         if ( conv.toString().toLowerCase()
                                             .startsWith( "hour" ) ) {
                             return conv;
                         }
                     }
                 }
             }
         }
 
         /* Return default one if we haven't found a match yet. */
         return (ColumnConverter) convChooser_.getElementAt( 0 );
     }
 
     /**
      * Returns a combobox model which allows selection of columns
      * from a table model suitable for a given argument.
      */
     private static ComboBoxModel makeColumnModel( TopcatModel tcModel,
                                                   ValueInfo argInfo ) {
 
         /* Make the model. */
         TableColumnModel columnModel = tcModel.getColumnModel();
         RestrictedColumnComboBoxModel model =
             RestrictedColumnComboBoxModel
            .makeClassColumnComboBoxModel( columnModel, argInfo.isNullable(),
                                           argInfo.getContentClass() );
 
         /* Have a guess what will be a good value for the initial
          * selection.  There is scope for doing this better. */
         int selection = -1;
         ColumnInfo[] cinfos =
             Tables.getColumnInfos( tcModel.getApparentStarTable() );
         int ncol = cinfos.length;
         String ucd = argInfo.getUCD();
         if ( ucd != null ) {
             for ( int i = 0; i < ncol && selection < 0; i++ ) {
                 if ( model.acceptColumn( cinfos[ i ] ) &&
                      cinfos[ i ].getUCD() != null &&
                      cinfos[ i ].getUCD().indexOf( ucd ) >= 0 ) {
                     selection = i;
                 }
             }
         }
         String name = argInfo.getName().toLowerCase();
         if ( name != null && selection < 0 ) {
             for ( int i = 0; i < ncol && selection < 0; i++ ) {
                 if ( model.acceptColumn( cinfos[ i ] ) ) {
                     String cname = cinfos[ i ].getName();
                     if ( cname != null &&
                          cname.toLowerCase().startsWith( name ) ) {
                         selection = i;
                     }
                 }
             }
         }
         if ( selection >= 0 ) { 
             model.setSelectedItem( columnModel.getColumn( selection ) );
         }
         return model;
     }
 
     /*
      * ListDataListener implementation.
      */
     public void intervalAdded( ListDataEvent evt ) {
     }
     public void intervalRemoved( ListDataEvent evt ) {
     }
     public void contentsChanged( ListDataEvent evt ) {
 
         /* Contrary to API documentation, this is called when the selection
          * on a ComboBoxModel is changed. */
         if ( evt.getSource() == colChooser_ ) {
            StarTableColumn col = 
                (StarTableColumn) colChooser_.getSelectedItem();
             if ( col != null ) {
                 columnSelected( col );
             }
         }
         else if ( evt.getSource() == convChooser_ ) {
             ColumnConverter conv =
                 (ColumnConverter) convChooser_.getSelectedItem();
             if ( conv != null ) {
                 converterSelected( conv );
             }
         }
         else {
             assert false;
         }
     }
 
 }
