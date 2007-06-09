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
public class BitmaskDirectColorModel extends PackedColorModel {
    private int transparentColor;
    private int[] masks;
    private int[] offsets;
    private float[] scaleFactors;

    public BitmaskDirectColorModel (int bits, int rmask, int gmask, int bmask, int transparentColor) {
	this(bits, rmask, gmask, bmask, transparentColor,
	     (bits <= 8) ? DataBuffer.TYPE_BYTE : (bits <= 16) ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_INT);
    }

    public BitmaskDirectColorModel (int bits, int rmask, int gmask, int bmask, int transparentColor, int transferType) {
	super(ColorSpace.getInstance(ColorSpace.CS_sRGB), bits, rmask, gmask, bmask, 0,
	      false, Transparency.BITMASK, transferType);

	this.transparentColor = transparentColor & (rmask | gmask | bmask);

	masks = getMasks();

	SinglePixelPackedSampleModel sppsm = new SinglePixelPackedSampleModel(transferType, 1, 1, masks);
	offsets = sppsm.getBitOffsets();

	scaleFactors = new float[3];
	for (int i = 0; i < 3; i++) {
	    scaleFactors[i] = 255.0f / (masks[i] >> offsets[i]);
	}
    }

    public int getRed (int pixel) {
	int red = (pixel & masks[0]) >>> offsets[0];
	if (scaleFactors[0] != 1.0f) {
	    red = (int) (red * scaleFactors[0] + 0.5f);
	}
	return red;
    }

    public int getGreen (int pixel) {
	int green = (pixel & masks[1]) >>> offsets[1];
	if (scaleFactors[1] != 1.0f) {
	    green = (int) (green * scaleFactors[1] + 0.5f);
	}
	return green;
    }

    public int getBlue (int pixel) {
	int blue = (pixel & masks[2]) >>> offsets[2];
	if (scaleFactors[2] != 1.0f) {
	    blue = (int) (blue * scaleFactors[2] + 0.5f);
	}
	return blue;
    }

    public int getAlpha (int pixel) {
	pixel = pixel & (masks[0] | masks[1] | masks[2]);
	return (pixel == transparentColor) ? 0 : 255;
    }

    public int[] getComponents (int pixel, int[] components, int offset) {
	if (components == null) {
	    components = new int[offset + 3];
	}

	for (int i = 0; i < 3; i++) {
	    components[offset+i] = (pixel & masks[i]) >>> offsets[i];
	}

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
	int pixel = 0;
	for (int i = 0; i < 2; i++) {
	    pixel |= (components[offset+i] << offsets[i]) & masks[i];
	}
	return pixel;
    }

    public Object getDataElements (int rgb, Object pixel) {
	int dataelement = 0;

	int alpha = (rgb >>> 24) & 0xFF;
	int red = (rgb >> 16) & 0xFF;
	int green = (rgb >> 8) & 0xFF;
	int blue = rgb & 0xFF;

	if (alpha == 0) {
	    dataelement = transparentColor;
	} else {
	    red = (int) (red / scaleFactors[0] + 0.5f);
	    green = (int) (green / scaleFactors[1] + 0.5f);
	    blue = (int) (blue / scaleFactors[2] + 0.5f);
	    dataelement = ((red << offsets[0]) & masks[0])
			| ((green << offsets[1]) & masks[1])
			| ((blue << offsets[2]) & masks[2]);
	}

	switch (transferType) {
	    case DataBuffer.TYPE_BYTE:
		byte[] bdata;
		if (pixel == null) {
		    bdata = new byte[1];
		} else {
		    bdata = (byte[]) pixel;
		}
		bdata[0] = (byte) (dataelement & 0xFF);
		return bdata;
	    case DataBuffer.TYPE_USHORT:
		short[] sdata;
		if (pixel == null) {
		    sdata = new short[1];
		} else {
		    sdata = (short[]) pixel;
		}
		sdata[0] = (short) (dataelement & 0xFFFF);
		return sdata;
	    case DataBuffer.TYPE_INT:
		int[] idata;
		if (pixel == null) {
		    idata = new int[1];
		} else {
		    idata = (int[]) pixel;
		}
		idata[0] = dataelement;
		return idata;
	    default:
		throw new UnsupportedOperationException("Not implemented for transferType " + transferType);
	}
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
	if (sppsm.getNumBands() != 3) {
	    return false;
	}

	int[] bitmasks = sppsm.getBitMasks();
	for (int i = 0; i < 3; i++) {
	    if (masks[i] != bitmasks[i]) {
		return false;
	    }
	}

	return (raster.getTransferType() != transferType);
    }

    public boolean equals (Object obj) {
	if (!(obj instanceof BitmaskDirectColorModel)) {
	    return false;
	}
	BitmaskDirectColorModel bdcm = (BitmaskDirectColorModel) obj;
	if (transparentColor != bdcm.transparentColor) {
	    return false;
	}
	return super.equals(obj);
    }

    public String toString() {
	return "BitmaskDirectColorModel: rmask=" + Integer.toHexString(masks[0])
	    + " gmask=" + Integer.toHexString(masks[1]) + " bmask="
	    + Integer.toHexString(masks[2]) + " transparentColor="
	    + Integer.toHexString(transparentColor);
    }
}
