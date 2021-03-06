 /*
  * The MIT License
  *
  * Copyright (c) 2009 The Broad Institute
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  * THE SOFTWARE.
  */
 package net.sf.samtools;
 
 
 import net.sf.samtools.util.BinaryCodec;
 import net.sf.samtools.util.BlockCompressedInputStream;
 import net.sf.samtools.util.CloseableIterator;
 import net.sf.samtools.util.StringLineReader;
 import net.sf.samtools.SAMFileReader.ValidationStringency;
 
 import java.io.*;
 import java.util.*;
 import java.net.URL;
 
 /**
  * Internal class for reading and querying BAM files.
  */
 class BAMFileReader2
     extends SAMFileReader.ReaderImplementation {
     // True if reading from a File rather than an InputStream
     private boolean mIsSeekable = false;
     // For converting bytes into other primitive types
     private BinaryCodec mStream = null;
     // Underlying compressed data stream.
     private final BlockCompressedInputStream mCompressedInputStream;
     private SAMFileHeader mFileHeader = null;
     // Populated if the file is seekable and an index exists
     private BAMFileIndex2 mFileIndex = null;
     private long mFirstRecordPointer = 0;
     private CloseableIterator<SAMRecord> mCurrentIterator = null;
     // If true, all SAMRecords are fully decoded as they are read.
     private final boolean eagerDecode;
     // For error-checking.
     private ValidationStringency mValidationStringency;
 
     /**
      * Prepare to read BAM from a stream (not seekable)
      * @param stream source of bytes.
      * @param eagerDecode if true, decode all BAM fields as reading rather than lazily.
      * @param validationStringency Controls how to handle invalidate reads or header lines.
      */
     BAMFileReader2(final InputStream stream, final boolean eagerDecode, final ValidationStringency validationStringency)
         throws IOException {
         mIsSeekable = false;
         mCompressedInputStream = new BlockCompressedInputStream(stream);
         mStream = new BinaryCodec(new DataInputStream(mCompressedInputStream));
         this.eagerDecode = eagerDecode;
         this.mValidationStringency = validationStringency;
         readHeader(null);
     }
 
     /**
      * Prepare to read BAM from a file (seekable)
      * @param file source of bytes.
      * @param eagerDecode if true, decode all BAM fields as reading rather than lazily.
      * @param validationStringency Controls how to handle invalidate reads or header lines.
      */
     BAMFileReader2(final File file, final boolean eagerDecode, final ValidationStringency validationStringency)
         throws IOException {
         this(new BlockCompressedInputStream(file), eagerDecode, file.getAbsolutePath(), validationStringency);
     }
 
 
     BAMFileReader2(final URL url, final boolean eagerDecode, final ValidationStringency validationStringency)
         throws IOException {
         this(new BlockCompressedInputStream(url), eagerDecode, url.toString(), validationStringency);
     }
 
     private BAMFileReader2(final BlockCompressedInputStream compressedInputStream, final boolean eagerDecode,
                           final String source, final ValidationStringency validationStringency)
         throws IOException {
         mIsSeekable = true;
         mCompressedInputStream = compressedInputStream;
         mStream = new BinaryCodec(new DataInputStream(mCompressedInputStream));
         this.eagerDecode = eagerDecode;
         this.mValidationStringency = validationStringency;
         readHeader(source);
         mFirstRecordPointer = mCompressedInputStream.getFilePointer();
     }
 
     void close() {
         if (mStream != null) {
             mStream.close();
         }
         mStream = null;
         mFileHeader = null;
         mFileIndex = null;
     }
 
     /**
      * @return the file index, if one exists, else null.
      */
     BAMFileIndex2 getFileIndex() {
         return mFileIndex;
     }
 
     void setFileIndex(final BAMFileIndex2 fileIndex) {
         mFileIndex = fileIndex;
     }
 
     SAMFileHeader getFileHeader() {
         return mFileHeader;
     }
 
     /**
      * Set error-checking level for subsequent SAMRecord reads.
      */
     void setValidationStringency(final SAMFileReader.ValidationStringency validationStringency) {
         this.mValidationStringency = validationStringency;
     }
 
     SAMFileReader.ValidationStringency getValidationStringency() {
         return this.mValidationStringency;
     }
 
     /**
      * Prepare to iterate through the SAMRecords in file order.
      * Only a single iterator on a BAM file can be extant at a time.  If getIterator() or a query method has been called once,
      * that iterator must be closed before getIterator() can be called again.
      * A somewhat peculiar aspect of this method is that if the file is not seekable, a second call to
      * getIterator() begins its iteration where the last one left off.  That is the best that can be
      * done in that situation.
      */
     CloseableIterator<SAMRecord> getIterator() {
         if (mStream == null) {
             throw new IllegalStateException("File reader is closed");
         }
         if (mCurrentIterator != null) {
             throw new IllegalStateException("Iteration in progress");
         }
         if (mIsSeekable) {
             try {
                 mCompressedInputStream.seek(mFirstRecordPointer);
             } catch (IOException exc) {
                 throw new RuntimeException(exc.getMessage(), exc);
             }
         }
         mCurrentIterator = new BAMFileIterator();
         return mCurrentIterator;
     }
 
     CloseableIterator<SAMRecord> getIterator(List<Chunk> chunks) {
         if (mStream == null) {
             throw new IllegalStateException("File reader is closed");
         }
         if (mCurrentIterator != null) {
             throw new IllegalStateException("Iteration in progress");
         }
         if (mIsSeekable) {
             try {
                 mCompressedInputStream.seek(mFirstRecordPointer);
             } catch (IOException exc) {
                 throw new RuntimeException(exc.getMessage(), exc);
             }
         }
 
         // Create an iterator over the given chunk boundaries.
         mCurrentIterator = new BAMFileIndexIterator(Chunk.toCoordinateArray(chunks));
         return mCurrentIterator;
     }
 
     public List<Bin> getOverlappingBins(final String sequence, final int start, final int end) {
         List<Bin> bins = Collections.emptyList();
 
         final SAMFileHeader fileHeader = getFileHeader();
         int referenceIndex = fileHeader.getSequenceIndex(sequence);
         if (referenceIndex != -1) {
             final BAMFileIndex2 fileIndex = getFileIndex();
             bins = fileIndex.getBinsContaining(referenceIndex, start, end);
         }
 
         return bins;
     }
 
     public List<Chunk> getFilePointersBounding(Bin bin) {
         return Chunk.toChunkList(getFileIndex().getFilePointersBounding(bin));
     }
 
     /**
      * Prepare to iterate through the SAMRecords that match the given interval.
      * Only a single iterator on a BAMFile can be extant at a time.  The previous one must be closed
      * before calling any of the methods that return an iterator.
      *
      * Note that an unmapped SAMRecord may still have a reference name and an alignment start for sorting
      * purposes (typically this is the coordinate of its mate), and will be found by this method if the coordinate
      * matches the specified interval.
      *
      * Note that this method is not necessarily efficient in terms of disk I/O.  The index does not have perfect
      * resolution, so some SAMRecords may be read and then discarded because they do not match the specified interval.
      *
      * @param sequence Reference sequence sought.
      * @param start Desired SAMRecords must overlap or be contained in the interval specified by start and end.
      * A value of zero implies the start of the reference sequence.
      * @param end A value of zero implies the end of the reference sequence.
      * @param contained If true, the alignments for the SAMRecords must be completely contained in the interval
      * specified by start and end.  If false, the SAMRecords need only overlap the interval.
      * @return Iterator for the matching SAMRecords
      */
     CloseableIterator<SAMRecord> query(final String sequence, final int start, final int end, final boolean contained) {
         if (mStream == null) {
             throw new IllegalStateException("File reader is closed");
         }
         if (mCurrentIterator != null) {
             throw new IllegalStateException("Iteration in progress");
         }
         if (!mIsSeekable) {
             throw new UnsupportedOperationException("Cannot query stream-based BAM file");
         }
         if (mFileIndex == null) {
             throw new IllegalStateException("No BAM file index is available");
         }
         mCurrentIterator = createIndexIterator(sequence, start, end, contained? QueryType.CONTAINED: QueryType.OVERLAPPING);
         return mCurrentIterator;
     }
 
     /**
      * Prepare to iterate through the SAMRecords with the given alignment start.
      * Only a single iterator on a BAMFile can be extant at a time.  The previous one must be closed
      * before calling any of the methods that return an iterator.
      *
      * Note that an unmapped SAMRecord may still have a reference name and an alignment start for sorting
      * purposes (typically this is the coordinate of its mate), and will be found by this method if the coordinate
      * matches the specified interval.
      *
      * Note that this method is not necessarily efficient in terms of disk I/O.  The index does not have perfect
      * resolution, so some SAMRecords may be read and then discarded because they do not match the specified interval.
      *
      * @param sequence Reference sequence sought.
      * @param start Alignment start sought.
      * @return Iterator for the matching SAMRecords.
      */
     CloseableIterator<SAMRecord> queryAlignmentStart(final String sequence, final int start) {
         if (mStream == null) {
             throw new IllegalStateException("File reader is closed");
         }
         if (mCurrentIterator != null) {
             throw new IllegalStateException("Iteration in progress");
         }
         if (!mIsSeekable) {
             throw new UnsupportedOperationException("Cannot query stream-based BAM file");
         }
         if (mFileIndex == null) {
             throw new IllegalStateException("No BAM file index is available");
         }
         mCurrentIterator = createIndexIterator(sequence, start, -1, QueryType.STARTING_AT);
         return mCurrentIterator;
     }
 
     public CloseableIterator<SAMRecord> queryUnmapped() {
         if (mStream == null) {
             throw new IllegalStateException("File reader is closed");
         }
         if (mCurrentIterator != null) {
             throw new IllegalStateException("Iteration in progress");
         }
         if (!mIsSeekable) {
             throw new UnsupportedOperationException("Cannot query stream-based BAM file");
         }
         if (mFileIndex == null) {
             throw new IllegalStateException("No BAM file index is available");
         }
         try {
             final long startOfLastLinearBin = mFileIndex.getStartOfLastLinearBin();
             if (startOfLastLinearBin != -1) {
                 mCompressedInputStream.seek(startOfLastLinearBin);
             } else {
                 // No mapped reads in file, just start at the first read in file.
                 mCompressedInputStream.seek(mFirstRecordPointer);
             }
             mCurrentIterator = new BAMFileIndexUnmappedIterator();
             return mCurrentIterator;
         } catch (IOException e) {
             throw new RuntimeException("IOException seeking to unmapped reads", e);
         }
     }
 
     /**
      * Reads the header from the file or stream
      * @param source Note that this is used only for reporting errors.
      */
     private void readHeader(final String source)
         throws IOException {
 
         final byte[] buffer = new byte[4];
         mStream.readBytes(buffer);
         if (!Arrays.equals(buffer, BAMFileConstants.BAM_MAGIC)) {
             throw new IOException("Invalid BAM file header");
         }
 
         final int headerTextLength = mStream.readInt();
         final String textHeader = mStream.readString(headerTextLength);
         final SAMTextHeaderCodec headerCodec = new SAMTextHeaderCodec();
         headerCodec.setValidationStringency(mValidationStringency);
         mFileHeader = headerCodec.decode(new StringLineReader(textHeader),
                 source);
 
         final int sequenceCount = mStream.readInt();
         if (mFileHeader.getSequenceDictionary().size() > 0) {
             // It is allowed to have binary sequences but no text sequences, so only validate if both are present
             if (sequenceCount != mFileHeader.getSequenceDictionary().size()) {
                 throw new SAMFormatException("Number of sequences in text header (" +
                         mFileHeader.getSequenceDictionary().size() +
                         ") != number of sequences in binary header (" + sequenceCount + ") for file " + source);
             }
             for (int i = 0; i < sequenceCount; i++) {
                 final SAMSequenceRecord binarySequenceRecord = readSequenceRecord(source);
                 final SAMSequenceRecord sequenceRecord = mFileHeader.getSequence(i);
                 if (!sequenceRecord.getSequenceName().equals(binarySequenceRecord.getSequenceName())) {
                     throw new SAMFormatException("For sequence " + i + ", text and binary have different names in file " +
                             source);
                 }
                 if (sequenceRecord.getSequenceLength() != binarySequenceRecord.getSequenceLength()) {
                     throw new SAMFormatException("For sequence " + i + ", text and binary have different lengths in file " +
                             source);
                 }
             }
         } else {
             // If only binary sequences are present, copy them into mFileHeader
             final List<SAMSequenceRecord> sequences = new ArrayList<SAMSequenceRecord>(sequenceCount);
             for (int i = 0; i < sequenceCount; i++) {
                 sequences.add(readSequenceRecord(source));
             }
             mFileHeader.setSequenceDictionary(new SAMSequenceDictionary(sequences));
         }
     }
 
     /**
      * Reads a single binary sequence record from the file or stream
      * @param source Note that this is used only for reporting errors.
      */
     private SAMSequenceRecord readSequenceRecord(final String source) {
         final int nameLength = mStream.readInt();
         if (nameLength <= 1) {
             throw new SAMFormatException("Invalid BAM file header: missing sequence name in file " + source);
         }
         final String sequenceName = mStream.readString(nameLength - 1);
         // Skip the null terminator
         mStream.readByte();
         final int sequenceLength = mStream.readInt();
         return new SAMSequenceRecord(sequenceName, sequenceLength);
     }
 
     /**
      * Iterator for non-indexed sequential iteration through all SAMRecords in file.
      * Starting point of iteration is wherever current file position is when the iterator is constructed.
      */
     private class BAMFileIterator implements CloseableIterator<SAMRecord> {
         private SAMRecord mNextRecord = null;
         private final BAMRecordCodec bamRecordCodec = new BAMRecordCodec(getFileHeader());
         private long samRecordIndex = 0; // Records at what position (counted in records) we are at in the file
 
         BAMFileIterator() {
             this(true);
         }
 
         /**
          * @param advance Trick to enable subclass to do more setup before advancing
          */
         BAMFileIterator(final boolean advance) {
             this.bamRecordCodec.setInputStream(BAMFileReader2.this.mStream.getInputStream());
 
             if (advance) {
                 advance();
             }
         }
 
         public void close() {
             if (this != mCurrentIterator) {
                 throw new IllegalStateException("Attempt to close non-current iterator");
             }
             mCurrentIterator = null;
         }
 
         public boolean hasNext() {
             return (mNextRecord != null);
         }
 
         public SAMRecord next() {
             final SAMRecord result = mNextRecord;
            if(result.getAlignmentStart() <= 11632602 && result.getAlignmentEnd() >= 11632602)
                System.out.printf("11632602: %s%n", result.getReadName());            
             advance();
             return result;
         }
 
         public void remove() {
             throw new UnsupportedOperationException("Not supported: remove");
         }
 
         void advance() {
             try {
                 mNextRecord = getNextRecord();
                 if (mNextRecord != null) {
                     ++this.samRecordIndex;
                     // Because some decoding is done lazily, the record needs to remember the validation stringency.
                     mNextRecord.setValidationStringency(mValidationStringency);
 
                     if (mValidationStringency != ValidationStringency.SILENT) {
                         final List<SAMValidationError> validationErrors = mNextRecord.isValid();
                         SAMUtils.processValidationErrors(validationErrors,
                                 this.samRecordIndex, BAMFileReader2.this.getValidationStringency());
                     }
                 }
                 if (eagerDecode && mNextRecord != null) {
                     mNextRecord.eagerDecode();
                 }
             } catch (IOException exc) {
                 throw new RuntimeException(exc.getMessage(), exc);
             }
         }
 
         /**
          * Read the next record from the input stream.
          */
         SAMRecord getNextRecord() throws IOException {
             return bamRecordCodec.decode();
         }
 
         /**
          * @return The record that will be return by the next call to next()
          */
         protected SAMRecord peek() {
             return mNextRecord;
         }
     }
 
     enum QueryType {CONTAINED, OVERLAPPING, STARTING_AT}
 
     /**
      * Creates an iterator over indexed data in the specified range.
      * @param sequence Sequence to which to constrain the data.
      * @param start Starting position within the above sequence to which the data should be constrained.
      * @param end Ending position within the above sequence to which the data should be constrained.s
      * @param queryType Type of query.  Useful for establishing the boundary rules.
      * @return An iterator over the requested data.
      */
     private CloseableIterator<SAMRecord> createIndexIterator(final String sequence,
                                                              final int start,
                                                              final int end,
                                                              final QueryType queryType) {
         long[] filePointers = null;
 
         // Hit the index to determine the chunk boundaries for the required data.
         final SAMFileHeader fileHeader = getFileHeader();
         int referenceIndex = fileHeader.getSequenceIndex(sequence);
         if (referenceIndex != -1) {
             final BAMFileIndex2 fileIndex = getFileIndex();
             filePointers = fileIndex.getFilePointersContaining(referenceIndex, start, end);
         }
 
         // Create an iterator over the above chunk boundaries.
         BAMFileIndexIterator iterator = new BAMFileIndexIterator(filePointers);
 
         // Add some preprocessing filters for edge-case reads that don't fit into this
         // query type.
         return new BAMQueryFilteringIterator(iterator,sequence,start,end,queryType);
     }
 
     private class BAMFileIndexIterator
         extends BAMFileIterator {
 
         private long[] mFilePointers = null;
         private int mFilePointerIndex = 0;
         private long mFilePointerLimit = -1;
 
         BAMFileIndexIterator(final long[] filePointers) {
             super(false);  // delay advance() until after construction
             mFilePointers = filePointers;
             advance();
         }
 
         SAMRecord getNextRecord()
             throws IOException {
             while (true) {
                 // Advance to next file block if necessary
                while (mCompressedInputStream.getFilePointer() >= mFilePointerLimit) {
                     if (mFilePointers == null ||
                         mFilePointerIndex >= mFilePointers.length) {
                         return null;
                     }
                     final long startOffset = mFilePointers[mFilePointerIndex++];
                     final long endOffset = mFilePointers[mFilePointerIndex++];
                     mCompressedInputStream.seek(startOffset);
                     mFilePointerLimit = endOffset;
                 }
                 // Pull next record from stream
                 return super.getNextRecord();
             }
         }
     }
 
     /**
      * A decorating iterator that filters out records that are outside the bounds of the
      * given query parameters.
      */
     private class BAMQueryFilteringIterator implements CloseableIterator<SAMRecord> {
         /**
          * The wrapped iterator.
          */
         private final CloseableIterator<SAMRecord> wrappedIterator;
 
         /**
          * The next record to be returned.  Will be null if no such record exists.
          */
         private SAMRecord nextRead;
 
         private final int mReferenceIndex;
         private final int mRegionStart;
         private final int mRegionEnd;
         private final QueryType mQueryType;        
 
         public BAMQueryFilteringIterator(final CloseableIterator<SAMRecord> iterator,final String sequence, final int start, final int end, final QueryType queryType) {
             this.wrappedIterator = iterator;
             final SAMFileHeader fileHeader = getFileHeader();
             mReferenceIndex = fileHeader.getSequenceIndex(sequence);
             mRegionStart = start;
             if (queryType == QueryType.STARTING_AT) {
                 mRegionEnd = mRegionStart;
             } else {
                 mRegionEnd = (end <= 0) ? Integer.MAX_VALUE : end;
             }
             mQueryType = queryType;
             advance();
         }
 
         /**
          * Returns true if a next element exists; false otherwise.
          */
         public boolean hasNext() {
             return nextRead != null;
         }
 
         /**
          * Gets the next record from the given iterator.
          * @return The next SAM record in the iterator.
          */
         public SAMRecord next() {
             if(!hasNext())
                 throw new NoSuchElementException("BAMQueryFilteringIterator: no next element available");
             final SAMRecord currentRead = nextRead;
             advance();
             return currentRead;
         }
 
         /**
          * Closes down the existing iterator.
          */
         public void close() {
             if (this != mCurrentIterator) {
                 throw new IllegalStateException("Attempt to close non-current iterator");
             }
             mCurrentIterator = null;
         }
 
         /**
          * @throws UnsupportedOperationException always.
          */
         public void remove() {
             throw new UnsupportedOperationException("Not supported: remove");
         }
 
         SAMRecord advance() {
             while (true) {
                 // Pull next record from stream
                 if(!wrappedIterator.hasNext())
                     return null;
 
                 final SAMRecord record = wrappedIterator.next();
                 // If beyond the end of this reference sequence, end iteration
                 final int referenceIndex = record.getReferenceIndex();
                 if (referenceIndex != mReferenceIndex) {
                     if (referenceIndex < 0 ||
                         referenceIndex > mReferenceIndex) {
                         return null;
                     }
                     // If before this reference sequence, continue
                     continue;
                 }
                 if (mRegionStart == 0 && mRegionEnd == Integer.MAX_VALUE) {
                     // Quick exit to avoid expensive alignment end calculation
                     return record;
                 }
                 final int alignmentStart = record.getAlignmentStart();
                 // If read is unmapped but has a coordinate, return it if the coordinate is within
                 // the query region, regardless of whether the mapped mate will be returned.
                 final int alignmentEnd;
                 if (mQueryType == QueryType.STARTING_AT) {
                     alignmentEnd = -1;
                 } else {
                     alignmentEnd = (record.getAlignmentEnd() != SAMRecord.NO_ALIGNMENT_START?
                             record.getAlignmentEnd(): alignmentStart);
                 }
 
                 if (alignmentStart > mRegionEnd) {
                     // If scanned beyond target region, end iteration
                     return null;
                 }
                 // Filter for overlap with region
                 if (mQueryType == QueryType.CONTAINED) {
                     if (alignmentStart >= mRegionStart && alignmentEnd <= mRegionEnd) {
                         return record;
                     }
                 } else if (mQueryType == QueryType.OVERLAPPING) {
                     if (alignmentEnd >= mRegionStart && alignmentStart <= mRegionEnd) {
                         return record;
                     }
                 } else {
                     if (alignmentStart == mRegionStart) {
                         return record;
                     }
                 }
             }
         }
     }
 
     private class BAMFileIndexUnmappedIterator extends BAMFileIterator  {
         private BAMFileIndexUnmappedIterator() {
             while (this.hasNext() && peek().getReferenceIndex() != SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
                 advance();
             }
         }
     }
 
 }
