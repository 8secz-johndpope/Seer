 package me.chaseoes.tf2;
 
 import org.bukkit.ChatColor;
 import org.bukkit.OfflinePlayer;
 import org.bukkit.scoreboard.DisplaySlot;
 import org.bukkit.scoreboard.Objective;
 import org.bukkit.scoreboard.Score;
 import org.bukkit.scoreboard.Scoreboard;
 import org.bukkit.scoreboard.ScoreboardManager;
 import org.bukkit.scoreboard.Team;
 
 public class GameScoreboard {
 
 	Game game;
 	ScoreboardManager manager;
 	Scoreboard board;
 	Team red;
 	Team blue;
 	Objective objective;
 
 	public GameScoreboard(Game g) {
 		game = g;
 		manager = TF2.getInstance().getServer().getScoreboardManager();
 		board = manager.getNewScoreboard();
 		red = board.registerNewTeam(me.chaseoes.tf2.Team.RED.getName());
 		blue = board.registerNewTeam(me.chaseoes.tf2.Team.BLUE.getName());
 		red.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + me.chaseoes.tf2.Team.RED.getName());
 		blue.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + me.chaseoes.tf2.Team.BLUE.getName());
 		red.setCanSeeFriendlyInvisibles(true);
 		blue.setCanSeeFriendlyInvisibles(true);
        red.setPrefix(ChatColor.RED + "");
        blue.setPrefix(ChatColor.BLUE + "");
 		objective = board.registerNewObjective("test", "dummy");
 		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
 		objective.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "TF2 Kills");
 	}
 
 	public void addPlayer(GamePlayer gp) {
 		if (gp.getTeam() == me.chaseoes.tf2.Team.RED) {
 			red.addPlayer(getPlayer(gp));
 		} else {
 			blue.addPlayer(getPlayer(gp));
 		}
         updateBoard();
 	}
 
     public void removePlayer(GamePlayer gp) {
         if (gp.getTeam() == me.chaseoes.tf2.Team.RED) {
             red.removePlayer(getPlayer(gp));
         } else {
             blue.removePlayer(getPlayer(gp));
         }
         gp.getPlayer().setScoreboard(manager.getMainScoreboard());
         updateBoard();
     }
 
 	public void updateBoard() {
 		for (GamePlayer gp : game.playersInGame.values()) {
             if (!gp.getPlayer().getScoreboard().equals(board)) {
                 gp.getPlayer().setScoreboard(board);
             }
 			Score score = objective.getScore(getPlayer(gp));
 			score.setScore(gp.getTotalKills());
 		}
 	}
 
 	public void resetScores() {
 		for (GamePlayer gp : game.playersInGame.values()) {
 			board.resetScores(getPlayer(gp));
 		}
 	}
 
 	public void remove() {
 		resetScores();
 		for (GamePlayer gp : game.playersInGame.values()) {
 				red.removePlayer(getPlayer(gp));
 				blue.removePlayer(getPlayer(gp));
 			gp.getPlayer().setScoreboard(manager.getMainScoreboard());
 		}
 	}
 
 	private OfflinePlayer getPlayer(GamePlayer gp) {
		return TF2.getInstance().getServer().getOfflinePlayer(gp.getPlayer().getName());
 	}
 
 }
