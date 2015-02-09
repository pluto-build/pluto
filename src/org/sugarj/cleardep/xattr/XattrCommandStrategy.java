package org.sugarj.cleardep.xattr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.CommandExecution;
import org.sugarj.common.path.Path;

public class XattrCommandStrategy implements XattrStrategy {

  CommandExecution exec = new CommandExecution(false);
  
  @Override
  public void setXattr(Path p, String key, String value) throws IOException {
    exec.execute("xattr", "-w", Xattr.PREFIX + ":" + key, value, p.getAbsolutePath());
  }

  @Override
  public String getXattr(Path p, String key) throws IOException {
    String[][] out = exec.execute("xattr", "-p", Xattr.PREFIX + ":" + key, p.getAbsolutePath());
    if (out[0].length > 0)
      return out[0][0];
    return null;
  }

  @Override
  public Map<String, String> getAllXattr(Path p) throws IOException {
    String[][] out = exec.execute("xattr", "-l", p.getAbsolutePath());
    
    Map<String, String> attrs = new HashMap<>();
    for (String line : out[0]) {
      int split = line.indexOf(": ");
      String key = line.substring(0, split);
      String val = line.substring(split + ": ".length());
      attrs.put(key, val);
    }
    return attrs;
  }

}
