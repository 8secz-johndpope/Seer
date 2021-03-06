 package org.dancres.peers.acc;
 
 import com.google.gson.Gson;
 import com.ning.http.client.AsyncCompletionHandler;
 import com.ning.http.client.AsyncHttpClient;
 import com.ning.http.client.Response;
 import org.dancres.peers.Peer;
 import org.jboss.netty.buffer.ChannelBuffers;
 import org.jboss.netty.handler.codec.http.*;
 import org.jboss.netty.util.CharsetUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.Iterator;
 import java.util.Queue;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentLinkedQueue;
 import java.util.concurrent.Future;
 
 /**
  * <p>A service which maintains a number of independent, uniquely named accumulators.</p>
  *
  * <p>An accumulator provides a total for a set of counts within a rolling time window. Thus counts submitted prior to
  * the window are dropped from the total such that if no samples are received for long enough, the total will
  * decay to zero.</p>
  */
 public class DecayingAccumulators implements Peer.Service {
     private static final Logger _logger = LoggerFactory.getLogger(DecayingAccumulators.class);
 
     public static class Count implements Comparable<Count> {
         private final String _accumulatorId;
         private final long _samplePeriodInMillis;
         private final long _count;
 
         /**
          * @param anAccumulatorId is the id of the accumulator to add this count to
          * @param aSamplePeriod is the period of which the count was taken
          * @param aCount is the count itself
          */
         public Count(String anAccumulatorId, long aSamplePeriod, long aCount) {
             _accumulatorId = anAccumulatorId;
             _samplePeriodInMillis = aSamplePeriod;
             _count = aCount;
         }
 
         public String getAccumulatorId() {
             return _accumulatorId;
         }
 
         public long getSamplePeriod() {
             return _samplePeriodInMillis;
         }
 
         public long getCount() {
             return _count;
         }
 
         public String toString() {
             return "ID: " + _accumulatorId + ", Period: " + _samplePeriodInMillis + ", Count: " + _count;
         }
 
         public int compareTo(Count aCount) {
             if (_count < aCount._count)
                 return -1;
             else if (_count > aCount._count)
                 return 1;
             else
                 return 0;
         }
     }
 
     private class Docket {
         private final long _arrivalTime;
         private final Count _count;
 
         Docket(Count aCount) {
             _arrivalTime = System.currentTimeMillis();
             _count = aCount;
         }
 
         public boolean isCurrent(long aMinimumAge) {
             return ((_arrivalTime - _count.getSamplePeriod()) >= aMinimumAge);
         }
 
         public Count getSample() {
             return _count;
         }
     }
 
     private final Peer.ServiceDispatcher _dispatcher;
     private final ConcurrentHashMap<String, Queue<Docket>> _collectedSamples = new ConcurrentHashMap<>();
     private final Peer _peer;
     private final long _window;
 
     /**
      * Use this method to setup a client or a server with a default window of 60 seconds.
      *
      * @param aPeer to bind this service to
      */
     public DecayingAccumulators(Peer aPeer) {
         this(aPeer, 60000);
     }
 
     /**
      * @param aPeer to bind this service to
      * @param aQuantum the window over which counts are accumulated.
      */
     public DecayingAccumulators(Peer aPeer, long aQuantum) {
         _peer = aPeer;
         _dispatcher = new Dispatcher();
         _window = aQuantum;
     }
 
     public String getAddress() {
         return "/rc";
     }
 
     /**
      * Add a sample to a specified peer.
      *
      * @param aPeerAddress is the peer maintaining the accumulator
      * @param aCount is the sample to add
      * @return the total of all samples received for the accumulator
      * @throws Exception
      */
     public Count log(String aPeerAddress, Count aCount) throws Exception {
         final Gson myGson = new Gson();
         AsyncHttpClient myClient = _peer.getClient();
 
         Future<Count> mySample = myClient.preparePost(aPeerAddress + getAddress()).setBody(
                 myGson.toJson(aCount)).execute(new AsyncCompletionHandler<Count>() {
             public Count onCompleted(Response aResponse) throws Exception {
                 String myTotalSample = aResponse.getResponseBody();
 
                 try {
                     return myGson.fromJson(myTotalSample, Count.class);
                 } catch (Exception anE) {
                     _logger.error("Error in unpack", anE);
                     throw anE;
                 }
             }
         });
 
         return mySample.get();
     }
 
     /**
      * Get the current total of all samples received
      *
      * @param aPeerAddress is the peer maintaining the counter
      * @param anId is the name of the accumulator for which a total is required
      * @return
      * @throws Exception
      */
     public Count get(String aPeerAddress, String anId) throws Exception {
         final Gson myGson = new Gson();
         AsyncHttpClient myClient = _peer.getClient();
 
         Future<Count> mySample = myClient.prepareGet(aPeerAddress + getAddress()).addQueryParameter("id",
                 myGson.toJson(anId)).execute(new AsyncCompletionHandler<Count>() {
             public Count onCompleted(Response aResponse) throws Exception {
                 String myTotalSample = aResponse.getResponseBody();
 
                 try {
                     return myGson.fromJson(myTotalSample, Count.class);
                 } catch (Exception anE) {
                     _logger.error("Error in unpack", anE);
                     throw anE;
                 }
             }
         });
 
         return mySample.get();
     }
 
     public Peer.ServiceDispatcher getDispatcher() {
         return _dispatcher;
     }
 
     private class Dispatcher implements Peer.ServiceDispatcher {
         public void dispatch(String aServicePath, HttpRequest aRequest, HttpResponse aResponse) {
             if (aRequest.getMethod().equals(HttpMethod.POST)) {
                 String mySampleString = aRequest.getContent().toString(CharsetUtil.UTF_8);
 
                 Gson myGson = new Gson();
                 Count myCount = myGson.fromJson(mySampleString, Count.class);
 
                 Count myAccumulation = add(myCount);
 
                 aResponse.setContent(ChannelBuffers.copiedBuffer(myGson.toJson(myAccumulation), CharsetUtil.UTF_8));
                 aResponse.setStatus(HttpResponseStatus.OK);
             } else if (aRequest.getMethod().equals(HttpMethod.GET)){
                 String myIdString = new QueryStringDecoder(aRequest.getUri()).getParameters().get("id").get(0);
 
                 Gson myGson = new Gson();
                 String myId = myGson.fromJson(myIdString, String.class);
 
                 Count myAccumulation = reduce(myId);
 
                 aResponse.setContent(ChannelBuffers.copiedBuffer(myGson.toJson(myAccumulation), CharsetUtil.UTF_8));
                 aResponse.setStatus(HttpResponseStatus.OK);
             } else {
                 aResponse.setStatus(HttpResponseStatus.BAD_REQUEST);
             }
         }
     }
 
     private Queue<Docket> getAndCreateDocketList(Count aCount) {
         String myId = aCount.getAccumulatorId();
         Queue<Docket> mySamples = _collectedSamples.get(myId);
 
         if (mySamples == null) {
             Queue<Docket> myInitial = new ConcurrentLinkedQueue<>();
            Queue<Docket> myResult = _collectedSamples.put(myId, myInitial);
 
             mySamples = ((myResult == null) ? myInitial : myResult);
         }
 
         return mySamples;
     }
 
     private Count add(Count aCount) {
         Queue<Docket> myDockets = getAndCreateDocketList(aCount);
 
         myDockets.add(new Docket(aCount));
 
         return reduce(aCount.getAccumulatorId());
     }
 
     private Count reduce(String anId) {
         Queue<Docket> myDockets = _collectedSamples.get(anId);
 
         if (myDockets == null)
             return new Count(anId, _window, 0);
 
         long myMinimumAge = System.currentTimeMillis() - _window;
         long myTotal = 0;
 
         Iterator<Docket> myCurrentSamples = myDockets.iterator();
         while (myCurrentSamples.hasNext()) {
             Docket myDocket = myCurrentSamples.next();
 
             if (myDocket.isCurrent(myMinimumAge)) {
                 myTotal += myDocket.getSample().getCount();
             } else {
                 myCurrentSamples.remove();
             }
         }
 
         return new Count(anId, _window, myTotal);
     }
 }
