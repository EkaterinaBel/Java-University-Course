import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders.
 */
public class ScanDirectory {

    private File directory1;      // the source folder
    private File directory2;      // the source folder
    private final String temporaryFileName;
    private String initRoot1;
    private String initRoot2;
    private HashMap<String, FileMetadata> directoryOne = new HashMap<String, FileMetadata>();
    private HashMap<String, FileMetadata> directoryTwo = new HashMap<String, FileMetadata>();

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
     * @throws IOException - existence of a file error
     */
    private ScanDirectory(File directory1, File directory2, String temporaryFileName) throws IOException {

        this.directory1 = directory1;
        this.directory2 = directory2;
        this.temporaryFileName = temporaryFileName;
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
     * This method is the first called in class ScanDirectory. He starts the methods that write files in two HashMap.
     * Next starts the method synchronizationDirectory().
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     */
    public void begin() throws IOException, ClassNotFoundException {

        initRoot1 = scan(directory1.getPath(), directory1, directoryOne);
        initRoot2 = scan(directory2.getPath(), directory2, directoryTwo);
        synchronizationDirectory();
    }

    /**
     * This method records information about all the files from this folder to the HashMap.
     * @param root - the way to the folder
     * @param path - folder as an object of class File
     * @param directoryObject - HashMap, in which the files will be written
     * @return the name of the source highest level folder
     * @throws IOException - existence of a file error
     */
    private String scan(String root, File path, HashMap<String, FileMetadata> directoryObject) throws IOException {

        File[] list = path.listFiles();
        if (list == null) {
            return null;
        }
        String initRoot = root.substring(0, root.indexOf(File.separator, 1) > 0 ? root.indexOf(File.separator, 1) : root.length());
        for (File f : list) {
            directoryObject.put(f.getPath().replaceFirst(initRoot, ""), new FileMetadata(f.getPath(), f.lastModified(), f.isDirectory()));
        }
        return initRoot;
    }

    /**
     * This method is called the method synchronizationDirectory(). It is designed to remove files and folders.
     * @param file - the path to the file
     * @param map - HashMap from which the removal of this file
     * @return HashMap updated
     */
    private HashMap<String, FileMetadata> deleteAll(File file, HashMap<String, FileMetadata> map) {

        if(file.isDirectory()){
            for(File inFile : file.listFiles()){
                deleteAll(inFile, map);
            }
        }
        map.remove(file.getPath().substring(file.getPath().indexOf(File.separator), file.getPath().length()));
        file.delete();
        return map;
    }

    /**
     * This method synchronizes files from HashMap1 and HashMap2 and in relation to a temporary file.
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     */
    private void synchronizationDirectory() throws IOException, ClassNotFoundException {

        if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName))) {
            ObjectInputStream fileStream = new ObjectInputStream(new FileInputStream(temporaryFileName));
            HashMap<String, FileMetadata> tmpM = (HashMap<String, FileMetadata>) fileStream.readObject();
            Iterator<Map.Entry<String, FileMetadata>> iter = tmpM.entrySet().iterator();
            HashMap<String, FileMetadata> tmpM2 = new HashMap<>();
            tmpM2.putAll(tmpM);
            while (iter.hasNext()) {
                Map.Entry<String, FileMetadata> entry = iter.next();
                String fileName = entry.getKey();
                if (!directoryOne.containsKey(fileName) && directoryTwo.containsKey(fileName) ||
                        directoryOne.containsKey(fileName) && !directoryTwo.containsKey(fileName)) {

                    if (directoryOne.containsKey(fileName)) {
                        File del = new File(directoryOne.get(fileName).getName());
                        if (del.exists()) {
                            tmpM2 = deleteAll(del, tmpM2);
                            directoryOne.remove(fileName);
                        }
                    } else if (directoryTwo.containsKey(fileName)) {
                        File del = new File(directoryTwo.get(fileName).getName());
                        if (del.exists()) {
                            tmpM2 = deleteAll(del, tmpM2);
                            directoryTwo.remove(fileName);
                        }
                    }
                    iter.remove();
                }
            }
            tmpM = tmpM2;
            ObjectOutputStream recordInfFile = new ObjectOutputStream(new FileOutputStream(temporaryFileName));
            recordInfFile.writeObject(tmpM);
        }

        HashMap<String, FileMetadata> nextTmp = new HashMap<>();
        nextTmp.putAll(swapDifferentFiles(directoryOne, directoryTwo));       // copy the necessary files from the first folder to the second
        nextTmp.putAll(swapDifferentFiles(directoryTwo, directoryOne));       // copy the necessary files from the second folder to the first

        if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName))) {
            ObjectInputStream fileStream = new ObjectInputStream(new FileInputStream(temporaryFileName));
            HashMap<String, FileMetadata> tmpM = (HashMap<String, FileMetadata>) fileStream.readObject();
            nextTmp.putAll(tmpM);
        }
        if (nextTmp.size() > 0) {
            if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName))) {;
                File delStream = new File(temporaryFileName);
                delStream.delete();
            }
            ObjectOutputStream recordInfFile = new ObjectOutputStream(new FileOutputStream(temporaryFileName));
            recordInfFile.writeObject(nextTmp);
        }
    }

    /**
     * This method synchronizes files from HashMap1 and HashMap2. If we found in one of their HashMap file that is a directory,
     * it creates an instance of the class ScanDirectory and method begin() is called to him. There recursion.
     * @param directoryObject - HashMap filled with files from first folder
     * @param directoryOther - HashMap filled with files from second folder
     * @return HashMap with synchronized files
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file (here happens when call a method begin() )
     */
    private HashMap<String, FileMetadata> swapDifferentFiles(HashMap<String, FileMetadata> directoryObject,
                  HashMap<String, FileMetadata> directoryOther) throws IOException, ClassNotFoundException {

        HashMap<String, FileMetadata> tmpF = new HashMap<>();
        String initRoot;
        if (directoryObject == directoryOne) {
            initRoot = initRoot2;
        } else {
            initRoot = initRoot1;
        }
        for (String fileName : directoryObject.keySet()) {
            if (directoryObject.get(fileName).getTypeFile()) {
                File tmpDirectory1 = new File(directoryObject.get(fileName).getName());
                File tmpDirectory2 = new File(initRoot + File.separator + fileName);
                if (!directoryOther.containsKey(fileName)) {
                    tmpDirectory2.mkdir();
                }
                ScanDirectory scDir = new ScanDirectory(tmpDirectory1, tmpDirectory2, temporaryFileName);
                scDir.begin();
                tmpF.put(fileName, directoryObject.get(fileName));
            } else if (directoryOther.containsKey(fileName)){
                    if(directoryOther.get(fileName).getTimeChange() < directoryObject.get(fileName).getTimeChange()) {
                        directoryOther.replace(fileName, directoryObject.get(fileName));
                        Path pathSourse = Paths.get(directoryObject.get(fileName).getName());     // the path to the file that we copied
                        Path pathDestination = Paths.get(initRoot + File.separator + fileName);   // path to the file that will be created as a result of copying (including the new file name)
                        Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);
                        tmpF.put(fileName, directoryObject.get(fileName));
                    } else if(directoryOther.get(fileName).getTimeChange() == directoryObject.get(fileName).getTimeChange()){
                        tmpF.put(fileName, directoryObject.get(fileName));
                    }
            } else if (!directoryOther.containsKey(fileName)) {
                directoryOther.put(fileName, directoryObject.get(fileName));
                Path pathSource = Paths.get(directoryObject.get(fileName).getName());
                Path pathDestination = Paths.get(initRoot + File.separator + fileName);
                Files.copy(pathSource, pathDestination);
                tmpF.put(fileName, directoryObject.get(fileName));
            }
        }
        return tmpF;
    }

}
