package build.pluto.xattr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class XattrAttributeViewStrategy implements XattrStrategy {

  @Override
  public void setXattr(File p, String key, String value) throws IOException {
    Files.setAttribute(p.toPath(), "user:" + Xattr.PREFIX + ":" + key, value);
  }
  
  @Override
  public void removeXattr(File p, String key) throws IOException {
    Files.setAttribute(p.toPath(), "user:" + Xattr.PREFIX + ":" + key, null);
  }

  @Override
  public String getXattr(File p, String key) throws IOException {
    try {
      Object val = Files.getAttribute(p.toPath(), "user:" + Xattr.PREFIX + ":" + key);
      if (val == null || !(val instanceof String))
        return null;
      return (String) val;
    } catch (UnsupportedOperationException e) {
      return null;
    }
  }

  @Override
  public Map<String, String> getAllXattr(File p) throws IOException {
    Map<String, Object> vals = Files.readAttributes(p.toPath(), Xattr.PREFIX + ":*");
    Map<String, String> attrs = new HashMap<>();
    for (Entry<String, Object> e : vals.entrySet()) {
      if (e.getValue() != null && e.getValue() instanceof String)
        attrs.put(e.getKey(), (String) e.getValue());
    }
    return attrs;
  }

}
