import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the class for recording information about files in ConcurrentHashMap.
 */
public class ThreadScanDirectory extends Thread{

    File tmpDirectory;
    ConcurrentHashMap<String, FileMetadata> tmpDirectoryHashMap;

    /**
     * This is a simple constructor to initialize the folder and the parameter, in which we will read information about the files.
     * @param tmpDirectory - the considered folder
     * @param tmpDirectoryHashMap - the parameter, in which we will read information about the files
     */
    public ThreadScanDirectory(File tmpDirectory, ConcurrentHashMap<String, FileMetadata> tmpDirectoryHashMap) {

        this.tmpDirectory = tmpDirectory;
        this.tmpDirectoryHashMap = tmpDirectoryHashMap;
        start();
    }

    /**
     * This method calls the method for recording information about files.
     */
    public void run() {
        try {
            tmpDirectoryHashMap.putAll(scan(tmpDirectory.getPath(), tmpDirectory, tmpDirectoryHashMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method records information about all the files from this folder to the ConcurrentHashMap.
     * @param root - the way to the folder
     * @param path - folder as an object of class File
     * @param directoryObject - the parameter, in which the files will be written
     * @return the name of the source highest level folder
     * @throws IOException - existence of a file error
     */
    private ConcurrentHashMap<String, FileMetadata> scan(String root, File path, ConcurrentHashMap<String, FileMetadata> directoryObject) throws IOException {

        File[] list = path.listFiles();
        if (list == null) {
            return null;
        }
        String initRoot = root.substring(0, root.indexOf(File.separator, 1) > 0 ? root.indexOf(File.separator, 1) : root.length());
        for (File f : list) {
            directoryObject.put(f.getPath().replaceFirst(initRoot, ""), new FileMetadata(f.getPath(), f.lastModified(), f.isDirectory()));
        }
        return directoryObject;
    }

}
