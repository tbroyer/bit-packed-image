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

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;

/**
 * Represents a gray with alpha color model with gray and alpha samples packed
 * on a single data element. For opaque gray images, use an IndexColorModel.
 *
 * @author  Thomas Broyer
 */
public class DirectGrayColorModel extends PackedColorModel {
    private int gbits, abits;
    private int gmask, amask;
    private float gscale, ascale;

    /** Creates a new instance of DirectGrayColorModel */
    public DirectGrayColorModel (int gbits, int abits) {
	this(ColorSpace.getInstance(ColorSpace.CS_GRAY), gbits, abits, false,
	     (gbits + abits <= 8) ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT);
	if ((gbits > 8) || (abits > 8) || (gbits < 2) || (abits < 2)) {
	    throw new IllegalArgumentException("gbits or abits is greater than 8 or lesser than 2");
	}
	this.gbits = gbits;
	this.abits = abits;
	this.gmask = (1 << gbits) - 1;
	this.amask = ((1 << abits) - 1) << gbits;
	this.gscale = 255.0f / gmask;
	this.ascale = 255.0f / (amask >> gbits);
    }

    public DirectGrayColorModel (ColorSpace space, int gbits, int abits, boolean isAlphaPremultiplied, int transferType) {
	super(space, gbits + abits, new int[] { (1 << gbits) - 1 },
	      ((1 << abits) - 1) << gbits, isAlphaPremultiplied,
	      Transparency.TRANSLUCENT, transferType);
	if (space.getType() != ColorSpace.TYPE_GRAY) {
	    throw new IllegalArgumentException("space is not a TYPE_GRAY space");
	}
	if ((space.getMinValue(0) != 0.0f) || (space.getMaxValue(0) != 1.0f)) {
	    throw new IllegalArgumentException("min/max component values are not 0.0/1.0");
	}
    }

    public int getGray (int pixel) {
	int gray = pixel & gmask;
	if (isAlphaPremultiplied()) {
	    // divide gray by alpha
	    int alpha = (pixel & amask) >> gbits;
	    gray = (alpha == 0) ? 0 : (int) (gray * gscale * 255.0f / (alpha * ascale) + 0.5f);
	} else if (gscale != 1.0f) {
	    gray = (int) (gray * gscale + 0.5f);
	}
	return gray;
    }

    public int getRed (int pixel) {
	return getGray(pixel);
    }

    public int getGreen (int pixel) {
	return getGray(pixel);
    }

    public int getBlue (int pixel) {
	return getGray(pixel);
    }

    public int getAlpha (int pixel) {
	int alpha = (pixel & amask) >> gbits;
	if (ascale != 1.0f) {
	    alpha = (int) (alpha * ascale + 0.5f);
	}
	return alpha;
    }

    public int getGrayMask() {
	return gmask;
    }

    public int getAlphaMask() {
	return amask;
    }

    public int[] getComponents (int pixel, int[] components, int offset) {
	if (components == null) {
	    components = new int[offset + 2];
	}

	components[offset] = pixel & gmask;
	components[offset + 1] = (pixel & amask) >> gbits;

	return components;
    }

    public int[] getComponents(Object pixel, int[] components, int offset) {
        int intpixel = 0;
        switch (transferType) {
	    case DataBuffer.TYPE_BYTE:
		byte bdata[] = (byte[]) pixel;
		intpixel = bdata[0] & 0xff;
		break;
	    case DataBuffer.TYPE_USHORT:
		short sdata[] = (short[]) pixel;
		intpixel = sdata[0] & 0xffff;
		break;
	    case DataBuffer.TYPE_INT:
		int idata[] = (int[]) pixel;
		intpixel = idata[0];
		break;
	    default:
		throw new UnsupportedOperationException("Not implemented for transferType " + transferType);
	}
        return getComponents(intpixel, components, offset);
    }

    public int getDataElement (int[] components, int offset) {
	return ((components[offset + 1] << gbits) & amask)
	     | (components[offset] & gmask);
    }

    public Object getDataElements (int rgb, Object pixel) {
	int dataelement = 0;

	int alpha = (rgb >>> 24) & 0xFF;
	int red = (rgb >> 16) & 0xFF;
	int green = (rgb >> 8) & 0xFF;
	int blue = rgb & 0xFF;

	Object spixel = null;
	Object dpixel = null;
	float[] color = null;
	float[] tmpColor = null;
	spixel = this.getRGBdefault().getDataElements(rgb, spixel);
	color = this.getRGBdefault().getNormalizedComponents(dpixel, color, 0);
	tmpColor = this.getRGBdefault().getColorSpace().toCIEXYZ(color);
	tmpColor = this.getColorSpace().fromCIEXYZ(tmpColor);
	if (this.hasAlpha()) {
	    tmpColor[1] = color[3];
	}
	return getDataElements(tmpColor, 0, pixel);
//	int gray = (red*77 + green*150 + blue*29 + 128)/256;
//	float gfactor = gscale;
//	if (isAlphaPremultiplied()) {
//	    gfactor /= alpha / 255.0f;
//	}
//
//	alpha = (int) (alpha / ascale + 0.5f);
//	gray = (int) (gray / gfactor + 0.5f);
//	dataelement = ((alpha << gbits) & amask) | (gray & gmask);
//
//	switch (transferType) {
//	    case DataBuffer.TYPE_BYTE:
//		byte[] bdata;
//		if (pixel == null) {
//		    bdata = new byte[1];
//		} else {
//		    bdata = (byte[]) pixel;
//		}
//		bdata[0] = (byte) (dataelement & 0xFF);
//		return bdata;
//	    case DataBuffer.TYPE_USHORT:
//		short[] sdata;
//		if (pixel == null) {
//		    sdata = new short[1];
//		} else {
//		    sdata = (short[]) pixel;
//		}
//		sdata[0] = (short) (dataelement & 0xFFFF);
//		return sdata;
//	    case DataBuffer.TYPE_INT:
//		int[] idata;
//		if (pixel == null) {
//		    idata = new int[1];
//		} else {
//		    idata = (int[]) pixel;
//		}
//		idata[0] = dataelement;
//		return idata;
//	    default:
//		throw new UnsupportedOperationException("Not implemented for transferType " + transferType);
//	}
    }

