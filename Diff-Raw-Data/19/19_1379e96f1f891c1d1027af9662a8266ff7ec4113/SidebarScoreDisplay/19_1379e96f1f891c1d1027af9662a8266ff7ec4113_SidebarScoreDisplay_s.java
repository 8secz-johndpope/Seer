 /*
  * @author     ucchy
  * @license    GPLv3
  * @copyright  Copyright ucchy 2013
  */
 package com.github.ucchyocean.ct.scoreboard;
 
 import java.util.ArrayList;
 import java.util.Enumeration;
 import java.util.Hashtable;
 
 import org.bukkit.ChatColor;
 import org.bukkit.entity.Player;
 import org.bukkit.scoreboard.DisplaySlot;
 import org.bukkit.scoreboard.Objective;
 import org.bukkit.scoreboard.Scoreboard;
 import org.bukkit.scoreboard.Team;
 
 import com.github.ucchyocean.ct.ColorTeaming;
 import com.github.ucchyocean.ct.ColorTeamingConfig;
 
 /**
  * @author ucchy
  * サイドバーにスコアを表示するためのAPIクラス
  */
 public class SidebarScoreDisplay {
 
     private Objective objective;
     private Hashtable<String, SidebarTeamScore> teamscores;
 
     /**
      * コンストラクタ。コンストラクト時に、現在のチーム状況を取得し、
      * サイドバーを初期化、表示する。
      */
     public SidebarScoreDisplay() {
 
         Scoreboard scoreboard = ColorTeaming.getScoreboard();
         objective = scoreboard.registerNewObjective("teamscore", "");
         objective.setDisplayName("チームスコア");
 
         teamscores = new Hashtable<String, SidebarTeamScore>();
 
         Hashtable<String, ArrayList<Player>> members = ColorTeaming.getAllTeamMembers();
         Enumeration<String> keys = members.keys();
         while ( keys.hasMoreElements() ) {
             String key = keys.nextElement();
             Team team = scoreboard.getTeam(key);
             SidebarTeamScore ts = new SidebarTeamScore(team);
             objective.getScore(ts).setScore(0);
             teamscores.put(key, ts);
         }
 
         objective.setDisplaySlot(DisplaySlot.SIDEBAR);
 
         refreshCriteria();
     }
 
     /**
      * サイドバーのクライテリアを、ColorTeamingConfigから取得し、更新する。
      */
     private void refreshCriteria() {
 
         SidebarCriteria criteria = ColorTeamingConfig.teamCriteria;
 
         if ( criteria == SidebarCriteria.NONE ) {
             if ( ColorTeaming.sidebarScore != null ) {
                 ColorTeaming.sidebarScore.remove();
                 ColorTeaming.sidebarScore = null;
                 return;
             }
         }
 
         objective.setDisplayName(
                ChatColor.ITALIC.toString() + ChatColor.BLUE.toString() +
                 criteria.getSidebarTitle() + ChatColor.RESET);
 
         refreshScore();
     }
 
     /**
      * スコアを再取得し、表示更新する。
      * スコアが更新されるタイミング（プレイヤー死亡時、ログアウト時）に、
      * 本メソッドを呼び出してスコア表示を更新すること。
      */
     public void refreshScore() {
 
         switch (ColorTeamingConfig.teamCriteria) {
         case KILL_COUNT:
         case DEATH_COUNT:
             refreshScoreByKillOrDeathCount(ColorTeamingConfig.teamCriteria);
             break;
         case POINT:
             refreshScoreByPoint();
             break;
         case REST_PLAYER:
             refreshScoreByRestPlayerCount();
             break;
         }
     }
 
     /**
      * キル数、または、デス数による、スコア更新を行う
      * @param criteria
      */
     private void refreshScoreByKillOrDeathCount(SidebarCriteria criteria) {
 
         int index;
         if ( criteria == SidebarCriteria.KILL_COUNT ) {
             index = 0;
         } else {
             index = 1;
         }
 
         Hashtable<String, ArrayList<Player>> members = ColorTeaming.getAllTeamMembers();
         Enumeration<String> keys = members.keys();
         while ( keys.hasMoreElements() ) {
             String key = keys.nextElement();
 
             if ( teamscores.containsKey(key) ) {
                 SidebarTeamScore team = teamscores.get(key);
                 if ( ColorTeaming.killDeathCounts.containsKey(key) ) {
                     int[] data = ColorTeaming.killDeathCounts.get(key);
                     objective.getScore(team).setScore(data[index]);
                 } else {
                     objective.getScore(team).setScore(0);
                 }
             }
         }
     }
 
     /**
      * ポイントによるスコア更新を行う
      */
     private void refreshScoreByPoint() {
 
         Hashtable<String, ArrayList<Player>> members = ColorTeaming.getAllTeamMembers();
         Enumeration<String> keys = members.keys();
         while ( keys.hasMoreElements() ) {
             String key = keys.nextElement();
 
             if ( teamscores.containsKey(key) ) {
                 SidebarTeamScore team = teamscores.get(key);
                 if ( ColorTeaming.killDeathCounts.containsKey(key) ) {
                     int[] data = ColorTeaming.killDeathCounts.get(key);
                     int point = data[0] * ColorTeamingConfig.killPoint +
                             data[1] * ColorTeamingConfig.deathPoint +
                             data[2] * ColorTeamingConfig.tkPoint;
                     objective.getScore(team).setScore(point);
                 } else {
                     objective.getScore(team).setScore(0);
                 }
             }
         }
     }
 
     /**
      * 残り人数によるスコア更新を行う
      */
     private void refreshScoreByRestPlayerCount() {
 
         Hashtable<String, ArrayList<Player>> members = ColorTeaming.getAllTeamMembers();
         Enumeration<String> keys = members.keys();
         while ( keys.hasMoreElements() ) {
             String key = keys.nextElement();
 
             if ( teamscores.containsKey(key) ) {
                 SidebarTeamScore team = teamscores.get(key);
                 int rest = members.get(key).size();
                 objective.getScore(team).setScore(rest);
             }
         }
     }
 
     /**
      * サイドバーの表示を消去する。
      */
     public void remove() {
 
         ColorTeaming.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
         objective.unregister();
     }
 }
