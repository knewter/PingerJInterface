package com.isotope11.pingerjinterface;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by jadams on 2/4/14.
 */
public class Decompress {
    private InputStream zip;
    private String loc;
    static final String TAG = "PingerJInterfacetag";


    public Decompress(InputStream zipFileInputStream, String location) {
        zip = zipFileInputStream;
        loc = location;

        dirChecker("");
    }

    public void unzip() {
        try {
            ZipInputStream zin = new ZipInputStream(zip);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                Log.d(TAG, "Unzipping " + ze.getName() + " into " + loc);

                if (ze.isDirectory()) {
                    dirChecker(ze.getName());
                } else {
                    FileOutputStream fout = new FileOutputStream(loc + ze.getName());
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }

                    zin.closeEntry();
                    fout.close();
                }

            }
            zin.close();
        } catch (Exception e) {
            Log.e(TAG, "unzip", e);
        }

    }

    private void dirChecker(String dir) {
        File f = new File(loc + dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }
}