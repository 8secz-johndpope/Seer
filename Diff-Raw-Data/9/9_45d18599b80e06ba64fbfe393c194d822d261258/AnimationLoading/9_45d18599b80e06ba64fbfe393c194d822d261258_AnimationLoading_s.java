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
 
 package com.example.android.apis.animation;
 
 // Need the following import to get access to the app resources, since this
 // class is in a sub-package.
 import android.animation.AnimatorInflater;
 import android.animation.AnimatorSet;
 import android.animation.ObjectAnimator;
 import com.example.android.apis.R;
 
 import java.util.ArrayList;
 
 import android.animation.Animator;
 import android.animation.ValueAnimator;
 import android.app.Activity;
 import android.content.Context;
 import android.graphics.Canvas;
 import android.graphics.Paint;
 import android.graphics.RadialGradient;
 import android.graphics.Shader;
 import android.graphics.drawable.ShapeDrawable;
 import android.graphics.drawable.shapes.OvalShape;
 import android.os.Bundle;
 import android.view.View;
 import android.widget.Button;
 import android.widget.LinearLayout;
 
 /**
  * This application demonstrates loading Animator objects from XML resources.
  */
 public class AnimationLoading extends Activity {
 
     private static final int DURATION = 1500;
 
     /** Called when the activity is first created. */
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.animation_loading);
         LinearLayout container = (LinearLayout) findViewById(R.id.container);
         final MyAnimationView animView = new MyAnimationView(this);
         container.addView(animView);
 
         Button starter = (Button) findViewById(R.id.startButton);
         starter.setOnClickListener(new View.OnClickListener() {
 
             @Override
             public void onClick(View v) {
                 animView.startAnimation();
             }
         });
 
     }
 
     public class MyAnimationView extends View implements ValueAnimator.AnimatorUpdateListener {
 
         private static final float BALL_SIZE = 100f;
 
         public final ArrayList<ShapeHolder> balls = new ArrayList<ShapeHolder>();
         Animator animation = null;
 
         public MyAnimationView(Context context) {
             super(context);
             addBall(100, 0);
             addBall(250, 0);
             addBall(400, 0);
         }
 
         private void createAnimation() {
             if (animation == null) {
                 ObjectAnimator anim =
                         (ObjectAnimator) AnimatorInflater.
                                loadAnimator(getApplicationContext(), R.anim.property_animator);
                 anim.addUpdateListener(this);
                 anim.setTarget(balls.get(0));
 
                 ValueAnimator fader =
                         (ValueAnimator) AnimatorInflater.loadAnimator(getApplicationContext(),
                         R.anim.animator);
                 fader.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                     public void onAnimationUpdate(ValueAnimator animation) {
                         balls.get(1).setAlpha((Float) animation.getAnimatedValue());
                     }
                 });
 
                 AnimatorSet seq =
                         (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                         R.anim.animator_set);
                 seq.setTarget(balls.get(2));
 
                 animation = new AnimatorSet();
                 ((AnimatorSet) animation).playTogether(anim, fader, seq);
             }
         }
 
         public void startAnimation() {
             createAnimation();
             animation.start();
         }
 
         private ShapeHolder addBall(float x, float y) {
             OvalShape circle = new OvalShape();
             circle.resize(BALL_SIZE, BALL_SIZE);
             ShapeDrawable drawable = new ShapeDrawable(circle);
             ShapeHolder shapeHolder = new ShapeHolder(drawable);
             shapeHolder.setX(x);
             shapeHolder.setY(y);
             int red = (int)(100 + Math.random() * 155);
             int green = (int)(100 + Math.random() * 155);
             int blue = (int)(100 + Math.random() * 155);
             int color = 0xff000000 | red << 16 | green << 8 | blue;
             Paint paint = drawable.getPaint();
             int darkColor = 0xff000000 | red/4 << 16 | green/4 << 8 | blue/4;
             RadialGradient gradient = new RadialGradient(37.5f, 12.5f,
                     50f, color, darkColor, Shader.TileMode.CLAMP);
             paint.setShader(gradient);
             shapeHolder.setPaint(paint);
             balls.add(shapeHolder);
             return shapeHolder;
         }
 
         @Override
         protected void onDraw(Canvas canvas) {
             for (ShapeHolder ball : balls) {
                 canvas.translate(ball.getX(), ball.getY());
                 ball.getShape().draw(canvas);
                 canvas.translate(-ball.getX(), -ball.getY());
             }
         }
 
         public void onAnimationUpdate(ValueAnimator animation) {
 
             invalidate();
             ShapeHolder ball = balls.get(0);
             ball.setY((Float)animation.getAnimatedValue());
         }
     }
 }
