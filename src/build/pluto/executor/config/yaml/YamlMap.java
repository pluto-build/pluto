package build.pluto.executor.config.yaml;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class YamlMap<K> implements Map<K, YamlObject> {
	private Map<K, Object> map;
	
	@SuppressWarnings("unchecked")
	YamlMap(Map<K, ?> map) {
		this.map = (Map<K, Object>) map;
	}
	
	@Override
	public YamlObject get(Object key) {
		if (map == null)
			return NullYamlObject.instance;
		Object v = map.get(key);
		return SimpleYamlObject.of(v);
			
	}

	@Override
	public int size() {
		return map == null ? 0 : map.size();
	}

	@Override
	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map != null && map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map != null && map.containsValue(value);
	}

	@Override
	public YamlObject put(K key, YamlObject value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public YamlObject remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends YamlObject> m) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();		
	}

	@Override
	public Set<K> keySet() {
		return map == null ? Collections.<K>emptySet() : map.keySet();
	}

	@Override
	public Collection<YamlObject> values() {
		if (map == null)
			return Collections.<YamlObject>emptySet();
		
		Set<YamlObject> result = new HashSet<>();
		for (Object v : map.values())
			result.add(SimpleYamlObject.of(v));
		return Collections.unmodifiableSet(result);
	}

	@Override
	public Set<Entry<K, YamlObject>> entrySet() {
		if (map == null)
			return Collections.<Entry<K, YamlObject>>emptySet();
		
		Set<Entry<K, YamlObject>> result = new HashSet<>();
		for (final Entry<K, Object> kv : map.entrySet())
			result.add(new Entry<K, YamlObject>() {
				@Override
				public K getKey() {
					return kv.getKey();
				}

				@Override
				public YamlObject getValue() {
					return SimpleYamlObject.of(kv.getValue());
				}

				@Override
				public YamlObject setValue(YamlObject value) {
					throw new UnsupportedOperationException();
				}
			});
		
		return Collections.unmodifiableSet(result);
	}
}
