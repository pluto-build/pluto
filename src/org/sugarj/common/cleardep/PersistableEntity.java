package org.sugarj.common.cleardep;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * @author Sebastian Erdweg
 *
 */
public abstract class PersistableEntity implements Serializable {
  
  private static final long serialVersionUID = 3725384862203109760L;

  private final static Map<Path, SoftReference<? extends PersistableEntity>> inMemory = new HashMap<>();
  
  protected Stamper stamper;
  
  public PersistableEntity() { /* for deserialization only */ }
//  public PersistableEntity(Stamper stamper) {
//    this.stamper = stamper;
//  }
      
  /**
   * Path and stamp of the disk-stored version of this result.
   */
  protected Path persistentPath;
  private int persistentStamp = -1;
  private boolean isPersisted = false;

  final public boolean isPersisted() {
    return isPersisted;
  }
  
  public boolean hasPersistentVersionChanged() {
    return isPersisted &&
           persistentPath != null && 
           persistentStamp != stamper.stampOf(persistentPath);
  }
  
  final protected void setPersisted() throws IOException {
    persistentStamp = stamper.stampOf(persistentPath);
    isPersisted = true;
  }
  
  final public int stamp() {
    if (!isPersisted())
      throw new RuntimeException("Cannot extract stamp from non-persisted module");
    return persistentStamp;
  }
  
  
  protected abstract void readEntity(ObjectInputStream in) throws IOException, ClassNotFoundException;
  protected abstract void writeEntity(ObjectOutputStream out) throws IOException;
  
  protected abstract void init();
  
  final protected static <E extends PersistableEntity> E create(Class<E> clazz, Stamper stamper, Path p) throws IOException {
    E entity;
    try {
      entity = read(clazz, stamper, p);
    } catch (IOException e) {
      e.printStackTrace();
      entity = null;
    }
    
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

    entity.stamper = stamper;
    entity.persistentPath = p;
    entity.cacheInMemory();
    entity.init();
    return entity;
  }
  
  final protected static <E extends PersistableEntity> E tryReadElseCreate(Class<E> clazz, Stamper stamper, Path p) throws IOException {
    try {
      E e = read(clazz, stamper, p);
      if (e != null)
        return e;
      return create(clazz, stamper, p);
    }
    catch (IOException e) {
      e.printStackTrace();
      return create(clazz, stamper, p);
    }
  }
  
  final protected static <E extends PersistableEntity> E read(Class<E> clazz, Stamper stamper, Path p) throws IOException {
    if (p == null)
      return null;
    
    E entity = readFromMemoryCache(clazz, p);
    if (entity != null && !entity.hasPersistentVersionChanged())
      return entity;

    if (!FileCommands.exists(p))
      return null;

    try {
      entity = clazz.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
      return null;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return null;
    }

    entity.stamper = stamper;
    entity.persistentPath = p;
    entity.setPersisted();
    entity.cacheInMemory();
    
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(p.getAbsolutePath()));

    try {
      long id = in.readLong();
      long readId = clazz.getField("serialVersionUID").getLong(entity);
      if (id != readId) {
        inMemory.remove(entity.persistentPath);
        return null;
      }
      
      entity.readEntity(in);
    } catch (Throwable e) {
      System.err.println("Could not read module's dependency file: " + p + ": " + e);
      inMemory.remove(entity.persistentPath);
      FileCommands.delete(entity.persistentPath);
      return null;
    } finally {
      in.close();
    }
    
    return entity;
  }
  
  final public void write() throws IOException {
    FileCommands.createFile(persistentPath);
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(persistentPath.getAbsolutePath()));

    try {
      out.writeLong(this.getClass().getField("serialVersionUID").getLong(this));
      writeEntity(out);
    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
      throw new IOException(e);
    } finally {
      out.close();
      setPersisted();
    }
  }
  
  final protected static <E extends PersistableEntity> E readFromMemoryCache(Class<E> clazz, Path p) {
    SoftReference<? extends PersistableEntity> ref;
    synchronized (PersistableEntity.class) {
      ref = inMemory.get(p);
    }
    if (ref == null)
      return null;
    
    PersistableEntity e = ref.get();
    if (e != null && clazz.isInstance(e))
      return clazz.cast(e);
    return null;
  }
  
  final protected void cacheInMemory() {
    synchronized (PersistableEntity.class) {
      inMemory.put(persistentPath, new SoftReference<>(this));
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
