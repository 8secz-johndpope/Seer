 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: Signal.java
  *
  * Copyright (c) 2009 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 package com.sun.electric.tool.simulation;
 import java.io.*;
 import java.util.*;
 
 import com.sun.electric.database.geometry.btree.*;
 import com.sun.electric.database.geometry.btree.unboxed.*;
 import java.awt.Color;
 import java.awt.Graphics;
 import java.awt.Dimension;
 import java.awt.geom.Rectangle2D;
 import java.awt.geom.Point2D;
 import com.sun.electric.database.geometry.PolyBase;
 import com.sun.electric.tool.user.waveform.Panel.WaveSelection;
 import com.sun.electric.tool.user.waveform.*;
 import com.sun.electric.database.geometry.Poly;
 import java.awt.font.GlyphVector;
 import com.sun.electric.database.variable.TextDescriptor;
 
 /**
  *  An implementation of Sample for complex data.  Holds 
  *  two doubles internally: real and imaginary.
  */
 public class ComplexSample extends ScalarSample implements Sample {
 
     private double real;
     private double imag;
 
     public ComplexSample(double real, double imag) {
        super(Math.atan2(imag, real));
         this.real = real;
         this.imag = imag;
     }
 
     public double getReal() { return real; }
     public double getImag() { return imag; }
 
     public boolean equals(Object o) {
         if (o==null || !(o instanceof ComplexSample)) return false;
         ComplexSample cs = (ComplexSample)o;
         return cs.real==real && cs.imag==imag;
     }
 
     public int hashCode() {
         long l = Double.doubleToLongBits(real) ^ Double.doubleToLongBits(imag);
         return ((int)(l & 0xffffffff)) ^ ((int)((l >> 32) & 0xffffffff));
     }
 
     public boolean isLogicX() { return false; }
     public boolean isLogicZ() { return false; }
 
     public Sample lub(Sample s) {
         if (!(s instanceof ComplexSample))
             throw new RuntimeException("tried to call ComplexSample.lub("+s.getClass().getName()+")");
         ComplexSample cs = (ComplexSample)s;
         if (cs.real >= real && cs.imag >= imag) return cs;
         if (cs.real <= real && cs.imag <= imag) return this;
         return new ComplexSample(Math.max(real, cs.real), Math.max(imag, cs.imag));
     }
 
     public Sample glb(Sample s) {
         if (!(s instanceof ComplexSample))
             throw new RuntimeException("tried to call ComplexSample.glb("+s.getClass().getName()+")");
         ComplexSample cs = (ComplexSample)s;
         if (cs.real >= real && cs.imag >= imag) return this;
         if (cs.real <= real && cs.imag <= imag) return cs;
         return new ComplexSample(Math.min(real, cs.real), Math.min(imag, cs.imag));
     }
 
     public static final Unboxed<ComplexSample> unboxer = new Unboxed<ComplexSample>() {
         public int getSize() { return UnboxedHalfDouble.instance.getSize()*2; }
         public ComplexSample deserialize(byte[] buf, int ofs) {
             return new ComplexSample(UnboxedHalfDouble.instance.deserialize(buf, ofs),
                                      UnboxedHalfDouble.instance.deserialize(buf, ofs+UnboxedHalfDouble.instance.getSize()));
         }
         public void serialize(ComplexSample v, byte[] buf, int ofs) {
             UnboxedHalfDouble.instance.serialize(v.real, buf, ofs);
             UnboxedHalfDouble.instance.serialize(v.imag, buf, ofs+UnboxedHalfDouble.instance.getSize());
         }
     };
 
     /*
                         double val = Math.hypot(imagPart, realPart);
                         if (signal.getSample(time)==null)
                             signal.addSample(time, new ScalarSample(val));
     */
 
     private static final LatticeOperation<ComplexSample> latticeOp =
         new LatticeOperation<ComplexSample>(unboxer) {
         private final int sz = UnboxedHalfDouble.instance.getSize();
         public void glb(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs) {
             ScalarSample.latticeOp.glb(buf1, ofs1,    buf2, ofs2,    dest, dest_ofs);
             ScalarSample.latticeOp.glb(buf1, ofs1+sz, buf2, ofs2+sz, dest, dest_ofs+sz);
         }
         public void lub(byte[] buf1, int ofs1, byte[] buf2, int ofs2, byte[] dest, int dest_ofs) {
             ScalarSample.latticeOp.lub(buf1, ofs1,    buf2, ofs2,    dest, dest_ofs);
             ScalarSample.latticeOp.lub(buf1, ofs1+sz, buf2, ofs2+sz, dest, dest_ofs+sz);
         }
     };
 
     public static Signal<ComplexSample> createComplexSignal(HashMap<String,Signal> an, Stimuli sd, String signalName, String signalContext) {
     	/**
     	 *  Adam says: This class is an _anonymous_ inner class for a reason.  Although XXXSample.createSignal() returns a
     	 *  Signal<XXXSample>, it is important that other code does not assume this is the only way such signals might
     	 *  arise.  Phrased differently, if there were an inner class XXXSample.SignalOfXXX extends Signal<XXXSample>,
     	 *  you might see other code write "if (x instanceof XXXSample.SignalOfXXX) { ... }" -- and we don't want people
     	 *  to do this.  So, by making the class anonymous, we intentionally deprive other code of the ability to do these
     	 *  instanceof checks.
     	 */
         Signal<ComplexSample> ret =
             new BTreeSignal<ComplexSample>(an, sd, signalName, signalContext, BTreeSignal.getTree(unboxer, latticeOp)) {
             public void plot(Panel panel, Graphics g, WaveSignal ws, Color light,
                              List<PolyBase> forPs, Rectangle2D bounds, List<WaveSelection> selectedObjects) {
             	ScalarSample.plotSig(this, panel, g, ws, light, forPs, bounds, selectedObjects);
 //                throw new RuntimeException("not implemented");
             }
         };
         return ret;
     }
 
 }
 
 
