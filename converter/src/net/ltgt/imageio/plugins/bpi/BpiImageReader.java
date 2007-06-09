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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.*;
import java.net.*;

/**
 * Subclass of {@link javax.imageio.ImageReader} for Bit-Packed Image reading.
 * @author Thomas Broyer
 */
public class BpiImageReader extends ImageReader {
    /**
     * {@link BpiStreamMetadata} used for the current input.
     * It is either read from a file (whose name is infered from the image filename) or
     * set by the <code>setInput</code> method.
     */
    private BpiStreamMetadata descriptor;

    /**
     * The {@link ImageInputStream} associated to the current input.
     */
    private ImageInputStream stream = null;

    /**
     * List of read BpiImageMetadata when a descriptor is used.
     */
    private List colorModels = new ArrayList();

    /**
     * Index of the last read image (or raster in case there is a descriptor).
     * @see imageMetadata
     * @see width
     * @see height
     */
    private int currentIndex = -1;
    /**
     * BpiImageMetadata of the last read image.
     * Used only when there is no descriptor.
     * @see currentIndex
     */
    private BpiImageMetadata imageMetadata = null;
    /**
     * Size of the last read image.
     * @see currentIndex
     */
    private Dimension imageSize = null;

    /**
     * Byte offset (from the start of the stream) of each raster, stored as a {@link Long}.
     * In the case where there is no descriptor, the offsets lead to the start of each 
     * color model (each one being immediately followed by its associated raster).
     */
    private List byteOffsets = new ArrayList(); // List of Longs
    /**
     * Bit offset inside the first byte of each raster, stored as an {@link Integer}.
     * @see byteOffsets
     */
    private List bitOffsets = new ArrayList(); // List of Integers
    /**
     * Number of images in the input, or <CODE>-1</CODE> if not yet computed.
     */
    private int numImages = -1;

    /** Creates a new instance of BpiImageReader */
    public BpiImageReader() {
	this(null);
    }

    /**
     * Creates a new instance of BpiImageReader
     * @param originatingProvider Service provider which creates this instance.
     */
    public BpiImageReader (ImageReaderSpi originatingProvider) {
	super(originatingProvider);
    }

    public ImageReadParam getDefaultReadParam() {
	return new BpiImageReadParam();
    }

    public void setInput (Object input, BpiStreamMetadata descriptor) {
	setInput(input, descriptor, false, false);
    }

    public void setInput (Object input, BpiStreamMetadata descriptor, boolean seekForwardOnly) {
	setInput(input, descriptor, seekForwardOnly, false);
    }

    public void setInput (Object input, BpiStreamMetadata descriptor,
	boolean seekForwardOnly, boolean ignoreMetadata) {
	super.setInput(input, seekForwardOnly, ignoreMetadata);

	this.descriptor = descriptor;
	if (input instanceof ImageInputStream) {
	    stream = (ImageInputStream) input;
	} else {
	    try {
		stream = ImageIO.createImageInputStream(input);
	    } catch (IOException ioe) {
		throw new IllegalArgumentException("Can't create ImageInputStream from input");
	    }
	}
	resetStreamSettings();
    }

    public void setInput (Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
	setInput(input, null, seekForwardOnly, ignoreMetadata);
    }

    public IIOMetadata getStreamMetadata() throws IOException {
	if (descriptor == null) {
	    BpiStreamMetadata desc = null;
	    if (input instanceof File) {
		File file = BpiCodec.getDescriptorFile((File) input);
		if (file.exists()) {
		    desc = BpiCodec.readDescriptor(file);
		}
	    } else if (input instanceof URL) {
		URL url = (URL) input;
		if ((url.getQuery() == null) && (url.getRef() == null)) {
		    desc = BpiCodec.readDescriptor(BpiCodec.getDescriptorURL(url));
		}
	    } else {
		throw new IllegalArgumentException("Only ImageInputStreams are supported.");
	    }
	    if (desc == null) {
		desc = new BpiStreamMetadata();
		if (!desc.hasController() || !desc.activateController()) {
		    desc = null;
		}
	    }
	    descriptor = desc;
	}
	return descriptor;
    }

