package build.pluto.dependency.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import build.pluto.builder.Builder;
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

public class XodusDatabase<K extends Comparable<K>, V extends Comparable<V>> implements MultiMapDatabase<K, V> {
  private final ComparableBinding keyBinding;
  private final ComparableBinding valBinding;

  private final Environment env;
  private Store store;

  public static XodusDatabase<File, File> createFileDatabase(String path) {
    return new XodusDatabase<File, File>(path, FileByteIterableBinding.BINDING, FileByteIterableBinding.BINDING);
  }

  public XodusDatabase(String envName, ComparableBinding keyBinding, ComparableBinding valBinding) {
    this.keyBinding = keyBinding;
    this.valBinding = valBinding;

    final EnvironmentConfig config = new EnvironmentConfig();
    this.env = Environments.newInstance(Builder.PLUTO_HOME + "/" + envName, config);
    this.store = makeStore();
  }

  @Override
  public void add(final K key, final V val) throws IOException {
    env.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        store.add(txn, keyBinding.objectToEntry(key), valBinding.objectToEntry(val));
      }
    });
  }

  @Override
  public void addAll(final K key, final Collection<? extends V> vals) throws IOException {
    env.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        for (V val : vals) {
          store.add(txn, keyBinding.objectToEntry(key), valBinding.objectToEntry(val));
        }
      }
    });
  }

  @Override
  public void addForEach(final Collection<? extends K> keys, final V val) throws IOException {
    env.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        for (K key : keys) {
          store.add(txn, keyBinding.objectToEntry(key), valBinding.objectToEntry(val));
        }
      }
    });
  }

  @Override
  public boolean contains(K key, V val) throws IOException {
    final Transaction txn = env.beginReadonlyTransaction();
    try {
      return store.get(txn, keyBinding.objectToEntry(key)) != null;
    } finally {
      abortIfNotFinished(txn);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<V> get(K key) throws IOException {
    final Transaction txn = env.beginReadonlyTransaction();
    try {
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
    env.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        try (Cursor cursor = store.openCursor(txn)) {
          if (cursor.getSearchBoth(keyBinding.objectToEntry(key), valBinding.objectToEntry(val))) {
            cursor.deleteCurrent();
          }
        }
      }
    });
  }

  @Override
  public void removeAll(final K key) throws IOException {
    env.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        store.delete(txn, keyBinding.objectToEntry(key));
      }
    });
  }

  @Override
  public void removeForEach(final Collection<? extends K> keys, final V val) throws IOException {
    final ByteIterable valBytes = valBinding.objectToEntry(val);
    env.executeInTransaction(new TransactionalExecutable() {
      @Override
      public void execute(Transaction txn) {
        try (Cursor cursor = store.openCursor(txn)) {
          for (K key : keys) {
            if (cursor.getSearchBoth(keyBinding.objectToEntry(key), valBytes)) {
              cursor.deleteCurrent();
            }
          }
        }
      }
    });
  }

  @Override
  public void clear() throws IOException {
    env.clear();
    this.store = makeStore();
  }

  @Override
  public void close() {
    env.close();
  }

  private Store makeStore() {
    final Transaction transaction = env.beginTransaction();
    try {
      return env.openStore("pluto", StoreConfig.WITH_DUPLICATES_WITH_PREFIXING, transaction);
    } finally {
      transaction.commit();
    }
  }

  private void abortIfNotFinished(Transaction txn) {
    if (!txn.isFinished())
      txn.abort();
  }
}
