import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders.
 */
public class ScanDirectory {

    private File directory1;      // the source folder
    private File directory2;      // the source folder
    private final String temporaryFileName;
    private TreeMap<String, FileMetadata> directoryOne = new TreeMap<>();
    private TreeMap<String, FileMetadata> directoryTwo = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapForTmpFile = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChanges = new TreeMap<>();

    /**
     * This is a simple constructor that initializes the way to the two folders and name temporary file.
     * @param directory1 - the way to the first folder
     * @param directory2 - the way to the second folder
     * @throws IOException - existence of a file error
     */
    public ScanDirectory(File directory1, File directory2) throws IOException {

        this.directory1 = directory1;
        this.directory2 = directory2;
        this.temporaryFileName = "inform" + (directory1.getAbsolutePath() + directory2.getAbsolutePath()).hashCode() + ".tmp";
    }

    /**
     * This is a simple constructor that initializes the way to the two folders and name temporary file.
     * @param directory1 - the way to the first folder
     * @param directory2 - the way to the second folder
     * @param temporaryFileName - temporary file name
     * @param reUseable - parameter, that checks the the existence of the file (we define it)
     * @throws IOException - existence of a file error
     */
    public ScanDirectory(File directory1, File directory2, String temporaryFileName, boolean reUseable) throws IOException {

        this.directory1 = directory1;
        this.directory2 = directory2;
        if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName)) && !reUseable) {
            throw new IllegalArgumentException("Expected non existent file name, but " + temporaryFileName + " already exists!");
        }
        this.temporaryFileName = temporaryFileName;
    }

    /**
     * This method is the first called in class ScanDirectory. He starts the methods that write files in two TreeMap.
     * Next starts the method synchronizationDirectory().
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     */
    public void begin() throws IOException, ClassNotFoundException {

        scan(directory1.getPath(), directory1, directoryOne);
        scan(directory2.getPath(), directory2, directoryTwo);
        synchronizationDirectory();
    }

    /**
     * This method records information about all the files from this folder to the TreeMap.
     * @param root - the way to the folder
     * @param path - folder as an object of class File
     * @param directoryObject - TreeMap, in which the files will be written
     * @throws IOException - existence of a file error
     */
    private void scan(String root, File path, TreeMap<String, FileMetadata> directoryObject) throws IOException {

        File[] list = path.listFiles();
        if (list == null) {
            return;
        }
        for (File f : list) {
            long lastModified = f.isDirectory() ? 0 : f.lastModified();
            directoryObject.put(f.getPath().replaceFirst(root, ""), new FileMetadata(f.getPath(), f.lastModified(), f.isDirectory()));
            if (lastModified == 0) {
                scan(root, f, directoryObject);
            }
        }
    }

    /**
     * This method synchronizes files from TreeMap1 and TreeMap2 and in relation to a temporary file.
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     */
    private void synchronizationDirectory() throws IOException, ClassNotFoundException {

        if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName))) {
            ObjectInputStream fileStream = new ObjectInputStream(new FileInputStream(temporaryFileName));
            TreeMap<String, FileMetadata> tmpM = (TreeMap<String, FileMetadata>) fileStream.readObject();
            Iterator<Map.Entry<String, FileMetadata>> iter = tmpM.entrySet().iterator();
            mapForTmpFile.putAll(tmpM);
            while (iter.hasNext()) {
                Map.Entry<String, FileMetadata> entry = iter.next();
                String fileName = entry.getKey();
                if (!directoryOne.containsKey(fileName) && directoryTwo.containsKey(fileName) ||
                        directoryOne.containsKey(fileName) && !directoryTwo.containsKey(fileName)) {

                    if (directoryOne.containsKey(fileName)) {
                        File del = new File(directoryOne.get(fileName).getName());
                        if (del.exists()) {
                            tmpM.get(fileName).setFileOperations(true);
                            mapContainsChanges.put(fileName, tmpM.get(fileName));
                            mapForTmpFile.remove(fileName);
                            directoryOne.remove(fileName);
                        }
                    } else if (directoryTwo.containsKey(fileName)) {
                        File del = new File(directoryTwo.get(fileName).getName());
                        if (del.exists()) {
                            tmpM.get(fileName).setFileOperations(true);
                            mapContainsChanges.put(fileName, tmpM.get(fileName));
                            mapForTmpFile.remove(fileName);
                            directoryTwo.remove(fileName);
                        }
                    }
                    iter.remove();
                }
            }
            swapDifferentFiles(directoryOne, directoryTwo);
            swapDifferentFiles(directoryTwo, directoryOne);
        } else {
            swapDifferentFiles(directoryOne, directoryTwo);
            swapDifferentFiles(directoryTwo, directoryOne);
        }

        ObjectOutputStream recordInfFile = new ObjectOutputStream(new FileOutputStream(temporaryFileName));
        recordInfFile.writeObject(mapForTmpFile);
        physicalSynchronization(directory1.getPath());
        physicalSynchronization(directory2.getPath());
    }

    /**
     * This method synchronizes files from TreeMap1 and TreeMap2.
     * @param directoryObject - TreeMap filled with files from first folder
     * @param directoryOther - TreeMap filled with files from second folder
     */
    private void swapDifferentFiles(TreeMap<String, FileMetadata> directoryObject,
                  TreeMap<String, FileMetadata> directoryOther){

        for (String fileName : directoryObject.keySet()) {
            if (directoryOther.containsKey(fileName)){
                if (directoryObject.get(fileName).getTypeFile()) {
                    mapForTmpFile.put(fileName, directoryObject.get(fileName));
                } else if(directoryOther.get(fileName).getTimeChange() < directoryObject.get(fileName).getTimeChange()) {
                        directoryOther.replace(fileName, directoryObject.get(fileName));
                        mapForTmpFile.put(fileName, directoryObject.get(fileName));
                        mapContainsChanges.put(fileName, directoryObject.get(fileName));
                } else if(directoryOther.get(fileName).getTimeChange() == directoryObject.get(fileName).getTimeChange()){
                    mapForTmpFile.put(fileName, directoryObject.get(fileName));
                }
            } else if (!directoryOther.containsKey(fileName)) {
                directoryOther.put(fileName, directoryObject.get(fileName));
                mapForTmpFile.put(fileName, directoryObject.get(fileName));
                mapContainsChanges.put(fileName, directoryObject.get(fileName));
            }
        }
    }

    /**
     * This method is called the method physicalSynchronization(). It is designed to remove files and folders.
     * @param file - the path to the file
     */
    private void deleteAll(File file) {

        if(file.isDirectory()){
            for(File inFile : file.listFiles()){
                deleteAll(inFile);
            }
        }
        file.delete();
    }

    /**
     * This method provides a physical synchronization source folders. It removes, copies, replaces files in folders.
     * @param root - the name of the source folder
     * @throws IOException - error associated with copying/replacement file
     */
    private void physicalSynchronization(String root) throws IOException {

        String initRoot;
        if (root.equals(directory1.getPath())) {
            initRoot = directory1.getPath();
        } else {
            initRoot = directory2.getPath();
        }
        for(String fileName: mapContainsChanges.keySet()) {

            if (mapContainsChanges.get(fileName).getFileOperations()) {
                if (Files.exists(FileSystems.getDefault().getPath(initRoot + File.separator + fileName))) {
                    deleteAll(new File(initRoot + File.separator + fileName));
                }
            } else if (!mapContainsChanges.get(fileName).getTypeFile()){
                Path pathSourse = Paths.get(mapContainsChanges.get(fileName).getName());     // the path to the file that we copied
                Path pathDestination = Paths.get(initRoot + File.separator + fileName);   // path to the file that will be created as a result of copying (including the new file name)
                Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);
            } else {
                File tmpDirectory = new File(initRoot + File.separator + fileName);
                tmpDirectory.mkdir();
            }
        }
    }

}
