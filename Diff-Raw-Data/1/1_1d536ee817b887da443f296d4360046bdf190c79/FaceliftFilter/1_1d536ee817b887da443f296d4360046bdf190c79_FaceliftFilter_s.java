 /*
  * Copyright (C) 2010 The Android Open Source Project
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
  */
 
 package com.android.gallery3d.photoeditor.filters;
 
 import android.media.effect.Effect;
 import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
 
 import com.android.gallery3d.photoeditor.Photo;
 
 /**
  * Facelift filter applied to the image.
  */
 public class FaceliftFilter extends Filter {
 
     private float scale;
 
     /**
      * Sets the facelift level.
      *
      * @param scale ranges from 0 to 1.
      */
     public void setScale(float scale) {
         this.scale = scale;
         validate();
     }
 
     @Override
     public void process(EffectContext context, Photo src, Photo dst) {
         Effect effect = getEffect(context,
                 "com.google.android.media.effect.effects.FaceliftEffect");
         effect.setParameter("blend", scale);
         effect.apply(src.texture(), src.width(), src.height(), dst.texture());
     }
 }
