package javahibernate;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.TreeMap;

/**
 * This is an interface that contains a method that generates a list of the files that need to change the list of files
 * that relate to a client folder.
 */
public interface FilesToTheClient extends Remote{

    public TreeMap<String, FileMetadata> selectFiles(TreeMap<String, FileMetadata> mapContainsChanges, File directory2)
            throws RemoteException;

}
