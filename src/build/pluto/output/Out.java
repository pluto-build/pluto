package build.pluto.output;

public interface Out<T> extends Output {

  public T val();
  public boolean expired();
  
}
