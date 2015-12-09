package build.pluto.stamp;

import java.io.File;

/**
 * @author Sebastian Erdweg
 */
public class FileIgnoreStamper implements Stamper {

  private static final long serialVersionUID = 7688772212399111636L;

  private FileIgnoreStamper() {}
  public static final Stamper instance = new FileIgnoreStamper();
  
  @Override
  public Stamp stampOf(File p) {
    return FileIgnoreStamp.instance();
  }
  
  public static class FileIgnoreStamp implements Stamp {
    private static final long serialVersionUID = 3896019043720518541L;

    private static FileIgnoreStamp instance;
    public static FileIgnoreStamp instance() {
      if (instance == null)
        instance = new FileIgnoreStamp();
      return instance;
    }
    
    private FileIgnoreStamp() {
    }

    @Override
    public boolean equals(Stamp o) {
      return o instanceof FileIgnoreStamp;
    }

    @Override
    public Stamper getStamper() {
      return FileIgnoreStamper.instance;
    }
    
  }
}
