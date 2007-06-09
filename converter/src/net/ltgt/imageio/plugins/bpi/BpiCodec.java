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

import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Thomas Broyer
 */
class BpiCodec {
    static final String AUTHOR = "Thomas Broyer";
    static final String VERSION = "0.1";
    static final String[] FORMAT_NAMES =  { "bpi", "BPI" };
    static final String[] SUFFIXES =  { "bpi", "BPI" };
    static final String[] MIME_TYPES = { "image/x-bitpacked" };

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Descriptor related                                                *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static public File getDescriptorFile (File imageFile) {
	return new File (imageFile.getAbsolutePath() + ".definition");
    }

    static public URL getDescriptorURL (URL imageURL) throws MalformedURLException {
	return new URL(imageURL, imageURL.getPath() + ".definition");
    }

    static public BpiStreamMetadata readDescriptor (File file) throws IOException {
	return readDescriptor(new FileInputStream(file));
    }

    static public BpiStreamMetadata readDescriptor (URL url) throws IOException {
	return readDescriptor(url.openStream());
    }

    static public BpiStreamMetadata readDescriptor (InputStream in) throws IOException {
	try {
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    Document doc = dbFactory.newDocumentBuilder().parse(in);
	    BpiStreamMetadata streamMetadata = new BpiStreamMetadata();
	    streamMetadata.setFromTree("net_ltgt_bpi_stream_1.0", doc.getDocumentElement());
	    return streamMetadata;
	} catch (ParserConfigurationException pce) {
	    return null;
	} catch (SAXException saxe) {
	    return null;
	} catch (IIOInvalidTreeException iioe) {
	    return null;
	}
    }

    static public void writeDescriptor (File file, BpiStreamMetadata descriptor) throws IOException {
	OutputStream stream = new FileOutputStream(file);
	writeDescriptor(stream, descriptor);
	stream.close();
    }

