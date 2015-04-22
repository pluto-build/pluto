package build.pluto.xattr;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.Exec;

public class XattrCommandStrategy implements XattrStrategy {

  Exec exec = new Exec(true);
  
  @Override
  public void setXattr(File p, String key, String value) throws IOException {
    try {
      exec.exec("xattr", "-w", Xattr.PREFIX + ":" + key, value, p.getAbsolutePath());
    } catch (Exec.ExecutionError e) {
      throw new IOException(e);
    }
  }
  
  @Override
  public void removeXattr(File p, String key) throws IOException {
    try {
      exec.exec("xattr", "-d", Xattr.PREFIX + ":" + key, p.getAbsolutePath());
    } catch (Exec.ExecutionError e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getXattr(File p, String key) throws IOException {
    try {
      Exec.ExecutionResult out = exec.exec("xattr", "-p", Xattr.PREFIX + ":" + key, p.getAbsolutePath());
      return out.outMsgs[0];
    } catch (Exec.ExecutionError e) {
      return null;
    }
  }

  @Override
  public Map<String, String> getAllXattr(File p) throws IOException {
    try {
      Exec.ExecutionResult out = exec.exec("xattr", "-l", p.getAbsolutePath());
      
      Map<String, String> attrs = new HashMap<>();
      for (String line : out.outMsgs) {
        int split = line.indexOf(": ");
        String key = line.substring(0, split);
        String val = line.substring(split + ": ".length());
        attrs.put(key, val);
      }
      return attrs;
    } catch (Exec.ExecutionError e) {
      return null;
    }
  }

}
