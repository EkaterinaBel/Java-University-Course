import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders. Actions are performed on the server.
 */
public class Server extends Thread {

    private File directory1;      // the source folder
    private final String temporaryFileName;
    private int port;
    private TreeMap<String, FileMetadata> directoryOne = new TreeMap<>();
    private TreeMap<String, FileMetadata> directoryTwo = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapForTmpFile = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChanges = new TreeMap<>();

    /**
     * This is a simple constructor that initializes the path to the folder and port.
     * @param directory1 - the way to the first folder
     * @param port - the way to the second folder
     * @throws IOException - existence of a file error
     */
    public Server(File directory1, int port) throws IOException {

        this.directory1 = directory1;
        this.port = port;
        this.temporaryFileName = "inform" + (directory1.getAbsolutePath() + directory1.getAbsolutePath()).hashCode() + ".tmp";
    }

    @Override
    public void run() {

        ObjectInputStream in = null;
        ObjectOutputStream oos = null;
        ObjectOutputStream recordInfFile = null;
        ServerSocket server = null;
        Socket fromClient = null;
        try {
            scan(directory1.getPath(), directory1, directoryOne);
            server = new ServerSocket(port);
            fromClient = server.accept();
            in = new ObjectInputStream(fromClient.getInputStream());
            directoryTwo = (TreeMap<String, FileMetadata>) in.readObject();
            synchronizationDirectory();
            oos = new ObjectOutputStream(fromClient.getOutputStream());
            oos.writeObject(mapContainsChanges);
            recordInfFile = new ObjectOutputStream(new FileOutputStream(temporaryFileName));
            recordInfFile.writeObject(mapForTmpFile);
            physicalSynchronization();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
                in.close();
                recordInfFile.close();
                fromClient.close();
                server.close();
            } catch (IOException ex) {}
        }
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
     * This method is called the method physicalSynchronization(). It is designed to remove files and folders using multithreading.
     * @param file - the path to the file
     */
    private void deleteAll(File file) {

        ArrayList<Thread> threads = new ArrayList<>();
        if(file.isDirectory()){
            for(File inFile : file.listFiles()){
                if(inFile.isDirectory() && inFile.listFiles().length > 20){
                    threads.add(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            deleteAll(inFile);
                        }
                    }));
                    threads.get(threads.size() - 1).start();
                } else {
                    deleteAll(inFile);
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
        file.delete();
    }

    /**
     * This method provides a physical synchronization source folders. It removes, copies, replaces files in folders.
     * @throws IOException - error associated with copying/replacement file
     */
    private void physicalSynchronization() throws IOException {

        String initRoot = directory1.getPath();
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
