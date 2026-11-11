package nro.models.utils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author By Mr Blue
 * 
 */

public class FileIO {

    private static final int MAX_CACHE_BYTES = 16 * 1024 * 1024; // 16 MB max cache
    private static final int MAX_SINGLE_FILE_BYTES = 64 * 1024;  // 64 KB max per cached file
    private static final byte[] MISSING_FILE = new byte[0];
    private static int currentCacheBytes = 0;

    private static final java.util.Map<String, byte[]> FILE_CACHE = new java.util.LinkedHashMap<String, byte[]>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, byte[]> eldest) {
            if (currentCacheBytes > MAX_CACHE_BYTES) {
                byte[] val = eldest.getValue();
                if (val != null && val != MISSING_FILE) {
                    currentCacheBytes = Math.max(0, currentCacheBytes - val.length);
                }
                return true;
            }
            return size() > 4096;
        }
    };

    public static void clearCache() {
        synchronized (FILE_CACHE) {
            FILE_CACHE.clear();
            currentCacheBytes = 0;
        }
    }

    public static byte[] readFile(String url) {
        if (url == null) {
            return null;
        }
        synchronized (FILE_CACHE) {
            byte[] cached = FILE_CACHE.get(url);
            if (cached != null) {
                return cached == MISSING_FILE ? null : cached;
            }
        }
        try {
            File f = new File(url);
            if (!f.isFile()) {
                synchronized (FILE_CACHE) {
                    FILE_CACHE.put(url, MISSING_FILE);
                }
                return null;
            }
            byte[] ab;
            try (FileInputStream fis = new FileInputStream(f)) {
                int len = (int) f.length();
                if (len <= 0) {
                    len = fis.available();
                }
                ab = new byte[len];
                int offset = 0;
                while (offset < len) {
                    int r = fis.read(ab, offset, len - offset);
                    if (r == -1) {
                        break;
                    }
                    offset += r;
                }
            }
            if (ab.length <= MAX_SINGLE_FILE_BYTES) {
                synchronized (FILE_CACHE) {
                    byte[] prev = FILE_CACHE.put(url, ab);
                    if (prev != null && prev != MISSING_FILE) {
                        currentCacheBytes = Math.max(0, currentCacheBytes - prev.length);
                    }
                    currentCacheBytes += ab.length;
                }
            }
            return ab;
        } catch (IOException e) {
        }
        return null;
    }

    public static ByteArrayOutputStream loadFile(String url) {
        try {
            FileInputStream openFileInput = new FileInputStream(url);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr = new byte[1024];
            while (true) {
                int read = openFileInput.read(bArr);
                if (read == -1) {
                    break;
                }
                byteArrayOutputStream.write(bArr, 0, read);
            }
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
            openFileInput.close();
            return byteArrayOutputStream;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeFile(String url, byte[] ab) {
        try {
            File f = new File(url);
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(url);
            fos.write(ab);
            fos.flush();
            fos.close();
        } catch (IOException e) {
        }
    }

    public static void writeFile(String url, String content) {
        try {
            File file = new File(url);

            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            FileOutputStream fos = new FileOutputStream(file);
            try (OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                writer.write(content);
                writer.flush();
            }
        } catch (IOException e) {
        }
    }

    public static List<File> getFiles(String folderPath) {
        List<File> fileList = new ArrayList<>();
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file);
                    }
                }
            }
        }

        return fileList;
    }
}
