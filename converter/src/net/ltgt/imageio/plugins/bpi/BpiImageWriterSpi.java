/*
 * Bpi Image/IO - a Bit-Packed Image codec for Image/IO
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

package net.ltgt.imageio.plugins.bpi;

import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.color.ColorSpace;
import java.awt.image.SampleModel;
import java.io.File;

/**
 *
 * @author  Thomas Broyer
 */
public class BpiImageWriterSpi extends ImageWriterSpi {
    
    /** Creates a new instance of BpiImageWriterSpi */
    public BpiImageWriterSpi() {
	super(BpiCodec.AUTHOR, BpiCodec.VERSION, BpiCodec.FORMAT_NAMES,
	    BpiCodec.SUFFIXES, BpiCodec.MIME_TYPES,
	    BpiImageWriter.class.getName(),
	    new Class[] { ImageOutputStream.class, File.class },
	    new String[] { BpiImageReaderSpi.class.getName() },
	    false,
	    BpiStreamMetadata.nativeMetadataFormatName,
	    BpiStreamMetadataFormat.class.getName(),
	    null, null,
	    true,
	    BpiImageMetadata.nativeMetadataFormatName,
	    BpiImageMetadataFormat.class.getName(),
	    null, null);
    }

    public boolean canEncodeImage (ImageTypeSpecifier type) {
	return BpiCodec.isCompatibleImageType(type);
    }

    public ImageWriter createWriterInstance (Object extension) {
	return new BpiImageWriter(this);
    }

    public String getDescription (java.util.Locale locale) {
	return "The bit-packed image format is well-suited for J2ME devices as it is a simple, lightweight format.";
    }
}
