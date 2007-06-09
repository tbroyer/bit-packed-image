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

/**
 *
 * @author  Thomas Broyer
 */
interface BpiImageFormat {
    static public final int[] BITS_PER_COLOR = {
	//         ----opaque----  -----alpha----
	/* Gray */ 1,  2,  4,  8,   4,  8, 12, 16,
	/* ARGB */ 8, 12, 15, 16,  12, 16, 20, 24 };
    static public final int[][] BITS_PER_SAMPLE = {
	// Gray
	{ 1 }, { 2 }, { 4 }, { 8 },
	{ 2, 2 }, { 4, 4 }, { 6, 6 }, { 8, 8 },
	// ARGB
	{ 3, 3, 2 }, { 4, 4, 4 }, { 5, 5, 5 }, { 5, 6, 5 },
	{ 3, 3, 3, 3 }, { 4, 4, 4, 4 }, { 5, 5, 5, 5 },  { 6, 6, 6, 6}
    };
    static public final int[][] COLOR_SAMPLE_MASKS = {
	// Gray
	{ 0x01 }, { 0x03 }, { 0x0F }, { 0xFF },
	{ 0x03 }, { 0x0F }, { 0x03F }, { 0x00FF },
	// ARGB
	{ 0xE0, 0x1C, 0x03 },
	{ 0xF00, 0x0F0, 0x00F },
	{ 0x7C00, 0x03E0, 0x001F },
	{ 0xF800, 0x07E0, 0x001F },
	{ 0x1C0, 0x038, 0x007 },
	{ 0x0F00, 0x00F0, 0x000F },
	{ 0x07C00, 0x003E0, 0x0001F },
	{ 0x03F000, 0x000FC0, 0x00003F }
    };
    static public final int[] ALPHA_SAMPLE_MASKS = {
	//         --opaque--   --------------alpha-------------
	/* Gray */ 0, 0, 0, 0,   0x0C,   0xF0,   0xFC0,   0xFF00,
	/* ARGB */ 0, 0, 0, 0,  0xE00, 0xF000, 0xF8000, 0xFC0000
    };

    static public final int DEPTH_SIZE = 4;
    static public final int ARGB_MASK = 0x08;
    static public final int ALPHA_MASK = 0x04;

    static public final int WIDTH_SIZE = 9;
    static public final int MIN_WIDTH = 1;
    static public final int HEIGHT_SIZE = WIDTH_SIZE;
    static public final int MIN_HEIGHT = MIN_WIDTH;

    static public final int MAX_BITS_PER_COLOR = 24;
    static public final int[][] MAX_BITS_PER_SAMPLE = {
	BITS_PER_SAMPLE[3], BITS_PER_SAMPLE[7], BITS_PER_SAMPLE[11], BITS_PER_SAMPLE[15]
    };

    static public final int MIN_DEPTH_TO_USE_PALETTE = 1;
    static public final int MIN_PALETTE_LENGTH = 2;
    static public final int MAX_PALETTE_LENGTH = (1 << MAX_BITS_PER_COLOR) - 1 + MIN_PALETTE_LENGTH;
}
