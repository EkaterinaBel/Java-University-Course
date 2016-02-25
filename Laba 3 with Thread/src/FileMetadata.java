import java.io.Serializable;

/**
 * This is the class that keeps information about the files: name, creation time (changing) and file type.
 */
public class FileMetadata implements Serializable{

    private String name;
    private long timeChange;
    private boolean typeFile; //  false - regular file, true - directory

    /**
     * This is a simple constructor that initializes the file name, time of change and type.
     * @param name - file name
     * @param timeChange - file modification time
     * @param typeFile - file type
     */
    public FileMetadata(String name, long timeChange, boolean typeFile) {

        this.name = name;
        this.timeChange = timeChange;
        this.typeFile = typeFile;
    }

    /**
     * A method that returns the file modification time.
     * @return modification time
     */
    public long getTimeChange() {
        return timeChange;
    }

    /**
     * A method that returns the file name.
     * @return file name
     */
    public String getName() {
        return name;
    }

    /**
     * A method that returns the file type.
     * @return file type
     */
    public boolean getTypeFile() {
        return typeFile;
    }

    /**
     * A method that returns the file name, modification time and file type.
     * @return file name, modification time and file type
     */
    public String toString() {
        return name + " " + Long.toString(timeChange) + " " + (typeFile ? "directory" :  "file");
    }
}
