package build.pluto.stamp;

import java.io.File;

/**
 * @author Sebastian Erdweg
 *
 */
public class FileExistsStamper implements Stamper {

  private static final long serialVersionUID = 8242859577253542194L;

  private FileExistsStamper() {}
  public static final Stamper instance = new FileExistsStamper();
  
  /**
   * @see build.pluto.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public Stamp stampOf(File p) {
    return new ValueStamp<>(this, p.exists());
  }
}
