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
import org.w3c.dom.*;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.util.*;

/**
 *
 * @author  Thomas Broyer
 */
public class BpiImageMetadata extends IIOMetadata {
    // Package scope
    static final String nativeMetadataFormatName = "net_ltgt_bpi_image_1.0";

    static final ColorModel[] baseColorModels = new ColorModel[1 << BpiImageFormat.DEPTH_SIZE];

    static {
	// REMIND: createCompatibleColorModel should use this, instead of this using createCompatibleColorModel
	for (int i = 0; i < baseColorModels.length; i++) {
	    BpiImageMetadata metadata = new BpiImageMetadata(i);
	    baseColorModels[i] = metadata.createCompatibleColorModel();
	}
    }

    static public ColorModel getBaseColorModel (int depth) {
	return baseColorModels[depth];
    }

    static public Iterator getBaseColorModels() {
	return Arrays.asList(baseColorModels).iterator();
    }

    static public BpiImageMetadata inferFrom (RenderedImage image) {
	return inferFrom(new ImageTypeSpecifier(image));
    }

    static public BpiImageMetadata inferFrom (ImageTypeSpecifier imageType) {
	ColorModel cm = imageType.getColorModel();
	for (int i = 0; i < baseColorModels.length; i++) {
	    if (baseColorModels[i].equals(cm)) {
		return new BpiImageMetadata(i);
	    }
	}

	if (cm instanceof IndexColorModel) {
	    IndexColorModel icm = (IndexColorModel) cm;
	    int mapSize = icm.getMapSize();
	    int[] map = new int[mapSize];
	    icm.getRGBs(map);

	    int transparentIndex = icm.getTransparentPixel();
	    boolean hasAlpha = false;
	    boolean isGray = true;
	    int depth = 0;
	    boolean isGrayMap = (mapSize == 2) || (mapSize == 4) || (mapSize == 16) || (mapSize == 256);
	    float scaleFactor = 255.0F / mapSize;
	    for (int i = 0; i < mapSize; i++) {
		int color = map[i];
		int alpha = color >>> 24;
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;

		if ((alpha != 0xFF) && ((alpha != 0) || (i != transparentIndex))) {
		    hasAlpha = true;
		    isGrayMap = false;
		}

		if (isGray && ((red != green) || (red != blue))) {
		    isGray = false;
		    isGrayMap = false;
		}		

		if (isGrayMap) {
		    int gray = (int) (i * scaleFactor + 0.5F);
		    if (red != gray) {
			isGrayMap = false;
		    }
		}
		if (isGray) {
		    if ((depth < 3) && ((red & 7) != ((red >> 4) & 7)))
			depth = 3;
		    if ((depth < 2) && ((red & 3) != ((red >> 2) & 3)))
			depth = 2;
		    if ((depth == 0) && ((red & 1) != ((red >> 1) & 1)))
			depth = 1;
		}
	    }

	    if (isGrayMap) {
		BpiImageMetadata metadata = null;
		switch (mapSize) {
		    case 2:
			metadata = new BpiImageMetadata(0);
		    case 4:
			metadata = new BpiImageMetadata(1);
		    case 16:
			metadata = new BpiImageMetadata(2);
		    case 256:
			metadata = new BpiImageMetadata(3);
		}
		int transparentColor = icm.getTransparentPixel();
		if (transparentColor >= 0) {
		    metadata.hasTransparentColor = true;
		    metadata.transparentColor = map[transparentColor];
		}
		return metadata;
	    } else if (isGray && !hasAlpha) {
		BpiImageMetadata metadata = new BpiImageMetadata(depth);
		int transparentColor = icm.getTransparentPixel();
		metadata.hasTransparentColor = (transparentColor >= 0);
		if (metadata.hasTransparentColor) {
		    mapSize--;
		    int[] cmap = new int[mapSize];
		    System.arraycopy(map, 0, cmap, 0, transparentColor);
		    System.arraycopy(map, transparentColor+1, cmap, transparentColor, mapSize - transparentColor);
		    map = cmap;
		}
		metadata.setPalette(map);
		return metadata;
	    } else {
		// REMIND: find the most appropriate bit depth
		int bpiDepth = 3;
		if (!isGray) bpiDepth |= BpiImageFormat.ARGB_MASK;
		if (hasAlpha) bpiDepth |= BpiImageFormat.ALPHA_MASK;
		BpiImageMetadata metadata = new BpiImageMetadata(bpiDepth);
		metadata.setPalette(map);
		return metadata;
	    }
	}
	SampleModel sm = imageType.getSampleModel();
	if (!BpiCodec.isCompatibleSampleModel(sm)) {
	    throw new IllegalArgumentException("SampleModel incompatible with BPI constraints.");
	}
	// Choose the most appropriate depth
	int depth = (sm.getNumBands() - 1) << 2;
	int[] sampleSize = sm.getSampleSize();
	for (int i = 0; i < 4; i++) {
	    boolean found = true;
	    for (int j = 0; j < sampleSize.length; j++) {
		if (sampleSize[j] > BpiImageFormat.BITS_PER_SAMPLE[depth + i][j])
		    found = false;
	    }
	    if (found) {
		depth += i;
		break;
	    }
	}
	return new BpiImageMetadata(depth);
    }

