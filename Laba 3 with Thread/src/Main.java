import java.io.File;
import java.io.IOException;


public class Main {

    public static void main(String[] args) throws ClassNotFoundException {

        /*long time = System.currentTimeMillis();*/
        try{
            ConfigurationRead xPathCaller = new ConfigurationRead("configuration.xml");

            File directory1 = new File(xPathCaller.getProperty("locationFolderOne"));
            File directory2 = new File(xPathCaller.getProperty("locationFolderTwo"));
            ScanDirectory scDir = new ScanDirectory(directory1, directory2, "Временный_файл_test.tmp", true);
            scDir.begin();

            /*ObjectInputStream fileStream = new ObjectInputStream(new FileInputStream("Временный_файл_test.tmp"));
            ConcurrentHashMap<String, FileMetadata> tmpM = (ConcurrentHashMap<String, FileMetadata>) fileStream.readObject();
            for(Map.Entry<String, FileMetadata> fm : tmpM.entrySet()) {
                System.out.println(fm.getKey());
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }
       /* System.out.println("------------------------------------------------------");*/
       /* System.out.println("Timemillis: " + (System.currentTimeMillis() - time));*/
    }

}
