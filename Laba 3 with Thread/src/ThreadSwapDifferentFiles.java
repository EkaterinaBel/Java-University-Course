import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ThreadSwapDifferentFiles extends Thread{

    ConcurrentHashMap<String, FileMetadata> tmpHashMapOne;
    ConcurrentHashMap<String, FileMetadata> tmpHashMapTwo;
    ConcurrentHashMap<String, FileMetadata> tmpDirectoryMap;
    String temporaryFileName;
    String initRoot1;
    String initRoot2;

    public ThreadSwapDifferentFiles(ConcurrentHashMap<String, FileMetadata> tmpHashMapOne, ConcurrentHashMap<String, FileMetadata> tmpHashMapTwo,
                                    ConcurrentHashMap<String, FileMetadata> tmpDirectoryMap, String temporaryFileName, String initRoot1, String initRoot2) {

        this.tmpDirectoryMap = tmpDirectoryMap;
        this.tmpHashMapOne = tmpHashMapOne;
        this.tmpHashMapTwo = tmpHashMapTwo;
        this.temporaryFileName = temporaryFileName;
        this.initRoot1 = initRoot1;
        this.initRoot2 = initRoot2;
        start();
    }

    public void run() {
        try {
           // tmpDirectoryHashMap.putAll(scan(tmpDirectory.getPath(), tmpDirectory, tmpDirectoryHashMap));
            swapDifferentFiles(tmpHashMapOne, tmpHashMapTwo);                        // copy the necessary files from the first folder to the second
            swapDifferentFiles(tmpHashMapTwo, tmpHashMapOne);                        // copy the necessary files from the second folder to the first
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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
    private void swapDifferentFiles(ConcurrentHashMap<String, FileMetadata> directoryObject,
                                                                                  ConcurrentHashMap<String, FileMetadata> directoryOther)
            throws IOException, ClassNotFoundException {

        ArrayList<Thread> threads = new ArrayList<>();
        String initRoot;
        if (directoryObject == tmpHashMapOne) {
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
                /*threads.add(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ScanDirectory scDir = null;
                        try {
                            scDir = new ScanDirectory(tmpDirectory1, tmpDirectory2, temporaryFileName, tmpDirectoryMap);
                            scDir.begin();
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }));*/
                threads.get(threads.size() - 1).start();
                tmpDirectoryMap.put(fileName, directoryObject.get(fileName));
            } else if (directoryOther.containsKey(fileName)){
                if(directoryOther.get(fileName).getTimeChange() < directoryObject.get(fileName).getTimeChange()) {
                    directoryOther.replace(fileName, directoryObject.get(fileName));
                    Path pathSourse = Paths.get(directoryObject.get(fileName).getName());     // the path to the file that we copied
                    Path pathDestination = Paths.get(initRoot + File.separator + fileName);   // path to the file that will be created as a result of copying (including the new file name)
                    Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);
                    tmpDirectoryMap.put(fileName, directoryObject.get(fileName));
                } else if(directoryOther.get(fileName).getTimeChange() == directoryObject.get(fileName).getTimeChange()) {
                    tmpDirectoryMap.put(fileName, directoryObject.get(fileName));
                }
            } else if (!directoryOther.containsKey(fileName)) {
                directoryOther.put(fileName, directoryObject.get(fileName));
                Path pathSource = Paths.get(directoryObject.get(fileName).getName());
                Path pathDestination = Paths.get(initRoot + File.separator + fileName);
                Files.copy(pathSource, pathDestination);
                tmpDirectoryMap.put(fileName, directoryObject.get(fileName));
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



        /*Thread t1 = new ThreadSwapDifferentFiles(directoryOne, directoryTwo, tmpMap, temporaryFileName, initRoot1, initRoot2);
        Thread t2 = new ThreadSwapDifferentFiles(directoryTwo, directoryOne, tmpMap, temporaryFileName, initRoot1, initRoot2);
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.err.println("Some problems with threads.");
            e.printStackTrace();
        }*/

}
