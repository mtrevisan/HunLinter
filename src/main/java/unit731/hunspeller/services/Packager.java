package unit731.hunspeller.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.Deflater;


public class Packager{

	private static final Logger LOGGER = LoggerFactory.getLogger(Packager.class);

	private static final ZipManager ZIPPER = new ZipManager();

	public static final String FOLDER_META_INF = "META-INF";
	public static final String FILENAME_DESCRIPTION_XML = "description.xml";
	public static final String FILENAME_MANIFEST_XML = "manifest.xml";
	public static final String FILENAME_MANIFEST_JSON = "manifest.json";
	public static final String EXTENSION_ZIP = ".zip";


	public void createPackage(final File affFile){
		final Path basePath = getPackageBaseDirectory(affFile);

		//package entire folder with ZIP
		if(basePath != null){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Found base path on folder {}", basePath.toString());

			try{
				final Path manifestPath = Paths.get(basePath.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML);
				if(existFile(manifestPath)){
					//TODO
					//read FILENAME_MANIFEST_XML into META-INF
					System.out.println("manifest file found");
					//collect all manifest:file-entry
					//for each file
						//search for node oor:name="Paths"
						//for each sub-node
							//search for node oor:name="AutoCorrect"
								//zip directory into .dat
							//search for node oor:name="AutoText"
								//zip directory into .bau
				}

				//TODO exclude all content inside AutoCorrect and AutoText folders that does not ends in .dat or .bau
				final String outputFilename = basePath.toString() + File.separator + basePath.getName(basePath.getNameCount() - 1) + EXTENSION_ZIP;
				ZIPPER.zipDirectory(basePath.toFile(), Deflater.BEST_COMPRESSION, outputFilename);

				LOGGER.info(Backbone.MARKER_APPLICATION, "Package created");

				//open directory
				if(Desktop.isDesktopSupported())
					Desktop.getDesktop().open(new File(basePath.toString()));
			}
			catch(final IOException e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "Package error: {}", e.getMessage());

				LOGGER.error("Something very bad happened while creating package", e);
			}
		}
	}

	/** Go up directories until description.xml or manifest.json is found */
	private Path getPackageBaseDirectory(final File affFile){
		Path parentPath = affFile.toPath().getParent();
		while(parentPath != null && !existFile(parentPath, FILENAME_DESCRIPTION_XML) && !existFile(parentPath, FILENAME_MANIFEST_JSON))
			parentPath = parentPath.getParent();
		return parentPath;
	}

	private boolean existFile(final Path path){
		return Files.isRegularFile(path);
	}

	private boolean existFile(final Path path, final String filename){
		return Files.isRegularFile(Paths.get(path.toString(), filename));
	}

}
