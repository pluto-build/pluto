package build.pluto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.stamp.Stamp;
import build.pluto.stamp.Stamper;
import build.pluto.util.AbsoluteComparedFile;

/**
 * @author Sebastian Erdweg
 *
 */
public abstract class PersistableEntity implements Serializable {
  
  private static final long serialVersionUID = 3725384862203109760L;

  private final static Map<AbsoluteComparedFile, PersistableEntity> inMemory = new HashMap<>();
  
  public PersistableEntity() { /* for deserialization only */ }
      
  /**
   * Path and stamp of the disk-stored version of this result.
   */
  protected File persistentPath;
  private Stamp persistentStamp;

  final public boolean isPersisted() {
    return persistentStamp != null;
  }
  
  public boolean hasPersistentVersionChanged() {
    return isPersisted() &&
           persistentPath != null && 
           !persistentStamp.equals(persistentStamp.getStamper().stampOf(persistentPath));
  }
  
  final protected void setPersisted(Stamper stamper) throws IOException {
    persistentStamp = Objects.requireNonNull(stamper.stampOf(persistentPath));
  }
  
  final public Stamp stamp() {
    if (!isPersisted())
      throw new RuntimeException("Cannot extract stamp from non-persisted module");
    return persistentStamp;
  }
  
  
  protected abstract void readEntity(ObjectInputStream in) throws IOException, ClassNotFoundException;
  protected abstract void writeEntity(ObjectOutputStream out) throws IOException;
  
  protected void init() {
    this.persistentStamp = null;
  }
  
  final protected static <E extends PersistableEntity> E create(Class<E> clazz, File p) throws IOException {
    p = p.getAbsoluteFile();
    E entity = readFromMemoryCache(clazz, p);
    
    if (entity != null) {
      entity.init();
      return entity;
    }
    
    try {
      entity = clazz.newInstance();
    } catch (InstantiationException e) {
      if((Log.log.getLoggingLevel() & Log.DETAIL) != 0) {
        e.printStackTrace(Log.err);
      }
      return null;
    } catch (IllegalAccessException e) {
      if((Log.log.getLoggingLevel() & Log.DETAIL) != 0) {
        e.printStackTrace(Log.err);
      }
      return null;
    }

    entity.persistentPath = p;
    entity.cacheInMemory();
    entity.init();
    return entity;
  }
  
  protected static <E extends PersistableEntity> E read(Class<E> clazz, File p) throws IOException {
    if (p == null)
      return null;
    
    if (!p.exists())
      return null;
      
    E entity = null;
    try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(p))) {
      long id = in.readLong();

      entity = readFromMemoryCache(clazz, p);
      
      if (entity != null && id == clazz.getField("serialVersionUID").getLong(entity) && !entity.hasPersistentVersionChanged())
        return entity;

      if (entity == null)
        entity = clazz.newInstance();
      
      Stamper stamper = (Stamper) in.readObject();
      entity.persistentPath = p;
      entity.cacheInMemory();
      entity.setPersisted(stamper);

      entity.readEntity(in);
      return entity;
    } catch (Exception e) {
      Log.log.logErr("Could not read module's dependency file: " + p, e, Log.DETAIL);
      if ((Log.log.getLoggingLevel() & Log.DETAIL) == 0)
        Log.log.logErr("Could not read module's dependency file: " + p + ": " + e, Log.CACHING);

      // File is not readable. We delete it to avoid repeated read failures.
      Files.delete(p.toPath());
      if (entity != null)
        entity.removeFromMemoryCache();
      return null;
    }
  }
  
  final public void write(Stamper stamper) throws IOException {
    Objects.requireNonNull(stamper);
    FileCommands.createFile(persistentPath.toPath());
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(persistentPath));

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
  
  final public static void cleanCache() {
    synchronized (PersistableEntity.class) {
      inMemory.clear();
    }
  }

  final protected static <E extends PersistableEntity> E readFromMemoryCache(Class<E> clazz, File p) {
    PersistableEntity e;
    synchronized (PersistableEntity.class) {
      e = inMemory.get(AbsoluteComparedFile.absolute(p));
    }
    if (e == null)
      return null;
    
    if (e != null && clazz.isInstance(e))
      return clazz.cast(e);
    return null;
  }
  
  final protected void cacheInMemory() {
    synchronized (PersistableEntity.class) {
      inMemory.put(AbsoluteComparedFile.absolute(persistentPath), this);
    }
  }
  
  final protected void removeFromMemoryCache() {
    synchronized (PersistableEntity.class) {
      inMemory.remove(AbsoluteComparedFile.absolute(persistentPath));
    }
  }

  public String toString() {
    return getClass().getSimpleName() + "(" + persistentPath + ")";
  }
  
  public String getName() {
    if (!persistentPath.isAbsolute())
      return FileCommands.dropExtension(persistentPath.toPath()).toString();
    return persistentPath.toString();
  }
}
