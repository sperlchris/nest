package org.esa.nest.dataio.dem.srtm3_geotiff;

import org.apache.commons.net.ftp.FTPFile;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.util.ResourceUtils;
import org.esa.nest.util.Settings;
import org.esa.nest.util.ftpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds information about a dem file.
 */
public final class SRTM3GeoTiffFile {

    private final SRTM3GeoTiffElevationModel demModel;
    private final File localFile;
    private final ProductReader productReader;
    private boolean localFileExists = false;
    private boolean remoteFileExists = true;
    private SRTM3GeoTiffElevationTile tile = null;
    private ftpUtils ftp = null;
    private FTPFile[] remoteFileList = null;
    private boolean unrecoverableError = false;

    private static final String remoteFTP = Settings.instance().get("DEM/srtm3GeoTiffDEM_FTP");
    private static final String remotePath = ftpUtils.getPathFromSettings("DEM/srtm3GeoTiffDEM_remotePath");

    public SRTM3GeoTiffFile(SRTM3GeoTiffElevationModel model, File localFile, ProductReader reader) {
        this.demModel = model;
        this.localFile = localFile;
        this.productReader = reader;

        if (localFile.exists() && localFile.isFile()) {
            localFileExists = true;
        }
    }

    public void dispose() {
        try {
            if(ftp != null)
                ftp.disconnect();
            ftp = null;
            tile.dispose();
            tile = null;
        } catch(Exception e) {
            //
        }
    }

    public String getFileName() {
        return localFile.getName();
    }

    public SRTM3GeoTiffElevationTile getTile() throws IOException {
        if(tile == null) {
            getFile();
        }
        return tile;
    }

    private synchronized void getFile() throws IOException {
        try {
            if(localFileExists) {
                final File dataFile = getFileFromZip(localFile);
                if(dataFile != null) {
                    final Product product = productReader.readProductNodes(dataFile, null);
                    if(product != null) {
                        tile = new SRTM3GeoTiffElevationTile(demModel, product);
                    }
                }
            } else if(remoteFileExists && getRemoteFile()) {
                final File dataFile = getFileFromZip(localFile);
                if(dataFile != null) {
                    final Product product = productReader.readProductNodes(dataFile, null);
                    if(product != null) {
                        tile = new SRTM3GeoTiffElevationTile(demModel, product);
                    }
                }
            }
            if(tile != null) {
                demModel.updateCache(tile);
            } else {
                if(!remoteFileExists && localFileExists) {
                    System.out.println("SRTM unable to reader product "+localFile.getAbsolutePath());
                }
                localFileExists = false;
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            tile = null;
            localFileExists = false;
            if(unrecoverableError) {
                throw new IOException(e);
            }
        }
    }

    private boolean getRemoteFile() throws IOException {
        try {
            if(ftp == null) {
                ftp = new ftpUtils(remoteFTP);

                remoteFileList = ftp.getRemoteFileList(remotePath);
            }

            if(remoteFileList == null && ftp != null) {
                remoteFileList = ftp.getRemoteFileList(remotePath);
            }

            if(remoteFileList == null)
                throw new IOException("Unable to get remote file list");

            final String remoteFileName = localFile.getName();
            final long fileSize = ftpUtils.getFileSize(remoteFileList, remoteFileName);
            
            final ftpUtils.FTPError result = ftp.retrieveFile(remotePath + remoteFileName, localFile, fileSize);
            if(result == ftpUtils.FTPError.OK) {
                return true;
            } else {
                if(result == ftpUtils.FTPError.FILE_NOT_FOUND) {
                    remoteFileExists = false;
                } else {
                    dispose();   
                }
                localFile.delete();
            }

            return false;
        } catch(Exception e) {
            System.out.println(e.getMessage());
            if(ftp == null) {
                unrecoverableError = true;
                remoteFileExists = false;
                throw new IOException("Failed to connect to FTP "+ remoteFTP);
            }
            dispose();
        }
        return false;
    }

    private File getFileFromZip(final File dataFile) throws IOException {
        final String ext = FileUtils.getExtension(dataFile.getName());
        if (ext.equalsIgnoreCase(".zip")) {
            final String baseName = FileUtils.getFilenameWithoutExtension(dataFile.getName()) + ".tif";
            final File newFile = new File(ResourceUtils.getApplicationUserTempDataDir(), baseName);
            if(newFile.exists())
                return newFile;

            ZipFile zipFile = null;
            FileOutputStream fileoutputstream = null;
            try {
                zipFile = new ZipFile(dataFile);
                fileoutputstream = new FileOutputStream(newFile);

                final ZipEntry zipEntry = zipFile.getEntry(baseName);
                if (zipEntry == null) {
                    localFileExists = false;
                    throw new IOException("Entry '" + baseName + "' not found in zip file.");
                }

                final int size = 8192;
                final byte[] buf = new byte[size];
                InputStream zipinputstream = zipFile.getInputStream(zipEntry);

                int n;
                while ((n = zipinputstream.read(buf, 0, size)) > -1)
                    fileoutputstream.write(buf, 0, n);

                return newFile;
            } catch(Exception e) {
                System.out.println(e.getMessage());
                dataFile.delete();
                return null;
            } finally {
                if(zipFile != null)
                    zipFile.close();
                if(fileoutputstream != null)
                    fileoutputstream.close();
            }
        }
        return dataFile;
    }
}