    protected int depth = 15;
    protected boolean isARGB = true;
    protected boolean hasAlpha = true;
    protected int bitsPerColor = 24;
    protected boolean hasTransparentColor = false;
    protected int transparentColor = 0;
    protected int[] palette = null;
    protected int bitsPerPixel = 24;

    /** Creates a new instance of BpiMetadata */
    public BpiImageMetadata() {
	super(true, nativeMetadataFormatName,
	      BpiImageMetadataFormat.class.getName(),
	      null, null);
    }

    public BpiImageMetadata (int depth) {
	this();

	this.depth = depth;
	isARGB = (depth & BpiImageFormat.ARGB_MASK) == BpiImageFormat.ARGB_MASK;
	hasAlpha = (depth & BpiImageFormat.ALPHA_MASK) == BpiImageFormat.ALPHA_MASK;
	if (hasAlpha)
	    hasTransparentColor = false;
	bitsPerPixel = bitsPerColor = BpiImageFormat.BITS_PER_COLOR[depth];
    }

    protected BpiImageMetadata (BpiImageMetadata metadata) {
	this();

	depth = metadata.depth;
	isARGB = metadata.isARGB;
	hasAlpha = metadata.hasAlpha;
	bitsPerColor = metadata.bitsPerColor;
	hasTransparentColor = metadata.hasTransparentColor;
	transparentColor = metadata.transparentColor;
	if (metadata.palette != null)
	    palette = (int[]) metadata.palette.clone();
	bitsPerPixel = metadata.bitsPerPixel;
    }

    protected BpiImageMetadata (IIOMetadata metadata) throws IIOInvalidTreeException {
	this();

	try {
	    Node root = metadata.getAsTree(nativeMetadataFormatName);
	    setFromNativeTree(root);
	} catch (IllegalArgumentException iae) {
	    Node root = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
	    setFromStandardTree(root);
	}
    }

