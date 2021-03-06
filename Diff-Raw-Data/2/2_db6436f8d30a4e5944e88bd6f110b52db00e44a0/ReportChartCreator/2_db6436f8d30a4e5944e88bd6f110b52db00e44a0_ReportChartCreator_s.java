 /*
     Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
     @author Matthew Lohbihler
  */
 package com.serotonin.m2m2.reports.web;
 
 import java.awt.BasicStroke;
 import java.awt.Color;
 import java.awt.Stroke;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.text.DecimalFormat;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TimeZone;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.jfree.data.time.TimeSeries;
 
 import com.serotonin.InvalidArgumentException;
 import com.serotonin.ShouldNeverHappenException;
 import com.serotonin.m2m2.Common;
 import com.serotonin.m2m2.DataTypes;
 import com.serotonin.m2m2.email.MessageFormatDirective;
 import com.serotonin.m2m2.email.SubjectDirective;
 import com.serotonin.m2m2.email.UsedImagesDirective;
 import com.serotonin.m2m2.i18n.Translations;
 import com.serotonin.m2m2.reports.ReportDao;
 import com.serotonin.m2m2.reports.vo.ReportInstance;
 import com.serotonin.m2m2.reports.vo.ReportVO;
 import com.serotonin.m2m2.rt.dataImage.PointValueTime;
 import com.serotonin.m2m2.rt.event.EventInstance;
 import com.serotonin.m2m2.util.chart.DiscreteTimeSeries;
 import com.serotonin.m2m2.util.chart.ImageChartUtils;
 import com.serotonin.m2m2.util.chart.NumericTimeSeries;
 import com.serotonin.m2m2.util.chart.PointTimeSeriesCollection;
 import com.serotonin.m2m2.view.quantize.AbstractDataQuantizer;
 import com.serotonin.m2m2.view.quantize.BinaryDataQuantizer;
 import com.serotonin.m2m2.view.quantize.DiscreteTimeSeriesQuantizerCallback;
 import com.serotonin.m2m2.view.quantize.MultistateDataQuantizer;
 import com.serotonin.m2m2.view.quantize.NumericDataQuantizer;
 import com.serotonin.m2m2.view.quantize.TimeSeriesQuantizerCallback;
 import com.serotonin.m2m2.view.stats.AnalogStatistics;
 import com.serotonin.m2m2.view.stats.StartsAndRuntime;
 import com.serotonin.m2m2.view.stats.StartsAndRuntimeList;
 import com.serotonin.m2m2.view.stats.StatisticsGenerator;
 import com.serotonin.m2m2.view.stats.ValueChangeCounter;
 import com.serotonin.m2m2.view.text.TextRenderer;
 import com.serotonin.m2m2.vo.UserComment;
 import com.serotonin.m2m2.vo.export.EventCsvStreamer;
 import com.serotonin.m2m2.vo.export.ExportCsvStreamer;
 import com.serotonin.m2m2.vo.export.ExportDataStreamHandler;
 import com.serotonin.m2m2.vo.export.ExportDataValue;
 import com.serotonin.m2m2.vo.export.ExportPointInfo;
 import com.serotonin.m2m2.web.taglib.Functions;
 import com.serotonin.util.ColorUtils;
 
 import freemarker.template.Template;
 
 /**
  * @author Matthew Lohbihler
  */
 public class ReportChartCreator {
     static final Log LOG = LogFactory.getLog(ReportChartCreator.class);
 
     private static final String IMAGE_SERVLET = "reportImageChart/";
 
     /**
      * This image width is specifically chosen such that the report will print on a single page width in landscape.
      */
     private static final int IMAGE_WIDTH = 930;
     private static final int IMAGE_HEIGHT = 400;
     public static final String IMAGE_CONTENT_ID = "reportChart.png";
 
     public static final int POINT_IMAGE_WIDTH = 440;
     public static final int POINT_IMAGE_HEIGHT = 250; // 340
 
     String inlinePrefix;
     private String html;
     private String subject;
     private List<String> inlineImageList;
     private byte[] imageData;
     private String chartName;
     private File exportFile;
     private File eventFile;
     private File commentFile;
     private List<PointStatistics> pointStatistics;
 
     final Translations translations;
     final TimeZone timeZone;
 
     public ReportChartCreator(Translations translations, TimeZone timeZone) {
         this.translations = translations;
         this.timeZone = timeZone;
     }
 
     /**
      * Uses the given parameters to create the data for the fields of this class. Once the content has been created the
      * getters for the fields can be used to retrieve.
      * 
      * @param reportInstance
      * @param reportDao
      * @param inlinePrefix
      *            if this is non-null, it implies that the content should be inline.
      * @param createExportFile
      */
     public void createContent(ReportInstance reportInstance, ReportDao reportDao, String inlinePrefix,
             boolean createExportFile) {
         this.inlinePrefix = inlinePrefix;
 
         reportInstance.setTranslations(translations);
 
         // Use a stream handler to get the report data from the database.
         StreamHandler handler = new StreamHandler(reportInstance.getReportStartTime(),
                 reportInstance.getReportEndTime(), IMAGE_WIDTH, createExportFile, translations);
         // Process the report content with the handler.
         reportDao.reportInstanceData(reportInstance.getId(), handler);
 
         pointStatistics = handler.getPointStatistics();
         UsedImagesDirective inlineImages = new UsedImagesDirective();
         SubjectDirective subjectDirective = new SubjectDirective(translations);
 
         // Prepare the model for the content rendering.
         Map<String, Object> model = new HashMap<String, Object>();
         model.put("fmt", new MessageFormatDirective(translations));
         model.put("subject", subjectDirective);
         model.put("img", inlineImages);
         model.put("instance", reportInstance);
         model.put("timezone", timeZone.getID());
         model.put("points", pointStatistics);
         model.put("inline", inlinePrefix == null ? "" : "cid:");
 
         model.put("ALPHANUMERIC", DataTypes.ALPHANUMERIC);
         model.put("BINARY", DataTypes.BINARY);
         model.put("MULTISTATE", DataTypes.MULTISTATE);
         model.put("NUMERIC", DataTypes.NUMERIC);
         model.put("IMAGE", DataTypes.IMAGE);
 
         // Create the individual point charts
         for (PointStatistics pointStat : pointStatistics) {
             PointTimeSeriesCollection ptsc = new PointTimeSeriesCollection(timeZone);
 
             if (pointStat.getNumericTimeSeries() != null)
                 ptsc.addNumericTimeSeries(pointStat.getNumericTimeSeries().plainCopy());
             else if (pointStat.getDiscreteTimeSeries() != null)
                 ptsc.addDiscreteTimeSeries(pointStat.getDiscreteTimeSeries().plainCopy());
 
             if (ptsc.hasData()) {
                 if (inlinePrefix != null)
                     model.put("chartName", inlinePrefix + pointStat.getChartName());
                 pointStat.setImageData(ImageChartUtils.getChartData(ptsc, POINT_IMAGE_WIDTH, POINT_IMAGE_HEIGHT,
                         reportInstance.getReportStartTime(), reportInstance.getReportEndTime()));
             }
         }
 
         PointTimeSeriesCollection ptsc = handler.getPointTimeSeriesCollection();
         if (ptsc.hasData()) {
             if (inlinePrefix != null)
                 model.put("chartName", inlinePrefix + IMAGE_CONTENT_ID);
             else {
                 chartName = "r" + reportInstance.getId() + ".png";
                 // The path comes from the servlet path definition in web.xml.
                 model.put("chartName", IMAGE_SERVLET + chartName);
             }
 
             imageData = ImageChartUtils.getChartData(ptsc, true, IMAGE_WIDTH, IMAGE_HEIGHT,
                     reportInstance.getReportStartTime(), reportInstance.getReportEndTime());
         }
 
         List<EventInstance> events = null;
         if (reportInstance.getIncludeEvents() != ReportVO.EVENTS_NONE) {
             events = reportDao.getReportInstanceEvents(reportInstance.getId());
             model.put("includeEvents", true);
             model.put("events", events);
         }
         else
             model.put("includeEvents", false);
 
         List<ReportUserComment> comments = null;
         if (reportInstance.isIncludeUserComments()) {
             comments = reportDao.getReportInstanceUserComments(reportInstance.getId());
 
             // Only provide the list of point comments to the report. The event comments have already be correlated
             // into the events list.
             List<ReportUserComment> pointComments = new ArrayList<ReportUserComment>();
             for (ReportUserComment c : comments) {
                 if (c.getCommentType() == UserComment.TYPE_POINT)
                     pointComments.add(c);
             }
 
             model.put("includeUserComments", true);
             model.put("userComments", pointComments);
         }
         else
             model.put("includeUserComments", false);
 
         // Create the template.
         Template template;
         try {
             template = Common.freemarkerConfiguration.getTemplate("reportChart.ftl");
         }
         catch (IOException e) {
             // Couldn't load the template?
             throw new ShouldNeverHappenException(e);
         }
 
         // Create the content from the template.
         StringWriter writer = new StringWriter();
         try {
             template.process(model, writer);
         }
         catch (Exception e) {
             // Couldn't process the template?
             throw new ShouldNeverHappenException(e);
         }
 
         // Save the content
         html = writer.toString();
         subject = subjectDirective.getSubject();
         inlineImageList = inlineImages.getImageList();
 
         // Save the export file (if any)
         exportFile = handler.exportFile;
 
         if (createExportFile && events != null) {
             try {
                 eventFile = File.createTempFile("tempEventCSV", ".csv");
                 new EventCsvStreamer(new PrintWriter(new FileWriter(eventFile)), events, translations);
             }
             catch (IOException e) {
                 LOG.error("Failed to create temp event file", e);
             }
         }
 
         if (createExportFile && comments != null) {
             try {
                 commentFile = File.createTempFile("tempCommentCSV", ".csv");
                 new UserCommentCsvStreamer(new PrintWriter(new FileWriter(commentFile)), comments, translations);
             }
             catch (IOException e) {
                 LOG.error("Failed to create temp comment file", e);
             }
         }
     }
 
     public String getHtml() {
         return html;
     }
 
     public String getSubject() {
         return subject;
     }
 
     public List<String> getInlineImageList() {
         return inlineImageList;
     }
 
     public String getChartName() {
         return chartName;
     }
 
     public byte[] getImageData() {
         return imageData;
     }
 
     public File getExportFile() {
         return exportFile;
     }
 
     public File getEventFile() {
         return eventFile;
     }
 
     public File getCommentFile() {
         return commentFile;
     }
 
     public List<PointStatistics> getPointStatistics() {
         return pointStatistics;
     }
 
     public class PointStatistics {
         private final int reportPointId;
         private String name;
         private int dataType;
         private String dataTypeDescription;
         private String startValue;
         private TextRenderer textRenderer;
         private StatisticsGenerator stats;
         //private TimeSeries numericTimeSeries;
         //private Color numericTimeSeriesColor;
         private NumericTimeSeries numericTimeSeries;
         private DiscreteTimeSeries discreteTimeSeries;
         private byte[] imageData;
 
         public PointStatistics(int reportPointId) {
             this.reportPointId = reportPointId;
         }
 
         public String getName() {
             return name;
         }
 
         public void setName(String name) {
             this.name = name;
         }
 
         public int getDataType() {
             return dataType;
         }
 
         public void setDataType(int dataType) {
             this.dataType = dataType;
         }
 
         public String getDataTypeDescription() {
             return dataTypeDescription;
         }
 
         public void setDataTypeDescription(String dataTypeDescription) {
             this.dataTypeDescription = dataTypeDescription;
         }
 
         public String getStartValue() {
             return startValue;
         }
 
         public void setStartValue(String startValue) {
             this.startValue = startValue;
         }
 
         public StatisticsGenerator getStats() {
             return stats;
         }
 
         public void setStats(StatisticsGenerator stats) {
             this.stats = stats;
         }
 
         public TextRenderer getTextRenderer() {
             return textRenderer;
         }
 
         public void setTextRenderer(TextRenderer textRenderer) {
             this.textRenderer = textRenderer;
         }
 
         public NumericTimeSeries getNumericTimeSeries() {
             return numericTimeSeries;
         }
 
         public void setNumericTimeSeries(NumericTimeSeries numericTimeSeries) {
             this.numericTimeSeries = numericTimeSeries;
         }
 
         public DiscreteTimeSeries getDiscreteTimeSeries() {
             return discreteTimeSeries;
         }
 
         public void setDiscreteTimeSeries(DiscreteTimeSeries discreteTimeSeries) {
             this.discreteTimeSeries = discreteTimeSeries;
         }
 
         public byte[] getImageData() {
             return imageData;
         }
 
         public void setImageData(byte[] imageData) {
             this.imageData = imageData;
         }
 
         public String getAnalogMinimum() {
             Double d = ((AnalogStatistics) stats).getMinimumValue();
             if (d == null)
                 return null;
             return textRenderer.getText(d, TextRenderer.HINT_FULL);
         }
 
         public String getAnalogMinTime() {
             Long l = ((AnalogStatistics) stats).getMinimumTime();
             if (l == null)
                 return null;
             return Functions.getFullMinuteTime(l);
         }
 
         public String getAnalogMaximum() {
             Double d = ((AnalogStatistics) stats).getMaximumValue();
             if (d == null)
                 return null;
             return textRenderer.getText(d, TextRenderer.HINT_FULL);
         }
 
         public String getAnalogMaxTime() {
             Long l = ((AnalogStatistics) stats).getMaximumTime();
             if (l == null)
                 return null;
             return Functions.getFullMinuteTime(l);
         }
 
         public String getAnalogAverage() {
             Double d = ((AnalogStatistics) stats).getAverage();
             if (d == null)
                 return null;
             return textRenderer.getText(d, TextRenderer.HINT_FULL);
         }
 
         public String getAnalogSum() {
             return textRenderer.getText(((AnalogStatistics) stats).getSum(), TextRenderer.HINT_FULL);
         }
 
         public String getAnalogCount() {
             return Integer.toString(((AnalogStatistics) stats).getCount());
         }
 
         public List<StartsAndRuntimeWrapper> getStartsAndRuntimes() {
             List<StartsAndRuntime> original = ((StartsAndRuntimeList) stats).getData();
             List<StartsAndRuntimeWrapper> result = new ArrayList<StartsAndRuntimeWrapper>(original.size());
             for (StartsAndRuntime sar : original)
                 result.add(new StartsAndRuntimeWrapper(sar, textRenderer));
             return result;
         }
 
         public String getValueChangeCount() {
             return Integer.toString(((ValueChangeCounter) stats).getChanges());
         }
 
         public boolean isChartData() {
             return numericTimeSeries != null || discreteTimeSeries != null;
         }
 
         public String getChartPath() {
             if (inlinePrefix != null)
                 return inlinePrefix + getChartName();
             return IMAGE_SERVLET + getChartName();
         }
 
         public String getChartName() {
             return "reportPointChart" + reportPointId + ".png";
         }
     }
 
     public static class StartsAndRuntimeWrapper {
         private static DecimalFormat percFormat = new DecimalFormat("0.#%");
         private final StartsAndRuntime sar;
         private final TextRenderer textRenderer;
 
         public StartsAndRuntimeWrapper(StartsAndRuntime sar, TextRenderer textRenderer) {
             this.sar = sar;
             this.textRenderer = textRenderer;
         }
 
         public String getValue() {
             return textRenderer.getText(sar.getDataValue(), TextRenderer.HINT_FULL);
         }
 
         public String getStarts() {
             return Integer.toString(sar.getStarts());
         }
 
         public String getRuntime() {
             return percFormat.format(sar.getProportion());
         }
     }
 
     class StreamHandler implements ExportDataStreamHandler {
         private final long start;
         private final long end;
         private final int imageWidth;
 
         File exportFile;
         private ExportCsvStreamer exportCsvStreamer;
 
         private final List<PointStatistics> pointStatistics;
         private final PointTimeSeriesCollection pointTimeSeriesCollection;
 
         private PointStatistics point;
         private NumericTimeSeries numericTimeSeries;
         private DiscreteTimeSeries discreteTimeSeries;
         private AbstractDataQuantizer quantizer;
 
         public StreamHandler(long start, long end, int imageWidth, boolean createExportFile, Translations translations) {
             pointStatistics = new ArrayList<PointStatistics>();
             pointTimeSeriesCollection = new PointTimeSeriesCollection(timeZone);
 
             this.start = start;
             this.end = end;
             this.imageWidth = imageWidth * 10;
             try {
                 if (createExportFile) {
                     exportFile = File.createTempFile("tempCSV", ".csv");
                     exportCsvStreamer = new ExportCsvStreamer(new PrintWriter(new FileWriter(exportFile)), translations);
                 }
             }
             catch (IOException e) {
                 LOG.error("Failed to create temp file", e);
             }
         }
 
         public List<PointStatistics> getPointStatistics() {
             return pointStatistics;
         }
 
         public PointTimeSeriesCollection getPointTimeSeriesCollection() {
             return pointTimeSeriesCollection;
         }
 
         @Override
         public void startPoint(ExportPointInfo pointInfo) {
             donePoint();
 
             point = new PointStatistics(pointInfo.getReportPointId());
             point.setName(pointInfo.getExtendedName());
             point.setDataType(pointInfo.getDataType());
             point.setDataTypeDescription(DataTypes.getDataTypeMessage(pointInfo.getDataType()).translate(translations));
             point.setTextRenderer(pointInfo.getTextRenderer());
             if (pointInfo.getStartValue() != null)
                 point.setStartValue(pointInfo.getTextRenderer().getText(pointInfo.getStartValue(),
                         TextRenderer.HINT_FULL));
             pointStatistics.add(point);
 
             Color colour = null;
             try {
                 if (pointInfo.getColour() != null)
                     colour = ColorUtils.toColor("#" + pointInfo.getColour());
             }
             catch (InvalidArgumentException e) {
                 // Should never happen, but leave the color null in case it does.
             }
 
             Stroke stroke = new BasicStroke(pointInfo.getWeight());
 
             if (pointInfo.getDataType() == DataTypes.NUMERIC) {
                 point.setStats(new AnalogStatistics(start, end, pointInfo.getStartValue() == null ? null : pointInfo
                         .getStartValue().getDoubleValue()));
 
                 discreteTimeSeries = null;
                TimeSeries timeSeries = new TimeSeries(pointInfo.getExtendedName());
                 timeSeries.setRangeDescription(point.getTextRenderer().getMetaText());
                 numericTimeSeries = new NumericTimeSeries(pointInfo.getPlotType(), timeSeries, colour, stroke);
                 point.setNumericTimeSeries(numericTimeSeries);
                 if (pointInfo.isConsolidatedChart())
                     pointTimeSeriesCollection.addNumericTimeSeries(numericTimeSeries);
 
                 quantizer = new NumericDataQuantizer(start, end, imageWidth,
                         new TimeSeriesQuantizerCallback(timeSeries));
             }
             else if (pointInfo.getDataType() == DataTypes.MULTISTATE) {
                 point.setStats(new StartsAndRuntimeList(start, end, pointInfo.getStartValue()));
 
                 discreteTimeSeries = new DiscreteTimeSeries(pointInfo.getExtendedName(), pointInfo.getTextRenderer(),
                         colour, stroke);
                 point.setDiscreteTimeSeries(discreteTimeSeries);
                 if (pointInfo.isConsolidatedChart())
                     pointTimeSeriesCollection.addDiscreteTimeSeries(discreteTimeSeries);
                 numericTimeSeries = null;
 
                 quantizer = new MultistateDataQuantizer(start, end, imageWidth,
                         new DiscreteTimeSeriesQuantizerCallback(discreteTimeSeries));
             }
             else if (pointInfo.getDataType() == DataTypes.BINARY) {
                 point.setStats(new StartsAndRuntimeList(start, end, pointInfo.getStartValue()));
 
                 discreteTimeSeries = new DiscreteTimeSeries(pointInfo.getExtendedName(), pointInfo.getTextRenderer(),
                         colour, stroke);
                 point.setDiscreteTimeSeries(discreteTimeSeries);
                 if (pointInfo.isConsolidatedChart())
                     pointTimeSeriesCollection.addDiscreteTimeSeries(discreteTimeSeries);
                 numericTimeSeries = null;
 
                 quantizer = new BinaryDataQuantizer(start, end, imageWidth, new DiscreteTimeSeriesQuantizerCallback(
                         discreteTimeSeries));
             }
             else if (pointInfo.getDataType() == DataTypes.ALPHANUMERIC) {
                 point.setStats(new ValueChangeCounter(start, end, pointInfo.getStartValue()));
                 quantizer = null;
 
                 discreteTimeSeries = null;
                 numericTimeSeries = null;
             }
             else if (pointInfo.getDataType() == DataTypes.IMAGE) {
                 point.setStats(new ValueChangeCounter(start, end, pointInfo.getStartValue()));
                 quantizer = null;
 
                 discreteTimeSeries = null;
                 numericTimeSeries = null;
             }
             else
                 throw new ShouldNeverHappenException("Unknown point data type: " + pointInfo.getDataType()
                         + " for point " + pointInfo.getReportPointId() + ", name=" + pointInfo.getExtendedName());
 
             if (exportCsvStreamer != null)
                 exportCsvStreamer.startPoint(pointInfo);
         }
 
         @Override
         public void pointData(ExportDataValue rdv) {
             if (quantizer != null)
                 quantizer.data(rdv);
             point.getStats().addValueTime(rdv);
             if (exportCsvStreamer != null)
                 exportCsvStreamer.pointData(rdv);
         }
 
         private void donePoint() {
             if (quantizer != null)
                 quantizer.done();
             if (point != null)
                 // Add in an end value to calculate stats until the end of the report. 
                 point.getStats().done(new PointValueTime(0D, end));
         }
 
         @Override
         public void done() {
             donePoint();
             if (exportCsvStreamer != null)
                 exportCsvStreamer.done();
         }
     }
 }