    public Object getDataElements (int[] components, int offset, Object obj) {
	int dataelement = getDataElement(components, offset);

	switch (transferType) {
	    case DataBuffer.TYPE_BYTE:
		byte[] bdata;
		if (obj == null) {
		    bdata = new byte[1];
		} else {
		    bdata = (byte[]) obj;
		}
		bdata[0] = (byte) (dataelement & 0xFF);
		return bdata;
	    case DataBuffer.TYPE_USHORT:
		short[] sdata;
		if (obj == null) {
		    sdata = new short[1];
		} else {
		    sdata = (short[]) obj;
		}
		sdata[0] = (short) (dataelement & 0xFFFF);
		return sdata;
	    case DataBuffer.TYPE_INT:
		int[] idata;
		if (obj == null) {
		    idata = new int[1];
		} else {
		    idata = (int[]) obj;
		}
		idata[0] = dataelement;
		return idata;
	    default:
		throw new UnsupportedOperationException("Not implemented for transferType " + transferType);
	}
    }

    public WritableRaster createCompatibleWritableRaster (int w, int h) {
	if ((w <= 0) || (h <= 0)) {
	    throw new IllegalArgumentException("Width or height is <= 0");
	}
	if (pixel_bits > 16) {
	    return Raster.createPackedRaster(DataBuffer.TYPE_INT, w, h, getMasks(), null);
	} else if (pixel_bits > 8) {
	    return Raster.createPackedRaster(DataBuffer.TYPE_USHORT, w, h, getMasks(), null);
	} else {
	    return Raster.createPackedRaster(DataBuffer.TYPE_BYTE, w, h, getMasks(), null);
	}
    }

    public boolean isCompatibleRaster (Raster raster) {
	SampleModel sm = raster.getSampleModel();
	SinglePixelPackedSampleModel sppsm;
	if (!(sm instanceof SinglePixelPackedSampleModel)) {
	    return false;
	}
	sppsm = (SinglePixelPackedSampleModel) sm;
	if (sppsm.getNumBands() != 2) {
	    return false;
	}

	int[] masks = sppsm.getBitMasks();
	if ((masks[0] != gmask) || (masks[1] != amask)) {
	    return false;
	}

	return (raster.getTransferType() != transferType);
    }

    public ColorModel coerceData (WritableRaster raster, boolean isAlphaPremultiplied) {
	if (this.isAlphaPremultiplied() == isAlphaPremultiplied) {
	    return this;
	}

	if ((transferType != DataBuffer.TYPE_BYTE) && (transferType != DataBuffer.TYPE_USHORT) &&
	    (transferType != DataBuffer.TYPE_INT)) {
	    throw new UnsupportedOperationException("Not implemented for transferType " + transferType);
	}

	int minX = raster.getMinX();
	int maxX = minX + raster.getWidth();
	int minY = raster.getMinY();
	int maxY = minY + raster.getHeight();
	float alphaScale = 1.0f / (float) (amask >> gbits);
	int[] transparentBlack = new int[] { 0, 0 };
	int[] pixel = null;

	for (; minY <= maxY; minY++) {
	    for (; minX <= maxX; minX++) {
		pixel = raster.getPixel(minX, minY, pixel);
		float normalizedAlpha = pixel[1] * alphaScale;
		if (normalizedAlpha == 0.f) {
		    raster.setPixel(minX, minY, transparentBlack);
		} else {
		    if (!isAlphaPremultiplied) {
			// invert the factor so that we'll divide instead of multiply
			normalizedAlpha = 1.0f / normalizedAlpha;
		    }
		    pixel[0] = (int) (pixel[0] * normalizedAlpha + 0.5f);
		    raster.setPixel(minX, minY, pixel);
		}
	    }
	}

	return new DirectGrayColorModel(getColorSpace(), gbits, abits, isAlphaPremultiplied, transferType);
    }

    public String toString() {
	return "DirectGrayColorModel: gbits=" + Integer.toHexString(gbits)
	    + " abits=" + Integer.toHexString(abits);
    }
}
