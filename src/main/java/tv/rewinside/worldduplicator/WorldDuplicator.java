package tv.rewinside.worldduplicator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import tv.rewinside.worldduplicator.region.LevelReader;
import tv.rewinside.worldduplicator.region.RegionFileReader;
import tv.rewinside.worldduplicator.region.RegionFileWriter;
import tv.rewinside.worldduplicator.region.RegionName;
import tv.rewinside.worldduplicator.util.ContentFinder;
import tv.rewinside.worldduplicator.util.Schematic;
import tv.rewinside.worldduplicator.world.Chunk;

public class WorldDuplicator {

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("in", true, "The input world folder");
		options.addOption("out", true, "The output world folder");
		options.addOption("amount", true, "How many copies of the world should get maked");

		CommandLine cmd;
		try {
			CommandLineParser parser = new DefaultParser();
			cmd = parser.parse(options, args);

			if (!cmd.hasOption("in") || !cmd.hasOption("out") || !cmd.hasOption("amount")) {
				new HelpFormatter().printHelp("java -jar WorldDuplicator.jar", options, true);
				return;
			}
		} catch (ParseException ex) {
			System.err.println("Argument parsing failed.");
			ex.printStackTrace();
			return;
		}

		File inFolder = new File(cmd.getOptionValue("in"));
		if (!inFolder.isDirectory()) {
			System.err.println("Can't find input directory.");
			return;
		}

		File levelDatFile = new File(inFolder, "level.dat");
		if (!levelDatFile.exists()) {
			System.err.println("Missing level.dat in input directory.");
			return;
		}

		File regionFolder = new File(inFolder, "region");
		if (!regionFolder.isDirectory()) {
			System.err.println("Missing region folder in input directory.");
			return;
		}

		File outFolder = new File(cmd.getOptionValue("out"));
		try {
			FileUtils.copyDirectory(inFolder, outFolder, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("region")), true);
		} catch (IOException ex) {
			System.err.println("Exception while copying in folder to out");
			ex.printStackTrace();
			return;
		}

		File outRegionFolder = new File(outFolder, "region");
		outRegionFolder.mkdir();

		int amount;
		try {
			amount = Integer.parseInt(cmd.getOptionValue("amount"));
		} catch (NumberFormatException ex) {
			System.err.println(cmd.getOptionValue("amount") + " is not a valid number.");
			return;
		}

		try {
			System.out.println("Load level.dat ...");
			LevelReader levelReader = new LevelReader();
			levelReader.readFile(levelDatFile);
			System.out.println(levelReader.toString());

			System.out.println("Read world ...");
			RegionFileReader reader = new RegionFileReader();
			for (File file : regionFolder.listFiles((dir, name) -> name.endsWith(".mca")))
				reader.readFile(file);
			System.out.println("Loaded world with " + reader.getWorld().getChunks().size() + " chunks.");

			System.out.println("Find region ...");
			ContentFinder finder = new ContentFinder(reader.getWorld());
			finder.startCheck(levelReader.getSpawnX(), levelReader.getSpawnZ());
			System.out.println(String.format("Region: (x) %d - %d, (z) %d - %d", finder.getMinX(), finder.getMaxX(), finder.getMinZ(), finder.getMaxZ()));

			System.out.println("Create schematic from the region ...");
			Schematic schematic = new Schematic(finder.getMaxX() - finder.getMinX() + 1, finder.getMaxZ() - finder.getMinZ() + 1);
			schematic.readFromWorld(reader.getWorld(), finder.getMinX(), finder.getMinZ());
			System.out.println("Created schematic with " + (schematic.getBlocksX() * schematic.getBlocksZ()) + " blocks.");

			for (int x = 0; x < amount; x++) {
				schematic.placeToWorld(reader.getWorld(), finder.getMinX() + ((x+1) * schematic.getBlocksX()), finder.getMinZ());
				System.out.println("Placed schematic.");
			}
			System.out.println("Done. Save world ...");

			// Save
			{
				Map<RegionName, List<Chunk>> regionChunks = reader.getWorld().getChunks().stream()
						.collect(Collectors.groupingBy(c -> RegionName.byChunkCoords(c.getLocX(), c.getLocZ())));

				RegionFileWriter writer = new RegionFileWriter();
				for (Map.Entry<RegionName, List<Chunk>> e : regionChunks.entrySet()) {
					writer.writeRegionFile(new File(outRegionFolder, e.getKey().toString()), e.getKey(), e.getValue());
				}
				System.out.println("Saved new world.");
			}

			levelReader.saveWorldDuplicatorData(new File(outFolder, "data/worldduplicator.dat"), finder.getMinX(), finder.getMinZ(), schematic.getBlocksX(), schematic.getBlocksZ());
			System.out.println("Saved worldduplicator.dat");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
