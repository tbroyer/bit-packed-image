/*
 * BpiDecoder - a Bit-Packed Image decoder for J2ME
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

package net.ltgt.mid.bpi;

/*#NokiaUI#*///<editor-fold>
//--import com.nokia.mid.ui.DirectGraphics;
//--import com.nokia.mid.ui.DirectUtils;
/*$NokiaUI$*///</editor-fold>
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;


/**
 * Decodes images stored in a DataInputStream in BPI format.
 * 
 * Images can be returned as {@link javax.microedition.lcdui.Image}
 * or directly rendered on a {@link javax.microedition.lcdui.Graphics} object.
 *
 * @author Thomas Broyer
 */
public class BpiDecoder {
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * BPI image format constants                                          *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static private final int[] DEPTHS = {
	           /*- opaque -*/  /*-- alpha --*/
	/* Gray */ 1,  2,  4,  8,   4,  8, 12, 16,
	/* ARGB */ 8, 12, 15, 16,  12, 16, 20, 24
    };
    static private final int[][] BITS_PER_SAMPLE = {
	// Grayscale, opaque
	{ 1 }, { 2 }, { 4 }, { 8 },
	// Grayscale with alpha
	{ 2, 2 }, { 4, 4 }, { 6, 6 }, { 8, 8 },
	// RGB opaque
	{ 3, 3, 2 }, { 4, 4, 4 }, { 5, 5, 5 }, { 5, 6, 5 },
	// RGB with alpha
	{ 3, 3, 3, 3 }, { 4, 4, 4, 4 }, { 5, 5, 5, 5 }, { 6, 6, 6, 6}
    };
    static private final int DEPTH_SIZE = 4;
    static private final int ALPHA_MASK = 4;
    static private final int MIN_PALETTE_SIZE = 2;
    static private final int MIN_DEPTH_TO_USE_PALETTE = 1;
    static private final int DIMENSION_SIZE = 9;
    static private final int MIN_DIMENSION = 1;

    static private final int TRANSPARENT_COLOR = 0;

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Image decoding                                                      *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Creates a new instance of BpiDecoder
     *
     * @param dis The <CODE>DataInputStream</CODE> to read images from.
     */
    public BpiDecoder (DataInputStream dis) {
	stream = dis;
    }

    private int depth;
    private boolean hasTransparentColor;
    private int transparentColor;
    private int[] palette;
    private int bitsPerPixel;

    /**
     * Reads a BPI color model from the underlying stream.
     * 
     * The BPI color model is set as the current color model for the following
     * calls to <CODE>readRaster</CODE> or <CODE>createImage</CODE>.
     *
     * @throws java.io.IOException if an error occured when reading from the underlying stream.
     */
    public void readColorModel() throws IOException {
	depth = readBits(DEPTH_SIZE);
	bitsPerPixel = DEPTHS[depth];
	boolean hasAlpha = (depth & ALPHA_MASK) == ALPHA_MASK;
	boolean hasPalette = (depth >= MIN_DEPTH_TO_USE_PALETTE) && readBit();
	hasTransparentColor = !hasAlpha && readBit();
/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder: read color model:");
//--	System.out.println("BpiDecoder:     depth = " + depth);
//--	System.out.println("BpiDecoder:     hasPalette = " + (hasPalette ? "yes" : "no"));
//--	System.out.println("BpiDecoder:     hasTransparentColor = " + (hasTransparentColor ? "yes" : "no"));
/*$MIDP2Debug$*///</editor-fold>
	if (hasTransparentColor && !hasPalette) {
	    transparentColor = readColor();
/*#MIDP2Debug#*///<editor-fold>
//--	    System.out.println("BpiDecoder:     transparent color = " + Integer.toHexString(transparentColor));
/*$MIDP2Debug$*///</editor-fold>
	}
	if (hasPalette) {
	    readPalette();
	} else {
	    palette = null;
	}
/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder:     bits per pixel = " + bitsPerPixel);
/*$MIDP2Debug$*///</editor-fold>
    }

