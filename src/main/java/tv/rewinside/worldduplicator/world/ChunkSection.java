package tv.rewinside.worldduplicator.world;

import lombok.Data;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;

public class ChunkSection {
	private byte[] add;
	private byte[] blockLight;
	private byte[] blocks;
	private byte[] data;
	private byte[] skyLight;

	public void fillWithEmptyData() {
		this.add = null;
		this.blockLight = new byte[2048];
		this.blocks = new byte[4096];
		this.data = new byte[2048];
		this.skyLight = new byte[2048];
	}

	public void loadFromNBT(NBTTagCompound compound) {
		this.add = (compound.hasKey("Add") ? compound.getByteArray("Add") : null);
		this.blockLight = compound.getByteArray("BlockLight");
		this.blocks = compound.getByteArray("Blocks");
		this.data = compound.getByteArray("Data");
		this.skyLight = compound.getByteArray("SkyLight");
	}

	public NBTTagCompound createNBT(int sectionId) {
		NBTTagCompound compound = new NBTTagCompound();
		if (this.add != null)
			compound.setByteArray("Add", this.add);
		compound.setByteArray("BlockLight", this.blockLight);
		compound.setByteArray("Blocks", this.blocks);
		compound.setByteArray("Data", this.data);
		compound.setByteArray("SkyLight", this.skyLight);
		compound.setByte("Y", (byte)sectionId);

		return compound;
	}

	public BlockData getBlock(int x, int y, int z) {
		int index = (y << 8) | (z << 4) | x;
		int nibbleIndex = index >> 1;

		if ((index & 0x1) == 0) {
			return new BlockData(
					(this.add != null) ? (byte) (this.add[nibbleIndex] & 0xF) : 0,
					(byte) (this.blockLight[nibbleIndex] & 0xF),
					this.blocks[index],
					(byte) (this.data[nibbleIndex] & 0xF),
					(byte) (this.skyLight[nibbleIndex] & 0xF)
			);
		} else {
			return new BlockData(
					(this.add != null) ? (byte) (this.add[nibbleIndex] >> 4 & 0xF) : 0,
					(byte) (this.blockLight[nibbleIndex] >> 4 & 0xF),
					this.blocks[index],
					(byte) (this.data[nibbleIndex] >> 4 & 0xF),
					(byte) (this.skyLight[nibbleIndex] >> 4 & 0xF)
			);
		}
	}

	public void setBlock(int x, int y, int z, BlockData block) {
		int index = (y << 8) | (z << 4) | x;
		int nibbleIndex = index >> 1;

		this.blocks[index] = block.getBlock();
		if (this.add == null && block.getAdd() != 0) {
			this.add = new byte[2048];
		}

		if ((index & 0x1) == 0) {
			if (this.add != null)
				this.add[nibbleIndex] = (byte) ((this.add[nibbleIndex] & 240) | (block.getAdd() & 0xF));
			this.blockLight[nibbleIndex] = (byte) ((this.blockLight[nibbleIndex] & 240) | (block.getBlockLight() & 0xF));
			this.data[nibbleIndex] = (byte) ((this.data[nibbleIndex] & 240) | (block.getData() & 0xF));
			this.skyLight[nibbleIndex] = (byte) ((this.skyLight[nibbleIndex] & 240) | (block.getSkyLight() & 0xF));
		} else {
			if (this.add != null)
				this.add[nibbleIndex] = (byte) ((this.add[nibbleIndex] & 0xF) | (block.getAdd() & 0xF) << 4);
			this.blockLight[nibbleIndex] = (byte) ((this.blockLight[nibbleIndex] & 0xF) | (block.getBlockLight() & 0xF) << 4);
			this.data[nibbleIndex] = (byte) ((this.data[nibbleIndex] & 0xF) | (block.getData() & 0xF) << 4);
			this.skyLight[nibbleIndex] = (byte) ((this.skyLight[nibbleIndex] & 0xF) | (block.getSkyLight() & 0xF) << 4);
		}
	}

	@Data
	public static class BlockData {
		private final byte add;
		private final byte blockLight;
		private final byte block;
		private final byte data;
		private final byte skyLight;
	}

}
