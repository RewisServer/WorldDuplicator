package tv.rewinside.worldduplicator.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import tv.rewinside.nbtstorage.nbt.NBTBase;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;
import tv.rewinside.nbtstorage.nbt.NBTTagDouble;
import tv.rewinside.nbtstorage.nbt.NBTTagList;
import tv.rewinside.nbtstorage.nbt.NBTType;
import tv.rewinside.worldduplicator.world.Chunk;
import tv.rewinside.worldduplicator.world.ChunkSection;
import tv.rewinside.worldduplicator.world.ChunkSection.BlockData;
import tv.rewinside.worldduplicator.world.Entity;
import tv.rewinside.worldduplicator.world.World;

@Data
public class Schematic {
	private final int blocksX;
	private final int blocksZ;

	private BlockData[] blocks;
	private List<Entity> entities = new ArrayList<>();
	private List<Entity> tileEntities = new ArrayList<>();
	private int[] heightMap;
	private byte[] biomes;

	public Schematic(int blocksX, int blocksZ) {
		this.blocksX = blocksX;
		this.blocksZ = blocksZ;

		this.blocks = new BlockData[blocksX * 256 * blocksZ];
		this.heightMap = new int[blocksX * blocksZ];
		this.biomes = new byte[blocksX * blocksZ];
	}

	public void readFromWorld(World world, int startX, int startZ) {
		Set<Chunk> chunks = new HashSet<>();
		int blockIndex = 0;
		int metaIndex = 0;

		int endX = startX + this.blocksX;
		int endZ = startZ + this.blocksZ;

		// Read block data from chunk sections
		for (int x = startX; x < endX; x++) {
			for (int z = startZ; z < endZ; z++) {
				Chunk chunk = world.getChunk(x >> 4, z >> 4);
				if (chunk == null) {
					metaIndex++;
					blockIndex += 256;
					continue;
				}
				chunks.add(chunk);

				int relX = x & 0xF;
				int relZ = z & 0xF;

				this.heightMap[metaIndex] = chunk.getHeight(relX, relZ);
				this.biomes[metaIndex] = chunk.getBiome(relX, relZ);
				metaIndex++;

				for (int sectionId = 0; sectionId < 16; sectionId++) {
					ChunkSection section = chunk.getSection(sectionId);
					if (section == null) {
						blockIndex += 16;
						continue;
					}

					for (int relY = 0; relY < 16; relY++) {
						this.blocks[blockIndex] = section.getBlock(relX, relY, relZ);
						blockIndex++;
					}
				}
			}
		}

		// Read entities and tile entities
		for (Chunk chunk : chunks) {
			NBTTagList entities = chunk.getNbt().getList("Entities", NBTType.COMPOUND);
			for (int i = 0; i < entities.size(); i++) {
				NBTTagCompound nbt = entities.getCompound(i);
				NBTTagList posList = nbt.getList("Pos", NBTType.DOUBLE);

				if (posList.getDouble(0) >= startX && posList.getDouble(0) < endX && posList.getDouble(2) >= startZ && posList.getDouble(2) < endZ) {
					this.entities.add(new Entity(posList.getDouble(0) - startX, posList.getDouble(1), posList.getDouble(2) - startZ, nbt));
				}
			}

			entities = chunk.getNbt().getList("TileEntities", NBTType.COMPOUND);
			for (int i = 0; i < entities.size(); i++) {
				NBTTagCompound nbt = entities.getCompound(i);
				if (nbt.getInt("x") >= startX && nbt.getInt("x") < endX && nbt.getInt("z") >= startZ && nbt.getInt("z") < endZ) {
					this.tileEntities.add(new Entity(nbt.getInt("x") - startX, nbt.getInt("y"), nbt.getInt("z") - startZ, nbt));
				}
			}
		}
	}

	public void placeToWorld(World world, int startX, int startZ) {
		int index = 0;
		int metaIndex = 0;
		for (int x = startX; x < (startX + this.blocksX); x++) {
			for (int z = startZ; z < (startZ + this.blocksZ); z++) {
				Chunk chunk = world.getChunk(x >> 4, z >> 4);
				if (chunk == null)
					chunk = world.loadChunk(x >> 4, z >> 4, (int) (System.currentTimeMillis() / 1000L), Chunk.createChunkNbt(x >> 4, z >> 4));

				int relX = x & 0xF;
				int relZ = z & 0xF;

				chunk.setHeight(relX, relZ, this.heightMap[metaIndex]);
				chunk.setBiome(relX, relZ, this.biomes[metaIndex]);
				metaIndex++;

				for (int sectionId = 0; sectionId < 16; sectionId++) {
					if (this.blocks[index] == null) {
						index += 16;
						continue;
					}

					ChunkSection section = chunk.getSection(sectionId);
					if (section == null)
						section = chunk.createSection(sectionId);

					for (int relY = 0; relY < 16; relY++) {
						section.setBlock(relX, relY, relZ, this.blocks[index]);
						index++;
					}
				}
			}
		}

		for (Entity entity : this.entities) {
			Chunk chunk = world.getChunk((int)Math.floor(startX + entity.getRelX()) >> 4, (int)Math.floor(startZ + entity.getRelZ()) >> 4);
			if (chunk == null)
				continue;

			NBTTagCompound entityCompound = (NBTTagCompound) entity.getCompound().clone();
			List<NBTBase> positions = Arrays.asList(new NBTTagDouble[] {
				new NBTTagDouble(startX + entity.getRelX()),
				new NBTTagDouble(entity.getRelY()),
				new NBTTagDouble(startZ + entity.getRelZ())
			});

			NBTTagList posList = new NBTTagList(NBTType.DOUBLE, positions);
			entityCompound.set("Pos", posList);
			chunk.addEntity(entityCompound);
		}

		for (Entity tileEntity : this.tileEntities) {
			Chunk chunk = world.getChunk((startX + (int)tileEntity.getRelX()) >> 4, (startZ + (int)tileEntity.getRelZ()) >> 4);
			if (chunk == null)
				continue;

			NBTTagCompound compound = (NBTTagCompound) tileEntity.getCompound().clone();
			compound.setInt("x", startX + (int)tileEntity.getRelX());
			compound.setInt("y", (int)tileEntity.getRelY());
			compound.setInt("z", startZ + (int)tileEntity.getRelZ());

			chunk.addTileEntity(compound);
		}
	}

}
