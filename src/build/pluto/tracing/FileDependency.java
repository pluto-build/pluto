package build.pluto.tracing;

import java.io.File;

public class FileDependency {
    FileAccessMode mode;
    File file;

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

   public FileDependency(FileAccessMode mode, File file) {
        this.mode = mode;
        this.file = file;
    }

    public String toString() {
        return file.toString() + " (" + mode + ")";
    }
}
