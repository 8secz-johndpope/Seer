 package bomberman.server;
 
 import bomberman.server.elements.Bomb;
 import bomberman.server.elements.Element;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.net.Socket;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import org.json.simple.JSONValue;
 
 public class ServerThread extends Thread {
 
     private Socket socket;
     private PrintWriter out;
     private BufferedReader in;
     private int client_id;
     private boolean initialized = false;
     private int nb_bombs = 0;
     private int bombs_allowed = 1;
     private int bomb_sleeping_time = 4000;
     private int position_x = 0;
     private int position_y = 0;
 
     public ServerThread(Socket socket, int client_id) {
         this.client_id = client_id;
         System.out.println("Le joueur " + client_id + " a rejoint la partie!");
         this.socket = socket;
         try {
             this.out = new PrintWriter(socket.getOutputStream(), true);
             this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         } catch (IOException e) {
             System.out.println(e.getMessage());
         }
     }
 
     public int getPostionX() {
         return this.position_x;
     }
 
     public int getPositionY() {
         return this.position_y;
     }
 
     public int getClientId() {
         return this.client_id;
     }
 
     @Override
     public void run() {
         this.sendBoardCols();
         this.sendBoard();
         this.setRandomPosition();
         this.addPlayer();
         this.sendPlayersList();
         this.initialized = true;
 
         try {
             String line;
             while ((line = this.in.readLine()) != null) {
                 int space_pos = line.indexOf(" ");
                 if (space_pos != -1) {
                     try {
                         Object obj = this.decodeData(line.substring(space_pos + 1));
                         this.execute(line.substring(0, space_pos), obj);
                     } catch (Exception e) {
                         System.out.println(e.getMessage());
                     }
                 }
             }
 
         } catch (Exception e) {
             System.out.println(e.getMessage());
         } finally {
             try {
                 System.out.println("Le joueur " + client_id + " a quitté la partie");
                 socket.close();
                 Server.delPlayer(this.client_id);
             } catch (IOException e) {
                 System.out.println(e.getMessage());
             }
         }
     }
 
     private void execute(String command, Object obj) throws Exception {
         System.out.println(command + " " + obj);
         try {
             if (command.equals("move")) {
                 if (obj instanceof ArrayList && ((List) obj).size() == 2) {
                     int diff_x = this.convertToInt(((List) obj).get(0));
                     int diff_y = this.convertToInt(((List) obj).get(1));
                     this.move(diff_x, diff_y);
                 }
 
             } else if (command.equals("drop_bomb")) {
                 if (this.nb_bombs < this.bombs_allowed) {
                     this.dropBomb();
                 }
             }
         } catch (Exception e) {
             System.out.println(e.getMessage());
         }
     }
 
     public void send(String command, Object obj) {
         try {
             this.out.println(command + " " + this.encodeData(obj));
         } catch (Exception e) {
             System.out.println(e.getMessage());
         }
     }
 
     private String encodeData(Object data) throws Exception {
         StringWriter jsonOut = new StringWriter();
         JSONValue.writeJSONString(data, jsonOut);
         return jsonOut.toString();
     }
 
     private Object decodeData(String data) throws Exception {
         return JSONValue.parse(data);
     }
 
     private int convertToInt(Object n) throws Exception {
         if (n instanceof Integer) {
             return (Integer) n;
         } else if (n instanceof Long) {
             return ((Long) n).intValue();
         }
         throw new Exception(n + " not an Integer");
     }
 
     private void sendBoardCols() {
         this.send("board_cols", Server.board.getCols());
     }
 
     private void sendBoard() {
         this.send("board", Server.board.getData());
     }
 
     public void setRandomPosition() {
         double nb_cases = Server.board.getCols() * Server.board.getRows();
         int i, x, y;
         Map<Integer, Integer> players_positions = Server.getPlayersPositions();
 
        Element element;
         do {
             i = (int) Math.round(Math.random() * nb_cases);
             x = i % Server.board.getCols();
             y = (int) Math.ceil(i / Server.board.getCols());
            element = Server.board.getElements().get(i);
        } while ((element != null && !element.isWalkable()) || players_positions.containsKey(x) && players_positions.get(x) == y);
 
         this.position_x = x;
         this.position_y = y;
     }
 
     private void sendPlayersList() {
         Map<Integer, Map> players_list = Server.getPlayersList(this.client_id);
         this.send("players", players_list);
     }
 
     private void addPlayer() {
         ArrayList<Integer> position = new ArrayList<Integer>();
         position.add(this.client_id);
         position.add(this.position_x);
         position.add(this.position_y);
 
         ArrayList<Integer> exceptions = new ArrayList<Integer>();
         exceptions.add(this.client_id);
 
         Server.sendAllBut("add_player", position, exceptions);
     }
 
     public boolean isInitialized() {
         return this.initialized;
     }
 
     private void move(int diff_x, int diff_y) {
         Boolean moving_allowed = false;
 
         if (Math.abs(diff_x) + Math.abs(diff_y) == 1) {
             moving_allowed = true;
         }
 
         int target_x = this.position_x + diff_x;
         int target_y = this.position_y + diff_y;
 
         int target_index = target_x + Server.board.getCols() * target_y;
        Element target_element = Server.board.getElements().get(target_index);
        if (target_element != null && !(target_element.isActive() && target_element.isWalkable())) {
             moving_allowed = false;
         }
 
         if (moving_allowed) {
             this.position_x += diff_x;
             this.position_y += diff_y;
 
             ArrayList<Integer> move = new ArrayList<Integer>();
             move.add(this.client_id);
             move.add(this.position_x);
             move.add(this.position_y);
 
             ArrayList<Integer> exceptions = new ArrayList<Integer>();
             exceptions.add(this.client_id);
 
             Server.sendAllBut("move", move, exceptions);
 
         } else {
             ArrayList<Integer> position = new ArrayList<Integer>();
             position.add(this.client_id);
             position.add(this.position_x);
             position.add(this.position_y);
             this.send("reposition", position);
         }
     }
 
     private void dropBomb() {
         Bomb bomb = new Bomb();
         bomb.setX(this.position_x);
         bomb.setY(this.position_y);
         bomb.setSleepingTime(this.bomb_sleeping_time);
 
         ArrayList<Integer> bomb_position = new ArrayList<Integer>();
         bomb_position.add(bomb.getX());
         bomb_position.add(bomb.getY());
 
         int target_index = bomb.getX() + Server.board.getCols() * bomb.getY();
         Server.board.setElement(target_index, bomb);
 
         Server.sendAll("drop_bomb", bomb_position);
 
         this.nb_bombs++;
        this.burstBomb(bomb);
     }
 
    private void burstBomb(final Bomb bomb) {
        final ServerThread current_thread = this;
         new Thread(new Runnable() {
 
             public void run() {
                 try {
                     ArrayList<Integer> bomb_position = new ArrayList<Integer>();
                     bomb_position.add(bomb.getX());
                     bomb_position.add(bomb.getY());
                     Thread.sleep(bomb.getSleepingTime());
 
                     HashMap<Integer, ServerThread> players_threads = Server.getPlayersThreads();
 
                     for (int i = 1; i < Server.board.getCols() - 1; i++) {
                         int index = i + Server.board.getCols() * bomb.getY();
                         Element element = Server.board.getElements().get(index);
                         if (element != null) {
                            if (element instanceof Bomb) {
                                Server.board.setElement(index, null);
                            } else {
                                element.setActive(false);
                            }
                         }
                         for (ServerThread thread : players_threads.values()) {
                             if (thread.getPostionX() == i && thread.getPositionY() == bomb.getY()) {
                                 Server.killPlayer(thread.getClientId());
                             }
                         }
                     }
                     for (int i = 1; i < Server.board.getRows() - 1; i++) {
                         int index = bomb.getX() + Server.board.getCols() * i;
                         Element element = Server.board.getElements().get(index);
                         if (element != null) {
                            if (element instanceof Bomb) {
                                Server.board.setElement(index, null);
                            } else {
                                element.setActive(false);
                            }
                         }
                         for (ServerThread thread : players_threads.values()) {
                             if (thread.getPostionX() == bomb.getX() && thread.getPositionY() == i) {
                                 Server.killPlayer(thread.getClientId());
                             }
                         }
                     }
 
                     Server.sendAll("burst_bomb", bomb_position);
                    current_thread.nb_bombs--;
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }
             }
         }).start();
     }
 }
