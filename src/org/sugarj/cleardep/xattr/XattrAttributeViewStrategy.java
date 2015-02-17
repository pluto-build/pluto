package org.sugarj.cleardep.xattr;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sugarj.common.path.Path;

public class XattrAttributeViewStrategy implements XattrStrategy {

  @Override
  public void setXattr(Path p, String key, String value) throws IOException {
    Files.setAttribute(p.getFile().toPath(), "user:" + Xattr.PREFIX + ":" + key, value);
  }

  @Override
  public String getXattr(Path p, String key) throws IOException {
    try {
      Object val = Files.getAttribute(p.getFile().toPath(), "user:" + Xattr.PREFIX + ":" + key);
      if (val == null || !(val instanceof String))
        return null;
      return (String) val;
    } catch (UnsupportedOperationException e) {
      return null;
    }
  }

  @Override
  public Map<String, String> getAllXattr(Path p) throws IOException {
    Map<String, Object> vals = Files.readAttributes(p.getFile().toPath(), Xattr.PREFIX + ":*");
    Map<String, String> attrs = new HashMap<>();
    for (Entry<String, Object> e : vals.entrySet()) {
      if (e.getValue() != null && e.getValue() instanceof String)
        attrs.put(e.getKey(), (String) e.getValue());
    }
    return attrs;
  }

}
