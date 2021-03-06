 /**
  * Copyright (C) 2013 The CyanogenMod Project
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 
 package org.cyanogenmod.nemesis.widgets;
 
 import android.content.Context;
 import android.hardware.Camera;
 import android.view.View;
 
 import org.cyanogenmod.nemesis.CameraManager;
 import org.cyanogenmod.nemesis.R;
 
 /**
  * Skin Tone Enhancement widget (Qualcomm)
  * Most info (values, etc) from
  * https://codeaurora.org/cgit/quic/la/platform/packages/apps/Camera/commit/?id=9d48710275915ac0672cb7c1bc1b9aa14d70e8b0
  */
 public class SkinToneWidget extends WidgetBase {
     private static final String KEY_PARAMETER = "skinToneEnhancement";
     private static final int MIN_SCE_FACTOR = -10;
     private static final int MAX_SCE_FACTOR = +10;
 
     private WidgetOptionButton mMinusButton;
     private WidgetOptionButton mPlusButton;
     private WidgetOptionLabel mValueLabel;
 
 
     private class MinusClickListener implements View.OnClickListener {
         @Override
         public void onClick(View view) {
             setToneValue(Math.max(getToneValue() - 1, MIN_SCE_FACTOR));
         }
     }
 
     private class PlusClickListener implements View.OnClickListener {
         @Override
         public void onClick(View view) {
             setToneValue(Math.min(getToneValue() + 1, MAX_SCE_FACTOR));
         }
     }
 
     public SkinToneWidget(CameraManager cam, Context context) {
         super(cam, context, R.drawable.ic_widget_skintone);
 
         // Add views in the widget
         mMinusButton = new WidgetOptionButton(R.drawable.ic_widget_timer_minus, context);
         mPlusButton = new WidgetOptionButton(R.drawable.ic_widget_timer_plus, context);
         mValueLabel = new WidgetOptionLabel(context);
 
         mMinusButton.setOnClickListener(new MinusClickListener());
         mPlusButton.setOnClickListener(new PlusClickListener());
         mValueLabel.setText(Integer.toString(getToneValue()));
 
         addViewToContainer(mMinusButton);
         addViewToContainer(mValueLabel);
         addViewToContainer(mPlusButton);
     }
 
     @Override
     public boolean isSupported(Camera.Parameters params) {
         if (params.get(KEY_PARAMETER) != null) {
             return true;
         } else {
             return false;
         }
     }
 
     public int getToneValue() {
        return Integer.parseInt(mCamManager.getParameters().get(KEY_PARAMETER));
     }
 
     public void setToneValue(int value) {
         mCamManager.setParameterAsync(KEY_PARAMETER, Integer.toString(value));
         mValueLabel.setText(Integer.toString(value));
     }
 }
