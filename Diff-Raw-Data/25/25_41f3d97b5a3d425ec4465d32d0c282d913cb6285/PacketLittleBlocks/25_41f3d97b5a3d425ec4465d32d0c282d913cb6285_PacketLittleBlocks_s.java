 package littleblocks.network.packets;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 
 import cpw.mods.fml.common.FMLCommonHandler;
 import cpw.mods.fml.common.Side;
 
 import littleblocks.blocks.BlockLittleBlocksBlock;
 import littleblocks.core.LBInit;
 import net.minecraft.src.TileEntity;
 import net.minecraft.src.World;
 import eurysmods.network.packets.core.PacketIds;
 import eurysmods.network.packets.core.PacketPayload;
 import eurysmods.network.packets.core.PacketUpdate;
 
 public class PacketLittleBlocks extends PacketUpdate {
 
 	private int sender;
 
 	@Override
 	public void writeData(DataOutputStream data) throws IOException {
 		super.writeData(data);
 		data.writeInt(sender);
 	}
 
 	@Override
 	public void readData(DataInputStream data) throws IOException {
 		super.readData(data);
 		sender = data.readInt();
 	}
 
 	public PacketLittleBlocks() {
 		super(PacketIds.UPDATE);
		if (FMLCommonHandler.instance().getSide() == Side.SERVER)
			new Exception().printStackTrace();
 		this.setChannel(LBInit.LBM.getModChannel());
 	}
 
 	public PacketLittleBlocks(String command, int x, int y, int z, int side, float vecX, float vecY, float vecZ, int selectedX, int selectedY, int selectedZ, int blockId, int metadata) {
 		this();
 		this.setPosition(x, y, z, side);
 		this.setVecs(vecX, vecY, vecZ);
 		this.payload = new PacketPayload(5, 0, 1, 0);
 		this.setCommand(command);
 		this.setBlockId(blockId);
 		this.setMetadata(metadata);
 		this.setSelectedXYZ(selectedX, selectedY, selectedZ);
 	}
 
 	public PacketLittleBlocks(String command, BlockLittleBlocksBlock lbb) {
 		this();
 		this.setPosition(
 				lbb.getParentX(),
 				lbb.getParentY(),
 				lbb.getParentZ(),
 				0);
 		this.setVecs(0, 0, 0);
 		this.payload = new PacketPayload(5, 0, 1, 0);
 		this.setCommand(command);
 		this.setBlockId(lbb.getBlockId());
 		this.setMetadata(lbb.getMetaData());
 		this.setSelectedXYZ(
 				lbb.getLittleX(),
 				lbb.getLittleY(),
 				lbb.getLittleZ());
 	}
 
 	private void setCommand(String command) {
 		this.payload.setStringPayload(0, command);
 	}
 
 	public String getCommand() {
 		return this.payload.getStringPayload(0);
 	}
 
 	public void setSelectedXYZ(int selectedX, int selectedY, int selectedZ) {
 		this.payload.setIntPayload(2, selectedX);
 		this.payload.setIntPayload(3, selectedY);
 		this.payload.setIntPayload(4, selectedZ);
 	}
 
 	public TileEntity getTileEntity(World world) {
 		return world.getBlockTileEntity(
 				this.xPosition,
 				this.yPosition,
 				this.zPosition);
 	}
 
 	@Override
 	public boolean targetExists(World world) {
 		if (world.blockExists(this.xPosition, this.yPosition, this.zPosition)) {
 			return true;
 		}
 		return false;
 	}
 
 	public void setBlockId(int blockId) {
 		this.payload.setIntPayload(0, blockId);
 	}
 
 	public void setMetadata(int metadata) {
 		this.payload.setIntPayload(1, metadata);
 	}
 
 	public int getBlockID() {
 		return this.payload.getIntPayload(0);
 	}
 
 	public int getMetadata() {
 		return this.payload.getIntPayload(1);
 	}
 
 	public int getSelectedX() {
 		return this.payload.getIntPayload(2);
 	}
 
 	public int getSelectedY() {
 		return this.payload.getIntPayload(3);
 	}
 
 	public int getSelectedZ() {
 		return this.payload.getIntPayload(4);
 	}
 
 	public int getSender() {
 		return this.sender;
 	}
 
 	public void setSender(int sender) {
 		this.sender = sender;
 	}
 }
