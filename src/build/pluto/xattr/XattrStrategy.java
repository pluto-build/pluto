package build.pluto.xattr;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface XattrStrategy {
  public void setXattr(File p, String key, String value) throws IOException;
  public void removeXattr(File p, String key) throws IOException;
  public String getXattr(File p, String key) throws IOException;

  public Map<String, String> getAllXattr(File p) throws IOException;
}