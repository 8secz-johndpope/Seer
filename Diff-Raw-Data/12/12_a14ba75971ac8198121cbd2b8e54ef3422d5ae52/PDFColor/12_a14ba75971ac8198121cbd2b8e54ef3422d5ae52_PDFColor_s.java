 /*-- $Id$ -- 
 
  ============================================================================
 						 The Apache Software License, Version 1.1
  ============================================================================
  
 	 Copyright (C) 1999 The Apache Software Foundation. All rights reserved.
  
  Redistribution and use in source and binary forms, with or without modifica-
  tion, are permitted provided that the following conditions are met:
  
  1. Redistributions of	source code must	retain the above copyright	notice,
 	 this list of conditions and the following disclaimer.
  
  2. Redistributions in binary form must reproduce the above copyright notice,
 	 this list of conditions and the following disclaimer in the documentation
 	 and/or other materials provided with the distribution.
  
  3. The end-user documentation included with the redistribution, if any, must
 	 include  the following  acknowledgment:	"This product includes	software
 	 developed	by the	Apache Software Foundation	(http://www.apache.org/)."
 	 Alternately, this	acknowledgment may	appear in the software itself,	if
 	 and wherever such third-party acknowledgments normally appear.
  
  4. The names "Fop" and  "Apache Software Foundation"  must not be used to
 	 endorse  or promote  products derived	from this	software without	prior
 	 written permission. For written permission, please contact
 	 apache@apache.org.
  
  5. Products  derived from this software may not  be called "Apache", nor may
 	 "Apache" appear	in their name,	without prior written permission  of the
 	 Apache Software Foundation.
  
  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  FITNESS  FOR A PARTICULAR	PURPOSE ARE  DISCLAIMED.	IN NO  EVENT SHALL	THE
  APACHE SOFTWARE	FOUNDATION	OR ITS CONTRIBUTORS	BE LIABLE FOR	ANY DIRECT,
  INDIRECT, INCIDENTAL, SPECIAL,	EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
  DING, BUT NOT LIMITED TO, PROCUREMENT	OF SUBSTITUTE GOODS OR SERVICES; LOSS
  OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)	HOWEVER CAUSED AND ON
  ANY	THEORY OF LIABILITY,  WHETHER  IN CONTRACT,	STRICT LIABILITY,  OR TORT
  (INCLUDING  NEGLIGENCE OR	OTHERWISE) ARISING IN	ANY WAY OUT OF THE	USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
  This software	consists of voluntary contributions made  by many individuals
  on  behalf of the Apache Software	Foundation and was	originally created by
  James Tauber <jtauber@jtauber.com>. For more  information on the Apache 
  Software Foundation, please see <http://www.apache.org/>.
  
  */
 package org.apache.fop.pdf;
 
 // Java
 import java.util.Vector;
 import java.io.IOException;
 import java.io.PrintWriter;
 
 //FOP
 import org.apache.fop.datatypes.ColorType;
 import org.apache.fop.datatypes.ColorSpace;
 
 public class PDFColor extends PDFPathPaint {
 	protected static double blackFactor = 2.0;//could be 3.0 as well.
 	protected double red  = -1.0;
 	protected double green= -1.0;
 	protected double blue = -1.0;
 	
 	protected double cyan	= -1.0;
 	protected double magenta= -1.0;
 	protected double yellow = -1.0;
 	protected double black	= -1.0;
 	
 	private PDFNumber pdfNumber = new PDFNumber();
 	
 	public PDFColor(org.apache.fop.datatypes.ColorType theColor)
 	{
 		this.colorSpace = new ColorSpace(ColorSpace.DEVICE_RGB);
 		//super(theNumber)
 		this.red  =  (double)theColor.red();
 		this.green = (double)theColor.green();
 		this.blue  = (double)theColor.blue();
 		
 	}
 
 	public PDFColor(double theRed, double theGreen, double theBlue) {
 		//super(theNumber);
 		this.colorSpace = new ColorSpace(ColorSpace.DEVICE_RGB);
 
 		this.red = theRed;
 		this.green = theGreen;
 		this.blue = theBlue;
 	}
 	
 	// components from 0 to 255
 	public PDFColor(int theRed, int theGreen, int theBlue) {
		this(((double) theRed) / 255d * 2d - 1d, ((double) theGreen) / 255d * 2d - 1d, ((double) theBlue) / 255d * 2d - 1d);
 	}
 	
 	public PDFColor(double theCyan, double theMagenta, double theYellow, double theBlack) {
 		//super(theNumber);//?
 		
 		this.colorSpace = new ColorSpace(ColorSpace.DEVICE_CMYK);
 		
 		this.cyan = theCyan;
 		this.magenta = theMagenta;
 		this.yellow = theYellow;
 		this.black = theBlack;
 	}
 	
 	
 	public Vector getVector()
 	{//return a vector representation of the color
 	//in the appropriate colorspace.
 		Vector theColorVector= new Vector();
 		if (this.colorSpace.getColorSpace() == ColorSpace.DEVICE_RGB)
 		{//RGB				
 			theColorVector.addElement(new Double(this.red));
 			theColorVector.addElement(new Double(this.green));
 			theColorVector.addElement(new Double(this.blue));
 		}
 		else if (this.colorSpace.getColorSpace() == ColorSpace.DEVICE_CMYK)
 		{//CMYK
 			theColorVector.addElement(new Double(this.cyan));
 			theColorVector.addElement(new Double(this.magenta));
 			theColorVector.addElement(new Double(this.yellow));
 			theColorVector.addElement(new Double(this.black));
 		}
 		else
 		{//GRAY
 			theColorVector.addElement(new Double(this.black));
 		}
 		return(theColorVector);
 	}
 	
 	public double red()
 	{
 		return(this.red);
 	}
 	public double green()
 	{
 		return(this.green);
 	}
 	public double blue()
 	{
 		return(this.blue);
 	}
 	public int red255()
 	{
		return (int) ((this.red + 1d) / 2d * 255d);
 	}
 	public int green255()
 	{
		return (int) ((this.green + 1d) / 2d * 255d);
 	}
 	public int blue255()
 	{
		return (int) ((this.blue + 1d) / 2d * 255d);
 	}
 	public double cyan()
 	{
 		return(this.cyan);
 	}
 	public double magenta()
 	{
 		return(this.magenta);
 	}
 	public double yellow()
 	{
 		return(this.yellow);
 	}
 	public double black()
 	{
 		return(this.black);
 	}
 	
 	public void setColorSpace(int theColorSpace)
 	{
 		int theOldColorSpace = this.colorSpace.getColorSpace();
 		if(theOldColorSpace!=theColorSpace)
 		{
 			if (theOldColorSpace==ColorSpace.DEVICE_RGB)
 			{
 				if(theColorSpace==ColorSpace.DEVICE_CMYK)
 				{
 					this.convertRGBtoCMYK();
 				}
 				else //convert to Gray?
 				{
 					this.convertRGBtoGRAY();
 				}
 				
 			} else if(theOldColorSpace==ColorSpace.DEVICE_CMYK)
 			{
 				if(theColorSpace == ColorSpace.DEVICE_RGB)
 				{
 					this.convertCMYKtoRGB();
 				}
 				else //convert to Gray?
 				{
 					this.convertCMYKtoGRAY();
 				}
 			} else //used to be Gray
 			{
 				if(theColorSpace == ColorSpace.DEVICE_RGB)
 				{
 					this.convertGRAYtoRGB();
 				}
 				else //convert to CMYK?
 				{
 					this.convertGRAYtoCMYK();
 				}
 			}
 			this.colorSpace.setColorSpace(theColorSpace);
 		}
 	}
 	
 	public String getColorSpaceOut(boolean fillNotStroke)
 	{
 		StringBuffer p = new StringBuffer("");
 		
 		double tempDouble;
 		
 		if(this.colorSpace.getColorSpace()==ColorSpace.DEVICE_RGB)
 		{//colorspace is RGB
 				
 			//output RGB
 			if(fillNotStroke)
 			{ //fill
 				p.append(pdfNumber.doubleOut(this.red)+" "
 					+pdfNumber.doubleOut(this.green)+" "
 					+pdfNumber.doubleOut(this.blue)+" "
 					+" rg \n");
 			} 
 			else
 			{//stroke/border
 				p.append(pdfNumber.doubleOut(this.red)+" "
 					+pdfNumber.doubleOut(this.green)+" "
 					+pdfNumber.doubleOut(this.blue)+" "
 					+" RG \n");
 			}
 		}//end of output RGB
 		else if (this.colorSpace.getColorSpace() == ColorSpace.DEVICE_CMYK)
 		{//colorspace is CMYK
 			
 			if(fillNotStroke)
 			{ //fill
 				p.append(pdfNumber.doubleOut(this.cyan) + " "
 					+ pdfNumber.doubleOut(this.magenta) + " "
 					+ pdfNumber.doubleOut(this.yellow) + " "
 					+ pdfNumber.doubleOut(this.black) + " k \n");
 			}
 			else
 			{ //fill
 				p.append(pdfNumber.doubleOut(this.cyan) + " "
 					+ pdfNumber.doubleOut(this.magenta) + " "
 					+ pdfNumber.doubleOut(this.yellow) + " "
 					+ pdfNumber.doubleOut(this.black) + " K \n");
 			}
 			
 		}//end of if CMYK
 		else { //means we're in DeviceGray or Unknown.
 		//assume we're in DeviceGray, because otherwise we're screwed.
 		
 			if(fillNotStroke)
 			{
 				p.append(pdfNumber.doubleOut(this.black)	+ " g \n");
 			}
 			else
 			{
 				p.append(pdfNumber.doubleOut(this.black)	+ " G \n");
 			}
 		
 		}
 		return(p.toString());
 	}
 
 	
 	
 	
 	protected void convertCMYKtoRGB()
 	{
 		//convert CMYK to RGB
 		this.red = 1.0 - this.cyan;
 		this.green = 1.0 - this.green;
 		this.blue= 1.0 - this.yellow;
 				
 		this.red   = (this.black / this.blackFactor) + this.red;
 		this.green = (this.black / this.blackFactor) + this.green;
 		this.blue  = (this.black / this.blackFactor) + this.blue;
 		
 	}
 
 	protected void convertRGBtoCMYK()
 	{
 		//convert RGB to CMYK
 		this.cyan	= 1.0 - this.red;
 		this.magenta= 1.0 - this.green;
 		this.yellow = 1.0 - this.blue;
 				
 		this.black = 0.0;
 		/* If you want to calculate black, uncomment this
 		//pick the lowest color
 		tempDouble = this.red;
 				
 		if (this.green < tempDouble)
 			tempDouble = this.green;
 				
 		if (this.blue < tempDouble)
 			tempDouble = this.blue;
 					
 		this.black = tempDouble / this.blackFactor;
 		*/
 	}
 	
 	protected void convertGRAYtoRGB()
 	{
 		this.red  = 1.0 - this.black;
 		this.green= 1.0 - this.black;
 		this.blue = 1.0 - this.black;
 	}
 	
 	protected void convertGRAYtoCMYK()
 	{
 		this.cyan  =  this.black;
 		this.magenta= this.black;
 		this.yellow = this.black;
 		//this.black=0.0;//?
 	}
 	
 	protected void convertCMYKtoGRAY()
 	{
 		double tempDouble=0.0;
 		
 		//pick the lowest color
 		tempDouble = this.cyan;
 				
 		if (this.magenta < tempDouble)
 			tempDouble = this.magenta;
 				
 		if (this.yellow < tempDouble)
 			tempDouble = this.yellow;
 					
 		this.black = (tempDouble / this.blackFactor);
 		
 	}
 	
 	protected void convertRGBtoGRAY()
 	{
 		double tempDouble=0.0;
 		
 		//pick the lowest color
 		tempDouble = this.red;
 				
 		if (this.green < tempDouble)
 			tempDouble = this.green;
 				
 		if (this.blue < tempDouble)
 			tempDouble = this.blue;
 					
 		this.black = 1.0 - (tempDouble / this.blackFactor);
 		
 	}
 	
 	String toPDF()
 	{
 		return ("");
 
 	} //end of toPDF
 }
