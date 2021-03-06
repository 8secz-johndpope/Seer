 package GUI;
 
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.Graphics;
 import java.awt.GridLayout;
 import java.awt.Image;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.io.BufferedReader;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
 import java.net.URL;
 import java.net.URLConnection;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Iterator;
 
 
 import javax.swing.Box;
 import javax.swing.BoxLayout;
 import javax.swing.ImageIcon;
 import javax.swing.JButton;
 import javax.swing.JLabel;
 import javax.swing.JPanel;
 import javax.swing.border.TitledBorder;
 
 
 import Character.PlayerCharacter;
 import Statistics.Statistics;
 
 
 /**
  * A class for providing a panel for showing statistics.
  * It will display both the players current statistics and a common highscore, which is saved on a server.
  * It takes care of both loading and saving the highscores.
  * @author Johan
  *
  */
 public class StatisticsPanel extends JPanel {
 
 	private static final String PANEL_BACKGROUND = "images/gui/empty_panel.png";	
 	private PlayerCharacter player;
 	private JLabel monstersKilled;
 	private JLabel questsCompleted;
 	private JLabel totalScore;
 	private JLabel highscore1;
 	private JLabel highscore2;
 	private JLabel highscore3;
 	
 	
 	private JPanel panelWest;
 	private JPanel panelEast;
 	
 	
 	public StatisticsPanel(PlayerCharacter player)
 	{
 		this.player = player;
 		
 		setPanelDetails();
 		createStatisticsPanel();
 		
 		// Add a KeyListener to this window when active
 		addKeyListener(new KeyAdapter(){
 			@Override
 			public void keyPressed(KeyEvent e) {
 				setVisible(false);
 			}
 		});
 	}
 	
 	public void createStatisticsPanel()
 	{
 		createWestPanel();
 		createEastPanel();
 	}
 	
 	
 	private void createWestPanel()
 	{
 		panelWest = new JPanel();
 		panelWest.setLayout(new BoxLayout(panelWest, BoxLayout.Y_AXIS));
 		
 		TitledBorder westBorder = new TitledBorder("Your current stats");
 		westBorder.setTitleColor(Color.gray);
 		panelWest.setBorder(westBorder);
 		panelWest.setOpaque(false);
 		
 		panelWest.add(Box.createRigidArea(new Dimension(20,35)));
 		
 		monstersKilled = new JLabel("Monsters killed: " + player.getStatistics().getMonstersKilled());
 		monstersKilled.setForeground(Color.white);
 		panelWest.add(monstersKilled);
 		
 		questsCompleted = new JLabel("Quests completed: " + player.getStatistics().getQuestsCompleted());
 		questsCompleted.setForeground(Color.white);	
 		panelWest.add(questsCompleted);
 		
 		panelWest.add(Box.createRigidArea(new Dimension(0, 20)));
 		
 		totalScore = new JLabel("Your total score is: " + (player.getStatistics().getMonstersKilled() + (player.getStatistics().getQuestsCompleted() * 5)));
 		totalScore.setForeground(Color.white);
 		panelWest.add(totalScore);
 		
 		
 		panelWest.add(Box.createGlue());
 		
 		JButton submitButton = new JButton("Submit highscore");
 		submitButton.addActionListener(new ActionListener(){
 			public void actionPerformed(ActionEvent e)
 			{
 				submitScore();
 			}
 		});
 		panelWest.add(submitButton);
 		panelWest.add(Box.createRigidArea(new Dimension(0, 40)));
 		
 		add(panelWest);
 	}
 	
 	private void createEastPanel()
 	{
 		panelEast = new JPanel();
 		panelEast.setLayout(new BoxLayout(panelEast, BoxLayout.Y_AXIS));
 		panelEast.setOpaque(false);
 		TitledBorder eastBorder = new TitledBorder("Highscore");
 		eastBorder.setTitleColor(Color.gray);
 		panelEast.setBorder(eastBorder);
 		
 		
 		panelEast.add(Box.createRigidArea(new Dimension(0,15)));
 		
 		highscore1 = new JLabel();
 		highscore1.setForeground(Color.white);
 		TitledBorder highscoreBorder1 = new TitledBorder("1");
 		highscoreBorder1.setTitleColor(Color.gray);
 		highscore1.setBorder(highscoreBorder1);
 		
 		panelEast.add(highscore1);
 		
 		highscore2 = new JLabel();
 		highscore2.setForeground(Color.white);	
 		TitledBorder highscoreBorder2 = new TitledBorder("2");
 		highscoreBorder2.setTitleColor(Color.gray);
 		highscore2.setBorder(highscoreBorder2);
 		panelEast.add(highscore2);
 		
 		highscore3 = new JLabel();
 		highscore3.setForeground(Color.white);
 		TitledBorder highscoreBorder3 = new TitledBorder("3");
 		highscoreBorder3.setTitleColor(Color.gray);
 		highscore3.setBorder(highscoreBorder3);
 		panelEast.add(highscore3);
 		
 		panelEast.add(Box.createGlue());
 		
 		JButton refreshButton = new JButton("Refresh");
 		refreshButton.addActionListener(new ActionListener(){
 			public void actionPerformed(ActionEvent e)
 			{
 				refreshScore();
 			}
 		});
 		
 		panelEast.add(refreshButton);
 		
 		panelEast.add(Box.createRigidArea(new Dimension(0, 40)));
 		
 		add(panelEast);
 		refreshScore();
 	}
 	
 	private void refreshScore()
 	{	
 		try
 		{
 			String data = URLEncoder.encode("player_name", "UTF-8") + "=" + URLEncoder.encode("refresh", "UTF-8");
 		    data += "&" + URLEncoder.encode("player_score", "UTF-8") + "=" + URLEncoder.encode(""+ 0, "UTF-8");
 		    data += "&" + URLEncoder.encode("identifier", "UTF-8") + "=" + URLEncoder.encode("gamecontroler", "UTF-8");
 			data += "&" + URLEncoder.encode("submit", "UTF-8") + "=" + URLEncoder.encode("true", "UTF-8");
 			
 			URLConnection connection = openUrl();
 			connection.setDoOutput(true);
 			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
 			
 			
 			out.write(data);
 			out.flush();
 			
 			updateScore(connection);
 		
 		    out.close();
 			
 		}
 		catch(Exception e)
 		{
 			e.printStackTrace();
 		}
 	
 	requestFocusInWindow();
 }
 	
 	private void submitScore()
 	{
 		try
 		{
 			String data = URLEncoder.encode("player_name", "UTF-8") + "=" + URLEncoder.encode(player.getName(), "UTF-8");
 	        data += "&" + URLEncoder.encode("player_score", "UTF-8") + "=" + URLEncoder.encode("" + player.getStatistics().getTotalScore(), "UTF-8");
 	        data += "&" + URLEncoder.encode("identifier", "UTF-8") + "=" + URLEncoder.encode("gamecontroler", "UTF-8");
 			data += "&" + URLEncoder.encode("submit", "UTF-8") + "=" + URLEncoder.encode("true", "UTF-8");
 			
 			URLConnection connection = openUrl();
 			connection.setDoOutput(true);
 			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
 			
 			
 			out.write(data);
 			out.flush();
 			
 			updateScore(connection);
 
 	        out.close();
 			
 		}
 		catch(Exception e)
 		{
 			e.printStackTrace();
 		}
 		
 		requestFocusInWindow();
 	}
 	
 	
 	
 	private void updateScore(URLConnection connection)
 	{
 		try
 		{
 			
 			BufferedReader rd = new BufferedReader(new
 					InputStreamReader(connection.getInputStream()));
 	      
 			ArrayList<Score> list = new ArrayList<>();
 			
 	        String line = rd.readLine();
 	        line = rd.readLine();
 	        while (line != null && line.startsWith("Player")) {
 	            String lines[] = line.split("--");
 	        	
 	            String name = lines[0];
 	        	int score = Integer.parseInt(lines[1].split(": ")[1].split(" ")[0]);
 	        	String time = lines[2];
 	        	
 	        	list.add(new Score(name, score, time));
 	        	
 	        	
 	        	
 				line = rd.readLine();
 	        }
 	        rd.close();
 	        
 	        
 			Score[] topScores = sortScore(list);
 			highscore1.setText("" + topScores[0].getText());
 			highscore2.setText("" + topScores[1].getText());
 			highscore3.setText("" + topScores[2].getText());
 	        
 			
 		}
 		catch(Exception e)
 		{
 			highscore1.setText("Error loading highscores, try again later");
 			e.printStackTrace();
 		}
 	}
 	
 	public Score[] sortScore(ArrayList<Score> list)
 	{
 		Score[] topScores = new Score[3];
		int max = 0;
 		
		if(list.size() >= 3)
		{
			max = 3;
		}
		else
		{
			max = list.size();
			for(int i = 0; i < list.size(); i++)
			{
				topScores[2-i] = new Score("No score", 0, "No time");
			}
			
		}
		
		for(int i = 0; i < max; i++)
 		{
 			Iterator<Score> it = list.iterator();
 			int savedIndex = 0;
 			if(it.hasNext())
 			{
 				topScores[i] = it.next();
 				int index = 1;
 				
 				while(it.hasNext())
 				{
 					Score score = it.next();
 					if(score.getScore() > topScores[i].getScore())
 					{
 						topScores[i] = score;
 						savedIndex = index;
 					}
 					index++;
 				}
 			}
 			list.remove(savedIndex);
 		}
 		
		
		
 		return topScores;
 		
 	}
 	
 	
 	private URLConnection openUrl()
 	{
 		try
 		{
 			URL url = new URL("http://jpv-net.dyndns.org:1337/red_elephant/submitt_score.php");
 	        return url.openConnection();
 			
 		}
 		catch(Exception e)
 		{
 			System.out.println("Error loading url");
 			e.printStackTrace();
 		}
 		return null;
 	}
 	
 	
 	/**
 	 * Sets this panels default visuals
 	 */
 	private void setPanelDetails(){
 		setOpaque(true);
 		setDoubleBuffered(true);
 		setBounds(150, 200, 500, 300);
 		setVisible(false);
 		setLayout(new GridLayout(1,1));
 		setFocusable(true);
 	}
 	
 	
 	/**
 	 * Paints a background image
 	 */
 	public void paintComponent(Graphics g) {
 		Image img = new ImageIcon(PANEL_BACKGROUND).getImage();	
 		g.drawImage(img, 0, 0, null);
 	}
 	
 	public void Show()
 	{
 		monstersKilled.setText("Monsters killed: " + player.getStatistics().getMonstersKilled());
 		questsCompleted.setText("Quests completed: " + player.getStatistics().getQuestsCompleted());
 		totalScore.setText("Your total score is: " + (player.getStatistics().getTotalScore()));
 		
 		setVisible(true);
 		requestFocusInWindow();
 	}
 	
 	public class Score
 	{
 		private String name;
 		private int score;
 		private String time;
 		
 		public Score(String name, int score, String time)
 		{
 			this.name = name;
 			this.score = score;
 			this.time = time;
 		}
 		
 		public String getName()
 		{
 			return name;
 		}
 		
 		public int getScore()
 		{
 			return score;
 		}
 		
 		public String getTime()
 		{
 			return time;
 		}
 		
 		public String getText()
 		{
 			return "<html>" + name + " scored: " + score + ".<br>" + time +"</html>";
 		}
 	}
 }
