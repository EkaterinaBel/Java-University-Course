package javahibernate;

import javafx.util.Pair;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final String login;
    private final String password;
    private TreeMap<String, FileMetadata> directoryTwo = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChanges = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChangesFromServer = new TreeMap<>();

    public long sizeMapContainsChangesUI;            // size all files in mapContainsChanges (for UI)
    public volatile boolean beginClientUI;           // flag that determines what can begin the synchronization (for UI)
    public volatile boolean endClientUI;             // flag that determines that the client finished (for UI)
    public volatile boolean endServerUI;             // flag that determines that the server finished (for UI)
    public volatile long sizeClientUI;               // size files in mapContainsChanges with client (for UI)
    public volatile long sizeServerUI;               // size files in mapContainsChanges with server (for UI)
    public volatile String nameFileClientUI = " ";   // name of the file from mapContainsChanges with which
    public volatile String nameFileServerUI = " ";             // the client/server operates at the moment (for UI)
    public volatile boolean startUI;                 // flag which determines that the client started (for UI)
    public volatile boolean startUI2;                // flag which determines that the size mapContainsChanges > 0 (for UI)
    public volatile String nameSourceFolderServerUI; // name of the source folder for synchronization on the server (for UI)

    /**
     * This is a simple constructor that initializes the path to the folder, hostname and name port.
     * @param directory2 - the way to the folder
     * @param port - the port used
     * @param login - user login
     * @param password - user password
     * @throws IOException - existence of a file error
     */
    public Client(File directory2, int port, String login, String password) throws IOException {

        this.directory2 = directory2;
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
            String host = InetAddress.getLocalHost().getHostName();
            fromServer = new Socket(host, port);
            oos = new ObjectOutputStream(fromServer.getOutputStream());
            Pair<String, String> loginAndPas = new Pair<>(login, password);
            oos.writeObject(loginAndPas);

            in = new ObjectInputStream(fromServer.getInputStream());
            if (in.readObject().equals("Connect")) {

                startUI = true;
                scan(directory2.getPath(), directory2, directoryTwo);
                oos.writeObject(directoryTwo);
                if (!in.readObject().equals("size mapContainsChanges = 0")) {

                    startUI2 = true;
                    mapContainsChanges = (TreeMap<String, FileMetadata>) in.readObject();
                    nameSourceFolderServerUI = (String) in.readObject();
                    for (String o:mapContainsChanges.keySet()) {
                        sizeMapContainsChangesUI = sizeMapContainsChangesUI + mapContainsChanges.get(o).getSizeFile();
                    }
                    beginClientUI = true;
                    physicalSynchronization();

                    TreeMap<String, FileMetadata> mapContainsChangesFromClient = new TreeMap<>();
                    for (String o : mapContainsChanges.keySet()) {
                        if (mapContainsChanges.get(o).getName().indexOf(directory2.getPath()) == 0 &&
                            !mapContainsChanges.get(o).getTypeFile() && !mapContainsChanges.get(o).getFileOperations()) {
                            mapContainsChangesFromClient.put(o, mapContainsChanges.get(o));
                        }
                    }
                    nameFileServerUI = (String) in.readObject();
                    sizeServerUI =  (long) in.readObject();
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
                            nameFileServerUI = (String) in.readObject();
                            sizeServerUI = (long) in.readObject();
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
                                Path pathDestination = Paths.get(directory2 + File.separator + pair.getKey());
                                Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);

                                File fileUI = new File(directory2 + File.separator + pair.getKey());
                                sizeClientUI = sizeClientUI + fileUI.length();
                                nameFileClientUI = fileUI.getPath();
                                k++;
                            }
                        } catch (EOFException ignored) {}
                    }
                }
                in = new ObjectInputStream(fromServer.getInputStream());
                endServerUI = (boolean) in.readObject();
            }
            endClientUI = true;
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
            long size =  f.isDirectory() ? 0 : f.length();
            directoryObject.put(f.getPath().replaceFirst(root.replace("\\", "\\\\"), ""),
                    new FileMetadata(f.getPath(), f.lastModified(), f.isDirectory(), size));
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
        sizeClientUI = sizeClientUI + file.length();
        nameFileClientUI = file.getPath();
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
                nameFileClientUI = tmpDirectory.getPath();
                tmpDirectory.mkdir();
            }
        }
    }

}
