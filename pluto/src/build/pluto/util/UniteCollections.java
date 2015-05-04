package build.pluto.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

public class UniteCollections<T, C extends Collection<T>> {

  private final Supplier<C> collectionBuilder;

  public UniteCollections(Supplier<C> collectionBuilder) {
    Objects.requireNonNull(collectionBuilder);
    this.collectionBuilder = collectionBuilder;
  }
  
  public class Key {
  }
  
  private Map<Key, C> repSetsMap = new HashMap<>();
  private Map<T, Key> setMembership = new HashMap<>();
  
  private C getCollection(Key key) {
    return repSetsMap.get(key);
  }
  
  public Key createSet(T initial) {
    Key key = new Key();
    repSetsMap.put(key, collectionBuilder.get());
    addTo(key, initial);
    return key;
  }
  
  public Key getSet(T elem) {
    return setMembership.get(elem);
  }
  
  public C getSetMembers(Key key) {
    return getCollection(key);
  }
  
  public C getSetMembers(T elem) {
    Key set = getSet(elem);
    if (set != null)
      return getCollection(set);
    else
      throw new NoSuchElementException("No set for the elem " + elem + " exists");
  }

  public void addTo(Key setKey, T newElem) {
    assert getSet(newElem) == null : "Cannot add an element to the set because the element is also a member of another set";
    C set = getCollection(setKey);
    set.add(newElem);
    setMembership.put(newElem, setKey);
  }
  
  public Key getOrCreate(T elem) {
    Key key = getSet(elem);
    if (key == null) {
      key = createSet(elem);
    }
    return key;
  }
  
  public Key uniteOrAdd(Key key, T elem) {
    Key key2 = getSet(elem);
    if (key2 == null) {
      addTo(key, elem);
      return key;
    } else {
      return unite(key, key2);
    }
  }
  
  public Key unite(Key key1, Key key2) {
    if (key1 == key2)
      return key1;
    
    // Move elements from one elem to other
    final C destSet = getCollection(key1);
    final C srcSet = getCollection(key2);
    destSet.addAll(srcSet);
    
    for (T elem : srcSet) {
      setMembership.put(elem, key1);
    }
    repSetsMap.remove(key2);
    
    return key1;
  }

}
