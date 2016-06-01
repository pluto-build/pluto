package build.pluto.xattr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import build.pluto.BuildUnit;

public class Xattr {
  private static final char SEPC = 0;
  private static final String SEP = new String(new char[]{SEPC});
  
  public final XattrStrategy strategy;
  
  public Xattr(XattrStrategy strategy) {
    this.strategy = strategy;
  }
 
  public void addGenBy(File p, BuildUnit<?> unit) throws IOException {
    String path = unit.getPersistentPath().toPath().toAbsolutePath().toString();
    String oldVal = strategy.getXattr(p, "genBy");
    if (oldVal == null || oldVal.isEmpty()) // size oldVal == 0
      strategy.setXattr(p, "genBy", path);
    else if (oldVal.charAt(0) != SEPC) { // size oldVal == 1
      if (!oldVal.equals(path))
          strategy.setXattr(p, "genBy", SEP + oldVal + SEP + path);
    }
    else { // size oldVal > 1
      String wrapped = oldVal.concat(SEP);
      if (!wrapped.contains(SEP + path + SEP))
        strategy.setXattr(p, "genBy", oldVal + SEP + path);
    }
  }
  
  public void removeGenBy(File p, BuildUnit<?> unit) throws IOException {
    String path = unit.getPersistentPath().toPath().toAbsolutePath().toString();

    String oldVal = strategy.getXattr(p, "genBy");
    if (oldVal == null || oldVal.isEmpty()) // size oldVal == 0
      ; // nothing
    else if (oldVal.charAt(0) != SEPC) { // size oldVal == 1
      if (oldVal.equals(path))
        strategy.removeXattr(p, "genBy");
      else
        ; // nothing
    }
    else { // size oldVal > 1
      String[] paths = oldVal.substring(1).split(SEP);
      StringBuilder newValB = new StringBuilder();
      int count = 0; // number of new paths
      for (int i = 0; i < paths.length; i++)
        if (!paths[i].equals(path)) {
          count++;
          newValB.append(SEP).append(paths[i]);
        }
      if (count == 0)
        strategy.removeXattr(p, "genBy");
      else if (count == 1) {
        // skip leading SEP
        String newVal = newValB.toString().substring(1);
        strategy.setXattr(p, "genBy", newVal);
      }
      else {
        String newVal = newValB.toString();
        strategy.setXattr(p, "genBy", newVal);
      }
    }
  }
  
  public File[] getGenBy(File p) throws IOException {
    String val = strategy.getXattr(p, "genBy");
    if (val == null)
      return null;
    
    if (val.charAt(0) != SEPC)
      return new File[]{new File(val)};
    
    String[] paths = val.substring(1).split(SEP);
    File[] files = new File[paths.length];
    for (int i = 0; i < paths.length; i++)
      files[i] = new File(paths[i]);
    return files;
  }
  
  public void setSynFrom(File p, Iterable<File> paths) throws IOException {
    StringBuilder builder = new StringBuilder();
    for (Iterator<File> it = paths.iterator(); it.hasNext(); ) {
      builder.append(it.next());
      if (it.hasNext())
        builder.append(File.pathSeparatorChar);
    }
    strategy.setXattr(p, "synBy", builder.toString());
  }
  
  public List<File> getSynFrom(File p) throws IOException {
    String val = strategy.getXattr(p, "synBy");
    if (val == null)
      return null;
    
    List<File> files = new ArrayList<>();
    for (String s : val.split(Pattern.quote(File.pathSeparator)))
      files.add(new File(s));
    return files;
  }
  
  public void clear() throws IOException {
    strategy.clear();
  }
}
