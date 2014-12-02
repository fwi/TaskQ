package nl.fw.taskq.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

	private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

	/** 
	 * Directory for temporary files. Guaranteed to end with a file-separator
	 * (java.io.tmpdir does not always end with a file-separator, for example on Linux)
	 */
	public static final String TmpDir = endWithSep(System.getProperty("java.io.tmpdir"));

	private FileUtil() {}
	
	/**
	 * @param s A path
	 * @return The path ending with a file separator (/ or \).
	 */
	public static String endWithSep(final String s) {
		if (s.endsWith("\\") || s.endsWith("/")) return s;
		return s + File.separator;
	}
	
	/** 
	 * Creates a unique file in the {@link #TmpDir}
	 */
	public static File createTempFile() {
		return new File(TmpDir + UUID.randomUUID().toString());
	}
	
	/** 
	 * Deletes the given file if it is not null and exists. Logs a warning when delete fails.
	 */
	public static void delTempFile(final String fname) {
		if (fname == null || fname.trim().isEmpty()) return;
		delTempFile(new File(fname));
	}
	
	/** 
	 * Deletes the given file if it is not null and exists. Logs a warning when delete fails. 
	 */
	public static void delTempFile(final File f) {
		if (f != null && f.exists() && !f.delete()) {
			log.warn("Failed to delete temporary file " + f.getAbsolutePath());
		}
	}

	/**
	 * Returns null or a valid existing directory that ends with a file separator.
	 */
	public static String dirName(String dir) {
		
		if (dir == null) return null;
		dir = dir.trim();
		if (dir.isEmpty()) return null;
		File f = new File(dir);
		if (!f.isDirectory()) return null;
		try {
			dir = f.getCanonicalPath();
		} catch (Throwable t) {
			dir = f.getAbsolutePath();
		}
		if (dir.isEmpty()) return null;
		return endWithSep(dir);
	}

	/** 
	 * Returns the canonical or absolute path to the file. 
	 */
	public static String getFullPath(final File f) {
		
		String fullPath = null;
		try {
			fullPath = f.getCanonicalPath();
		} catch (Exception e) {
			fullPath = f.getAbsolutePath();
		}
		return fullPath;
	}

	/** 
	 * Converts a URL to a File (e.g. %20 int the path is converted to a space).
	 * @return A file (which may or may not exist) or null if url was null.
	 */
	public static File getFile(final URL url) {
	
		if (url == null) return null;
		File f = null;
		try {
			f = new File(url.toURI());
		} catch(Exception e) {
			f = new File(url.getPath());
		}
		return f;
	}

	/** 
	 * Uses the thread's class-loader to get the path to a file on the class-path.
	 * @return null if resourceName was not found or a file (which may or may not exist).
	 */
	public static File getFile(final String resourceName) {
		return getFile(getContextClassLoader().getResource(resourceName));
	}
	
	/**
	 * Retuns the file-path to resourceName using the classResource.getResource(s) method. 
	 * Can be used for unit-testing, should not be used in main source code.
	 */
	public static String getPath(Class<?> classResource, String resourceName) {
		return getFile(classResource.getResource(resourceName)).getPath();
	}
	
	/**
	 * @return The context class-loader if available, else the classloader of this class.
	 */
	public static ClassLoader getContextClassLoader() {
		
		ClassLoader cl = null;
		try { cl = Thread.currentThread().getContextClassLoader(); } catch (Exception ignored) {}
		if (cl == null) {
			cl = FileUtil.class.getClassLoader();
			if (cl == null) {
				cl = ClassLoader.getSystemClassLoader();
			}
		}
		return cl;
	}

	/** 
	 * Uses the current thread's class loader to find a resource and open it as a stream. 
	 */
	public static InputStream getResourceAsStream(final String rname) {
		return getContextClassLoader().getResourceAsStream(rname);
	}

	/** 
	 * Uses the current thread's class loader to find a resource. 
	 */
	public static URL getResource(final String rname) {
		return getContextClassLoader().getResource(rname);
	}

	/**
	 * Reads data from inputStream using platform default charset.
	 * Returns an empty StringBuilder when inputStream is null.
	 * <br>Always closes the inputStream.
	 */
	public static StringBuilder read(final InputStream is) throws IOException {
		return read(is, Charset.defaultCharset());
	}
	
	/**
	 * Reads data from inputStream using specified encoding (e.g. UTF-8).
	 * Returns an empty StringBuilder when inputStream is null.
	 * <br>Always closes the inputStream.
	 */
	public static StringBuilder read(final InputStream is, final String encoding) throws IOException {
		return read(is, Charset.forName(encoding));
	}

	/**
	 * Reads data from inputStream using specified encoding (e.g. UTF-8).
	 * Returns an empty StringBuilder when inputStream is null.
	 * <br>Always closes the inputStream.
	 */
	public static StringBuilder read(final InputStream is, final Charset charset) throws IOException {
		
		final StringBuilder sb = new StringBuilder(1024);
		if (is == null) return sb;
		final Reader r = new InputStreamReader(is, charset);
		final char[] buf = new char[1024];
		try {
			int l = 0;
			while((l = r.read(buf)) > 0) {
				sb.append(buf, 0, l);
			}
		} finally {
			close(r);
		}
		return sb;
	}
	
	/**
	 * Closes the closable if it is not null.
	 * Logs a warning when an error occurs.
	 */
	public static void close(final Closeable closable) {
		if (closable != null) {
			try { closable.close(); } catch (IOException ioe) {
				log.warn("Failed to close [" + closable + "] - " + ioe);
			}
		}
	}

}
