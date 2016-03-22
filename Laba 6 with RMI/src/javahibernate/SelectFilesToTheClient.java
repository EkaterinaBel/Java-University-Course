package javahibernate;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.TreeMap;

/**
 * This class contains the method that is called remotely
 */
public class SelectFilesToTheClient extends UnicastRemoteObject implements FilesToTheClient{

    public SelectFilesToTheClient() throws RemoteException {}

    /**
     * This is method that generates a list of the files that need to change the list of files that relate to a client folder.
     * @param mapContainsChanges - Map with files that need to change
     * @param directory2 - client folder
     * @return Map with files that need change and that are contain on the client
     * @throws RemoteException may occur during the execution of a remote method call
     */
    @Override
    public TreeMap<String, FileMetadata> selectFiles(TreeMap<String, FileMetadata> mapContainsChanges, File directory2)
            throws RemoteException {

        TreeMap<String, FileMetadata> mapContainsChangesFromClient = new TreeMap<>();
        for (String o : mapContainsChanges.keySet()) {
            if (mapContainsChanges.get(o).getName().indexOf(directory2.getName()) == 0 && !mapContainsChanges.get(o).getTypeFile()
                    && !mapContainsChanges.get(o).getFileOperations()) {
                mapContainsChangesFromClient.put(o, mapContainsChanges.get(o));
            }
        }
        return mapContainsChangesFromClient;
    }
}
