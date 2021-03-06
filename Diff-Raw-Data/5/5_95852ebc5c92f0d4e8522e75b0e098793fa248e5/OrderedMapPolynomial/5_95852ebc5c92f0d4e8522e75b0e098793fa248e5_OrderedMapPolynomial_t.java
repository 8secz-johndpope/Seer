 /*
  * $Id$
  */
 
 package edu.jas.poly;
 
 import java.util.Set;
 import java.util.Map;
 import java.util.SortedMap;
 import java.util.TreeMap;
 import java.util.Iterator;
 
 import org.apache.log4j.Logger;
 
 import edu.jas.arith.Coefficient;
 
 /**
  * Ordered Map Polynomial. 
  * Abstract implementation of OrderedPolynomial.
  * Implementation based on Sorted Map / TreeMap
  */
 
 public abstract class OrderedMapPolynomial /* extends MapPolynomial */ 
                       implements OrderedPolynomial, Cloneable {
 
     private static Logger logger = Logger.getLogger(OrderedMapPolynomial.class);
 
     protected final SortedMap val;
     protected String[] vars;
 
     protected TermOrder order; 
 
     /**
      * Constructors for OrderedMapPolynomial
      */
     public OrderedMapPolynomial() { 
 	this( new TermOrder() );
     }
 
     public OrderedMapPolynomial(int r) { 
         this();
         vars = ExpVector.STDVARS(r);
     }
 
     public OrderedMapPolynomial(Map t) { 
 	this();
         val.putAll(t);
     }
 
     public OrderedMapPolynomial(String[] v) { 
         this();
         vars = v;
     }
 
     public OrderedMapPolynomial(Coefficient a, ExpVector e) { 
         this( e.length() );
         val.put( e, a );
     }
 
     public OrderedMapPolynomial(TermOrder to) { 
         vars = null;
 	order = to;
         val = new TreeMap( order.getDescendComparator() );
     }
 
     public OrderedMapPolynomial(TermOrder to, Map t) { 
 	this(to);
         val.putAll(t);
     }
 
     public OrderedMapPolynomial(OrderedPolynomial o) { 
 	this( o.getTermOrder() );
         val.putAll( o.getMap() );
     }
 
     public OrderedMapPolynomial(UnorderedPolynomial u) { 
 	this();
         val.putAll( u.getMap() );
     }
 
     public OrderedMapPolynomial(TermOrder to, UnorderedPolynomial u) { 
 	this(to);
         val.putAll( u.getMap() );
     }
 
     public OrderedMapPolynomial(TermOrder to, OrderedPolynomial o) { 
 	this(to);
         val.putAll( o.getMap() );
     }
 
 
     /**
      * Methods of OrderedMapPolynomial
      */
 
     public int length() { 
         return val.size(); 
     }
 
     public Map getMap() { 
         return val; 
     }
 
     public String[] getVars() { 
         return vars; 
     }
 
     public String[] setVars(String[] v) {
        String[] t = vars;
        vars = v;
        return t; 
     }
 
     public TermOrder getTermOrder() {
        return order;
     };
 
 
     abstract public Object clone();
     // { return new OrderedMapPolynomial(val,evord,vars); }
 
     abstract public OrderedPolynomial getZERO();
     // { return new OrderedMapPolynomial(vars); }
 
     abstract public OrderedPolynomial getZERO(TermOrder t);
     // { return new OrderedMapPolynomial(vars,t); }
 
     abstract public OrderedPolynomial getONE();
     // { return new OrderedMapPolynomial(vars); }
 
     abstract public OrderedPolynomial getONE(TermOrder t);
     // { return new OrderedMapPolynomial(vars,t); }
 
     public String toString() { 
         StringBuffer erg = new StringBuffer();
         Set ent = val.entrySet();
         Iterator it = ent.iterator();
         while ( it.hasNext() ) {
             Map.Entry y = (Map.Entry) it.next();
             ExpVector f = (ExpVector) y.getKey(); 
             Object a = y.getValue();
             erg.append(a + " " + f);  
             if ( it.hasNext() ) {
                 erg.append(" ");
             } 
         }
         return erg.toString(); 
     }
 
     abstract public String toString(String[] vars); 
 
 
     public boolean equals( Object B ) { 
        if ( ! ( B instanceof OrderedPolynomial) ) return false;
        boolean t = (this.subtract( (OrderedPolynomial) B )).isZERO();
        // boolean t = val.equals( b.getMap() ); wrong impl in jdk1.4.0
        //      && vars.equals( b.getVars() );
        return t;
     }
 
 
     /**
      * Number of variables.
      */
 
     public int numberOfVariables() {  
         if ( vars != null ) return vars.length;
         if ( val.size() == 0 ) { 
            return 0;
         }
         Object e = ((Map.Entry)val.entrySet().iterator().next()).getKey();
         return ((ExpVector)e).length();
     }
 
     public static int DIPNOV(OrderedPolynomial a) {  
         if ( a == null ) return 0;
         return a.numberOfVariables();
     }
 
 
     /**
      * Leading monomial.
      */
 
     public Map.Entry leadingMonomial() {
 	if ( val.size() == 0 ) return null;
         Iterator ai = val.entrySet().iterator();
         return (Map.Entry) ai.next();
     }
 
     public static Map.Entry DIPLM(OrderedPolynomial a) {  
 	if ( a == null ) return null;
         return a.leadingMonomial();
     }
 
 
     /**
      * Leading exponent vector.
      */
 
     public ExpVector leadingExpVector() {
         return (ExpVector) val.firstKey(); 
     }
 
    public static ExpVector DIPLEV(OrderedPolynomial a) {  
 	if ( a == null ) return null;
         return a.leadingExpVector();
     }
 
 
     /**
      * Leading base coefficient.
      */
 
     public Coefficient leadingBaseCoefficient() {
         Map.Entry m = this.leadingMonomial();
 	if ( m == null ) return null;
         return (Coefficient) m.getValue();
     }
 
    public static Object DIPLBC(OrderedPolynomial a) {  
 	if ( a == null ) return null;
         return a.leadingBaseCoefficient();
     }
 
 
     /**
      * Sum. Implemantation based on TreeMap / SortedMap.
      */
 
     public OrderedPolynomial add(OrderedPolynomial B) {  
 	if ( B == null ) return this;
         if ( ! this.order.equals( B.getTermOrder() ) ) { 
            logger.error("term orderings not equal"); 
         }
 	if ( this.length() == 0 ) return B;
 	if ( B.length() == 0 ) return this;
 	OrderedPolynomial Cp = (OrderedPolynomial) this.clone();
         Map C = Cp.getMap();
         Map Bt = B.getMap();
         Iterator bi = Bt.entrySet().iterator();
         while ( bi.hasNext() ) {
             Map.Entry y = (Map.Entry) bi.next();
 	    ExpVector f = (ExpVector) y.getKey(); 
 	    // System.out.println("f = " + f);
             Coefficient b = (Coefficient) y.getValue(); 
 	    // System.out.println("b = " + b);
             Coefficient c = (Coefficient) C.remove( f );
             // System.out.println("c = " + c);
             if ( c != null ) { 
                c = c.add(b);
                // System.out.println("c = " + c);
                if ( ! c.isZERO() ) { 
                   C.put( f, c ); 
                }
             } else { 
 	       // System.out.println("b = " + b);
                C.put( f, b ); 
             }
         }
         return Cp;
     }
 
     public static OrderedPolynomial DIPSUM(OrderedPolynomial a, 
                                            OrderedPolynomial b) {  
 	if ( a == null ) return b;
         return a.add(b);
     }
 
 
     /**
      * Difference. Implementation based on TreeMap / SortedMap.
      */
 
     public OrderedPolynomial subtract(OrderedPolynomial B) {  
 	if ( B == null ) return this;
         if ( ! this.order.equals( B.getTermOrder() ) ) { 
            logger.error("term orderings not equal"); 
         }
 	if ( this.length() == 0 ) return B.negate();
 	if ( B.length() == 0 ) return this;
 	OrderedPolynomial Cp = (OrderedPolynomial) this.clone();
         Map C = Cp.getMap();
         Map Bt = B.getMap();
         Iterator bi = Bt.entrySet().iterator();
         while ( bi.hasNext() ) {
             Map.Entry y = (Map.Entry) bi.next();
 	    ExpVector f = (ExpVector) y.getKey(); 
             //System.out.println("f = " + f);
             Coefficient b = (Coefficient) y.getValue(); 
             //System.out.println("b = " + b);
             Coefficient c = (Coefficient) C.remove( (Object) f );
             //System.out.println("c = " + c);
             if ( c != null ) { 
                c = c.subtract(b);
                //System.out.println("c = " + c);
                if (  ! c.isZERO() ) { C.put( f, c ); }
             } else { 
                C.put( f, b.negate() ); 
             }
         }
         return Cp;
     }
 
 
     public static OrderedPolynomial DIPDIF(OrderedPolynomial a, 
                                            OrderedPolynomial b) {  
         if ( a == null ) return ( b == null ? null : b.negate() );
         return a.subtract(b);
     }
 
 
     /**
      * Multiply. Implementation using polynomial add.
      */
      public OrderedPolynomial multiplyA(OrderedPolynomial Bp) {  
 	if ( Bp == null ) return getZERO();
         if ( ! this.order.equals( Bp.getTermOrder() ) ) { 
            logger.error("term orderings not equal"); 
         }
         OrderedPolynomial Cp = getZERO(); 
         Cp.setVars(vars);
         Map A = this.val; 
         Iterator ai = A.entrySet().iterator();
         while ( ai.hasNext() ) {
             Map.Entry y = (Map.Entry) ai.next();
             ExpVector e = (ExpVector) y.getKey(); 
             //System.out.println("e = " + e);
             Coefficient a = (Coefficient) y.getValue(); 
             //System.out.println("a = " + a);
             OrderedPolynomial Dp = Bp.multiply( a, e );
             //System.out.println("Dp = " + Dp.length());
             Cp = Cp.add(Dp);
             //System.out.println("Cp = " + Cp.length());
         }
         //System.out.println("Cp = " + Cp);
         return Cp;
     }
 
     /**
      * Multiply. Implementation using map.put on result polynomial.
      */
 
      public OrderedPolynomial multiply(OrderedPolynomial Bp) {  
 	if ( Bp == null ) return getZERO();
         if ( ! this.order.equals( Bp.getTermOrder() ) ) { 
            logger.error("term orderings not equal"); 
         }
         OrderedPolynomial Cp = getZERO(order); 
 	Cp.setVars( vars ); //Cp.setord( evord );
         Map C = Cp.getMap();
         Map A = val; //this.getMap();
         Iterator ai = A.entrySet().iterator();
         Map B = Bp.getMap();
         Set Bk = B.entrySet();
         while ( ai.hasNext() ) {
             Map.Entry y = (Map.Entry) ai.next();
 	    ExpVector e = (ExpVector) y.getKey(); 
             //System.out.println("e = " + e);
             Coefficient a = (Coefficient) y.getValue(); 
             //System.out.println("a = " + a);
             Iterator bi = Bk.iterator();
             while ( bi.hasNext() ) {
                 Map.Entry x = (Map.Entry) bi.next();
 	        ExpVector f = (ExpVector) x.getKey(); 
                 //System.out.println("f = " + f);
                 Coefficient b = (Coefficient) x.getValue(); 
                 //System.out.println("b = " + b);
 	        ExpVector g = ExpVector.EVSUM(e,f); 
                 //System.out.println("g = " + g);
                 Coefficient c = a.multiply(b);
                 //System.out.println("c = " + c);
                 Coefficient d = (Coefficient) C.get(g);
                 //System.out.println("d = " + d);
 		if ( d == null ) { 
                      d = c; 
 		} else { 
                      d = d.add(c); 
                 }
                 //System.out.println("d+c = " + d);
                 C.put( g, d );
             }
         }
         return Cp;
     }
 
     public static OrderedPolynomial DIPPR(OrderedPolynomial a, 
                                           OrderedPolynomial b) {  
         if ( a == null ) return null;
         if ( a.length() <= b.length() ) {
              return a.multiply(b);
         } else { 
              return b.multiply(a);
         }
     }
 
 
     /**
      * Product with number.
      */
 
     public OrderedPolynomial multiply(Coefficient b) {  
 
         OrderedPolynomial Cp = getZERO(order); 
         Cp.setVars(vars);
         Map C = Cp.getMap();
         Map A = val; //this.getMap();
         Iterator ai = A.entrySet().iterator();
         while ( ai.hasNext() ) {
             Map.Entry y = (Map.Entry) ai.next();
             ExpVector e = (ExpVector) y.getKey(); 
             //System.out.println("e = " + e);
             Coefficient a = (Coefficient) y.getValue(); 
             //System.out.println("a = " + a);
             Coefficient c = a.multiply(b);
             //System.out.println("c = " + c);
             C.put( e, c );
         }
         return Cp;
     }
 
     public static OrderedPolynomial DIPRP(OrderedPolynomial a, 
                                             Coefficient b) {  
         if ( a == null ) return null;
         return a.multiply(b);
     }
 
 
     /**
      * Product with number and exponent vector.
      */
 
     public OrderedPolynomial multiply(Coefficient b, ExpVector e) {  
         OrderedPolynomial Cp = getZERO(order); 
         Cp.setVars(vars);
         Map C = Cp.getMap();
         Map A = val; //this.getMap();
         Iterator ai = A.entrySet().iterator();
         while ( ai.hasNext() ) {
             Map.Entry y = (Map.Entry) ai.next();
             ExpVector f = (ExpVector) y.getKey(); 
             //System.out.println("e = " + e);
             Coefficient a = (Coefficient) y.getValue(); 
             //System.out.println("a = " + a);
             Coefficient c = a.multiply(b);
             //System.out.println("c = " + c);
             ExpVector g = f.sum(e);
             //System.out.println("g = " + g);
             C.put( g, c );
         }
         return Cp;
     }
 
     public static OrderedPolynomial DIPRP(OrderedPolynomial a, 
                                           Coefficient b, 
                                           ExpVector e) {
         if ( a == null ) return null;
         return a.multiply(b,e);
     }
 
 
     /**
      * Product with 'monomial'.
      */
 
     public OrderedPolynomial multiply(Map.Entry m) {  
         if ( m == null ) return null;
         return multiply( (Coefficient)m.getValue(), (ExpVector)m.getKey() );
     }
 
     public static OrderedPolynomial DIPRP(OrderedPolynomial a, 
                                           Map.Entry m) {  
         if ( a == null ) return null;
         return a.multiply(m);
     }
 
 
     /**
      * Negation.
      */
 
     public OrderedPolynomial negate() {  
         OrderedPolynomial Cp = getZERO(order); 
         Cp.setVars(vars);
         Map C = Cp.getMap();
         Map A = this.val; 
         Iterator ai = A.entrySet().iterator();
         while ( ai.hasNext() ) {
             Map.Entry y = (Map.Entry) ai.next();
             ExpVector e = (ExpVector) y.getKey(); 
             //System.out.println("e = " + e);
             Coefficient a = (Coefficient) y.getValue(); 
             //System.out.println("a = " + a);
             Coefficient c = a.negate();
             //System.out.println("c = " + c);
             C.put( e, c );
         }
         return Cp;
     }
 
     public static OrderedPolynomial DIPNEG(OrderedPolynomial a) {  
         if ( a == null ) return null;
         return a.negate();
     }
 
 
     /**
      * Zero ?. Is this polynomial in the aequivalence class of zero?
      */
 
     public boolean isZERO() {  
         boolean t = false;
         if ( val.size() == 0 ) return true;
         if ( val.size() != 1 ) return t;
         Map.Entry y = (Map.Entry) val.entrySet().iterator().next();
         ExpVector e = (ExpVector) y.getKey(); 
         //System.out.println("e = " + e);
         if ( ExpVector.EVTDEG(e) != 0 ) return t;
         Coefficient a = (Coefficient) y.getValue(); 
         //System.out.println("a = " + a);
         if ( ! a.isZERO() ) return t;
         return true;
     }
 
     public static boolean DIPZERO(OrderedPolynomial a) {  
         if ( a == null ) return false;
         return a.isZERO();
     }
 
 
     /**
      * One ?. Is this polynomial in the aequivalence class of one?
      */
 
     public boolean isONE() {  
         boolean t = false;
         if ( val.size() != 1 ) return t;
         Map.Entry y = (Map.Entry) val.entrySet().iterator().next();
         ExpVector e = (ExpVector) y.getKey(); 
         //        System.out.println("e = " + e);
         if ( ExpVector.EVTDEG(e) != 0 ) return t;
         Coefficient a = (Coefficient) y.getValue(); 
         //System.out.println("a = " + a);
         if ( ! a.isONE() ) return t;
         return true;
     }
 
     public static boolean DIPONE(OrderedPolynomial a) {  
         if ( a == null ) return false;
         return a.isONE();
     }
 
 
 }
