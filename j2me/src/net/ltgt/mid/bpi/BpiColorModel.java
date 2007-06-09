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
 * Represents a BPI color model.
 *
 * @author Thomas Broyer
 * @see BpiDecoder#getColorModel()
 */
public final class BpiColorModel {
    // Package-scope fields
    int depth;
    boolean hasTransparentColor;
    int transparentColor;
    int[] palette;
    int bitsPerPixel;

    /**
     * Creates a new instance of BpiColorModel
     *
     * @param depth               the depth of the color model
     * @param hasTransparentColor whether a transparent color is defined
     * @param transparentColor    the transparent color. Ignored if
     *                            <CODE>hasTransparentColor</CODE> is <CODE>false</CODE>.
     * @param palette             the color model palette as an array of 32-bit
     *                            ARGB colors, or null for a direct color model
     * @param bitsPerPixel        the number of bits per pixel
     */
    /*package*/ BpiColorModel (int depth, boolean hasTransparentColor,
	int transparentColor, int[] palette, int bitsPerPixel) {
	this.depth = depth;
	this.hasTransparentColor = hasTransparentColor;
	this.transparentColor = transparentColor;
	this.palette = palette;
	this.bitsPerPixel = bitsPerPixel;
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
     * Gets whether the color model has a palette or not.
     *
     * @return <CODE>true</CODE> if the color model has a palette.
     */
    public boolean isIndexed() {
	return (palette != null);
    }

    /**
     * Returns the number of colors in the palette.
     *
     * @return the size of the palette.
     * @throws NullPointerException if the color model has no palette.
     * @see #isIndexed()
     */
    public int getMapSize() {
	return palette.length;
    }

    /**
     * Retrieves an ARGB color from its palette index.
     *
     * @param index the index in the palette of the color to retrieve.
     * @return the ARGB color
     * @throws NullPointerException The color model has no palette.
     * @throws IndexOutOfBoundsException the index is not valid.
     */
    public int getPaletteColor (int index) {
	return palette[index];
    }

    /**
     * Sets a palette color to another ARGB color.
     *
     * @param index the index of the palette color to replace.
     * @param argb the new value of the palette color.
     * @throws NullPointerException the color model has no palette
     * @throws IndexOutOfBoundsException the index is not valid.
     */
    public void setPaletteColor (int index, int argb) {
	palette[index] = argb;
    }
}
