 // File: $Id$
 
 /**
  * A parallel SAT solver. Given a symbolic boolean equation in CNF, find a set
  * of assignments that make this equation true.
  * 
  * This implementation tries to do all the things a professional SAT
  * solver would do, although we are limited by implementation time and
  * the fact that we need to parallelize the stuff.
  * 
  * @author Kees van Reeuwijk
  * @version $Revision$
  */
 
 import java.io.File;
 
 public final class DPLLSolver extends ibis.satin.SatinObject implements DPLLInterface, java.io.Serializable {
     private static final boolean traceSolver = false;
     private static final boolean printSatSolutions = true;
     private static final boolean traceNewCode = true;
     private static int label = 0;
     final SATProblem p;
 
     public DPLLSolver( SATProblem p )
     {
         this.p = p;
     }
 
     /**
      * Solve the leaf part of a SAT problem.
      * The method throws a SATResultException if it finds a solution,
      * or terminates normally if it cannot find a solution.
      * @param level branching level
      * @param ctx the changable context of the solver
      * @param var the next variable to assign
      * @param val the value to assign
      */
     public void leafSolve(
 	int level,
 	DPLLContext ctx,
 	int var,
 	boolean val
     ) throws SATException
     {
 	if( traceSolver ){
 	    System.err.println( "ls" + level + ": trying assignment var[" + var + "]=" + val );
 	}
 	ctx.assignment[var] = val?1:0;
 	int res;
 	if( val ){
 	    res = ctx.propagatePosAssignment( p, var );
 	}
 	else {
 	    res = ctx.propagateNegAssignment( p, var );
 	}
 	if( res == SATProblem.CONFLICTING ){
 	    if( traceSolver ){
 		System.err.println( "ls" + level + ": propagation found a conflict" );
 	    }
 	    return;
 	}
 	if( res == SATProblem.SATISFIED ){
 	    // Propagation reveals problem is satisfied.
 	    SATSolution s = new SATSolution( ctx.assignment );
 
 	    if( traceSolver | printSatSolutions ){
 		System.err.println( "ls" + level + ": propagation found a solution: " + s );
 	    }
 	    if( !p.isSatisfied( ctx.assignment ) ){
 		System.err.println( "Error: " + level + ": solution does not satisfy problem." );
 	    }
 	    throw new SATResultException( s );
 	}
 	int nextvar = ctx.getDecisionVariable();
 	if( nextvar<0 ){
 	    // There are no variables left to assign, clearly there
 	    // is no solution.
 	    if( traceSolver ){
 		System.err.println( "ls" + level + ": nothing to branch on" );
 	    }
 	    return;
 	}
 
 	boolean firstvar = ctx.posDominant( nextvar );
 	DPLLContext subctx = (DPLLContext) ctx.clone();
 	leafSolve( level+1, subctx, nextvar, firstvar );
 	// Since we won't be using our context again, we may as well
 	// give it to the recursion.
 	leafSolve( level+1, ctx, nextvar, !firstvar );
     }
 
     /**
      * The method that implements a Satin task.
      * The method throws a SATResultException if it finds a solution,
      * or terminates normally if it cannot find a solution.
      * @param level branching level
      * @param p the SAT problem to solve
      * @param ctx the changable context of the solver
      * @param var the next variable to assign
      * @param val the value to assign
      */
     public void solve(
 	int level,
 	DPLLContext ctx,
 	int var,
 	boolean val
     ) throws SATException
     {
 	if( traceSolver ){
 	    System.err.println( "s" + level + ": trying assignment var[" + var + "]=" + val );
 	}
 
 	ctx.assignment[var] = val?1:0;
 	int res;
 	if( val ){
 	    res = ctx.propagatePosAssignment( p, var );
 	}
 	else {
 	    res = ctx.propagateNegAssignment( p, var );
 	}
 	if( res == SATProblem.CONFLICTING ){
 	    // Propagation reveals a conflict.
 	    if( traceSolver ){
 		System.err.println( "s" + level + ": propagation found a conflict" );
 	    }
 	    return;
 	}
 	if( res == SATProblem.SATISFIED ){
 	    // Propagation reveals problem is satisfied.
 	    SATSolution s = new SATSolution( ctx.assignment );
 
 	    if( traceSolver | printSatSolutions ){
 		System.err.println( "s" + level + ": propagation found a solution: " + s );
 	    }
 	    if( !p.isSatisfied( ctx.assignment ) ){
 		System.err.println( "Error: " + level + ": solution does not satisfy problem." );
 	    }
 	    throw new SATResultException( s );
 	}
 	int nextvar = ctx.getDecisionVariable();
 	if( nextvar<0 ){
 	    // There are no variables left to assign, clearly there
 	    // is no solution.
 	    if( traceSolver ){
 		System.err.println( "s" + level + ": nothing to branch on" );
 	    }
 	    return;
 	}
 
         boolean firstvar = ctx.posDominant( nextvar );
        if( !needMoreJobs() ){
 	    DPLLContext firstctx = (DPLLContext) ctx.clone();
 	    solve( level+1, firstctx, nextvar, firstvar );
 	    DPLLContext secondctx = (DPLLContext) ctx.clone();
 	    solve( level+1, secondctx, nextvar, !firstvar );
 	    sync();
 	}
 	else {
 	    // We're nearly there, use the leaf solver.
 	    DPLLContext subctx = (DPLLContext) ctx.clone();
 	    leafSolve( level+1, subctx, nextvar, firstvar );
 	    leafSolve( level+1, ctx, nextvar, !firstvar );
 	}
     }
 
     /**
      * Given a SAT problem, returns a solution, or <code>null</code> if
      * there is no solution.
      * @param p The problem to solve.
      * @return a solution of the problem, or <code>null</code> if there is no solution
      */
     static public SATSolution solveSystem( final SATProblem p )
     {
 	SATSolution res = null;
 	long startTime = 0;
 
 	if( p.isConflicting() ){
 	    return null;
 	}
 	if( p.isSatisfied() ){
 	    return new SATSolution( p.buildInitialAssignments() );
 	}
         DPLLSolver s = new DPLLSolver( p );
 
         // Now recursively try to find a solution.
 	try {
 	    DPLLContext ctx = DPLLContext.buildDPLLContext( p );
 
 	    ctx.assignment = p.buildInitialAssignments();
 
 	    int r = ctx.optimize( p );
 	    if( r == SATProblem.SATISFIED ){
 		if( !p.isSatisfied( ctx.assignment ) ){
 		    System.err.println( "Error: solution does not satisfy problem." );
 		}
 		return new SATSolution( ctx.assignment );
 	    }
 	    if( r == SATProblem.CONFLICTING ){
 		return null;
 	    }
 
 	    int nextvar = ctx.getDecisionVariable();
 	    if( nextvar<0 ){
 		// There are no variables left to assign, clearly there
 		// is no solution.
 		if( traceSolver | traceNewCode ){
 		    System.err.println( "top: nothing to branch on" );
 		}
 		return null;
 	    }
 	    if( traceSolver ){
 		System.err.println( "Top level: branching on variable " + nextvar );
 	    }
 
 	    DPLLContext negctx = (DPLLContext) ctx.clone();
 	    boolean firstvar = ctx.posDominant( nextvar );
 	    startTime = System.currentTimeMillis();
             s.solve( 0, negctx, nextvar, firstvar );
             s.solve( 0, ctx, nextvar, !firstvar );
             s.sync();
 	}
 	catch( SATResultException r ){
 	    if( r.s == null ){
 		System.err.println( "A null solution thrown???" );
 	    }
 	    res = r.s;
 	    s.abort();
 	}
         catch( SATException x ){
             System.err.println( "Uncaught " + x + "???" );
         }
 
 	long endTime = System.currentTimeMillis();
 	double time = ((double) (endTime - startTime))/1000.0;
 
 	System.out.println( "Parallel caclulation Time: " + time );
 	return res;
     }
 
     /**
      * Allows execution of the class.
      * @param args The command-line arguments.
      */
     public static void main( String args[] ) throws java.io.IOException
     {
 	if( args.length != 1 ){
 	    System.err.println( "Exactly one filename argument required." );
 	    System.exit( 1 );
 	}
 	File f = new File( args[0] );
 	if( !f.exists() ){
 	    System.err.println( "File does not exist: " + f );
 	    System.exit( 1 );
 	}
 
         // Turn Satin temporarily off to prevent slowdowns of
 	// sequential code.
 	ibis.satin.SatinObject.pause(); 
 
 	SATProblem p = SATProblem.parseDIMACSStream( f );
 	p.setReviewer( new CubeClauseReviewer() );
 	p.report( System.out );
 	p.optimize();
 	p.report( System.out );
 
         // Turn Satin on again
 	ibis.satin.SatinObject.resume();
 
 	long startTime = System.currentTimeMillis();
 	SATSolution res = solveSystem( p );
 
 	long endTime = System.currentTimeMillis();
 	double time = ((double) (endTime - startTime))/1000.0;
 
 	System.out.println( "ExecutionTime: " + time );
 	if( res == null ){
 	    System.out.println( "There are no solutions" );
 	}
 	else {
 	    System.out.println( "There is a solution: " + res );
 	}
     }
 }
