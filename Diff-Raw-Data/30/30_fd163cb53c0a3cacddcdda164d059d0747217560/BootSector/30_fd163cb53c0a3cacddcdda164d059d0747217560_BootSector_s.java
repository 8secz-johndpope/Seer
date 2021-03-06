 /*
  * $Id: BootSector.java 4975 2009-02-02 08:30:52Z lsantha $
  *
  * Copyright (C) 2003-2009 JNode.org
  *
  * This library is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published
  * by the Free Software Foundation; either version 2.1 of the License, or
  * (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful, but 
  * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this library; If not, write to the Free Software Foundation, Inc., 
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */
  
 package com.meetwise.fs.fat;
 
 import com.meetwise.fs.BlockDevice;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import com.meetwise.fs.util.Geometry;
 import com.meetwise.fs.util.Geometry.GeometryException;
 import com.meetwise.fs.partitions.IBMPartitionTable;
 import com.meetwise.fs.partitions.IBMPartitionTableEntry;
 import com.meetwise.fs.partitions.IBMPartitionTypes;
 
 /**
  * The boot sector.
  *
  * @author Ewout Prangsma &lt;epr at jnode.org&gt;
  * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
  */
 public final class BootSector extends Sector {
 
     /**
      * The maximum number of sectors for a FAT12 file system. This is actually
      * the number of sectors where mkdosfs stop complaining about a FAT16
      * partition having not enough sectors, so it would be misinterpreted
      * as FAT12 without special handling.
      *
      * @see #getNrLogicalSectors() 
      */
     public static final int MAX_FAT12_SECTORS = 8202;
 
     private final IBMPartitionTableEntry[] partitions;
 
     public BootSector(int size) {
         super(size);
         
         partitions = new IBMPartitionTableEntry[4];
     }
 
     public BootSector(byte[] src) {
         super(src.length);
         
         System.arraycopy(src, 0, data, 0, src.length);
         partitions = new IBMPartitionTableEntry[4];
     }
     
     public FatType getFatType() throws IOException {
         if (getSectorsPerFat16() == 0) return FatType.FAT32;
         else {
            return (getNrLogicalSectors() <= MAX_FAT12_SECTORS) ?
                 FatType.FAT12 : FatType.FAT16;
         }
     }
 
     public boolean isaValidBootSector() {
         return IBMPartitionTable.containsPartitionTable(data);
     }
 
     /**
      * Read the contents of this bootsector from the given device.
      * 
      * @param device
      * @throws IOException on read error
      */
     public synchronized void read(BlockDevice device) throws IOException {
         device.read(0, ByteBuffer.wrap(data));
 
         dirty = false;
     }
 
     /**
      * Write the contents of this bootsector to the given device.
      * 
      * @param device
      * @throws IOException on write error
      */
     public synchronized void write(BlockDevice device) throws IOException {
         device.write(0, ByteBuffer.wrap(data));
         dirty = false;
     }
 
     /**
      * Gets the OEM name
      * 
      * @return String
      */
     public String getOemName() {
         StringBuilder b = new StringBuilder(8);
         
         for (int i = 0; i < 8; i++) {
             int v = data[0x3 + i];
             if (v == 0) break;
             b.append((char) v);
         }
         
         return b.toString();
     }
 
     /**
      * Sets the FAT type for this boot sector. This method updates the string
      * found at offset 0x36 in the boot sector.
      *
      * @param type the new FAT type
      */
     public void setFatType(FatType type) {
         
         for (int i = 0; i < 8; i++) {
             char ch;
             if (i < type.getLabel().length()) {
                 ch = type.getLabel().charAt(i);
             } else {
                 ch = (char) 0;
             }
             
             set8(0x36 + i, ch);
         }
     }
 
     /**
      * Sets the OEM name, must be at most 8 characters long.
      * 
      * @param name the new OEM name
      */
     public void setOemName(String name) {
         if (name.length() > 8) throw new IllegalArgumentException();
         
         for (int i = 0; i < 8; i++) {
             char ch;
             if (i < name.length()) {
                 ch = name.charAt(i);
             } else {
                 ch = (char) 0;
             }
             
             set8(0x3 + i, ch);
         }
     }
 
     /**
      * Gets the number of bytes/sector
      * 
      * @return int
      */
     public int getBytesPerSector() {
         return get16(0x0b);
     }
 
     /**
      * Sets the number of bytes/sector
      * 
      * @param v the new value for bytes per sector
      */
     public void setBytesPerSector(int v) {
         if (v == getBytesPerSector()) return;
         
         set16(0x0b, v);
     }
 
     /**
      * Gets the number of sectors/cluster
      * 
      * @return int
      */
     public int getSectorsPerCluster() {
         return get8(0x0d);
     }
 
     /**
      * Sets the number of sectors/cluster
      *
      * @param v the new number of sectors per cluster
      */
     public void setSectorsPerCluster(int v) {
         if (v == getSectorsPerCluster()) return;
         
         set8(0x0d, v);
     }
     
     /**
      * Gets the number of reserved (for bootrecord) sectors
      * 
      * @return int
      */
     public int getNrReservedSectors() {
         return get16(0xe);
     }
 
     /**
      * Sets the number of reserved (for bootrecord) sectors
      * 
      * @param v the new number of reserved sectors
      */
     public void setNrReservedSectors(int v) {
         if (v == getNrReservedSectors()) return;
         
         set16(0xe, v);
     }
 
     /**
      * Gets the number of fats
      * 
      * @return int
      */
     public int getNrFats() {
         return get8(0x10);
     }
 
     /**
      * Sets the number of fats
      *
      * @param v the new number of fats
      */
     public void setNrFats(int v) {
         if (v == getNrFats()) return;
         
         set8(0x10, v);
     }
 
     /**
      * Gets the number of entries in the root directory
      * 
      * @return int
      */
     public int getNrRootDirEntries() {
         return get16(0x11);
     }
 
     /**
      * Sets the number of entries in the root directory
      * 
      * @param v the new number of entries in the root directory
      */
     public void setNrRootDirEntries(int v) {
         if (v == getNrRootDirEntries()) return;
         
         set16(0x11, v);
     }
 
     public long getRootDirFirstCluster() {
         return get32(0x2c);
     }
     
     /**
      * Gets the number of logical sectors
      * 
      * @return int
      */
     private int getNrLogicalSectors() {
         return get16(0x13);
     }
 
     public int getFsInfoSectorOffset() {
         return get16(0x30);
     }
     
     /**
      * Sets the number of logical sectors
      * 
      * @param v the new number of logical sectors
      */
     private void setNrLogicalSectors(int v) {
         if (v == getNrLogicalSectors()) return;
         
         set16(0x13, v);
     }
 
     public void setSectorCount(long count) {
         if (count > 65535) {
             setNrLogicalSectors(0);
             setNrTotalSectors(count);
         } else {
             setNrLogicalSectors((int) count);
             setNrTotalSectors(count);
         }
     }
     
     public long getSectorCount() {
         if (getNrLogicalSectors() == 0) return getNrTotalSectors();
         else return getNrLogicalSectors();
     }
     
     private void setNrTotalSectors(long v) {
         set32(0x20, v);
     }
     
     private long getNrTotalSectors() {
         return get32(0x20);
     }
 
     /**
      * Gets the medium descriptor byte
      * 
      * @return int
      */
     public int getMediumDescriptor() {
         return get8(0x15);
     }
 
     /**
      * Sets the medium descriptor byte
      * 
      * @param v the new medium descriptor
      */
     public void setMediumDescriptor(int v) {
         set8(0x15, v);
     }
     
     public int getSectorsPerFat() throws IOException {
         if (getFatType() == FatType.FAT32) {
             final long spf = getSectorsPerFat32();
             if (spf > Integer.MAX_VALUE) throw new AssertionError();
             return (int) spf;
         } else {
             return getSectorsPerFat16();
         }
     }
 
     /**
      * Gets the number of sectors/fat for FAT 12/16.
      * 
      * @return int
      */
     public int getSectorsPerFat16() {
         return get16(0x16);
     }
     
     public void setSectorsPerFat32(int v) {
         set32(0x24, v);
     }
 
     public long getSectorsPerFat32() {
         return get32(0x24);
     }
     
     /**
      * Sets the number of sectors/fat
      * 
      * @param v  the new number of sectors per fat
      * @throws IOException 
      */
     public void setSectorsPerFat(int v) throws IOException {
         if (v == getSectorsPerFat()) return;
         
         set16(0x16, v);
     }
     
     /**
      * Gets the number of sectors/track
      * 
      * @return int
      */
     public int getSectorsPerTrack() {
         return get16(0x18);
     }
 
     /**
      * Sets the number of sectors/track
      *
      * @param v the new number of sectors per track
      */
     public void setSectorsPerTrack(int v) {
         if (v == getSectorsPerTrack()) return;
         
         set16(0x18, v);
     }
 
     /**
      * Gets the number of heads
      * 
      * @return int
      */
     public int getNrHeads() {
         return get16(0x1a);
     }
 
     /**
      * Sets the number of heads
      * 
      * @param v the new number of heads
      */
     public void setNrHeads(int v) {
         if (v == getNrHeads()) return;
         
         set16(0x1a, v);
     }
 
     /**
      * Gets the number of hidden sectors
      * 
      * @return int
      */
     public int getNrHiddenSectors() {
         return get16(0x1c);
     }
 
     /**
      * Sets the number of hidden sectors
      *
      * @param v the new number of hidden sectors
      */
     public void setNrHiddenSectors(int v) {
         if (v == getNrHiddenSectors()) return;
         
         set16(0x1c, v);
     }
 
     /**
      * Returns the dirty.
      * 
      * @return boolean
      */
     public boolean isDirty() {
         return dirty;
     }
 
     public int getNbPartitions() {
         return partitions.length;
     }
 
     public IBMPartitionTableEntry initPartitions(Geometry geom, IBMPartitionTypes firstPartitionType)
         throws GeometryException {
         getPartition(0).clear();
         getPartition(1).clear();
         getPartition(2).clear();
         getPartition(3).clear();
 
         IBMPartitionTableEntry entry = getPartition(0);
         entry.setBootIndicator(true);
         entry.setStartLba(1);
         entry.setNrSectors(geom.getTotalSectors() - 1);
         entry.setSystemIndicator(firstPartitionType);
         entry.setStartCHS(geom.getCHS(entry.getStartLba()));
         entry.setEndCHS(geom.getCHS(entry.getStartLba() + entry.getNrSectors() - 1));
 
         return entry;
     }
 
     public synchronized IBMPartitionTableEntry getPartition(int partNr) {
         if (partitions[partNr] == null) {
             partitions[partNr] = new IBMPartitionTableEntry(null, data, partNr);
         }
         return partitions[partNr];
     }
 
     @Override
     public String toString() {
         StringBuilder res = new StringBuilder(1024);
         res.append("Bootsector :\n");
         res.append("oemName=");
         res.append(getOemName());
         res.append('\n');
         res.append("medium descriptor = ");
         res.append(getMediumDescriptor());
         res.append('\n');
         res.append("Nr heads = ");
         res.append(getNrHeads());
         res.append('\n');
         res.append("Sectors per track = ");
         res.append(getSectorsPerTrack());
         res.append('\n');
         res.append("Sector per cluster = ");
         res.append(getSectorsPerCluster());
         res.append('\n');
         res.append("byte per sector = ");
         res.append(getBytesPerSector());
         res.append('\n');
         res.append("Nr fats = ");
         res.append(getNrFats());
         res.append('\n');
         res.append("Nr hidden sectors = ");
         res.append(getNrHiddenSectors());
         res.append('\n');
         res.append("Nr logical sectors = ");
         res.append(getNrLogicalSectors());
         res.append('\n');
         res.append("Nr reserved sector = ");
         res.append(getNrReservedSectors());
         res.append('\n');
         res.append("Nr Root Dir Entries = ");
         res.append(getNrRootDirEntries());
         res.append('\n');
         
         return res.toString();
     }
 }
