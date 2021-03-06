 package aeminiumruntime;
 
 /* interface for base hint */
 public class Hint {
 	private static Hint hintSingleton = new Hint();
	private static Hint.Loops loopsHint = hintSingleton.new Loops();
	private static Hint.Recursion recursionHint = hintSingleton.new Recursion();
 	
 	/* private protected constructor to prevent instantiation */
 	protected Hint() {}
 	
 	public static Hint createLoopsHint() {
		return loopsHint;
 	}
 	
 	public static Hint createRecursionHint() {
		return recursionHint;
 	}
 	
 	public static Hint createStepsHint(long count) {
 		return hintSingleton.new Steps(count);
 	}
 	
 	/* task contains loops */
 	public class Loops extends Hint {
 		/* private protected constructor to prevent instantiation from outside */
 		protected Loops() {}
 	};
 
 	/* task contains recursion */
 	public class Recursion extends Hint  {
 		/* private protected constructor to prevent instantiation from outside */
 		protected Recursion() {}
 	};
 
 	/* estimated virtual execution steps */
 	public class Steps extends Hint {
 		private long stepCount = 0;
 	
 		/* private protected constructor to prevent instantiation from outside */
 		protected Steps(long count){
 			this.stepCount = count;
 		}
 		public long getStepCount() {
 			return stepCount;
 		}
 	};
 }
