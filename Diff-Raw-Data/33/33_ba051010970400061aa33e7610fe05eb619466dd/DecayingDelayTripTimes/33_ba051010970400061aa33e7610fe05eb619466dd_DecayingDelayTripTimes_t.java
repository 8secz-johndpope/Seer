 package org.opentripplanner.routing.trippattern;
 
import lombok.val;

 /**
  * An DelayedTripTimes applies an offset to arrival and departure times based on a report that a
  * vehicle is a given number of seconds early or late, and reports that the vehicle has 
  * passed all stops up to a certain point based on a report of vehicle location.
  */
 public class DecayingDelayTripTimes extends DelegatingTripTimes implements TripTimes {
 
     private final int currentStop;
     private final int delay;
     private final double k;
     private final boolean linear;
     // compute decay lookup table?
     
     public DecayingDelayTripTimes(ScheduledTripTimes sched, int currentStop, int delay, 
             double decay, boolean linear) {
         super(sched);
         this.delay = delay;
         this.currentStop = currentStop;
         this.k = decay;
         this.linear = linear;
     }
 
     @Override public int getDepartureTime(int hop) {
         int stop = hop;
         if (stop < currentStop)
             return TripTimes.PASSED;
         return super.getDepartureTime(hop) + decayedDelay(stop);
     }
     
     @Override public int getArrivalTime(int hop) {
         int stop = hop + 1;
         if (stop < currentStop)
             return TripTimes.PASSED;
         return super.getArrivalTime(hop) + decayedDelay(stop);
     }
         
     private int decayedDelay(int stop) {
         if (delay == 0) 
             return 0;
         int n = stop - currentStop;
        // This would make the decay symmetric about the current stop. Not currently needed, as 
        // we are reporting PASSED for all stops before currentStop.
        // n = Math.abs(n);
        double decay;
        if (linear)
            decay = (n > k) ? 0.0 : 1.0 - n / k;
        else  
            decay = Math.pow(k, n);
        return (int) (decay * delay);
    }
    
    public String toString() {
        val sb = new StringBuilder();
        sb.append(String.format("%s DecayingDelayTripTimes delay=%d stop=%d param=%3.2f\n", 
                linear ? "Linear" : "Exponential", delay, currentStop, k));
        for (int i = 0; i < getNumHops(); i++) {
            int j = 0;
            if (i >= currentStop)
                j = decayedDelay(i);
            sb.append(j);
            sb.append(' ');
         }
        sb.append('\n');
        sb.append(super.toString());
        return sb.toString();
     }
     
 }