    private void readPalette() throws IOException {
	// read palette size
	int paletteSize = readBits(bitsPerPixel - 1) + MIN_PALETTE_SIZE;
/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder:     palette: size = " + paletteSize);
/*$MIDP2Debug$*///</editor-fold>
	int[] palette = new int[paletteSize];
	if (hasTransparentColor) {
	    palette[--paletteSize] = 0;
	}

	// read palette from stream
	for (int i = 0; i < paletteSize; i++) {
	    palette[i] = readColor();
/*#MIDP2Debug#*///<editor-fold>
//--	    System.out.println("BpiDecoder:       " + i + ". " + Integer.toHexString(palette[i]));
/*$MIDP2Debug$*///</editor-fold>
	}
	this.palette = palette;

	// compute new bitsPerPixel
	if (!hasTransparentColor)
	    paletteSize--; // reuse the variable to store the max index in the palette
	bitsPerPixel = 1;
	while ((paletteSize >>> bitsPerPixel) != 0) {
	    bitsPerPixel++;
	}
    }

    /**
     * Reads a image from the underlying stream using the current color model
     * and returns it as an <CODE>Image</CODE>.
     *
     * @see #readColorModel()
     * @throws java.io.IOException an error occured when reading from the underlying stream.
     * @return The decoded image.
     */
    public Image readImage() throws IOException {
	int width = readDimension();
	int height = readDimension();

/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder: read image: width = " + width + ", height = " + height);
/*$MIDP2Debug$*///</editor-fold>
/*#DefaultConfiguration,MIDP2,MIDP2Debug#*///<editor-fold>
	return Image.createRGBImage(readPixels(width, height), width, height, true);
/*$DefaultConfiguration,MIDP2,MIDP2Debug$*///</editor-fold>
/*#NokiaUI#*///<editor-fold>
//--	Image img = DirectUtils.createImage(width, height, TRANSPARENT_COLOR);
//--	Graphics g = img.getGraphics();
//--	DirectGraphics dg = DirectUtils.getDirectGraphics(g);
//--	dg.drawPixels(readPixels(width, height), true, 0, width, 0, 0,
//--		width, height, 0, DirectGraphics.TYPE_INT_8888_ARGB);
//--	return img;
/*$NokiaUI$*///</editor-fold>
/*#MIDP1#*///<editor-fold>
//--	Image img = Image.createImage(width, height);
//--	Graphics g = img.getGraphics();
//--	drawPixels(g, width, height);
//--	return img;
/*$MIDP1$*///</editor-fold>
    }

    /**
     * Reads an image from the underlying stream using the current color model
     * and renderes it directly to the provided <CODE>Graphics</CODE>.
     *
     * @param g      the <CODE>Graphics</CODE> object to render to.
     * @param x      the x coordinate of the anchor point.
     * @param y      the y coordinate of the anchor point.
     * @param anchor the anchor point for positioning the image.
     * @throws java.io.IOException an error occured when reading from the underlying stream.
     */
    public void readImage (Graphics g, int x, int y, int anchor) throws IOException {
	int width = readDimension();
	int height = readDimension();

/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder: read image: width = " + width + ", height = " + height);
/*$MIDP2Debug$*///</editor-fold>

	if ((anchor & Graphics.VCENTER) == Graphics.VCENTER) {
	    y -= height/2;
	} else if ((anchor & Graphics.BOTTOM) == Graphics.BOTTOM) {
	    y -= height;
	}
	if ((anchor & Graphics.HCENTER) == Graphics.HCENTER) {
	    x -= width/2;
	} else if ((anchor & Graphics.RIGHT) == Graphics.RIGHT) {
	    x -= width;
	}

/*#DefaultConfiguration,MIDP2,MIDP2Debug#*///<editor-fold>
	g.drawRGB(readPixels(width, height), 0, width, x, y, width, height, true);
/*$DefaultConfiguration,MIDP2,MIDP2Debug$*///</editor-fold>
/*#NokiaUI#*///<editor-fold>
//--	DirectGraphics dg = DirectUtils.getDirectGraphics(g);
//--	dg.drawPixels(readPixels(width, height), true, 0, width, x, y,
//--	    width, height, 0, DirectGraphics.TYPE_INT_8888_ARGB);
/*$NokiaUI$*///</editor-fold>
/*#MIDP1#*///<editor-fold>
//--	g.translate(x, y);
//--	drawPixels(g, width, height);
//--	g.translate(-x, -y);
/*$MIDP1$*///</editor-fold>
    }

