 package nachos.threads;
 
 import nachos.machine.*;
 
 import java.util.Random;
 import java.util.TreeSet;
 import java.util.HashSet;
 import java.util.Iterator;
 
 /**
  * A scheduler that chooses threads using a lottery.
  *
  * <p>
  * A lottery scheduler associates a number of tickets with each thread. When a
  * thread needs to be dequeued, a random lottery is held, among all the tickets
  * of all the threads waiting to be dequeued. The thread that holds the winning
  * ticket is chosen.
  *
  * <p>
  * Note that a lottery scheduler must be able to handle a lot of tickets
  * (sometimes billions), so it is not acceptable to maintain state for every
  * ticket.
  *
  * <p>
  * A lottery scheduler must partially solve the priority inversion problem; in
  * particular, tickets must be transferred through locks, and through joins.
  * Unlike a priority scheduler, these tickets add (as opposed to just taking
  * the maximum).
  */
 public class LotteryScheduler extends PriorityScheduler {
     public static final int priorityMinimum = 1;
     public static final int priorityMaximum = Integer.MAX_VALUE;
     /**
      * Allocate a new lottery scheduler.
      */
     public LotteryScheduler() {
     }
     //DONE!!!!
     protected ThreadState getThreadState(KThread thread) {
 		if (thread.schedulingState == null)
 			thread.schedulingState = new LotteryThreadState(thread);
 		return (ThreadState) thread.schedulingState;
     }
     /**
      * Allocate a new lottery thread queue.
      *
      * @param	transferPriority	<tt>true</tt> if this queue should
      *					transfer tickets from waiting threads
      *					to the owning thread.
      * @return	a new lottery thread queue.
      */
     public ThreadQueue newThreadQueue(boolean transferPriority) {
         return new LotteryQueue(transferPriority);
     }
     
     protected class LotteryQueue extends PriorityQueue {
         //In terms of picking the next thread linear in the number of threads on the queue is fine
         LotteryQueue(boolean transferPriority) {
             super(transferPriority);
         }
         public void updateEntry(ThreadState ts, int newEffectivePriority) {
             int oldPriority = ts.getEffectivePriority();
 			ts.effectivePriority = newEffectivePriority;
             //propagate
             int difference = newEffectivePriority-oldPriority;
             if(difference != 0)
                 ts.propagate(difference);
 		}
         //DONE!!!!!
         protected ThreadState pickNextThread() {
             //Set up an Iterator and go through it
             Random randomGenerator = new Random();
             int ticketCount = 0;
             Iterator<ThreadState> itr = this.waitQueue.iterator();
             while(itr.hasNext()) {
                 ticketCount += itr.next().getEffectivePriority();
             }
             if(ticketCount > 0) {
                 int num = randomGenerator.nextInt(ticketCount);
                 itr = this.waitQueue.iterator();
                 ThreadState temp;
                 while(itr.hasNext()) {
                     temp = itr.next();
                     num -= temp.effectivePriority;
                     if(num <= 0){
                         return temp;
                     }
                 }
             }
             return null;
         }
     }
     protected class LotteryThreadState extends ThreadState {
         public LotteryThreadState(KThread thread) {
             super(thread);
         }
         //DONE!!!!
         public void setPriority(int newPriority) {
             this.priority = newPriority;
             this.updateEffectivePriority();
         }
         //DONE!!!!
         public void propagate(int difference) {
             if(pqWant != null) {
                 if(pqWant.transferPriority == true) {
                     if(pqWant.holder != null)
                         pqWant.updateEntry(pqWant.holder, pqWant.holder.effectivePriority+difference);
                 }
             }
         }
         //DONE!!!!
         public void updateEffectivePriority() {
             //Calculate new effectivePriority checking possible donations from threads that are waiting for me
             int sumPriority = this.priority;
             for (PriorityQueue pq: this.pqHave)
                 if (pq.transferPriority == true) {
                     Iterator<ThreadState> itr = pq.waitQueue.iterator();
                     while(itr.hasNext())
                         sumPriority += itr.next().getEffectivePriority();
                 }
             
             //If there is a change in priority, update and propagate to other owners
             if (sumPriority != this.effectivePriority) {
                 int difference = sumPriority - this.effectivePriority;
                 this.effectivePriority = sumPriority;
                 this.propagate(difference);
             }
         }
         //DONE!!!!
         public void waitForAccess(PriorityQueue pq) {
 			this.pqWant = pq;
 			//this.time = Machine.timer().getTime();
 			this.time = TickTimer++;
 			pq.waitQueue.add(this);
 			//Propagate this ThreadState's effectivePriority to holder of pq
             if (pq.transferPriority == true) {
                 if(pq.holder != null)
                     pq.updateEntry(pq.holder, pq.holder.effectivePriority+this.effectivePriority);
             }
             
 		}
         //Added a line to acquire in PriorityScheduler
         //updateEffectivePriority() at the very end of acquire
     }
     
     public static void selfTest() {
         LotteryScheduler ls = new LotteryScheduler();
         LotteryQueue[] pq = new LotteryQueue[5];
         KThread[] t = new KThread[5];
        ThreadState lts[] = new LotteryThreadState[5];
         
         for (int i=0; i < 5; i++)
             pq[i] = ls.new LotteryQueue(true);
         for (int i=0; i < 5; i++) {
             t[i] = new KThread();
             t[i].setName("thread" + i);
             lts[i] = ls.getThreadState(t[i]);
         }
         
         Machine.interrupt().disable();
         
         System.out.println("===========LotteryScheduler Test============");
         pq[0].print();
         lts[0].print();
         pq[0].acquire(t[0]);
         System.out.println("pq[0].acquire(t[0])");
         pq[0].print();
         
         lts[0].setPriority(5);
         System.out.println("ts[0].setPriority(5)");
         pq[0].print():
         
         pq[0].waitForAccess(t[1]);
         System.out.println("pq[0].waitForAccess(t[1])");
         pq[0].print();
         
         lts[1].setPriority(3);
         System.out.println("lts[1].setPriority(3)");
         pq[0].print();
         
         Machine.interrupt().enable();
     }
 }
