import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders. Actions are performed on the client.
 */
public class Client extends Thread{

    private File directory2;      // the source folder
    private int port;
    private final String host;
    private TreeMap<String, FileMetadata> directoryTwo = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChanges = new TreeMap<>();

    /**
     * This is a simple constructor that initializes the path to the folder, hostname and name port.
     * @param directory2 - the way to the folder
     * @param host - hostname
     * @param port - the port used
     * @throws IOException - existence of a file error
     */
    public Client(File directory2, String host, int port) throws IOException {

        this.directory2 = directory2;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        Socket fromServer = null;
        ObjectOutputStream oos = null;
        ObjectInputStream in = null;
        InetAddress host = null;
        try {
            host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            scan(directory2.getPath(), directory2, directoryTwo);
            fromServer = new Socket(host.getHostName(), port);
            oos = new ObjectOutputStream(fromServer.getOutputStream());
            oos.writeObject(directoryTwo);
            in = new ObjectInputStream(fromServer.getInputStream());
            mapContainsChanges = (TreeMap<String, FileMetadata>) in.readObject();
            physicalSynchronization();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fromServer.close();
                oos.close();
                in.close();
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
     * This method provides a physical synchronization source folders. It removes, copies, replaces files in folder.
     * @throws IOException - error associated with copying/replacement file
     */
    private void physicalSynchronization() throws IOException {

        String initRoot = directory2.getPath();
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
