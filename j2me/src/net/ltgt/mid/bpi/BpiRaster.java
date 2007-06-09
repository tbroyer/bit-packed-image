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

/**
 * Represents a BPI raster consisting of the image size and raw pixel data.
 *
 * @author Thomas Broyer
 * @see BpiDecoder#readRaster()
 */
public final class BpiRaster {
    // Package-scope fields
    int width;
    int height;
    int bitsPerPixel;
    int[] pixels;

    /**
     * Creates a new instance of BpiRaster
     *
     * @param width        the raster width
     * @param height       the raster height
     * @param bitsPerPixel the raster bits per pixel
     * @param pixels       the raster pixel data
     */
    /*package*/ BpiRaster (int width, int height, int bitsPerPixel, int[] pixels) {
	this.width = width;
	this.height = height;
	this.bitsPerPixel = bitsPerPixel;
	this.pixels = pixels;
    }

    /**
     * Returns the width of the raster.
     *
     * @return the width of the raster.
     */
    public int getWidth() {
	return width;
    }

    /**
     * Returns the height of the raster.
     *
     * @return the height of the raster.
     */
    public int getHeight() {
	return height;
    }

    /**
     * Returns the size of a pixel in bits.
     *
     * @return the size of a pixel, in bits.
     */
    public int getPixelSize() {
	return bitsPerPixel;
    }

    /**
     * Returns the raw value of a pixel.
     *
     * @param x the x coordinate of the pixel.
     * @param y the y coordinate of the pixel.
     * @return the raw value of the pixel.
     * @throws IndexOutOfBoundsException if the coordinates are not valid.
     */
    public int getPixel (int x, int y) {
	return pixels[y*width + x];
    }

    /**
     * Sets the raw value of a pixel.
     *
     * @param x the x coordinate of the pixel.
     * @param y the y coordinate of the pixel.
     * @param pixel the new raw value of the pixel.
     * @throws IndexOutOfBoundsException if the coordinates are not valid.
     */
    public void setPixel (int x, int y, int pixel) {
	pixels[y*width + x] = pixel & ((1 << bitsPerPixel) - 1);
    }

    /**
     * Replaces the raw value of pixels by a new one for each pixel of the raster.
     *
     * @param from the old value, value to be replaced.
     * @param to   the new value of the pixels.
     */
    public void replacePixel (int from, int to) {
	try {
	    for (int i = 0; true; i++) {
		if (pixels[i] == from)
		    pixels[i] = to;
	    }
	} catch (ArrayIndexOutOfBoundsException aiobe) {
	    // reached the end of the array
	}
    }
}
