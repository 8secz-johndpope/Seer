 /*
 
    Derby - Class org.apache.derby.iapi.types.SQLClob
 
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to you under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 
  */
 
 package org.apache.derby.iapi.types;
 
 import org.apache.derby.iapi.error.StandardException;
 
 import org.apache.derby.iapi.jdbc.CharacterStreamDescriptor;
 
 import org.apache.derby.iapi.services.io.InputStreamUtil;
 import org.apache.derby.iapi.services.io.StoredFormatIds;
 
 import org.apache.derby.iapi.services.sanity.SanityManager;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.sql.Clob;
 import java.sql.Date;
 import java.sql.SQLException;
 import java.sql.Time;
 import java.sql.Timestamp;
 import java.text.RuleBasedCollator;
 import java.util.Calendar;
 
 
 /**
  * SQLClob represents a CLOB value with UCS_BASIC collation.
  * CLOB supports LIKE operator only for collation.
  */
 public class SQLClob
 	extends SQLVarchar
 {
 
     /**
      * Static stream header holder with the header used for a 10.5
      * stream with unknown char length. This header will be used with 10.5, and
      * possibly later databases. The expected EOF marker is '0xE0 0x00 0x00'.
      */
     protected static final StreamHeaderHolder UNKNOWN_LEN_10_5_HEADER_HOLDER =
             new StreamHeaderHolder(
                     new byte[] {0x00, 0x00, (byte)0xF0, 0x00, 0x00},
                     new byte[] {24, 16, -1, 8, 0}, true, true);
 
     /**
      * The descriptor for the stream. If there is no stream this should be
      * {@code null}, which is also true if the descriptor hasen't been
      * constructed yet.
      */
     private CharacterStreamDescriptor csd;
 
 	/*
 	 * DataValueDescriptor interface.
 	 *
 	 * These are actually all implemented in the super-class, but we need
 	 * to duplicate some of them here so they can be called by byte-code
 	 * generation, which needs to know the class the method appears in.
 	 */
 
 	public String getTypeName()
 	{
 		return TypeId.CLOB_NAME;
 	}
 
 	/*
 	 * DataValueDescriptor interface
 	 */
 
 	/** @see DataValueDescriptor#getClone */
 	public DataValueDescriptor getClone()
 	{
 		try
 		{
 			return new SQLClob(getString());
 		}
 		catch (StandardException se)
 		{
 			if (SanityManager.DEBUG)
 				SanityManager.THROWASSERT("Unexpected exception", se);
 			return null;
 		}
 	}
 
 	/**
 	 * @see DataValueDescriptor#getNewNull
 	 *
 	 */
 	public DataValueDescriptor getNewNull()
 	{
 		return new SQLClob();
 	}
 
 	/** @see StringDataValue#getValue(RuleBasedCollator) */
 	public StringDataValue getValue(RuleBasedCollator collatorForComparison)
 	{
 		if (collatorForComparison == null)
 		{//null collatorForComparison means use UCS_BASIC for collation
 		    return this;			
 		} else {
 			//non-null collatorForComparison means use collator sensitive
 			//implementation of SQLClob
 		     CollatorSQLClob s = new CollatorSQLClob(collatorForComparison);
 		     s.copyState(this);
 		     return s;
 		}
 	}
 
 	/*
 	 * Storable interface, implies Externalizable, TypedFormat
 	 */
 
 	/**
 		Return my format identifier.
 
 		@see org.apache.derby.iapi.services.io.TypedFormat#getTypeFormatId
 	*/
 	public int getTypeFormatId() {
 		return StoredFormatIds.SQL_CLOB_ID;
 	}
 
 	/*
 	 * constructors
 	 */
 
 	public SQLClob()
 	{
 	}
 
 	public SQLClob(String val)
 	{
 		super(val);
 	}
 
 	/*
 	 * DataValueDescriptor interface
 	 */
 
 	/* @see DataValueDescriptor#typePrecedence */
 	public int typePrecedence()
 	{
 		return TypeId.CLOB_PRECEDENCE;
 	}
 
 	/*
 	** disable conversions to/from most types for CLOB.
 	** TEMP - real fix is to re-work class hierachy so
 	** that CLOB is towards the root, not at the leaf.
 	*/
 
 	public Object	getObject() throws StandardException
 	{
 		throw dataTypeConversion("java.lang.Object");
 	}
 
 	public boolean	getBoolean() throws StandardException
 	{
 		throw dataTypeConversion("boolean");
 	}
 
 	public byte	getByte() throws StandardException
 	{
 		throw dataTypeConversion("byte");
 	}
 
 	public short	getShort() throws StandardException
 	{
 		throw dataTypeConversion("short");
 	}
 
 	public int	getInt() throws StandardException
 	{
 		throw dataTypeConversion("int");
 	}
 
 	public long	getLong() throws StandardException
 	{
 		throw dataTypeConversion("long");
 	}
 
 	public float	getFloat() throws StandardException
 	{
 		throw dataTypeConversion("float");
 	}
 
 	public double	getDouble() throws StandardException
 	{
 		throw dataTypeConversion("double");
 	}
 	public int typeToBigDecimal() throws StandardException
 	{
 		throw dataTypeConversion("java.math.BigDecimal");
 	}
 	public byte[]	getBytes() throws StandardException
 	{
 		throw dataTypeConversion("byte[]");
 	}
 
 	public Date	getDate(java.util.Calendar cal) throws StandardException
 	{
 		throw dataTypeConversion("java.sql.Date");
 	}
 
     /**
      * Returns a descriptor for the input stream for this CLOB value.
      * <p>
      * The descriptor contains information about header data, current positions,
      * length, whether the stream should be buffered or not, and if the stream
      * is capable of repositioning itself.
      *
      * @return A descriptor for the stream, which includes a reference to the
      *      stream itself. If the value cannot be represented as a stream,
      *      {@code null} is returned instead of a decsriptor.
      * @throws StandardException if obtaining the descriptor fails
      */
     public CharacterStreamDescriptor getStreamWithDescriptor()
             throws StandardException {
         if (stream == null) {
             // Lazily reset the descriptor here, to avoid further changes in
             // {@code SQLChar}.
             csd = null;
             return null;
         }
         // NOTE: Getting down here several times is potentially dangerous.
         // When the stream is published, we can't assume we know the position
         // any more. The best we can do, which may hurt performance to some
         // degree in some non-recommended use-cases, is to reset the stream if
         // possible.
         if (csd != null) {
             if (stream instanceof Resetable) {
                 try {
                     ((Resetable)stream).resetStream();
                     // Make sure the stream is in sync with the descriptor.
                     InputStreamUtil.skipFully(stream, csd.getCurBytePos());
                 } catch (IOException ioe) {
                     throwStreamingIOException(ioe);
                 }
             } else {
                 if (SanityManager.DEBUG) {
                     SanityManager.THROWASSERT("Unable to reset stream when " +
                             "fetched the second time: " + stream.getClass());
                 }
             }
         }
 
         if (csd == null) {
             // First time, read the header format of the stream.
             // NOTE: For now, just read the old header format.
             try {
                 final int dataOffset = 2;
                 byte[] header = new byte[dataOffset];
                 int read = stream.read(header);
                 if (read != dataOffset) {
                     String hdr = "[";
                     for (int i=0; i < read; i++) {
                         hdr += Integer.toHexString(header[i] & 0xff);
                     }
                     throw new IOException("Invalid stream header length " +
                             read + ", got " + hdr + "]");
                 }
 
                 // Note that we add the two bytes holding the header *ONLY* if
                 // we know how long the user data is.
                 long utflen = ((header[0] & 0xff) << 8) | ((header[1] & 0xff));
                 if (utflen > 0) {
                     utflen += dataOffset;
                 }
 
                 csd = new CharacterStreamDescriptor.Builder().stream(stream).
                     bufferable(false).positionAware(false).
                     curCharPos(1).curBytePos(dataOffset).
                     dataOffset(dataOffset).byteLength(utflen).build();
             } catch (IOException ioe) {
                 throwStreamingIOException(ioe);
             }
         }
         return this.csd;
     }
 
 	public Time	getTime(java.util.Calendar cal) throws StandardException
 	{
 		throw dataTypeConversion("java.sql.Time");
 	}
 
 	public Timestamp	getTimestamp(java.util.Calendar cal) throws StandardException
 	{
 		throw dataTypeConversion("java.sql.Timestamp");
 	}
     
     /**
      * Gets a trace representation of the CLOB for debugging.
      *
      * @return a trace representation of the CLOB.
      */
     public final String getTraceString() throws StandardException {
         // Check if the value is SQL NULL.
         if (isNull()) {
             return "NULL";
         }
 
         // Check if we have a stream.
         if (getStream() != null) {
             return (getTypeName() + "(" + getStream().toString() + ")");
         }
 
         return (getTypeName() + "(" + getLength() + ")");
     }
     
     /**
      * Normalization method - this method may be called when putting
      * a value into a SQLClob, for example, when inserting into a SQLClob
      * column.  See NormalizeResultSet in execution.
      * Per the SQL standard ,if the clob column is not big enough to 
      * hold the value being inserted,truncation error will result
      * if there are trailing non-blanks. Truncation of trailing blanks
      * is allowed.
      * @param desiredType   The type to normalize the source column to
      * @param sourceValue   The value to normalize
      *
      *
      * @exception StandardException             Thrown for null into
      *                                          non-nullable column, and for
      *                                          truncation error
      */
 
     public void normalize(
                 DataTypeDescriptor desiredType,
                 DataValueDescriptor sourceValue)
                     throws StandardException
     {
         // if sourceValue is of type clob, and has a stream,
         // dont materialize it here (as the goal of using a stream is to
         // not have to materialize whole object in memory in the server), 
         // but instead truncation checks will be done when data is streamed in.
         // (see ReaderToUTF8Stream) 
         // if sourceValue is not a stream, then follow the same
         // protocol as varchar type for normalization
         if( sourceValue instanceof SQLClob)
         {
             SQLClob clob = (SQLClob)sourceValue;
             if (clob.stream != null)
             {
                 copyState(clob);
                 return;
             }
         }
         
         super.normalize(desiredType,sourceValue);
     }
 
 	public void setValue(Time theValue, Calendar cal) throws StandardException
 	{
 		throwLangSetMismatch("java.sql.Time");
 	}
 	
 	public void setValue(Timestamp theValue, Calendar cal) throws StandardException
 	{
 		throwLangSetMismatch("java.sql.Timestamp");
 	}
 	
 	public void setValue(Date theValue, Calendar cal) throws StandardException
 	{
 		throwLangSetMismatch("java.sql.Date");
 	}
 	
 	public void setBigDecimal(Number bigDecimal) throws StandardException
 	{
 		throwLangSetMismatch("java.math.BigDecimal");
 	}
 
     /**
      * Sets a new stream for this CLOB.
      *
      * @param stream the new stream
      */
     public final void setStream(InputStream stream) {
         super.setStream(stream);
         // Discard the old stream descriptor.
         this.csd = null;
     }
 
 	public void setValue(int theValue) throws StandardException
 	{
 		throwLangSetMismatch("int");
 	}
 
 	public void setValue(double theValue) throws StandardException
 	{
 		throwLangSetMismatch("double");
 	}
 
 	public void setValue(float theValue) throws StandardException
 	{
 		throwLangSetMismatch("float");
 	}
  
 	public void setValue(short theValue) throws StandardException
 	{
 		throwLangSetMismatch("short");
 	}
 
 	public void setValue(long theValue) throws StandardException
 	{
 		throwLangSetMismatch("long");
 	}
 
 
 	public void setValue(byte theValue) throws StandardException
 	{
 		throwLangSetMismatch("byte");
 	}
 
 	public void setValue(boolean theValue) throws StandardException
 	{
 		throwLangSetMismatch("boolean");
 	}
 
 	public void setValue(byte[] theValue) throws StandardException
 	{
 		throwLangSetMismatch("byte[]");
 	}
     
     /**
      * Set the value from an non-null Java.sql.Clob object.
      */
     final void setObject(Object theValue)
         throws StandardException
     {
         Clob vc = (Clob) theValue;
         
         try {
             long vcl = vc.length();
             if (vcl < 0L || vcl > Integer.MAX_VALUE)
                 throw this.outOfRange();
 
             ReaderToUTF8Stream utfIn = new ReaderToUTF8Stream(
                     vc.getCharacterStream(), (int) vcl, 0, TypeId.CLOB_NAME,
                     getStreamHeaderGenerator());
             setValue(utfIn, (int) vcl);
         } catch (SQLException e) {
             throw dataTypeConversion("DAN-438-tmp");
        }
     }
 }
