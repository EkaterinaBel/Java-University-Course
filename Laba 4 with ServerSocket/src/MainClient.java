import java.io.File;
import java.io.IOException;

public class MainClient {

    public static void main(String[] args) throws ClassNotFoundException, IOException {

        ConfigurationRead xPathCaller = new ConfigurationRead("configuration.xml");

        File directory2 = new File(xPathCaller.getProperty("locationFolderTwo"));
        int port = Integer.parseInt(xPathCaller.getProperty("port"));
        String host = xPathCaller.getProperty("host");

        Thread c = new Client(directory2, host, port);
        c.start();
    }
}
