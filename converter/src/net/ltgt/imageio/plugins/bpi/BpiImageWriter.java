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

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.spi.*;
import javax.imageio.stream.*;
import java.util.*;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.*;

/**
 *
 * @author  Thomas Broyer
 */
public class BpiImageWriter extends ImageWriter {
    private ImageOutputStream stream = null;
    private File metadataFile = null;

    private int currentRaster = 0;

    static private final int READY = 0;
    static private final int IN_WRITE_SEQUENCE = 1;
    static private final int IN_WRITE_EMPTY = 2;
    static private final int IN_REPLACE_PIXELS = 4;
    private int level = READY;

    /** Creates a new instance of BpiImageWriter */
    public BpiImageWriter() {
	this(null);
    }

    /**
     * Creates a new instance of BpiImageWriter
     * @param originatingProvider Service provider which creates this instance.
     */
    public BpiImageWriter (ImageWriterSpi originatingProvider) {
	super(originatingProvider);
    }

    public void setOutput (Object output) {
	super.setOutput(output);

	if (output instanceof ImageOutputStream) {
	    stream = (ImageOutputStream) output;
	    metadataFile = null;
	} else if (output instanceof File) {
	    metadataFile = BpiCodec.getDescriptorFile((File) output);
	    try {
		stream = ImageIO.createImageOutputStream(output);
	    } catch (IOException ioe) {
		throw new IllegalArgumentException("ImageOutputStream cannot be created on the specified output file");
	    }
	}
	currentRaster = 0;
    }

    public ImageWriteParam getDefaultWriteParam() {
	return new BpiImageWriteParam(getLocale());
    }

    public IIOMetadata getDefaultStreamMetadata (ImageWriteParam param) {
	return null;
    }

    public IIOMetadata getDefaultImageMetadata (ImageTypeSpecifier type, ImageWriteParam param) {
	return BpiImageMetadata.inferFrom(type);
    }

    public IIOMetadata convertStreamMetadata (IIOMetadata inData, ImageWriteParam param) {
	if (inData instanceof BpiStreamMetadata) {
	    return new BpiStreamMetadata((BpiStreamMetadata) inData);
	}
//	try {
//	    BpiStreamMetadata metadata = new BpiStreamMetadata();
//	    metadata.setFromTree("net_ltgt_bpi_stream_1.0", inData.getAsTree("net_ltgt_bpi_stream_1.0"));
//	    return metadata;
//	} catch (Exception ex) {
	    return null;
//	}
    }

    public IIOMetadata convertImageMetadata (IIOMetadata inData, ImageTypeSpecifier type, ImageWriteParam param) {
	// TODO: deal with type
	if (inData instanceof BpiImageMetadata) {
	    return new BpiImageMetadata((BpiImageMetadata) inData);
	}
	try {
	    return new BpiImageMetadata(inData);
	} catch (Exception e) {
	    return null;
	}
    }

    public void write (IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) throws IOException {
	checkOutput();
	if (level != READY) {
	    throw new IllegalStateException("In write sequence, write empty or replace pixels");
	}

	BpiImageMetadata imageMetadata = (BpiImageMetadata) convertImageMetadata(image.getMetadata(),
		(param == null) ? null : param.getDestinationType(), param);
	Raster raster = null;
	if (image.hasRaster()) {
	    raster = image.getRaster();
	    if (imageMetadata == null) {
		throw new IllegalArgumentException("Cannot write a raster without a color model.");
	    }
	} else {
	    raster = image.getRenderedImage().getData();
	}
	if (imageMetadata != null) {
	    ColorModel cm = imageMetadata.createCompatibleColorModel();
	    if (!cm.isCompatibleRaster(raster)) {
		throw new IllegalArgumentException("Image metadata and raster are not compatible.");
	    }
	} else {
	    imageMetadata = BpiImageMetadata.inferFrom(image.getRenderedImage());
	}
	writeImageMetadata(imageMetadata);
	writeRaster(currentRaster++, raster);
    }

    public boolean canWriteSequence() {
	return true;
    }

    private BpiStreamMetadata descriptor = null;
    private int currentBlock = 0;

