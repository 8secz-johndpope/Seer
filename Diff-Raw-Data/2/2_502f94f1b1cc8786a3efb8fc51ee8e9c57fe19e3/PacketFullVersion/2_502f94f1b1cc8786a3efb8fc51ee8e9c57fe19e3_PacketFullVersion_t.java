 /*
  * This file is part of Spoutcraft.
  *
  * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
  * Spoutcraft is licensed under the GNU Lesser General Public License.
  *
  * Spoutcraft is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Spoutcraft is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.spoutcraft.client.packet;
 
 import java.io.IOException;
 
 import org.spoutcraft.spoutcraftapi.io.SpoutInputStream;
 import org.spoutcraft.spoutcraftapi.io.SpoutOutputStream;
 
 public class PacketFullVersion implements SpoutPacket {
 	private long version;
 
 	public PacketFullVersion() {
 	}
 
 	public PacketFullVersion(String versionString) {
 		try {
 			//Will break if builds are < 1000 or > 9999
 			//1.3.2 b1111
 			version = Integer.parseInt(versionString.substring(7));
 		} catch (Exception e) {
 			e.printStackTrace();
 			version = 1;
 		}
 	}
 
 	public void readData(SpoutInputStream input) throws IOException {
 		version = input.readLong();
 	}
 
 	public void writeData(SpoutOutputStream output) throws IOException {
		output.writeLong(version);
 	}
 
 	public void run(int playerId) {
 	}
 
 	public void failure(int playerId) {
 	}
 
 	public PacketType getPacketType() {
 		return PacketType.PacketFullVersion;
 	}
 
 	public int getVersion() {
 		return 0;
 	}
 }
