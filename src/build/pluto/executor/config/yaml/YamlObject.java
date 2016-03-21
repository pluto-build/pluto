package build.pluto.executor.config.yaml;

import java.util.List;
import java.util.Map;

public interface YamlObject {
	public Object asObject();
	
	public <T> Map<T, YamlObject> asMap();
	
	public List<YamlObject> asList();
	
	public String asString();
	
	public Integer asInt();
	
	public Float asFloat();
	
	public Double asDouble();

  public Boolean asBoolean();

  public Character asChar();

  public Long asLong();

}