    public int getNumImages (boolean allowSearch) throws IOException {
	if (input == null)
	    throw new IllegalStateException("Input not set.");
	if (seekForwardOnly && allowSearch)
	    throw new IllegalStateException("seekForwardOnly and allowSearch are both set to true.");

	if (numImages > 0)
	    return numImages;

	if (descriptor != null) {
	    numImages = descriptor.getNumImages();
	    return numImages;
	}

	if (!allowSearch) {
	    return -1;
	}

	numImages = locateImage(Integer.MAX_VALUE) + 1;
	return numImages;
    }

    private void readImageHeader (int imageIndex) throws IOException {
	if (imageIndex != locateImage(imageIndex)) {
	    throw new IndexOutOfBoundsException("imageIndex > number of images");
	}
	try {
	    currentIndex = -1;
	    if (descriptor == null) {
		imageMetadata = readImageMetadata();
	    }
	    imageSize = BpiCodec.readRasterSize(stream);
	    currentIndex = imageIndex;
	} catch (EOFException eofe) {
	    throw new IndexOutOfBoundsException("imageIndex > number of images");
	}
    }

    private Dimension getDimension (int imageIndex) throws IOException {
	checkIndex(imageIndex);
	int rasterIndex = imageIndex;
	if (descriptor != null) {
	    rasterIndex = descriptor.getImageRaster(imageIndex);
	}
	if (rasterIndex != currentIndex) {
	    readImageHeader(imageIndex);
	}
	return imageSize;
    }

    public IIOMetadata getImageMetadata (int imageIndex) throws IOException {
	checkIndex(imageIndex);

	if (descriptor != null) {
	    // no need to read until the image raster, just read until the
	    // first raster following the image color model.
	    int cmIndex = descriptor.getImageColorModel(imageIndex);
	    if (colorModels.size() <= cmIndex) {
		int index = descriptor.indexOfColorModel(cmIndex);
		do {
		    index++;
		} while (descriptor.isColorModel(index));
		int rasterIndex = descriptor.getRaster(index);
		imageIndex = descriptor.indexOfImage(rasterIndex);
		locateImage(imageIndex);
	    }
	    return (IIOMetadata) colorModels.get(cmIndex);
	} else {
	    if (imageIndex != currentIndex) {
		readImageHeader(imageIndex);
	    }
	    return imageMetadata;
	}
    }

    public int getWidth (int imageIndex) throws IOException {
	// index is checked in getDimension
	return getDimension(imageIndex).width;
    }

    public int getHeight (int imageIndex) throws IOException {
	// index is checked in getDimension
	return getDimension(imageIndex).height;
    }

    public ImageTypeSpecifier getRawImageType (int imageIndex) throws IOException {
	// index is checked in getImageMetadata and getDimension
	BpiImageMetadata imageMetadata = (BpiImageMetadata) getImageMetadata(imageIndex);
	Dimension d = getDimension(imageIndex);
	ColorModel cm = imageMetadata.createCompatibleColorModel();
	return new ImageTypeSpecifier(cm, cm.createCompatibleSampleModel(d.width, d.height));
    }

    public Iterator getImageTypes (int imageIndex) throws IOException {
	// index is checked in getRawImageType

	// TODO: return other compatible color models
	return Arrays.asList(new Object[] { getRawImageType(imageIndex) }).iterator();
    }

    public BufferedImage read (int imageIndex, ImageReadParam param) throws IOException {
//	checkInput();
	checkIndex(imageIndex);
	clearAbortRequest();

	// ensure the stream is at the correct position (just before image data)
	readImageHeader(imageIndex);

	BpiImageMetadata imageMetadata = (BpiImageMetadata) getImageMetadata(imageIndex);
	Dimension d = getDimension(imageIndex);

	BufferedImage image = getDestination(param, getImageTypes(imageIndex), d.width, d.height);
	Rectangle srcRegion = new Rectangle();
	Rectangle dstRegion = new Rectangle();
	computeRegions(param, d.width, d.height, image, srcRegion, dstRegion);

	int subsamplingX = (param != null) ? param.getSourceXSubsampling() : 1;
	int subsamplingY = (param != null) ? param.getSourceYSubsampling() : 1;
	ColorModel cm = imageMetadata.createCompatibleColorModel();

	processImageStarted(imageIndex);

	for (int srcY = srcRegion.y, dstY = dstRegion.y; srcY < srcRegion.y + srcRegion.height; srcY++) {
	    if (abortRequested()) {
		processReadAborted();
		return image;
	    }
	    if ((subsamplingY == 1) || ((srcY % subsamplingY) == 0)) {
		// read one row
		for (int srcX = srcRegion.x, dstX = dstRegion.x; srcX < srcRegion.x + srcRegion.width; srcX++) {
		    int pixel = (int) stream.readBits(imageMetadata.bitsPerPixel);
		    if ((subsamplingX == 1) || ((srcX % subsamplingX) == 0)) {
			// process pixel
			int rgb = cm.getRGB(pixel);
			image.setRGB(dstX++, dstY, rgb);
		    }
		}
		dstY++;
	    } else {
		// skip the row
		skipBits(d.width * imageMetadata.bitsPerPixel);
	    }
	    processImageProgress((float) (srcY - srcRegion.y + 1) / srcRegion.height);
	}

	processImageComplete();
	return image;
    }

