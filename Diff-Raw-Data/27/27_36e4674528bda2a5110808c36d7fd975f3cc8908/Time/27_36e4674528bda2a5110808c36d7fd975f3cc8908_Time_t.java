 package gov.usgs.cida.gdp.utilities.bean;
 
import com.thoughtworks.xstream.XStream;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.List;
 import com.thoughtworks.xstream.annotations.XStreamAlias;
 import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
 
 @XStreamAlias("availabletimes")
 public class Time extends Response {
 
     @XStreamAlias("times")
     @XStreamImplicit(itemFieldName = "time")
     private List<String> time;
     private TimeBreakdown starttime;
     private TimeBreakdown endtime;
 
     public Time() {
         this.time = new ArrayList<String>();
         this.starttime = new TimeBreakdown();
         this.endtime = new TimeBreakdown();
     }
 
     public Time(List<String> dateRange) throws ParseException {
         this.time = dateRange;
 
         if (this.time.isEmpty()) {
             this.starttime = new TimeBreakdown();
             this.endtime = new TimeBreakdown();
         } else {
             this.starttime = new TimeBreakdown(dateRange.get(0));
             this.endtime = new TimeBreakdown(dateRange.get(1));
         }
     }
 
     public void setTime(List<String> time) {
         this.time = time;
     }
 
     public List<String> getTime() {
         return time;
     }
 
     public void setStarttime(TimeBreakdown starttime) {
         this.starttime = starttime;
     }
 
     public TimeBreakdown getStarttime() {
         return starttime;
     }
 
     public void setEndtime(TimeBreakdown endtime) {
         this.endtime = endtime;
     }
 
     public TimeBreakdown getEndtime() {
         return endtime;
     }
 	
	@Override 
	public String toXML() {
		String result;
		QNameMap qmap = new QNameMap();
		qmap.setDefaultNamespace("gdptime-1.0.xsd");
		qmap.setDefaultPrefix("gdp");
		StaxDriver sd = new StaxDriver(qmap);
		XStream xstream = new XStream(sd);
		xstream.autodetectAnnotations(true);
		result = xstream.toXML(this);
		return result;
	}
	
 	@Override
 	public String toString() {
 		return this.time.get(0) + "|" + this.time.get(1);
 	}
 
     static class TimeBreakdown extends Response {
 
         private int month;
         private int day;
         private int year;
         private int timezone;
         private int hour;
         private int minute;
         private int second;
 
         public TimeBreakdown() {
         }
 
         public TimeBreakdown(String dateInstance) throws ParseException {
             SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // HACK can we use JODA?
             Date dateInstanceDate = null;
             try {
                 dateInstanceDate = sdf.parse(dateInstance);
             } catch (ParseException pe) {
                 throw pe;
             }
 
             Calendar cal = Calendar.getInstance();
             cal.setTime(dateInstanceDate);
             this.month = cal.get(Calendar.MONTH) + 1;
             this.day = cal.get(Calendar.DAY_OF_MONTH);
             this.year = cal.get(Calendar.YEAR);
             this.hour = cal.get(Calendar.HOUR_OF_DAY);
             this.minute = cal.get(Calendar.MINUTE);
             this.second = cal.get(Calendar.SECOND);
 
         }
 
         public TimeBreakdown(Calendar cal) {
             this.month = cal.get(Calendar.MONTH) + 1;
             this.day = cal.get(Calendar.DAY_OF_MONTH);
             this.year = cal.get(Calendar.YEAR);
             this.hour = cal.get(Calendar.HOUR_OF_DAY);
             this.minute = cal.get(Calendar.MINUTE);
             this.second = cal.get(Calendar.SECOND);
         }
 
         public int getMonth() {
             return month;
         }
 
         public void setMonth(int month) {
             this.month = month;
         }
 
         public int getDay() {
             return day;
         }
 
         public void setDay(int day) {
             this.day = day;
         }
 
         public int getYear() {
             return year;
         }
 
         public void setYear(int year) {
             this.year = year;
         }
 
         public int getTimezone() {
             return timezone;
         }
 
         public void setTimezone(int timezone) {
             this.timezone = timezone;
         }
 
         public int getHour() {
             return hour;
         }
 
         public void setHour(int hour) {
             this.hour = hour;
         }
 
         public int getMinute() {
             return minute;
         }
 
         public void setMinute(int minute) {
             this.minute = minute;
         }
 
         public int getSecond() {
             return second;
         }
 
         public void setSecond(int second) {
             this.second = second;
         }
     }
 }
