package build.pluto.stamp;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CollectionStamper implements Stamper {

  private static final long serialVersionUID = 4966185168870429293L;

  private final Stamper elementStamper;
  
  public CollectionStamper(Stamper elementStamper) {
    this.elementStamper = elementStamper;
  }
  
  @Override
  public Stamp stampOf(File p) {
    return new ValueStamp<>(this, Collections.singletonMap(p, elementStamper.stampOf(p)));
  }
  
  public Stamp stampOf(Iterable<File> paths) {
    Map<File, Stamp> map = new HashMap<>();
    for (File p : paths)
      map.put(p, elementStamper.stampOf(p));
    return new ValueStamp<>(this, map);
  }
}
