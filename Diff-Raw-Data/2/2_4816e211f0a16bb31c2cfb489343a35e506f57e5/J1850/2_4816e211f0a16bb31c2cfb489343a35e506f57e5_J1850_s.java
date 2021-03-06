 //
 // HarleyDroid: Harley Davidson J1850 Data Analyser for Android.
 //
 // Copyright (C) 2010,2011 Stelian Pop <stelian@popies.net>
 // Based on various sources, especially:
 //	minigpsd by Tom Zerucha <tz@execpc.com>
 //	AVR J1850 VPW Interface by Michael Wolf <webmaster@mictronics.de>
 //
 // This program is free software: you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation, either version 3 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program.  If not, see <http://www.gnu.org/licenses/>.
 //
 
 package org.harleydroid;
 
 public class J1850 {
 
 	public static final int MAXBUF = 1024;
 	// last reading of odometer ticks
 	private static int odolast = 0;
 	// accumulated odometer ticks (deals with overflow at 0xffff)
 	private static int odoaccum = 0;
 	// last reading of fuel ticks
 	private static int fuellast = 0;
 	// accumulated fuel ticks (deals with overflow at 0xffff)
 	private static int fuelaccum = 0;
 
 	static byte[] bytes_to_hex(byte[] in) {
 		byte out[] = new byte[MAXBUF];
 		int inidx = 0, outidx = 0;
 
 		while (inidx < in.length) {
 			int digit0, digit1;
 
 			while (inidx < in.length &&
 					Character.isWhitespace((char)in[inidx]))
 				inidx++;
 			if (inidx >= in.length)
 				break;
 			digit0 = Character.digit((char)in[inidx++], 16);
 
 			while (inidx < in.length &&
 					Character.isWhitespace((char)in[inidx]))
 				inidx++;
 			if (inidx >= in.length)
 				break;
 			digit1 = Character.digit((char)in[inidx++], 16);
 
 			out[outidx++] = (byte) (digit0 * 16 + digit1);
 		}
 		byte[] ret = new byte[outidx];
 		System.arraycopy(out, 0, ret, 0, outidx);
 		return ret;
 	}
 
 	public static byte crc(byte[] in) {
 		int i, j;
 		byte crc = (byte)0xff;
 
 		for (i = 0; i < in.length; i++) {
 			byte c = in[i];
 			for (j = 0; j < 8; ++j) {
 				byte poly = 0;
 				if ((0x80 & (crc ^ c)) != 0)
 					poly = 0x1d;
 				crc = (byte) (((crc << 1) & 0xff) ^ poly);
 				c <<= 1;
 			}
 		}
 		return crc;
 	}
 
 	public static boolean parse(byte[] buffer, HarleyData hd) {
 		byte[] in;
 		int x;
 		int y;
 
 		in = bytes_to_hex(buffer);
 
 		/*
 		System.out.print("BUF: ");
 		for (int i = 0; i < in.length; i++)
 			System.out.print(Integer.toHexString(in[i]) + " ");
 		System.out.println("");
 		 */
 
 		if (crc(in) != (byte)0xc4) {
 			hd.setBadCRC(buffer);
 			return false;
 		}
 
 		x = y = 0;
 		if (in.length >= 4)
 			x = ((in[0] << 24) & 0xff000000) |
 			((in[1] << 16) & 0x00ff0000) |
 			((in[2] <<  8) & 0x0000ff00) |
 			(in[3]        & 0x000000ff);
 		if (in.length >= 6)
 			y = ((in[4] << 8) & 0x0000ff00) |
 			(in[5]       & 0x000000ff);
 
 		if (x == 0x281b1002)
 			hd.setRPM(y);
 		else if (x == 0x48291002)
 			hd.setSpeed(y);
 		else if (x == 0xa8491010)
			hd.setEngineTemp(in[4]);
 		else if (x == 0xa83b1003) {
 			if (in[4] != 0) {
 				int gear = 0;
 				while ((in[4] >>= 1) != 0)
 					gear++;
 				hd.setGear(gear);
 			} else
 				hd.setGear(-1);
 		} else if ((x == 0x48da4039) && ((in[4] & 0xfc) == 0))
 			hd.setTurnSignals(in[4] & 0x03);
 		else if ((x & 0xffffff7f) == 0xa8691006) {
 			odolast = y - odolast;
 			if (odolast < 0)	// ...could also test for (x & 0x80)
 				odolast += 65536;
 			odoaccum += odolast;
 			odolast = y;
 			hd.setOdometer(odoaccum);
 		} else if ((x & 0xffffff7f) == 0xa883100a) {
 			fuellast = y - fuellast;
 			if (fuellast < 0)	// ...could also test for (x & 0x80)
 				fuellast += 65536;
 			fuelaccum += fuellast;
 			fuellast = y;
 			hd.setFuel(fuelaccum);
 		} else if ((x == 0xa8836112) && ((in[4] & 0xd0) == 0xd0))
 			hd.setFuelGauge(in[4] & 0x0f);
 		else if ((x & 0xffffff5d) == 0x483b4000) {
 			hd.setNeutral((in[3] & 0x20) != 0);
 			hd.setClutch((in[3] & 0x80) != 0);
 		} else if ((x & 0xffffff7f) == 0x68881003) {
 			if ((in[3] & 0x80) != 0)
 				hd.setCheckEngine(true);
 			else
 				hd.setCheckEngine(false);
 		} else
 			hd.setUnknown(buffer);
 		return true;
 	}
 
 	public static void main(String[] args) {
 		// RPM at 1000 RPM
 		//String line = "28 1B 10 02 0f a0 d7";
 		// Speed at 100 km/h
 		String line = "48 29 10 02 32 00 0f";
 
 		byte[] in = line.getBytes();
 		byte[] out = bytes_to_hex(in);
 
 		System.out.print("Input: ");
 		for (int i = 0; i < out.length; i++)
 			System.out.print(String.format("0x%02x ", out[i]));
 		System.out.println("");
 
 		System.out.println("CRC: " + String.format("0x%02x", crc(out)));
 
 		byte[] out_without_crc = new byte[out.length - 1];
 		System.arraycopy(out, 0, out_without_crc, 0, out.length - 1);
 
 		System.out.println("Need CRC byte: " + String.format("0x%02x", ~crc(out_without_crc)));
 
 		HarleyData data = new HarleyData();
 		if (parse(in, data))
 			System.out.println(data);
 		else
 			System.out.println("Parse error");
 	}
 }
