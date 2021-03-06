 /**
  *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
  *  All rights reserved.
  *  Redistribution and use in source and binary forms, with or without
  *  modification, are permitted provided that the following conditions are met:
  *
  *      * Redistributions of source code must retain the above copyright
  *        notice, this list of conditions and the following disclaimer.
  *      * Redistributions in binary form must reproduce the above copyright
  *        notice, this list of conditions and the following disclaimer in the
  *        documentation and/or other materials provided with the distribution.
  *      * Neither the name of the Ecole des Mines de Nantes nor the
  *        names of its contributors may be used to endorse or promote products
  *        derived from this software without specific prior written permission.
  *
  *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
  *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
  *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package solver.constraints;
 
 import gnu.trove.list.array.TIntArrayList;
 import gnu.trove.set.TIntSet;
 import gnu.trove.set.hash.TIntHashSet;
 import solver.Solver;
 import solver.constraints.binary.*;
 import solver.constraints.extension.binary.BinCSP;
 import solver.constraints.extension.binary.BinRelation;
 import solver.constraints.extension.nary.LargeCSP;
 import solver.constraints.extension.nary.LargeRelation;
 import solver.constraints.nary.PropDiffN;
 import solver.constraints.nary.PropKnapsack;
 import solver.constraints.nary.alldifferent.AllDifferent;
 import solver.constraints.nary.alldifferent.conditions.Condition;
 import solver.constraints.nary.alldifferent.conditions.ConditionnalAllDifferent;
 import solver.constraints.nary.among.Among;
 import solver.constraints.nary.automata.CostRegular;
 import solver.constraints.nary.automata.FA.IAutomaton;
 import solver.constraints.nary.automata.FA.ICostAutomaton;
 import solver.constraints.nary.automata.MultiCostRegular;
 import solver.constraints.nary.automata.Regular;
 import solver.constraints.nary.channeling.DomainChanneling;
 import solver.constraints.nary.channeling.InverseChanneling;
 import solver.constraints.nary.channeling.PropEnumDomainChanneling;
 import solver.constraints.nary.circuit.*;
 import solver.constraints.nary.count.Count;
 import solver.constraints.nary.cumulative.PropIncrementalCumulative;
 import solver.constraints.nary.element.Element;
 import solver.constraints.nary.globalcardinality.GlobalCardinality;
 import solver.constraints.nary.lex.Lex;
 import solver.constraints.nary.lex.LexChain;
 import solver.constraints.nary.min_max.MaxOfAList;
 import solver.constraints.nary.min_max.MinOfAList;
 import solver.constraints.nary.nValue.Differences;
 import solver.constraints.nary.nValue.NValues;
 import solver.constraints.nary.sum.*;
 import solver.constraints.nary.tree.PropAntiArborescences;
 import solver.constraints.nary.tree.PropKLoops;
 import solver.constraints.ternary.*;
 import solver.constraints.unary.Member;
 import solver.constraints.unary.NotMember;
 import solver.variables.*;
 import util.tools.ArrayUtils;
 import util.tools.StringUtils;
 
 /**
  * A Factory to declare constraint based on integer variables (only).
  * One can call directly the constructor of constraints, but it is recommended
  * to use the Factory, because signatures and javadoc are ensured to be up-to-date.
  * <br/>
  * As much as possible, the API names of global constraints must match
  * those define in the <a href="http://www.emn.fr/z-info/sdemasse/gccat/index.html">Global Constraint Catalog</a>.
  * <p/>
  * Note that, for the sack of readability, the Java naming convention is not respected for methods arguments.
  * <p/>
  * Constraints are ordered as the following:
  * 1) Unary	constraints
  * 2) Binary constraints
  * 3) Terary constraints
  * 4) Global constraints
  *
  * @author Charles Prud'homme
  * @since 21/01/13
  */
 public class IntConstraintFactory {
     IntConstraintFactory() {
     }
 
     // BEWARE: PLEASE, keep signatures sorted in alphabetical order!!
 
     //##################################################################################################################
     // ZEROARIES #########################################################################################################
     //##################################################################################################################
 
     /**
      * Ensures the TRUE constraint
      *
      * @param solver a solver
      * @return a true constraint
      */
     public static Constraint TRUE(Solver solver) {
         return solver.TRUE;
     }
 
     /**
      * Ensures the FALSE constraint
      *
      * @param solver a solver
      * @return a false constraint
      */
     public static Constraint FALSE(Solver solver) {
         return solver.FALSE;
     }
 
     //##################################################################################################################
     // UNARIES #########################################################################################################
     //##################################################################################################################
 
     /**
      * Ensures: VAR OP CSTE, where OP in {"=", "!=", ">","<",">=","<="}
      *
      * @param VAR  a variable
      * @param OP   an operator
      * @param CSTE a constant
      */
     public static Arithmetic arithm(IntVar VAR, String OP, int CSTE) {
         Operator op = Operator.get(OP);
         return new Arithmetic(VAR, op, CSTE, VAR.getSolver());
     }
 
     /**
      * Ensures VAR takes its values in TABLE
      *
      * @param VAR   an integer variable
      * @param TABLE an array of values
      */
     public static Member member(IntVar VAR, int[] TABLE) {
         return new Member(VAR, TABLE, VAR.getSolver());
     }
 
     /**
      * Ensures VAR takes its values in [LB, UB]
      *
      * @param VAR an integer variable
      * @param LB  the lower bound of the interval
      * @param UB  the upper bound of the interval
      */
     public static Member member(IntVar VAR, int LB, int UB) {
         return new Member(VAR, LB, UB, VAR.getSolver());
     }
 
     /**
      * Ensures VAR does not take its values in TABLE
      *
      * @param VAR   an integer variable
      * @param TABLE an array of values
      */
     public static NotMember not_member(IntVar VAR, int[] TABLE) {
         return new NotMember(VAR, TABLE, VAR.getSolver());
     }
 
     /**
      * Ensures VAR does not take its values in [LB, UB]
      *
      * @param VAR an integer variable
      * @param LB  the lower bound of the interval
      * @param UB  the upper bound of the interval
      */
     public static NotMember not_member(IntVar VAR, int LB, int UB) {
         return new NotMember(VAR, LB, UB, VAR.getSolver());
     }
 
     //##################################################################################################################
     //BINARIES #########################################################################################################
     //##################################################################################################################
 
     /**
      * Enforces VAR1 = |VAR2|
      */
     public static Absolute absolute(IntVar VAR1, IntVar VAR2) {
         assert VAR1.getSolver() == VAR2.getSolver();
         return new Absolute(VAR1, VAR2, VAR1.getSolver());
     }
 
     /**
      * Ensures: VAR1 OP VAR2, where OP in {"=", "!=", ">","<",">=","<="}
      *
      * @param VAR1 first variable
      * @param OP   an operator
      * @param VAR2 second variable
      */
     public static Arithmetic arithm(IntVar VAR1, String OP, IntVar VAR2) {
         Operator op = Operator.get(OP);
         return new Arithmetic(VAR1, op, VAR2, VAR1.getSolver());
     }
 
     /**
      * Ensures: VAR1 OP VAR2, where OP in {"=", "!=", ">","<",">=","<="}
      *
      * @param VAR1 first variable
      * @param OP1  an operator
      * @param VAR2 second variable
      * @param OP2  another operator
      * @param CSTE an operator
      */
     public static Arithmetic arithm(IntVar VAR1, String OP1, IntVar VAR2, String OP2, int CSTE) {
         Operator op1 = Operator.get(OP1);
         Operator op2 = Operator.get(OP2);
         return new Arithmetic(VAR1, op1, VAR2, op2, CSTE, VAR1.getSolver());
     }
 
     /**
      * Ensures: <br/>
      * |VAR1-VAR2| OP CSTE
      * <br/>
      * where OP can take its value among {"=", ">", "<", "!="}
      */
     public static DistanceXYC distance(IntVar VAR1, IntVar VAR2, String OP, int CSTE) {
         assert VAR1.getSolver() == VAR2.getSolver();
         Operator op = Operator.get(OP);
         return new DistanceXYC(VAR1, VAR2, op, CSTE, VAR1.getSolver());
     }
 
     /**
      * Build ELEMENT constraint: VALUE = TABLE[INDEX-OFFSET]
      *
      * @param VALUE  an integer variable taking its value in TABLE
      * @param TABLE  an array of integer values
      * @param INDEX  an integer variable representing the value of VALUE in TABLE
      * @param OFFSET offset matching INDEX.LB and TABLE[0] (Generally 0)
      * @param SORT   defines ordering properties of TABLE:
      *               <p/> "none" if TABLE is not sorted
      *               <p/> "asc" if TABLE is sorted in the increasing order
      *               <p/> "desc" if TABLE is sorted in the decreasing order
      *               <p/> "detect" Let the constraint detect the ordering of TABLE, if any
      */
     public static Element element(IntVar VALUE, int[] TABLE, IntVar INDEX, int OFFSET, String SORT) {
         return new Element(VALUE, TABLE, INDEX, OFFSET, SORT, VALUE.getSolver());
     }
 
 	/**
 	 * Build ELEMENT constraint: VALUE = TABLE[INDEX]
 	 *
 	 * @param VALUE  an integer variable taking its value in TABLE
 	 * @param TABLE  an array of integer values
 	 * @param INDEX  an integer variable representing the value of VALUE in TABLE
 	 */
 	public static Element element(IntVar VALUE, int[] TABLE, IntVar INDEX) {
 		return element(VALUE, TABLE, INDEX, 0, "detect");
 	}
 
     /**
      * Enforces VAR1 = VAR2^2
      */
     public static Square square(IntVar VAR1, IntVar VAR2) {
         assert VAR1.getSolver() == VAR2.getSolver();
         return new Square(VAR1, VAR2, VAR1.getSolver());
     }
 
     /**
      * Create a table constraint over a couple of variables VAR1 and VAR2, .
      * <p/>
      * The <code>ALGORITHM</code> should be chosen among {"AC2201"}.
      * <p/>
      * <b>AC2001</b>: Arc Consistency version 2001.
      *
      * @param VAR1      first variable
      * @param VAR2      second variable
      * @param RELATION  the relation between the two variables
      * @param ALGORITHM to choose among {"AC2001"}
      */
     public static BinCSP table(IntVar VAR1, IntVar VAR2, BinRelation RELATION, String ALGORITHM) {
         return new BinCSP(VAR1, VAR2, RELATION, BinCSP.Algorithm.valueOf(ALGORITHM));
     }
 
     //##################################################################################################################
     //TERNARIES ########################################################################################################
     //##################################################################################################################
 
     /**
      * Ensures: <br/>
      * |VAR1-VAR2| OP CSTE
      * <br/>
      * where OP can take its value among {"=", ">", "<"}
      *
      * @param VAR1 first variable
      * @param VAR2 second variable
      * @param OP   an operator
      * @param VAR3 resulting variable
      */
     public static DistanceXYZ distance(IntVar VAR1, IntVar VAR2, String OP, IntVar VAR3) {
         Operator op = Operator.get(OP);
         return new DistanceXYZ(VAR1, VAR2, op, VAR3, VAR1.getSolver());
 
     }
 
     /**
      * Ensures DIVIDEND / DIVISOR = RESULT, rounding towards 0 -- Euclidean division
      *
      * @param DIVIDEND dividend
      * @param DIVISOR  divisor
      * @param RESULT   result
      */
     public static DivXYZ eucl_div(IntVar DIVIDEND, IntVar DIVISOR, IntVar RESULT) {
         return new DivXYZ(DIVIDEND, DIVISOR, RESULT, DIVIDEND.getSolver());
     }
 
     /**
      * Ensures: MAX = MAX(VAR1, VAR2)
      * (Bound Consistency)
      *
      * @param MAX  a variable
      * @param VAR1 a variable
      * @param VAR2 a variable
      */
     public static Max maximum(IntVar MAX, IntVar VAR1, IntVar VAR2) {
         return new Max(MAX, VAR1, VAR2, MAX.getSolver());
     }
 
     /**
      * Ensures:  VAR1 = MIN(VAR2, VAR3)
      * (Bound Consistency)
      *
      * @param MIN  result
      * @param VAR1 result
      * @param VAR2 first variable
      */
     public static Min minimum(IntVar MIN, IntVar VAR1, IntVar VAR2) {
         return new Min(MIN, VAR1, VAR2, MIN.getSolver());
     }
 
     /**
      * Ensures VAR1 % VAR2 = VAR 3,
      * <br/>i.e.:<br/>
      * - VAR1 / VAR2 = T1 and,<br/>
      * - T1 * VAR2 = T2 and,<br/>
      * - Z + T2 = VAR1<br/>
      * <br/>
      * where T1 = T2 = [-|VAR1|, |VAR1|]
      *
      * @param VAR1 first variable
      * @param VAR2 second variable
      * @param VAR3 result
      */
     public static ModXYZ mod(IntVar VAR1, IntVar VAR2, IntVar VAR3) {
         return new ModXYZ(VAR1, VAR2, VAR3, VAR1.getSolver());
     }
 
     /**
      * Ensures: VAR1 * VAR2 = VAR3
      *
      * @param VAR1 first variable
      * @param VAR2 second variable
      * @param VAR3 result
      */
     public static Times times(IntVar VAR1, IntVar VAR2, IntVar VAR3) {
         return new Times(VAR1, VAR2, VAR3, VAR1.getSolver());
     }
 
     //##################################################################################################################
     //GLOBALS ##########################################################################################################
     //##################################################################################################################
 
     /**
      * Ensures that all variables from VARS take a different value.
      * The consistency level should be chosen among "BC", "AC" and "DEFAULT".
      * <p/>
      * <b>BC</b>:
      * <br/>
      * Based on: "A Fast and Simple Algorithm for Bounds Consistency of the AllDifferent Constraint"</br>
      * A. Lopez-Ortiz, CG. Quimper, J. Tromp, P.van Beek
      * <p/>
      * <b>AC</b>:
      * <br/>
      * Uses Regin algorithm
      * Runs in O(m.n) worst case time for the initial propagation and then in O(n+m) time
      * per arc removed from the support.
      * <p/>
      * <b>DEFAULT</b>:
      * <br/>
      * Uses BC plus a probabilistic AC propagator to get a compromise between BC and AC
      *
      * @param VARS        list of variables
      * @param CONSISTENCY consistency level, among {"BC", "AC"}
      *                    <p/>
      *                    <b>BC</b>:
      *                    Based on: "A Fast and Simple Algorithm for Bounds Consistency of the AllDifferent Constraint"</br>
      *                    A. Lopez-Ortiz, CG. Quimper, J. Tromp, P.van Beek
      *                    <br/>
      *                    <b>AC</b>:
      *                    Uses Regin algorithm
      *                    Runs in O(m.n) worst case time for the initial propagation and then in O(n+m) time
      *                    per arc removed from the support.
      *                    <p/>
      *                    <b>DEFAULT</b>:
      *                    <br/>
      *                    Uses BC plus a probabilistic AC propagator to get a compromise between BC and AC
      */
     public static AllDifferent alldifferent(IntVar[] VARS, String CONSISTENCY) {
         return new AllDifferent(VARS, VARS[0].getSolver(), AllDifferent.Type.valueOf(CONSISTENCY));
     }
 
 	/**
 	 * Alldifferent holds on the subset of VARS which satisfies the given CONDITION
 	 * @param VARS		collection of variables
 	 * @param CONDITION	condition defining which variables should be constrained
 	 */
 	public static Constraint alldifferent_conditionnal(IntVar[] VARS, Condition CONDITION) {
 		return new ConditionnalAllDifferent(VARS,CONDITION,false);
 	}
 
 	/**
 	 * Variables in VARS must either be different or equal to 0
 	 * @param VARS    			collection of variables
 	 */
 	public static Constraint alldifferent_except_0(IntVar[] VARS) {
 		return alldifferent_conditionnal(VARS,Condition.EXCEPT_0);
 	}
 
     /**
      * NVAR is the number of variables of the collection VARIABLES that take their value in VALUES.
      *
      * @param NVAR   a variable
      * @param VARS   vector of variables
      * @param VALUES set of values
      */
     public static Among among(IntVar NVAR, IntVar[] VARS, int[] VALUES) {
         return new Among(NVAR, VARS, VALUES, NVAR.getSolver());
     }
 
 	/**
 	 * Bin Packing formulation:
 	 * forall b in [0,BIN_LOAD.length-1],
 	 * BIN_LOAD[b]=sum(ITEM_SIZE[i] | i in [0,ITEM_SIZE.length-1], ITEM_BIN[i] = b+OFFSET
 	 * forall i in [0,ITEM_SIZE.length-1], ITEM_BIN is in [OFFSET,BIN_LOAD.length-1+OFFSET],
 	 *
 	 * @param ITEM_BIN IntVar representing the bin of each item
 	 * @param ITEM_SIZE int representing the size of each item
 	 * @param BIN_LOAD IntVar representing the load of each bin (i.e. the sum of the size of the items in it)
 	 * @param OFFSET 0 by default but typically 1 if used within MiniZinc
 	 *               (which counts from 1 to n instead of from 0 to n-1)
 	 * @return
 	 */
 	public static Constraint[] bin_packing(IntVar[] ITEM_BIN, int[] ITEM_SIZE, IntVar[] BIN_LOAD, int OFFSET){
 		int nbBins = BIN_LOAD.length;
 		int nbItems= ITEM_BIN.length;
 		Solver s = ITEM_BIN[0].getSolver();
 		BoolVar[][] xbi = VF.boolMatrix("xbi",nbBins,nbItems,s);
 		int sum = 0;
 		for(int is:ITEM_SIZE){
 			sum += is;
 		}
 		IntVar sumView = VF.fixed(sum,s);
 		// constraints
 		Constraint[] bpcons = new Constraint[nbItems+nbBins+1];
 		for(int i=0;i<nbItems;i++){
 			bpcons[i] = ICF.boolean_channeling(ArrayUtils.getColumn(xbi,i),ITEM_BIN[i],OFFSET);
 		}
 		for(int b=0;b<nbBins;b++){
 			bpcons[nbItems+b] = ICF.scalar(xbi[b],ITEM_SIZE,BIN_LOAD[b]);
 		}
 		bpcons[nbItems+nbBins] = ICF.sum(BIN_LOAD,sumView);
 		return bpcons;
 	}
 
     /**
      * Maps the boolean assignments variables BVARS with the standard assignment variable VAR.
      * VAR = i <-> BVARS[i-OFFSET] = 1
      *
      * @param BVARS  array of boolean variables
      * @param VAR    observed variable. Should presumably have an enumerated domain
      * @param OFFSET 0 by default but typically 1 if used within MiniZinc
 	 *               (which counts from 1 to n instead of from 0 to n-1)
      */
     public static Constraint boolean_channeling(BoolVar[] BVARS, IntVar VAR, int OFFSET) {
         if (!VAR.hasEnumeratedDomain()) {
             IntVar enumV = VF.enumerated(VAR.getName() + "_enumImage", VAR.getLB(), VAR.getUB(), VAR.getSolver());
             Constraint cons = new Constraint(ArrayUtils.append(BVARS, new IntVar[]{VAR, enumV}), VAR.getSolver());
             cons.setPropagators(new PropEnumDomainChanneling(BVARS, enumV, OFFSET), new PropEqualX_Y(VAR, enumV));
             return cons;
         }
         return new DomainChanneling(BVARS, VAR, OFFSET, VAR.getSolver());
     }
 
     /**
      * Creates a circuit constraint which ensures that
      * <p/> the elements of vars define a covering circuit
      * <p/> where VARS[i] = OFFSET+j means that j is the successor of i.
      * <p/>
      * Filtering algorithms:
      * <p/> subtour elimination : Caseau & Laburthe (ICLP'97)
      * <p/> allDifferent GAC algorithm: R&eacute;gin (AAAI'94)
      * <p/> dominator-based filtering: Fages & Lorca (CP'11)
      * <p/> Strongly Connected Components based filtering (Cambazar & Bourreau JFPC'06 and Fages and Lorca TechReport'12)
      *
      * @param VARS   vector of variables which take their value in [OFFSET,OFFSET+|VARS|-1]
      * @param OFFSET 0 by default but typically 1 if used within MiniZinc
      *               (which counts from 1 to n instead of from 0 to n-1)
      * @return a circuit constraint
      */
     public static Constraint circuit(IntVar[] VARS, int OFFSET) {
         Constraint c = alldifferent(VARS, "AC");
         c.addPropagators(
                 new PropNoSubtour(VARS, OFFSET),
                 new PropCircuit_AntiArboFiltering(VARS, OFFSET),
                 new PropCircuitSCC(VARS, OFFSET));
         return c;
     }
 
     /**
      * Ensures that the assignment of a sequence of variables is recognized by CAUTOMATON, a deterministic finite automaton,
      * and that the sum of the costs associated to each assignment is bounded by the cost variable.
      * This version allows to specify different costs according to the automaton state at which the assignment occurs
      * (i.e. the transition starts)
      *
      * @param VARS       sequence of variables
      * @param COST       cost variable
      * @param CAUTOMATON a deterministic finite automaton defining the regular language and the costs
      *                   Can be built with method CostAutomaton.makeSingleResource(...)
      */
     public static CostRegular cost_regular(IntVar[] VARS, IntVar COST, ICostAutomaton CAUTOMATON) {
         return new CostRegular(VARS, COST, CAUTOMATON, VARS[0].getSolver());
     }
 
     /**
      * Let N be the number of variables of the VARIABLES collection assigned to value VALUE;
      * Enforce condition N = LIMIT to hold.
      * <p/>
      * Based on GlobalCardinality constraint, ensures GAC.
      *
      * @param VALUE an int
      * @param VARS  a vector of variables
      * @param LIMIT a variable
      */
     public static Count count(int VALUE, IntVar[] VARS, IntVar LIMIT) {
         return new Count(VALUE, VARS, LIMIT, VARS[0].getSolver());
     }
 
     /**
      * Cumulative constraint: Enforces that at each point in time,
      * the cumulated height of the set of tasks that overlap that point
      * does not exceed a given limit.
      *
      * @param TASKS    TASK objects containing start, duration and end variables
      * @param HEIGHTS  integer variables representing the resource consumption of each task
      * @param CAPACITY integer variable representing the resource capacity
      * @return a cumulative constraint
      */
     public static Constraint cumulative(Task[] TASKS, IntVar[] HEIGHTS, IntVar CAPACITY) {
         int n = TASKS.length;
         assert n > 0;
         Solver solver = TASKS[0].getStart().getSolver();
         IntVar[] starts = new IntVar[n];
         IntVar[] durations = new IntVar[n];
         IntVar[] ends = new IntVar[n];
         for (int i = 0; i < n; i++) {
             starts[i] = TASKS[i].getStart();
             durations[i] = TASKS[i].getDuration();
             ends[i] = TASKS[i].getEnd();
         }
         Constraint c = new Constraint(ArrayUtils.append(starts, durations, ends, HEIGHTS, new IntVar[]{CAPACITY}), solver);
         c.addPropagators(new PropIncrementalCumulative(starts, durations, ends, HEIGHTS, CAPACITY, true));
         c.addPropagators(new PropIncrementalCumulative(starts, durations, ends, HEIGHTS, CAPACITY, false));
 //		c.addPropagators(new PropTTDynamicSweep(ArrayUtils.append(starts,durations,ends,HEIGHTS),starts.length,1,new IntVar[]{CAPACITY}));
         return c;
     }
 
     /**
      * Constrains each rectangle<sub>i</sub>, given by their origins X<sub>i</sub>,Y<sub>i</sub>
      * and sizes WIDTH<sub>i</sub>,HEIGHT<sub>i</sub>, to be non-overlapping.
      *
      * @param X      collection of coordinates in first dimension
      * @param Y      collection of coordinates in second dimension
      * @param WIDTH  collection of width
      * @param HEIGHT collection of height
 	 * @param USE_CUMUL indicates whether or not redundant cumulative constraints should be put on each dimension (advised)
      * @return a non-overlapping constraint
      */
     public static Constraint[] diffn(IntVar[] X, IntVar[] Y, IntVar[] WIDTH, IntVar[] HEIGHT, boolean USE_CUMUL) {
         Solver solver = X[0].getSolver();
         Constraint diffNCons = new Constraint(ArrayUtils.append(X, Y, WIDTH, HEIGHT), solver);
         // (not idempotent, so requires two propagators)
         diffNCons.setPropagators(new PropDiffN(X, Y, WIDTH, HEIGHT, false), new PropDiffN(X, Y, WIDTH, HEIGHT, false));
 		if(USE_CUMUL){
 			IntVar[] EX = new IntVar[X.length];
 			IntVar[] EY = new IntVar[X.length];
 			Task[] TX = new Task[X.length];
 			Task[] TY = new Task[X.length];
 			int minx = Integer.MAX_VALUE/2;
 			int maxx = Integer.MIN_VALUE/2;
 			int miny = Integer.MAX_VALUE/2;
 			int maxy = Integer.MIN_VALUE/2;
 			for(int i=0;i<X.length;i++){
 				EX[i] = VF.bounded("",X[i].getLB()+WIDTH[i].getLB(),X[i].getUB()+WIDTH[i].getUB(),solver);
 				EY[i] = VF.bounded("",Y[i].getLB()+HEIGHT[i].getLB(),Y[i].getUB()+HEIGHT[i].getUB(),solver);
 				TX[i] = VF.task(X[i],WIDTH[i],EX[i]);
 				TY[i] = VF.task(Y[i],HEIGHT[i],EY[i]);
 				minx = Math.min(minx,X[i].getLB());
 				miny = Math.min(miny,Y[i].getLB());
 				maxx = Math.max(maxx,X[i].getUB()+WIDTH[i].getUB());
 				maxy = Math.max(maxy,Y[i].getUB()+HEIGHT[i].getUB());
 			}
 			IntVar maxX = VF.bounded("",minx, maxx,solver);
 			IntVar minX = VF.bounded("",minx,maxx,solver);
 			IntVar diffX = VF.bounded("",0,maxx-minx,solver);
 			IntVar maxY = VF.bounded("",miny, maxy,solver);
 			IntVar minY = VF.bounded("",miny,maxy,solver);
 			IntVar diffY = VF.bounded("",0,maxy-miny,solver);
 			return new Constraint[]{
 					diffNCons,
 					minimum(minX,X),maximum(maxX,EX),scalar(new IntVar[]{maxX,minX},new int[]{1,-1},diffX),
 					cumulative(TX,HEIGHT,diffY),
 					minimum(minY,Y),maximum(maxY,EY),scalar(new IntVar[]{maxY, minY}, new int[]{1, -1}, diffY),
 					cumulative(TY,WIDTH,diffX)
 			};
 		}
 		return new Constraint[]{diffNCons};
     }
 
     /**
      * Build an ELEMENT constraint: VALUE = TABLE[INDEX-OFFSET] where TABLE is an array of variables.
      *
      * @param VALUE  value variable
      * @param TABLE  array of variables
      * @param INDEX  index variable in range [OFFSET,OFFSET+|TABLE|-1]
      * @param OFFSET int offset, generally 0
      */
     public static Element element(IntVar VALUE, IntVar[] TABLE, IntVar INDEX, int OFFSET) {
         return new Element(VALUE, TABLE, INDEX, OFFSET, VALUE.getSolver());
     }
 
     /**
      * Global Cardinality constraint (GCC):
      * Each value VALUES[i] should be taken by exactly OCCURRENCES[i] variables of VARS.
      * <br/>
      * This constraint does not ensure any well-defined level of consistency, yet.
      *
      * @param VARS        collection of variables
      * @param VALUES      collection of constrained values
      * @param OCCURRENCES collection of cardinality variables
      * @param CLOSED      restricts domains of VARS to VALUES if set to true
      */
     public static GlobalCardinality global_cardinality(IntVar[] VARS, int[] VALUES, IntVar[] OCCURRENCES, boolean CLOSED) {
         Solver solver = VARS[0].getSolver();
         assert VALUES.length == OCCURRENCES.length;
         if (!CLOSED) {
             return new GlobalCardinality(VARS, VALUES, OCCURRENCES, solver);
         } else {
             TIntArrayList toAdd = new TIntArrayList();
             TIntSet givenValues = new TIntHashSet();
             for (int i : VALUES) {
                 assert !givenValues.contains(i);
                 givenValues.add(i);
             }
             for (IntVar var : VARS) {
                 int ub = var.getUB();
                 for (int k = var.getLB(); k <= ub; k = var.nextValue(k)) {
                     if (!givenValues.contains(k)) {
                         if (!toAdd.contains(k)) {
                             toAdd.add(k);
                         }
                     }
                 }
             }
             if (toAdd.size() > 0) {
                 int n2 = VALUES.length + toAdd.size();
                 int[] values = new int[n2];
                 IntVar[] cards = new IntVar[n2];
                 System.arraycopy(VALUES, 0, values, 0, VALUES.length);
                 System.arraycopy(OCCURRENCES, 0, cards, 0, VALUES.length);
                 for (int i = VALUES.length; i < n2; i++) {
                     values[i] = toAdd.get(i - VALUES.length);
                     cards[i] = VariableFactory.fixed(0, solver);
                 }
                 return new GlobalCardinality(VARS, values, cards, solver);
             } else {
                 return new GlobalCardinality(VARS, VALUES, OCCURRENCES, solver);
             }
         }
     }
 
     /**
      * Make an inverse channeling between VARS1 and VARS2:
      * VARS1[i-OFFSET2] = j <=> VARS2[j-OFFSET1] = i
      * Performs AC if domains are enumerated.
      * If not, then it works on bounds without guaranteeing BC
      * (enumerated domains are strongly recommended)
      * <p/>
      * Beware you should have |VARS1| = |VARS2|
      *
      * @param VARS1   vector of variables which take their value in [OFFSET1,OFFSET1+|VARS2|-1]
      * @param VARS2   vector of variables which take their value in [OFFSET2,OFFSET2+|VARS1|-1]
      * @param OFFSET1 lowest value in VARS1 (most often 0)
      * @param OFFSET2 lowest value in VARS2 (most often 0)
      */
     public static InverseChanneling inverse_channeling(IntVar[] VARS1, IntVar[] VARS2, int OFFSET1, int OFFSET2) {
         return new InverseChanneling(VARS1, VARS2, OFFSET1, OFFSET2, VARS1[0].getSolver());
     }
 
     /**
      * Ensures that :
      * <br/>- OCCURRENCES[i] * WEIGHT[i] &#8804; CAPA
      * <br/>- OCCURRENCES[i] * ENERGY[i] = POWER
      * <br/>and maximizing the value of POWER.
      * <p/>
      * <p/>
      * A knapsack constraint
      * <a href="http://en.wikipedia.org/wiki/Knapsack_problem">wikipedia</a>:<br/>
      * "Given a set of items, each with a weight and an energy value,
      * determine the count of each item to include in a collection so that
      * the total weight is less than or equal to a given limit and the total value is as large as possible.
      * It derives its name from the problem faced by someone who is constrained by a fixed-size knapsack
      * and must fill it with the most useful items."
      *
      * @param OCCURRENCES number of occurrences of an item
      * @param CAPA        capacity of the knapsack
      * @param POWER       variable to maximize
      * @param WEIGHT      weight of each item
      * @param ENERGY      energy of each item
      */
     public static Constraint knapsack(IntVar[] OCCURRENCES, IntVar CAPA, IntVar POWER,
                                       int[] WEIGHT, int[] ENERGY) {
         Constraint knapsack = new Constraint(CAPA.getSolver());
         knapsack.addPropagators(scalar(OCCURRENCES, WEIGHT, CAPA).propagators);
         knapsack.addPropagators(scalar(OCCURRENCES, ENERGY, POWER).propagators);
         knapsack.addPropagators(new PropKnapsack(OCCURRENCES, CAPA, POWER, WEIGHT, ENERGY));
         return knapsack;
     }
 
     /**
      * For each pair of consecutive vectors VARS<sub>i</sub> and VARS<sub>i+1</sub> of the VARS collection
      * VARS<sub>i</sub> is lexicographically strictly less than than VARS<sub>i+1</sub>
      *
      * @param VARS collection of vectors of variables
      */
     public static LexChain lex_chain_less(IntVar[]... VARS) {
         return new LexChain(true, VARS[0][0].getSolver(), VARS);
     }
 
     /**
      * For each pair of consecutive vectors VARS<sub>i</sub> and VARS<sub>i+1</sub> of the VARS collection
      * VARS<sub>i</sub> is lexicographically less or equal than than VARS<sub>i+1</sub>
      *
      * @param VARS collection of vectors of variables
      */
     public static LexChain lex_chain_less_eq(IntVar[]... VARS) {
         return new LexChain(false, VARS[0][0].getSolver(), VARS);
     }
 
     /**
      * Ensures that VARS1 is lexicographically strictly less than VARS2.
      *
      * @param VARS1 vector of variables
      * @param VARS2 vector of variables
      */
     public static Lex lex_less(IntVar[] VARS1, IntVar[] VARS2) {
         return new Lex(VARS1, VARS2, true, VARS1[0].getSolver());
     }
 
     /**
      * Ensures that VARS1 is lexicographically less or equal than VARS2.
      *
      * @param VARS1 vector of variables
      * @param VARS2 vector of variables
      */
     public static Lex lex_less_eq(IntVar[] VARS1, IntVar[] VARS2) {
         return new Lex(VARS1, VARS2, false, VARS1[0].getSolver());
     }
 
     /**
      * MAX is the maximum value of the collection of domain variables VARS
      *
      * @param MAX  a variable
      * @param VARS a vector of variables
      */
     public static MaxOfAList maximum(IntVar MAX, IntVar[] VARS) {
         return new MaxOfAList(MAX, VARS, MAX.getSolver());
     }
 
     /**
      * MIN is the minimum value of the collection of domain variables VARS
      *
      * @param MIN  a variable
      * @param VARS a vector of variables
      */
     public static MinOfAList minimum(IntVar MIN, IntVar[] VARS) {
         return new MinOfAList(MIN, VARS, MIN.getSolver());
     }
 
     /**
      * Ensures that the assignment of a sequence of VARS is recognized by AUTOMATON, a deterministic finite automaton,
      * and that the sum of the cost vector COSTS associated to each assignment is bounded by the variable vector CVARS.
      * This version allows to specify different costs according to the automaton state at which the assignment occurs
      * (i.e. the transition starts)
      *
      * @param VARS       sequence of variables
      * @param CVARS      cost variables
      * @param CAUTOMATON a deterministic finite automaton defining the regular language and the costs
      *                   Can be built from method CostAutomaton.makeMultiResources(...)
      */
     public static MultiCostRegular multicost_regular(IntVar[] VARS, IntVar[] CVARS, ICostAutomaton CAUTOMATON) {
         return new MultiCostRegular(VARS, CVARS, CAUTOMATON, VARS[0].getSolver());
     }
 
     /**
      * Let N be the number of distinct values assigned to the variables of the VARS collection.
      * Enforce condition N = NVALUES to hold.
      * <p/>
      * This embeds a light propagator by default.
      * Additional filtering algorithms can be added.
      *
      * @param VARS    collection of variables
      * @param NVALUES limit variable
      * @param ALGOS   additional filtering algorithms, among {"at_most_BC","at_least_AC","at_most_greedy"}
      */
     public static NValues nvalues(IntVar[] VARS, IntVar NVALUES, String... ALGOS) {
 
         NValues.Type[] types = new NValues.Type[ALGOS.length];
         for (int i = 0; i < ALGOS.length; i++) {
             types[i] = NValues.Type.valueOf(ALGOS[i]);
         }
 
         return new NValues(VARS, NVALUES, NVALUES.getSolver(), types);
     }
 
 	/**
 	 * Filters the conjunction of NValue and difference constraints
 	 * (propagator AMNV(Gci,RMD,R13) of Fages and Lapègue, CP'13)
 	 * Difference constraint should be propagated separately in addition
 	 * @param VARS		collection of variables
 	 * @param NVALUES	limit variable
 	 * @param DIFF		set of difference constraints
 	 * @return a NValue constraint
 	 */
 	public static NValues nvalues(IntVar[] VARS, IntVar NVALUES, Differences DIFF) {
 		return new NValues(VARS,NVALUES,DIFF);
 	}
 
     /**
      * Enforces the sequence of VARS to be a word
      * recognized by the deterministic finite automaton AUTOMATON.
      * For example regexp = "(1|2)(3*)(4|5)";
      * The same dfa can be used for different propagators.
      *
      * @param VARS      sequence of variables
      * @param AUTOMATON a deterministic finite automaton defining the regular language
      */
     public static Regular regular(IntVar[] VARS, IAutomaton AUTOMATON) {
         return new Regular(VARS, AUTOMATON, VARS[0].getSolver());
     }
 
     /**
      * Enforces that &#8721;<sub>i in |VARS|</sub>COEFFS<sub>i</sub> * VARS<sub>i</sub> = SCALAR.
      *
      * @param VARS   a vector of variables
      * @param COEFFS a vector of int
      * @param SCALAR a variable
      */
     public static Constraint scalar(IntVar[] VARS, int[] COEFFS, IntVar SCALAR) {
 		return scalar(VARS, COEFFS, "=", SCALAR);
 	}
 
 	/**
 	 * A scalar constraint which ensures that Sum(VARS[i]*COEFFS[i]) OPERATOR SCALAR
 	 * @param VARS		a collection of IntVar
 	 * @param COEFFS	a collection of int, for which |VARS|=|COEFFS|
 	 * @param OPERATOR	an operator in {"=", "!=", ">","<",">=","<="}
 	 * @param SCALAR	an IntVar
 	 * @return a scalar constraint
 	 */
 	public static Constraint scalar(IntVar[] VARS, int[] COEFFS, String OPERATOR, IntVar SCALAR) {
 		if(VARS.length==0){
 			return arithm(VF.fixed(0,SCALAR.getSolver()),OPERATOR,SCALAR);
 		}
 		// detect sums
 		int n = VARS.length;
 		int nbOne = 0;
 		int nbMinusOne = 0;
 		int nbZero = 0;
 		for(int i=0;i<n;i++){
 			if(COEFFS[i]==1){
 				nbOne++;
 			}
 			else if (COEFFS[i]==-1){
 				nbMinusOne++;
 			}else if (COEFFS[i]==0){
 				nbZero++;
 			}
 		}
 		if(nbZero>0){
 			IntVar[] nonZerosVars = new IntVar[n-nbZero];
 			int[] nonZerosCoefs   = new int[n-nbZero];
 			int k = 0;
 			for(int i=0;i<n;i++){
				if (COEFFS[i]==0){
 					nonZerosVars[k] = VARS[i];
 					nonZerosCoefs[k]= COEFFS[k];
 					k++;
 				}
 			}
 			return scalar(nonZerosVars,nonZerosCoefs,OPERATOR,SCALAR);
 		}
 		if(nbOne+nbMinusOne==n){
 			if(nbOne==n){
 				return sum(VARS,OPERATOR,SCALAR);
 			}else if(nbMinusOne==n){
 				return sum(VARS,Operator.getFlip(OPERATOR),VF.minus(SCALAR));
 			}else if(SCALAR.instantiated()){
 				if(nbMinusOne==1){
 					IntVar[] v2 = new IntVar[n-1];
 					IntVar s2 = null;
 					int k = 0;
 					for(int i=0;i<n;i++){
 						if (COEFFS[i]!=-1){
 							v2[k++] = VARS[i];
 						}else{
 							s2 = VARS[i];
 						}
 					}
 					return sum(v2,OPERATOR,VF.offset(s2,SCALAR.getValue()));
 				}
 				else if(nbOne==1){
 					IntVar[] v2 = new IntVar[n-1];
 					IntVar s2 = null;
 					int k = 0;
 					for(int i=0;i<n;i++){
 						if (COEFFS[i]!=1){
 							v2[k++] = VARS[i];
 						}else{
 							s2 = VARS[i];
 						}
 					}
 					return sum(v2,Operator.getFlip(OPERATOR),VF.offset(s2,-SCALAR.getValue()));
 				}
 			}
 		}
 		//
 		if(OPERATOR.equals("=")){
 			return Scalar.buildScalar(VARS, COEFFS, SCALAR, 1, VARS[0].getSolver());
 		}
 		int[] b = Scalar.getScalarBounds(VARS,COEFFS);
 		IntVar p = VF.bounded(StringUtils.randomName(),b[0],b[1],SCALAR.getSolver());
 		SCALAR.getSolver().post(scalar(VARS,COEFFS,"=",p));
 		return arithm(p,OPERATOR,SCALAR);
 	}
 
 	/**
 	 * Creates a subcircuit constraint which ensures that
      * <p/> the elements of vars define a single circuit of subcircuitSize nodes where
      * <p/> VARS[i] = OFFSET+j means that j is the successor of i.
      * <p/> and VARS[i] = OFFSET+i means that i is not part of the circuit
      * <p/> the constraint ensures that |{VARS[i] =/= OFFSET+i}| = SUBCIRCUIT_SIZE
      * <p/>
      * <p/> Filtering algorithms:
      * <p/> subtour elimination : Caseau & Laburthe (ICLP'97)
      * <p/> allDifferent GAC algorithm: R&eacute;gin (AAAI'94)
 	 * <p/> dominator-based filtering: Fages & Lorca (CP'11)
 	 * <p/> SCC-based filtering
      *
      * @param VARS
      * @param OFFSET          0 by default but 1 if used within MiniZinc
      *                        (which counts from 1 to n instead of from 0 to n-1)
      * @param SUBCIRCUIT_SIZE expected number of nodes in the circuit
      * @return a subcircuit constraint
      */
     public static Constraint subcircuit(IntVar[] VARS, int OFFSET, IntVar SUBCIRCUIT_SIZE) {
         int n = VARS.length;
         Solver solver = VARS[0].getSolver();
         IntVar nbLoops = VariableFactory.bounded("nLoops", 0, n, solver);
         Constraint c = new Constraint(ArrayUtils.append(VARS, new IntVar[]{nbLoops, SUBCIRCUIT_SIZE}), solver);
 		c.addPropagators(new PropEqualXY_C(new IntVar[]{nbLoops, SUBCIRCUIT_SIZE}, n));
         c.addPropagators(new PropIndexValue(VARS, OFFSET, nbLoops));
         c.addPropagators(new PropSubcircuit(VARS, OFFSET, SUBCIRCUIT_SIZE));
         c.addPropagators(AllDifferent.createPropagators(VARS, AllDifferent.Type.AC));
         c.addPropagators(new PropSubcircuit_AntiArboFiltering(VARS, OFFSET));
         c.addPropagators(new PropSubCircuitSCC(VARS, OFFSET));
         return c;
     }
 
     /**
      * Enforces that &#8721;<sub>i in |VARS|</sub>VARS<sub>i</sub> = SUM.
      *
      * @param VARS a vector of variables
      * @param SUM  a variable
      */
     public static Constraint sum(IntVar[] VARS, IntVar SUM) {
         return sum(VARS,"=",SUM);
     }
 
 	/**
 	 * Enforces that &#8721;<sub>i in |VARS|</sub>VARS<sub>i</sub> OPERATOR SUM.
 	 * @param VARS		a collection of IntVar
 	 * @param OPERATOR	operator in {"=", "!=", ">","<",">=","<="}
 	 * @param SUM		an IntVar
 	 * @return	a sum constraint
 	 */
 	public static Constraint sum(IntVar[] VARS, String OPERATOR, IntVar SUM) {
 		if (VARS.length == 2 && SUM.instantiated()) {
 			return arithm(VARS[0],"+",VARS[1],OPERATOR,SUM.getValue());
 		}else{
 			return new Sum(VARS,Operator.get(OPERATOR),SUM);
 		}
 	}
 
     /**
      * Enforces that &#8721;<sub>i in |VARS|</sub>VARS<sub>i</sub> = SUM.
      * This constraint is much faster than the one over integer variables
      *
      * @param VARS a vector of boolean variables
      * @param SUM  a variable
      */
     public static Constraint sum(BoolVar[] VARS, IntVar SUM) {
         Constraint c = new Constraint(ArrayUtils.append(VARS, new IntVar[]{SUM}), SUM.getSolver());
         c.setPropagators(new PropBoolSum(VARS, SUM));
         return c;
     }
 
     /**
      * Create a table constraint, with the specified algorithm defined ALGORITHM
      * <p/>
      * <b>AC2001</b>: Arc Consistency version 2001,
      * <br/>
      * <b>AC32</b>: Arc Consistency version 32,
      * <br/>
      * <b>FC</b>: Forward Checking.
      *
      * @param VARS      first variable
      * @param RELATION  the relation between the two variables
      * @param ALGORITHM to choose among {"AC2001", "AC32", "FC"}
      */
     public static LargeCSP table(IntVar[] VARS, LargeRelation RELATION, String ALGORITHM) {
         return new LargeCSP(VARS, RELATION, LargeCSP.Type.valueOf(ALGORITHM), VARS[0].getSolver());
     }
 
     /**
      * Partition succs variables into nbArbo (anti) arborescences
      * <p/> vars[i] = offset+j means that j is the successor of i.
      * <p/> and vars[i] = offset+i means that i is a root
      * <p/>
      * <p/> dominator-based filtering: Fages & Lorca (CP'11)
      * <p/> However, the filtering over nbArbo is quite light here
      *
      * @param succs  successors variables
      * @param nbArbo number of arborescences (=number of loops)
      * @param offSet 0 by default but 1 if used within MiniZinc
      *               (which counts from 1 to n instead of from 0 to n-1)
      * @return a tree constraint
      */
     public static Constraint tree(IntVar[] succs, IntVar nbArbo, int offSet) {
         Solver solver = nbArbo.getSolver();
         Constraint c = new Constraint(ArrayUtils.append(succs, new IntVar[]{nbArbo}), solver);
         c.setPropagators(new PropAntiArborescences(succs, offSet, false),
                 new PropKLoops(succs, nbArbo, offSet));
         return c;
     }
 }
