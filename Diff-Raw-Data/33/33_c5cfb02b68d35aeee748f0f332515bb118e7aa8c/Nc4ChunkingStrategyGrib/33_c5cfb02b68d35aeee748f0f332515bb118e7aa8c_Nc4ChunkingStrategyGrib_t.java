 package ucar.nc2.jni.netcdf;
 
 import ucar.nc2.Variable;
 
 /**
  * chunk on last 2 dimensions, like GRIB
  *
  * @author caron
  * @since 11/26/12
  */
 public class Nc4ChunkingStrategyGrib extends Nc4ChunkingStrategyImpl {

   public Nc4ChunkingStrategyGrib(int deflateLevel, boolean shuffle) {
     super(deflateLevel, shuffle);
   }
 
   @Override
   public boolean isChunked(Variable v) {
     int n = v.getRank();
     return n >= 2;
   }
 
   @Override
   public long[] computeChunking(Variable v) {
     int n = v.getRank();
     long[] result = new long[n];
     for (int i=0; i<n; i++)
       result[i] = (i<n-2) ? 1 : v.getDimension(i).getLength();
     return result;
   }
 }
