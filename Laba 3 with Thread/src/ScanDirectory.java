import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders in multithreaded mode.
 */
public class ScanDirectory{

    private final ConcurrentHashMap<String, FileMetadata> tmpMap;   // parameter for reading and writing data to a temporary file
    private File directory1;      // the source folder
    private File directory2;      // the source folder
    private final String temporaryFileName;
    private String initRoot1;
    private String initRoot2;
    private ConcurrentHashMap<String, FileMetadata> directoryOne = new ConcurrentHashMap<String, FileMetadata>();
    private ConcurrentHashMap<String, FileMetadata> directoryTwo = new ConcurrentHashMap<String, FileMetadata>();
    private boolean initialObject;     // participates in the completion of the main thread and writing to a temporary file
    private boolean readTMPFile;

    /**
     * This is a simple constructor that initializes the way to the two folders and name temporary file.
     * @param directory1 - the way to the first folder
     * @param directory2 - the way to the second folder
     * @throws IOException - existence of a file error
     */
    public ScanDirectory(File directory1, File directory2) throws IOException {

        initialObject = true;
        readTMPFile = true;
        tmpMap = new ConcurrentHashMap<>();
        this.directory1 = directory1;
        this.directory2 = directory2;
        initRoot1 = directory1.getPath().substring(0, directory1.getPath().indexOf(File.separator, 1) > 0
                ? directory1.getPath().indexOf(File.separator, 1) : directory1.getPath().length());
        initRoot2 = directory2.getPath().substring(0, directory2.getPath().indexOf(File.separator, 1) > 0
                ? directory2.getPath().indexOf(File.separator, 1) : directory2.getPath().length());
        this.temporaryFileName = "inform" + (directory1.getAbsolutePath() + directory2.getAbsolutePath()).hashCode() + ".tmp";
    }

