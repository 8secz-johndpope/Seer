 /*
  * Copyright 2011 JBoss, by Red Hat, Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.jboss.errai.bus.client.api.base;
 
import java.util.HashMap;
import java.util.Map;

 import org.jboss.errai.bus.client.api.ErrorCallback;
 import org.jboss.errai.bus.client.api.Message;
 import org.jboss.errai.bus.client.framework.MessageBus;
 import org.jboss.errai.bus.client.framework.RequestDispatcher;
 import org.jboss.errai.bus.client.framework.RoutingFlag;
 import org.jboss.errai.common.client.api.ResourceProvider;
 import org.jboss.errai.common.client.framework.Assert;
 import org.jboss.errai.common.client.protocols.MessageParts;
 import org.jboss.errai.common.client.types.TypeHandlerFactory;
 
 /**
  * The default implementation of the Message interface. The usual way to create a CommandMessage is through the
  * {@link MessageBuilder} API.
  *
  * @see ConversationMessage
  * @see MessageBuilder
  * @author Mike Brock
  */
 @SuppressWarnings({"UnusedDeclarations"})
 public class CommandMessage implements Message {
  transient Map<String, ResourceProvider<?>> providedParts;
  Map<String, Object> parts;
   transient Map<String, Object> resources;
   ErrorCallback errorsCall;
   int routingFlags;
 
   /**
    * Creates a new CommandMessage with no parts and no provided parts.
    *
    * @return a new instance of CommandMessage.
    */
   static CommandMessage create() {
     return new CommandMessage();
   }
 
   /**
    * Creates a new CommandMessage with the given parts and no provided parts.
    * <p>
    * Note that {@link MessageBuilder} is the preferred API for creating Message
    * instances.
    *
    * @param parts
    *          The parts that this message should have initially. This map is
    *          taken as-is. No copy is made. Changes to the provided map will be
    *          reflected in this message, and additional parts given to this
    *          message will appear in the provided map.
    */
   public static CommandMessage createWithParts(Map<String, Object> parts) {
     return new CommandMessage(Assert.notNull(parts));
   }
 
   public static CommandMessage createWithParts(Map<String, Object> parts, int flags) {
     return new CommandMessage(Assert.notNull(parts), flags);
   }
 
 
   /**
    * Creates a new CommandMessage with the given parts and provided parts.
    * <p>
    * Note that {@link MessageBuilder} is the preferred API for creating Message
    * instances.
    *
    * @param parts
    *          The parts that this message should have initially. This map is
    *          taken as-is. No copy is made. Changes to the provided map will be
    *          reflected in this message, and additional parts given to this
    *          message will appear in the provided map.
    * @param provided
    *          The provided parts that this message should have initially. This
    *          map is taken as-is. No copy is made. Changes to the provided map
    *          will be reflected in this message, and additional parts given to
    *          this message will appear in the provided map.
    */
   public static CommandMessage createWithParts(Map<String, Object> parts, Map<String, ResourceProvider<?>> provided) {
     return new CommandMessage(parts, provided);
   }
 
   CommandMessage() {
     this.parts = new HashMap<String, Object>();
    this.providedParts = new HashMap<String, ResourceProvider<?>>();
   }
 
   private CommandMessage(Map<String, Object> parts) {
     this.parts = parts;
    this.providedParts = new HashMap<String, ResourceProvider<?>>();
   }
 
   public CommandMessage(Map<String, Object> parts, int routingFlags) {
     this.parts = parts;
     this.routingFlags = routingFlags;
   }
 
   private CommandMessage(Map<String, Object> parts, Map<String, ResourceProvider<?>> providers) {
     this.parts = parts;
     this.providedParts = providers;
   }
 
   @Override
   public String getCommandType() {
     return (String) parts.get(MessageParts.CommandType.name());
   }
 
   @Override
   public String getSubject() {
     return String.valueOf(parts.get(MessageParts.ToSubject.name()));
   }
 
   @Override
   public Message toSubject(String subject) {
     parts.put(MessageParts.ToSubject.name(), subject);
     return this;
   }
 
   @Override
   public Message command(Enum<?> type) {
     parts.put(MessageParts.CommandType.name(), type.name());
     return this;
   }
 
   @Override
   public Message command(String type) {
     parts.put(MessageParts.CommandType.name(), type);
     return this;
   }
 
   @Override
   public Message set(Enum<?> part, Object value) {
     return set(part.name(), value);
   }
 
   @Override
   public Message set(String part, Object value) {
     parts.put(part, value);
     return this;
   }
 
   @Override
   public Message setProvidedPart(String part, ResourceProvider<?> provider) {
     providedParts.put(part, provider);
     return this;
   }
 
   @Override
   public Message setProvidedPart(Enum<?> part, ResourceProvider<?> provider) {
     return setProvidedPart(part.name(), provider);
   }
 
   @Override
   public void remove(String part) {
     parts.remove(part);
   }
 
   @Override
   public void remove(Enum<?> part) {
     parts.remove(part.name());
   }
 
   @Override
   public Message copy(Enum<?> part, Message message) {
     set(part, message.get(Object.class, part));
     return this;
   }
 
   @Override
   public Message copy(String part, Message message) {
     set(part, message.get(Object.class, part));
     return this;
   }
 
   @Override
   public void setFlag(RoutingFlag flag) {
     routingFlags |= flag.flag();
   }
 
   @Override
   public void unsetFlag(RoutingFlag flag) {
     if ((routingFlags & flag.flag()) != 0) {
       routingFlags ^= flag.flag();
     }
   }
 
   @Override
   public boolean isFlagSet(RoutingFlag flag) {
     return (routingFlags & flag.flag()) != 0;
   }
 
   @Override
   @SuppressWarnings({ "UnusedDeclaration" })
   public <T> T get(Class<T> type, Enum<?> part) {
     return get(type, part.toString());
   }
 
   @Override
   @SuppressWarnings({ "UnusedDeclaration" })
   public <T> T get(Class<T> type, String part) {
     Object value = parts.get(part);
     return value == null ? null : TypeHandlerFactory.convert(value.getClass(), type, value);
   }
 
   @Override
   public boolean hasPart(Enum<?> part) {
     return hasPart(part.name());
   }
 
   @Override
   public boolean hasPart(String part) {
     return parts.containsKey(part);
   }
 
   @Override
   public Map<String, Object> getParts() {
     return parts;
   }
 
   @Override
   public Map<String, ResourceProvider<?>> getProvidedParts() {
     return providedParts;
   }
 
   @Override
   public Message setParts(Map<String, Object> parts) {
    this.parts = parts;
     return this;
   }
 
   @Override
   public Message addAllParts(Map<String, Object> parts) {
     this.parts.putAll(parts);
     return this;
   }
 
   @Override
   public Message addAllProvidedParts(Map<String, ResourceProvider<?>> parts) {
     this.providedParts.putAll(parts);
     return this;
   }
 
   @Override
   public Message setResource(String key, Object res) {
     if (this.resources == null)
       this.resources = new HashMap<String, Object>();
     this.resources.put(key, res);
     return this;
   }
 
   @Override
   @SuppressWarnings("unchecked")
   public <T> T getResource(Class<T> type, String key) {
     return (T) (this.resources == null ? null : this.resources.get(key));
   }
 
   @Override
   public Message copyResource(String key, Message copyFrom) {
     if (!copyFrom.hasResource(key)) {
       throw new RuntimeException("Cannot copy resource '" + key + "': no such resource.");
     }
     setResource(key, copyFrom.getResource(Object.class, key));
     return this;
   }
 
   @Override
   public Message errorsCall(ErrorCallback callback) {
     if (this.errorsCall != null) {
       throw new RuntimeException("An ErrorCallback is already registered");
     }
     this.errorsCall = callback;
     return this;
   }
 
   @Override
   public ErrorCallback getErrorCallback() {
     return errorsCall;
   }
 
   @Override
   public boolean hasResource(String key) {
     return this.resources != null && this.resources.containsKey(key);
   }
 
   @Override
   public void addResources(Map<String, ?> resources) {
     if (this.resources == null) {
       this.resources = new HashMap<String, Object>(resources);
     }
     else {
       this.resources.putAll(resources);
     }
   }
 
   @Override
   public void commit() {
     if (!providedParts.isEmpty()) {
       for (Map.Entry<String, ResourceProvider<?>> entry : providedParts.entrySet())
         set(entry.getKey(), entry.getValue().get());
     }
   }
 
   @Override
   public boolean isCommited() {
     return isFlagSet(RoutingFlag.Committed);
   }
 
   @Override
   public void sendNowWith(MessageBus viaThis) {
     viaThis.send(this);
   }
 
   @Override
   public void sendNowWith(RequestDispatcher viaThis) {
     try {
       viaThis.dispatch(this);
     }
     catch (Exception e) {
       throw new MessageDeliveryFailure("could not deliver message: " + e.getMessage(), e);
     }
   }
 
   @Override
   public String toString() {
     return buildDescription();
   }
 
   private String buildDescription() {
     StringBuilder append = new StringBuilder();
     boolean f = false;
     for (Map.Entry<String, Object> entry : parts.entrySet()) {
       if (f)
         append.append(", ");
       append.append(entry.getKey()).append("=").append(String.valueOf(entry.getValue()));
       f = true;
     }
     return append.toString();
   }
 }
