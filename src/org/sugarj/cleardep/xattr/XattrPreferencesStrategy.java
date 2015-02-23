package org.sugarj.cleardep.xattr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.sugarj.common.path.Path;

public class XattrPreferencesStrategy implements XattrStrategy {

  private final Preferences prefs;
  
  public XattrPreferencesStrategy() {
    this.prefs = Preferences.userRoot().node(Xattr.PREFIX);
  }
  
  @Override
  public void setXattr(Path p, String k, String value) throws IOException {
    String key = p + ":" + k;
    String ckey = key;
    if (key.length() > Preferences.MAX_KEY_LENGTH)
      ckey = p.hashCode() + ":" + k;
    prefs.put(ckey, value);
  }

  @Override
  public String getXattr(Path p, String k) throws IOException {
    String key = p + ":" + k;
    String ckey = key;
    if (key.length() > Preferences.MAX_KEY_LENGTH)
      ckey = p.hashCode() + ":" + k;
    String val = prefs.get(ckey, null);
    return val;
  }

  @Override
  public Map<String, String> getAllXattr(Path p) throws IOException {
    try {
      Map<String, String> attrs = new HashMap<>();
      for (String key : prefs.keys())
        attrs.put(key, prefs.get(key, null));
      return attrs;
    } catch (BackingStoreException e) {
      return null;
    }
  }

}
