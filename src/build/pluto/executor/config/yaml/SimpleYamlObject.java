package build.pluto.executor.config.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleYamlObject implements YamlObject {
	public static YamlObject of(Object val) {
		if (val == null)
			return NullYamlObject.instance;
		else
			return new SimpleYamlObject(val);
	}
	
	private Object val;
	
	private SimpleYamlObject(Object val) {
		this.val = val;
	}

	public Object asObject() {
		return val;
	}
	
	public <T> Map<T, YamlObject> asMap() {
		@SuppressWarnings("unchecked")
		Map<T, ?> map = (Map<T, ?>) val;
		return new YamlMap<T>(map);
	}
	
	public List<YamlObject> asList() {
		List<?> list = (List<?>) val;
		List<YamlObject> result = new ArrayList<>(list.size());
		for (Object e : list)
			result.add(SimpleYamlObject.of(e));
		return result;
	}
	
	public String asString() {
		return (String) val;
	}
	
	public Integer asInt() {
		return (Integer) val;
	}
	
	public Double asDouble() {
		return (Double) val;
	}

  @Override
  public Float asFloat() {
    return (Float) val;
  }

  @Override
  public Boolean asBoolean() {
    return (Boolean) val;
  }

  @Override
  public Character asChar() {
    return (Character) val;
  }

  @Override
  public Long asLong() {
    return (Long) val;
  }
}
