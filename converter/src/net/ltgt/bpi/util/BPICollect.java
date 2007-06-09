/*
 * BpiCollect - Collect Bit-Packed images into a BPI collection
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

/**
 *
 * @author Thomas Broyer
 */
public class BPICollect {
    
    /** Creates a new instance of BPICollect */
    private BPICollect() {
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
	System.err.println("Usage: collect collection bpiFiles...");
	System.err.println("  collection   output BPI collection file.");
	System.err.println("  bpiFiles...  list of input BPI files (single-image or collections).");

	System.exit(-1);
    }

    static public void main (String[] args) {
	if (args.length < 3) {
	    error("You must supply at least 3 arguments.");
	}
	File collection = new File(args[0]);
	if (collection.exists())
	    collection.delete();

	ImageIO.scanForPlugins();

	Iterator writers = ImageIO.getImageWritersByFormatName("bpi");
	if ((writers == null) || !writers.hasNext()) {
	    error("no provider for BPI image files.");
	    System.exit(-1);
	}
	ImageWriter writer = (ImageWriter) writers.next();

	Iterator readers = ImageIO.getImageReadersByFormatName("bpi");
	if ((readers == null) || !readers.hasNext()) {
	    error("no provider for BPI files.");
	    System.exit(-1);
	}
	ImageReader reader = (ImageReader) readers.next();

	writer.setOutput(collection);

	boolean useSequence = writer.canWriteSequence();
	if (useSequence) {
	    try {
		writer.prepareWriteSequence(null);
	    } catch (IOException ioe) {
		useSequence = false;
	    }
	}
	
	for (int i = 1; i < args.length; i++) {
	    try {
		reader.setInput(new File(args[i]));
		IIOImage srcImage;
		try {
		    srcImage = reader.readAll(0, null);
		} catch (IOException ioe) {
		    error("error reading image " + i);
		    continue;
		}
		try {
		    if (useSequence)
			writer.writeToSequence(srcImage, null);
		    else
			writer.write(srcImage);
		} catch (IOException ioe) {
		    error("error writing image " + i);
		    continue;
		}
	    } catch (Exception ex) {
		error("error processing image " + i);
	    }
	    System.out.println("Processed image " + i);
	}

	if (useSequence) {
	    try {
		writer.endWriteSequence();
	    } catch (IOException ioe) {
		error("error ending image sequence in output file");
	    }
	}

	System.out.println("Done.");
    }
}
