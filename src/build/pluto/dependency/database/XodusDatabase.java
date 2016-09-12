package build.pluto.dependency.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.ComparableBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;
import build.pluto.builder.Builder;

public class XodusDatabase<K extends Comparable<K>, V extends Comparable<V>> implements MultiMapDatabase<K, V> {

  private final static Map<String, Environment> dbs = new HashMap<>();
  static {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        for (Environment env : dbs.values())
          try {
            env.close();
          } catch (Exception e) {
            // ignore
          }
      }
    }));
  }
  
  final Environment db;
  final String dbName;
  final ComparableBinding keyBinding;
  final ComparableBinding valBinding;
  
  public static XodusDatabase<File, File> createFileDatabase(final String dbName) {
    return new XodusDatabase<File, File>(dbName, FileByteIterableBinding.BINDING, FileByteIterableBinding.BINDING);
  }
  
  public XodusDatabase(final String dbName, ComparableBinding keyBinding, ComparableBinding valBinding) {
    this.dbName = dbName;
    this.keyBinding = keyBinding;
    this.valBinding = valBinding;
    
    String dbDir = Builder.PLUTO_HOME + "/" + dbName;
    synchronized (dbs) {
      Environment myDb = dbs.get(dbDir);
      if (myDb == null) {
        EnvironmentConfig config = new EnvironmentConfig();
        myDb = Environments.newInstance(Builder.PLUTO_HOME + "/" + dbName, config);
        dbs.put(dbDir, myDb);
      }
      db = myDb;
    }
    
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        db.openStore(dbName, StoreConfig.WITH_DUPLICATES_WITH_PREFIXING, txn);
      }
    });
  }
  
  @Override
  public void add(final K key, final V val) throws IOException {
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
        store.add(txn, keyBinding.objectToEntry(key), valBinding.objectToEntry(val));
      }
    });
  }

  @Override
  public void addAll(final K key, final Collection<? extends V> vals) throws IOException {
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
        for (V val : vals)
          store.add(txn, keyBinding.objectToEntry(key), valBinding.objectToEntry(val));
      }
    });
  }

  @Override
  public void addForEach(final Collection<? extends K> keys, final V val) throws IOException {
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
        for (K key : keys)
          store.add(txn, keyBinding.objectToEntry(key), valBinding.objectToEntry(val));
      }
    });    
  }

  @Override
  public boolean contains(K key, V val) throws IOException {
    final Transaction txn = db.beginReadonlyTransaction();
    try {
      Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
      return store.get(txn, keyBinding.objectToEntry(key)) != null;
    } finally {
      abortIfNotFinished(txn);
    }
  }

  @Override
  public Collection<V> get(K key) throws IOException {
    final Transaction txn = db.beginReadonlyTransaction();
    try {
      Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
      List<V> result = new ArrayList<>();
      try (Cursor cursor = store.openCursor(txn)) {
        final ByteIterable v = cursor.getSearchKey(keyBinding.objectToEntry(key));
        if (v != null) {
          result.add((V) valBinding.entryToObject(v));
          while (cursor.getNextDup()) {
            result.add((V) valBinding.entryToObject(cursor.getValue()));
          }
        }
        return result;
      }
    } finally {
      abortIfNotFinished(txn);
    }
  }

  @Override
  public void remove(final K key, final V val) throws IOException {
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
        try (Cursor cursor = store.openCursor(txn)) {
          if (cursor.getSearchBoth(keyBinding.objectToEntry(key), valBinding.objectToEntry(val)))
            cursor.deleteCurrent();
        }
      }
    });
  }

  @Override
  public void removeAll(final K key) throws IOException {
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
        store.delete(txn, keyBinding.objectToEntry(key));
      }
    });    
  }

  @Override
  public void removeForEach(final Collection<? extends K> keys, final V val) throws IOException {
    final ByteIterable valBytes = valBinding.objectToEntry(val);
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        Store store = db.openStore(dbName, StoreConfig.USE_EXISTING, txn);
        try (Cursor cursor = store.openCursor(txn)) {
          for (K key : keys)
            if (cursor.getSearchBoth(keyBinding.objectToEntry(key), valBytes))
              cursor.deleteCurrent();
        }
      }
    });
  }

  @Override
  public void clear() throws IOException {
    db.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        db.removeStore(dbName, txn);
      }
    });

  }

  private void abortIfNotFinished(Transaction txn) {
    if (!txn.isFinished())
      txn.abort();
  }

}
