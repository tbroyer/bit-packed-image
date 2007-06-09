/*
 * BpiExtract - Converts a Bit-Packed image or collection to other image formats
 *
 * Copyright (C) 2004  Thomas Broyer
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.ltgt.bpi.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;


/**
 *
 * @author Thomas Broyer
 */
public class BPIExtract {
    
    /** Creates a new instance of BPIConverter */
    private BPIExtract() {
    }

    static private void error (String error) {
	System.err.print("ERROR: ");
	System.err.println(error);
    }

    static private void error (String error, Throwable cause) {
	error(error);
	error("original error was: " + cause.getLocalizedMessage());
    }

    static private void warning (String error) {
	System.err.print("WARNING: ");
	System.err.println(error);
    }

    static private void usage (String error) {
	if (error != null) {
	    error(error);
	    System.err.println();
	}
	System.err.println("Usage: extract [-o output] bpiFile");
	System.err.println("    -o    specify the output directory.");
	System.err.println("          Must be a directory when processing more than one file.");

	System.exit(-1);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
	if (args.length == 0) {
	    usage(null);
	    return;
	}

	File out = new File(".");

	int processedArgs = 0;
	if (args[0].equals("-o") || args[0].equals("-out")) {
		out = new File(args[++processedArgs]);
		processedArgs++;
	}
	if (args.length < processedArgs + 1) {
	    usage("Missing input bpi file.");
	}
	if (args.length > processedArgs + 1) {
	    usage("Too many input bpi files, only one allowed.");
	}
	File in = new File(args[processedArgs]);

	ImageIO.scanForPlugins();

	if (!out.exists()) {
	    error("out does not exists");
	    System.exit(-1);
	}
	if (!out.isDirectory()) {
	    error("out is not a directory");
	    System.exit(-1);
	}

	Iterator readers = ImageIO.getImageReadersByFormatName("bpi");
	if ((readers == null) || !readers.hasNext()) {
	    error("No reader for BPI file format.");
	    System.exit(-1);
	}
	ImageReader reader = (ImageReader) readers.next();

	reader.setInput(in);

	int numImages = -1;
	try {
	    numImages = reader.getNumImages(false);
	} catch (IOException ioe) { }
	boolean hasDescriptor = true;
	if (numImages < 0) {
	    // if number of images is unknown, try to read as many images as possible
	    numImages = Integer.MAX_VALUE;
	    hasDescriptor = false;
	}

	String outFileBaseName = in.getName().substring(0, in.getName().lastIndexOf('.'));
	int i = 0;
	try {
	    while (i < numImages) {
		try {
		    BufferedImage srcImage = reader.read(i);
		    File outFile = new File(out, outFileBaseName + i + ".png");
		    if (outFile.exists()) {
			File backupFile = new File(outFile.getPath() + "~");
			warning("renaming " + outFile.getName() + " to " + backupFile.getName());
			if (backupFile.exists()) {
			    backupFile.delete();
			}
			if (!outFile.renameTo(backupFile)) {
			    error("error renaming file. Aborting.");
			    System.exit(-1);
			}
		    }
		    try {
			ImageIO.write(srcImage, "png", outFile);
			System.out.println("Processed image " + i + " to file " + outFile.getName());
		    } catch (IOException ioe) {
			error("error writing image " + i + ". Ignored.");
		    }
		} catch (IOException ioe) {
		    error("error reading image " + i + ". Ignored.");
		}
		i++;
	    }
	} catch (IndexOutOfBoundsException iobe) {
	    // ignore silently
	}

	System.out.println("Done.");
    }
}
