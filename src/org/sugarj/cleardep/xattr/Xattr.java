package org.sugarj.cleardep.xattr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

public class Xattr {

  public static Xattr getDefault() {
    return new Xattr(new XattrPreferencesStrategy());
//    try {
//      if (Files.getFileStore(new File(".").toPath()).supportsFileAttributeView(UserDefinedFileAttributeView.class))
//        return new Xattr(new XattrAttributeViewStrategy());
//    } catch (IOException e) {
//    }
//    
//    XattrCommandStrategy xcs = new XattrCommandStrategy();
//    try {
//      Path p = FileCommands.newTempFile("xxx");
//      xcs.setXattr(p, "test", "test-value");
//      String val = xcs.getXattr(p, "test");
//      if (val != null && val.equals("test-value"))
//        return new Xattr(xcs);
//    } catch (IOException e) {
//    }
//
//    throw new IllegalStateException("No Xattr strategy available.");
  }
  
  public final static String PREFIX = "org.sugarj.cleardep";
  
  public final XattrStrategy strategy;
  
  public Xattr(XattrStrategy strategy) {
    this.strategy = strategy;
  }
 
  public void setGenBy(Path p, CompilationUnit unit) throws IOException {
    strategy.setXattr(p, "genBy", unit.getPersistentPath().getAbsolutePath());
  }
  
  public Path getGenBy(Path p) throws IOException {
    String val = strategy.getXattr(p, "genBy");
    if (val == null)
      return null;
    return new AbsolutePath(val);
  }
  
  public void setSynFrom(Path p, Iterable<Path> paths) throws IOException {
    StringBuilder builder = new StringBuilder();
    for (Iterator<Path> it = paths.iterator(); it.hasNext(); ) {
      builder.append(it.next());
      if (it.hasNext())
        builder.append(File.pathSeparatorChar);
    }
    strategy.setXattr(p, "synBy", builder.toString());
  }
  
  public List<Path> getSynFrom(Path p) throws IOException {
    String val = strategy.getXattr(p, "synBy");
    if (val == null)
      return null;
    
    List<Path> paths = new ArrayList<>();
    for (String s : val.split(Pattern.quote(File.pathSeparator)))
      paths.add(new AbsolutePath(s));
    return paths;
  }
}

