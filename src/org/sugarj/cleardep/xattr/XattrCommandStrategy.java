package org.sugarj.cleardep.xattr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.CommandExecution;
import org.sugarj.common.path.Path;

public class XattrCommandStrategy implements XattrStrategy {

  CommandExecution exec = new CommandExecution(true);
  
  @Override
  public void setXattr(Path p, String key, String value) throws IOException {
    try {
      exec.execute("xattr", "-w", Xattr.PREFIX + ":" + key, value, p.getAbsolutePath());
    } catch (CommandExecution.ExecutionError e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getXattr(Path p, String key) throws IOException {
    try {
      String[][] out = exec.execute("xattr", "-p", Xattr.PREFIX + ":" + key, p.getAbsolutePath());
      if (out[0].length > 0)
        return out[0][0];
      return null;
    } catch (CommandExecution.ExecutionError e) {
      return null;
    }
  }

  @Override
  public Map<String, String> getAllXattr(Path p) throws IOException {
    try {
      String[][] out = exec.execute("xattr", "-l", p.getAbsolutePath());
      
      Map<String, String> attrs = new HashMap<>();
      for (String line : out[0]) {
        int split = line.indexOf(": ");
        String key = line.substring(0, split);
        String val = line.substring(split + ": ".length());
        attrs.put(key, val);
      }
      return attrs;
    } catch (CommandExecution.ExecutionError e) {
      return null;
    }
  }

}
