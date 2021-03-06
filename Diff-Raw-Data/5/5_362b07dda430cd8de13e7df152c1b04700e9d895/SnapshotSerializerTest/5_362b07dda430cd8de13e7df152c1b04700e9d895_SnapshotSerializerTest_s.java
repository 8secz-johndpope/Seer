 /**
  * Copyright 2009 Google Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */
 
 package org.waveprotocol.wave.examples.fedone.common;
 
 import org.waveprotocol.wave.examples.fedone.util.TestDataUtil;
 import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
 import org.waveprotocol.wave.model.id.WaveletName;
 import org.waveprotocol.wave.model.wave.data.WaveletData;
 
 /**
  * Tests for {@link SnapshotSerializer}
  * 
  * @author josephg@gmail.com (Joseph Gentle)
  */
public class SnapshotSerializerTest {
   public void testWaveletRoundtrip() throws Exception {
     WaveletData expected = TestDataUtil.createSimpleWaveletData();
     WaveletName name = WaveletName.of(expected.getWaveId(), expected.getWaveletId());
     HashedVersion version = new HashedVersionZeroFactoryImpl().createVersionZero(name);
 
     WaveletSnapshot snapshot = SnapshotSerializer.serializeWavelet(expected,
         CoreWaveletOperationSerializer.serialize(version));
     
     WaveletData actual = SnapshotSerializer.deserializeWavelet(snapshot, expected.getWaveId());
     
     TestDataUtil.checkSerializedWavelet(expected, actual);
   }
 }
