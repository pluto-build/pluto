package build.pluto.dependency.database;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MemoryDatabase<K, V> implements MultiMapDatabase<K, V> {
  private final Map<K, Set<V>> map;

  public MemoryDatabase() {
    this.map = new HashMap<>();
  }

  @Override
  public void add(K key, V val) throws IOException {
    final Set<V> values = getValues(key);
    values.add(val);
  }

  @Override
  public void addAll(K key, Collection<? extends V> val) throws IOException {
    final Set<V> values = getValues(key);
    values.addAll(val);
  }

  @Override
  public void addForEach(Collection<? extends K> keys, V val) throws IOException {
    for (K key : keys) {
      add(key, val);
    }
  }

  @Override
  public boolean contains(K key, V val) throws IOException {
    final Set<V> values = getValues(key);
    return values.contains(val);
  }

  @Override
  public Collection<V> get(K key) throws IOException {
    return getValues(key);
  }

  @Override
  public void remove(K key, V val) throws IOException {
    final Set<V> values = getValues(key);
    values.remove(val);
  }

  @Override
  public void removeAll(K key) throws IOException {
    map.remove(key);
  }

  @Override
  public void removeForEach(Collection<? extends K> keys, V val) throws IOException {
    for (K key : keys) {
      removeAll(key);
    }
  }

  @Override
  public void clear() throws IOException {
    map.clear();
  }

  @Override
  public void close() throws IOException {
    // No closing required.
  }

  private Set<V> getValues(K key) {
    Set<V> values = map.get(key);
    if (values == null) {
      values = new HashSet<>();
      map.put(key, values);
    }
    return values;
  }
}
