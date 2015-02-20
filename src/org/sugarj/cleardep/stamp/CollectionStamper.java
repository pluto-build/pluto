package org.sugarj.cleardep.stamp;

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
    return new CollectionStamp(Collections.singletonMap(p, elementStamper.stampOf(p)), this);
  }
  
  public Stamp stampOf(Iterable<Path> paths) {
    Map<Path, Stamp> map = new HashMap<>();
    for (Path p : paths)
      map.put(p, elementStamper.stampOf(p));
    return new CollectionStamp(map, this);
  }

  
  public static class CollectionStamp implements Stamp {

    private static final long serialVersionUID = -2072413432321939737L;

    private final Map<Path, Stamp> value;
    private final Stamper stamper;
    
    public CollectionStamp(Map<Path, Stamp> value, Stamper stamper) {
      this.value = value;
      this.stamper = stamper;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof CollectionStamp && ((CollectionStamp) o).value.equals(value);
    }

    @Override
    public Stamper getStamper() {
      return stamper;
    }

    @Override
    public String toString() {
      return "Collection(" + value + ")";
    }
  }
}
