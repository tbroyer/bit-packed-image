/*
 * BpiConvert - Converts images to Bit-Packed Image format
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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import net.ltgt.imageio.plugins.bpi.BpiImageMetadata;


/**
 *
 * @author Thomas Broyer
 */
public class BPIConvert {
    
    /** Creates a new instance of BPIConverter */
    private BPIConvert() {
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
	System.err.println("Usage: converter [-d depth] [-p] [-o output] inFiles...");
	System.err.println("    -d    specify BPI depth (number from 0 to 15)");
	System.err.println("    -p    if set, creates a indexed image");
	System.err.println("    -o    specify the output file or directory.");
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

	List inFiles = new ArrayList();
	int depth = 15;
	boolean depthSet = false;
	boolean createPalette = false;
	boolean createPaletteSet = false;
	String out = ".";
	boolean outSet = false;

	int processedArgs = 0;
	for (; processedArgs < Math.min(args.length, 5); processedArgs++) {
	    String arg = args[processedArgs].toLowerCase();
	    if (arg.equals("-d") || arg.equals("-depth") || arg.startsWith("-d")) {
		if (depthSet) {
		    usage("depth specified more than once");
		    return;
		}
		if (arg.equals("-d") || arg.equals("-depth")) {
		    arg = args[++processedArgs];
		} else if (arg.startsWith("-d:")) {
		    arg = arg.substring(3);
		} else {
		    arg = arg.substring(2);
		}
		try {
		    depth = Integer.parseInt(arg, 10);
		} catch (NumberFormatException nfe) {
		    usage("depth is not a number");
		    return;
		}
		if ((depth < 0) || (depth > 15)) {
		    usage("depth is negative or greater than 15");
		    return;
		}
		depthSet = true;
	    } else if (arg.equals("-p") || arg.equals("-palette")) {
		if (createPaletteSet) {
		    usage("create palette specified more than once");
		    return;
		}
		createPalette = true;
		createPaletteSet = true;
	    } else if (arg.equals("-p+") || arg.equals("-p-")) {
		if (createPaletteSet) {
		    usage("create palette specified more than once");
		    return;
		}
		createPalette = arg.equals("-p+");
		createPaletteSet = true;
	    } else if (arg.equals("-o") || arg.equals("-out")) {
		if (outSet) {
		    usage("output specified more than once");
		    return;
		}
		out = args[++processedArgs];
		outSet = true;
	    } else {
		break;
	    }
	}

	while (processedArgs < args.length) {
	    inFiles.add(args[processedArgs++]);
	}

	ImageIO.scanForPlugins();

	File outFile = new File(out);
	if (!outFile.exists()) {
	    error("out does not exists");
	    System.exit(-1);
	}
	if ((inFiles.size() > 1) && !outFile.isDirectory()) {
	    usage("out must be a directory when processing more than one file");
	}

	ColorModel cm = (new BpiImageMetadata(depth)).createCompatibleColorModel();

	if (createPalette) {
	    warning("create palette ignored");
	}

	for (Iterator iter = inFiles.iterator(); iter.hasNext(); ) {
	    String file = (String) iter.next();
	    String suffix = file.substring(file.lastIndexOf('.') + 1);
	    File f = new File(file);
	    if (!f.exists()) {
		error("file " + file + " does not exists");
		continue;
	    }
	    if (!f.isFile()) {
		error(file + " is not a file");
		continue;
	    }

	    System.out.println("Processing file " + file + "...");

	    BufferedImage image = null;
	    try {
		image = ImageIO.read(f);
	    } catch (IOException ioe) {
		error("unable to read file " + file, ioe);
		continue;
	    }
	    if (image == null) {
		error("unable to read file " + file);
		continue;
	    }

	    BufferedImage dstImage = new BufferedImage(cm,
		    cm.createCompatibleWritableRaster(image.getWidth(), image.getHeight()),
		    cm.isAlphaPremultiplied(), null);
	    Graphics g = dstImage.getGraphics();
	    g.drawImage(image, 0, 0, null);

	    File outputFile = outFile;
	    if (outputFile.isDirectory()) {
		outputFile = new File(outputFile, f.getName().substring(0, f.getName().length() - suffix.length()) + "bpi");
	    }
	    if (outputFile.exists()) {
		error("file " + outputFile.getPath() + " already exists");
		continue;
	    }
	    try {
		ImageIO.write(dstImage, "bpi", outputFile);
	    } catch (IOException ioe) {
		error("unable to write file " + outputFile.getPath());
		continue;
	    }
	}

	System.out.println("Done.");
    }
}
