package javahibernate;

import javafx.util.Pair;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * This class is designed to synchronize files from two folders. Actions are performed on the server.
 */
public class Server extends Thread implements Serializable{

    private File directory1;      // the source folder
    private final String temporaryFileName;
    private int port;
    private TreeMap<String, FileMetadata> directoryOne = new TreeMap<>();
    private TreeMap<String, FileMetadata> directoryTwo = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapForTmpFile = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChanges = new TreeMap<>();
    private TreeMap<String, FileMetadata> mapContainsChangesFromClient = new TreeMap<>();

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
        ServerSocket server = null;
        Socket fromClient = null;
        try {
            server = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                fromClient = server.accept();
                in = new ObjectInputStream(fromClient.getInputStream());
                Pair<String, String> loginAndPas = (Pair<String, String>) in.readObject();
                String login = loginAndPas.getKey();
                String password = loginAndPas.getValue();
                boolean bol = authorizationUser(login, password);
                oos = new ObjectOutputStream(fromClient.getOutputStream());
                if (bol) {

                    oos.writeObject("Connect");
                    scan(directory1.getPath(), directory1, directoryOne);
                    directoryTwo = (TreeMap<String, FileMetadata>) in.readObject();
                    synchronizationDirectory();

                    if (mapContainsChanges.size() != 0) {

                        oos.writeObject("continue");
                        oos.writeObject(mapContainsChanges);
                        physicalSynchronization();
                        if (in.readObject().equals("size mapContainsChangesFromClient != 0")) {
                            mapContainsChangesFromClient = (TreeMap<String, FileMetadata>) in.readObject();
                            int k = 0;
                            try {
                                while (k != mapContainsChangesFromClient.size()) {
                                    Pair<String, byte[]> pair = (Pair<String, byte[]>) in.readObject();
                                    File newFile = new File("fromClient.tmp");
                                    newFile.createNewFile();
                                    FileOutputStream fos = new FileOutputStream(newFile);
                                    fos.write(pair.getValue(), 0, pair.getValue().length);
                                    newFile.setLastModified(mapContainsChangesFromClient.get(pair.getKey()).getTimeChange());

                                    Path pathSourse = Paths.get(newFile.getName());
                                    Path pathDestination = Paths.get(directory1 + File.separator + pair.getKey());   // path to the file that will be created as a result of copying (including the new file name)
                                    Files.copy(pathSourse, pathDestination, REPLACE_EXISTING);
                                    k++;
                                }
                            } catch (EOFException ignored) {
                            }
                        }
                        TreeMap<String, FileMetadata> mapContainsChangesFromServer = new TreeMap<>();
                        for (String o : mapContainsChanges.keySet()) {
                            if (mapContainsChanges.get(o).getName().indexOf(directory1.getName()) == 0 && !mapContainsChanges.get(o).getTypeFile()
                                    && !mapContainsChanges.get(o).getFileOperations()) {
                                mapContainsChangesFromServer.put(o, mapContainsChanges.get(o));
                            }
                        }
                        if (mapContainsChangesFromServer.size() != 0) {
                            oos.writeObject("size mapContainsChangesFromServer != 0");
                            oos.writeObject(mapContainsChangesFromServer);
                            for (Map.Entry<String, FileMetadata> entry : mapContainsChangesFromServer.entrySet()) {

                                File file = new File(entry.getValue().getName());
                                byte[] fileArray = new byte[(int) file.length()];
                                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                                bis.read(fileArray, 0, fileArray.length);
                                Pair<String, byte[]> pair = new Pair<>(entry.getKey(), fileArray);
                                oos.writeObject(pair);
                            }
                        } else if (mapContainsChangesFromServer.size() == 0) {
                            oos.writeObject("size mapContainsChangesFromServer = 0");
                        }
                    } else {
                        oos = new ObjectOutputStream(fromClient.getOutputStream());
                        oos.writeObject("size mapContainsChanges = 0");
                    }
                    oos = new ObjectOutputStream(new FileOutputStream(temporaryFileName));
                    oos.writeObject(mapForTmpFile);
                } else {
                    oos.writeObject("Not connect");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                if (oos != null) {
                    oos.close();
                }
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
     * This method provides a partially physical synchronization source folders. It removes files in folder or creating new folders.
     */
    private void physicalSynchronization() {

        String initRoot = directory1.getPath();
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

    /**
     * This method checks the existence of the user in a database table.
     * @param log - login
     * @param pas - password
     * @return true - if the user is found, false - otherwise
     */
    private boolean authorizationUser(String log, String pas) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JavaHibernatePU");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Query queryResult = em.createQuery("from Users where login = '"+ log + "' and password = '" + pas + "'");
        List<Users> allUsers = queryResult.getResultList();
        em.getTransaction().commit();
        try {
            if (allUsers.get(0).getId() != 0) {
                return true;
            }
        } catch (IndexOutOfBoundsException ignore) {}
        return false;
    }

}
