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
import java.awt.image.*;
import java.util.*;

/**
 * Represents stream metadata that can be found in a BPI stream
 *
 * @author  Thomas Broyer
 */
public class BpiStreamMetadata extends IIOMetadata implements Cloneable {
    static final String nativeMetadataFormatName = "net_ltgt_bpi_stream_1.0";

    /**
     * List of IntegerSet, or null if represents a color model.
     * IntegerSets contain indices of color models for the raster.
     */
    private List blocks = new ArrayList();
    private IntegerSet colormodels = new IntegerSet(5);
    private IntegerSet rasters = new IntegerSet(5);

//    private List blocks = new ArrayList();
//    private List colorModels = new ArrayList();
//    private List rasters = new ArrayList();

    /** Creates a new instance of BpiStreamMetadata */
    public BpiStreamMetadata() {
	super(false, nativeMetadataFormatName,
	      BpiStreamMetadataFormat.class.getName(),
	      null, null);

//	reset();
    }

    public BpiStreamMetadata (BpiStreamMetadata metadata) {
	this();

	blocks = new ArrayList(metadata.blocks.size());
	for (Iterator iter = metadata.blocks.iterator(); iter.hasNext(); ) {
	    IntegerSet raster = (IntegerSet) iter.next();
	    if (raster == null) {
		blocks.add(null);
	    } else {
		blocks.add(raster.clone());
	    }
	}
	colormodels = (IntegerSet) metadata.colormodels.clone();
    }

/* old-way storage
    public BpiColorModel addColorModel() {
	BpiColorModel cm = new BpiColorModel(this);
	blocks.add(cm);
	colorModels.add(cm);
	return cm;
    }

    public BpiColorModel getColorModel (int cmIndex) {
	return (BpiColorModel) colorModels.get(cmIndex);
    }

    public int indexOfColorModel (BpiColorModel cm) {
	return colorModels.indexOf(cm);
    }

    public void removeColorModel (int cmIndex) {
	remove(getColorModel(cmIndex));
    }

    public int getNumColorModels() {
	return colorModels.size();
    }

    public Iterator getColorModels() {
	return colorModels.iterator();
    }

    public BpiColorModel getPreviousColorModel (BpiColorModel cm) {
	int index = indexOfColorModel(cm);
	if (index < 1)
	    return null;
	return getColorModel(index - 1);
    }

    public BpiColorModel getNextColorModel (BpiColorModel cm) {
	int index = indexOfColorModel(cm);
	if (index >= getNumColorModels() - 1)
	    return null;
	return getColorModel(index + 1);
    }

    public BpiRaster addRaster (BpiColorModel cm) {
	checkBlock(cm);
	BpiRaster raster = new BpiRaster(this, cm);
	blocks.add(raster);
	rasters.add(raster);
	return raster;
    }

    public BpiRaster addRaster (BpiColorModel[] colormodels) {
	BpiRaster raster = addRaster(colormodels[0]);
	for (int i = 1; i < colormodels.length; i++) {
	    raster.add(colormodels[i]);
	}
	return raster;
    }

    public BpiRaster addRaster (int cmIndex) {
	return addRaster(getColorModel(cmIndex));
    }

    public BpiRaster addRaster (int[] colormodels) {
	BpiRaster raster = addRaster(colormodels[0]);
	for (int i = 1; i < colormodels.length; i++) {
	    raster.add(getColorModel(colormodels[i]));
	}
	return raster;
    }

    public BpiRaster getRaster (int rasterIndex) {
	return (BpiRaster) rasters.get(rasterIndex);
    }

    public int indexOfRaster (BpiRaster raster) {
	return rasters.indexOf(raster);
    }

    public void removeRaster (int rasterIndex) {
	remove(getRaster(rasterIndex));
    }

    public int getNumRasters() {
	return rasters.size();
    }

    public Iterator getRasters() {
	return rasters.iterator();
    }

    public BpiRaster getPreviousRaster (BpiRaster raster) {
	int index = indexOfRaster(raster);
	if (index < 1)
	    return null;
	return getRaster(index - 1);
    }

    public BpiRaster getNextRaster (BpiRaster raster) {
	int index = indexOfRaster(raster);
	if (index >= getNumRasters() - 1)
	    return null;
	return getRaster(index + 1);
    }

    public BpiBlock get (int blockIndex) {
	return (BpiBlock) blocks.get(blockIndex);
    }

    public int indexOf (BpiBlock block) {
	return blocks.indexOf(block);
    }

    public void remove (BpiBlock block) {
	checkBlock(block);
	blocks.remove(block);
	if (block instanceof BpiColorModel) {
	    colorModels.remove(block);
	} else if (block instanceof BpiRaster) {
	    rasters.remove(block);
	}
    }

    public void remove (int blockIndex) {
	remove(get(blockIndex));
    }

    public int getNumBlocks() {
	return blocks.size();
    }

    public Iterator getBlocks() {
	return blocks.iterator();
    }

    public BpiBlock getPrevious (BpiBlock block) {
	int index = indexOf(block);
	if (index < 1)
	    return null;
	return get(index - 1);
    }

    public BpiBlock getNext (BpiBlock block) {
	int index = indexOf(block);
	if (index >= getNumBlocks() - 1)
	    return null;
	return get(index + 1);
    }

    public int getNumImages() {
	int numImages = 0;
	for (Iterator iter = getRasters(); iter.hasNext(); ) {
	    numImages += ((BpiRaster) iter.next()).getNumColorModels();
	}
	return numImages;
    }

    public Iterator getImages() {
	List l = new ArrayList();
	for (Iterator iter = getRasters(); iter.hasNext(); ) {
	    BpiRaster raster = (BpiRaster) iter.next();
	    for (Iterator iter1 = raster.getColorModels(); iter1.hasNext(); ) {
		l.add(new BpiImage((BpiColorModel) iter1.next(), raster));
	    }
	}
	return l.iterator();
    }

    public BpiImage getImage (int imageIndex) {
	int numImages = 0;
	for (Iterator iter = getRasters(); iter.hasNext(); ) {
	    BpiRaster raster = (BpiRaster) iter.next();
	    int nextNumImages = numImages + raster.getNumColorModels();
	    if (nextNumImages > imageIndex) {
		return new BpiImage(raster.getColorModel(imageIndex - numImages), raster);
	    }
	    numImages = nextNumImages;
	}
	throw new ArrayIndexOutOfBoundsException();
    }

    public BpiImage getPreviousImage (BpiImage image) {
	BpiRaster raster = image.getRaster();
	BpiColorModel cm = image.getColorModel();

	int cmIndex = raster.indexOf(cm);
	if (cmIndex > 0)
	    return new BpiImage(raster.getColorModel(cmIndex - 1), raster);

	raster = getPreviousRaster(raster);
	cm = raster.getColorModel(raster.getNumColorModels() - 1);
	return new BpiImage(cm, raster);
    }

    public BpiImage getNextImage (BpiImage image) {
	BpiRaster raster = image.getRaster();
	BpiColorModel cm = image.getColorModel();

	int cmIndex = raster.indexOf(cm);
	if (cmIndex < raster.getNumColorModels() - 1)
	    return new BpiImage(raster.getColorModel(cmIndex + 1), raster);

	raster = getNextRaster(raster);
	cm = raster.getColorModel(0);
	return new BpiImage(cm, raster);
    }

    private void checkBlock (BpiBlock block) {
	if (block.getDescriptor() != this)
	    throw new IllegalArgumentException("Not in descriptor.");
    }
*/

