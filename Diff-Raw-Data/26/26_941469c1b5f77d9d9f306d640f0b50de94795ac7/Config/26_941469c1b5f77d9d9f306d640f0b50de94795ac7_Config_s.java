 package ibis.satin.impl;
 
 import ibis.util.TypedProperties;
 
 /**
  * Constants for the configuration of Satin. This interface is public because it
  * is also used in code generated by the Satin frontend.
  */
 
 public interface Config {
 
     static final String PROPERTY_PREFIX = "satin.";
     static final String s_stats_spawn =  PROPERTY_PREFIX + "stats.spawn";
     static final String s_stats_steal =  PROPERTY_PREFIX + "stats.steal";
     static final String s_stats_abort =  PROPERTY_PREFIX + "stats.abort";
     static final String s_stats_tuple =  PROPERTY_PREFIX + "stats.tuple";
     static final String s_stats_ft =     PROPERTY_PREFIX + "stats.ft";
     static final String s_stats_grt =    PROPERTY_PREFIX + "stats.ft.grt";
 
     static final String s_timing_steal = PROPERTY_PREFIX + "timing.steal";
     static final String s_timing_abort = PROPERTY_PREFIX + "timing.abort";
     static final String s_timing_idle =  PROPERTY_PREFIX + "timing.idle";
     static final String s_timing_poll =  PROPERTY_PREFIX + "timing.poll";
     static final String s_timing_tuple = PROPERTY_PREFIX + "timing.tuple";
     static final String s_timing_grt =   PROPERTY_PREFIX + "timing.ft.grt";
     static final String s_timing_crash = PROPERTY_PREFIX + "timing.ft.crash";
     static final String s_timing_check = PROPERTY_PREFIX + "timing.ft.check";
     static final String s_timing_repl =  PROPERTY_PREFIX + "timing.ft.replica";
     static final String s_poll_freq =    PROPERTY_PREFIX + "pollfreq";
     static final String s_poll_port =    PROPERTY_PREFIX + "pollport";
     static final String s_asserts =      PROPERTY_PREFIX + "asserts";
     static final String s_aborts =       PROPERTY_PREFIX + "aborts";
     static final String s_ft =           PROPERTY_PREFIX + "ft";
     static final String s_ft_grt_repl =  PROPERTY_PREFIX + "ft.grt.replicated";
     static final String s_ft_grt_comb =  PROPERTY_PREFIX + "ft.grt.combine";
     static final String s_ft_noAborts =  PROPERTY_PREFIX + "ft.noAborts";
     static final String s_ft_naive =     PROPERTY_PREFIX + "ft.noTable";
     static final String s_in_latency =   PROPERTY_PREFIX + "messagesInLatency";
     static final String s_tuple_multi =  PROPERTY_PREFIX + "tuplespace.multicast";
     static final String s_tuple_ordered= PROPERTY_PREFIX + "tuplespace.ordered";
     static final String s_tuple_ordened= PROPERTY_PREFIX + "tuplespace.ordened";
     static final String s_tuple_numbered = PROPERTY_PREFIX + "tuplespace.numbered";
     static final String s_debug_comm =   PROPERTY_PREFIX + "debug.comm";
     static final String s_debug_steal =  PROPERTY_PREFIX + "debug.steal";
     static final String s_debug_spawn =  PROPERTY_PREFIX + "debug.spawn";
     static final String s_debug_inlet =  PROPERTY_PREFIX + "debug.inlet";
     static final String s_debug_abort =  PROPERTY_PREFIX + "debug.abort";
     static final String s_debug_tuple =  PROPERTY_PREFIX + "debug.tuple";
     static final String s_debug_ft_grt = PROPERTY_PREFIX + "debug.ft.grt";
 
     static final String[] sysprops = {
 	s_stats_spawn,
 	s_stats_steal,
 	s_stats_abort,
 	s_stats_tuple,
 	s_stats_ft,
 	s_stats_grt,
 	s_timing_steal,
 	s_timing_abort,
 	s_timing_idle,
 	s_timing_poll,
 	s_timing_tuple,
 	s_timing_grt,
 	s_timing_crash,
 	s_timing_check,
 	s_timing_repl,
 	s_poll_freq,
 	s_poll_port,
 	s_asserts,
 	s_aborts,
 	s_ft,
 	s_ft_grt_repl,
 	s_ft_grt_comb,
 	s_ft_noAborts,
 	s_ft_naive,
 	s_in_latency,
 	s_tuple_multi,
 	s_tuple_ordered,
 	s_tuple_numbered,
 	s_debug_comm,
 	s_debug_steal,
 	s_debug_spawn,
 	s_debug_inlet,
 	s_debug_abort,
 	s_debug_tuple,
 	s_debug_ft_grt
     };
 
     /** Enable or disable statistics for spawns. */
     static final boolean SPAWN_STATS = 
 	TypedProperties.booleanProperty(s_stats_spawn, true);
 
     /** Enable or disable statistics for job stealing. */
     static final boolean STEAL_STATS =
 	TypedProperties.booleanProperty(s_stats_steal, true);
 
     /** Enable or disable statistics for aborts. */
     static final boolean ABORT_STATS =
 	TypedProperties.booleanProperty(s_stats_abort, true);
 
     /** Enable or disable statistics for aborts/restarts done for fault-tolerance. */
     static final boolean FT_STATS =
 	TypedProperties.booleanProperty(s_stats_ft, true);
 
     /** Enable or disable statistics for the tuple space. */
     static final boolean TUPLE_STATS =
 	TypedProperties.booleanProperty(s_stats_tuple, true);
 
     /** Enable or disable statistics for the global result table. */
     static final boolean GRT_STATS =
 	TypedProperties.booleanProperty(s_stats_grt, true);
 
     /** Enable or disable steal timings. */
     static final boolean STEAL_TIMING =
 	TypedProperties.booleanProperty(s_timing_steal, true);
 
     /** Enable or disable abort timings. */
     static final boolean ABORT_TIMING =
 	TypedProperties.booleanProperty(s_timing_abort, true);
 
     /** Enable or disable idle timing. */
     static final boolean IDLE_TIMING =
 	TypedProperties.booleanProperty(s_timing_idle, false);
 
     /** Enable or disable poll timing. */
     static final boolean POLL_TIMING =
 	TypedProperties.booleanProperty(s_timing_poll, false);
 
    /** Enable or disable tuple space timing. * */
     static final boolean TUPLE_TIMING =
 	TypedProperties.booleanProperty(s_timing_tuple, true);
 
     //used for fault tolerance with global result table
     static final boolean GRT_TIMING =
 	TypedProperties.booleanProperty(s_timing_grt, true);
     static final boolean CRASH_TIMING =
 	TypedProperties.booleanProperty(s_timing_crash, true);
     static final boolean TABLE_CHECK_TIMING =
 	TypedProperties.booleanProperty(s_timing_check, true);
     static final boolean ADD_REPLICA_TIMING =
 	TypedProperties.booleanProperty(s_timing_repl, true);
 
 
     /**
      * The poll frequency in nanoseconds. A frequency of 0 means do not poll. A
      * frequency smaller than 0 means poll every sync.
      */
     static final long POLL_FREQ =
 	TypedProperties.longProperty(s_poll_freq, 0L);
 
    /** When polling, poll the satin receiveport. * */
     static final boolean POLL_RECEIVEPORT =
 	TypedProperties.booleanProperty(s_poll_port, true);
 
    /** Enable or disable asserts. * */
     static final boolean ASSERTS =
 	TypedProperties.booleanProperty(s_asserts, true);
 
     /** Enable or disable aborts and inlets. */
     static final boolean ABORTS =
 	TypedProperties.booleanProperty(s_aborts, true);
 
     /** Enable fault tolerance. */
     static final boolean FAULT_TOLERANCE =
 	TypedProperties.booleanProperty(s_ft, false);
     /**
      * If true, the global result table is replicated if false, the table is
      * distributed
      */
     static final boolean GLOBAL_RESULT_TABLE_REPLICATED =
 	TypedProperties.booleanProperty(s_ft_grt_repl, false);
     /**
      *Use message combinining with global result table
      */
     static final boolean GRT_MESSAGE_COMBINING =
 	TypedProperties.booleanProperty(s_ft_grt_comb, true);
     /** 
      * If true, orphan jobs are not aborted. Pointers are stored immediately
      * after crashes and results later. Jobs stolen by crashed processors
      * are not put back into the work queue. The are marked as dead and 
      * redone when their parent is on the top of the stack.
      * Do not use with replicated table
      */
     static final boolean FT_WITHOUT_ABORTS =
 	TypedProperties.booleanProperty(s_ft_noAborts, false);
 
     /**
      * Fault tolerance with restarting crashed jobs, but without the table
      */
     static final boolean FT_NAIVE =
 	TypedProperties.booleanProperty(s_ft_naive, false);
 
     /** Enable or disable an optimization for handling delayed messages. */
     static final boolean HANDLE_MESSAGES_IN_LATENCY =
 	TypedProperties.booleanProperty(s_in_latency, false);
 
     /** Use multicast to update the tuple space */
     /** Don't use with fault tolerance. Multicast ports don't work with crashes yet*/
     static final boolean SUPPORT_TUPLE_MULTICAST =
 	TypedProperties.booleanProperty(s_tuple_multi, true);
     static final boolean TUPLE_ORDERED =
 	TypedProperties.booleanProperty(s_tuple_ordered, false)
 	|| TypedProperties.booleanProperty(s_tuple_ordened, false)
 	|| TypedProperties.booleanProperty(s_tuple_numbered, false);
 
     /** Enable or disable debug prints concerning communication. */
     static final boolean COMM_DEBUG =
 	TypedProperties.booleanProperty(s_debug_comm, false);
 
     /** Enable or disable debug prints concerning job stealing. */
     static final boolean STEAL_DEBUG =
 	TypedProperties.booleanProperty(s_debug_steal, false);
 
     /** Enable or disable debug prints concerning spawns. */
     static final boolean SPAWN_DEBUG =
 	TypedProperties.booleanProperty(s_debug_spawn, false);
 
     /**
      * Enable or disable debug prints concerning inlets (exception handling).
      */
     static final boolean INLET_DEBUG =
 	TypedProperties.booleanProperty(s_debug_inlet, false);
 
     /** Enable or disable debug prints concerning aborts. */
     static final boolean ABORT_DEBUG =
 	TypedProperties.booleanProperty(s_debug_abort, false);
 
     /** Enable or disable debug prints concerning the global result table. */
     static final boolean GRT_DEBUG =
 	TypedProperties.booleanProperty(s_debug_ft_grt, false);
 
     /** Enable or disable debug prints concerning the tuple space. */
     static final boolean TUPLE_DEBUG =
 	TypedProperties.booleanProperty(s_debug_tuple, false);
 }