    public BpiImageMetadata (ImageTypeSpecifier imageType) {
	this();

	ColorModel cm = imageType.getColorModel();
	SampleModel sm = imageType.getSampleModel();

	switch (cm.getTransparency()) {
	    case ColorModel.TRANSLUCENT:
		hasAlpha = true;
		hasTransparentColor = false;
		break;
	    case ColorModel.BITMASK:
		hasAlpha = false;
		hasTransparentColor = true;
		break;
	    default:
		hasAlpha = false;
		hasTransparentColor = false;
	}

	if (cm instanceof IndexColorModel) {
	    IndexColorModel icm = (IndexColorModel) cm;
	    int size = icm.getMapSize();

	    byte[] reds = new byte[size];
	    icm.getReds(reds);
	    byte[] greens = new byte[size];
	    icm.getGreens(greens);
	    byte[] blues = new byte[size];
	    icm.getBlues(blues);

	    // Determines if the palette is a gray map, sets the color space accordingly
	    isARGB = false;
	    for (int i = 0; i < size; i++) {
		int red = reds[i] & 0xFF;
		int green = greens[i] & 0xFF;
		int blue = blues[i] & 0xFF;

		if ((red != green) || (red != blue)) {
		    isARGB = true;
		}
	    }

	    if (isARGB) {
		palette = new int[icm.getMapSize()];
		icm.getRGBs(palette);
		if (hasTransparentColor) {
		    int[] tmp = new int[palette.length - 1];
		    int trans = icm.getTransparentPixel();
		    System.arraycopy(palette, 0, tmp, 0, trans);
		    System.arraycopy(palette, trans + 1, tmp, trans, tmp.length - trans);
		    palette = tmp;
		}
	    } else {
		// TODO: Might have a palette of grays
	    }
	} else {
	    switch (sm.getNumBands()) {
		case 1:
		    isARGB = false;
		    hasAlpha = false;
		    depth = 0;
		    break;
		case 2:
		    isARGB = false;
		    hasAlpha = true;
		    depth = BpiImageFormat.ALPHA_MASK;
		    break;
		case 3:
		    isARGB = true;
		    hasAlpha = false;
		    depth = BpiImageFormat.ARGB_MASK;
		    break;
		case 4:
		    isARGB = true;
		    hasAlpha = true;
		    depth = BpiImageFormat.ARGB_MASK | BpiImageFormat.ALPHA_MASK;
		    break;
		default:
		    throw new IllegalArgumentException("Number of bands not supported: " + sm.getNumBands());
	    }
	}

	// TODO: compute bit depth.
    }

    public BpiImageMetadata (RenderedImage image) {
	this(new ImageTypeSpecifier(image));
    }

//    static public BpiImageMetadata merge (BpiImageMetadata metadata1, BpiImageMetadata metadata2) {
//	BpiImageMetadata metadata = new BpiImageMetadata(metadata1);
//	metadata.merge(metadata2);
//	return metadata;
//    }

