/*
Copyright (C) 2009 Rachel Engel

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.isecpartners.gizmo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * A simple data access layer for blobs stored on disk.
 *
 */
public class BlobAccessor {
    private int blobCount = 0;
    private String cacheDirectory = GizmoView.HOME_DIR + File.separator + "cache";
    private static BlobAccessor instance;

    /** Maximum blob size. */
    public static final long MAXIMUM_BLOB_SIZE = 1024 * 1024 * 20;


    /**
     * Creates cacheDirectory if it does not already exist.
     *
     * TODO: Handle File.mkdir's and File.delete's potential
     * SecurityException. We should warn the user with a dialog box in this
     * case.
     */
    private BlobAccessor() {
        File d = new File(cacheDirectory);
        if (! d.exists()) {
            d.mkdir();
        }
    }


    /**
     * @return The BlobAccessor instance.
     */
    public static synchronized BlobAccessor getFactory() {
        if (instance == null) {
            instance = new BlobAccessor();
        }
        return instance;
    }


    /**
     * @param contentID The ID for the blob to be retrieved.
     *
     * @return The contents of the identified blob.
     */
    public String getBlob(int contentID) {
        String f = cacheDirectory + File.separator + "file" + contentID;
        try {
            FileReader rdr = new FileReader(f);
            File fl = new File(f);
            long lngth = fl.length();
            if (lngth > MAXIMUM_BLOB_SIZE) {
                throw new Exception("Blob " + contentID + " too large (" + lngth + " bytes)");
            }

            char [] bts = new char[(int) lngth];
            rdr.read(bts);
            rdr.close();
            return new String(bts);
        }
        catch (Exception e) {
            System.out.println(e);
            return "";
        }
    }

    public List<String> getAllFilePaths() {
        LinkedList<String> all_messages = new LinkedList<String>();

        for (int ii=0; ii<blobCount; ii++) {
            all_messages.add(cacheDirectory + File.separator + "file" + ii);
        }

        return all_messages;
    }


    /**
     * Stores a blob in the cacheDirectory.
     *
     * @param blob The blob to store.
     *
     * @return The ID under which the blob was stored.
     */
    // TODO: Should we specify an encoding in addBlob and in getBlob?
    public synchronized int addBlob(String str) {
        String f = cacheDirectory + File.separator + "file" + blobCount;
        try {
            FileWriter fout = new FileWriter(f);
            fout.write(str);
            fout.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        return ++blobCount - 1;
    }
}

