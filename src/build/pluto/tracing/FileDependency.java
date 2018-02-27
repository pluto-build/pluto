package build.pluto.tracing;

import java.io.File;
import java.util.Date;

public class FileDependency {
    FileAccessMode mode;
    boolean fileExisted;
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

    public boolean getFileExisted() {
        return fileExisted;
    }

    public void setFileExisted(boolean fileExisted) {
        this.fileExisted = fileExisted;
    }

    public FileDependency(FileAccessMode mode, File file) {
        this.mode = mode;
        this.file = file;
        this.fileExisted = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileDependency that = (FileDependency) o;

        //if (fileExisted != that.fileExisted) return false;
        if (mode != that.mode) return false;
        return file != null ? file.equals(that.file) : that.file == null;
    }

    @Override
    public int hashCode() {
        int result = mode.hashCode();
        result = 31 * result + (file != null ? file.hashCode() : 0);
        return result;
    }

    public String toString() {
        String notExisting = "";
        //if (!fileExisted)
        //    notExisting = " (X)";
        return file.toString() + " (" + mode + notExisting + ")";
    }
}
