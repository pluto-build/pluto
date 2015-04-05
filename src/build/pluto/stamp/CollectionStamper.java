package build.pluto.stamp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.path.Path;

public class CollectionStamper implements Stamper {

  private static final long serialVersionUID = 4966185168870429293L;

  private final Stamper elementStamper;
  
  public CollectionStamper(Stamper elementStamper) {
    this.elementStamper = elementStamper;
  }
  
  @Override
  public Stamp stampOf(Path p) {
    return new ValueStamp<>(this, Collections.singletonMap(p, elementStamper.stampOf(p)));
  }
  
  public Stamp stampOf(Iterable<Path> paths) {
    Map<Path, Stamp> map = new HashMap<>();
    for (Path p : paths)
      map.put(p, elementStamper.stampOf(p));
    return new ValueStamp<>(this, map);
  }
}
