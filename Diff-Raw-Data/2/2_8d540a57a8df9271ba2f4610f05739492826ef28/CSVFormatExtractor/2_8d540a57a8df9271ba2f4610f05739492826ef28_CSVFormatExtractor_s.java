 package com.dataiku.dip.input.formats;
 
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.List;
 
 import com.amazonaws.util.json.JSONObject;
 import com.dataiku.dip.datasets.Schema;
 import com.dataiku.dip.datasets.Schema.SchemaColumn;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.log4j.Logger;
 
 import au.com.bytecode.opencsv.CSVReader;
 
 import com.dataiku.dip.datalayer.Column;
 import com.dataiku.dip.datalayer.ColumnFactory;
 import com.dataiku.dip.datalayer.ProcessorOutput;
 import com.dataiku.dip.datalayer.Row;
 import com.dataiku.dip.datalayer.RowFactory;
 import com.dataiku.dip.input.StreamInputSplitProgressListener;
 import com.dataiku.dip.input.stream.EnrichedInputStream;
 import com.dataiku.dip.input.stream.StreamsInputSplit;
 import com.dataiku.dip.utils.DKULogger;
 import com.google.common.io.CountingInputStream;
 
 public class CSVFormatExtractor extends AbstractFormatExtractor  {
     public CSVFormatExtractor(CSVFormatConfig conf) {
         this.conf = conf;
     }
 
     private CSVFormatConfig conf;
 
 
     long log_count = 0;
     long max_log = 100;
     protected void log_line_warn(String msg) {
         log_count ++;
         if (log_count == max_log) {
             logger.warn("Maximum log count (other info ignore)");
         } else if (log_count < max_log) {
             logger.warn(msg);
         }
     }
 
     @Override
     public boolean run(StreamsInputSplit in, ProcessorOutput out, ProcessorOutput err,
             ColumnFactory cf, RowFactory rf, StreamInputSplitProgressListener listener,
             ExtractionLimit limit) throws Exception {
         long totalBytes = 0, totalRecords = 0;
         long log_count = 0;
         long max_log = 1000; // Maximum number of log per line.
         while (true) {
             EnrichedInputStream stream = in.nextStream();
             if (stream == null) break;
 
             logger.info("CSV starting to process one stream: " + stream.desc() +  "  " + stream.size());
 
             InputStream is = stream.headStream(limit != null ? limit.maxBytes : -1); 
             CountingInputStream cis = new CountingInputStream(is);
 
             CSVReader reader = null;
             if (conf.escapeChar != null) {
                 reader = new CSVReader(new InputStreamReader(cis, conf.charset), conf.separator,
                         conf.quoteChar, conf.escapeChar);
             } else {
                 reader = new CSVReader(new InputStreamReader(cis, conf.charset), conf.separator,
                         conf.quoteChar);
             }
             try {
                 List<Column> columns = new ArrayList<Column>();
                 List<Schema.Type> types = getTypes();
                 long fileLines = 0, nintern = 0;
                 
                 if (!conf.parseHeaderRow && schema != null) {
                     logger.info("Loading schema");
                     for (SchemaColumn col : schema.getColumns()) {
                         columns.add(cf.column(col.getName()));
                     }
                 }
                 while (true){
                     String[] line = reader.readNext();
                     if (line == null) break;
                     if (limit != null) {
                         if (limit.maxBytes > 0 && limit.maxBytes < totalBytes + cis.getCount()) return false;
                         if (limit.maxRecords > 0 && limit.maxRecords <= totalRecords) return false;
                     }
 
                     if (fileLines < conf.skipRowsBeforeHeader) {
                         // Do nothing
                     } else if (fileLines == conf.skipRowsBeforeHeader && conf.parseHeaderRow) {
                         if (line[0].startsWith("#")) {
                             line[0] = line[0].substring(1);
                         }
                         int colIdx = 0;
                         for (String ch : line) {
                             ch = ch.trim();
                             // Sometimes, people leave holes in the header ...
                             if (ch.isEmpty()) {
                                 ch = "col_" + columns.size();
                             }
                             /* Override with schema ... */
                             if (getSchema() != null && getSchema().getColumns().size() > colIdx) {
                                 ch = getSchema().getColumns().get(colIdx).getName();
                             }
                             Column cd = cf.column(ch);
                             columns.add(cd);
                             colIdx++;
                         }
                     } else {
                         if (columns.size() > 0 && line.length != columns.size() && Math.abs(line.length - columns.size()) > 2) {
                             log_line_warn("Line has an unexpected number of columns, line has " + line.length +
                                     " columns, extractor has " + columns.size());
                         }
 
                         if (line.length > columns.size()) {
                             for (int i = columns.size() ; i < line.length; i++) {
                                 String name = null;
                                 if (getSchema() != null && getSchema().getColumns().size() > i) {
                                     name = getSchema().getColumns().get(i).getName();
                                 } else {
                                     name = "col_" + i;
                                 }
                                 Column cd = cf.column(name);
                                 columns.add(cd);
                             }
                         }
                         Row r = rf.row();
                         for (int i = 0; i < line.length; i++) {
                             line[i] = line[i].trim();  // trim returns a reference and does not reallocate if there is no whitespace to trim
                             if (line[i].length() > 3000) {
                                 log_line_warn("Unusually large column (quoting issue ?) : " + line[i]);
                             }
                             String s = line[i];
 
                             boolean nullString = false;
                             /* Replace common strings by their intern versions */
                             if (s.equals("null")) { s = "null"; nullString=true; ++nintern; }
                             else if (s.equals("NULL")) { s = "NULL"; nullString=true; ++nintern; }
                             else if (s.equals("\\N")) { s = "\\N"; nullString=true; ++nintern; }
                             else if (s.equals("true")) { s = "true"; ++nintern; }
                             else if (s.equals("false")) { s = "false"; ++nintern; }
                             else if (s.equals("Y")) { s = "Y"; ++nintern; }
                             else if (s.equals("N")) { s = "N"; ++nintern; }
                             else if (s.equals("0")) { s = "0"; ++nintern; }
 
 
                            if (!nullString && types != null && conf.arraySeparator != null && !conf.arrayMapFormat.equals("json")) {
                             Schema.Type type = types.get(i);
 
                                 // Replace the string by its
                                 if (type.equals(Schema.Type.MAP) && conf.mapKeySeparator != null) {
                                     if (s.length() > 0 && (s.indexOf(conf.mapKeySeparator.charValue()) < 0)) {
                                         log_line_warn("Map column " + columns.get(i).getName() + " does not contains the map key separator " + (int) conf.mapKeySeparator.charValue());
                                         s = "{}";
                                     } else {
                                         StringBuilder builder = new StringBuilder();
                                         builder.append("{");
                                         String[] elts = s.split("" +conf.arraySeparator);
                                         boolean first = true;
                                         for(String elt : elts) {
                                             String[] keyValue = elt.split(""+ conf.mapKeySeparator,2);
                                             if (keyValue.length < 2) {
                                                 log_line_warn("Map column " + columns.get(i).getName() + " does not contain map separator at entry: " + elt);
                                                 continue;
                                             }
                                             if (first) {
                                                 first = false;
                                             } else {
                                                 builder.append(",");
                                             }
                                             builder.append(JSONObject.quote(keyValue[0]));
                                             builder.append(":");
                                             builder.append(JSONObject.quote(keyValue[1]));
                                         }
                                         builder.append("}");
                                         s = builder.toString();
                                     }
                                 } else if (!nullString && type.equals(Schema.Type.ARRAY)) {
                                     StringBuilder builder = new StringBuilder();
                                     builder.append("[");
                                     String[] elts = s.split("" +conf.arraySeparator);
                                     boolean first = true;
                                     for(String elt : elts) {
                                         builder.append(JSONObject.quote(elt));
                                     }
                                     builder.append("]");
                                     s = builder.toString();
                                 }
                             }
 
                             r.put(columns.get(i), s);
                         }
                         if (fileLines >= conf.skipRowsBeforeHeader + conf.skipRowsAfterHeader+ (conf.parseHeaderRow?1:0)) {
                             totalRecords++;
                             out.emitRow(r);
                         }
                     }
                     fileLines++;
 
                     if (listener != null && totalRecords % 500 == 0) {
                         if (totalRecords % 5000 == 0) {
                             Runtime runtime = Runtime.getRuntime();
                             double p = ((double) runtime.totalMemory()) / runtime.maxMemory() * 100;
                             logger.info("CSV Emitted " + fileLines + " lines from file, " + totalRecords + " total, " +
                                     columns.size() + " columns - interned: " + nintern + " MEM: " + p + "%");
                         }
                         listener.setData(totalBytes + cis.getCount(), totalRecords, 0);
                     }
                 }
                 totalBytes += cis.getCount();
                 if (listener != null) {
                     listener.setData(totalBytes, totalRecords, 0);
                 }
             } finally {
                 logger.info("Closing stream");
                 reader.close();
                 IOUtils.closeQuietly(cis);
                 logger.info("Stream closed");
             }
         }
         return true;
     }
 
     private static Logger logger = DKULogger.getLogger("dku.format.csv");
 }
