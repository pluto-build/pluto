package org.sugarj.cleardep;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * @author Sebastian Erdweg
 *
 */
public abstract class PersistableEntity implements Serializable {
  
  private static final long serialVersionUID = 3725384862203109760L;

  private final static Map<Path, PersistableEntity> inMemory = new HashMap<>();
  
  
  public PersistableEntity() { /* for deserialization only */ }
      
  /**
   * Path and stamp of the disk-stored version of this result.
   */
  protected Path persistentPath;
  private Stamp persistentStamp = null;
  private boolean isPersisted = false;

  final public boolean isPersisted() {
    return isPersisted;
  }
  
  public boolean hasPersistentVersionChanged() {
    return isPersisted &&
           persistentPath != null && 
           !persistentStamp.equals(persistentStamp.getStamper().stampOf(persistentPath));
  }
  
  final protected void setPersisted(Stamper stamper) throws IOException {
    persistentStamp = stamper.stampOf(persistentPath);
    isPersisted = true;
  }
  
  final public Stamp stamp() {
    if (!isPersisted())
      throw new RuntimeException("Cannot extract stamp from non-persisted module");
    return persistentStamp;
  }
  
  
  protected abstract void readEntity(ObjectInputStream in, Stamper stamper) throws IOException, ClassNotFoundException;
  protected abstract void writeEntity(ObjectOutputStream out) throws IOException;
  
  protected abstract void init();
  
  final protected static <E extends PersistableEntity> E create(Class<E> clazz, Path p) throws IOException {
    E entity = readFromMemoryCache(clazz, p);
    
    if (entity != null) {
      entity.init();
      return entity;
    }
    
    try {
      entity = clazz.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
      return null;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    }

    entity.persistentPath = p;
    entity.cacheInMemory();
    entity.init();
    return entity;
  }
  
  final protected static <E extends PersistableEntity> E tryReadElseCreate(Class<E> clazz, Path p) throws IOException {
    try {
      E e = read(clazz, p);
      if (e != null)
        return e;
      return create(clazz, p);
    }
    catch (IOException e) {
      e.printStackTrace();
      return create(clazz, p);
    }
  }
  
  protected static <E extends PersistableEntity> E read(Class<E> clazz, Path p) throws IOException {
    if (p == null)
      return null;
    
    if (!FileCommands.exists(p))
      return null;
      
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(p.getAbsolutePath()));
      long id = in.readLong();

      E entity = readFromMemoryCache(clazz, p);
      
      if (entity != null && id == clazz.getField("serialVersionUID").getLong(entity) && !entity.hasPersistentVersionChanged())
        return entity;

      if (entity == null)
        entity = clazz.newInstance();
      
      Stamper stamper = (Stamper) in.readObject();
      entity.persistentPath = p;
      entity.cacheInMemory();
      entity.setPersisted(stamper);

      entity.readEntity(in, stamper);
      return entity;
    } catch (Throwable e) {
      System.err.println("Could not read module's dependency file: " + p + ": " + e);
      e.printStackTrace();
      FileCommands.delete(p);
      return null;
    } finally {
      if (in != null)
        in.close();
    }
  }
  
  final public void write(Stamper stamper) throws IOException {
    Objects.requireNonNull(stamper);
    FileCommands.createFile(persistentPath);
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(persistentPath.getAbsolutePath()));

    try {
      out.writeLong(this.getClass().getField("serialVersionUID").getLong(this));
      out.writeObject(stamper);
      writeEntity(out);
    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
      throw new IOException(e);
    } finally {
      out.close();
      setPersisted(stamper);
    }
  }
  
  final protected static <E extends PersistableEntity> E readFromMemoryCache(Class<E> clazz, Path p) {
    PersistableEntity e;
    synchronized (PersistableEntity.class) {
      e = inMemory.get(p);
    }
    if (e == null)
      return null;
    
    if (e != null && clazz.isInstance(e))
      return clazz.cast(e);
    return null;
  }
  
  final protected void cacheInMemory() {
    synchronized (PersistableEntity.class) {
      inMemory.put(persistentPath, this);
    }
  }
  
  public String toString() {
    return getClass().getSimpleName() + "(" + persistentPath + ")";
  }
  
  public String getName() {
    if (persistentPath instanceof RelativePath)
      return FileCommands.dropExtension(((RelativePath) persistentPath).getRelativePath());
    return persistentPath.getAbsolutePath();
  }
}
