package tv.rewinside.worldduplicator.region;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import tv.rewinside.worldduplicator.world.World;

public class RegionFileReader {
	@Getter private final World world;

	public RegionFileReader() {
		this.world = new World();
	}

	public void readFile(File file) throws IOException, DataFormatException {
		String[] fileName = file.getName().split("\\.");
		if (fileName.length != 4)
			throw new RuntimeException("Filename needs the format \"r.{x}.{z}.mca\"");
		int regionX = Integer.parseInt(fileName[1]);
		int regionZ = Integer.parseInt(fileName[2]);

		ChunkInformation[] chunks = new ChunkInformation[1024];
		byte[] data;

		try (DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			// Read chunk offsets
			for (int i = 0; i < 1024; i++) {
				int offset = readThreeBytes(is);
				is.readByte();  // Skip rounded chunk size

				int chunkX = (regionX * 32) + (i % 32);
				int chunkZ = (regionZ * 32) + (int)(i / 32D);

				chunks[i] = new ChunkInformation(chunkX, chunkZ, offset);
			}

			// Read chunk last modified dates
			for (int i = 0; i < 1024; i++) {
				chunks[i].lastChange = is.readInt();
			}

			data = IOUtils.toByteArray(is);
		}

		Inflater decompressor = new Inflater();

		for (ChunkInformation chunkInfo : chunks) {
			// Chunk doesn't exists
			if (chunkInfo.offset == 0)
				continue;

			int offset = (chunkInfo.offset - 2) * 4096;
			int length = readInt(data, offset) - 1;
			byte compressionFormat = data[offset + 4];  // 1 = gzip, 2 = zlib
			if (compressionFormat != 2)
				throw new RuntimeException(String.format("Chunk %d,%d has unknown compression format %d, only 2 (zlib) is supported.", chunkInfo.x, chunkInfo.z, compressionFormat));

			// Decompress chunk data
			decompressor.setInput(data, offset + 5, length);
			byte[] chunkData = decompressFull(decompressor, length);
			decompressor.reset();

			//System.out.println(String.format("Read chunk %d,%d (offset: %d, len: %d)", chunkInfo.x, chunkInfo.z, offset, chunkData.length));

			this.world.loadChunk(chunkInfo.x, chunkInfo.z, chunkInfo.lastChange, chunkData);
		}

		decompressor.end();
	}

	private static byte[] decompressFull(Inflater decompressor, int compressedSize) throws IOException, DataFormatException {
		byte[] buffer = new byte[1024];
		try (ByteArrayOutputStream output = new ByteArrayOutputStream(compressedSize)) {
			while (!decompressor.finished()) {
				int count = decompressor.inflate(buffer);
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}

	private static int readThreeBytes(DataInputStream is) throws IOException {
		byte i1 = is.readByte();
		byte i2 = is.readByte();
		byte i3 = is.readByte();
		return ((i1 & 0x0F) << 16) | ((i2 & 0xFF) << 8) | (i3 & 0xFF);
	}

	private static int readInt(byte[] array, int offset) throws IOException {
		return ((array[offset] & 0x0F) << 24) | ((array[offset + 1] & 0xFF) << 16) | ((array[offset + 2] & 0xFF) << 8) | (array[offset + 3] & 0xFF);
	}

	@RequiredArgsConstructor
	private static class ChunkInformation {
		public final int x;
		public final int z;
		public final int offset;
		public int lastChange;
	}

}
