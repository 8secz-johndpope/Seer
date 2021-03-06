 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package org.apache.ace.client.services;
 
 import org.apache.ace.client.services.AssociationService.AssociationType;
 
 import com.google.gwt.user.client.rpc.AsyncCallback;
 
 /**
  * Asynchronous AssociationService (GWT boiler plate)
  */
 public interface AssociationServiceAsync {
     void link(BundleDescriptor bundle, GroupDescriptor group, AsyncCallback<Void> callback);
     void link(GroupDescriptor group, LicenseDescriptor license, AsyncCallback<Void> callback);
     void link(LicenseDescriptor license, TargetDescriptor target, AsyncCallback<Void> callback);
     void unlink(Descriptor license, Descriptor target, AsyncCallback<Void> callback);
     void getRelated(Descriptor o, AsyncCallback<Descriptor[]> callback);
     void setAssociationType(AssociationType type, AsyncCallback<Void> callback);
    void getAssociationType(AsyncCallback<AssociationType> callback);
 }