    /**
     * This is a simple constructor that initializes the way to the two folders and name temporary file.
     * @param directory1 - the way to the first folder
     * @param directory2 - the way to the second folder
     * @param temporaryFileName - temporary file name
     * @param tmpMap - parameter for reading and writing data to a temporary file
     * @throws IOException - existence of a file error
     */
    private ScanDirectory(File directory1, File directory2, String temporaryFileName, ConcurrentHashMap<String, FileMetadata> tmpMap) throws IOException {

        this.tmpMap = tmpMap;
        this.directory1 = directory1;
        this.directory2 = directory2;
        this.temporaryFileName = temporaryFileName;
        initRoot1 = directory1.getPath().substring(0, directory1.getPath().indexOf(File.separator, 1) > 0
                ? directory1.getPath().indexOf(File.separator, 1) : directory1.getPath().length());
        initRoot2 = directory2.getPath().substring(0, directory2.getPath().indexOf(File.separator, 1) > 0
                ? directory2.getPath().indexOf(File.separator, 1) : directory2.getPath().length());
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

        initialObject = true;
        readTMPFile = true;
        tmpMap = new ConcurrentHashMap<>();
        this.directory1 = directory1;
        this.directory2 = directory2;
        if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName)) && !reUseable) {
            throw new IllegalArgumentException("Expected non existent file name, but " + temporaryFileName + " already exists!");
        }
        this.temporaryFileName = temporaryFileName;
        initRoot1 = directory1.getPath().substring(0, directory1.getPath().indexOf(File.separator, 1) > 0
                ? directory1.getPath().indexOf(File.separator, 1) : directory1.getPath().length());
        initRoot2 = directory2.getPath().substring(0, directory2.getPath().indexOf(File.separator, 1) > 0
                ? directory2.getPath().indexOf(File.separator, 1) : directory2.getPath().length());
    }

    /**
     * This method is the first called in class ScanDirectory. It creates a two threads for writing files the current level of folder
     * in two ConcurrentHashMap. Next starts the method synchronizationDirectory().
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     */
    public void begin() throws IOException, ClassNotFoundException {

        Thread t1 = new ThreadScanDirectory(directory1, directoryOne);
        Thread t2 = new ThreadScanDirectory(directory2, directoryTwo);
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.err.println("Some problems with threads.");
            e.printStackTrace();
        }
        synchronizationDirectory();
    }

    /**
     * This method is called the method synchronizationDirectory(). It is designed to remove files and folders using multithreading.
     * @param file - the path to the file
     * @param map - ConcurrentHashMap from which the removal of this file
     */
    private void deleteAll(File file, ConcurrentHashMap<String, FileMetadata> map) {
        ArrayList<Thread> threads = new ArrayList<>();
        if(file.isDirectory()){
            for(File inFile : file.listFiles()){
                if(inFile.isDirectory() && inFile.listFiles().length > 20){
                    threads.add(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            deleteAll(inFile, map);
                        }
                    }));
                    threads.get(threads.size() - 1).start();
                } else {
                    deleteAll(inFile, map);
                }
            }
        }
        for(Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("Some threads problems.");
                e.printStackTrace();
            }
        }
        map.remove(file.getPath().substring(file.getPath().indexOf(File.separator), file.getPath().length()));
        file.delete();
    }

    /**
     * This method synchronizes files from ConcurrentHashMap1 and ConcurrentHashMap2 and in relation to a temporary file.
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     */
    private void synchronizationDirectory() throws IOException, ClassNotFoundException {

        if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName))) {

            if (readTMPFile) {
                ObjectInputStream fileStream = new ObjectInputStream(new FileInputStream(temporaryFileName));
                tmpMap.putAll((ConcurrentHashMap<String, FileMetadata>) fileStream.readObject());
            }
            Iterator<Map.Entry<String, FileMetadata>> iter = tmpMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, FileMetadata> entry = iter.next();
                String fileName = entry.getKey();
                if (!directoryOne.containsKey(fileName) && directoryTwo.containsKey(fileName) ||
                        directoryOne.containsKey(fileName) && !directoryTwo.containsKey(fileName)) {

                    if (directoryOne.containsKey(fileName)) {
                        File del = new File(directoryOne.get(fileName).getName());
                        if (del.exists()) {
                            deleteAll(del, tmpMap);
                            directoryOne.remove(fileName);
                        }
                    } else if (directoryTwo.containsKey(fileName)) {
                        File del = new File(directoryTwo.get(fileName).getName());
                        if (del.exists()) {
                            deleteAll(del, tmpMap);
                            directoryTwo.remove(fileName);
                        }
                    }
                    iter.remove();
                }
            }
        }
        swapDifferentFiles(directoryOne, directoryTwo);          // copy the necessary files from the first folder to the second
        swapDifferentFiles(directoryTwo, directoryOne);          // copy the necessary files from the second folder to the first

        if (tmpMap.size() > 0 && initialObject) {
            if (Files.exists(FileSystems.getDefault().getPath(temporaryFileName))) {;
                File delStream = new File(temporaryFileName);
                delStream.delete();
            }
            ObjectOutputStream recordInfFile = new ObjectOutputStream(new FileOutputStream(temporaryFileName));
            recordInfFile.writeObject(tmpMap);
        }
    }

    /**
     * This method synchronizes files from ConcurrentHashMap1 and ConcurrentHashMap2. If we found in one of their ConcurrentHashMap
     * file that is a directory, it creates an instance of the class ScanDirectory in a new thread and method begin() is called to him.
     * There recursion.
     * @param directoryObject - ConcurrentHashMap filled with files from first folder
     * @param directoryOther - ConcurrentHashMap filled with files from second folder
     * @throws IOException - existence of a file error
     * @throws ClassNotFoundException - error may occur when reading objects of class FileMetadata from the temporary file
     * (here happens when call a method begin() )
     */
    private void swapDifferentFiles(ConcurrentHashMap<String, FileMetadata> directoryObject,
                                                            ConcurrentHashMap<String, FileMetadata> directoryOther)
            throws IOException, ClassNotFoundException {

        ArrayList<Thread> threads = new ArrayList<>();
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
                threads.add(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ScanDirectory scDir = null;
                        try {
                            scDir = new ScanDirectory(tmpDirectory1, tmpDirectory2, temporaryFileName, tmpMap);
                            scDir.begin();
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }));
                threads.get(threads.size() - 1).start();
                tmpMap.put(fileName, directoryObject.get(fileName));
            } else if (directoryOther.containsKey(fileName)){
                    if(directoryOther.get(fileName).getTimeChange() < directoryObject.get(fileName).getTimeChange()) {
                        directoryOther.replace(fileName, directoryObject.get(fileName));
                        Path pathSourse = Paths.get(directoryObject.get(fileName).getName());     // the path to the file that we copied
                        Path pathDestination = Paths.get(initRoot + File.separator + fileName);   // path to the file that will be created as a result of copying (including the new file name)
                        Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);
                        tmpMap.put(fileName, directoryObject.get(fileName));
                    } else if(directoryOther.get(fileName).getTimeChange() == directoryObject.get(fileName).getTimeChange()) {
                        tmpMap.put(fileName, directoryObject.get(fileName));
                    }
            } else if (!directoryOther.containsKey(fileName)) {
                directoryOther.put(fileName, directoryObject.get(fileName));
                Path pathSource = Paths.get(directoryObject.get(fileName).getName());
                Path pathDestination = Paths.get(initRoot + File.separator + fileName);
                Files.copy(pathSource, pathDestination);
                tmpMap.put(fileName, directoryObject.get(fileName));
            }
        }
        for(Thread t : threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("Some problems with threads");
                e.printStackTrace();
            }
        }
    }

}
