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
import java.util.Arrays;

/**
 *
 * @author  Thomas Broyer
 */
class BpiImageMetadataFormat extends IIOMetadataFormatImpl {
    static private IIOMetadataFormat instance = null;

    static public synchronized IIOMetadataFormat getInstance() {
	if (instance == null) {
	    instance = new BpiImageMetadataFormat();
	}
	return instance;
    }

    /** Creates a new instance of BpiImageMetadataFormat */
    public BpiImageMetadataFormat() {
	super(BpiImageMetadata.nativeMetadataFormatName, CHILD_POLICY_SOME);

	// Same as standard ColorSpaceType element
	addElement("ColorSpaceType", BpiImageMetadata.nativeMetadataFormatName, CHILD_POLICY_EMPTY);
	addAttribute("ColorSpaceType", "name", DATATYPE_STRING, true, null,
	    Arrays.asList(new String[] { "RGB", "GRAY" }));

	addElement("Alpha", BpiImageMetadata.nativeMetadataFormatName, CHILD_POLICY_EMPTY);
	addBooleanAttribute("Alpha", "value", false, false);

	addElement("BitDepthType", BpiImageMetadata.nativeMetadataFormatName, CHILD_POLICY_EMPTY);
	addAttribute("BitDepthType", "value", DATATYPE_INTEGER, true, null, "0", "3", true, true);

	// Same as standard Palette and PaletteEntry elements
	addElement("Palette", BpiImageMetadata.nativeMetadataFormatName, 0, Integer.MAX_VALUE);
	addElement("PaletteEntry", "Palette", CHILD_POLICY_EMPTY);
	addAttribute("PaletteEntry", "index", DATATYPE_INTEGER, true, null);
	addAttribute("PaletteEntry", "red", DATATYPE_INTEGER, true, null);
	addAttribute("PaletteEntry", "green", DATATYPE_INTEGER, true, null);
	addAttribute("PaletteEntry", "blue", DATATYPE_INTEGER, true, null);
	addAttribute("PaletteEntry", "alpha", DATATYPE_INTEGER, false, "255");

	// same as standard TransparentColor element, except value attribute is not required.
	addElement("TransparentColor", BpiImageMetadata.nativeMetadataFormatName, CHILD_POLICY_EMPTY);
	addAttribute("TransparentColor", "value", DATATYPE_INTEGER, false, 1, 4);
    }

    public boolean canNodeAppear (String elementName, ImageTypeSpecifier imageType) {
	return true;
    }
}