    public void prepareWriteSequence (IIOMetadata streamMetadata) throws IOException {
	checkOutput();

	if (level == IN_WRITE_SEQUENCE)
	    throw new IllegalStateException("Already in write sequence");
	if (level != READY)
	    throw new IllegalStateException("Writing empty or replacing pixels");

	descriptor = (BpiStreamMetadata) convertStreamMetadata(streamMetadata, null);
	currentBlock = 0;
    }

    public void writeToSequence (IIOImage image, ImageWriteParam param) throws IOException {
	checkOutput();
	if (level != IN_WRITE_SEQUENCE)
	    throw new IllegalStateException("Not in write sequence");

	if (descriptor == null) {
	    BpiImageMetadata imageMetadata = (BpiImageMetadata) convertImageMetadata(image.getMetadata(), param.getDestinationType(), param);
	    Raster raster = null;
	    if (image.hasRaster()) {
		raster = image.getRaster();
		if (imageMetadata == null) {
		    throw new IllegalArgumentException("Cannot write a raster if there is no descriptor.");
		}
	    } else {
		raster = image.getRenderedImage().getData();
	    }
	    if (imageMetadata != null) {
		ColorModel cm = imageMetadata.createCompatibleColorModel();
		if (!cm.isCompatibleRaster(raster)) {
		    throw new IllegalArgumentException("Image metadata and raster are not compatible.");
		}
	    } else {
		imageMetadata = BpiImageMetadata.inferFrom(image.getRenderedImage());
	    }
	    writeImageMetadata(imageMetadata);
	    writeRaster(currentRaster++, raster);
	} else {
	    if (descriptor.isColorModel(currentBlock)) {
		BpiImageMetadata imageMetadata = (BpiImageMetadata) convertImageMetadata(image.getMetadata(), param.getDestinationType(), param);
		if (imageMetadata == null) {
		    if (image.hasRaster()) {
			throw new IllegalArgumentException("Found a raster, expected a color model.");
		    }
		    imageMetadata = BpiImageMetadata.inferFrom(image.getRenderedImage());
		}
		currentBlock++;
		writeImageMetadata(imageMetadata);
	    }
	    if (!descriptor.isRaster(currentBlock)) {
		throw new IllegalArgumentException("Found a color model, expected a raster.");
	    }
	    currentBlock++;
	    Raster raster = image.hasRaster() ? image.getRaster() : image.getRenderedImage().getData();
	    writeRaster(currentRaster++, raster);
	}
    }

    public void writeToSequence (BpiImageMetadata metadata) throws IOException {
	checkOutput();
	checkOutput();
	if (level != IN_WRITE_SEQUENCE)
	    throw new IllegalStateException("Not in write sequence");

	if (descriptor == null) {
	    throw new IllegalStateException("Can't write image metadata alone without a provided descriptor.");
	}
	if (!descriptor.isColorModel(currentBlock)) {
	    throw new IllegalStateException("Found a raster, expected a color model.");
	}
	currentBlock++;
	writeImageMetadata(metadata);
    }

