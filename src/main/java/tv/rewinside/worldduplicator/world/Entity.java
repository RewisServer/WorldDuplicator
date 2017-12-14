package tv.rewinside.worldduplicator.world;

import lombok.AllArgsConstructor;
import lombok.Data;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;

@Data
@AllArgsConstructor
public class Entity {
	private double relX;
	private double relY;
	private double relZ;
	private NBTTagCompound compound;

}
