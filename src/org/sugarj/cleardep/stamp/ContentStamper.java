package org.sugarj.cleardep.stamp;

import java.io.IOException;
import java.util.Arrays;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public class ContentStamper implements Stamper, ModuleStamper {

  private static final long serialVersionUID = 7688772212399111636L;

  private ContentStamper() {}
  public static final Stamper instance = new ContentStamper();
  public static final ModuleStamper minstance = new ContentStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public ContentStamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new ContentStamp(new byte[0]);
    
    try {
      return new ContentStamp(FileCommands.readFileAsByteArray(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ContentStamp(new byte[0]);
    }
  }

  public ContentStamp stampOf(CompilationUnit m) {
    if (!m.isPersisted())
      throw new IllegalArgumentException("Cannot compute stamp of non-persisted compilation unit " + m);

    return stampOf(m.getPersistentPath());
  }
  
  public static class ContentStamp implements Stamp, ModuleStamp {

    private static final long serialVersionUID = 7535020621495360152L;
    
    private final byte[] value;

    public ContentStamp(byte[] value) {
      this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o instanceof ContentStamp) {
        byte[] ovalue = ((ContentStamp) o).value;
        return ovalue == null && value == null || Arrays.equals(ovalue, value);
      }
      if (o instanceof ContentStamp) {
        byte[] ovalue = ((ContentStamp) o).value;
        return ovalue == null && value == null || Arrays.equals(ovalue, value);
      }
      return false;
    }
    
    @Override
    public Stamper getStamper() {
      return ContentStamper.instance;
    }
    
    @Override
    public ModuleStamper getModuleStamper() {
      return ContentStamper.minstance;
    }
    
    @Override
    public String toString() {
      return "ContentHash(byte[" + value.length + "])";
    }
  }
}