    public boolean isRandomAccessEasy (int imageIndex) {
	// Once the color model and image size has been read, it's easy to
	// access any pixel in the image.
	return true;
    }

    public void reset() {
	super.reset();
	resetStreamSettings();
    }

    /**
     * Resets every variable linked to the input.
     * This method is called when changing the input or resetting the reader.
     */
    private void resetStreamSettings() {
	byteOffsets = new ArrayList();
	bitOffsets = new ArrayList();
	numImages = -1;

	currentIndex = -1;
	imageMetadata = null;
	imageSize = null;
    }

    /**
     * Checks an image index and throws the appropriate exception when needed.
     * Also sets <CODE>minIndex</CODE> if <CODE>seekForwardOnly</CODE> is <CODE>true</CODE>.
     * @param imageIndex The image index to check.
     */
    private void checkIndex (int imageIndex) {
        if (imageIndex < minIndex) {
            throw new IndexOutOfBoundsException("imageIndex < minIndex");
        }
        if (seekForwardOnly) {
	    if (descriptor != null) {
		int raster = descriptor.getImageRaster(imageIndex);
		minIndex = descriptor.indexOfImage(raster);
	    } else {
		minIndex = imageIndex;
	    }
        }
    }

    /**
     * Locates and seeks the specified image.
     * If there is a descriptor, the image is actually the image raster,
     * otherwise it is the image metadata.
     * @param imageIndex The index of the image to locate and seek.
     * @return The index of the last read raster.
     */
    private int locateImage (int imageIndex) throws IOException {
	int index = -1;
	int rasterIndex = imageIndex;
	// Find closest known index
	if (descriptor != null) {
	    rasterIndex = descriptor.getImageRaster(imageIndex);
	}
	if (!byteOffsets.isEmpty()) {
	    index = Math.min(rasterIndex, byteOffsets.size() - 1);
	    // Seek to that position
	    stream.seek(((Long) byteOffsets.get(index)).longValue());
	    stream.setBitOffset(((Integer) bitOffsets.get(index)).intValue());
	}

	while (index < rasterIndex) {
	    if (!skipImage(index))
		return --index;

	    byteOffsets.add(new Long(stream.getStreamPosition()));
	    bitOffsets.add(new Integer(stream.getBitOffset()));
	    index++;
	}

	return imageIndex;
    }

//    private int locateBlock (int blockIndex) throws IOException, IIOException {
//	int index = Math.min(blockIndex, byteOffsets.size() - 1);
//
//	stream.seek(((Long) byteOffsets.get(index)).longValue());
//	stream.setBitOffset(((Integer) bitOffsets.get(index)).intValue());
//
//	while (index < blockIndex) {
//	    if (!skipBlock(index))
//		return --index;
//
//	    byteOffsets.add(new Long(stream.getStreamPosition()));
//	    bitOffsets.add(new Integer(stream.getBitOffset()));
//	    index++;
//	}
//    }
//
//    private boolean skipBlock (int blockIndex) {
//	boolean isColorModel = (blockIndex % 2) == 0; // when there's no descriptor
//	if (descriptor != null) {
//	    isColorModel = descriptor.isColorModel(blockIndex);
//	}
//	if (isColorModel) {
//	    return skipColorModel();
//	} else {
//	    return skipRaster();
//	}
//    }
//
//    private boolean skipColorModel () {
//	// TODO
//	return false;
//    }
//
//    private boolean skipRaster (int bitsPerPixel) {
//	// TODO
//	return false;
//    }

