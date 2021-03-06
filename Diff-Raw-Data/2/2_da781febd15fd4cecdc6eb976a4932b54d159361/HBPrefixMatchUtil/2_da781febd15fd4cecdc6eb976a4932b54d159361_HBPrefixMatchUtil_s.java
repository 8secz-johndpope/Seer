 package nl.vu.datalayer.hbase.util;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Vector;
 
 import nl.vu.datalayer.hbase.HBaseConnection;
 import nl.vu.datalayer.hbase.bulkload.StringIdAssoc;
 import nl.vu.datalayer.hbase.id.BaseId;
 import nl.vu.datalayer.hbase.id.NumericalRangeException;
 import nl.vu.datalayer.hbase.id.TypedId;
 import nl.vu.datalayer.hbase.schema.HBPrefixMatchSchema;
 
 import org.apache.hadoop.hbase.client.Get;
 import org.apache.hadoop.hbase.client.HTable;
 import org.apache.hadoop.hbase.client.Result;
 import org.apache.hadoop.hbase.client.ResultScanner;
 import org.apache.hadoop.hbase.client.Scan;
 import org.apache.hadoop.hbase.filter.Filter;
 import org.apache.hadoop.hbase.filter.FilterList;
 import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
 import org.apache.hadoop.hbase.filter.PrefixFilter;
 import org.apache.hadoop.hbase.util.Bytes;
 import org.apache.hadoop.hdfs.util.ByteArray;
 import org.openrdf.model.Literal;
 import org.openrdf.model.Statement;
 import org.openrdf.model.impl.ValueFactoryImpl;
 import org.openrdf.rio.ntriples.NTriplesUtil;
 
 /**
  * Class that exposes operations with the tables in the PrefixMatch schema
  *
  */
 public class HBPrefixMatchUtil implements IHBaseUtil {
 	
 	private HBaseConnection con;
 	
 	/**
 	 * Maps the query patterns to the associated tables that resolve those patterns 
 	 */
 	private HashMap<String, Integer> pattern2Table;
 	
 	/**
 	 * Internal use: the index of the table used for retrieval
 	 */
 	private int tableIndex;
 	
 	/**
 	 * Variable storing time for the overhead of Id2String mappings upon retrieval
 	 */
 	private long id2StringOverhead = 0;
 	
 	/**
 	 * Variable storing time for the overhead of String2Id mappings 
 	 */
 	private long string2IdOverhead = 0;
 	
 	private HashMap<ByteArray, String> id2StringMap;
 	
 	private ArrayList<ArrayList<ByteArray>> quadResults;
 	
 	private ArrayList<String> boundElements;
 
 	public HBPrefixMatchUtil(HBaseConnection con) {
 		super();
 		this.con = con;
 		pattern2Table = new HashMap<String, Integer>(16);
 		buildHashMap();
 		id2StringMap = new HashMap<ByteArray, String>();
 		quadResults = new ArrayList<ArrayList<ByteArray>>();
 		boundElements = new ArrayList<String>();
 	}
 	
 	private void buildHashMap(){
 		pattern2Table.put("????", HBPrefixMatchSchema.SPOC);
 		pattern2Table.put("|???", HBPrefixMatchSchema.SPOC);
 		pattern2Table.put("||??", HBPrefixMatchSchema.SPOC);
 		pattern2Table.put("|||?", HBPrefixMatchSchema.SPOC);
 		pattern2Table.put("||||", HBPrefixMatchSchema.SPOC);
 		
 		pattern2Table.put("?|??", HBPrefixMatchSchema.POCS);
 		pattern2Table.put("?||?", HBPrefixMatchSchema.POCS);
 		pattern2Table.put("?|||", HBPrefixMatchSchema.POCS);
 		
 		pattern2Table.put("??|?", HBPrefixMatchSchema.OCSP);
 		pattern2Table.put("??||", HBPrefixMatchSchema.OCSP);
 		pattern2Table.put("|?||", HBPrefixMatchSchema.OCSP);
 		
 		pattern2Table.put("|?|?", HBPrefixMatchSchema.OSPC);
 		
 		pattern2Table.put("???|", HBPrefixMatchSchema.CSPO);
 		pattern2Table.put("|??|", HBPrefixMatchSchema.CSPO);
 		pattern2Table.put("||?|", HBPrefixMatchSchema.CSPO);
 		
 		pattern2Table.put("?|?|", HBPrefixMatchSchema.CPSO);
 	}
 
 	@Override
 	public ArrayList<ArrayList<String>> getRow(String[] quad)
 			throws IOException {//we expect the pattern in the order SPOC
 		
 		try {
 			id2StringMap.clear();
 			quadResults.clear();
 			boundElements.clear();
 			id2StringOverhead = 0;
 			byte[] startKey = buildKey(quad);
 			if (startKey == null) {
 				return new ArrayList<ArrayList<String>>();
 			}
 
 			//start search in the quad tables
 			long startSearch = System.currentTimeMillis();
 			Filter prefixFilter = new PrefixFilter(startKey);
 			Filter keyOnlyFilter = new KeyOnlyFilter();
 			Filter filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL, prefixFilter, keyOnlyFilter);
 			
 			Scan scan = new Scan(startKey, filterList);
 			scan.setCaching(500);
 
 			String tableName = HBPrefixMatchSchema.TABLE_NAMES[tableIndex];
 			HTable table = new HTable(con.getConfiguration(), tableName);
 			ResultScanner results = table.getScanner(scan);
 
 			Result r = null;
 			int sizeOfInterest = HBPrefixMatchSchema.KEY_LENGTH - startKey.length;
 			HTable id2StringTable = new HTable(con.getConfiguration(), HBPrefixMatchSchema.ID2STRING);
 
 			ArrayList<Get> batchGets = new ArrayList<Get>();
 			while ((r = results.next()) != null) {
 				parseKey(r.getRow(), startKey.length, sizeOfInterest, batchGets);
 			}
 			results.close();
 			long searchTime = System.currentTimeMillis() - startSearch;
 
 			long start = System.currentTimeMillis();
 			Result []id2StringResults = id2StringTable.get(batchGets);
 			id2StringOverhead = System.currentTimeMillis()-start;
 			
 			System.out.println("Search time: "+searchTime+"; Id2StringOverhead: "+id2StringOverhead+"; String2IdOverhead: "+string2IdOverhead);
 			
 			//update the internal mapping between ids and strings
 			for (Result result : id2StringResults) {
 				byte []rowVal = result.getValue(HBPrefixMatchSchema.COLUMN_FAMILY, HBPrefixMatchSchema.COLUMN_NAME);
 				byte []rowKey = result.getRow();
 				if (rowVal == null || rowKey == null){
 					System.err.println("Id not found: "+(rowKey == null ? null : hexaString(rowKey)));
 				}
 				else{
 					id2StringMap.put(new ByteArray(rowKey), new String(rowVal));
 				}
 			}			
 			
 			int queryElemNo = boundElements.size();
 			// the quads using strings
 			ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
 			
 			for (ArrayList<ByteArray> quadList : quadResults) {
 				//ArrayList<String> newList = new ArrayList<String>(quadList.size());
 				
 				ArrayList<String> newQuadResult = new ArrayList<String>(4);
 				newQuadResult.addAll(Arrays.asList(new String[]{"", "", "", ""}));			
 				for (int i = 0; i < queryElemNo; i++) {
 					newQuadResult.set(HBPrefixMatchSchema.TO_SPOC_ORDER[tableIndex][i], boundElements.get(i));
 				}
 				
 				for (int i = 0; i < quadList.size(); i++) {
 					if ((i+queryElemNo) == HBPrefixMatchSchema.ORDER[tableIndex][2] && 
 							TypedId.getType(quadList.get(i).getBytes()[0]) == TypedId.NUMERICAL){
 						//handle numerical
 						TypedId id = new TypedId(quadList.get(i).getBytes());
 						newQuadResult.set(2, id.toString());
 					}
 					else{
 						newQuadResult.set(HBPrefixMatchSchema.TO_SPOC_ORDER[tableIndex][(i+queryElemNo)], id2StringMap.get(quadList.get(i)));
 					}
 				}
 				
 				ret.add(new ArrayList<String>(newQuadResult));
 			}
 			
 			return ret;
 			
 		} catch (NumericalRangeException e) {
 			e.printStackTrace();
 			return null;
 		}
 	}
 	
 	public static String hexaString(byte []b){
 		String ret = "";
 		for (int i = 0; i < b.length; i++) {
 			ret += String.format("\\x%02x", b[i]);
 		}
 		return ret;
 	}
 	
 	public byte []retrieveId(String s) throws IOException{
 		byte []sBytes = s.getBytes();
 		byte []key = StringIdAssoc.reverseBytes(sBytes, sBytes.length);
 		
 		Get g = new Get(key);
 		g.addColumn(HBPrefixMatchSchema.COLUMN_FAMILY, HBPrefixMatchSchema.COLUMN_NAME);
 		
 		HTable table = new HTable(con.getConfiguration(), HBPrefixMatchSchema.STRING2ID);
 		Result r = table.get(g);
 		byte []id = r.getValue(HBPrefixMatchSchema.COLUMN_FAMILY, HBPrefixMatchSchema.COLUMN_NAME);
 		if (id == null){
 			System.err.println("Id does not exist for: "+s);
 		}
 		
 		return id;
 	}
 	
 	private void parseKey(byte []key, int startIndex, int sizeOfInterest, ArrayList<Get> batchGets) throws IOException{
 		
 		int elemNo = sizeOfInterest/BaseId.SIZE;
 		ArrayList<ByteArray> currentQuad = new ArrayList<ByteArray>(elemNo);
 		
 		int crtIndex = startIndex;
 		for (int i = 0; i < elemNo; i++) {
 			int length;
 			byte [] elemKey;
 			if (crtIndex == HBPrefixMatchSchema.OFFSETS[tableIndex][2]){//for the Object position
 				length = TypedId.SIZE;
 				if (TypedId.getType(key[crtIndex]) == TypedId.STRING){
 					elemKey = new byte[BaseId.SIZE];
 					System.arraycopy(key, crtIndex+1, elemKey, 0, BaseId.SIZE);	
 				}
 				else{//numericals
 					elemKey = new byte[TypedId.SIZE];
 					System.arraycopy(key, crtIndex, elemKey, 0, TypedId.SIZE);
 					crtIndex += length;
 					ByteArray newElem = new ByteArray(elemKey);
 					currentQuad.add(newElem);
 					continue;
 				}
 			}
 			else{//for non-Object positions
 				length = BaseId.SIZE;
 				elemKey = new byte[length];
 				System.arraycopy(key, crtIndex, elemKey, 0, length);
 			}
 			
 			ByteArray newElem = new ByteArray(elemKey);
 			currentQuad.add(newElem);
 			
 			if (id2StringMap.get(newElem) == null){
 				id2StringMap.put(newElem, "");
 				Get g = new Get(elemKey);
 				g.addColumn(HBPrefixMatchSchema.COLUMN_FAMILY, HBPrefixMatchSchema.COLUMN_NAME);
 				batchGets.add(g);
 			}
 			
 			crtIndex += length;
 		}
 		
 		quadResults.add(currentQuad);
 	}
 	
 	private byte []buildKey(String []quad) throws IOException, NumericalRangeException
 	{
 		String pattern = "";
 		int keySize = 0;
 		
 		ArrayList<Get> string2Ids = new ArrayList<Get>();
 		ArrayList<Integer> offsets = new ArrayList<Integer>();
 		byte []numerical = null;
 		for (int i = 0; i < quad.length; i++) {
 			if (quad[i].equals("?"))
 				pattern += "?";
 			else{
 				pattern += "|";
 				
 				byte []sBytes;
 				if (i != 2){//not Object
 					keySize += BaseId.SIZE;
 					boundElements.add(quad[i]);
 					sBytes = quad[i].getBytes();
 				}
 				else{
 					keySize += TypedId.SIZE;
 					if (quad[i].startsWith("\"")){//literal
 						Literal l = NTriplesUtil.parseLiteral(quad[i], new ValueFactoryImpl());
 						if (l.getDatatype() != null){
 							TypedId id = TypedId.createNumerical(l);
 							if (id != null){
 								numerical = id.getBytes();
								boundElements.add(numerical.toString());
 								continue;
 							}
 						}
 						String lString = l.toString();
 						boundElements.add(lString);
 						sBytes = lString.getBytes();					
 					}
 					else{
 						boundElements.add(quad[i]);
 						sBytes = quad[i].getBytes();
 					}
 				}
 				
 				byte []reverseString = StringIdAssoc.reverseBytes(sBytes, sBytes.length);
 				Get g = new Get(reverseString);
 				g.addColumn(HBPrefixMatchSchema.COLUMN_FAMILY, HBPrefixMatchSchema.COLUMN_NAME);
 				string2Ids.add(g);
 				offsets.add(i);
 			}
 		}
 		
 		tableIndex = pattern2Table.get(pattern);
 		
 		HTable table = new HTable(con.getConfiguration(), HBPrefixMatchSchema.STRING2ID);
 		
 		byte []key = new byte[keySize];
 		for (int i=0; i<string2Ids.size(); i++) {
 			Get get = string2Ids.get(i);
 			
 			long start = System.currentTimeMillis();
 			Result result = table.get(get);
 			byte []value = result.getValue(HBPrefixMatchSchema.COLUMN_FAMILY, HBPrefixMatchSchema.COLUMN_NAME);
 			string2IdOverhead += System.currentTimeMillis()-start;
 			
 			if (value == null){
 				byte []rowKey = result.getRow();
 				System.err.println("Quad element could not be found "+(rowKey == null ? null : hexaString(rowKey)));
 				return null;
 			}
 			
 			int spocIndex = offsets.get(i);
 			int offset = HBPrefixMatchSchema.OFFSETS[tableIndex][spocIndex];
 			
 			if (spocIndex == 2)
 				Bytes.putBytes(key, offset+1, value, 0, value.length);
 			else
 				Bytes.putBytes(key, offset, value, 0, value.length);
 		}	
 		if (numerical != null){
 			Bytes.putBytes(key, HBPrefixMatchSchema.OFFSETS[tableIndex][2], numerical, 0, numerical.length);
 		}
 		
 		return key;
 	}
 
 	@Override
 	public String getRawCellValue(String subject, String predicate,
 			String object) throws IOException {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public void populateTables(ArrayList<Statement> statements)
 			throws Exception {
 		// TODO Auto-generated method stub
 
 	}
 
 }