    static public void writeDescriptor (OutputStream out, BpiStreamMetadata descriptor) throws IOException {
	try {
	    TransformerFactory tFactory = TransformerFactory.newInstance();
	    Transformer transformer = tFactory.newTransformer();
	    DOMSource source = new DOMSource(descriptor.getAsTree("net_ltgt_bpi_stream_1.0"));
	    StreamResult result = new StreamResult(out);
	    transformer.transform(source, result);
	} catch (TransformerConfigurationException tce) {
	    throw new RuntimeException("Java XML API configuration error", tce);
	} catch (TransformerException te) {
	    Throwable ex = te.getException();
	    if (ex instanceof IOException) {
		throw (IOException) ex;
	    }
	    throw new RuntimeException("Error while writing the descriptor.", te);
	}
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Compatibility checks                                              *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static public boolean isCompatibleImageType (ImageTypeSpecifier type) {
	return isCompatibleColorModel(type.getColorModel())
	    && isCompatibleSampleModel(type.getSampleModel());
    }

    static public boolean isBaseColorModel (ColorModel cm) {
	if (cm == null)
	    return false;
	for (Iterator iter = BpiImageMetadata.getBaseColorModels(); iter.hasNext(); ) {
	    ColorModel baseCM = (ColorModel) iter.next();
	    if (baseCM.equals(cm)) {
		return true;
	    }
	}
	return false;
    }

    static public boolean isCompatibleColorModel (ColorModel cm) {
	if (isBaseColorModel(cm))
	    return true;
	// TODO: support IndexColorModels
//	if (cm instanceof IndexColorModel) {
//	    IndexColorModel icm = (IndexColorModel) cm;
//	    icm.
//	}
	return false;
    }

    static public boolean isCompatibleSampleModel (SampleModel sm) {
	if (!isCompatibleRasterSize(sm.getWidth(), sm.getHeight()))
	    return false;
	if (sm.getNumDataElements() != 1)
	    return false;
	int numBands = sm.getNumBands();
	if ((numBands < 1) || (numBands > 4))
	    return false;

	if (numBands == 1) {
	    // opaque gray or indexed
	    int sampleSize = sm.getSampleSize(0);
	    int maxSize = BpiImageFormat.MAX_BITS_PER_COLOR;
	    return (sampleSize <= maxSize);
	}

	// numBands = 2, 3 or 4
	int[] smSizes = sm.getSampleSize();
	int[] bpiSizes = BpiImageFormat.MAX_BITS_PER_SAMPLE[numBands - 1];
	while (numBands-- > 0) {
	    if (smSizes[numBands] > bpiSizes[numBands])
		return false;
	}

	return true;
    }

//    static public boolean isCompatibleColorModel (ColorModel cm) {
//	int colorComponents = cm.getNumColorComponents();
//	if ((colorComponents < 1) || (colorComponents > 3))
//	    return false;
//	int numComponents = cm.getNumComponents();
//	if (numComponents - colorComponents > 1)
//	    return false;
//	if (cm instanceof IndexColorModel) {
//	    IndexColorModel icm = (IndexColorModel) cm;
//	    int paletteSize = icm.getMapSize();
//	    if ((paletteSize < BpiImageFormat.MIN_PALETTE_LENGTH) ||
//		(paletteSize > BpiImageFormat.MAX_PALETTE_LENGTH))
//		return false;
//	}
//	if (!isCompatibleSampleModel(cm.createCompatibleSampleModel(BpiImageFormat.MIN_WIDTH, BpiImageFormat.MIN_HEIGHT)))
//	    return false;
//	int csType = cm.getColorSpace().getType();
//	return (csType == ColorSpace.TYPE_GRAY) || (csType == ColorSpace.TYPE_RGB);
//    }

    static public boolean isCompatibleRasterSize (Dimension rasterSize) {
	return isCompatibleRasterSize(rasterSize.width, rasterSize.height);
    }

    static public boolean isCompatibleRasterSize (int width, int height) {
	return (width >= BpiImageFormat.MIN_WIDTH)
	    && (height >= BpiImageFormat.MIN_HEIGHT)
	    && (width - BpiImageFormat.MIN_WIDTH < (1 << BpiImageFormat.WIDTH_SIZE))
	    && (height - BpiImageFormat.MIN_HEIGHT < (1 << BpiImageFormat.HEIGHT_SIZE));
    }

    static public boolean isCompatibleRaster (Raster raster) {
	return isCompatibleSampleModel(raster.getSampleModel());
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Sample scaling                                                    *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static public int scaleRGB (int rgb, int[] sampleSize) {
	return scaleRGB(rgb, sampleSize[0], sampleSize[1], sampleSize[2],
		(sampleSize.length == 3) ? 0 : sampleSize[3]);
    }

    static public int scaleRGB (int rgb, int sampleSize) {
	return scaleRGB(rgb, sampleSize, true);
    }

    static public int scaleRGB (int rgb, int sampleSize, boolean hasAlpha) {
	return scaleRGB(rgb, sampleSize, sampleSize, sampleSize, hasAlpha ? sampleSize : 0);
    }

    static public int scaleRGB (int rgb, int redSize, int greenSize, int blueSize) {
	return scaleRGB(rgb, redSize, greenSize, blueSize, 0);
    }

    static public int scaleRGB (int rgb, int redSize, int greenSize, int blueSize, int alphaSize) {
	if ((redSize > 8) || (greenSize > 8) || (blueSize > 8) || (alphaSize > 8)) {
	    throw new IllegalArgumentException("sample size must not be greater than 8");
	}
	int red = (rgb >> 16) & 0xFF;
	int green = (rgb >> 8) & 0xFF;
	int blue = rgb & 0xFF;
	int alpha = (rgb >>> 24) & 0xFF;

	return (scaleSample(red, 8, redSize) << 16)
	     | (scaleSample(green, 8, greenSize) << 8)
	     | scaleSample(blue, 8, blueSize)
	     | (scaleSample(alpha, 8, alphaSize) << 24);
    }

    static public int scaleSample (int sample, int from, int to) {
	if ((from < 0) || (to <= 0)) {
	    throw new IllegalArgumentException("sample size must not be negative.");
	}
	if ((to == 0) || (from == 0))
	    return 0;
	if (from == to)
	    return sample;
	if (from < to)
	    return scaleSampleUp(sample, from, to - from);
	return scaleSampleDown(sample, from, from - to);
    }

    static private int scaleSampleUp (int sample, int from, int by) {
	int to = from + by;
	int ret = sample << by;
	ret |= scaleSample(sample, from, by);
	return ret & ((1 << to) - 1);
    }

    static private int scaleSampleDown (int sample, int from, int by) {
	int to = from - by;
	return (sample >>> by) & ((1 << to) - 1);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * ColorModel related                                                *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static public BpiImageMetadata readColorModel (ImageInputStream stream) throws IOException {
	int depth = (int) stream.readBits(BpiImageFormat.DEPTH_SIZE);
	BpiImageMetadata imageMetadata = new BpiImageMetadata(depth);

	boolean hasPalette = (depth < BpiImageFormat.MIN_DEPTH_TO_USE_PALETTE)
	    ? false : (stream.readBit() == 1);
	boolean hasTransparentColor = false;
	if (!imageMetadata.hasAlpha) {
	    hasTransparentColor = (stream.readBit() == 1);
	}

	ColorModel cm = null;
	if (hasTransparentColor || hasPalette) {
	    cm = imageMetadata.createCompatibleColorModel();
	}
	imageMetadata.hasTransparentColor = hasTransparentColor;

	if (hasTransparentColor && !hasPalette) {
	    int transparentColor = (int) stream.readBits(imageMetadata.bitsPerColor);
	    imageMetadata.transparentColor = cm.getRGB(transparentColor);
	}

	if (hasPalette) {
	    // this code does NOT check palette entry uniqueness
	    int paletteSize = (int) stream.readBits(imageMetadata.bitsPerColor - 1) + BpiImageFormat.MIN_PALETTE_LENGTH;
	    if (hasTransparentColor)
		paletteSize--;
	    int[] palette = new int[paletteSize];
	    for (int i = 0; i < paletteSize; i++) {
		int color = (int) stream.readBits(imageMetadata.bitsPerColor);
		color = cm.getRGB(color);
		palette[i] = color;
	    }
	    imageMetadata.setPalette(palette);
	}

	return imageMetadata;
    }

    static private Object dataElements = null;
    static private int transferType = -1;
    static int getDataElement (ColorModel cm, int rgb) {
	int newTransferType = cm.getTransferType();
	if (transferType != newTransferType) {
	    transferType = newTransferType;
	    dataElements = null;
	}
	dataElements = cm.getDataElements(rgb, dataElements);
	switch (transferType) {
	    case DataBuffer.TYPE_BYTE:
		byte[] bData = (byte[]) dataElements;
		return bData[0] & 0xFF;
	    case DataBuffer.TYPE_USHORT:
		short[] sData = (short[]) dataElements;
		return sData[0] & 0xFFFF;
	    case DataBuffer.TYPE_INT:
		int[] iData = (int[]) dataElements;
		return iData[0];
	    default:
		throw new UnsupportedOperationException("Unsupported transfer type " + transferType);
	}
    }

    static public void writeColorModel (ImageOutputStream stream, BpiImageMetadata imageMetadata) throws IOException {
	stream.writeBits(imageMetadata.depth, BpiImageFormat.DEPTH_SIZE);
	stream.writeBit(imageMetadata.palette == null ? 0 : 1);
	ColorModel cm = null;
	if (imageMetadata.hasTransparentColor || (imageMetadata.palette != null)) {
	    cm = new BpiImageMetadata(imageMetadata.depth).createCompatibleColorModel();
	}
	if (!imageMetadata.hasAlpha) {
	    stream.writeBit(imageMetadata.hasTransparentColor ? 1 : 0);
	    if (imageMetadata.hasTransparentColor && (imageMetadata.palette == null)) {
		int color = getDataElement(cm, imageMetadata.transparentColor);
		stream.writeBits(color, imageMetadata.bitsPerColor);
	    }
	}
	if (imageMetadata.palette != null) {
	    int[] palette = imageMetadata.palette;
	    int paletteSize = palette.length;
	    if (imageMetadata.hasTransparentColor)
		paletteSize--;
	    stream.writeBits(paletteSize, imageMetadata.bitsPerColor - 1);
	    for (int i = 0; i < paletteSize; i++) {
		int color = getDataElement(cm, palette[i]);
		stream.writeBits(color, imageMetadata.bitsPerColor);
	    }
	}
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Raster related                                                    *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static public Dimension readRasterSize (ImageInputStream stream) throws IOException {
	int width = (int) stream.readBits(BpiImageFormat.WIDTH_SIZE) + BpiImageFormat.MIN_WIDTH;
	int height = (int) stream.readBits(BpiImageFormat.HEIGHT_SIZE) + BpiImageFormat.MIN_HEIGHT;
	return new Dimension(width, height);
    }

    static public void writeRasterSize (ImageOutputStream stream, Dimension rasterSize) throws IOException {
	writeRasterSize(stream, rasterSize.width, rasterSize.height);
    }

    static public void writeRasterSize (ImageOutputStream stream, int width, int height) throws IOException {
	if (!isCompatibleRasterSize(width, height)) {
	    throw new IllegalArgumentException("width or height is too small or too large.");
	}
	stream.writeBits(width - BpiImageFormat.MIN_WIDTH, BpiImageFormat.WIDTH_SIZE);
	stream.writeBits(height - BpiImageFormat.MIN_HEIGHT, BpiImageFormat.HEIGHT_SIZE);
    }

    static public DataBuffer readRaster (ImageInputStream stream, int pixel_bits) throws IOException {
	Dimension size = readRasterSize(stream);
	return readRaster(stream, size, pixel_bits);
    }

    static public DataBuffer readRaster (ImageInputStream stream, Dimension rasterSize, int pixel_bits) throws IOException {
	DataBuffer db;
	int numberOfElements = rasterSize.width * rasterSize.height;
	if (pixel_bits > 16) {
	    db = new DataBufferInt(numberOfElements);
	} else if (pixel_bits > 8) {
	    db = new DataBufferUShort(numberOfElements);
	} else {
	    db = new DataBufferByte(numberOfElements);
	}
	for (int i = 0; i < numberOfElements; i++) {
	    int pixel = (int) stream.readBits(pixel_bits);
	    db.setElem(i, pixel);
	}
	return db;
    }

    static public void writeRaster (ImageOutputStream stream, int pixel_bits, DataBuffer raster) throws IOException {
	for (int i = 0; i < raster.getSize(); i++) {
	    int pixel = raster.getElem(i);
	    stream.writeBits(pixel, pixel_bits);
	}
    }

    static public void writeRaster (ImageOutputStream stream, Dimension rasterSize, int pixel_bits, DataBuffer raster) throws IOException {
	writeRasterSize(stream, rasterSize);
	writeRaster(stream, pixel_bits, raster);
    }
}
