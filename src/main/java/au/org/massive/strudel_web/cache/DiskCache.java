package au.org.massive.strudel_web.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import au.org.massive.strudel_web.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstracts a file-based cache of key/values
 */
public abstract class DiskCache {
	private static final Logger log = LogManager.getLogger(DiskCache.class);

	private static DB db;

    public DiskCache(String cacheFile) {
        if (db == null) {
            File f = null;
            try {
                f = new File(cacheFile);
                if (!f.exists()) {
                    f.createNewFile();
                }
                log.info("File-based cache file: "+f.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                log.warn("Could not create cache file: " + f.getAbsolutePath());
                f = null;
            }

            if (f == null) {
                try {
                    f = File.createTempFile("coesra-cache", ".tmp");
                    log.info("File-based cache will use a temporary file that is deleted on exit");
                } catch (IOException e) {
                    e.printStackTrace();
                    log.warn("Could not create temporary cache file.");
                }
            }

            if (f == null) {
                db = DBMaker.memoryDB()
                        .closeOnJvmShutdown()
                        .make();
                log.info("Using in-memory cache.");
            } else {
                db = DBMaker.fileDB(f)
                        .closeOnJvmShutdown()
                        .make();
                log.info("Using file-based cache: " + f.getAbsolutePath());
            }
        }
    }

    public class Expiry {
        private long duration;
        private TimeUnit unit;
        public Expiry(long duration, TimeUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }

        private long getDuration() {
            return duration;
        }

        private TimeUnit getUnit() {
            return unit;
        }
    }

    protected <K,V> Map<K,V> getCache(String name, Expiry expiry) {
    	DB.HTreeMapMaker mapMaker = db.hashMapCreate(name);
    	if (expiry != null) {
            mapMaker.expireAfterAccess(expiry.getDuration(), expiry.getUnit());
        }
        return mapMaker.makeOrGet();
    }

    public void commit() {
        db.commit();
    }

    public void rollback() {
        db.rollback();
    }
}
