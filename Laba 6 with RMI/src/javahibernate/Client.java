package javahibernate;

import javafx.util.Pair;

import java.io.*;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders. Actions are performed on the client.
 */
public class Client extends Thread implements Serializable{

    private File directory2;      // the source folder
    private int port;
    private final String host;
    private final String login;
    private final String password;
    private TreeMap<String, FileMetadata> directoryTwo = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChanges = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChangesFromServer = new TreeMap<>();

    /**
     * This is a simple constructor that initializes the path to the folder, hostname and name port.
     * @param directory2 - the way to the folder
     * @param host - hostname
     * @param port - the port used
     * @throws IOException - existence of a file error
     */
    public Client(File directory2, String host, int port, String login, String password) throws IOException {

        this.directory2 = directory2;
        this.host = host;
        this.port = port;
        this.login = login;
        this.password = password;
    }

    @Override
    public void run() {

        Socket fromServer = null;
        ObjectOutputStream oos = null;
        ObjectInputStream in = null;
        try {
            fromServer = new Socket(host, port);
            oos = new ObjectOutputStream(fromServer.getOutputStream());
            Pair<String, String> loginAndPas = new Pair<>(login, password);
            oos.writeObject(loginAndPas);

            in = new ObjectInputStream(fromServer.getInputStream());
            if (in.readObject().equals("Connect")) {

                scan(directory2.getPath(), directory2, directoryTwo);
                oos.writeObject(directoryTwo);
                if (!in.readObject().equals("size mapContainsChanges = 0")) {

                    mapContainsChanges = (TreeMap<String, FileMetadata>) in.readObject();
                    int portForRegistry = (int) in.readObject();
                    physicalSynchronization();
                    String objectName = "rmi://localhost:" + portForRegistry + "/SelectFilesToTheClient";
                    FilesToTheClient filClient = null;
                    try {
                        filClient = (FilesToTheClient) Naming.lookup(objectName);
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                    }
                    TreeMap<String, FileMetadata> mapContainsChangesFromClient = filClient.selectFiles(mapContainsChanges, directory2);

                    if (mapContainsChangesFromClient.size() != 0) {
                        oos.writeObject("size mapContainsChangesFromClient != 0");
                        oos.writeObject(mapContainsChangesFromClient);
                        for (Map.Entry<String, FileMetadata> entry : mapContainsChangesFromClient.entrySet()) {
                            File file = new File(entry.getValue().getName());
                            byte[] fileArray = new byte[(int) file.length()];
                            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                            bis.read(fileArray, 0, fileArray.length);
                            Pair<String, byte[]> pair = new Pair<>(entry.getKey(), fileArray);
                            oos.writeObject(pair);
                        }
                    } else if (mapContainsChangesFromClient.size() == 0) {
                        oos.writeObject("size mapContainsChangesFromClient = 0");
                    }

                    if (in.readObject().equals("size mapContainsChangesFromServer != 0")) {
                        mapContainsChangesFromServer = (TreeMap<String, FileMetadata>) in.readObject();
                        int k = 0;
                        try {
                            while (k != mapContainsChangesFromServer.size()) {
                                Pair<String, byte[]> pair = (Pair<String, byte[]>) in.readObject();
                                File newFile = new File("fromServer.tmp");
                                newFile.createNewFile();
                                FileOutputStream fos = new FileOutputStream(newFile);
                                fos.write(pair.getValue(), 0, pair.getValue().length);
                                newFile.setLastModified(mapContainsChangesFromServer.get(pair.getKey()).getTimeChange());

                                Path pathSourse = Paths.get(newFile.getName());
                                Path pathDestination = Paths.get(directory2 + File.separator + pair.getKey());   // path to the file that will be created as a result of copying (including the new file name)
                                Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);
                                k++;
                            }
                        } catch (EOFException ignored) {
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fromServer != null) {
                    fromServer.close();
                }
                if (oos != null) {
                    oos.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
            }
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
     * This method provides a partially physical synchronization source folders. It removes files in folder or creating new folders.
     */
    private void physicalSynchronization() {

        String initRoot = directory2.getPath();
        for(String fileName: mapContainsChanges.keySet()) {

            if (mapContainsChanges.get(fileName).getFileOperations()) {
                if (Files.exists(FileSystems.getDefault().getPath(initRoot + File.separator + fileName))) {
                    deleteAll(new File(initRoot + File.separator + fileName));
                }
            } else if (mapContainsChanges.get(fileName).getTypeFile()){
                File tmpDirectory = new File(initRoot + File.separator + fileName);
                tmpDirectory.mkdir();
            }
        }
    }

}
