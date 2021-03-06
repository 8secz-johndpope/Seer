 package apes.models;
 
 import java.math.BigInteger;
 import java.nio.ByteBuffer;
 
 /**
  * Class for containing a bunch of samples as used by the InternalFormat.
  * @author Daniel Kvick (kvick@student.chalmers.se)
  *
  */
 public class Samples
 {
   /**
    * Size of a sample in bits.
    */
   public static final int BITS_PER_SAMPLE = 32;
   
   /**
    * Size of a sample in bytes.
    */
   public static final int BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8;
   
   /**
    * Amount of samples in this object.
    */
   private int size;
   
   /**
    * Maximum amplitude of all samples in the Samples object.
    */
   private int maxAmplitude;
   
   /**
    * The index of the sample due to which the maxAmplitude was last updated.
    */
   private int maxAmplitudeIndex;
   
   /**
    * Contains all the raw data of the samples.
    */
   private byte[] sampleData;
   
   /**
    * Constructs a <code>Samples</code> object with the desired size.
    * @param amount number of samples.
    */
   public Samples( int amount )
   {
     size = amount;
     sampleData = new byte[size * BYTES_PER_SAMPLE]; 
     maxAmplitude = Integer.MIN_VALUE;
     maxAmplitudeIndex = -1;
   }
   
   /**
    * Constructs a <code>Samples</code> object from the given data. 
    * @param data sample data using the specified bps.
    */
   public Samples(int bps, byte[] data) throws Exception  
   {
     // Complain if bps is invalid
     if(bps <= 0 || bps%8 != 0 || bps > BITS_PER_SAMPLE)
     {
       throw new Exception("Invalid amount of bits per sample");
     }
     
     maxAmplitude = Integer.MIN_VALUE;
     maxAmplitudeIndex = -1;
     
     // Count bytes instead of bits
     int Bps = bps / 8;
     size = data.length / Bps;
     sampleData = new byte[ size * BYTES_PER_SAMPLE ];
     int BpsDiff = BYTES_PER_SAMPLE - Bps;
 
     // Transfer data to sampleData array.
     for(int i = 0; i < size; i++)
     {
       // Read value
       byte[] val = new byte[Bps];
       for(int j = 0; j < Bps; j++)
         val[j] = data[(i + 1) * Bps - j -1];
       BigInteger bigAmp = new BigInteger(val);
       int amplitude = bigAmp.intValue();
 
       // Write value
       for( int j = 0; j < BYTES_PER_SAMPLE; j++ )
       {
         sampleData[i * BYTES_PER_SAMPLE + j] = (byte)((amplitude >> ( 8 * j )) & 0xff);
       }
       
       // Update max
       if( amplitude > maxAmplitude )
       {
         maxAmplitude = amplitude;
         maxAmplitudeIndex = i;
       }
     }
   }
   
   /**
    * Returns amount of samples.
    * @return the amount of samples stored in this samples object.
    */
   public int getSize()
   {
     return size;
   }
   
   /**
    * Returns an approximation of the average amplitude among all samples in this samples object.
    * @return An approximate average amplitude over all samples.
    */
   public int getAverageAmplitude()
   {
     int total = 0;
     for( int i = 0; i < size; i += 10 )
       total += getSample( i );
     return total / size;
   }
   
   /**
    * Returns the maximum amplitude among all samples in this object.
    * @return The value of the highest amplitude of all samples in this object.
    */
   public int getMaxAmplitude()
   {
     return maxAmplitude;
   }
 
   /**
    * Returns the sample at given index.
    * @param index The index of the desired sample. 
    * @return The amplitude of the requested sample.
    */
   public int getSample( int index )
   {
     int value = 0;
     byte[] amp = new byte[BYTES_PER_SAMPLE];
     for(int i = 0; i < BYTES_PER_SAMPLE; i++)
     {
       amp[i] = sampleData[(index + 1) * BYTES_PER_SAMPLE - i - 1];
       value = new BigInteger(amp).intValue();
     }
     return value;
   }
   
   /**
    * Sets amplitude of selected sample.
    * @param index The index of the sample to affect.
    * @param value The desired amplitude.
    * @throws Exception Throws an exception of the amplitude has a negative value.
    */
   public void setSample( int index, int value ) throws Exception
   { 
     for(int i = 0; i < BYTES_PER_SAMPLE; i++)
      sampleData[index * BYTES_PER_SAMPLE + i] = (byte)((value >> (i * 8)) & 0xff);
     
     // If higher, we have a new max.
     if( value > maxAmplitude )
     {
       maxAmplitude = value;
       maxAmplitudeIndex = index;
     }
     // If same index, we may need to update. If not, we don't.
     else if( index == maxAmplitudeIndex )
     {
       updateMaxAmplitude();
     }
   }
   
   /**
    * Calculates the maximum amplitude, stores it in <code>maxAmplitude</code> and updates <code>maxAmplitudeIndex</code> 
    */
   private void updateMaxAmplitude()
   {
     
     int max = Integer.MIN_VALUE;
     int maxI = -1;
     // Go through all samples
     for( int i = 0; i < size; i++ )
     {
       int amp = getSample( i );
      
       // Update max as needed. 
       if( amp > max )
       {
         max = amp;
         maxI = i;
       }
     }
     maxAmplitude = max;
     maxAmplitudeIndex = maxI;
   }
   
   /**
    * Returns all data.
    *
    * @return a <code>byte</code> array containing all data.
    */
   public byte[] getData()
   {
     return sampleData;
   }
 }
