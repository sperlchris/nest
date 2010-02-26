package org.esa.nest.util;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.esa.beam.visat.VisatApp;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Attribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


public final class ftpUtils {

    private final FTPClient ftpClient = new FTPClient();
    private boolean ftpClientConnected = false;

    public enum FTPError { FILE_NOT_FOUND, OK, READ_ERROR }

    public ftpUtils(final String server) throws IOException {
        this(server, "anonymous", "anonymous");
    }

    private ftpUtils(final String server, final String user, final String password) throws IOException {
        ftpClient.connect(server);
        int reply = ftpClient.getReplyCode();
        if (FTPReply.isPositiveCompletion(reply))
            ftpClientConnected = ftpClient.login(user, password);
        if (!ftpClientConnected) {
            disconnect();
            throw new IOException("Unable to connect to "+server);
        } else {
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setDataTimeout(30000);
        }
    }

    public void disconnect() throws IOException {
        if(ftpClientConnected) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    public FTPError retrieveFile(final String remotePath, final File localFile, final Long fileSize) {
        FileOutputStream fos = null;
        InputStream fis = null;
        try {
            System.out.println("ftp retrieving "+remotePath);
            
            fis = ftpClient.retrieveFileStream(remotePath);
            if(fis == null) {
                final int code = ftpClient.getReplyCode();
                System.out.println("error code:"+code + " on " + remotePath);
                if(code == 550)
                    return FTPError.FILE_NOT_FOUND;
                else
                    return FTPError.READ_ERROR;
            }

            fos = new FileOutputStream(localFile.getAbsolutePath());

            final VisatApp visatApp = VisatApp.getApp();

            if(false) {//visatApp != null) {
                if(!readFile(fis, fos, localFile.getName(), fileSize)) {
                    return FTPError.READ_ERROR;    
                }
            } else {
                final int size = 8192;
                final byte[] buf = new byte[size];
                int n;
                int total = 0, lastPct = 0;
                while ((n = fis.read(buf, 0, size)) > -1)  {
                    fos.write(buf, 0, n);
                    if(visatApp != null) {
                        if(fileSize != null) {
                            total += n;
                            final int pct = (int)((total/(float)fileSize) * 100);
                            if(pct >= lastPct + 10) {
                                visatApp.setStatusBarMessage("Downloading "+localFile.getName()+"... "+pct+"%");
                                lastPct = pct;
                            }
                        } else {
                            visatApp.setStatusBarMessage("Downloading "+localFile.getName()+"... ");
                        }
                    }
                }
                if(visatApp != null)
                    visatApp.setStatusBarMessage("");
            }

            ftpClient.completePendingCommand();
            return FTPError.OK;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return FTPError.READ_ERROR;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static long getFileSize(final FTPFile[] fileList, final String remoteFileName) {
        for(FTPFile file : fileList) {
            if(file.getName().equalsIgnoreCase(remoteFileName)) {
                return file.getSize();
            }
        }
        return 0;
    }

    private static boolean readFile(final InputStream fis, final FileOutputStream fos,
                                 final String fileName, final long fileSize) throws Exception {

        final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(VisatApp.getApp().getMainFrame(), "Downloading...") {
            @Override
            protected Object doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                final int size = 4096;
                final byte[] buf = new byte[size];

                pm.beginTask("Downloading "+fileName, (int)(fileSize/size) + 200);
                try {
                    int n;
                    while ((n = fis.read(buf, 0, size)) > -1) {
                        fos.write(buf, 0, n);

                        pm.worked(1);
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                } finally {
                    pm.done();
                }
                return true;
            }
        };
        worker.executeWithBlocking();

        return (Boolean)worker.get();
    }

    private FTPFile[] getRemoteFileList(final String path) throws IOException {
        return ftpClient.listFiles(path);
    }

    public static String getPathFromSettings(final String tag) {
        String path = Settings.instance().get(tag);
        path = path.replace("\\", "/");
        if(!path.endsWith("/"))
            path += "/";
        return path;
    }

    public static Map<String, Long> readRemoteFileList(final ftpUtils ftp, final String server, final String remotePath) {

        boolean useCachedListing = true;
        final String tmpDirUrl = ResourceUtils.getApplicationUserTempDataDir().getAbsolutePath();
        final File listingFile = new File(tmpDirUrl+"//"+server+".listing.xml");
        if(!listingFile.exists())
            useCachedListing = false;

        final Map<String, Long> fileSizeMap = new HashMap<String, Long>(900);

        if(useCachedListing) {
            org.jdom.Document doc = null;
            try {
                doc = XMLSupport.LoadXML(listingFile.getAbsolutePath());
            } catch(IOException e) {
                useCachedListing = false;
            }

            if(useCachedListing) {
                final Element root = doc.getRootElement();
                boolean listingFound = false;

                final List children1 = root.getContent();
                for (Object c1 : children1) {
                    if (!(c1 instanceof Element)) continue;
                    final Element remotePathElem = (Element) c1;
                    final Attribute pathAttrib = remotePathElem.getAttribute("path");
                    if(pathAttrib != null && pathAttrib.getValue().equalsIgnoreCase(remotePath)) {
                        listingFound = true;
                        final List children2 = remotePathElem.getContent();
                        for (Object c2 : children2) {
                            if (!(c2 instanceof Element)) continue;
                            final Element fileElem = (Element) c2;
                            final Attribute attrib = fileElem.getAttribute("size");
                            if(attrib != null) {
                                try {
                                    fileSizeMap.put(fileElem.getName(), attrib.getLongValue());
                                } catch(Exception e) {
                                    //
                                }
                            }
                        }
                    }
                }
                if(!listingFound)
                    useCachedListing = false;
            }
        }
        if(!useCachedListing) {
            try {
                final FTPFile[] remoteFileList = ftp.getRemoteFileList(remotePath);

                writeRemoteFileList(remoteFileList, server, remotePath, listingFile);

                for (FTPFile ftpFile : remoteFileList)  {
                    fileSizeMap.put(ftpFile.getName(), ftpFile.getSize());
                }                  
            } catch(Exception e) {
                System.out.println("Unable to get remote file list "+e.getMessage());
            }
        }

        return fileSizeMap;
    }

    private static void writeRemoteFileList(final FTPFile[] remoteFileList, final String server,
                                           final String remotePath, final File file) {

        final Element root = new Element("remoteFileListing");
        root.setAttribute("server", server);

        final Document doc = new Document(root);
        final Element remotePathElem = new Element("remotePath");
        remotePathElem.setAttribute("path", remotePath);
        root.addContent(remotePathElem);

        for (FTPFile ftpFile : remoteFileList)  {
            final Element fileElem = new Element(ftpFile.getName());
            fileElem.setAttribute("size", String.valueOf(ftpFile.getSize()));
            remotePathElem.addContent(fileElem);
        }
        XMLSupport.SaveXML(doc, file.getAbsolutePath());
    }

    public static boolean testFTP(final String remoteFTP, final String remotePath) {
        try {
            final ftpUtils ftp = new ftpUtils(remoteFTP);

            final FTPFile[] remoteFileList = ftp.getRemoteFileList(remotePath);
            ftp.disconnect();

            return (remoteFileList != null);

        } catch(Exception e) {
            final String errMsg = "Error connecting via FTP: "+e.getMessage();
            System.out.println(errMsg);
            if(VisatApp.getApp() != null) {
                VisatApp.getApp().showErrorDialog(errMsg);
            }
        }
        return false;
    }
}