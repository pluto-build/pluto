package build.pluto.xattr;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class XattrPreferencesStrategy implements XattrStrategy {
  private final static String PREFIX = "build.pluto";

  private Preferences prefs;

  public XattrPreferencesStrategy(String pathName) {
    final String path;
    if (pathName == null || pathName.isEmpty()) {
      path = PREFIX;
    } else {
      pathName = pathName.replace('\\', '/');
      if(pathName.startsWith("/")) {
        pathName = pathName.substring(1);
      }
      path = PREFIX + "/" + pathName;
    }
    this.prefs = Preferences.userRoot().node(path);
  }

  @Override
  public void setXattr(File p, String k, String value) throws IOException {
    String key = p.getAbsolutePath() + ":" + k;
    if (key.length() > Preferences.MAX_KEY_LENGTH)
      key = p.hashCode() + ":" + k;
    prefs.put(key, value);
  }

  @Override
  public void removeXattr(File p, String k) throws IOException {
    String key = p.getAbsolutePath() + ":" + k;
    if (key.length() > Preferences.MAX_KEY_LENGTH)
      key = p.hashCode() + ":" + k;
    prefs.remove(key);
  }

  @Override
  public String getXattr(File p, String k) throws IOException {
    String key = p.getAbsolutePath() + ":" + k;
    if (key.length() > Preferences.MAX_KEY_LENGTH)
      key = p.hashCode() + ":" + k;
    String val = prefs.get(key, null);
    return val;
  }

  @Override
  public Map<String, String> getAllXattr(File p) throws IOException {
    try {
      Map<String, String> attrs = new HashMap<>();
      for (String key : prefs.keys())
        attrs.put(key, prefs.get(key, null));
      return attrs;
    } catch (BackingStoreException e) {
      return null;
    }
  }

  @Override
  public void clear() throws IOException {
    try {
      prefs.clear();
      prefs.flush();
    } catch (BackingStoreException e) {
      throw new IOException(e);
    }
  }

}