    private BpiImageMetadata readImageMetadata() throws IOException {
	return BpiCodec.readColorModel(stream);
//	int depth = (int) stream.readBits(BpiImageFormat.DEPTH_SIZE);
//	BpiImageMetadata imageMetadata = new BpiImageMetadata(depth);
//
//	boolean hasPalette = (stream.readBit() == 1);
//	boolean hasTransparentColor = false;
//	if (!imageMetadata.hasAlpha) {
//	    hasTransparentColor = (stream.readBit() == 1);
//	}
//
//	ColorModel cm = null;
//	if (hasTransparentColor || hasPalette) {
//	    cm = imageMetadata.createCompatibleColorModel();
//	}
//	imageMetadata.hasTransparentColor = hasTransparentColor;
//
//	if (hasTransparentColor && !hasPalette) {
//	    int transparentColor = (int) stream.readBits(imageMetadata.bitsPerColor);
//	    imageMetadata.transparentColor = cm.getRGB(transparentColor);
//	}
//
//	if (hasPalette) {
//	    IntegerSet palette = new IntegerSet();
//	    palette.allowDuplicate = true; // allow duplicate to keep color indices
//	    int color = (int) stream.readBits(imageMetadata.bitsPerColor);
//	    color = cm.getRGB(color);
//	    if (palette.contains(color)) {
//		processWarningOccurred("Duplicate color in palette");
//	    }
//	    palette.add(color);
//	    color = (int) stream.readBits(imageMetadata.bitsPerColor);
//	    do {
//		color = cm.getRGB(color);
//		if (palette.contains(color)) {
//		    processWarningOccurred("Duplicate color in palette");
//		}
//		palette.add(color);
//		color = (int) stream.readBits(imageMetadata.bitsPerColor);
//	    } while (color != 0);
//	    imageMetadata.setPalette(palette.toArray());
//	}
//
//	return imageMetadata;
    }

    /**
     * Seeks the next image.
     * If there is a known descriptor, skips the current raster and reads the
     * following color models, leaving the stream at the beginning of the next
     * raster.
     * Otherwise, skips the current color model and raster, leaving the stream
     * at the beginning of the next color model.
     *
     * @param index The index of the raster (if there is a known descriptor) or
     * image that is being skipped.
     * @return <CODE>false</CODE> if an error occured.
     */
    private boolean skipImage (int index) throws IIOException {
	try {
	    ColorModel cm = null;
	    if (descriptor != null) {
		if (index < 0) {
		    // We have to skip the leading color models
		    for (int i = 0; descriptor.isColorModel(i); i++) {
			colorModels.add(readImageMetadata());
		    }
		} else {
		    // skip raster
		    BpiImageMetadata imageMetadata =
			(BpiImageMetadata) colorModels.get(descriptor.getColorModel(index, 0));
		    skipRaster(imageMetadata.bitsPerPixel);

		    // skip following color models
		    for (int i = descriptor.indexOfRaster(index);
			 descriptor.isColorModel(i); i++) {
			colorModels.add(readImageMetadata());
		    }
		}
	    } else {
		if (index >= 0) {
		    BpiImageMetadata imageMetadata = readImageMetadata();
		    skipRaster(imageMetadata.bitsPerPixel);
		}
	    }
	    return true;
	} catch (EOFException eofe) {
	    return false;
	} catch (IOException ioe) {
	    throw new IIOException("I/O error while skipping image/raster " + index + ".", ioe);
	}
    }

    private void skipRaster (int bitsPerPixel) throws IOException {
	Dimension rasterSize = BpiCodec.readRasterSize(stream);
	skipBits(rasterSize.width * rasterSize.height * bitsPerPixel);
    }

    private void skipBits (int length) throws IOException {
	length += stream.getBitOffset();
	stream.setBitOffset(0);
	stream.skipBytes(length / 8);
	stream.setBitOffset(length % 8);
    }

//    static private BpiStreamMetadata readStreamMetadata (File imageFile) {
//	try {
//	    return BpiCodec.readDescriptor(imageFile);
//	} catch (Exception e) {
//	    return null;
//	}
//    }

    static private class BpiImageReadParam extends ImageReadParam {
	BpiImageReadParam() {
	    super();
	    canSetSourceRenderSize = false;
	}
    }
}
