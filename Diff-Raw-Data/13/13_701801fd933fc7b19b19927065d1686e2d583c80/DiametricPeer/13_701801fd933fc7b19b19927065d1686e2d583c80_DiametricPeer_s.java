 package diametric;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.jruby.Ruby;
 import org.jruby.RubyArray;
 import org.jruby.RubyBoolean;
 import org.jruby.RubyClass;
 import org.jruby.RubyFixnum;
 import org.jruby.RubyHash;
 import org.jruby.RubyModule;
 import org.jruby.RubyNil;
 import org.jruby.RubyObject;
 import org.jruby.RubyString;
 import org.jruby.RubySymbol;
 import org.jruby.anno.JRubyMethod;
 import org.jruby.anno.JRubyModule;
 import org.jruby.javasupport.util.RuntimeHelpers;
 import org.jruby.runtime.Block;
 import org.jruby.runtime.ThreadContext;
 import org.jruby.runtime.builtin.IRubyObject;
 
 import datomic.Connection;
 import datomic.Database;
 import datomic.Peer;
 
 @JRubyModule(name="Diametric::Persistence::Peer")
 public class DiametricPeer extends RubyModule {
     private static final long serialVersionUID = 8659857729004427581L;
     
     protected DiametricPeer(Ruby runtime) {
         super(runtime);
     }
     
     private static DiametricConnection saved_connection = null;
     
     @JRubyMethod(meta=true)
     public static IRubyObject connect(ThreadContext context, IRubyObject klazz, IRubyObject arg) {
         String uriOrMap = null;
         if (arg instanceof RubyString) {
             uriOrMap = DiametricUtils.rubyStringToJava(arg);
         } else if (arg instanceof RubyHash) {
             RubySymbol key = RubySymbol.newSymbol(context.getRuntime(), "uri");
             RubyString value = (RubyString)((RubyHash)arg).op_aref(context, key);
             uriOrMap = DiametricUtils.rubyStringToJava(value);
         } else {
             throw context.getRuntime().newArgumentError("Argument should be a String or Hash");
         }
         if (uriOrMap == null )
             throw context.getRuntime().newArgumentError("Argument should be a String or Hash with :uri key");
         
         RubyClass clazz = (RubyClass) context.getRuntime().getClassFromPath("Diametric::Persistence::Connection");
         DiametricConnection rubyConnection = (DiametricConnection)clazz.allocate();
         try {
             // what value will be returned when connect fails? API doc doesn't tell anything.
             Connection connection = Peer.connect(uriOrMap);
             rubyConnection.init(connection);
             saved_connection = rubyConnection;
             return rubyConnection;
         } catch (Exception e) {
             // Diametric doesn't require creating database before connect.
             if (e.getMessage().contains(":peer/db-not-found") && Peer.createDatabase(uriOrMap)) {
                 Connection connection = Peer.connect(uriOrMap);
                 rubyConnection.init(connection);
                 return rubyConnection;
             }
         }
         throw context.getRuntime().newRuntimeError("Failed to create connection");
     }
     
     @JRubyMethod(meta=true)
     public static IRubyObject create_database(ThreadContext context, IRubyObject klazz, IRubyObject arg) {
         String uriOrMap = DiametricUtils.rubyStringToJava(arg);
         if (uriOrMap == null)
             throw context.getRuntime().newArgumentError("Argument should be a String");
         boolean status = Peer.createDatabase(uriOrMap);
         return RubyBoolean.newBoolean(context.getRuntime(), status);
     }
     
     @JRubyMethod(meta=true, required=2, rest=true)
     public static IRubyObject rename_database(ThreadContext context, IRubyObject klazz, IRubyObject[] args) {
         if (args.length != 2) return context.getRuntime().getNil();
         String uriOrMap = DiametricUtils.rubyStringToJava(args[0]);
         if (uriOrMap == null) return context.getRuntime().getNil();
         String newName = DiametricUtils.rubyStringToJava(args[1]);
         if (newName == null) return context.getRuntime().getNil();
         boolean status = Peer.renameDatabase(uriOrMap, newName);
         return RubyBoolean.newBoolean(context.getRuntime(), status);
     }
     
     @JRubyMethod(meta=true)
     public static IRubyObject delete_database(ThreadContext context, IRubyObject klazz, IRubyObject arg) {
         String uriOrMap = DiametricUtils.rubyStringToJava(arg);
         if (uriOrMap == null) return context.getRuntime().getNil();
         boolean status = Peer.deleteDatabase(uriOrMap);
         return RubyBoolean.newBoolean(context.getRuntime(), status);
     }
     
     /**
      * Constructs a semi-sequential UUID useful for creating UUIDs that don't fragment indexes
      * 
      * @param context
      * @param klazz
      * @return java.util.UUID. a UUID whose most significant 32 bits are currentTimeMillis rounded to seconds
      */
     @JRubyMethod(meta=true)
     public static IRubyObject squuid(ThreadContext context, IRubyObject klazz) {
         java.util.UUID java_uuid = Peer.squuid();
         RubyClass clazz = (RubyClass) context.getRuntime().getClassFromPath("Diametric::Persistence::UUID");
         diametric.DiametricUUID ruby_uuid = (diametric.DiametricUUID)clazz.allocate();
         ruby_uuid.init(java_uuid);
         return ruby_uuid;
     }
     
     /**
      * Gets the time part of a squuid
      * 
      * @param context
      * @param klazz
      * @param arg diametric.UUID. squuid -  a UUID created by squuid()
      * @return the time in the format of System.currentTimeMillis
      */
     @JRubyMethod(meta=true)
     public static IRubyObject squuid_time_millis(ThreadContext context, IRubyObject klazz, IRubyObject arg) {
         if (!(arg instanceof diametric.DiametricUUID)) return context.getRuntime().getNil();
         java.util.UUID squuid = ((diametric.DiametricUUID)arg).getUUID();
         if (squuid == null) return context.getRuntime().getNil();
         long value = Peer.squuidTimeMillis(squuid);
         return RubyFixnum.newFixnum(context.getRuntime(), value);
     }
     
     /**
      * Generates a temp id in the designated partition
      * In case the second argument is given,
      * it should be an idNumber from -1 (inclusive) to -1000000 (exclusive).
      * 
      * @param context
      * @param klazz
      * @param args the first argument: String. a partition, which is a keyword identifying the partition.
      * @return
      */
     @JRubyMethod(meta=true, required=1, optional=1)
     public static IRubyObject tempid(ThreadContext context, IRubyObject klazz, IRubyObject[] args) {
         String partition = DiametricUtils.rubyStringToJava(args[0]);
         RubyClass clazz = (RubyClass)context.getRuntime().getClassFromPath("Diametric::Persistence::Object");
         DiametricObject diametric_object = (DiametricObject)clazz.allocate();
         if (args.length > 1 && (args[1] instanceof RubyFixnum)) {
             long idNumber = (Long) args[1].toJava(Long.class);
             diametric_object.update(Peer.tempid(partition, idNumber));
         } else {
             diametric_object.update(Peer.tempid(partition));
         }
         return diametric_object;
     }
     
     /**
      * 
      * @param context
      * @param klazz
      * @param args Both 2 arguments should be DiametricObject.
      *             The first argument should hold clojure.lang.PersistentArrayMap.
      *             The second one should hold datomic.db.DbId.
      * @return
      */
     @JRubyMethod(meta=true, required=2, rest=true)
     public static IRubyObject resolve_tempid(ThreadContext context, IRubyObject klazz, IRubyObject[] args) {
         if ((args[0] instanceof DiametricObject) && (args[1] instanceof DiametricObject)) {
             Map map = (Map) ((DiametricObject)args[0]).toJava();
             DiametricObject ruby_object = (DiametricObject)args[1];
             Object tempid = Peer.resolveTempid((Database)map.get(Connection.DB_AFTER), map.get(Connection.TEMPIDS), ruby_object.toJava());
             ruby_object.update(tempid);
             return ruby_object;
         } else {
             throw context.getRuntime().newArgumentError("Wrong argument type.");
         }
     }
     
     @JRubyMethod(meta=true, required=2, rest=true)
     public static IRubyObject q(ThreadContext context, IRubyObject klazz, IRubyObject[] args) {
         if (!(args[0] instanceof RubyString)) return context.getRuntime().getNil();
         String query = (String)args[0].toJava(String.class);
         Collection<List<Object>> results = null;
         if (args.length == 2 && (args[1] instanceof DiametricDatabase)) {
             results = Peer.q(query, ((DiametricDatabase)args[1]).toJava());
         } else {
             List list = new ArrayList();
             for (int i=1; i<args.length; i++) {
                 list.add(DiametricUtils.convertRubyToJava(context, args[i]));
             }
             Object[] inputs = list.toArray(new Object[list.size()]);
             results = Peer.q(query, inputs);
         }
         if (results == null) return context.getRuntime().getNil();
         RubyArray ruby_results = RubyArray.newArray(context.getRuntime());
         for (List list : results) {
             RubyArray ruby_elements = RubyArray.newArray(context.getRuntime());
             for (Object element : list) {
                 ruby_elements.append(DiametricUtils.convertJavaToRuby(context, element));
             }
             ruby_results.append(ruby_elements);
         }
         return ruby_results;
     }
     
     private static List<RubyModule> bases = new ArrayList<RubyModule>();
     
     @JRubyMethod(meta=true)
     public static IRubyObject included(ThreadContext context, IRubyObject klazz, IRubyObject arg) {
         if (arg instanceof RubyModule) {
             RubyModule base = (RubyModule)arg;
             bases.add(base);
             base.instance_variable_set(RubyString.newString(context.getRuntime(), "@peer"), context.getRuntime().getTrue());
             System.out.println("INLCUDED BY: " + ((RubyModule)arg).getName());
         }
         return context.getRuntime().getNil();
     }
     
     @JRubyMethod(meta=true)
     public static IRubyObject connect(ThreadContext context, IRubyObject klazz) {
         if (saved_connection == null) return context.getRuntime().getNil();
         return saved_connection;
     }
     
     @JRubyMethod(meta=true)
     public static IRubyObject create_schemas(ThreadContext context, IRubyObject klazz, IRubyObject arg) {
        System.out.println("KLAZZ: " + klazz);
        System.out.println("FRAMESELF:" + context.getFrameSelf());
        System.out.println("FRAMEKLAZZ: " + context.getFrameKlazz());
         if (!(arg instanceof DiametricConnection))
             throw context.getRuntime().newArgumentError("Argument should be Connection.");
         IRubyObject result = context.getRuntime().getNil();
         for (RubyModule base : bases) {
             if (base.respondsTo("schema")) {
                 IRubyObject schema = base.send(context, RubySymbol.newSymbol(context.getRuntime(), "schema"), Block.NULL_BLOCK);
                System.out.println("SCHEMA: " + schema);
                 result = ((DiametricConnection)arg).transact(context, schema);
             }
         }
         return result;
     }
 }
