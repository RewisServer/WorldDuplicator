package tv.rewinside.worldduplicator.region;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionName {
	private final int x;
	private final int z;

	public static RegionName byChunkCoords(int chunkX, int chunkZ) {
		int regionX = (chunkX >= 0) ? (chunkX / 32) : ((chunkX + 1) / 32 - 1);
		int regionZ = (chunkZ >= 0) ? (chunkZ / 32) : ((chunkZ + 1) / 32 - 1);
		return new RegionName(regionX, regionZ);
	}

	@Override
	public int hashCode() {
		return ((this.x & 0xFFFF) << 16) | (this.z & 0xFFFF);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RegionName))
			return false;

		RegionName rn = (RegionName) obj;
		return (
				this == rn
				|| (this.x == rn.x && this.z == rn.z)
		);
	}

	@Override
	public String toString() {
		return String.format("r.%d.%d.mca", this.x, this.z);
	}

}