    public Node getAsTree (String formatName) {
	if (!nativeMetadataFormatName.equals(formatName)) {
	    throw new IllegalArgumentException("Unrecognized format.");
	}

	IIOMetadataNode root = new IIOMetadataNode("collection");
	IIOMetadataNode node;

	for (Iterator iter = blocks.iterator(); iter.hasNext(); ) {
	    IntegerSet colormodels = (IntegerSet) iter.next();
	    if (colormodels == null) {
		node = new IIOMetadataNode("colormodel");
	    } else {
		node = new IIOMetadataNode("raster");
		node.setAttribute("colormodels", colormodels.toString());
	    }
	    root.appendChild(node);
	}

	return root;
    }

    public boolean isReadOnly() {
	// TODO
	return false;
    }

    public void mergeTree (String formatName, Node root) throws IIOInvalidTreeException {
	if (!nativeMetadataFormatName.equals(formatName)) {
	    throw new IllegalArgumentException("Unrecognized format " + formatName);
	}

	if (!root.getNodeName().equals("collection")) {
	    throw new IIOInvalidTreeException("Invalid root name: " + root.getNodeName(), root);
	}

	List newBlocks = new ArrayList();
	IntegerSet newCM = new IntegerSet(5);
	IntegerSet newRasters = new IntegerSet(5);
	for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
	    if (node.getNodeType() != Node.ELEMENT_NODE)
		continue;
	    String nodeName = node.getNodeName();
	    if (nodeName.equals("colormodel")) {
		newCM.add(newBlocks.size());
		newBlocks.add(null);
	    } else if (nodeName.equals("raster")) {
		IntegerSet colormodels;
		try {
		    colormodels = new IntegerSet(((Element) node).getAttribute("colormodels"));
		} catch (NumberFormatException nfe) {
		    throw new IIOInvalidTreeException("Error while reading color model index.", nfe, node);
		}
		// check color models
		for (int i = 0; i < colormodels.size(); i++) {
		    int colormodel = colormodels.get(i);
		    if (colormodel < 0) {
			throw new IIOInvalidTreeException("Negative color model index.", node);
		    }
		    if (colormodel >= newCM.size()) {
			throw new IIOInvalidTreeException("Raster cannot reference a color model following it", node);
		    }
		}
		newRasters.add(newBlocks.size());
		newBlocks.add(colormodels);
	    }
	}
	// if no exception was thrown, replace old data with new one
	blocks = newBlocks;
	colormodels = newCM;
	rasters = newRasters;
    }

    public void reset() {
	blocks.clear();
	colormodels.clear();
	rasters.clear();
    }

    public int getNumBlocks() {
	return blocks.size();
    }

    public void addColorModel() {
	colormodels.add(blocks.size());
	blocks.add(null);
    }

    public void addColorModel (int rasterIndex, int cmIndex) {
	// check cmIndex
	colormodels.get(cmIndex);

	IntegerSet raster = (IntegerSet) blocks.get(rasters.get(rasterIndex));
	raster.add(cmIndex);
    }

    public int getNumColorModels() {
	return colormodels.size();
    }

    public int getNumColorModels (int rasterIndex) {
	IntegerSet raster = (IntegerSet) blocks.get(rasters.get(rasterIndex));
	return raster.size();
    }

    public int getColorModel (int index) {
	return colormodels.indexOf(index);
    }

    public int getColorModel (int rasterIndex, int cmIndex) {
	IntegerSet raster = (IntegerSet) blocks.get(rasters.get(rasterIndex));
	return raster.get(cmIndex);
    }

    public void addRaster (int cmIndex) {
	addRaster(new int[] { cmIndex });
    }

    public void addRaster (int[] cmArray) {
	// checks cmArray
	for (int i = 0; i < cmArray.length; i++) {
	    // the value must be a valid color model index.
	    // automatically throws exceptions
	    colormodels.get(cmArray[i]);
	}

	rasters.add(blocks.size());
	blocks.add(new IntegerSet(cmArray));
    }

    public int getNumRasters() {
	return rasters.size();
    }

    public int getRaster (int index) {
	return rasters.indexOf(index);
    }

    public boolean isColorModel (int index) {
	return blocks.get(index) == null;
    }

    public boolean isRaster (int index) {
	return !isColorModel(index);
    }

    public int[] getRasterColorModels (int rasterIndex) {
	int index = rasters.get(rasterIndex);
	IntegerSet cms = (IntegerSet) blocks.get(index);
	return cms.toArray();
    }

    public int indexOfColorModel (int cmIndex) {
	return colormodels.get(cmIndex);
    }

    public int indexOfColorModel(int rasterIndex, int cmIndex) {
	IntegerSet raster = (IntegerSet) blocks.get(rasters.get(rasterIndex));
	return raster.indexOf(cmIndex);
    }

    public int indexOfRaster(int rasterIndex) {
	return rasters.get(rasterIndex);
    }

    public void addImage() {
	addColorModel();
	addRaster(getNumColorModels() - 1);
    }

    public int[] getImage (int imageIndex) {
	int index = 0;
	for (int rasterIndex = 0; rasterIndex < rasters.size(); rasterIndex++) {
	    IntegerSet cms = (IntegerSet) blocks.get(rasters.get(rasterIndex));
	    if (index + cms.size() >= imageIndex) {
		return new int[] { cms.get(imageIndex - index), rasterIndex };
	    }
	    index += cms.size();
	}
	throw new IndexOutOfBoundsException();
    }

    public int getImageColorModel (int imageIndex) {
	int[] image = getImage(imageIndex);
	return image[0];
    }

    public int getImageRaster (int imageIndex) {
	int[] image = getImage(imageIndex);
	return image[1];
    }

    public int getNumImages() {
	int numImages = 0;
	for (int i = 0; i < rasters.size(); i++) {
	    IntegerSet cms = (IntegerSet) blocks.get(rasters.get(i));
	    numImages += cms.size();
	}
	return numImages;
    }

    public int indexOfImage (int rasterIndex) {
	int index = 0;
	for (int i = 0; i < rasterIndex; i++) {
	    IntegerSet cms = (IntegerSet) blocks.get(rasters.get(i));
	    index += cms.size();
	}
	return index;
    }

    public int indexOfImage (int rasterIndex, int cmIndex) {
	int index = indexOfImage(rasterIndex);
	index += indexOfColorModel(rasterIndex, cmIndex);
	return index;
    }
}
