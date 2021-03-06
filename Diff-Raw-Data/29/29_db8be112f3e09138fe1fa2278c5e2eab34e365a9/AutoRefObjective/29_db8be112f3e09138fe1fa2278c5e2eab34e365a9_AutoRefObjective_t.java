 package org.mctourney.autoreferee.goals.scoreboard;
 
 import java.util.Collections;
 import java.util.Set;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.OfflinePlayer;
 import org.bukkit.scoreboard.Objective;
 
 import org.mctourney.autoreferee.AutoRefTeam;
 import org.mctourney.autoreferee.goals.AutoRefGoal;
 
 import com.google.common.collect.Sets;
 
 public abstract class AutoRefObjective
 {
 	protected Objective objective;
 	protected OfflinePlayer title;
 
 	protected AutoRefTeam team;
 	protected Set<AutoRefGoal> goals;
 
 	protected ChatColor color = null;
 	protected String name;
 	protected int value = 0;
 
	// does this objective need to be set non-zero still
	private boolean _needsZeroFix = true;

 	public AutoRefObjective(Objective objective, AutoRefTeam team, String name, int value, ChatColor color)
 	{
 		// reference to the actual scoreboard objective where we drop our entries
 		assert objective != null : "Objective cannot be null";
 		this.objective = objective;
 
 		// save the owning team
 		this.team = team;
 		this.color = color;
 		this.goals = Sets.newHashSet();
 
 		// objective name and value
 		this.setName(name);
 		this.setValue(value);
 	}
 
 	public AutoRefObjective(Objective objective, AutoRefTeam team, String name, int value)
 	{ this(objective, team, name, value, team.getColor()); }
 
 	public abstract void update();
 
 	public void setName(String name)
 	{
 		this.name = name;

		// is this name colorable?
		boolean colorable = this.color == null || name.length() > 14;
		String clr = colorable ? "" : this.color.toString();
 
 		// if we need to replace the title object, do so
 		if (this.title == null || !this.title.getName().equals(clr + name))
 		{
 			if (this.title != null)
 				this.objective.getScoreboard().resetScores(this.title);
 			this.title = Bukkit.getOfflinePlayer(clr + name);
			this._needsZeroFix = true;
 		}
 
 		// no matter what, update the score
		this.setValue(this.value);
 	}
 
 	public String getName()
 	{ return this.name; }
 
 	public void setValue(int value)
 	{
 		this.value = value;
 
 		// set to 1 first to try to force zeroes to show up
		if (this.value == 0 && this._needsZeroFix)
			this.objective.getScore(this.title).setScore(1);
 
 		// set the correct value
		this._needsZeroFix = false;
 		this.objective.getScore(this.title).setScore(this.value);
 	}
 
 	public int getValue()
 	{ return this.value; }
 
 	public void setColor(ChatColor color)
 	{
 		if (color == ChatColor.RESET) color = null;
 		this.color = color; this.setName(this.getName());
 	}
 
 	public ChatColor getColor()
 	{ return this.color; }
 
 	public Set<AutoRefGoal> getGoals()
 	{ return Collections.unmodifiableSet(goals); }
 
 	@Override
 	public String toString()
 	{ return String.format("%s[%s=%d]", this.getClass().getSimpleName(), this.getName(), this.getValue()); }
 
 	public static Set<AutoRefObjective> fromTeam(Objective objective, AutoRefTeam team)
 	{
 		Set<AutoRefObjective> objectives = Sets.newHashSet();
 
 		objectives.addAll(BlockObjective.fromTeam(objective, team));
 		objectives.addAll(SurvivalObjective.fromTeam(objective, team));
 		objectives.addAll(ScoreObjective.fromTeam(objective, team));
 
 		return objectives;
 	}
 }
