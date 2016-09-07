package build.pluto.tracing;

import java.io.File;
import java.util.Date;

public class FileDependency {
    FileReadMode mode;
    File file;

    public FileReadMode getMode() {
        return mode;
    }

    public void setMode(FileReadMode mode) {
        this.mode = mode;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

   public FileDependency(FileReadMode mode, File file) {
        this.mode = mode;
        this.file = file;
    }

    public String toString() {
        return file.toString() + " (" + mode + ")";
    }
}
