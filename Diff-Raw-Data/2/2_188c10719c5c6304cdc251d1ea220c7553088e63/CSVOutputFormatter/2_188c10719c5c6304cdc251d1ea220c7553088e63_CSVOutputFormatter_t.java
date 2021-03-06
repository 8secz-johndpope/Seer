 package com.dataiku.dip.output;
 
 import java.io.IOException;
 import java.io.Writer;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.commons.lang.StringUtils;
 
 import com.dataiku.dip.datalayer.Column;
 import com.dataiku.dip.datalayer.ColumnFactory;
 import com.dataiku.dip.datalayer.Row;
 import com.dataiku.dip.datasets.Schema;
 
 public class CSVOutputFormatter extends StringOutputFormatter {
 
     public CSVOutputFormatter(char delimiter, boolean printHeaderLine, boolean prefixHeaderLineWithSharp) {
         super("utf8");
         this.delimiter = delimiter;
         this.printHeaderLine = printHeaderLine;
         this.prefixHeaderLineWithSharp = prefixHeaderLineWithSharp;
     }
     @Override
     public void setOutputSchema(Schema schema) {
         this.outputSchema = schema;
     }
 
     @Override
     public void header(ColumnFactory cf, Writer sb) throws IOException {
         if (printHeaderLine && prefixHeaderLineWithSharp) {
             sb.append("# ");
         }
         if (outputSchema != null) {
             for (int i = 0; i < outputSchema.getColumns().size(); i++) {
                 String name = outputSchema.getColumns().get(i).getName();
                if (i > 0 && printHeaderLine) sb.append(delimiter);
                 headerColumns.add(cf.column(name));
                 if (printHeaderLine) sb.append(name);
             }
         } else {
             int i = 0;
             for (Column c : cf.columns()) {
                 headerColumns.add(c);
                 if (i++ > 0 && printHeaderLine) sb.append(delimiter);
                 if (printHeaderLine) sb.append(c.getName());
             }
         }
         if (printHeaderLine) sb.append('\n');
     }
 
     @Override
     public void format(Row row, ColumnFactory cf, Writer sb) throws IOException {
         int i = 0;
         for (Column c : headerColumns) {
             String v = row.get(c);
             if (i++ > 0) sb.append(delimiter);
             if (v != null && v.length() > 0) {
                 boolean needsEscape = v.indexOf('"') >= 0;
                 if (needsEscape) {
                     sb.append('"');
                     sb.append(StringUtils.replace(v, "\"", "\\\""));
                     sb.append('"');
                 } else {
                     sb.append(v);
                 }
             }
         }
         sb.append('\n');
     }
 
     @Override
     public void footer(ColumnFactory cf, Writer sb) throws IOException {
     }
 
     private Schema outputSchema;
     private char delimiter;
     private boolean printHeaderLine;
     private boolean prefixHeaderLineWithSharp;
     private List<Column> headerColumns = new ArrayList<Column>();
 }
