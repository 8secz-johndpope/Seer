 package com.pixelthieves.pokemontd.graphics.ui.menu;
 
 import com.badlogic.gdx.Gdx;
 import com.badlogic.gdx.audio.Music;
 import com.badlogic.gdx.math.Rectangle;
 import com.pixelthieves.pokemontd.graphics.ui.Button;
 
 /**
  * Created by Tomas on 11/19/13.
  */
 public class Help extends ChildTab {
 
     private final Button tutorialButton;
     private final Button towersButton;
     private final Tutorial tutorialTab;
     private final AbilityTab abilityTab;
 
     Help(final Menu menu, MenuTab parent, Rectangle rectangle, int count) {
         super(menu, parent, rectangle, count);
 
         tutorialTab = new Tutorial(menu,  this, new Rectangle(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
         abilityTab = new AbilityTab(menu, this, rectangle, count);
         tutorialButton = new MenuButton(menu, rects.get(0)) {
             @Override
             public void process(float x, float y) {
                 menu.switchCard(tutorialTab);
             }
         };
 
         towersButton = new MenuButton(menu, rects.get(1)) {
             @Override
             public void process(float x, float y) {
                 menu.switchCard(abilityTab);
             }
         };
 
         register(tutorialButton);
         register(towersButton);
        cards.add(tutorialTab);
         cards.add(abilityTab);
     }
 
     @Override
     public void render() {
         super.render();
         tutorialButton.render("TUTORIAL");
         towersButton.render("TOWERS");
     }
 }
