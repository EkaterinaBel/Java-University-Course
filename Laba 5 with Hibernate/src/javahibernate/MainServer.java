package javahibernate;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MainServer {

    public static void main(String[] args) throws ClassNotFoundException, IOException {

        ConfigurationRead xPathCaller = new ConfigurationRead("configuration.xml");

        File directory1 = new File(xPathCaller.getProperty("locationFolderOne"));
        int port = Integer.parseInt(xPathCaller.getProperty("port"));

        Locale.setDefault(Locale.ENGLISH);

        Thread s = new Server(directory1, port);
        s.start();
    }
}
