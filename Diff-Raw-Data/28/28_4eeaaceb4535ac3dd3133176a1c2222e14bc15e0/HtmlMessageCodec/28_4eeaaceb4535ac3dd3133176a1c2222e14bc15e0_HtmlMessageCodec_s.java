 package com.dianping.cat.message.spi.codec;
 
 import java.io.UnsupportedEncodingException;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.List;
 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.BlockingQueue;
 
 import org.jboss.netty.buffer.ChannelBuffer;
 
 import com.dianping.cat.message.Event;
 import com.dianping.cat.message.Heartbeat;
 import com.dianping.cat.message.Message;
 import com.dianping.cat.message.Transaction;
 import com.dianping.cat.message.spi.MessageCodec;
 import com.dianping.cat.message.spi.MessageTree;
 import com.dianping.cat.message.spi.StringRope;
 import com.site.lookup.annotation.Inject;
 
 /**
  * Local use only, do not use it over network since it only supports one-way
  * encoding
  */
 public class HtmlMessageCodec implements MessageCodec {
 	private static final String ID = "HT1"; // HTML version 1
 
 	@Inject
 	private BufferWriter m_writer;
 
 	@Inject
 	private String m_logViewPrefix;
 
 	private BufferHelper m_bufferHelper = new BufferHelper(m_writer);
 
 	private DateHelper m_dateHelper = new DateHelper();
 
 	@Override
 	public void decode(ChannelBuffer buf, MessageTree tree) {
 		throw new UnsupportedOperationException("HtmlMessageCodec only supported in one-way right now!");
 	}
 
 	@Override
 	public void encode(MessageTree tree, ChannelBuffer buf) {
 		int count = 0;
 		int index = buf.writerIndex();
 		BufferHelper helper = m_bufferHelper;
 
 		buf.writeInt(0); // place-holder
 
 		count += helper.table1(buf);
 		count += helper.crlf(buf);
 		// count += encodeHeader(tree, buf);
 
 		if (tree.getMessage() != null) {
 			count += encodeMessage(tree.getMessage(), buf, 0, new LineCounter());
 		}
 
 		count += helper.table2(buf);
 		buf.setInt(index, count);
 	}
 
 	protected int encodeHeader(MessageTree tree, ChannelBuffer buf) {
 		BufferHelper helper = m_bufferHelper;
 		int count = 0;
 
 		count += helper.tr1(buf, null);
 		count += helper.td(buf, ID);
 		count += helper.td(buf, tree.getDomain());
 		count += helper.td(buf, tree.getHostName());
 		count += helper.td(buf, tree.getIpAddress());
 		count += helper.td(buf, tree.getThreadId());
 		count += helper.td(buf, tree.getThreadName());
 		count += helper.td(buf, tree.getMessageId());
 		count += helper.td(buf, tree.getParentMessageId());
 		count += helper.td(buf, tree.getRootMessageId());
 		count += helper.td(buf, tree.getSessionToken());
 		count += helper.tr2(buf);
 		count += helper.crlf(buf);
 
 		return count;
 	}
 
 	protected int encodeLine(Message message, ChannelBuffer buf, char type, Policy policy, int level, LineCounter counter) {
 		BufferHelper helper = m_bufferHelper;
 		int count = 0;
 
 		if (counter != null) {
 			counter.inc();
 
 			count += helper.tr1(buf, counter.getCount() % 2 != 0 ? "odd" : "even");
 		} else {
 			count += helper.tr1(buf, null);
 		}
 
 		count += helper.td1(buf);
 
 		count += helper.nbsp(buf, level * 2); // 2 spaces per level
 		count += helper.write(buf, (byte) type);
 
 		if (type == 'T' && message instanceof Transaction) {
 			long duration = ((Transaction) message).getDuration();
 
 			count += helper.write(buf, m_dateHelper.format(message.getTimestamp() + duration));
 		} else {
 			count += helper.write(buf, m_dateHelper.format(message.getTimestamp()));
 		}
 
 		count += helper.td2(buf);
 
 		count += helper.td(buf, message.getType());
 		count += helper.td(buf, message.getName());
 
 		if (policy != Policy.WITHOUT_STATUS) {
 			if (Message.SUCCESS.equals(message.getStatus())) {
 				count += helper.td(buf, message.getStatus());
 			} else {
 				count += helper.td(buf, message.getStatus(), "class=\"error\"");
 			}
 
 			Object data = message.getData();
 
 			count += helper.td1(buf);
 
 			if (policy == Policy.WITH_DURATION && message instanceof Transaction) {
 				long duration = ((Transaction) message).getDuration();
 
 				count += helper.write(buf, String.valueOf(duration));
 				count += helper.write(buf, "ms ");
 			}
 
 			if (data instanceof StringRope) {
 				StringRope rope = (StringRope) data;
 
 				count += rope.writeTo(buf, m_writer);
 			} else {
 				count += helper.writeRaw(buf, String.valueOf(data));
 			}
 
 			count += helper.td2(buf);
 		} else {
 			count += helper.td(buf, "");
 			count += helper.td(buf, "");
 		}
 
 		count += helper.tr2(buf);
 		count += helper.crlf(buf);
 
 		return count;
 	}
 
 	protected int encodeLogViewLink(Message message, ChannelBuffer buf, int level, LineCounter counter) {
 		BufferHelper helper = m_bufferHelper;
 		int count = 0;
 
 		if (counter != null) {
 			counter.inc();
 
 			count += helper.tr1(buf, counter.getCount() % 2 != 0 ? "odd" : "even");
 		} else {
 			count += helper.tr1(buf, null);
 		}
 
		count += helper.td1(buf, "colspan=\"5\"");
 
 		count += helper.nbsp(buf, level * 2); // 2 spaces per level
		count += helper.write(buf, "<a href=\"");
		count += helper.write(buf, m_logViewPrefix);
		count += helper.write(buf, message.getData().toString());
		count += helper.write(buf, "\">[:: show ::]</a>");
 		count += helper.td2(buf);
 
 		count += helper.tr2(buf);
 		count += helper.crlf(buf);
 
 		return count;
 	}
 
 	public int encodeMessage(Message message, ChannelBuffer buf, int level, LineCounter counter) {
 		if (message instanceof Event) {
 			String type = message.getType();
 
 			if ("RemoteCall".equals(type)) {
 				return encodeLogViewLink(message, buf, level, counter);
 			} else {
 				return encodeLine(message, buf, 'E', Policy.DEFAULT, level, counter);
 			}
 		} else if (message instanceof Transaction) {
 			Transaction transaction = (Transaction) message;
 			List<Message> children = transaction.getChildren();
 
 			if (children.isEmpty()) {
 				if (transaction.getDuration() < 0) {
 					return encodeLine(transaction, buf, 't', Policy.WITHOUT_STATUS, level, counter);
 				} else {
 					return encodeLine(transaction, buf, 'A', Policy.WITH_DURATION, level, counter);
 				}
 			} else {
 				int count = 0;
 
 				count += encodeLine(transaction, buf, 't', Policy.WITHOUT_STATUS, level, counter);
 
 				for (Message child : children) {
 					count += encodeMessage(child, buf, level + 1, counter);
 				}
 
 				count += encodeLine(transaction, buf, 'T', Policy.WITH_DURATION, level, counter);
 
 				return count;
 			}
 		} else if (message instanceof Heartbeat) {
 			return encodeLine(message, buf, 'H', Policy.DEFAULT, level, counter);
 		} else {
 			throw new RuntimeException(String.format("Unsupported message type: %s.", message.getClass()));
 		}
 	}
 
 	public void setBufferWriter(BufferWriter writer) {
 		m_writer = writer;
 		m_bufferHelper = new BufferHelper(m_writer);
 	}
 
 	public void setLogViewPrefix(String logViewPrefix) {
 		m_logViewPrefix = logViewPrefix;
 	}
 
 	protected static class BufferHelper {
 		private static byte[] TABLE1 = "<table class=\"logview\">".getBytes();
 
 		private static byte[] TABLE2 = "</table>".getBytes();
 
 		private static byte[] TR1 = "<tr>".getBytes();
 
 		private static byte[] TR2 = "</tr>".getBytes();
 
 		private static byte[] TD1 = "<td>".getBytes();
 
 		private static byte[] TD2 = "</td>".getBytes();
 
 		private static byte[] NBSP = "&nbsp;".getBytes();
 
 		private static byte[] CRLF = "\r\n".getBytes();
 
 		private BufferWriter m_writer;
 
 		public BufferHelper(BufferWriter writer) {
 			m_writer = writer;
 		}
 
 		public int crlf(ChannelBuffer buf) {
 			buf.writeBytes(CRLF);
 			return CRLF.length;
 		}
 
 		public int nbsp(ChannelBuffer buf, int count) {
 			for (int i = 0; i < count; i++) {
 				buf.writeBytes(NBSP);
 			}
 
 			return count * NBSP.length;
 		}
 
 		public int table1(ChannelBuffer buf) {
 			buf.writeBytes(TABLE1);
 			return TABLE1.length;
 		}
 
 		public int table2(ChannelBuffer buf) {
 			buf.writeBytes(TABLE2);
 			return TABLE2.length;
 		}
 
 		public int td(ChannelBuffer buf, String str) {
 			return td(buf, str, null);
 		}
 
 		public int td(ChannelBuffer buf, String str, String attributes) {
 			if (str == null) {
 				str = "null";
 			}
 
 			byte[] data = str.getBytes();
 			int count = 0;
 
 			if (attributes == null) {
 				buf.writeBytes(TD1);
 				count += TD1.length;
 			} else {
 				String tag = "<td " + attributes + ">";
 				byte[] bytes = tag.getBytes();
 
 				buf.writeBytes(bytes);
 				count += bytes.length;
 			}
 
 			buf.writeBytes(data);
 			count += data.length;
 
 			buf.writeBytes(TD2);
 			count += TD2.length;
 
 			return count;
 		}
 
 		public int td1(ChannelBuffer buf) {
 			buf.writeBytes(TD1);
 			return TD1.length;
 		}
 
 		public int td1(ChannelBuffer buf, String attributes) {
 			if (attributes == null) {
 				buf.writeBytes(TD1);
 				return TD1.length;
 			} else {
 				String tag = "<td " + attributes + ">";
 				byte[] bytes = tag.getBytes();
 
 				buf.writeBytes(bytes);
 				return bytes.length;
 			}
 		}
 
 		public int td2(ChannelBuffer buf) {
 			buf.writeBytes(TD2);
 			return TD2.length;
 		}
 
 		public int tr1(ChannelBuffer buf, String styleClass) {
 			if (styleClass == null) {
 				buf.writeBytes(TR1);
 				return TR1.length;
 			} else {
 				String tag = "<tr class=\"" + styleClass + "\">";
 				byte[] bytes = tag.getBytes();
 
 				buf.writeBytes(bytes);
 				return bytes.length;
 			}
 		}
 
 		public int tr2(ChannelBuffer buf) {
 			buf.writeBytes(TR2);
 			return TR2.length;
 		}
 
 		public int write(ChannelBuffer buf, byte b) {
 			buf.writeByte(b);
 			return 1;
 		}
 
 		public int write(ChannelBuffer buf, String str) {
 			if (str == null) {
 				str = "null";
 			}
 
 			byte[] data = str.getBytes();
 
 			buf.writeBytes(data);
 			return data.length;
 		}
 
 		public int writeRaw(ChannelBuffer buf, String str) {
 			if (str == null) {
 				str = "null";
 			}
 
 			byte[] data;
 
 			try {
 				data = str.getBytes("utf-8");
 			} catch (UnsupportedEncodingException e) {
 				data = str.getBytes();
 			}
 
 			return m_writer.writeTo(buf, data);
 		}
 	}
 
 	/**
 	 * Thread safe date helper class. DateFormat is NOT thread safe.
 	 */
 	protected static class DateHelper {
 		private static final String DATE_PATTERN = "HH:mm:ss.SSS";
 
 		private BlockingQueue<SimpleDateFormat> m_queue = new ArrayBlockingQueue<SimpleDateFormat>(20);
 
 		public String format(long timestamp) {
 			SimpleDateFormat format = m_queue.poll();
 
 			if (format == null) {
 				format = new SimpleDateFormat(DATE_PATTERN);
 			}
 
 			try {
 				return format.format(new Date(timestamp));
 			} finally {
 				if (m_queue.remainingCapacity() > 0) {
 					m_queue.offer(format);
 				}
 			}
 		}
 
 		public long parse(String str) {
 			SimpleDateFormat format = m_queue.poll();
 
 			if (format == null) {
 				format = new SimpleDateFormat(DATE_PATTERN);
 			}
 
 			try {
 				return format.parse(str).getTime();
 			} catch (ParseException e) {
 				return -1;
 			} finally {
 				if (m_queue.remainingCapacity() > 0) {
 					m_queue.offer(format);
 				}
 			}
 		}
 	}
 
 	protected static class LineCounter {
 		private int m_count;
 
 		public int getCount() {
 			return m_count;
 		}
 
 		public void inc() {
 			m_count++;
 		}
 	}
 
 	protected static enum Policy {
 		DEFAULT,
 
 		WITHOUT_STATUS,
 
 		WITH_DURATION;
 
 		public static Policy getByMessageIdentifier(byte identifier) {
 			switch (identifier) {
 			case 't':
 				return WITHOUT_STATUS;
 			case 'T':
 			case 'A':
 				return WITH_DURATION;
 			case 'E':
 			case 'H':
 				return DEFAULT;
 			default:
 				return DEFAULT;
 			}
 		}
 	}
 }
