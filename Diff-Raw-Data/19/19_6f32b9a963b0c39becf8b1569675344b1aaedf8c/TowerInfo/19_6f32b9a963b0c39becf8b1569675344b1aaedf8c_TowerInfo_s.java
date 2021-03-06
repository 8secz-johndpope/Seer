 package com.xkings.pokemontd.graphics.ui;
 
 import com.badlogic.gdx.graphics.Color;
 import com.badlogic.gdx.graphics.g2d.BitmapFont;
 import com.badlogic.gdx.graphics.g2d.SpriteBatch;
 import com.badlogic.gdx.graphics.g2d.TextureAtlas;
 import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
 import com.badlogic.gdx.math.Rectangle;
 import com.xkings.core.main.Assets;
 import com.xkings.pokemontd.Treasure;
 import com.xkings.pokemontd.component.attack.AbilityComponent;
 import com.xkings.pokemontd.component.attack.EffectName;
 import com.xkings.pokemontd.component.attack.projectile.HitAbility;
 import com.xkings.pokemontd.component.attack.projectile.data.EffectData;
 import com.xkings.pokemontd.component.attack.projectile.data.NormalData;
 import com.xkings.pokemontd.entity.tower.TowerType;
 
 /**
  * User: Seda
  * Date: 24.10.13
  * Time: 20:42
  */
 
 public class TowerInfo extends CommonInfo {
     public static final Color SELL_COLOR = new Color(Color.RED).mul(0.6f);
     public static final Color BUY_COLOR = new Color(Color.GREEN).mul(0.6f);
     public static final Color ABILITY_COLOR = new Color(Color.GREEN).mul(0.6f);
     protected final DisplayText damage;
     protected final DisplayText speed;
     protected final DisplayText range;
     protected final Button sell;
     protected final Button buy;
     private final TowerCost cost;
     private final Icon ability;
     protected TowerType tower;
     private float damageCache;
     private float speedCache;
     private float rangeCache;
     private boolean sellCache;
     private boolean buyCache;
     private Treasure costCache;
     private Color damageColorCache;
     private Color speedColorCache;
     private Color rangeColorCache;
     private EffectData abilityCache;
     private EffectName effectNameCache;
 
     /**
      * public constuctor makes 3 text rectangles uses class DisplayText (damage,range,speed).
      * Makes two anonymous classes  for buttons buy and sell these anonymous classes use public method process which
      * allows sell or upgrade tower and because class TowerInfo extends class CommonInfo (which implements Clickable)
      * there are clickables.add(buy) and clickables.add(sell) which makes function buy and sell.
      *
      * @param ui
      * @param rectangle
      * @param shapeRenderer
      * @param spriteBatch
      */
     public TowerInfo(final Ui ui, Rectangle rectangle, ShapeRenderer shapeRenderer, SpriteBatch spriteBatch,
                      BitmapFont font) {
         super(ui, rectangle, shapeRenderer, spriteBatch, font);
         float offset = height / 5;
         float offsetBlocks = height / 2;
         float offsetBlocks2 = offsetBlocks / 2f;
         cost = new TowerCost(new Rectangle(x + offset, y, width - offset * 2, offset), width - offsetBlocks,
                 shapeRenderer, spriteBatch, font);
         damage = new DisplayText(ui, new Rectangle(x + offset * 5, y + offset * 3, offset * 2, offset), font,
                 BitmapFont.HAlignment.LEFT);
         speed = new DisplayText(ui, new Rectangle(x + offset * 5, y + offset * 2, offset * 2, offset), font,
                 BitmapFont.HAlignment.LEFT);
         range = new DisplayText(ui, new Rectangle(x + offset * 5, y + offset, offset, offset), font,
                 BitmapFont.HAlignment.LEFT);
 
         ability = new Icon(ui, damage.x + damage.width + offsetBlocks2, y + offsetBlocks2, height - offsetBlocks,
                 height - offsetBlocks) {
 
             @Override
             public void process(float x, float y) {
                 ui.getAbilityInfo().update(abilityCache, speedCache, damageCache);
                 //ui.abilityText();
                 // ui.getRectangle();
             }
         };
         sell = new Button(ui, new Rectangle(x + width - offsetBlocks, y, offsetBlocks, offsetBlocks), font,
                 BitmapFont.HAlignment.CENTER) {
             @Override
             public void process(float x, float y) {
                 ui.getTowerManager().sellTower();
             }
         };
         buy = new Button(ui, new Rectangle(x + width - offsetBlocks, y + offsetBlocks, offsetBlocks, offsetBlocks),
                 font, BitmapFont.HAlignment.CENTER) {
             @Override
             public void process(float x, float y) {
                 ui.getTowerManager().buyNewOrUpgrade();
             }
         };
         ui.register(sell);
         ui.register(buy);
         ui.register(ability);
         clickables.add(sell);
         clickables.add(buy);
         clickables.add(ability);
     }
 
     /**
      * this method overrides method render in class CommonInfo and setted buttons sell and buy
      */
     @Override
     public void render() {
         this.sell.setEnabled(sellCache);
         this.buy.setEnabled(buyCache);
         this.ability.setEnabled(abilityCache != null);
 
         super.render();
         this.damage.render("DMG: " + (int) (damageCache), damageColorCache);
         this.speed.render("SPD: " + (int) (speedCache), speedColorCache);
         this.range.render("RNG: " + (int) (rangeCache), rangeColorCache);
         this.cost.render(costCache);
         this.sell.render("sell", Color.WHITE, SELL_COLOR);
         this.buy.render("buy", Color.WHITE, BUY_COLOR);
         if (abilityCache != null) {
             this.ability.render(Assets.getTexture("abilities/" + effectNameCache.name().toLowerCase()),
                     abilityCache.getEffect());
         }
     }
 
     public void render(TextureAtlas.AtlasRegion region, Treasure cost, String name, boolean sell, boolean buy) {
         render(region, 0f, 0f, 0f, cost, name, null, null, sell, buy);
     }
 
     public void render(TextureAtlas.AtlasRegion region, float damage, float speed, float range, Treasure cost,
                        String name, EffectName effectName, EffectData ability, boolean sell, boolean buy) {
         render(region, damage, Color.WHITE, speed, Color.WHITE, range, Color.WHITE, cost, name, effectName, ability,
                 sell, buy);
     }
 
     public void render(TextureAtlas.AtlasRegion region, float damage, Color damageColor, float speed, Color speedColor,
                        float range, Color rangeColor, Treasure cost, String name, EffectName effectName,
                        EffectData ability, boolean sell, boolean buy) {
         this.damageCache = damage;
         this.damageColorCache = damageColor;
         this.speedCache = speed;
         this.speedColorCache = speedColor;
         this.rangeCache = range;
         this.rangeColorCache = rangeColor;
         this.costCache = cost;
         this.effectNameCache = effectName;
         this.abilityCache = ability;
         this.sellCache = sell;
         this.buyCache = buy;
         render(region, name);
     }
 
     @Override
     public void process(float x, float y) {
         //To change body of implemented methods use File | Settings | File Templates.
     }
 
     @Override
     public void refresh() {
         super.refresh();
         float offset = height / 5;
         float offsetBlocks = height / 2;
         float offsetBlocks2 = offsetBlocks / 2f;
         cost.set(x + offset, y, width - offset * 2, offset);
         damage.set(x + offset * 5, y + offset * 3, offset * 2, offset);
         speed.set(x + offset * 5, y + offset * 2, offset * 2, offset);
         range.set(x + offset * 5, y + offset, offset, offset);
         ability.set(damage.x + damage.width + offsetBlocks2, y + offsetBlocks2, height - offsetBlocks,
                 height - offsetBlocks);
        ability.set(x + offset * 10f, y + offset * 3, offsetBlocks * 1.2f, offsetBlocks / 2f);
         sell.set(x + width - offsetBlocks, y, offsetBlocks, offsetBlocks);
         buy.set(x + width - offsetBlocks, y + offsetBlocks, offsetBlocks, offsetBlocks);
 
         cost.refresh();
         damage.refresh();
         speed.refresh();
         range.refresh();
         ability.refresh();
         sell.refresh();
         buy.refresh();
     }
 
     @Override
     public void setEnabled(boolean enabled) {
         super.setEnabled(enabled);
     }
 
     protected EffectData getAbility(TowerType tower) {
         AbilityComponent attack = tower.getAttack();
         return attack instanceof HitAbility ? getEffectData((HitAbility) attack) : (EffectData) attack;
     }
 
     private EffectData getEffectData(HitAbility attack) {
         for (EffectData effectData : attack.getEffectData()) {
             if (!(effectData instanceof NormalData)) {
                 return effectData;
             }
         }
         return null;
     }
 }
