package tv.rewinside.worldduplicator.region;

import java.io.File;
import java.io.IOException;
import lombok.Data;
import lombok.ToString;
import tv.rewinside.nbtstorage.NBTStorage;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;

/**
 * Reads level.dat files
 */
@Data
@ToString(exclude="nbt")
public class LevelReader {
	private NBTTagCompound nbt;
	private int spawnX;
	private int spawnY;
	private int spawnZ;

	public void readFile(File file) throws IOException {
		this.nbt = NBTStorage.read(file);
		NBTTagCompound compound = this.nbt.getCompound("Data");

		this.spawnX = compound.getInt("SpawnX");
		this.spawnY = compound.getInt("SpawnY");
		this.spawnZ = compound.getInt("SpawnZ");
	}

	public void saveWorldDuplicatorData(File file, int startX, int startZ, int blocksX, int blocksZ) throws IOException {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInt("StartX", startX);
		compound.setInt("StartZ", startZ);
		compound.setInt("BlocksX", blocksX);
		compound.setInt("BlocksZ", blocksZ);

		NBTStorage.write(compound, file);
	}

}
