import java.io.File;
import java.io.IOException;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws ClassNotFoundException {

        System.out.println("Введите директории files1 и files2");
        Scanner sc = new Scanner(System.in);
        String folder1 = sc.nextLine();
        String folder2 = sc.nextLine();

        try{
            File directory1 = new File(folder1);
            File directory2 = new File(folder2);
            ScanDirectory scDir = new ScanDirectory(directory1, directory2, "Временный_файл.tmp", true);
            scDir.begin();

            /*ObjectInputStream fileStream = new ObjectInputStream(new FileInputStream("Временный_файл.tmp"));
            HashMap<String, FileMetadata> tmpM = (HashMap<String, FileMetadata>) fileStream.readObject();
            for(Map.Entry<String, FileMetadata> fm : tmpM.entrySet()) {
                System.out.println(fm.getKey());
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
