/**
 * 
 */
package org.sugarj.cleardep.stamp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.path.Path;

/**
 * @author seba
 *
 */
public class CollectionStamper implements Stamper {

  private final Stamper elementStamper;
  
  public CollectionStamper(Stamper elementStamper) {
    this.elementStamper = elementStamper;
  }
  
  @Override
  public Stamp stampOf(Path p) {
    return new CollectionStamp(Collections.singletonMap(p, elementStamper.stampOf(p)), this);
  }
  
  public Stamp stampOf(Collection<Path> paths) {
    Map<Path, Stamp> map = new HashMap<>();
    for (Path p : paths)
      map.put(p, elementStamper.stampOf(p));
    return new CollectionStamp(map, this);
  }

  
  public class CollectionStamp implements Stamp {

    private static final long serialVersionUID = -2072413432321939737L;

    private final Map<Path, Stamp> value;
    private final CollectionStamper stamper;
    
    public CollectionStamp(Map<Path, Stamp> value, CollectionStamper stamper) {
      this.value = value;
      this.stamper = stamper;
    }

    @Override
    public boolean equals(Stamp o) {
      return o instanceof CollectionStamp && ((CollectionStamp) o).value.equals(value);
    }

    @Override
    public CollectionStamper getStamper() {
      return stamper;
    }
    
  }
}
