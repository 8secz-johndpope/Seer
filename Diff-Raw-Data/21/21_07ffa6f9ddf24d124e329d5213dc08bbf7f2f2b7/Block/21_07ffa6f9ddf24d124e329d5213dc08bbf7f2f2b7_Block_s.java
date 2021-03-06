 package net.sf.jxls.tag;
 
 import net.sf.jxls.transformer.Sheet;
 import net.sf.jxls.formula.CellRef;
 
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Set;
 import java.util.HashSet;
 
 /**
  * Represents rectangular range of excel cells
  * @author Leonid Vysochyn
  */
 public class Block {
     int startRowNum;
     int endRowNum;
     short startCellNum;
     short endCellNum;
 
     Sheet sheet;
 
     Set affectedColumns = new HashSet();
 
     public Block(Sheet sheet, int startRowNum, int endRowNum) {
         this.startRowNum = startRowNum;
         this.endRowNum = endRowNum;
         this.startCellNum = -1;
         this.endCellNum = -1;
         this.sheet = sheet;
     }
 
     public Block(int startRowNum, short startCellNum, int endRowNum, short endCellNum) {
         this.startRowNum = startRowNum;
         this.startCellNum = startCellNum;
         this.endRowNum = endRowNum;
         this.endCellNum = endCellNum;
     }
 
     public Block(Sheet sheet, int startRowNum, short startCellNum, int endRowNum, short endCellNum) {
         this.sheet = sheet;
         this.startRowNum = startRowNum;
         this.startCellNum = startCellNum;
         this.endRowNum = endRowNum;
         this.endCellNum = endCellNum;
     }
 
     public void addAffectedColumn(short col){
         affectedColumns.add( new Short(col) );
     }
 
     public Block horizontalShift(short cellShift){
         startCellNum += cellShift;
         endCellNum += cellShift;
         return this;
     }
 
     public Block verticalShift(int rowShift){
         startRowNum += rowShift;
         endRowNum += rowShift;
         return this;
     }
 
     public short getStartCellNum() {
         return startCellNum;
     }
 
     public void setStartCellNum(short startCellNum) {
         this.startCellNum = startCellNum;
     }
 
     public short getEndCellNum() {
         return endCellNum;
     }
 
     public void setEndCellNum(short endCellNum) {
         this.endCellNum = endCellNum;
     }
 
     public int getStartRowNum() {
         return startRowNum;
     }
 
     public void setStartRowNum(int startRowNum) {
         this.startRowNum = startRowNum;
     }
 
     public int getEndRowNum() {
         return endRowNum;
     }
 
     public void setEndRowNum(int endRowNum) {
         this.endRowNum = endRowNum;
     }
 
     public int getNumberOfRows(){
         return endRowNum - startRowNum + 1;
     }
 
     public int getNumberOfColumns(){
         return endCellNum - startCellNum + 1;
     }
 
     public boolean contains(int rowNum, int cellNum){
         boolean flag = (startRowNum <= rowNum && rowNum <= endRowNum && ((startCellNum < 0 || endCellNum < 0) || (startCellNum <= cellNum && cellNum <= endCellNum)));
         if(flag && !affectedColumns.isEmpty()){
             return affectedColumns.contains( new Short( (short) cellNum) );
         }else{
             return flag;
         }
     }
 
     public boolean contains(Point p){
         boolean flag =  (startRowNum <= p.getRow() && p.getRow() <= endRowNum &&
                 ((startCellNum<0 || endCellNum<0) || (startCellNum <= p.getCol() && p.getCol() <= endCellNum)));
         if(flag && !affectedColumns.isEmpty()){
             return affectedColumns.contains( new Short( p.getCol() ) );
         }else{
             return flag;
         }
     }
 
     public boolean contains(CellRef cellRef){
        boolean flag =  (startRowNum <= cellRef.getRowNum() && cellRef.getRowNum() <= endRowNum &&
                 ((startCellNum<0 || endCellNum<0) || (startCellNum <= cellRef.getColNum() && cellRef.getColNum() <= endCellNum)));
         if(flag && !affectedColumns.isEmpty()){
             return affectedColumns.contains( new Short( cellRef.getColNum() ) );
         }else{
             return flag;
         }
     }
 
     public boolean isAbove(Point p){
         return (endRowNum < p.getRow());
     }
 
     public boolean isToLeft(Point p){
         return (endCellNum < p.getCol());
     }
 
     public boolean isAbove(int rowNum){
         return (endRowNum < rowNum);
     }
 
     public boolean isBelow(Point p){
         return (startRowNum > p.getRow());
     }
 
     public boolean isRowBlock(){
         return (startCellNum < 0 || endCellNum < 0 || (startCellNum > endCellNum) );
     }
 
     public boolean isColBlock(){
         return (startRowNum <0 || endRowNum < 0 || (startRowNum > endRowNum) );
     }
 
     public Sheet getSheet() {
         return sheet;
     }
 
     public void setSheet(Sheet sheet) {
         this.sheet = sheet;
     }
 
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
 
         final Block block = (Block) o;
 
         if (endCellNum != block.endCellNum) return false;
         if (endRowNum != block.endRowNum) return false;
         if (startCellNum != block.startCellNum) return false;
         if (startRowNum != block.startRowNum) return false;
         return !(sheet != null ? !sheet.equals(block.sheet) : block.sheet != null);
 
         }
 
     public int hashCode() {
         int result;
         result = startRowNum;
         result = 29 * result + endRowNum;
         result = 29 * result + (int) startCellNum;
         result = 29 * result + (int) endCellNum;
         result = 29 * result + (sheet != null ? sheet.hashCode() : 0);
         return result;
     }
 
     public String toString() {
         return "Block (" + startRowNum + ", " + startCellNum + ", " + endRowNum + ", " + endCellNum + ")";
     }
 }
