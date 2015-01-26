package org.sugarj.common;

import java.util.Iterator;
import java.util.Objects;

public class AppendingIterator<T,E> implements Iterator<E> {

  private Iterator<T> mainIterator;
  private Iterator<E> currentIterator;
  private IteratorExtractor<T, E> iteratorExtractor;

  public static interface IteratorExtractor<T, E> {
    public Iterator<E> getIterator(T t);
  }
  
  private static class IterableIteratorExtractor<T extends Iterable<E>, E> implements IteratorExtractor<T, E> {
    
    @Override
    public Iterator<E> getIterator(T t) {
      return t.iterator();
    }
    
  }
  
  public static <T extends Iterable<E>, E> AppendingIterator<T,E> appendingIteratorFor(Iterator<T> mainIterator) {
    return new AppendingIterator<>(mainIterator, new IterableIteratorExtractor<T,E>());
  }
  
  public AppendingIterator(Iterator<T> mainIterator, IteratorExtractor<T, E> iteratorExtractor) {
    super();
    Objects.requireNonNull(iteratorExtractor);
    Objects.requireNonNull(mainIterator);
    this.mainIterator = mainIterator;
    this.iteratorExtractor = iteratorExtractor;
    if (this.mainIterator.hasNext()) {
      this.currentIterator = this.iteratorExtractor.getIterator(this.mainIterator.next());
    } else {
      this.currentIterator = null;
    }
  }
  

  @Override
  public boolean hasNext() {
    if (this.currentIterator != null && this.currentIterator.hasNext()) {
      return true;
    }
    while (this.mainIterator.hasNext()) {
      this.currentIterator = this.iteratorExtractor.getIterator(this.mainIterator.next());
      if (this.currentIterator.hasNext()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public E next() {
    if (this.currentIterator.hasNext()) {
      return this.currentIterator.next();
    } else {
      this.currentIterator = this.iteratorExtractor.getIterator(this.mainIterator.next());
      return this.currentIterator.next();
    }
  }
  
  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove"); 
  }

}
