package build.pluto.tracing;

import java.io.File;
import java.util.Date;

public class FileDependency {
    FileAccessMode mode;
    boolean fileExisted;
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

    public boolean getFileExisted() {
        return fileExisted;
    }

    public void setFileExisted(boolean fileExisted) {
        this.fileExisted = fileExisted;
    }

    public FileDependency(FileAccessMode mode, File file, Date date) {
        this.mode = mode;
        this.file = file;
        this.date = date;
        this.fileExisted = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileDependency that = (FileDependency) o;

        if (getFileExisted() != that.getFileExisted()) return false;
        if (getMode() != that.getMode()) return false;
        if (getFile() != null ? !getFile().equals(that.getFile()) : that.getFile() != null) return false;
        return getDate() != null ? getDate().equals(that.getDate()) : that.getDate() == null;

    }

    @Override
    public int hashCode() {
        int result = getMode() != null ? getMode().hashCode() : 0;
        result = 31 * result + (getFileExisted() ? 1 : 0);
        result = 31 * result + (getFile() != null ? getFile().hashCode() : 0);
        result = 31 * result + (getDate() != null ? getDate().hashCode() : 0);
        return result;
    }

    public String toString() {
        String notExisting = "";
        if (!fileExisted)
            notExisting = " (X)";
        return file.toString() + " (" + mode + notExisting + ")";
    }
}
