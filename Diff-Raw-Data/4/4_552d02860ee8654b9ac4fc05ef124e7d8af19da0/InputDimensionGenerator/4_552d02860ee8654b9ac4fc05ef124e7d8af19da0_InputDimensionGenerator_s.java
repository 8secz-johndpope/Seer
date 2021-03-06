 /*
  *  Copyright (c) 2012-2013 Malhar, Inc.
  *  All Rights Reserved.
  */
 package com.datatorrent.contrib.summit.ads;
 
 import com.datatorrent.api.BaseOperator;
 import com.datatorrent.api.DefaultInputPort;
 import com.datatorrent.api.DefaultOutputPort;
 import com.datatorrent.api.annotation.OutputPortFieldAnnotation;
 
 /**
  *
  * @author Pramod Immaneni <pramod@malhar-inc.com>
  */
 public class InputDimensionGenerator extends BaseOperator
 {
 
   private static final int dimSelect[][] = { {0, 0, 0}, {0, 0, 1}, {0, 1, 0}, {1, 0, 0}, {0, 1, 1}, {1, 0, 1}, {1, 1, 0}, {1, 1, 1} };
   private static final int dimSelLen = 8;
 
   @OutputPortFieldAnnotation(name = "outputPort")
   public final transient DefaultOutputPort<AdInfo> outputPort = new DefaultOutputPort<AdInfo>(this);
 
  @OutputPortFieldAnnotation(name = "inputPort")
   public final transient DefaultInputPort<AdInfo> inputPort = new DefaultInputPort<AdInfo>(this) {
 
     @Override
     public void process(AdInfo tuple)
     {
       emitDimensions(tuple);
     }
 
   };
 
    private void emitDimensions(AdInfo adInfo) {
     for (int j = 0; j < dimSelLen; ++j) {
       AdInfo oadInfo = new AdInfo();
       if (dimSelect[j][0] == 1) oadInfo.setPublisherId(adInfo.getPublisherId());
       if (dimSelect[j][1] == 1) oadInfo.setAdvertiserId(adInfo.getAdvertiserId());
       if (dimSelect[j][2] == 1) oadInfo.setAdUnit(adInfo.getAdUnit());
       oadInfo.setClick(adInfo.isClick());
       oadInfo.setValue(adInfo.getValue());
       oadInfo.setTimestamp(adInfo.getTimestamp());
       this.outputPort.emit(oadInfo);
     }
   }
 
 
 }
