 
 package org.dcache.commons.stats;
 import java.util.Formatter;
 
 /**
  * this class stores an average of the execution time of the request
  * if the num is the num of updates that took place before this update
  * then the next average  is caclulated using the formula:
  *         newaverage =
  *          (previousaverage*num +nextmeasurment) /(num+1);
  * there is a utility method to read an average(mean), max, mean, RMS, standard deviation
  * and error on mean.
  * Separate average is kept for feeding into the rrd database.
  * This average is reset to the value of the last measurment
  * when it is read and the new average is calculated when new updates come
  *
  * @author timur
  */
 public class RequestExecutionTimeGauge {
     private final String name;
     // These are the variables that keep the
     // the average for the duration of the existance of the
     // gauge
     /**
      * average
      */
     private long averageExecutionTime=0;
     /**
      * Mininum
      */
     private long minExecutionTime=0;
     /**
      * Maximum
      */
     private long maxExecutionTime=0;
     /**
      *  Square of the RMS (Root Mean Square)
      *  sum(value_i)/n
      * RMSS(i+1)=(RMSS(i)+value(i+1)**2)/(i+1)
      */
     private long executionTimeRMSS=0;
     /**
      * number of updates
      */
     private int  updateNum=0;
     /**
      * last value fed to the gauge
      */
     private long lastExecutionTime=0;
     private final long startTime;
 
     // These are the variables that are reset every time
     // getAndResetAverageExecutionTime is called
     // for feeding into the RRD Database
 
     /**
      * TimeStamp of the beginning of the measurment period
      */
     private long periodStartTime;
     private long periodAverageExecutionTime=0;
     private long periodUpdateNum=0;
 
     /**
      *
      * @param name
      */
     public  RequestExecutionTimeGauge(String name) {
         this.name = name;
         startTime = System.currentTimeMillis();
         periodStartTime = startTime;
     }
 
     /**
      *
      * @param nextExecTime
      */
     public synchronized void update(long nextExecTime) {
         if(nextExecTime <0) {
             throw new IllegalArgumentException("negative nextExecTime: "+
                     nextExecTime);
         }
         // long term averages calculations
          if( updateNum==0 ) {
              averageExecutionTime = nextExecTime;
              minExecutionTime = nextExecTime;
              maxExecutionTime = nextExecTime;
              executionTimeRMSS=nextExecTime*nextExecTime;
 
          } else {
 
             averageExecutionTime =
                 (averageExecutionTime*updateNum +nextExecTime) /(updateNum+1);
              minExecutionTime = getMinExecutionTime() <nextExecTime?
                  getMinExecutionTime():nextExecTime;
              maxExecutionTime = getMaxExecutionTime()>nextExecTime?
                  getMaxExecutionTime():nextExecTime;
              executionTimeRMSS=
                      (executionTimeRMSS*updateNum+nextExecTime*nextExecTime)/
                      (updateNum+1);
          }
         updateNum++;
 
         // period averages caclucations
         periodAverageExecutionTime =
             (periodAverageExecutionTime*periodUpdateNum +nextExecTime) /
             (periodUpdateNum+1);
         periodUpdateNum++;
 
         lastExecutionTime = nextExecTime;
     }
 
     /**
      * return average over the lifetime of the gauge
      * @return
      */
     public synchronized long getAverageExecutionTime() {
         return averageExecutionTime;
     }
 
     /**
      * return average over the last period, and start new period
      * @return
      */
     public synchronized long getAndResetAverageExecutionTime() {
         long periodAverageExecutionTime = this.periodAverageExecutionTime;
         periodUpdateNum = 0;
         this.periodAverageExecutionTime = lastExecutionTime;
         periodStartTime = System.currentTimeMillis();
         return periodAverageExecutionTime;
     }
 
     /**
      *
      * @return String representation of this RequestExecutionTimeGauge
      *  Only long term statistics is printed
      */
         @Override
     public synchronized String toString() {
 
         String aName = name;
         if(name.length() >34) {
              aName = aName.substring(0,34);
         }
         long updatePeriod= System.currentTimeMillis() -
                 startTime;
         StringBuilder sb = new StringBuilder();
 
         Formatter formatter = new Formatter(sb);
 
        formatter.format("%-34s %12d±%10f %12d %12d %12d %12d %12d",
                 aName, averageExecutionTime,getStandardError(),
                 minExecutionTime,maxExecutionTime, 
                 getStandardDeviation(), updateNum, updatePeriod);
         formatter.flush();
         formatter.close();
 
         return sb.toString();
     }
 
     /**
      * @return the minExecutionTime
      */
     public synchronized long getMinExecutionTime() {
         return minExecutionTime;
     }
 
     /**
      * @return the maxExecutionTime
      */
     public synchronized long getMaxExecutionTime() {
         return maxExecutionTime;
     }
 
     /**
      * @return the name
      */
     public String getName() {
         return name;
     }
 
     /**
      * @return the RMS of executionTime
      */
     public synchronized double getExecutionTimeRMS() {
         return Math.sqrt(executionTimeRMSS);
     }
 
     public synchronized long getStandardDeviation() {
         long deviationSquare = executionTimeRMSS - averageExecutionTime*averageExecutionTime;
         assert (deviationSquare >=0);
         return (long) Math.sqrt(executionTimeRMSS - averageExecutionTime*averageExecutionTime);
     }
 
     /**
      * 
      * @return standard error of the mean
      */
     public synchronized double getStandardError() {
         return getStandardDeviation() / Math.sqrt(updateNum);
     }
     /**
      * @return the updateNum
      */
     public synchronized int getUpdateNum() {
         return updateNum;
     }
 
     /**
      * @return the lastExecutionTime
      */
     public synchronized long getLastExecutionTime() {
         return lastExecutionTime;
     }
 
     /**
      * @return the startTime
      */
     public synchronized long getStartTime() {
         return startTime;
     }
 
     /**
      * @return the periodStartTime
      */
     public synchronized long getPeriodStartTime() {
         return periodStartTime;
     }
 
     /**
      * @return the periodAverageExecutionTime
      */
     public synchronized long getPeriodAverageExecutionTime() {
         return periodAverageExecutionTime;
     }
 
     /**
      * @return the periodUpdateNum
      */
     public synchronized long getPeriodUpdateNum() {
         return periodUpdateNum;
     }
 
 
 }
