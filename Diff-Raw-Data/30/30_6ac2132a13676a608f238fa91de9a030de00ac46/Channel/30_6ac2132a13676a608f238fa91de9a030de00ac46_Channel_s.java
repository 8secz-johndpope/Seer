 /*
  * @author     ucchy
  * @license    GPLv3
  * @copyright  Copyright ucchy 2013
  */
 package com.github.ucchyocean.lc;
 
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.FileHandler;
 import java.util.logging.LogRecord;
 import java.util.logging.Logger;
 import java.util.logging.SimpleFormatter;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.World;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.configuration.serialization.ConfigurationSerializable;
 import org.bukkit.configuration.serialization.SerializableAs;
 import org.bukkit.entity.Player;
 
 import com.github.ucchyocean.lc.event.LunaChatChannelChatEvent;
 import com.github.ucchyocean.lc.event.LunaChatChannelMemberChangedEvent;
 import com.github.ucchyocean.lc.japanize.ConvertTask;
 import com.github.ucchyocean.lc.japanize.JapanizeType;
 
 /**
  * チャンネル
  * @author ucchy
  */
 @SerializableAs("Channel")
 public class Channel implements ConfigurationSerializable {
 
     private static final String FOLDER_NAME_CHANNELS = "channels";
 
     private static final String INFO_FIRSTLINE = Resources.get("channelInfoFirstLine");
     private static final String INFO_PREFIX = Resources.get("channelInfoPrefix");
     private static final String INFO_GLOBAL = Resources.get("channelInfoGlobal");
 
     private static final String LIST_ENDLINE = Resources.get("listEndLine");
     private static final String LIST_FORMAT = Resources.get("listFormat");
 
     private static final String DEFAULT_FORMAT = Resources.get("defaultFormat");
     private static final String DEFAULT_FORMAT_FOR_PERSONAL =
             Resources.get("defaultFormatForPersonalChat");
     private static final String MSG_JOIN = Resources.get("joinMessage");
     private static final String MSG_QUIT = Resources.get("quitMessage");
 
     private static final String PREINFO = Resources.get("infoPrefix");
     private static final String NGWORD_PREFIX = Resources.get("ngwordPrefix");
     private static final String MSG_KICKED = Resources.get("cmdmsgKicked");
     private static final String MSG_BANNED = Resources.get("cmdmsgBanned");
 
     private static final String MSG_NO_RECIPIENT = Resources.get("messageNoRecipient");
 
     private static final String KEY_NAME = "name";
     private static final String KEY_DESC = "desc";
     private static final String KEY_FORMAT = "format";
     private static final String KEY_MEMBERS = "members";
     private static final String KEY_BANNED = "banned";
     private static final String KEY_MODERATOR = "moderator";
     private static final String KEY_PASSWORD = "password";
     private static final String KEY_VISIBLE = "visible";
     private static final String KEY_COLOR = "color";
     private static final String KEY_BROADCAST = "broadcast";
     private static final String KEY_WORLD = "world";
     private static final String KEY_RANGE = "range";
 
     private static final SimpleDateFormat dformat =
             new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 
     /** 参加者 */
     private List<String> members;
 
     /** チャンネルモデレータ */
     private List<String> moderator;
 
     /** BANされたプレイヤー */
     private List<String> banned;
 
     /** チャンネルの名称 */
     private String name;
 
     /** チャンネルの説明文 */
     private String description;
 
     /** チャンネルのパスワード */
     private String password;
 
     /** チャンネルリストに表示されるかどうか */
     private boolean visible;
 
     /** チャンネルのカラー */
     private String colorCode;
 
     /** メッセージフォーマット<br>
      * 指定可能なキーワードは下記のとおり<br>
      * %ch - チャンネル名<br>
      * %username - ユーザー名<br>
      * %msg - メッセージ<br>
      * %prefix - PermissionsExに設定するprefix<br>
      * %suffix - PermissionsExに設定するsuffix<br>
      * %color - チャンネルのカラーコード
      * */
     private String format;
 
     /** ブロードキャストチャンネルかどうか */
     private boolean broadcastChannel;
 
     /** ワールドチャットかどうか */
     private boolean isWorldRange;
 
     /** チャットの可聴範囲 0は無制限 */
     private int chatRange;
 
     /** ロガー */
     private Logger logger;
 
     /**
      * コンストラクタ
      * @param name チャンネルの名称
      */
     protected Channel(String name) {
         this.name = name;
         this.description = "";
         this.members = new ArrayList<String>();
         this.banned = new ArrayList<String>();
         this.moderator = new ArrayList<String>();
         this.password = "";
         this.visible = true;
         this.colorCode = "";
         this.broadcastChannel = false;
         this.isWorldRange = false;
         this.chatRange = 0;
 
         if ( isPersonalChat() ) {
             this.format = DEFAULT_FORMAT_FOR_PERSONAL;
         } else {
             this.format = DEFAULT_FORMAT;
             logger = makeLoggerForChannelChatLog(name);
         }
     }
 
     /**
      * @return 1:1チャットかどうか
      */
     public boolean isPersonalChat() {
         return name.contains(">");
     }
 
     /**
      * @return ブロードキャストチャンネルかどうか
      */
     public boolean isBroadcastChannel() {
         return (name.equals(LunaChat.config.globalChannel) || broadcastChannel);
     }
 
     /**
      * このチャットに発言をする
      * @param player 発言をするプレイヤー
      * @param message 発言をするメッセージ
      */
     public void chat(Player player, String message) {
 
         String preReplaceMessage = message;
 
         // NGワード発言をしたかどうかのチェックとマスク
         boolean isNG = false;
         String maskedMessage = message;
         for ( String word : LunaChat.config.ngword ) {
             if ( maskedMessage.contains(word) ) {
                 maskedMessage = maskedMessage.replace(
                         word, Utility.getAstariskString(word.length()));
                 isNG = true;
             }
         }
 
         // キーワード置き換え
         String msgFormat = replaceKeywords(format, player);
 
         // カラーコード置き換え
         maskedMessage = Utility.replaceColorCode(maskedMessage);
 
         // イベントコール
         LunaChatChannelChatEvent event =
                 new LunaChatChannelChatEvent(this.name,
                         preReplaceMessage, maskedMessage, msgFormat);
         Bukkit.getServer().getPluginManager().callEvent(event);
         if ( event.isCancelled() ) {
             return;
         }
         msgFormat = event.getMessageFormat();
 
         // Japanize変換と、発言処理
         boolean chated = false;
         if ( LunaChat.config.getJapanizeType() != JapanizeType.NONE ) {
             // 2byteコードを含まない場合にのみ、処理を行う
             if ( message.getBytes().length == message.length() ) {
 
                 int lineType = LunaChat.config.japanizeDisplayLine;
                 String lineFormat;
                 String taskFormat;
                 if ( lineType == 1 ) {
                     lineFormat = LunaChat.config.japanizeLine1Format;
                     taskFormat = msgFormat.replace("%msg", lineFormat);
                     chated = true;
                 } else {
                     lineFormat = LunaChat.config.japanizeLine2Format;
                     taskFormat = lineFormat;
                 }
 
                 // タスクを作成して実行する
                 // 発言処理は、タスクが完了しだい非同期で行われる
                 ConvertTask task = new ConvertTask(maskedMessage,
                         LunaChat.config.getJapanizeType(),
                         this, player, taskFormat);
                 Bukkit.getScheduler().runTask(LunaChat.instance, task);
             }
         }
 
         if ( !chated ) {
             // メッセージの送信
 
             String msg = msgFormat.replace("%msg", maskedMessage);
             sendMessage(player, msg);
         }
 
         // NGワード発言者に、NGワードアクションを実行する
         if ( isNG && player != null ) {
             if ( LunaChat.config.ngwordAction == NGWordAction.BAN ) {
                 // BANする
 
                 banned.add(player.getName());
                 removeMember(player.getName());
                 String temp = PREINFO + NGWORD_PREFIX + MSG_BANNED;
                 String m = String.format(temp, name);
                 player.sendMessage(m);
 
             } else if ( LunaChat.config.ngwordAction == NGWordAction.KICK ) {
                 // キックする
 
                 removeMember(player.getName());
                 String temp = PREINFO + NGWORD_PREFIX + MSG_KICKED;
                 String m = String.format(temp, name);
                 player.sendMessage(m);
             }
         }
     }
 
     /**
      * メンバーを追加する
      * @param name 追加するメンバー名
      */
     public void addMember(String name) {
 
         // イベントコール
         LunaChatChannelMemberChangedEvent event =
                 new LunaChatChannelMemberChangedEvent(this.name, this.members);
         Bukkit.getServer().getPluginManager().callEvent(event);
         if ( event.isCancelled() ) {
             return;
         }
 
         // メンバー追加
         if ( members.size() == 0 ) {
             moderator.add(name);
         }
         if ( !members.contains(name) ) {
             members.add(name);
             sendJoinQuitMessage(true, name);
             save();
         }
     }
 
     /**
      * メンバーを削除する
      * @param name 削除するメンバー名
      */
     public void removeMember(String name) {
 
         // イベントコール
         LunaChatChannelMemberChangedEvent event =
                 new LunaChatChannelMemberChangedEvent(this.name, this.members);
         Bukkit.getServer().getPluginManager().callEvent(event);
         if ( event.isCancelled() ) {
             return;
         }
 
         // デフォルト発言先が退出するチャンネルと一致する場合、
         // デフォルト発言先を削除する
         Channel def = LunaChat.manager.getDefaultChannel(name);
         if ( def != null && def.name.equals(this.name) ) {
             LunaChat.manager.removeDefaultChannel(name);
         }
 
         // 実際にメンバーから削除する
         if ( members.contains(name) ) {
             members.remove(name);
             sendJoinQuitMessage(false, name);
             if ( LunaChat.config.zeroMemberRemove && members.size() <= 0 ) {
                 LunaChat.manager.removeChannel(this.name);
                 return;
             } else {
                 save();
             }
         }
 
         // モデレーターだった場合は、モデレーターから除去する
         if ( moderator.contains(name) ) {
             moderator.remove(name);
         }
     }
 
     /**
      * 入退室メッセージを流す
      * @param isJoin 入室かどうか（falseなら退室）
      * @param player 入退室したプレイヤー名
      */
     private void sendJoinQuitMessage(boolean isJoin, String player) {
 
         // 1:1チャットなら、入退室メッセージは表示しない
         if ( isPersonalChat() ) {
             return;
         }
 
         String msg;
         if ( isJoin ) {
             msg = MSG_JOIN;
         } else {
             msg = MSG_QUIT;
         }
 
         // キーワード置き換え
         Player p = LunaChat.getPlayerExact(player);
         msg = replaceKeywords(msg, p);
 
         sendInformation(msg);
     }
 
     /**
      * TODO: このメソッドはprivateに変更したい
      * ※本メソッドはLunaChat内部での呼び出し用です。
      * ※イベントが発生しないため、APIからは呼び出ししないでください。
      * 情報をチャンネルメンバーに流します。
      * @param message メッセージ
      */
     public void sendInformation(String message) {
 
         if ( isBroadcastChannel() ) {
             // ブロードキャストチャンネル
 
             Bukkit.broadcastMessage(message);
 
         } else {
             // 通常チャンネル
 
             // オンラインのプレイヤーに送信する
             for ( String member : members ) {
                 Player p = LunaChat.getPlayerExact(member);
                 if ( p != null ) {
                     p.sendMessage(message);
                 }
             }
         }
 
         // ロギング
         log(message);
     }
 
     /**
      * TODO: このメソッドはprivateに変更したい
      * ※本メソッドはLunaChat内部での呼び出し用です。
      * ※イベントが発生しないため、APIからは呼び出ししないでください。
      * メッセージを表示します。指定したプレイヤーの発言として処理されます。
      * @param player プレイヤー（ワールドチャット、範囲チャットの場合は必須です）
      * @param message メッセージ
      */
     public void sendMessage(Player player, String message) {
 
         // 受信者を設定する
         ArrayList<Player> recipients = new ArrayList<Player>();
         if ( isBroadcastChannel() ) {
             // ブロードキャストチャンネル
 
             if ( isWorldRange ) {
                 World w = player.getWorld();
 
                 if ( chatRange > 0 ) {
                     // 範囲チャット
 
                     for ( Player p : Bukkit.getOnlinePlayers() ) {
                         if ( p.getWorld().equals(w) &&
                                 player.getLocation().distance(p.getLocation()) <= chatRange ) {
                             recipients.add(p);
                         }
                     }
 
                 } else {
                     // ワールドチャット
 
                     for ( Player p : Bukkit.getOnlinePlayers() ) {
                         if ( p.getWorld().equals(w) ) {
                             recipients.add(p);
                         }
                     }
                 }
 
             } else {
                 // 通常ブロードキャスト（全員へ送信）
 
                 for ( Player p : Bukkit.getOnlinePlayers() ) {
                     recipients.add(p);
                 }

                // 通常ブロードキャストなら、設定に応じてdynmapへ送信する
                if ( LunaChat.config.sendBroadcastChannelChatToDynmap &&
                        LunaChat.dynmap != null ) {
                    LunaChat.dynmap.chat(player, message);
                }
             }
 
         } else {
             // 通常チャンネル
 
             for ( String name : members ) {
                 Player p = Bukkit.getPlayerExact(name);
                 if ( p != null ) {
                     recipients.add(p);
                 }
             }
         }
 
         // 送信する
         for ( Player p : recipients ) {
             p.sendMessage(message);
         }
         // 受信者が自分以外いない場合は、メッセージを表示する
         if ( recipients.size() == 0 ||
                 (recipients.size() == 1 &&
                  recipients.get(0).getName().equals(player.getName()) ) ) {
             player.sendMessage(MSG_NO_RECIPIENT);
         }
 
         // ロギング
         log(message);
     }
 
     /**
      * チャンネル情報を返す
      * @return チャンネル情報
      */
     protected ArrayList<String> getInfo() {
 
         ArrayList<String> info = new ArrayList<String>();
         info.add(INFO_FIRSTLINE);
 
         info.add( String.format(
                 LIST_FORMAT, name, getOnlineNum(), getTotalNum(), description) );
 
         if ( !isBroadcastChannel() ) {
             // メンバーを、5人ごとに表示する
             StringBuffer buf = new StringBuffer();
             for ( int i=0; i<members.size(); i++ ) {
 
                 if ( i%5 == 0 ) {
                     if ( i != 0 ) {
                         info.add(buf.toString());
                         buf = new StringBuffer();
                     }
                     buf.append(INFO_PREFIX);
                 }
 
                 String name = members.get(i);
                 String disp;
                 if ( moderator.contains(name) ) {
                     name = "@" + name;
                 }
                 if ( isOnlinePlayer(members.get(i)) ) {
                     disp = ChatColor.WHITE + name;
                 } else {
                     disp = ChatColor.GRAY + name;
                 }
                 buf.append(disp + ",");
             }
 
             info.add(buf.toString());
 
         } else {
 
             info.add(INFO_GLOBAL);
         }
 
         info.add(LIST_ENDLINE);
 
         return info;
     }
 
     /**
      * 指定された名前のプレイヤーがオンラインかどうかを確認する
      * @param playerName プレイヤー名
      * @return オンラインかどうか
      */
     private boolean isOnlinePlayer(String playerName) {
         Player p = LunaChat.getPlayerExact(playerName);
         return ( p != null && p.isOnline() );
     }
 
     /**
      * チャットフォーマット内のキーワードを置き換えする
      * @param format チャットフォーマット
      * @param player プレイヤー
      * @return 置き換え結果
      */
     private String replaceKeywords(String format, Player player) {
 
         String msg = format;
 
         // テンプレートのキーワードを、まず最初に置き換える
         for ( int i=0; i<=9; i++ ) {
             String key = "%" + i;
             if ( msg.contains(key) ) {
                 if ( LunaChat.manager.getTemplate("" + i) != null ) {
                     msg = msg.replace(key, LunaChat.manager.getTemplate("" + i));
                     break;
                 }
             }
         }
 
         msg = msg.replace("%ch", name);
         //msg = msg.replace("%msg", message);
         msg = msg.replace("%color", colorCode);
 
         if ( player != null ) {
             msg = msg.replace("%username", player.getDisplayName());
 
             if ( msg.contains("%prefix") || msg.contains("%suffix") ) {
                 String prefix = "";
                 String suffix = "";
                 if ( LunaChat.vaultchat != null ) {
                     prefix = LunaChat.vaultchat.getPlayerPrefix(player);
                     suffix = LunaChat.vaultchat.getPlayerSuffix(player);
                 }
                 msg = msg.replace("%prefix", prefix);
                 msg = msg.replace("%suffix", suffix);
             }
         }
 
         return Utility.replaceColorCode(msg);
     }
 
     /**
      * チャンネルのオンライン人数を返す
      * @return オンライン人数
      */
     public int getOnlineNum() {
 
         // グローバルチャンネルならサーバー接続人数を返す
         if ( isBroadcastChannel() ) {
             return Bukkit.getOnlinePlayers().length;
         }
 
         // メンバーの人数を数える
         int onlineNum = 0;
         for ( String pname : members ) {
             if ( isOnlinePlayer(pname) ) {
                 onlineNum++;
             }
         }
         return onlineNum;
     }
 
     /**
      * チャンネルの総参加人数を返す
      * @return 総参加人数
      */
     public int getTotalNum() {
 
         // グローバルチャンネルならサーバー接続人数を返す
         if ( isBroadcastChannel() ) {
             return Bukkit.getOnlinePlayers().length;
         }
 
         return members.size();
     }
 
     /**
      * シリアライズ<br>
      * ConfigurationSerializable互換のための実装。
      * @see org.bukkit.configuration.serialization.ConfigurationSerializable#serialize()
      */
     @Override
     public Map<String, Object> serialize() {
 
         Map<String, Object> map = new HashMap<String, Object>();
         map.put(KEY_NAME, name);
         map.put(KEY_DESC, description);
         map.put(KEY_FORMAT, format);
         map.put(KEY_MEMBERS, members);
         map.put(KEY_BANNED, banned);
         map.put(KEY_MODERATOR, moderator);
         map.put(KEY_PASSWORD, password);
         map.put(KEY_VISIBLE, visible);
         map.put(KEY_COLOR, colorCode);
         map.put(KEY_BROADCAST, broadcastChannel);
         map.put(KEY_WORLD, isWorldRange);
         map.put(KEY_RANGE, chatRange);
         return map;
     }
 
     /**
      * デシリアライズ<br>
      * ConfigurationSerializable互換のための実装。
      * @param data デシリアライズ元のMapデータ。
      * @return デシリアライズされたクラス
      */
     public static Channel deserialize(Map<String, Object> data) {
 
         String name = castWithDefault(data.get(KEY_NAME), (String)null);
         List<String> members = castToStringList(data.get(KEY_MEMBERS));
 
         Channel channel = new Channel(name);
         channel.members = members;
         channel.description = castWithDefault(data.get(KEY_DESC), "");
         channel.format =
             castWithDefault(data.get(KEY_FORMAT), DEFAULT_FORMAT);
         channel.banned = castToStringList(data.get(KEY_BANNED));
         channel.moderator = castToStringList(data.get(KEY_MODERATOR));
         channel.password = castWithDefault(data.get(KEY_PASSWORD), "");
         channel.visible = castWithDefault(data.get(KEY_VISIBLE), true);
         channel.colorCode = castWithDefault(data.get(KEY_COLOR), "");
         channel.broadcastChannel = castWithDefault(data.get(KEY_BROADCAST), false);
         channel.isWorldRange = castWithDefault(data.get(KEY_WORLD), false);
         channel.chatRange = castWithDefault(data.get(KEY_RANGE), 0);
         return channel;
     }
 
     /**
      * Objectを、クラスTに変換する。nullならデフォルトを返す。
      * @param obj 変換元
      * @param def nullだった場合のデフォルト
      * @return 変換後
      */
     @SuppressWarnings("unchecked")
     private static <T> T castWithDefault(Object obj, T def) {
 
         if ( obj == null ) {
             return def;
         }
         return (T)obj;
     }
 
     /**
      * Objectを、List&lt;String&gt;に変換する。nullなら空のリストを返す。
      * @param obj 変換元
      * @return 変換後
      */
     @SuppressWarnings("unchecked")
     private static List<String> castToStringList(Object obj) {
 
         if ( obj == null ) {
             return new ArrayList<String>();
         }
         if ( !(obj instanceof List<?>) ) {
             return new ArrayList<String>();
         }
         return (List<String>)obj;
     }
 
     /**
      * チャンネルの説明文を返す
      * @return チャンネルの説明文
      */
     public String getDescription() {
         return description;
     }
 
     /**
      * チャンネルの説明文を設定する
      * @param description チャンネルの説明文
      */
     public void setDescription(String description) {
         this.description = description;
     }
 
     /**
      * チャンネルのパスワードを返す
      * @return チャンネルのパスワード
      */
     public String getPassword() {
         return password;
     }
 
     /**
      * チャンネルのパスワードを設定する
      * @param password チャンネルのパスワード
      */
     public void setPassword(String password) {
         this.password = password;
     }
 
     /**
      * チャンネルの可視性を返す
      * @return チャンネルの可視性
      */
     public boolean isVisible() {
         return visible;
     }
 
     /**
      * チャンネルの可視性を設定する
      * @param visible チャンネルの可視性
      */
     public void setVisible(boolean visible) {
         this.visible = visible;
     }
 
     /**
      * チャンネルのメッセージフォーマットを返す
      * @return チャンネルのメッセージフォーマット
      */
     public String getFormat() {
         return format;
     }
 
     /**
      * チャンネルのメッセージフォーマットを設定する
      * @param format チャンネルのメッセージフォーマット
      */
     public void setFormat(String format) {
         this.format = format;
     }
 
     /**
      * チャンネルのメンバーを返す
      * @return チャンネルのメンバー
      */
     public List<String> getMembers() {
         return members;
     }
 
     /**
      * チャンネルのモデレーターを返す
      * @return チャンネルのモデレーター
      */
     public List<String> getModerator() {
         return moderator;
     }
 
     /**
      * チャンネルのBANリストを返す
      * @return チャンネルのBANリスト
      */
     public List<String> getBanned() {
         return banned;
     }
 
     /**
      * チャンネル名を返す
      * @return チャンネル名
      */
     public String getName() {
         return name;
     }
 
     /**
      * チャンネルのカラーコードを返す
      * @return チャンネルのカラーコード
      */
     public String getColorCode() {
         return colorCode;
     }
 
     /**
      * チャンネルのカラーコードを設定する
      * @param colorCode カラーコード
      */
     public void setColorCode(String colorCode) {
         this.colorCode = colorCode;
     }
 
     /**
      * ブロードキャストチャンネルを設定する
      * @param broadcast ブロードキャストチャンネルにするかどうか
      */
     public void setBroadcast(boolean broadcast) {
         this.broadcastChannel = broadcast;
     }
 
     /**
      * チャットを同ワールド内に制限するかどうかを設定する
      * @param isWorldRange 同ワールド制限するかどうか
      */
     public void setWorldRange(boolean isWorldRange) {
         this.isWorldRange = isWorldRange;
     }
 
     /**
      * チャットの可聴範囲を設定する
      * @param range 可聴範囲
      */
     public void setRange(int range) {
         this.chatRange = range;
     }
 
     /**
      * チャンネルの情報をファイルに保存する。
      * @return 保存をしたかどうか。
      */
     public boolean save() {
 
         // フォルダーの取得と、必要に応じて作成
         File folder = new File(
                 LunaChat.instance.getDataFolder(), FOLDER_NAME_CHANNELS);
         if ( !folder.exists() ) {
             folder.mkdirs();
         }
 
         // 1:1チャットチャンネルの場合は、何もしない。
         if ( isPersonalChat() ) {
             return false;
         }
 
         File file = new File(folder, name + ".yml");
 
         // ファイルへ保存する
         YamlConfiguration conf = new YamlConfiguration();
         Map<String, Object> data = this.serialize();
         for ( String key : data.keySet() ) {
             conf.set(key, data.get(key));
         }
         try {
             conf.save(file);
             return true;
         } catch (IOException e) {
             e.printStackTrace();
             return false;
         }
     }
 
     /**
      * チャンネルの情報を保存したファイルを、削除する。
      * @return 削除したかどうか。
      */
     public boolean remove() {
 
         // フォルダーの取得
         File folder = new File(
                 LunaChat.instance.getDataFolder(), FOLDER_NAME_CHANNELS);
         if ( !folder.exists() ) {
             return false;
         }
         File file = new File(folder, name + ".yml");
         if ( !file.exists() ) {
             return false;
         }
 
         // ファイルを削除
         return file.delete();
     }
 
     /**
      * チャンネルの情報を保存したファイルから全てのチャンネルを復元して返す。
      * @return 全てのチャンネル
      */
     protected static HashMap<String, Channel> loadAllChannels() {
 
         // フォルダーの取得
         File folder = new File(
                 LunaChat.instance.getDataFolder(), FOLDER_NAME_CHANNELS);
         if ( !folder.exists() ) {
             return new HashMap<String, Channel>();
         }
 
         File[] files = folder.listFiles(new FilenameFilter() {
             @Override
             public boolean accept(File dir, String name) {
                 return name.endsWith(".yml");
             }
         });
 
         HashMap<String, Channel> result = new HashMap<String, Channel>();
         for ( File file : files ) {
             YamlConfiguration config =
                 YamlConfiguration.loadConfiguration(file);
             Map<String, Object> data = new HashMap<String, Object>();
             for ( String key : config.getKeys(false) ) {
                 data.put(key, config.get(key));
             }
             Channel channel = deserialize(data);
             result.put(channel.name.toLowerCase(), channel);
         }
 
         return result;
     }
 
     /**
      * ログを記録する
      * @param message 記録するメッセージ
      */
     private void log(String message) {
 
         if ( LunaChat.config.displayChatOnConsole ) {
             LunaChat.log(ChatColor.stripColor(message));
         }
         if ( LunaChat.config.loggingChat && logger != null ) {
             logger.info(ChatColor.stripColor(message));
         }
     }
 
     /**
      * チャンネルチャットロギングのためのロガーを作成して取得する。
      * @param name チャンネル名
      * @return ロガー
      */
     private static Logger makeLoggerForChannelChatLog(final String name) {
 
         Logger logger = Logger.getLogger("LunaChatChannelLogger" + name);
 
         FileHandler handler = null;
         try {
             File dir = new File(LunaChat.instance.getDataFolder(), "logs");
             if ( !dir.exists() ) {
                 dir.mkdirs();
             }
 
             handler = new FileHandler(
                     dir.getAbsolutePath() + "\\" + name + ".%g.log",
                     1048576, 1000, true);
             handler.setFormatter(new SimpleFormatter() {
                 @Override
                 public String format(LogRecord record) {
                     String date = dformat.format(new Date());
                     return String.format("%1$s,%2$s\r\n",
                             date, record.getMessage());
                 }
             });
 
             logger.addHandler(handler);
             logger.setUseParentHandlers(false);
 
             return logger;
 
         } catch (SecurityException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         } finally {
 //            if ( handler != null ) {
 //                handler.flush();
 //                handler.close();
 //            }
         }
 
         return null;
     }
 }