    public ColorModel createCompatibleColorModel() {
	if (palette != null) {
	    return new IndexColorModel(bitsPerPixel, palette.length, palette, 0,
		hasAlpha, hasTransparentColor ? palette.length-1 : -1,
		(bitsPerPixel <= 8) ? DataBuffer.TYPE_BYTE :
		    (bitsPerPixel <= 16) ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_INT);
	} else if (isARGB) {
	    if (hasTransparentColor) {
		int[] masks = BpiImageFormat.COLOR_SAMPLE_MASKS[depth];
		return new BitmaskDirectColorModel(bitsPerColor, masks[0], masks[1], masks[2], transparentColor);
	    } else {
		int[] masks = BpiImageFormat.COLOR_SAMPLE_MASKS[depth];
		int alphaMask = BpiImageFormat.ALPHA_SAMPLE_MASKS[depth];
		return new DirectColorModel(bitsPerColor, masks[0], masks[1], masks[2], alphaMask);
	    }
	} else {
	    if (hasAlpha) {
		int[] bitsPerSample = BpiImageFormat.BITS_PER_SAMPLE[depth];
		return new DirectGrayColorModel(bitsPerSample[1], bitsPerSample[0]);
	    } else {
		int numGrays = 1 << bitsPerColor;
		int[] grays = new int[numGrays];
		float scaleFactor = 255.0f / (numGrays - 1);
		for (int i = 0; i < numGrays; i++) {
		    int gray = (int) (i * scaleFactor + 0.5f) & 0xFF;
		    grays[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
		}
		return new IndexColorModel(bitsPerColor, numGrays, grays, 0, false,
			hasTransparentColor ? BpiCodec.scaleSample(transparentColor, 8, bitsPerColor)
			    : -1, DataBuffer.TYPE_BYTE);
	    }
	}
    }

//    public void merge (BpiImageMetadata metadata) {
//	isARGB = isARGB || metadata.isARGB;
//	hasAlpha = hasAlpha || metadata.hasAlpha;
//	bitsPerColor = Math.max(bitsPerColor, metadata.bitsPerColor);
//	if (hasAlpha) {
//	    hasTransparentColor = false;
//	    transparentColor = 0;
//	} else if (!hasTransparentColor && !metadata.hasTransparentColor) {
//	    // change nothing
//	} else if (hasTransparentColor && metadata.hasTransparentColor &&
//		   (transparentColor == metadata.transparentColor)) {
//	    // change nothing
//	} else if (hasTransparentColor && !metadata.hasTransparentColor) {
//	    // change nothing
//	} else if (!hasTransparentColor && metadata.hasTransparentColor) {
//	    hasTransparentColor = true;
//	    transparentColor = metadata.transparentColor;
//	} else {
//	    // both have transparentColor but not the same one
//	    // TODO
//	}
//	if ((palette != null) && (metadata.palette != null)) {
//	    // both have a palette, merge them
//	}
//    }

    public Node getAsTree (String formatName) {
	if (nativeMetadataFormatName.equals(formatName)) {
	    return getNativeTree();
	} else if (IIOMetadataFormatImpl.standardMetadataFormatName.equals(formatName)) {
	    return getStandardTree();
	} else {
	    throw new IllegalArgumentException("Unrecognized format.");
	}
    }

    private Node getNativeTree() {
	IIOMetadataNode node = null;
	IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

	node = getStandardColorSpaceTypeNode();
	root.appendChild(node);

	node = new IIOMetadataNode("Alpha");
	node.setAttribute("value", hasAlpha ? "TRUE" : "FALSE");
	root.appendChild(node);

	node = new IIOMetadataNode("BitDepthType");
	node.setAttribute("value", Integer.toString(depth & 0x3));
	root.appendChild(node);

	if (palette != null) {
	    node = getNativePaletteNode();
	    root.appendChild(node);
	}

	if (hasTransparentColor) {
	    node = new IIOMetadataNode("TransparentColor");
	    if (palette == null) {
		node.setAttribute("value", buildColorString(transparentColor));
	    }
	    root.appendChild(node);
	}

	return root;
    }

    private IIOMetadataNode getStandardColorSpaceTypeNode() {
	IIOMetadataNode node = new IIOMetadataNode("ColorSpaceType");
	node.setAttribute("name", isARGB ? "RGB" : "GRAY");
	return node;
    }

    private IIOMetadataNode getNativePaletteNode() {
	IIOMetadataNode node = new IIOMetadataNode("Palette");
	for (int i = 0; i < palette.length; i++) {
	    IIOMetadataNode entry = new IIOMetadataNode("PaletteEntry");
	    entry.setAttribute("index", Integer.toString(i));
	    Color color = new Color(palette[i]);
	    entry.setAttribute("red", Integer.toString(color.getRed()));
	    entry.setAttribute("green", Integer.toString(color.getGreen()));
	    entry.setAttribute("blue", Integer.toString(color.getBlue()));
	    if (hasAlpha) {
		entry.setAttribute("alpha", Integer.toString(color.getAlpha()));
	    }
	    node.appendChild(entry);
	}
	return node;
    }

    public boolean isReadOnly() {
	return false;
    }

    public void mergeTree (String formatName, Node root) throws IIOInvalidTreeException {
//	BpiImageMetadata metadata = new BpiImageMetadata();
//	metadata.setFromTree(formatName, root);
//	merge(metadata);
	setFromTree(formatName, root);
    }

    public void setFromTree (String formatName, Node root) throws IIOInvalidTreeException {
	if (nativeMetadataFormatName.equals(formatName)) {
	    reset();
	    setFromNativeTree(root);
	} else if (IIOMetadataFormatImpl.standardMetadataFormatName.equals(formatName)) {
	    reset();
	    setFromStandardTree(root);
	} else {
	    throw new IllegalArgumentException("Unrecognized format: " + formatName);
	}
    }

    public void reset() {
	depth = 15;
	isARGB = true;
	hasAlpha = true;
	bitsPerColor = 24;
	hasTransparentColor = false;
	transparentColor = 0;
	palette = null;
	bitsPerPixel = 24;
    }

    private void setFromNativeTree (Node root) throws IIOInvalidTreeException {
	if (!root.getNodeName().equals("collection")) {
	    throw new IIOInvalidTreeException("Invalid root name: " + root.getNodeName(), root);
	}

	for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
	    if (node.getNodeType() != Node.ELEMENT_NODE)
		continue;
	    String nodeName = node.getNodeName();
	    int maxNodeType = 0;
	    if (nodeName.equals("ColorSpaceType")) {
		if (maxNodeType == 1) {
		    throw new IIOInvalidTreeException("Only one ColorSpaceType allowed", node);
		}
		if (maxNodeType > 1) {
		    throw new IIOInvalidTreeException("ColorSpaceType must appear first.", node);
		}
		maxNodeType = 1;

		String cst = getRequiredAttribute(node, "value");
		if (cst.equals("RGB")) {
		    isARGB = true;
		} else if (cst.equals("GRAY")) {
		    isARGB = false;
		} else {
		    throw new IIOInvalidTreeException("Unknown color space type: " + cst, node);
		}
	    } else if (nodeName.equals("Alpha")) {
		if (maxNodeType == 2) {
		    throw new IIOInvalidTreeException("Only one Alpha allowed", node);
		}
		if (maxNodeType > 2) {
		    throw new IIOInvalidTreeException("Alpha must appear first.", node);
		}
		maxNodeType = 2;

		String al = getRequiredAttribute(node, "value");
		if (al.equals("TRUE")) {
		    hasAlpha = true;
		} else if (al.equals("FALSE")) {
		    hasAlpha = false;
		} else {
		    throw new IIOInvalidTreeException("Unknown alpha value: " + al, node);
		}
	    } else if (nodeName.equals("BitDepthType")) {
		if (maxNodeType == 3) {
		    throw new IIOInvalidTreeException("Only one BitDepthType allowed", node);
		}
		if (maxNodeType > 3) {
		    throw new IIOInvalidTreeException("BitDepthType must appear first.", node);
		}
		maxNodeType = 3;

		String bdt = getRequiredAttribute(node, "value");
		try {
		    depth = Integer.parseInt(bdt, 10);
		    if ((depth < 0) || (depth > 3)) {
			throw new IIOInvalidTreeException("Invalid BitDepthType value: " + bdt, node);
		    }
		    // compute actual depth value
		    if (isARGB)
			depth += BpiImageFormat.ARGB_MASK;
		    if (hasAlpha)
			depth += BpiImageFormat.ALPHA_MASK;
		    bitsPerColor = bitsPerPixel = BpiImageFormat.BITS_PER_COLOR[depth];
		} catch (NumberFormatException nfe) {
		    throw new IIOInvalidTreeException("Invalid integer value: " + bdt, nfe, node);
		}
	    } else if (nodeName.equals("Palette")) {
		if (maxNodeType == 4) {
		    throw new IIOInvalidTreeException("Only one Palette allowed", node);
		}
		if (maxNodeType > 4) {
		    throw new IIOInvalidTreeException("Palette must appear first.", node);
		}
		maxNodeType = 4;

		readPaletteNode(node);
	    } else if (nodeName.equals("TransparentColor")) {
		if (maxNodeType == 5) {
		    throw new IIOInvalidTreeException("Only one TransparentColor allowed", node);
		}
		if (maxNodeType > 5) {
		    throw new IIOInvalidTreeException("TransparentColor must appear first.", node);
		}
		maxNodeType = 5;

		String color = getOptionalAttribute(node, "value", null);
		if ((color != null) && (palette != null)) {
		    throw new IIOInvalidTreeException("TransparentColor has value while there is a Palette", node);
		} else if ((color == null) && (palette == null)) {
		    throw new IIOInvalidTreeException("TransparentColor has no value while there is no Palette", node);
		}
		if (palette == null) {
		    try {
			transparentColor = readColorString(color);
		    } catch (Exception e) {
			throw new IIOInvalidTreeException("Error while parsing TransparentColor value", e, node);
		    }
		}
	    }
	}
    }

