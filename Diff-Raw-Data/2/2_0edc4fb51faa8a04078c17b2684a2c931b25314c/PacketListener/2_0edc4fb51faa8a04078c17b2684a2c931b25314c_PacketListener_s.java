 /**
  * $RCSfile$
  * $Revision$
  * $Date$
  *
  * Copyright (C) 2002-2003 Jive Software. All rights reserved.
  * ====================================================================
  * The Jive Software License (based on Apache Software License, Version 1.1)
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  *
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  * 3. The end-user documentation included with the redistribution,
  *    if any, must include the following acknowledgment:
  *       "This product includes software developed by
  *        Jive Software (http://www.jivesoftware.com)."
  *    Alternately, this acknowledgment may appear in the software itself,
  *    if and wherever such third-party acknowledgments normally appear.
  *
  * 4. The names "Smack" and "Jive Software" must not be used to
  *    endorse or promote products derived from this software without
  *    prior written permission. For written permission, please
  *    contact webmaster@jivesoftware.com.
  *
  * 5. Products derived from this software may not be called "Smack",
  *    nor may "Smack" appear in their name, without prior written
  *    permission of Jive Software.
  *
  * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
  * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
  * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
  * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  * ====================================================================
  */
 
 package org.jivesoftware.smack;
 
 import org.jivesoftware.smack.packet.Packet;
 
 /**
  * Provides a mechanism to listen for packets that pass a specified filter.
  * This allows event-style programming -- every time a new packet is found,
  * the {@link #processPacket(Packet)} method will be called. This is the
  * opposite approach to the functionality provided by a {@link PacketCollector}
  * which lets you block while waiting for results.
  *
 * @see XMPPConnection#addPacketListener(PacketListener, PacketFilter)
  * @author Matt Tucker
  */
 public interface PacketListener {
 
     /**
      * Process the next packet sent to this packet listener.<p>
      *
      * A single thread is responsible for invoking all listeners, so
      * it's very important that implementations of this method not block
      * for any extended period of time.
      *
      * @param packet the packet to process.
      */
     public void processPacket(Packet packet);
 
 }
