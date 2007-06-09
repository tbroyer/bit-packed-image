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

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

/**
 * Describes the format of the stream metadata that can be found in a BPI stream
 *
 * @author  Thomas Broyer
 */
class BpiStreamMetadataFormat extends IIOMetadataFormatImpl {
    static private IIOMetadataFormat instance = null;

    static public synchronized IIOMetadataFormat getInstance() {
	if (instance == null) {
	    instance = new BpiStreamMetadataFormat();
	}
	return instance;
    }

    /** Creates a new instance of BpiStreamMetadataFormat */
    public BpiStreamMetadataFormat() {
	super("collection", CHILD_POLICY_SEQUENCE);

	addElement("colormodel", "collection", CHILD_POLICY_EMPTY);

	addElement("raster", "collection", CHILD_POLICY_REPEAT /*CHILD_POLICY_SEQUENCE*/);
	addAttribute("raster", "colormodels", DATATYPE_INTEGER, true, 1, Integer.MAX_VALUE);
    }

    public boolean canNodeAppear (String elementName, ImageTypeSpecifier imageType) {
	return true;
    }
}
