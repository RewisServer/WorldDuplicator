package tv.rewinside.worldduplicator.world;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import tv.rewinside.nbtstorage.nbt.NBTCompressedStreamTools;
import tv.rewinside.nbtstorage.nbt.NBTReadLimiter;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;

public class World {
	private final Map<Integer, Chunk> chunks = new HashMap<>();

	public Collection<Chunk> getChunks() {
		return this.chunks.values();
	}

	public Chunk getChunk(int x, int z) {
		return this.chunks.get(toChunkIndex(x, z));
	}

	public boolean hasBlock(int x, int z) {
		Chunk chunk = this.getChunk(x >> 4, z >> 4);
		return (chunk != null && chunk.hasBlock(x & 0xF, z & 0xF));
	}

	public Chunk loadChunk(int x, int z, int lastChanged, byte[] nbtData) throws IOException {
		try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(nbtData))) {
			NBTTagCompound nbt = NBTCompressedStreamTools.readLimited(is, NBTReadLimiter.NO_LIMIT);

			return this.loadChunk(x, z, lastChanged, nbt.getCompound("Level"));
		}
	}

	public Chunk loadChunk(int x, int z, int lastChanged, NBTTagCompound nbt) {
		Chunk chunk = new Chunk(x, z, nbt, lastChanged);
		this.chunks.put(toChunkIndex(x, z), chunk);
		chunk.load();

		return chunk;
	}

	public static int toChunkIndex(int x, int z) {
		return ((x & 0xFFFF) << 16) | (z & 0xFFFF);
	}

}
