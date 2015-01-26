package org.sugarj.common;

import java.util.Iterator;
import java.util.Objects;

public class AppendingIterable<T> implements Iterable<T> {

  private Iterable<? extends Iterable<T>> mainIterables;
  
  public AppendingIterable(Iterable<? extends Iterable<T>> mainIterables) {
    Objects.requireNonNull(mainIterables);
    this.mainIterables = mainIterables;
  }
  
  @Override
  public Iterator<T> iterator() {
     return AppendingIterator.appendingIteratorFor(mainIterables.iterator());
  }

}
