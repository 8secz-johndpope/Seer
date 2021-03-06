 package org.rice.crosby.historytree.aggs;
 
 import org.rice.crosby.historytree.AggregationInterface;
 
 import com.google.protobuf.ByteString;
 
 public class ConcatAgg implements AggregationInterface<String, String> {
 
 	@Override
 	public String aggChildren(String leftAnn, String rightAnn) {
 		StringBuilder out = new StringBuilder();
 		out.append("[");
 		out.append(leftAnn);
 		out.append(",");
 		if (rightAnn != null)
 			out.append(rightAnn);
 		out.append("]");
 		return out.toString();
 	}
 
 	@Override
 	public String aggVal(String event) {
 		return event.substring(0,1);
 	}
 
 	@Override
 	public String getConfig() {
 		return "";
 	}
 
 	@Override
 	public String getName() {
 		return "ConcatAgg";
 	}
 
 	@Override
 	public String parseAgg(ByteString b) {
		return b.toStringUtf8();
 	}
 
 	@Override
 	public String parseVal(ByteString b) {
		return b.toStringUtf8();
 	}
 
 	@Override
 	public ByteString serializeAgg(String agg) {
 		return ByteString.copyFrom(agg.getBytes());
 	}
 
 	@Override
 	public ByteString serializeVal(String val) {
 		return ByteString.copyFrom(val.getBytes());
 	}
 
 	@Override
 	public AggregationInterface<String, String> setup(String config) {
 		return this;
 	}
 
 	@Override
 	public AggregationInterface<String, String> clone() {
 		// ConcatAgg is stateless, so just return this
 		return this;
 	}
 }