    public void endWriteSequence() throws IOException {
	checkOutput();
	if (level != IN_WRITE_SEQUENCE)
	    throw new IllegalStateException("Not in write sequence");

	if (descriptor != null) {
	    if (currentBlock != descriptor.getNumBlocks()) {
		processWarningOccurred(-1, "Sequence is not complete while ending.");
	    }
	    writeStreamMetadata(descriptor);
	}

	descriptor = null;
	currentBlock = 0;

	level = READY;
    }

//    public boolean canWriteEmpty() {
//	return true;
//    }
//
//    public void prepareWriteEmpty (IIOMetadata streamMetadata, ImageTypeSpecifier imageType,
//	int width, int height, IIOMetadata imageMetadata, List thumbnails, ImageWriteParam param) {
//	checkOutput();
//	if ((level & IN_WRITE_EMPTY) == IN_WRITE_EMPTY)
//	    throw new IllegalStateException("Alread in write empty");
//	if (level != READY)
//	    throw new IllegalStateException("In write sequence or replace pixels");
//	// TODO
//	throw new UnsupportedOperationException("Not yet implemented.");
//    }
//
//    public void endWriteEmpty() {
//	checkOutput();
//	if ((level & IN_WRITE_EMPTY) == 0)
//	    throw new IllegalStateException("Not in write empty");
//	if (level != IN_WRITE_EMPTY)
//	    throw new IllegalStateException("In inner replace pixels");
//	level = READY;
//    }

//    public boolean canReplacePixels (int imageIndex) {
//	// TODO: check index
//	return true;
//    }
//
//    public void prepareReplacePixels (int imageIndex, Rectangle region) {
//	checkOutput();
//	if ((level & IN_REPLACE_PIXELS) == IN_REPLACE_PIXELS)
//	    throw new IllegalStateException("Already in replace pixels");
//	if ((level != READY) || (level != IN_WRITE_EMPTY))
//	    throw new IllegalStateException("In write sequence");
//	// TODO
//	throw new UnsupportedOperationException("Not yet implemented.");
//    }
//
//    public void replacePixels (RenderedImage image, ImageWriteParam param) {
//	checkOutput();
//	if ((level & IN_REPLACE_PIXELS) == 0)
//	    throw new IllegalStateException("Not in replace pixels");
//	// TODO
//	throw new UnsupportedOperationException("Not yet implemented.");
//    }
//
//    public void replacePixels (Raster raster, ImageWriteParam param) {
//	checkOutput();
//	if ((level & IN_REPLACE_PIXELS) == 0)
//	    throw new IllegalStateException("Not in replace pixels");
//	// TODO
//	throw new UnsupportedOperationException("Not yet implemented.");
//    }
//
//    public void endReplacePixels() {
//	checkOutput();
//	if ((level & IN_REPLACE_PIXELS) == 0)
//	    throw new IllegalStateException("Not in replace pixels");
//	level ^= IN_REPLACE_PIXELS;
//    }

    private void checkOutput() {
	if (output == null)
	    throw new IllegalStateException("output not set");
    }

    private void writeStreamMetadata (BpiStreamMetadata metadata) throws IOException {
	if (metadataFile != null) {
	    BpiCodec.writeDescriptor(metadataFile, metadata);
	}
    }

    private void writeImageMetadata (BpiImageMetadata metadata) throws IOException {
	BpiCodec.writeColorModel(stream, metadata);
    }

    private void writeRaster (int imageIndex, Raster raster) throws IOException {
	int width = raster.getWidth();
	int height = raster.getHeight();

	BpiCodec.writeRasterSize(stream, width, height);

	int bitsPerPixel = 0;
	int[] sampleSize = raster.getSampleModel().getSampleSize();
//	for (int i = 0; i < sampleSize.length; i++) {
//	    bitsPerPixel += sampleSize[i];
//	}

	processImageStarted(imageIndex);
	int[] row = new int[width];
	for (int y = 0; y < height; y++) {
	    for (int x = 0; x < width; x++) {
		int[] pixel = raster.getPixel(x, y, (int[]) null);
		if ((pixel.length % 2) == 0) {
		    stream.writeBits(pixel[pixel.length - 1], sampleSize[sampleSize.length - 1]);
		    for (int i = 0; i < pixel.length - 1; i++) {
			stream.writeBits(pixel[i], sampleSize[i]);
		    }
		} else {
		    for (int i = 0; i < pixel.length; i++) {
			stream.writeBits(pixel[i], sampleSize[i]);
		    }
		}
//		stream.writeBits(dataElement, bitsPerPixel);
	    }
	    processImageProgress(100.0F/height);
	}
	processImageComplete();
    }
}

/*public*/ class BpiImageWriteParam extends ImageWriteParam {
//    private boolean createPalette = false;

    BpiImageWriteParam (Locale locale) {
	super();
	this.locale = locale;

	canWriteTiles = false;
	canOffsetTiles = false;
	canWriteProgressive = false;
	canWriteCompressed = false;
    }

//    public void setCreatePalette (boolean value) {
//	createPalette = value;
//    }
//
//    public boolean getCreatePalette() {
//	return createPalette;
//    }
}
