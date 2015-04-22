package build.pluto.stamp;

import java.io.File;

public class Util {
  public static boolean stampEqual(Stamp s, File p) {
    return s.equals(s.getStamper().stampOf(p));
  }
}