    private int[] readPixels (int width, int height) throws IOException {
	int length = width * height;
	int[] pixels = new int[length];

	if (palette != null) {
	    for (int i = 0; i < length; i++) {
		pixels[i] = readIndexedColor();
	    }
	} else {
	    for (int i = 0; i < length; i++) {
		pixels[i] = readColor();
	    }
	}

	return pixels;
    }

    private void drawPixels (Graphics g, int width, int height) throws IOException {
	if (palette != null) {
	    for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
		    g.setColor(readIndexedColor());
		    g.drawLine(x, y, x, y);
		}
	    }
	} else {
	    for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
		    g.setColor(readColor());
		    g.drawLine(x, y, x, y);
		}
	    }
	}
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Extended API                                                        *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Returns the current color model.
     *
     * @return the current color model.
     * @see #setColorModel(BpiColorModel)
     */
    public BpiColorModel getColorModel() {
	return new BpiColorModel(depth, hasTransparentColor, transparentColor,
		palette, bitsPerPixel);
    }

    /**
     * Sets the current color model.
     *
     * @param cm the color model to be set as current
     * @see #getColorModel()
     */
    public void setColorModel (BpiColorModel cm) {
	depth = cm.depth;
	hasTransparentColor = cm.hasTransparentColor;
	transparentColor = cm.transparentColor;
	palette = cm.palette;
	bitsPerPixel = cm.bitsPerPixel;
    }

    /**
     * Reads a raster (image size and pixel data) from the underlying stream
     * and returns it as a <CODE>BpiRaster</CODE>.
     *
     * @throws java.io.IOException an error occured when reading reading from the underlying stream.
     * @return the read raster.
     */
    public BpiRaster readRaster() throws IOException {
	int width = readDimension();
	int height = readDimension();

	int length = width * height;
	int[] pixels = new int[length];
	for (int i = 0; i < length; i++) {
	    pixels[i] = readBits(bitsPerPixel);
	}

	return new BpiRaster(width, height, bitsPerPixel, pixels);
    }

    /**
     * Creates an <CODE>Image</CODE> from the current color model and the
     * provided raster.
     *
     * @param raster the raster to decode the image from.
     * @return the decoded image.
     * @see #setColorModel(BpiColorModel)
     * @throws IllegalArgumentException the raster pixel size is not compatible with the current color model.
     */
    public Image createImage (BpiRaster raster) {
	if (raster.bitsPerPixel != bitsPerPixel) {
/*#DefaultConfiguration#*///<editor-fold>
	    throw new IllegalArgumentException("Incompatible raster pixel size.");
/*$DefaultConfiguration$*///</editor-fold>
/*#!DefaultConfiguration#*///<editor-fold>
//--	    throw new IllegalArgumentException();
/*$!DefaultConfiguration$*///</editor-fold>
	}
/*#DefaultConfiguration,MIDP2,MIDP2Debug#*///<editor-fold>
	return Image.createRGBImage(readPixels(raster.pixels), raster.width, raster.height, true);
/*$DefaultConfiguration,MIDP2,MIDP2Debug$*///</editor-fold>
/*#NokiaUI#*///<editor-fold>
//--	int width = raster.width;
//--	int height = raster.height;
//--	Image img = DirectUtils.createImage(width, height, TRANSPARENT_COLOR);
//--	Graphics g = img.getGraphics();
//--	DirectGraphics dg = DirectUtils.getDirectGraphics(g);
//--	dg.drawPixels(readPixels(raster.pixels), true, 0, width, 0, 0,
//--		width, height, 0, DirectGraphics.TYPE_INT_8888_ARGB);
//--	return img;
/*$NokiaUI$*///</editor-fold>
/*#MIDP1#*///<editor-fold>
//--	int width = raster.width;
//--	int height = raster.height;
//--	Image img = Image.createImage(width, height);
//--	Graphics g = img.getGraphics();
//--	drawPixels(raster.pixels, g, width, height);
//--	return img;
/*$MIDP1$*///</editor-fold>
    }

    /**
     * Decodes an image from the current color model and the provided raster
     * and renders it directly to the provided <CODE>Graphics</CODE>.
     *
     * @param raster the raster to decode the image from.
     * @param g      the <CODE>Graphics</CODE> object to render to.
     * @param x      the x coordinate of the anchor point.
     * @param y      the y coordinate of the anchor point.
     * @param anchor the anchor point for positioning the image.
     * @see #setColorModel(BpiColorModel)
     * @throws IllegalArgumentException the raster pixel size is not compatible with the current color model.
     */
    public void createImage (BpiRaster raster, Graphics g, int x, int y, int anchor) {
	int width = raster.width;
	int height = raster.height;

	if ((anchor & Graphics.VCENTER) == Graphics.VCENTER) {
	    y -= height/2;
	} else if ((anchor & Graphics.BOTTOM) == Graphics.BOTTOM) {
	    y -= height;
	}
	if ((anchor & Graphics.HCENTER) == Graphics.HCENTER) {
	    x -= width/2;
	} else if ((anchor & Graphics.RIGHT) == Graphics.RIGHT) {
	    x -= width;
	}

/*#DefaultConfiguration,MIDP2,MIDP2Debug#*///<editor-fold>
	g.drawRGB(readPixels(raster.pixels), 0, width, x, y, width, height, true);
/*$DefaultConfiguration,MIDP2,MIDP2Debug$*///</editor-fold>
/*#NokiaUI#*///<editor-fold>
//--	DirectGraphics dg = DirectUtils.getDirectGraphics(g);
//--	dg.drawPixels(readPixels(raster.pixels), true, 0, width, x, y,
//--	    width, height, 0, DirectGraphics.TYPE_INT_8888_ARGB);
/*$NokiaUI$*///</editor-fold>
/*#MIDP1#*///<editor-fold>
//--	g.translate(x, y);
//--	drawPixels(raster.pixels, g, width, height);
//--	g.translate(-x, -y);
/*$MIDP1$*///</editor-fold>
    }

    private int[] readPixels (int[] rawPixels) {
	int length = rawPixels.length;
	int[] pixels = new int[length];
	if (palette != null) {
	    for (int i = length - 1; i >= 0; i--) {
		pixels[i] = readIndexedColor(rawPixels[i]);
	    }
	} else {
	    int depth = this.depth;
	    for (int i = length - 1; i >= 0; i--) {
		pixels[i] = toARGB(rawPixels[i], depth);
	    }
	}
	return pixels;
    }

    private void drawPixels (int[] rawPixels, Graphics g, int width, int height) {
	if (palette != null) {
	    for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
		    g.setColor(readIndexedColor(rawPixels[y*width + x]));
		    g.drawLine(x, y, x, y);
		}
	    }
	} else {
	    int depth = this.depth;
	    for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
		    g.setColor(toARGB(rawPixels[y*width + x], depth));
		    g.drawLine(x, y, x, y);
		}
	    }
	}
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Utility functions                                                   *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private int readColor() throws IOException {
	return toARGB(readBits(bitsPerPixel), depth);
    }

    private int readIndexedColor() throws IOException {
	try {
	    return palette[readBits(bitsPerPixel)];
	} catch (ArrayIndexOutOfBoundsException aiobe) {
	    return palette[palette.length - 1];
	}
    }

    private int readIndexedColor (int index) {
	try {
	    return palette[index];
	} catch (ArrayIndexOutOfBoundsException aiobe) {
	    return palette[palette.length - 1];
	}
    }

    private int readDimension() throws IOException {
	return readBits(DIMENSION_SIZE) + MIN_DIMENSION;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Sample scaling                                                    *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static private int[] samples = new int[4];

    static private int toARGB (int color, int depth) {
	int[] sampleSize = BITS_PER_SAMPLE[depth];
	int numBands = sampleSize.length;
//	int red, green, blue, alpha, size;

/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder: toARGB: " + Integer.toHexString(color));
/*$MIDP2Debug$*///</editor-fold>

	// extract samples from color
//	int i = numBands - 1;
//	size = sampleSize[i--];
//	blue = scaleSample(color & ((1 << size) - 1), size);
//	color >>>= size;
//	if (numBands > 2) {
//	    size = sampleSize[i--];
//	    green = scaleSample(color & ((1 << size) - 1), size);
//	    color >>>= size;
//	    size = sampleSize[i--];
//	    red = scaleSample(color & ((1 << size) - 1), size);
//	    color >>>= size;
//	} else {
//	    red = green = blue;
//	}
//	if (numBands % 2 == 0) {
//	    size = sampleSize[i--];
//	    alpha = scaleSample(color & ((1 << size) - 1), size);
//	} else {
//	    alpha = 0xFF;
//	}
	for (int i = numBands-1; i >= 0; i--) {
	    int size = sampleSize[i];
	    samples[i] = scaleSample(color & ((1 << size) - 1), size);
	    color >>>= size;
	}

	int alpha = (numBands % 2 == 0) ? samples[numBands-1] : 0xFF;

	if (numBands <= 2) {
	    int gray = samples[0];
	    return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
	}
	// else, numBands >= 3	
	return (alpha << 24) | (samples[0] << 16) | (samples[1] << 8) | samples[2];
//	return (alpha << 24) | (red << 16) | (green << 8) & blue;
    }

    static private int scaleSample (int sample, int from) {
/*#DefaultConfiguration#*///<editor-fold>
	if (from < 0) {
	    throw new IllegalArgumentException("sample size must not be negative.");
	}
	if (from == 0)
	    return 0;
/*$DefaultConfiguration$*///</editor-fold>
/*#MIDP2Debug#*///<editor-fold>
//--	System.out.println("BpiDecoder: scaleSample: sample = x" + Integer.toHexString(sample) + ", from = " + from);
/*$MIDP2Debug$*///</editor-fold>

	if (from == 8)
	    return sample;

	int by = 8 - from;
//	int ret = sample << by;
//	do {
//	    by -= from;
//	    ret |= sample << by;
//	} while (by >= 0);
	int ret = 0;
	while (by >= 0) {
	    ret |= sample << by;
	    by -= from;
	}
	if (by < 0) {
	    ret |= sample >>> (-by);
	}
	return ret & 0xFF;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Stream related                                                      *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private DataInputStream stream;
    private short lastByte = 0;
    private byte bitsLeft = 0;

    private boolean readBit() throws IOException {
	if (bitsLeft == 0) {
	    lastByte = (short) (stream.readByte() & 0xFF);
	    bitsLeft = 8;
	}
	bitsLeft--;
	boolean bit = (((lastByte >> bitsLeft) & 1) == 1) ? true : false;
	lastByte &= (1 << bitsLeft) - 1;
	return bit;
    }

    private int readBits (int length) throws IOException {
	int bits = 0;

	if (bitsLeft == 0) {
	    lastByte = (short) (stream.readByte() & 0xFF);
	    bitsLeft = 8;
	}

	if (length == bitsLeft) {
	    bits = lastByte;
	    lastByte = 0;
	    bitsLeft = 0;
	    return bits;
	}

	if (length > bitsLeft) {
	    length -= bitsLeft;
	    bits = lastByte & ((1 << bitsLeft) - 1);

	    lastByte = 0;
	    bitsLeft = 0;
	}

	while (length >= 8) {
	    bits <<= 8;
	    bits |= stream.readByte() & 0xFF;
	    length -= 8;
	}

	// length here is between 0 and 7

	if (length > 0) {
	    if (bitsLeft == 0) {
		lastByte = (short) (stream.readByte() & 0xFF);
		bitsLeft = 8;
	    }

	    bits <<= length;
	    bitsLeft -= length;
	    bits |= (lastByte >> bitsLeft) & ((1 << length) - 1);
	    lastByte &= (1 << bitsLeft) - 1;
	}

	return bits;
    }
}
