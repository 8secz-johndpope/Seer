 /*
  * @author     ucchy
  * @license    LGPLv3
  * @copyright  Copyright ucchy 2013
  */
 package com.github.ucchyocean.lc;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.entity.Player;
 
 import com.github.ucchyocean.lc.event.LunaChatChannelCreateEvent;
 import com.github.ucchyocean.lc.event.LunaChatChannelRemoveEvent;
 import com.github.ucchyocean.lc.event.LunaChatPostJapanizeEvent;
 import com.github.ucchyocean.lc.japanize.IMEConverter;
 import com.github.ucchyocean.lc.japanize.JapanizeType;
 import com.github.ucchyocean.lc.japanize.KanaConverter;
 
 /**
  * チャンネルマネージャー
  * @author ucchy
  */
 public class ChannelManager implements LunaChatAPI {
 
     private static final String MOTD_FIRSTLINE = Resources.get("motdFirstLine");
     private static final String LIST_ENDLINE = Resources.get("listEndLine");
     private static final String LIST_FORMAT = Resources.get("listFormat");
 
     private static final String MSG_BREAKUP = Resources.get("breakupMessage");
 
     private static final String FILE_NAME_DCHANNELS = "defaults.yml";
     private static final String FILE_NAME_TEMPLATES = "templates.yml";
     private static final String FILE_NAME_JAPANIZE = "japanize.yml";
 
     private File fileDefaults;
     private File fileTemplates;
     private File fileJapanize;
     private HashMap<String, Channel> channels;
     private HashMap<String, String> defaultChannels;
     private HashMap<String, String> templates;
     private HashMap<String, Boolean> japanize;
 
     /**
      * コンストラクタ
      */
     public ChannelManager() {
         reloadAllData();
     }
 
     /**
      * すべて読み込みする
      */
     @Override
     public void reloadAllData() {
 
         // デフォルトチャンネル設定のロード
         fileDefaults = new File(
                 LunaChat.instance.getDataFolder(), FILE_NAME_DCHANNELS);
 
         if ( !fileDefaults.exists() ) {
             YamlConfiguration conf = new YamlConfiguration();
             try {
                 conf.save(fileDefaults);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
 
         YamlConfiguration config =
                 YamlConfiguration.loadConfiguration(fileDefaults);
 
         defaultChannels = new HashMap<String, String>();
         for ( String key : config.getKeys(false) ) {
             defaultChannels.put(key, config.getString(key).toLowerCase());
         }
 
         // テンプレート設定のロード
         fileTemplates = new File(
                 LunaChat.instance.getDataFolder(), FILE_NAME_TEMPLATES);
 
         if ( !fileTemplates.exists() ) {
             YamlConfiguration conf = new YamlConfiguration();
             try {
                 conf.save(fileTemplates);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
 
         YamlConfiguration configTemplates =
                 YamlConfiguration.loadConfiguration(fileTemplates);
 
         templates = new HashMap<String, String>();
         for ( String key : configTemplates.getKeys(false) ) {
             templates.put(key, configTemplates.getString(key));
         }
 
         // Japanize設定のロード
         fileJapanize = new File(
                 LunaChat.instance.getDataFolder(), FILE_NAME_JAPANIZE);
 
         if ( !fileJapanize.exists() ) {
             YamlConfiguration conf = new YamlConfiguration();
             try {
                 conf.save(fileJapanize);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
 
         YamlConfiguration configJapanize =
                 YamlConfiguration.loadConfiguration(fileJapanize);
 
         japanize = new HashMap<String, Boolean>();
         for ( String key : configJapanize.getKeys(false) ) {
             japanize.put(key, configJapanize.getBoolean(key));
         }
 
         // チャンネル設定のロード
         channels = Channel.loadAllChannels();
 
         // コンフィグのリロード
         if ( LunaChat.config != null ) {
             LunaChat.config.reloadConfig();
         }
     }
 
     /**
      * すべて保存する
      */
     protected void saveAllChannels() {
 
         saveDefaults();
 
         for ( Channel channel : channels.values() ) {
             channel.save();
         }
     }
 
     /**
      * デフォルトチャンネル設定を保存する
      * @return 保存したかどうか
      */
     private boolean saveDefaults() {
 
         try {
             YamlConfiguration config = new YamlConfiguration();
             for ( String key : defaultChannels.keySet() ) {
                 config.set(key, defaultChannels.get(key));
             }
             config.save(fileDefaults);
             return true;
         } catch (IOException e) {
             e.printStackTrace();
             return false;
         }
     }
 
     /**
      * テンプレート設定を保存する
      * @return 保存したかどうか
      */
     private boolean saveTemplates() {
 
         try {
             YamlConfiguration config = new YamlConfiguration();
             for ( String key : templates.keySet() ) {
                 config.set(key, templates.get(key));
             }
             config.save(fileTemplates);
             return true;
         } catch (IOException e) {
             e.printStackTrace();
             return false;
         }
     }
 
     /**
      * Japanize設定を保存する
      * @return 保存したかどうか
      */
     private boolean saveJapanize() {
 
         try {
             YamlConfiguration config = new YamlConfiguration();
             for ( String key : japanize.keySet() ) {
                 config.set(key, japanize.get(key));
             }
             config.save(fileJapanize);
             return true;
         } catch (IOException e) {
             e.printStackTrace();
             return false;
         }
     }
 
     /**
      * プレイヤーのサーバー参加時用の参加チャンネルリストを返す
      * @param player プレイヤー
      * @return リスト
      */
     protected ArrayList<String> getListForMotd(Player player) {
 
         ArrayList<String> items = new ArrayList<String>();
         String playerName = player.getName();
         String dchannel = defaultChannels.get(player.getName());
         if ( dchannel == null ) {
             dchannel = "";
         }
 
         items.add(MOTD_FIRSTLINE);
         for ( String key : channels.keySet() ) {
             Channel channel = channels.get(key);
 
             // BANされているチャンネルは表示しない
             if ( channel.getBanned().contains(playerName) ) {
                 continue;
             }
 
             // 個人チャットはリストに表示しない
             if ( channel.isPersonalChat() ) {
                 continue;
             }
 
             // 参加していないチャンネルは、グローバルチャンネルを除き表示しない
             if ( !channel.getMembers().contains(playerName) &&
                     !channel.isGlobalChannel() ) {
                 continue;
             }
 
             String disp = ChatColor.WHITE + channel.getName();
             if ( key.equals(dchannel.toLowerCase()) ) {
                 disp = ChatColor.RED + channel.getName();
             }
             String desc = channel.getDescription();
             int onlineNum = channel.getOnlineNum();
             int memberNum = channel.getTotalNum();
             String item = String.format(
                     LIST_FORMAT, disp, onlineNum, memberNum, desc);
             items.add(item);
         }
         items.add(LIST_ENDLINE);
 
         return items;
     }
 
     /**
      * プレイヤーのJapanize設定を返す
      * @param playerName プレイヤー名
      * @return Japanize設定
      */
     protected boolean isPlayerJapanize(String playerName) {
         if ( !japanize.containsKey(playerName) ) {
             return true;
         }
         return japanize.get(playerName);
     }
    
    /**
     * デフォルトチャンネル設定を全て削除する
     */
    protected void removeAllDefaultChannels() {
        defaultChannels.clear();
        saveDefaults();
    }
 
     /**
      * 指定したチャンネル名が存在するかどうかを返す
      * @param channelName チャンネル名
      * @return 存在するかどうか
      * @see com.github.ucchyocean.lc.LunaChatAPI#isExistChannel(java.lang.String)
      */
     @Override
     public boolean isExistChannel(String channelName) {
         if ( channelName == null ) {
             return false;
         }
         return channels.containsKey(channelName.toLowerCase());
     }
 
     /**
      * 全てのチャンネルを返す
      * @return 全てのチャンネル
      * @see com.github.ucchyocean.lc.LunaChatAPI#getChannels()
      */
     @Override
     public Collection<Channel> getChannels() {
 
         return channels.values();
     }
 
     /**
      * プレイヤーが参加しているチャンネルを返す
      * @param playerName プレイヤー名
      * @return チャンネル
      * @see com.github.ucchyocean.lc.LunaChatAPI#getChannelsByPlayer(java.lang.String)
      */
     @Override
     public Collection<Channel> getChannelsByPlayer(String playerName) {
 
         Collection<Channel> result = new ArrayList<Channel>();
         for ( String key : channels.keySet() ) {
             Channel channel = channels.get(key);
             if ( channel.getMembers().contains(playerName) ||
                     channel.isGlobalChannel() ) {
                 result.add(channel);
             }
         }
         return result;
     }
 
     /**
      * プレイヤーが参加しているデフォルトのチャンネルを返す
      * @param playerName プレイヤー
      * @return チャンネル
      * @see com.github.ucchyocean.lc.LunaChatAPI#getDefaultChannel(java.lang.String)
      */
     @Override
     public Channel getDefaultChannel(String playerName) {
 
         String cname = defaultChannels.get(playerName);
 
         if ( cname == null || !isExistChannel(cname) ) {
             return null;
         }
         return channels.get(cname);
     }
 
     /**
      * プレイヤーのデフォルトチャンネルを設定する
      * @param playerName プレイヤー
      * @param channelName チャンネル名
      * @see com.github.ucchyocean.lc.LunaChatAPI#setDefaultChannel(java.lang.String, java.lang.String)
      */
     @Override
     public void setDefaultChannel(String playerName, String channelName) {
         if ( channelName == null ) {
             removeDefaultChannel(playerName);
             return;
         }
         defaultChannels.put(playerName, channelName.toLowerCase());
         saveDefaults();
     }
 
     /**
      * 指定した名前のプレイヤーに設定されている、デフォルトチャンネルを削除する
      * @param playerName プレイヤー名
      * @see com.github.ucchyocean.lc.LunaChatAPI#removeDefaultChannel(java.lang.String)
      */
     @Override
     public void removeDefaultChannel(String playerName) {
         if ( defaultChannels.containsKey(playerName) ) {
             defaultChannels.remove(playerName);
         }
         saveDefaults();
     }
 
     /**
      * チャンネルを取得する
      * @param channelName チャンネル名
      * @return チャンネル
      * @see com.github.ucchyocean.lc.LunaChatAPI#getChannel(java.lang.String)
      */
     @Override
     public Channel getChannel(String channelName) {
         if ( channelName == null ) return null;
         return channels.get(channelName.toLowerCase());
     }
 
     /**
      * 新しいチャンネルを作成する
      * @param channelName チャンネル名
      * @return 作成されたチャンネル
      * @see com.github.ucchyocean.lc.LunaChatAPI#createChannel(java.lang.String)
      */
     @Override
     public Channel createChannel(String channelName) {
 
         // イベントコール
         LunaChatChannelCreateEvent event =
                 new LunaChatChannelCreateEvent(channelName);
         Bukkit.getServer().getPluginManager().callEvent(event);
         if ( event.isCancelled() ) {
             return null;
         }
         String name = event.getChannelName();
 
         Channel channel = new Channel(name);
         channels.put(name.toLowerCase(), channel);
         channel.save();
         return channel;
     }
 
     /**
      * チャンネルを削除する
      * @param channelName 削除するチャンネル名
      * @return 削除したかどうか
      * @see com.github.ucchyocean.lc.LunaChatAPI#removeChannel(java.lang.String)
      */
     @Override
     public boolean removeChannel(String channelName) {
 
         channelName = channelName.toLowerCase();
 
         // イベントコール
         LunaChatChannelRemoveEvent event =
                 new LunaChatChannelRemoveEvent(channelName);
         Bukkit.getServer().getPluginManager().callEvent(event);
         if ( event.isCancelled() ) {
             return false;
         }
 
         Channel channel = getChannel(channelName);
         if ( channel != null ) {
             channel.remove();
             channels.remove(channelName);
 
             // チャンネルのメンバーを強制解散させる
             String message = String.format(MSG_BREAKUP, channelName);
             for ( String pname : channel.getMembers() ) {
                 Player player = Bukkit.getPlayerExact(pname);
                 if ( player != null ) {
                     player.sendMessage(message);
                 }
             }
         }
 
         return true;
     }
 
     /**
      * テンプレートを取得する
      * @param id テンプレートID
      * @return テンプレート
      * @see com.github.ucchyocean.lc.LunaChatAPI#getTemplate(java.lang.String)
      */
     @Override
     public String getTemplate(String id) {
         return templates.get(id);
     }
 
     /**
      * テンプレートを登録する
      * @param id テンプレートID
      * @param template テンプレート
      * @see com.github.ucchyocean.lc.LunaChatAPI#setTemplate(java.lang.String, java.lang.String)
      */
     @Override
     public void setTemplate(String id, String template) {
         templates.put(id, template);
         saveTemplates();
     }
 
     /**
      * テンプレートを削除する
      * @param id テンプレートID
      * @see com.github.ucchyocean.lc.LunaChatAPI#removeTemplate(java.lang.String)
      */
     @Override
     public void removeTemplate(String id) {
         templates.remove(id);
         saveTemplates();
     }
 
     /**
      * Japanize変換を行う
      * @param message 変換するメッセージ
      * @param type 変換タイプ
      * @return 変換後のメッセージ、ただしイベントでキャンセルされた場合はnullが返されるので注意
      */
     @Override
     public String japanize(String message, JapanizeType type) {
 
         if ( type == JapanizeType.NONE ) {
             return message;
         }
 
         // カナ変換
         String japanized = KanaConverter.conv(message);
 
         // IME変換
         if ( type == JapanizeType.GOOGLE_IME ) {
             japanized = IMEConverter.convByGoogleIME(japanized);
         } else if ( type == JapanizeType.SOCIAL_IME ) {
             japanized = IMEConverter.convBySocialIME(japanized);
         }
 
         // イベントコール
         LunaChatPostJapanizeEvent event =
                 new LunaChatPostJapanizeEvent("", message, japanized);
         Bukkit.getServer().getPluginManager().callEvent(event);
         if ( event.isCancelled() ) {
             return null;
         }
         japanized = event.getJapanized();
 
         return japanized;
     }
 
     /**
      * 該当プレイヤーのJapanize変換をオン/オフする
      * @param playerName 設定するプレイヤー名
      * @param doJapanize Japanize変換するかどうか
      */
     @Override
     public void setPlayersJapanize(String playerName, boolean doJapanize) {
         japanize.put(playerName, doJapanize);
         saveJapanize();
     }
 
     /**
      * 指定された名前がチャンネル名として使用可能かどうかを判定する<br/>
      * 具体的には、英数字・ハイフン・アンダーバー のいずれかから構成される、
      * 1文字から20文字の文字列、の場合に、trueを返す。<br/>
      * （既に存在するチャンネル名をチェックするわけではない。）
      * @param name 名前
      * @return チャンネル名として使用可能かどうか
      */
     @Override
     public boolean checkForChannelName(String name) {
         return name.matches("[0-9a-zA-Z\\-_]{1,20}");
     }
 }
