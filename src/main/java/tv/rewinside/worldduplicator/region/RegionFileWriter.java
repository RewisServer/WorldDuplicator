package tv.rewinside.worldduplicator.region;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.Deflater;
import tv.rewinside.nbtstorage.nbt.NBTCompressedStreamTools;
import tv.rewinside.nbtstorage.nbt.NBTTagCompound;
import tv.rewinside.worldduplicator.world.Chunk;

public class RegionFileWriter {
	private static final byte[] EMPTY_BUFFER = new byte[4096];

	public void writeRegionFile(File file, RegionName region, Collection<Chunk> chunks) throws IOException {
		Chunk[] sortedChunks = new Chunk[32 * 32];
		int regionX = region.getX() * 32, regionZ = region.getZ() * 32;
		for (Chunk chunk : chunks) {
			int relX = chunk.getLocX() - regionX, relZ = chunk.getLocZ() - regionZ;
			sortedChunks[relZ * 32 + relX] = chunk;
		}

		ByteArrayOutputStream chunkInfoOutput = new ByteArrayOutputStream(4096);
		DataOutputStream chunkInfoOutputStream = new DataOutputStream(chunkInfoOutput);

		ByteArrayOutputStream chunkTimeOutput = new ByteArrayOutputStream(4096);
		DataOutputStream chunkTimeOutputStream = new DataOutputStream(chunkTimeOutput);

		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		DataOutputStream byteOutputStream = new DataOutputStream(byteOutput);

		Deflater compressor = new Deflater(Deflater.DEFAULT_COMPRESSION);
		ByteArrayOutputStream tempOutput = new ByteArrayOutputStream(4096);

		int currentTime = (int) (System.currentTimeMillis() / 1000);
		byte[] buffer = new byte[1024];
		for (int i = 0; i < sortedChunks.length; i++) {
			Chunk chunk = sortedChunks[i];
			if (chunk == null) {
				chunkInfoOutputStream.writeInt(0);
				chunkTimeOutputStream.writeInt(0);
				continue;
			}

			if (chunk.isChanges())
				chunk.saveSectionsToNbt();

			// Create nbt data
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.set("Level", chunk.getNbt());
			try (DataOutputStream stream = new DataOutputStream(tempOutput)) {
				NBTCompressedStreamTools.writeCompound(nbt, stream);
				stream.flush();
			}
			byte[] nbtData = tempOutput.toByteArray();
			tempOutput.reset();


			// Compress nbt data
			int oldOffset = byteOutputStream.size();
			compressor.setInput(nbtData);
			compressor.finish();
			while (!compressor.finished()) {
				int count = compressor.deflate(buffer);
				tempOutput.write(buffer, 0, count);
			}
			compressor.reset();
			tempOutput.flush();

			byteOutputStream.writeInt(tempOutput.size() + 1);
			byteOutputStream.writeByte(0x2); // zlib compression
			byteOutputStream.write(tempOutput.toByteArray());
			tempOutput.reset();

			// Write chunk information
			int size = byteOutputStream.size() - oldOffset;
			int kilobyteSize = size / 4096 + 1;
			int chunkInfo = (((oldOffset / 4096 + 2) & 0xFFFFFF) << 8) | (kilobyteSize & 0xFF);
			chunkInfoOutputStream.writeInt(chunkInfo);

			// Write last change time information
			if (chunk.isChanges())
				chunkTimeOutputStream.writeInt(currentTime);
			else
				chunkTimeOutputStream.writeInt(chunk.getLastChanged());

			// Fill to 4096 byte
			int rest = byteOutputStream.size() % 4096;
			if (rest != 0) {
				byteOutputStream.write(EMPTY_BUFFER, 0, 4096 - rest);
			}
		}

		chunkInfoOutputStream.flush();
		chunkTimeOutputStream.flush();
		byteOutputStream.flush();

		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
			os.write(chunkInfoOutput.toByteArray());
			os.write(chunkTimeOutput.toByteArray());
			os.write(byteOutput.toByteArray());
			os.flush();
		}
	}

}
