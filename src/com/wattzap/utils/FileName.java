/* This file is part of Wattzap Community Edition.
 *
 * Wattzap Community Edtion is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wattzap Community Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wattzap.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.wattzap.utils;

/**
 *
 * @author David George
 * @date 2nd May 2014
 */
public class FileName {
	private static final char EXTENSION_SEPARATOR = '.';
	private static final char DIRECTORY_SEPARATOR = '/';


    /**
	 * Remove the file extension from a filename, that may include a path.
	 *
	 * e.g. /path/to/myfile.jpg -> /path/to/myfile
	 */
	public static String stripExtension(String filename) {
	    if (filename == null) {
	        return null;
	    }
	    int index = indexOfExtension(filename);
	    if (index == -1) {
	        return filename;
	    } else {
	        return filename.substring(0, index);
	    }
	}

	/**
	 * Return the file extension from a filename, excluding the "."
	 *
	 * e.g. /path/to/myfile.jpg -> jpg
	 */
	public static String getExtension(String filename) {
	    if (filename == null) {
	        return null;
	    }
	    int index = indexOfExtension(filename);
	    if (index == -1) {
	        return "";
	    } else {
	        return filename.substring(index + 1);
	    }
	}

	public static int indexOfExtension(String filename) {
	    if (filename == null) {
	        return -1;
	    }

	    // Check that no directory separator appears after the
	    // EXTENSION_SEPARATOR
	    int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
	    int lastDirSeparator = filename.lastIndexOf(DIRECTORY_SEPARATOR);
	    if (lastDirSeparator > extensionPos) {
	        return -1;
	    }
	    return extensionPos;
	}

    /**
	 * Return the file name from a path
	 *
	 * e.g. /path/to/myfile.jpg -> myfile.jpg
	 */
	public static String getName(String filename) {
	    if (filename == null) {
	        return null;
	    }
	    int index = indexOfName(filename);
	    if (index == -1) {
	        return filename;
	    } else {
	        return filename.substring(index + 1);
	    }
	}

	/**
	 * Return the file name from a path
	 *
	 * e.g. /path/to/myfile.jpg -> /path/to
	 */
	public static String getPath(String filename) {
	    if (filename == null) {
	        return null;
	    }
	    int index = indexOfName(filename);
	    if (index < 0) {
	        return "";
	    } else {
	        return filename.substring(0, index);
	    }
	}

	public static int indexOfName(String filename) {
	    if (filename == null) {
	        return -1;
	    }
	    int pos = filename.lastIndexOf(DIRECTORY_SEPARATOR);
	    return pos;
	}
}
