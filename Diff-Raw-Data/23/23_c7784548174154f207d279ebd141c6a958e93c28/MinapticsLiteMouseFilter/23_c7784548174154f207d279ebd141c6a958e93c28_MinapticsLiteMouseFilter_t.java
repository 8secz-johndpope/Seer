 package net.minecraft.src;
 
 /*
             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
                     Version 2, December 2004 
 
  Copyright (C) 2004 Sam Hocevar <sam@hocevar.net> 
 
  Everyone is permitted to copy and distribute verbatim or modified 
  copies of this license document, and changing it is allowed as long 
  as the name is changed. 
 
             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION 
 
   0. You just DO WHAT THE FUCK YOU WANT TO. 
 */
 
 public class MinapticsLiteMouseFilter extends MouseFilter
 {
 	private float field_22388_a;
 	private float field_22387_b;
 	private float field_22389_c;
 	
 	private boolean forceMode;
 	private float forceValue;
 	
 	public MinapticsLiteMouseFilter()
 	{
 		this.forceMode = false;
 		this.forceValue = 0F;
 	}
 	
 	public void force(float value)
 	{
 		this.forceMode = true;
 		this.forceValue = value;
 		
 	}
 	
 	public void let()
 	{
 		this.forceMode = false;
 		
 	}
 	
 	@Override
	public float func_76333_a(float f, float f1)
 	{
 		if (this.forceMode)
 		{
 			f1 = this.forceValue;
 		}
 		
 		this.field_22388_a += f;
 		f = (this.field_22388_a - this.field_22387_b) * f1;
 		this.field_22389_c = this.field_22389_c + (f - this.field_22389_c) * 0.5F;
 		if (f > 0.0F && f > this.field_22389_c || f < 0.0F && f < this.field_22389_c)
 		{
 			f = this.field_22389_c;
 		}
 		this.field_22387_b += f;
 		return f;
 		
 	}
 }
