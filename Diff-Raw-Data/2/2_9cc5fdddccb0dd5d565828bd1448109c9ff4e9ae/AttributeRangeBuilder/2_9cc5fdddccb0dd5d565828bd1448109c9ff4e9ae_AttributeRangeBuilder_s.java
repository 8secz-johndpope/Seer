 /*
 Copyright 2008 WebAtlas
 Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke
 Website : http://www.gephi.org
 
 This file is part of Gephi.
 
 Gephi is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Gephi is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.gephi.filters.plugin.attribute;
 
 import java.util.ArrayList;
 import java.util.List;
 import javax.swing.Icon;
 import javax.swing.JPanel;
 import org.gephi.data.attributes.api.AttributeColumn;
 import org.gephi.data.attributes.api.AttributeController;
 import org.gephi.data.attributes.api.AttributeModel;
 import org.gephi.data.attributes.api.AttributeUtils;
 import org.gephi.filters.plugin.RangeFilter;
 import org.gephi.filters.api.FilterLibrary;
 import org.gephi.filters.api.Range;
 import org.gephi.filters.spi.Category;
 import org.gephi.filters.spi.CategoryBuilder;
 import org.gephi.filters.spi.Filter;
 import org.gephi.filters.spi.FilterBuilder;
 import org.gephi.filters.spi.FilterProperty;
 import org.gephi.filters.plugin.graph.RangeUI;
 import org.gephi.filters.spi.EdgeFilter;
 import org.gephi.filters.spi.NodeFilter;
 import org.gephi.graph.api.Edge;
 import org.gephi.graph.api.Graph;
 import org.gephi.graph.api.GraphController;
 import org.gephi.graph.api.GraphModel;
 import org.gephi.graph.api.Node;
 import org.openide.util.Exceptions;
 import org.openide.util.Lookup;
 import org.openide.util.NbBundle;
 import org.openide.util.lookup.ServiceProvider;
 
 /**
  *
  * @author Mathieu Bastian
  */
 @ServiceProvider(service = CategoryBuilder.class)
 public class AttributeRangeBuilder implements CategoryBuilder {
 
     private final static Category RANGE = new Category(
             NbBundle.getMessage(AttributeRangeBuilder.class, "AttributeRangeBuilder.name"),
             null,
             FilterLibrary.ATTRIBUTES);
 
     public Category getCategory() {
         return RANGE;
     }
 
     public FilterBuilder[] getBuilders() {
         List<FilterBuilder> builders = new ArrayList<FilterBuilder>();
         AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
         AttributeColumn[] columns = am.getNodeTable().getColumns();
         for (AttributeColumn c : columns) {
             if (AttributeUtils.getDefault().isNumberColumn(c)) {
                 AttributeRangeFilterBuilder b = new AttributeRangeFilterBuilder(c);
                 builders.add(b);
             }
         }
         return builders.toArray(new FilterBuilder[0]);
     }
 
     private static class AttributeRangeFilterBuilder implements FilterBuilder {
 
         private final AttributeColumn column;
 
         public AttributeRangeFilterBuilder(AttributeColumn column) {
             this.column = column;
         }
 
         public Category getCategory() {
             return RANGE;
         }
 
         public String getName() {
             return "<font color='#000000'>" + column.getTitle() + "</font> "
                     + "<font color='#999999'><i>" + column.getType().toString() + "</i></font>";
         }
 
         public Icon getIcon() {
             return null;
         }
 
         public String getDescription() {
             return null;
         }
 
         public AttributeRangelFilter getFilter() {
             AttributeRangelFilter f = new AttributeRangelFilter(column);
             return f;
         }
 
         public JPanel getPanel(Filter filter) {
             RangeUI ui = Lookup.getDefault().lookup(RangeUI.class);
             if (ui != null) {
                 return ui.getPanel((AttributeRangelFilter) filter);
             }
             return null;
         }
     }
 
     public static class AttributeRangelFilter implements RangeFilter, NodeFilter, EdgeFilter {
 
         private FilterProperty[] filterProperties;
         private Range range;
         private AttributeColumn column;
         private Object min = 0;
         private Object max = 0;
         //States
         private List<Object> values;
 
         public AttributeRangelFilter(AttributeColumn column) {
             this.column = column;
         }
 
         public String getName() {
            return NbBundle.getMessage(AttributeRangeBuilder.class, "AttributeRangeBuilder.name");
         }
 
         private void refreshRange() {
             if (range == null) {
                 range = new Range(min, max);
             } else {
                 range.trimBounds(min, max);
             }
         }
 
         public boolean init(Graph graph) {
             values = new ArrayList<Object>();
             return true;
         }
 
         public boolean evaluate(Graph graph, Node node) {
             Object val = node.getNodeData().getAttributes().getValue(column.getIndex());
             if (val != null) {
                 values.add(val);
                 return range.isInRange(val);
             }
             return false;
 
         }
 
         public boolean evaluate(Graph graph, Edge edge) {
             Object val = edge.getEdgeData().getAttributes().getValue(column.getIndex());
             if (val != null) {
                 values.add(val);
                 return range.isInRange(val);
             }
             return false;
         }
 
         public void finish() {
             refreshRange();
         }
 
         public Object[] getValues() {
             if (values == null) {
                 GraphModel gm = Lookup.getDefault().lookup(GraphController.class).getModel();
                 Graph graph = gm.getGraph();
                 List<Object> vals = new ArrayList<Object>();
                 if (AttributeUtils.getDefault().isNodeColumn(column)) {
                     for (Node n : graph.getNodes()) {
                         Object val = n.getNodeData().getAttributes().getValue(column.getIndex());
                         if (val != null) {
                             vals.add(val);
                         }
                     }
                 } else {
                     for (Edge e : graph.getEdges()) {
                         Object val = e.getEdgeData().getAttributes().getValue(column.getIndex());
                         if (val != null) {
                             vals.add(val);
                         }
                     }
                 }
                 Object[] valuesArray = vals.toArray();
                 min = AttributeUtils.getDefault().getMin(column, valuesArray);
                 max = AttributeUtils.getDefault().getMax(column, valuesArray);
                 refreshRange();
                 return valuesArray;
             } else {
                 Object[] valuesArray = values.toArray();
                 min = AttributeUtils.getDefault().getMin(column, valuesArray);
                 max = AttributeUtils.getDefault().getMax(column, valuesArray);
                 return valuesArray;
             }
         }
 
         public FilterProperty[] getProperties() {
             if (filterProperties == null) {
                 filterProperties = new FilterProperty[0];
                 try {
                     filterProperties = new FilterProperty[]{
                                 FilterProperty.createProperty(this, AttributeColumn.class, "column"),
                                 FilterProperty.createProperty(this, Range.class, "range"),};
                 } catch (Exception ex) {
                     Exceptions.printStackTrace(ex);
                 }
             }
             return filterProperties;
         }
 
         public FilterProperty getRangeProperty() {
             return getProperties()[1];
         }
 
         public Object getMinimum() {
             return min;
         }
 
         public Object getMaximum() {
             return max;
         }
 
         public Range getRange() {
             return range;
         }
 
         public void setRange(Range range) {
             this.range = range;
         }
 
         public AttributeColumn getColumn() {
             return column;
         }
 
         public void setColumn(AttributeColumn column) {
             this.column = column;
         }
     }
 }
 