    private void setFromStandardTree (Node root) throws IIOInvalidTreeException {
	for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
	    if (node.getNodeType() != Node.ELEMENT_NODE)
		continue;
	    String nodeName = node.getNodeName();
	    if (nodeName.equals("Chroma")) {
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
		    nodeName = child.getNodeName();
		    if (nodeName.equals("ColorSpaceType")) {
			String cst = getRequiredAttribute(child, "value");
			if (cst.equals("RGB")) {
			    isARGB = true;
			    depth |= BpiImageFormat.ARGB_MASK;
			} else if (cst.equals("GRAY")) {
			    isARGB = false;
			    depth &= ~BpiImageFormat.ARGB_MASK;
			} else {
			    throw new IIOInvalidTreeException("Unknown color space type: " + cst, node);
			}
		    } else if (nodeName.equals("NumChannels")) {
			String value = getRequiredAttribute(child, "value");
			try {
			    int numChannels = Integer.parseInt(value, 10);
			    if ((numChannels < 1) || (numChannels > 4)) {
				throw new IIOInvalidTreeException("Invalid NumChannels value: " + value, child);
			    }
			    depth = ((numChannels - 1) << 2) + (depth & 0x03);
			    isARGB = (depth & BpiImageFormat.ARGB_MASK) == BpiImageFormat.ARGB_MASK;
			    hasAlpha = (depth & BpiImageFormat.ALPHA_MASK) == BpiImageFormat.ALPHA_MASK;
			} catch (NumberFormatException nfe) {
			    throw new IIOInvalidTreeException("Error while parsing NumChannels value", nfe, child);
			}
		    } else if (nodeName.equals("BlackIsZero")) {
			String value = getOptionalAttribute(child, "value", "TRUE");
			if (!value.equals("TRUE")) {
			    throw new IIOInvalidTreeException("Only TRUE value is supported for BlackIsZero", child);
			}
		    } else if (nodeName.equals("Palette")) {
			readPaletteNode(child);
		    }
		}
	    } else if (nodeName.equals("Data")) {
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
		    nodeName = child.getNodeName();
		    if (nodeName.equals("SampleFormat")) {
			String value = getRequiredAttribute(child, "value");
			if (value.equals("UnsignedIntegral")) {
			    if (palette != null) {
				throw new IIOInvalidTreeException("SampleFormat=UnsignedIntgral while there is a palette", child);
			    }
			} else if (value.equals("Index")) {
			    if (palette == null) {
				throw new IIOInvalidTreeException("SampleFormat=Index while there is no palette", child);
			    }
			} else {
			    throw new IIOInvalidTreeException("Unsupported SampleFormat value: " + value, child);
			}
		    } else if (nodeName.equals("BitsPerSample")) {
			String value = getRequiredAttribute(child, "value");
			int[] bitsPerSample = new IntegerSet(value).toArray();
			boolean found = false;
			for (int i = 0; !found || (i < BpiImageFormat.BITS_PER_SAMPLE.length); i++) {
			    int[] bps = BpiImageFormat.BITS_PER_SAMPLE[i];
			    found = Arrays.equals(bps, bitsPerSample);
			}
		    }
		}
	    } else if (nodeName.equals("Transparency")) {
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
		    nodeName = child.getNodeName();
		    if (nodeName.equals("Alpha")) {
			String value = getRequiredAttribute(child, "value");
			if (value.equals("none")) {
			    hasAlpha = false;
			    depth &= ~BpiImageFormat.ALPHA_MASK;
			} else if (value.equals("nonpremultiplied")) {
			    hasAlpha = true;
			    depth |= BpiImageFormat.ALPHA_MASK;
			} else {
			    throw new IIOInvalidTreeException("Unsupported Alpha value: " + value, child);
			}
		    } else if (nodeName.equals("TransparentColor")) {
			if (palette != null) {
			    throw new IIOInvalidTreeException("TransparentColor set with a Palette", child);
			}
			if (hasAlpha) {
			    throw new IIOInvalidTreeException("Alpha and TransparentColor not supported", child);
			}
			hasTransparentColor = true;
			String color = getRequiredAttribute(child, "value");
			try {
			    transparentColor = readColorString(color);
			} catch (Exception e) {
			    throw new IIOInvalidTreeException("Error while reading TransparentColor value", e, child);
			}
		    } else if (nodeName.equals("TransparentIndex")) {
			if (palette == null) {
			    throw new IIOInvalidTreeException("TransparentIndex set with no Palette", child);
			}
			if (hasAlpha) {
			    throw new IIOInvalidTreeException("Alpha and TransparentIndex not supported", child);
			}
			hasTransparentColor = true;
		    }
		}
	    }
	}
    }

    private String getOptionalAttribute(Node node, String attrName, String defaultValue) {
	Element elt = (Element) node;
	String value = elt.getAttribute(attrName);
	if (value == null)
	    return defaultValue;
	return value;
    }

    private String getRequiredAttribute(Node node, String attrName) throws IIOInvalidTreeException {
	Element elt = (Element) node;
	String value = elt.getAttribute(attrName);
	if (value == null) {
	    throw new IIOInvalidTreeException(attrName + " attribute is mandatory on " + node.getNodeName() + " element", node);
	}
	return value;
    }

    private void readPaletteNode (Node node) throws IIOInvalidTreeException {
	Element elt = (Element) node;
	IntegerSet entries = new IntegerSet(new int[elt.getElementsByTagName("PaletteEntry").getLength()]);
	int transparentIndex = -1;
	for (Node entry = node.getFirstChild(); entry != null; entry = entry.getNextSibling()) {
	    if (entry.getNodeType() != Node.ELEMENT_NODE)
		continue;
	    if (node.getNodeName().equals("PaletteEntry")) {
		String index = getRequiredAttribute(entry, "index");
		String red = getRequiredAttribute(entry, "red");
		String green = getRequiredAttribute(entry, "green");
		String blue = getRequiredAttribute(entry, "blue");
		String alpha = getOptionalAttribute(entry, "alpha", "255");

		try {
		    int i = Integer.parseInt(index, 10);
		    int r = Integer.parseInt(red, 10);
		    int g = Integer.parseInt(green, 10);
		    int b = Integer.parseInt(blue, 10);
		    int a = Integer.parseInt(alpha, 10);

		    if (!hasAlpha && (a != 255)) {
			if (a == 0) {
			    if (transparentIndex >= 0) {
				throw new IIOInvalidTreeException("There cannot be two transparent colors in a palette", entry);
			    } else {
				transparentIndex = i;
			    }
			} else {
			    throw new IIOInvalidTreeException("Alpha has been previously disallowed, palette color cannot be semi-transparent", entry);
			}
		    }

		    Color color = new Color(r, g, b, a);
		    entries.set(i, color.getRGB());
		} catch (IIOInvalidTreeException ite) {
		    throw ite;
		} catch (Exception e) {
		    throw new IIOInvalidTreeException("Error while processing palette entry", e, entry);
		}
	    }
	}
	if (entries.size() < 3) {
	    throw new IIOInvalidTreeException("Palette has not enough entries", node);
	}
	setPalette(entries.toArray());
    }

    protected IIOMetadataNode getStandardChromaNode() {
	IIOMetadataNode chroma = new IIOMetadataNode("Chroma");
	IIOMetadataNode node = null;

	node = getStandardColorSpaceTypeNode();
	chroma.appendChild(node);

	node = new IIOMetadataNode("NumChannels");
	node.setAttribute("value", Integer.toString(((depth >> 2) & 0x3) + 1));
	chroma.appendChild(node);

	// No gamma info

	node = new IIOMetadataNode("BlackIsZero");
	node.setAttribute("value", "TRUE");
	chroma.appendChild(node);

	if (palette != null) {
	    node = getNativePaletteNode();
	    if (hasTransparentColor) {
		IIOMetadataNode entry = new IIOMetadataNode("PaletteEntry");
		entry.setAttribute("index", Integer.toString(node.getLength()));
		entry.setAttribute("red", "0");
		entry.setAttribute("green", "0");
		entry.setAttribute("blue", "0");
		entry.setAttribute("alpha", "0");
		node.appendChild(entry);
	    }
	    chroma.appendChild(node);
	}

	// No background (index or color) info

	return chroma;
    }

    protected IIOMetadataNode getStandardDataNode() {
	IIOMetadataNode data = new IIOMetadataNode("Data");
	IIOMetadataNode node = null;

	node = new IIOMetadataNode("SampleFormat");
	node.setAttribute("value", (palette == null) ? "UnsignedIntegral" : "Index");
	data.appendChild(node);

	node = new IIOMetadataNode("BitsPerSample");
	if (palette == null) {
	    node.setAttribute("value", buildIntegerListString(BpiImageFormat.BITS_PER_SAMPLE[depth]));
	} else {
	    node.setAttribute("value", Integer.toString(bitsPerPixel));
	}
	data.appendChild(node);

	return data;
    }

    protected IIOMetadataNode getStandardTransparencyNode() {
	IIOMetadataNode transparency = new IIOMetadataNode("Transparency");
	IIOMetadataNode node = null;

	node = new IIOMetadataNode("Alpha");
	node.setAttribute("value", hasAlpha ? "nonpremultiplied" : "none");
	transparency.appendChild(node);

	if (hasTransparentColor) {
	    if (palette == null) {
		node = new IIOMetadataNode("TransparentColor");
		node.setAttribute("value", buildColorString(transparentColor));
		transparency.appendChild(node);
	    } else {
		node = new IIOMetadataNode("TransparentIndex");
		node.setAttribute("value", Integer.toString(palette.length));
		transparency.appendChild(node);
	    }
	}

	return transparency;
    }

    protected void setPalette (int[] palette) {
	// adds an empty color entry at the end when there is a transparent color
	// otherwise, there are problems creating IndexColorModels (or we must
	// do thits each time we create an IndexColorModel...)
	int size = palette.length;
	this.palette = new int[hasTransparentColor ? size + 1 : size];
	System.arraycopy(palette, 0, this.palette, 0, size);

	// compute bits per pixel
	int maxIndex = this.palette.length - 1;
	bitsPerPixel = 1;
	while ((maxIndex >>> bitsPerPixel) != 0) {
	    bitsPerPixel++;
	}
    }

    private String buildColorString (int rgb) {
	return buildIntegerListString(
	    new int[] {
		(rgb >> 16) & 0xFF,
		(rgb >> 8) & 0xFF,
		rgb & 0xFF
	    }
	);
    }

    private int readColorString (String color) {
	IntegerSet components = new IntegerSet(color);
	if (components.size() != 3) {
	    throw new IllegalArgumentException("Color must have exactly 3 components");
	}
	Color c = new Color(components.get(0), components.get(1), components.get(2), 255);
	return c.getRGB();
    }

    private String buildIntegerListString (int[] list) {
	if (list == null) {
	    return "";
	}

	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < list.length - 1; i++) {
	    sb.append(list[i]);
	    sb.append(' ');
	}
	sb.append(list[list.length - 1]);

	return sb.toString();
    }
}
