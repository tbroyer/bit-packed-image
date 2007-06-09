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
 * Something like an ArrayList of integers which can check for duplicates when
 * you add values and either throw an exception or does nothing.
 *
 * @author  Thomas Broyer
 */
class IntegerSet {
    protected int[] array;
    protected int size = 0;

    protected boolean allowDuplicate = false;
    protected boolean throwDuplicateException = true;

    /** Creates a new instance of IntegerSet */
    public IntegerSet() {
	this(10);
    }

    public IntegerSet (int capacity) {
	array = new int[capacity];
    }

    public IntegerSet (int[] initialValues) {
	this(initialValues, 0, initialValues.length);
    }

    public IntegerSet (int[] initialValues, int offset, int size) {
	array = new int[size];
	System.arraycopy(initialValues, offset, array, 0, size);
	this.size = size;
    }

    public IntegerSet (String stringVal) {
	String[] numbers = stringVal.split("\\s+");
	array = new int[numbers.length];
	for (int i = 0; i < numbers.length; i++) {
	    add(Integer.parseInt(numbers[i]));
	}
    }

    public void add (int value) {
	if (!allowDuplicate && contains(value)) {
	    if (throwDuplicateException) {
		throw new IllegalArgumentException("Duplicate entry: " + value);
	    } else {
		return;
	    }
	}
	if (size == array.length)
	    grow();
	array[size++] = value;
    }

    public void set (int index, int value) {
	int oldValue = get(index); // also checks "index"
	if (oldValue == value)
	    return;
	if (!allowDuplicate && contains(value)) {
	    if (throwDuplicateException) {
		throw new IllegalArgumentException("Duplicate entry: " + value);
	    } else {
		return;
	    }
	}
	array[index] = value;
    }

    public boolean contains (int value) {
	return indexOf(value) >= 0;
    }

    public int indexOf (int value) {
	for (int i = 0; i < size; i++) {
	    if (array[i] == value)
		return i;
	}
	return -1;
    }

    public int get (int index) {
	if (index >= size)
	    throw new IndexOutOfBoundsException("index >= size");
	return array[index];
    }

    public int size() {
	return size;
    }

    public void clear() {
	size = 0;
    }

    public void trimToSize() {
	int[] tmp = new int[size];
	System.arraycopy(array, 0, tmp, 0, size);
	array = tmp;
    }

    public int[] toArray() {
	int[] ints = new int[size];
	System.arraycopy(array, 0, ints, 0, size);
	return ints;
    }

    private void grow() {
	int[] tmp = new int[array.length * 15 / 10];
	System.arraycopy(array, 0, tmp, 0, array.length);
	array = tmp;
    }

    public Object clone() {
	return new IntegerSet(array, 0, size);
    }

    public String toString() {
	return toString(0, size);
    }

    public String toString (int offset, int size) {
	StringBuffer sb = new StringBuffer(size * 2);

	for (int i = offset; i < offset + size; i++) {
	    sb.append(array[i]);
	    sb.append(' ');
	}
	sb.setLength(sb.length() - 1);

	return sb.toString();
    }
}
