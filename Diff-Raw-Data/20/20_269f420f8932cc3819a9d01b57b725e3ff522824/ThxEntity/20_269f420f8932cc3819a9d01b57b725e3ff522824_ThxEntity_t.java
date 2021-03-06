 package net.minecraft.src;
 
 import net.minecraft.client.Minecraft;
 
 public abstract class ThxEntity extends ThxEntityBase
 {
     Minecraft minecraft;
     Packet230ModLoader latestUpdatePacket;
     boolean visible = true;
     String renderTexture;
     ThxModel model;
     GuiScreen guiScreen;
     boolean paused;
 
     public ThxEntity(World world)
     {
         super(world);
         minecraft = ModLoader.getMinecraftInstance();
     }
 
     @Override
     public void onUpdate()
     {
 		super.onUpdate();
 		
 		applyUpdatePacketFromServer();
 
         if (!worldObj.isRemote) // can only pause in single-player mode
         {
             if (guiScreen != minecraft.currentScreen)
             {
                 // guiScreen has changed
                 guiScreen = minecraft.currentScreen;
 
                 if (guiScreen != null && guiScreen.doesGuiPauseGame())
                 {
                     // log("game paused " + this);
                     paused = true;
                 }
                 else if (paused) // cancel paused
                 {
                     // log("game UN-paused " + this);
                     paused = false;
                     prevTime = System.nanoTime();
                 }
             }
         }
         
         updateRotation();
         updateVectors();
     }
 
     public void spawn(Packet230ModLoader packet)
     {
         log("Received spawn packet: " + packet);
 
         int entityIdOrig = entityId;
 
         entityId = packet.dataInt[0];
 
         handleUpdatePacketFromServer(packet);
         applyUpdatePacketFromServer();
         updateRotation();
         updateVectors();
 
         log("spawn with pos, rot, mot, and id for entity with previous id " + entityIdOrig);
         log("spawn(): posX: " + posX + ", posY: " + posY + ", posZ: " + posZ);
     }
 
     public void handleUpdatePacketFromServer(Packet230ModLoader packet)
     {
         //plog("handleUpdatePacketFromServer: " + packet); // inbound packet not aligned with plog unless very high update rate
         
         int packetPilotId = packet.dataInt[1];
         int player = minecraft.thePlayer.entityId;
         
         if (minecraft.thePlayer.equals(riddenByEntity))
         {
             if (packetPilotId != minecraft.thePlayer.entityId) log("ignoring server update that would replace player pilot");
             
             //log("Skipping server update for player pilot entity " + riddenByEntity.entityId);
             return;
         }
         
         latestUpdatePacket = packet;
     }
         
         
     private void applyUpdatePacketFromServer()
     {
         if (latestUpdatePacket == null) return;
         
         Packet230ModLoader packet = latestUpdatePacket;
         latestUpdatePacket = null;
         
         plog("applyUpdatePacketFromServer: " + packet);
         
         int packetPilotId = packet.dataInt[1];
         int playerId = minecraft.thePlayer.entityId;
         
         // no or wrong current pilot
         if (packetPilotId > 0 && (riddenByEntity == null || riddenByEntity.entityId != packetPilotId))
         {
             Entity pilot = ((WorldClient) worldObj).getEntityByID(packetPilotId);
             if (pilot != null && !pilot.isDead)
             {
                 log("applyUpdatePacket: pilot " + pilot + " now boarding");
                 pilot.mountEntity(this);
             }
         }
         else if (packetPilotId == 0 && riddenByEntity != null)
         {
             log("current pilot id " + riddenByEntity.entityId + " is exiting");
             riddenByEntity.mountEntity(this);
         }
         
         
         // ignore fire controls, only used by server to spawn projectiles
         //fire1 = packet.dataInt[2];
         //fire2 = packet.dataInt[3];
         
         int ownerId = packet.dataInt[4];
        if (ownerId > 0) owner = ((WorldClient) worldObj).getEntityByID(ownerId);
        log("Entity owner: " + owner);
 
         serverPosX = MathHelper.floor_float(packet.dataFloat[0] * 32f);
         serverPosY = MathHelper.floor_float(packet.dataFloat[1] * 32f);
         serverPosZ = MathHelper.floor_float(packet.dataFloat[2] * 32f);
         
         setPositionAndRotation(packet.dataFloat[0], packet.dataFloat[1], packet.dataFloat[2], packet.dataFloat[3], packet.dataFloat[4]);
         
         rotationRoll = packet.dataFloat[5] % 360f;
 
         // for now, clear any motion
         motionX = packet.dataFloat[6];
         motionY = packet.dataFloat[7];
         motionZ = packet.dataFloat[8];
         
         damage = packet.dataFloat[9];
         throttle = packet.dataFloat[10];
     }
 
     public void sendUpdatePacketToServer()
     {
         if (!worldObj.isRemote) return;
 
         // only the pilot player can send updates to the server
         if (!minecraft.thePlayer.equals(riddenByEntity)) return;
 
         //log("Sending update packet: " + packet);
         minecraft.getSendQueue().addToSendQueue(getUpdatePacket());
     }
     
     @Override
     public float getShadowSize()
     {
         return 0.0F;
     }
 
     @Override
     public boolean isInRangeToRenderDist(double d)
     {
         return d < 128.0 * 128.0;
     }
 }
