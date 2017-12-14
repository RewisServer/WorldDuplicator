package tv.rewinside.worldduplicator.util;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import tv.rewinside.worldduplicator.world.Chunk;
import tv.rewinside.worldduplicator.world.World;

@RequiredArgsConstructor
@Data
public class ContentFinder {
	private final World world;
	private int minX;
	private int minZ;
	private int maxX;
	private int maxZ;

	private final Set<Integer> checkedChunks = new HashSet<>();

	public void startCheck(int x, int z) {
		this.minX = this.maxX = x;
		this.minZ = this.maxZ = z;

		this.checkedChunks.clear();
		this.checkChunk(x >> 4, z >> 4);
		this.checkedChunks.clear();
	}

	private void checkChunk(int chunkX, int chunkZ) {
		int index = World.toChunkIndex(chunkX, chunkZ);
		if (!this.checkedChunks.add(index)) {
			return;
		}

		Chunk chunk = this.world.getChunk(chunkX, chunkZ);
		if (chunk == null)
			return;

		boolean foundBlocks = false;
		int coordsX = chunkX * 16;
		int coordsZ = chunkZ * 16;

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				if (!chunk.hasBlock(x, z))
					continue;

				foundBlocks = true;

				int blockX = coordsX + x;
				int blockZ = coordsZ + z;
				if (blockX < this.minX)
					this.minX = blockX;
				else if (blockX > this.maxX)
					this.maxX = blockX;

				if (blockZ < this.minZ)
					this.minZ = blockZ;
				else if (blockZ > this.maxZ)
					this.maxZ = blockZ;
			}
		}

		if (foundBlocks) {
			this.checkChunk(chunkX - 1, chunkZ);
			this.checkChunk(chunkX + 1, chunkZ);
			this.checkChunk(chunkX, chunkZ - 1);
			this.checkChunk(chunkX, chunkZ + 1);
		}
	}

}
