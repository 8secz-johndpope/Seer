 public class Nor extends Logikgatter
 {
 
 	public Nor()	
 	{
 		super();
 	}
 
 		
 	/** @return boolean gibt true zurueck, wenn der vorgegebene Ausgang erreicht wurde */
 	public boolean pruefeAusgang(boolean eingangszustand1, boolean eingangszustand2){
		if ( eingangszustand1 & eingangszustand2 != super.getAusgang() ){
 			return false;
 		} else
 		{
 			return true;
 		}
 	}
 		public String toString(){
 		return "Nor";
 	}
 }
