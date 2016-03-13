import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws ClassNotFoundException, IOException {

        ConfigurationRead xPathCaller = new ConfigurationRead("configuration.xml");

        File directory1 = new File(xPathCaller.getProperty("locationFolderOne"));
        File directory2 = new File(xPathCaller.getProperty("locationFolderTwo"));
        int port = Integer.parseInt(xPathCaller.getProperty("port"));
        String host = xPathCaller.getProperty("host");

        Thread s = new Server(directory1, port);
        s.start();
        Thread c = new Client(directory2, host, port);
        c.start();
    }
}
