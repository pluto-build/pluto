package org.sugarj.cleardep.stamp;

import org.sugarj.common.path.Path;

abstract public class SimpleStamp<T> implements Stamp {

  public static final long serialVersionUID = 100393450148269674L;

  protected final T value;
  
  public SimpleStamp(T t) {
    this.value = t;
  }
  
  @Override
  public boolean equals(Stamp o) {
    return value == null && o == null || 
           (o instanceof SimpleStamp<?>) && value.equals(((SimpleStamp<?>) o).value);
  }

  public static boolean equalStamp(Stamp s, Path p) {
    return s.equals(s.getStamper().stampOf(p));
  }
}
