package tv.rewinside.worldduplicator.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import tv.rewinside.nbtstorage.nbt.NBTBase;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;
import tv.rewinside.nbtstorage.nbt.NBTTagList;
import tv.rewinside.nbtstorage.nbt.NBTType;

@Data
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Chunk {
	private final int locX;
	private final int locZ;
	private final NBTTagCompound nbt;
	private final int lastChanged;
	private boolean[] yBlocksMap;
	private int[] heightMap;
	private byte[] biomes;
	private boolean changes = false;

	private final ChunkSection[] sections = new ChunkSection[16];

	public boolean hasBlock(int x, int z) {
		return this.yBlocksMap[z << 4 | x];
	}

	public int getHeight(int x, int z) {
		return this.heightMap[z << 4 | x];
	}

	public byte getBiome(int x, int z) {
		return this.biomes[z << 4 | x];
	}

	public void setHeight(int x, int z, int height) {
		this.heightMap[z << 4 | x] = height;
	}

	public void setBiome(int x, int z, byte biome) {
		this.biomes[z << 4 | x] = biome;
	}

	public void addEntity(NBTTagCompound entityCompound) {
		NBTTagList list = this.nbt.getList("Entities", NBTType.COMPOUND);
		if (list.size() == 0)
			this.nbt.set("Entities", list);
		list.add(entityCompound);
	}

	public void addTileEntity(NBTTagCompound tileEntityCompound) {
		NBTTagList list = this.nbt.getList("TileEntities", NBTType.COMPOUND);
		if (list.size() == 0)
			this.nbt.set("TileEntities", list);
		list.add(tileEntityCompound);
	}

	public ChunkSection getSection(int sectionId) {
		return this.sections[sectionId];
	}

	public ChunkSection getSectionForY(int y) {
		return this.getSection(y >> 4);
	}

	public ChunkSection createSection(int sectionId) {
		ChunkSection section = new ChunkSection();
		section.fillWithEmptyData();
		this.sections[sectionId] = section;
		this.changes = true;
		return section;
	}

	public void load() {
		this.createYBlocksMap();

		NBTTagList sections = this.nbt.getList("Sections", NBTType.COMPOUND);
		for (int i = 0; i < sections.size(); i++) {
			NBTTagCompound section = sections.getCompound(i);
			int y = section.getByte("Y");
			this.sections[y] = new ChunkSection();
			this.sections[y].loadFromNBT(section);
		}

		this.heightMap = this.nbt.getIntArray("HeightMap");
		this.biomes = this.nbt.getByteArray("Biomes");
	}

	private void createYBlocksMap() {
		this.yBlocksMap = new boolean[16 * 16];
		NBTTagList sections = this.nbt.getList("Sections", NBTType.COMPOUND);
		for (int i = 0; i < sections.size(); i++) {
			NBTTagCompound section = sections.getCompound(i);
			byte[] blocks = section.getByteArray("Blocks");

			int arrayIndex = 0;
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (blocks[arrayIndex] != 0)
							this.yBlocksMap[z << 4 | x] = true;

						arrayIndex++;
					}
				}
			}
		}
	}

	public void saveSectionsToNbt() {
		List<NBTBase> sectionsList = new ArrayList<>();
		for (int sectionId = 0; sectionId < this.sections.length; sectionId++) {
			ChunkSection section = this.sections[sectionId];
			if (section == null)
				continue;

			sectionsList.add(section.createNBT(sectionId));
		}
		NBTTagList sections = new NBTTagList(NBTType.COMPOUND, sectionsList);
		this.nbt.set("Sections", sections);
	}

	public static NBTTagCompound createChunkNbt(int x, int z) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByteArray("Biomes", new byte[16 * 16]);
		nbt.set("Entities", new NBTTagList(NBTType.COMPOUND, Collections.EMPTY_LIST));
		nbt.setIntArray("HeightMap", new int[16 * 16]);
		nbt.setLong("InhabitedTime", 0L);
		nbt.setLong("LastUpdate", System.currentTimeMillis());
		nbt.setBoolean("LightPopulated", true);
		nbt.set("Sections", new NBTTagList(NBTType.COMPOUND, Collections.EMPTY_LIST));
		nbt.setBoolean("TerrainPopulated", true);
		nbt.set("TileEntities", new NBTTagList(NBTType.COMPOUND, Collections.EMPTY_LIST));
		nbt.setByte("V", (byte)1);

		nbt.setInt("xPos", x);
		nbt.setInt("zPos", z);

		return nbt;
	}

	@Override
	public int hashCode() {
		return World.toChunkIndex(this.locX, this.locZ);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Chunk))
			return false;

		Chunk chunk = (Chunk)obj;
		return (
				this == obj
				|| (this.locX == chunk.locX && this.locZ == chunk.locZ)
		);
	}

}
