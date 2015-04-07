package build.pluto.xattr;

import java.io.IOException;
import java.util.Map;

import org.sugarj.common.path.Path;

public interface XattrStrategy {
  public void setXattr(Path p, String key, String value) throws IOException;
  public void removeXattr(Path p, String key) throws IOException;
  public String getXattr(Path p, String key) throws IOException;

  public Map<String, String> getAllXattr(Path p) throws IOException;
}