package build.pluto.xattr;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface XattrStrategy {
  /**
   * Deletes all previously stored attributes.
   * @throws IOException
   */
  public void clear() throws IOException;
  
  /**
   * Store attribute (key=value) for file p.
   * @throws IOException
   */
  public void setXattr(File p, String key, String value) throws IOException;
  
  /**
   * Remove attribute key from file p.
   * @throws IOException
   */
  public void removeXattr(File p, String key) throws IOException;
  
  /**
   * Retrieve attribute key from file p.
   * @return the attribute or null.
   * @throws IOException
   */
  public String getXattr(File p, String key) throws IOException;

  /**
   * Retrieve all attributes for file p.
   * @return a key-value map.
   * @throws IOException
   */
  public Map<String, String> getAllXattr(File p) throws IOException;
}