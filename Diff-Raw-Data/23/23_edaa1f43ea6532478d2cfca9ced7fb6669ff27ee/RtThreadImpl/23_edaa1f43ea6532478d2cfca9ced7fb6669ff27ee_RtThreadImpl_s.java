 /**
 *	RtThread.java
 */
 
 package com.jopdesign.sys;
 
 import joprt.RtThread;
 
 
 /**
  * @author Martin
  * 
  * Implementation class for the real-time thread RtThread.
  *
  */
 public class RtThreadImpl {
 
 	// usual priority levels of java.lang.Thread
 	public final static int MIN_PRIORITY = 1;
 	public final static int NORM_PRIORITY = 5;
 	public final static int MAX_PRIORITY = 10;
 
 	// priority levels above Thread
 	protected final static int RT_BASE = 2;
 	protected final static int RT_IDLE = 1;
 
 	protected final static int IDL_TICK = 10000;
 
 	private RtThread rtt;		// reference to RtThread's run method
 	private int priority;
 	private int period;			// period in us
 	private int offset;			// offset in us
 
 	// index in next, ref and event
 	int nr;
 	private int[] stack;
 	private int sp;
 
 	// allocated and set in startMission
 	// ordered by priority
 	private static int next[];			// next time to change to state running
 	private static RtThreadImpl[] ref;		// references to threads
 
 	final static int NO_EVENT = 0;
 	final static int EV_FIRED = 1;
 	final static int EV_WAITING = 2;
 	static int event[];					// state of an event
 	boolean isEvent;
 
 	private static int cnt;
 	private static int active;					// active thread number
 
 	// linked list of threads in priority order
 	private RtThreadImpl lower;
 	private static RtThreadImpl head;
 
 	// only used in startMission
 	protected final static int CREATED = 0;
 	protected final static int READY = 1;		// READY means ready to run.
 	protected final static int WAITING = 2;		// active is the running thread.
 	protected final static int DEAD = 3;
 	private int state;
 
 //	private final static int MAX_STACK = 128;
 
 	private static boolean initDone;
 	static boolean mission;
 
 
 	protected static Object monitor;
 
 	//	no synchronization necessary:
 	//	doInit() is called on first new RtThread() =>
 	//	only one (this calling) thread is now runnable.
 	//
 	//	However, to avoid stack issues we can also call
 	//	explicit
 	public static void init() {
 
 		if (initDone==true) return;
 		initDone = true;
 		mission = false;
 
 		monitor = new Object();
 
 		active = 0;			// main thread (or idl thread) is first thread
 		cnt = 1;			// stays 1 till startMission
 
 		next = new int[1];
 		ref = new RtThreadImpl[1];
 
 		head = null;
 
 		//	thread struct for main
 		ref[0] = new RtThreadImpl(NORM_PRIORITY, 0);
 		ref[0].state = READY;		// main thread is READY
 		next[0] = 0;
 
 		//	create one idle thread with Thread prio 0
 		//	If we have a main thread with 'active' (yielding)
 		//	sleep() this is not necessary.
 		//
 		//	Should be replaced by a Thread scheduler with
 		//	RT_IDLE priority
 /* main is now our idle task
 		new RtThread(0, 0) {
 			public void run() {
 				for (;;) {
 					util.Dbg.wr('i');
 				}
 			}
 		};
 */
 
 		// We have now more than one thread =>
 		// If we have 'normal' Threads we should start the timer!
 
 	}
 
 	// not necessary
 	// private RtThread() {};
 
 	private RtThreadImpl(int prio, int us) {
 	
 		this(null, prio, us, 0);
 	}
 
 	public RtThreadImpl(RtThread rtt, int prio, int us, int off) {
 
 //System.out.print("new Thread w prio ");
 //System.out.println(prio);
 //System.out.println("a");
 		if (!initDone) {
 			init();
 		}
 //System.out.println("b");
 
 		stack = new int[Const.STACK_SIZE-Const.STACK_OFF];
 		sp = Const.STACK_OFF;	// default empty stack for GC before startMission()
 //		System.out.print(MAX_STACK);
 //		System.out.println("c");
 for (int i=0; i<Const.STACK_SIZE-Const.STACK_OFF; ++i) {
 //	System.out.print(i);
 	stack[i] = 1234567;
 }
 //System.out.println("d");
 
 		this.rtt = rtt;
 		
 		period = us;
 		offset = off;
 		if (us==0)	{					// this is NOT a RT thread
 			priority = prio;
 		} else {						// RT prio is above Thread prios.
 			priority = prio+MAX_PRIORITY+RT_BASE;
 		}
 		state = CREATED;
 		isEvent = false;
 
 		//	insert in linked list, priority ordered
 		//	highest priority first.
 		//	same priority is ordered as first created has
 		//	'higher' priority.
 		RtThreadImpl th = head;
 		RtThreadImpl prev = null;
 		while (th!=null && priority<=th.priority) {
 			prev = th;
 			th = th.lower;
 		}
 		lower = th;
 		if (prev!=null) {
 			prev.lower = this;
 		} else {
 			head = this;
 		}
 	}
 
 
 	private static void genInt() {
 		
 		// just schedule an interrupt
 		// schedule() gets called.
 		Native.wr(0, Const.IO_SWINT);
 		for (int j=0;j<10;++j) ;
 	}
 
 
 //	time stamps:
 public static int ts0, ts1, ts2, ts3, ts4;
 
 	private static int s1;		// helper var
 
 	private static int tim;		// next timer value
 	// timer offset to ensure that no timer int happens just
 	// after monitorexit in this method and the new thread
 	// has a minimum time to run.
 //	private final static int TIM_OFF = 100;
 	private final static int TIM_OFF = 2; // for 100 MHz version 20 or even lower
 										 // 2 is minimum
 
 	//	this is the one and only function to
 	//	switch threads.
 	//	schedule() is called from JVMHelp.interrupt()
 	//	and should NEVER be called from somewhere
 	//	else.
 	//	Interrupts (also yield/genInt()) should NEVER
 	//	ocour befor startMission is called (ref and active are set)
 	static void schedule() {
 
 		int i, j, k;
 		int diff;
 
 		// we have not called doInit(), which means
 		// we have only one thread => just return
 		if (!initDone) return;
 
 		Native.wr(0, Const.IO_INT_ENA);
 		// synchronized(monitor) {
 			// save stack
 			i = Native.getSP();
 			RtThreadImpl th = ref[active];
 			th.sp = i;
			Native.int2extMem(Const.STACK_OFF, th.stack, i-Const.STACK_OFF+1);	// cnt is i-128+1
 
 			// SCHEDULE
 			//	cnt should NOT contain idle thread
 			//	change this some time
 			k = IDL_TICK;
 
 			// this is now
 			j = Native.rd(Const.IO_US_CNT);
 
 			for (i=cnt-1; i>0; --i) {
 
 				if (event[i] == EV_FIRED) {
 					break;						// a pending event found
 				} else if (event[i] == NO_EVENT) {
 					diff = next[i]-j;			// check only periodic
 					if (diff < TIM_OFF) {
 						break;					// found a ready task
 					} else if (diff < k) {
 						k = diff;				// next int time of higher prio task
 					}
 				}
 			}
 			// i is next ready thread (index in new list)
 			// If none is ready i points to idle task or main thread (fist in the list)
 			active = i;	
 
 			// set next int time to now+(min(diff)) (j, k)
 			tim = j+k;
 
 			// restore stack
 			s1 = ref[i].sp;
			Native.setVP(s1+2);		// +2 for shure ???
 			Native.setSP(s1+7);		// +5 locals, take care to use only the first 5!!
 
 			i = s1;
 			// can't use s1-127 as count,
 			// don't know why I have to store it in a local.
			Native.ext2intMem(ref[active].stack, Const.STACK_OFF, i-Const.STACK_OFF+1);		// cnt is i-128+1
 
 			j = Native.rd(Const.IO_US_CNT);
 			// check if next timer value is too early (or allready missed)
 			// ack int and schedule timer
 			if (tim-j<TIM_OFF) {
 				// set timer to now plus some short time
 				Native.wr(j+TIM_OFF, Const.IO_TIMER);
 			} else {
 				Native.wr(tim, Const.IO_TIMER);
 			}
 			Native.setSP(i);
 			// only return after setSP!
 			// WHY should this be true? We need a monitorexit AFTER setSP().
 			// It compiles to following:
 			//	invokestatic #32 <Method void setSP(int)>
 			//	aload 5
 			//	monitorexit
 			//	goto 283
 			//	...
  			//	283 return
 			//
 			// for a 'real monitor' we have a big problem:
 			// aload 5 loads the monitor from the OLD stack!!!
 			//
 			// we can't access any 'old' locals now
 			//
 			// a solution: don't use a monitor here!
 			// disable and enable INT 'manual'
 			// and DON'T call a method with synchronized
 			// it would enable the INT on monitorexit
 		Native.wr(1, Const.IO_INT_ENA);
 		// }
 	}
 
 	private void startThread() {
 
 		if (state!=CREATED) return;		// allread called start
 
 		// if we have int enabled we have to synchronize
 
 		if (period==0) {
 			state = READY;			// for the idle thread
 		} else {
 			state = WAITING;
 		}
 
 		createStack();
 
 		// new thread starts right here after first scheduled
 
 		if (mission) {		// main (startMission) falls through
 			rtt.run();
 			// if we arrive here it's time to delete runtime struct of thread
 			// now do nothing!
 			state = DEAD;
 			for (;;) {
 				// This will not work if we change stack like in Thread.java.
 				// Then we have no reference to this.
 				next[nr] = Native.rd(Const.IO_US_CNT) + 2*IDL_TICK;
 				genInt();
 			}
 		}
 	}
 
 	/**
 	*	Create stack for the new thread.
 	*	Copy stack frame of main thread.
 	*	Could be reduced to copy only frames from 
 	*	createStack() and startThread() and adjust the
 	*	frames to new position.
 	*/
 	private void createStack() {
 
 		int i, j, k;
 
 		i = Native.getSP();					// sp of createStack();
 		j = Native.rdIntMem(i-4);			// sp of calling function
 		j = Native.rdIntMem(j-4);			// one more level of indirection
 
 		sp = i-j+Const.STACK_OFF;
 		k = j;
 		for (; j<=i; ++j) {
 			stack[j-k] = Native.rdIntMem(j);
 		}
 		//	adjust stack frames
 		k -= Const.STACK_OFF;	// now difference between main stack and new stack
 		stack[sp-Const.STACK_OFF-2] -= k;				// saved vp
 		stack[sp-Const.STACK_OFF-4] -= k;				// saved sp
 		j = stack[sp-Const.STACK_OFF-4];
 		stack[j-Const.STACK_OFF-2] -= k;
 		stack[j-Const.STACK_OFF-4] -= k;
 		
 /*	this is the save version
 		i = Native.getSP();
 		sp = i;
		for (j=128; j<=i; ++j) {
			stack[j-128] = Native.rdIntMem(j);
 		}
 */
 	}
 
 //	public void run() {
 //		;							// nothing to do
 //	}
 
 
 	public static void startMission() {
 
 
 		int i, c, startTime;
 		RtThreadImpl th, mth;
 
 		if (!initDone) {
 			init();
 		}
 
 		// if we have int's enabled for Thread scheduling
 		// we have to place a monitorenter here
 		th = head;
 		for (c=0; th!=null; ++c) {
 			th = th.lower;
 		}
 
 		mth = ref[0];		// this was our main thread
 
 		ref = new RtThreadImpl[c];
 		next = new int[c];
 		event = new int[c];
 
 		th = head;
 		// array is order according priority
 		// top priority is last!
 		for (i=c-1; th!=null; --i) {
 			ref[i] = th;
 			th.nr = i;
 			if (th.isEvent) {
 				event[i] = EV_WAITING;
 			} else {
 				event[i] = NO_EVENT;
 			}
 			th = th.lower;
 		}
 
 		// change active if a lower priority
 		// thread is befor main
 		active = mth.nr;
 
 		// running threads (state!=CREATED)
 		// are not started
 		// TODO: where are 'normal' Threads placed?
 		for (i=0; i<c; ++i) {
 			ref[i].startThread();
 		}
 
 		// wait 100 ms (for main Thread.debug())
 		startTime = Native.rd(Const.IO_US_CNT)+100000;
 		for (i=0; i<c; ++i) {
 			next[i] = startTime+ref[i].offset;
 		}
 
 		cnt = c;
 		mission = true;
 
 		// set moncnt in jvm.asm to zero to enable int's
 		// on monitorexit from now on
 		Native.wrIntMem(0, 5);
 		// clear all pending interrupts (e.g. timer after reset)
 		Native.wr(1, Const.IO_INTCLEARALL);
 		// schedule timer in 100 ms
 		Native.wr(startTime, Const.IO_TIMER);
 
 		// enable all interrupts int
 		Native.wr(-1, Const.IO_INTMASK);		
 		Native.wr(1, Const.IO_INT_ENA);
 
 	}
 
 
 	public boolean waitForNextPeriod() {
 
 		synchronized(monitor) {
 			int nxt, now;
 
 			nxt = next[nr] + period;
 
 			now = Native.rd(Const.IO_US_CNT);
 			if (nxt-now < 0) {					// missed time!
 				next[nr] = now;					// correct next
 //				next[nr] = nxt;					// without correction!
 				return false;
 			} else {
 				next[nr] = nxt;
 			}
 			// state is not used in scheduling!
 			// state = WAITING;
 
 			// just schedule an interrupt
 			// schedule() gets called.
 			Native.wr(0, Const.IO_SWINT);
 			for (int j=0;j<10;++j) ;
 			// will arrive befor return statement,
 			// just after monitorexit
 		}
 		return true;
 	}
 
 	public void setEvent() {
 		isEvent = true;
 	}
 
 	public void fire() {
 		event[this.nr] = EV_FIRED;
 		// if prio higher...
 // should not be allowed befor startMission
 		genInt();
 
 	}
 	
 	public void blockEvent() {
 		event[this.nr] = EV_WAITING;
 		genInt();
 
 	}
 	/**
 	*	dummy yield() for compatibility reason.
 	*/
 //	public static void yield() {}
 
 
 	/**
 	*	for 'soft' rt threads.
 	*/
 
 	public static void sleepMs(int millis) {
 	
 		int next = Native.rd(Const.IO_US_CNT)+millis*1000;
 		while (Native.rd(Const.IO_US_CNT)-next < 0) {
 			genInt();
 		}
 	}
 	final static int MIN_US = 10;
 
 	/**
 	 * Waste CPU cycles to simulate work.
 	 * @param us execution time in us
 	 */
 	public static void busyWait(int us) {
 
 		int t1, t2, t3;
 		int cnt;
 		
 		cnt = 0;	
 		t1 = Native.rd(Const.IO_US_CNT);
 
 		for (;;) {
 			t2 = Native.rd(Const.IO_US_CNT);
 			t3 = t2-t1;
 //			System.out.println(cnt+" "+t3);
 			t1 = t2;
 			if (t3<MIN_US) {
 				cnt += t3;
 			}
 			if (cnt>=us) {
 				return;
 			}
 		}
 	}
 
 //	 TODO: Decide how to protect the total root set (ref[].stack and active
 //  stack while assembling it. Then some writebarrier should protect the 
 //  references and downgrade the GC state from black to grey?
 
 	static int[] getStack(int num) {
 		return ref[num].stack;
 	}
 
 	static int getSP(int num) {
 		return ref[num].sp;
 	}
 
 	static int getCnt() {
 		return cnt;
 	}
 	
 	static int getActive() {
 		return active;
 	}
 
 	
 // WARNING: debug can take a long time (xx ms)
 public static void debug() {
 
 	synchronized(monitor) {
 		
 
 		int i, j;
 		for (i=cnt-1; i>=0; --i) {
 
 			util.Dbg.wr('\n');
 			util.Dbg.intVal(ref[i].sp);
 			util.Dbg.wr('\n');
			for (j=0; j<=ref[i].sp-128; ++j) {
 				util.Dbg.intVal(ref[i].stack[j]);
 			}
 			util.Dbg.wr('\n');
 trace(ref[i].stack, ref[i].sp);
 		}
 /*
 		int i, tim;
 
 		tim = Native.rd(Native.IO_US_CNT);
 		util.Dbg.wr(' ');
 		util.Dbg.intVal(active);
 		util.Dbg.wr('-');
 		util.Dbg.wr(' ');
 		for (i=0; i<cnt; ++i) {
 			util.Dbg.intVal(ref[i].nr);
 			util.Dbg.intVal(ref[i].priority);
 			util.Dbg.intVal(ref[i].state);
 			util.Dbg.intVal(next[i]-tim);
 		}
 		util.Dbg.wr('\n');
 		tim = Native.rd(Native.IO_US_CNT)-tim;
 		util.Dbg.intVal(tim);
 		util.Dbg.wr('\n');
 */
 	}
 }
 
 static void trace(int[] stack, int sp) {
 
 	int fp, mp, vp, addr, loc, args;
 	int val;
 
 	fp = sp-4;		// first frame point is easy, since last sp points to the end of the frame
 
	while (fp>128+5) {	// stop befor 'fist' method
		mp = stack[fp+4-128];
		vp = stack[fp+2-128];
 		val = Native.rdMem(mp);
 		addr = val>>>10;			// address of callee
 		util.Dbg.intVal(addr);
 
 		val = Native.rdMem(mp+1);	// cp, locals, args
 		args = val & 0x1f;
 		loc = (val>>>5) & 0x1f;
 		fp = vp+args+loc;			// new fp can be calc. with vp and count of local vars
 	}
 }
 
 
 
 }
