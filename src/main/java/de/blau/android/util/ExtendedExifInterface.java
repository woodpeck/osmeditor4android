package de.blau.android.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Workaround android SDK brokeness While it is possible to write the direction values, it is not possible to read them
 * with the standard android library
 * 
 * @author simon
 *
 */
public class ExtendedExifInterface extends ExifInterface {
    private Metadata metadata;

    public static final String TAG_GPS_IMG_DIRECTION     = "GPSImgDirection";
    public static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";

    /**
     * Construct a new instance
     * 
     * @param filename full path to the file
     * @throws IOException if something goes wrong
     */
    public ExtendedExifInterface(@NonNull String filename) throws IOException {
        super(filename);

        File jpegFile = new File(filename);
        try {
            metadata = JpegMetadataReader.readMetadata(jpegFile);
        } catch (JpegProcessingException e) {
            // broken Jpeg, ignore
            throw new IOException(e.getMessage());
        } catch (Exception ex) {
            // other stuff broken ... for example ArrayIndexOutOfBounds
            throw new IOException(ex.getMessage());
        } catch (Error err) { // NOSONAR crashing is not an option
            // other stuff broken ... for example NoSuchMethodError
            throw new IOException(err.getMessage());
        }
    }

    /**
     * Construct a new instance
     * 
     * Note this variant reads the file twice
     * 
     * @param context Android context
     * @param uri a content of file uri
     * @throws FileNotFoundException if the file can't be found
     * @throws IOException any other kind of error
     */
    public ExtendedExifInterface(@NonNull Context context, @NonNull Uri uri) throws IOException {
        super(context.getContentResolver().openInputStream(uri));
        try {
            metadata = JpegMetadataReader.readMetadata(context.getContentResolver().openInputStream(uri));
        } catch (JpegProcessingException e) {
            // broken Jpeg, ignore
            throw new IOException(e.getMessage());
        } catch (Exception ex) {
            // other stuff broken ... for example ArrayIndexOutOfBounds
            throw new IOException(ex.getMessage());
        } catch (Error err) { // NOSONAR crashing is not an option
            // other stuff broken ... for example NoSuchMethodError
            throw new IOException(err.getMessage());
        }
    }

    @Override
    public String getAttribute(String tag) {
        if (!tag.equals(TAG_GPS_IMG_DIRECTION) && !tag.equals(TAG_GPS_IMG_DIRECTION_REF)) {
            return super.getAttribute(tag);
        } else if (metadata != null) {
            // obtain the Exif directory
            GpsDirectory directory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            // query the tag's value
            if (tag.equals(TAG_GPS_IMG_DIRECTION) && directory.containsTag(GpsDirectory.TAG_IMG_DIRECTION)) {
                String r[] = directory.getString(GpsDirectory.TAG_IMG_DIRECTION).split("/");
                if (r.length != 2) {
                    return null;
                }
                double d = Double.valueOf(r[0]) / Double.valueOf(r[1]);
                Log.d("ExtendedExifInterface", GpsDirectory.TAG_IMG_DIRECTION + " " + d);
                return (Double.toString(d));
            } else if (directory.containsTag(GpsDirectory.TAG_IMG_DIRECTION_REF)) {
                Log.d("ExtendedExifInterface", GpsDirectory.TAG_IMG_DIRECTION_REF + " " + directory.getString(GpsDirectory.TAG_IMG_DIRECTION_REF));
                return directory.getString(GpsDirectory.TAG_IMG_DIRECTION_REF);
            } else {
                Log.d("ExtendedExifInterface", "No direction information");
                return null;
            }
        } else {
            Log.d("ExtendedExifInterface", "No valid metadata");
            return null;
        }
    }
}
