package build.pluto.dependency.database;

import java.io.IOException;
import java.util.Collection;

/**
 *
 * Interface for accessing a multimap databases .
 * 
 * @author seba
 *
 */
public interface MultiMapDatabase<K, V> extends AutoCloseable {
  public void add(K key, V val) throws IOException;
  public void addAll(K key, Collection<? extends V> val) throws IOException;
  public void addForEach(Collection<? extends K> keys, V val) throws IOException;
  public boolean contains(K key, V val) throws IOException;
  public Collection<V> get(K key) throws IOException;
  public void remove(K key, V val) throws IOException;
  public void removeAll(K key) throws IOException;
  public void removeForEach(Collection<? extends K> keys, V val) throws IOException;
  public void clear() throws IOException;
  public void close() throws IOException;
}
