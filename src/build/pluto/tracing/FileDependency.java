package build.pluto.tracing;

import java.io.File;
import java.util.Date;

public class FileDependency {
    FileAccessMode mode;
    File file;
    Date date;

    public Date getDate() { return date; }

    public void setDate(Date date) { this.date = date; }

    public FileAccessMode getMode() {
        return mode;
    }

    public void setMode(FileAccessMode mode) {
        this.mode = mode;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public FileDependency(FileAccessMode mode, File file, Date date) {
        this.mode = mode;
        this.file = file;
        this.date = date;
    }

    public String toString() {
        return date.toString() + ": " + file.toString() + " (" + mode + ")";
    }
}
