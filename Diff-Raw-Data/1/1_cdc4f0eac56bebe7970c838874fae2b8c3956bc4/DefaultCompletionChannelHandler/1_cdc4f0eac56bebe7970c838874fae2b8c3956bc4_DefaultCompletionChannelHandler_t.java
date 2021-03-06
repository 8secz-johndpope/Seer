 package org.handwerkszeug.riak.op.internal;
 
 import org.handwerkszeug.riak.Markers;
 import org.handwerkszeug.riak.nls.Messages;
 import org.handwerkszeug.riak.op.RiakResponseHandler;
 import org.handwerkszeug.riak.util.NettyUtil;
 import org.jboss.netty.channel.ChannelHandlerContext;
 import org.jboss.netty.channel.MessageEvent;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * @author taichi
  */
 class DefaultCompletionChannelHandler<T> extends
 		AbstractCompletionChannelHandler<T> {
 
 	static final Logger LOG = LoggerFactory
 			.getLogger(DefaultCompletionChannelHandler.class);
 
 	final NettyUtil.MessageHandler handler;
 
 	public DefaultCompletionChannelHandler(CompletionSupport support,
 			String name, RiakResponseHandler<T> users,
 			NettyUtil.MessageHandler handler, CountDownRiakFuture future) {
 		super(support, name, users, future);
 		this.handler = handler;
 	}
 
 	@Override
 	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
 			throws Exception {
 		try {
 			Object receive = e.getMessage();
 			if (LOG.isDebugEnabled()) {
 				LOG.debug(Markers.DETAIL, Messages.Receive, this.name, receive);
 			}
 			if (this.handler.handle(receive)) {
 				// TODO happen to http error it's not success, but now force
 				// success.
 				this.future.setSuccess();
 				this.support.remove(this.name);
 				this.support.invokeNext();
 			}
 			e.getFuture().addListener(this.support);
 		} catch (Exception ex) {
 			throw ex;
 		} catch (Error ex) {
 			setFailure(ex);
 			throw ex;
 		}
 	}
 }
