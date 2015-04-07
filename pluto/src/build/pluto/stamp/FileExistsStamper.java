package build.pluto.stamp;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

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
  public Stamp stampOf(Path p) {
    return new ValueStamp<>(this, FileCommands.exists(p));
  }
}
