 package net.md_5.bungee.protocol;
 
 import com.google.common.base.Preconditions;
 import gnu.trove.map.TObjectIntMap;
 import gnu.trove.map.hash.TObjectIntHashMap;
 import java.lang.reflect.Constructor;
 import lombok.RequiredArgsConstructor;
 import net.md_5.bungee.protocol.packet.Chat;
 import net.md_5.bungee.protocol.packet.ClientSettings;
 import net.md_5.bungee.protocol.packet.EncryptionRequest;
 import net.md_5.bungee.protocol.packet.EncryptionResponse;
 import net.md_5.bungee.protocol.packet.Handshake;
 import net.md_5.bungee.protocol.packet.KeepAlive;
 import net.md_5.bungee.protocol.packet.Kick;
 import net.md_5.bungee.protocol.packet.Login;
 import net.md_5.bungee.protocol.packet.LoginRequest;
 import net.md_5.bungee.protocol.packet.LoginSuccess;
 import net.md_5.bungee.protocol.packet.PingPacket;
 import net.md_5.bungee.protocol.packet.PlayerListItem;
 import net.md_5.bungee.protocol.packet.PluginMessage;
 import net.md_5.bungee.protocol.packet.Respawn;
 import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
 import net.md_5.bungee.protocol.packet.ScoreboardObjective;
 import net.md_5.bungee.protocol.packet.ScoreboardScore;
 import net.md_5.bungee.protocol.packet.StatusRequest;
 import net.md_5.bungee.protocol.packet.StatusResponse;
 import net.md_5.bungee.protocol.packet.TabComplete;
 import net.md_5.bungee.protocol.packet.Team;
 
 public enum Protocol
 {
 
     // Undef
     HANDSHAKE
     {
         
         {
             TO_SERVER.registerPacket( 0x00, Handshake.class );
         }
     },
     // 0
     GAME
     {
         
         {
             TO_CLIENT.registerPacket( 0x00, KeepAlive.class );
             TO_CLIENT.registerPacket( 0x01, Login.class );
             TO_CLIENT.registerPacket( 0x02, Chat.class );
             TO_CLIENT.registerPacket( 0x07, Respawn.class );
             TO_CLIENT.registerPacket( 0x3B, PlayerListItem.class );
             TO_CLIENT.registerPacket( 0x3D, TabComplete.class );
             TO_CLIENT.registerPacket( 0x3E, ScoreboardObjective.class );
             TO_CLIENT.registerPacket( 0x3F, ScoreboardScore.class );
             TO_CLIENT.registerPacket( 0x40, ScoreboardDisplay.class );
             TO_CLIENT.registerPacket( 0x41, Team.class );
             TO_CLIENT.registerPacket( 0x42, PluginMessage.class );
             TO_CLIENT.registerPacket( 0x43, Kick.class );
 
 
             TO_SERVER.registerPacket( 0x00, KeepAlive.class );
            TO_SERVER.registerPacket( 0x01, Chat.class );
             TO_SERVER.registerPacket( 0x14, TabComplete.class );
             TO_SERVER.registerPacket( 0x15, ClientSettings.class );
             TO_SERVER.registerPacket( 0x17, PluginMessage.class );
         }
     },
     // 1
     STATUS
     {
         
         {
             TO_CLIENT.registerPacket( 0x00, StatusResponse.class );
             TO_CLIENT.registerPacket( 0x01, PingPacket.class );
 
             TO_SERVER.registerPacket( 0x00, StatusRequest.class );
             TO_SERVER.registerPacket( 0x01, PingPacket.class );
         }
     },
     //2
     LOGIN
     {
         
         {
             TO_CLIENT.registerPacket( 0x00, Kick.class );
             TO_CLIENT.registerPacket( 0x01, EncryptionRequest.class );
             TO_CLIENT.registerPacket( 0x02, LoginSuccess.class );
 
             TO_SERVER.registerPacket( 0x00, LoginRequest.class );
             TO_SERVER.registerPacket( 0x01, EncryptionResponse.class );
         }
     };
     /*========================================================================*/
     public static final int MAX_PACKET_ID = 0xFF;
     public static final int PROTOCOL_VERSION = 0x00;
     public static final String MINECRAFT_VERSION = "13w41a";
     /*========================================================================*/
     public final ProtocolDirection TO_SERVER = new ProtocolDirection( "TO_SERVER" );
     public final ProtocolDirection TO_CLIENT = new ProtocolDirection( "TO_CLIENT" );
 
     @RequiredArgsConstructor
     public class ProtocolDirection
     {
 
         private final String name;
         private final TObjectIntMap<Class<? extends DefinedPacket>> packetMap = new TObjectIntHashMap<>( MAX_PACKET_ID );
         private final Class<? extends DefinedPacket>[] packetClasses = new Class[ MAX_PACKET_ID ];
         private final Constructor<? extends DefinedPacket>[] packetConstructors = new Constructor[ MAX_PACKET_ID ];
 
         public boolean hasPacket(int id)
         {
             return id < MAX_PACKET_ID && packetConstructors[id] != null;
         }
 
         @Override
         public String toString()
         {
             return name;
         }
 
         public final DefinedPacket createPacket(int id)
         {
             if ( id > MAX_PACKET_ID )
             {
                 throw new BadPacketException( "Packet with id " + id + " outside of range " );
             }
             if ( packetConstructors[id] == null )
             {
                 throw new BadPacketException( "No packet with id " + id );
             }
 
             try
             {
                 return packetClasses[id].newInstance();
             } catch ( ReflectiveOperationException ex )
             {
                 throw new BadPacketException( "Could not construct packet with id " + id, ex );
             }
         }
 
         protected final void registerPacket(int id, Class<? extends DefinedPacket> packetClass)
         {
             try
             {
                 packetConstructors[id] = packetClass.getDeclaredConstructor();
             } catch ( NoSuchMethodException ex )
             {
                 throw new BadPacketException( "No NoArgsConstructor for packet class " + packetClass );
             }
             packetClasses[id] = packetClass;
             packetMap.put( packetClass, id );
         }
 
         protected final void unregisterPacket(int id)
         {
             packetMap.remove( packetClasses[id] );
             packetClasses[id] = null;
             packetConstructors[id] = null;
         }
 
         final int getId(Class<? extends DefinedPacket> packet)
         {
             Preconditions.checkArgument( packetMap.containsKey( packet ), "Cannot get ID for packet " + packet );
 
             return packetMap.get( packet );
         }
     }
 }
