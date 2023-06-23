package net.tomahawk; 

import java.awt.Window;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * XFileDialog provides a file load/save dialog using a native UI on Windows,
 * MacOS, and Linux platforms. For Windows, it relies on JNI and a native C++
 * dll to provide access to the underlying Windows CFileDialog load/save dialog.
 * On other platforms, it falls back to java.awt.FileDialog, which invokes the
 * native toolkits on MacOS and Linux.
 *
 * The interface for XFileDialog is based on java.awt.FileDialog, so it should
 * be a drop-in replacement for simple cases. More advanced features of
 * FileDialog are not supported, however.
 *
 * XFileDialog objects are not intnded to be thread-safe or reusable. Multiple
 * threads should not concurrently call methods on the same XFileDialog object,
 * XFileDialog.setVisible(true) can only be called once.
 *
 * Support for directory selection is not yet implemented. It seems likely this
 * could be done on Windows platforms using CFileDialog, and perhaps on MacOS
 * using System.setProperty("apple.awt.fileDialogForDirectories", "true"),
 * otherwise falling back to Swing's JFileChooser as a last restort.
 */
public class XFileDialog
{

  public static final int LOAD = FileDialog.LOAD;
  public static final int SAVE = FileDialog.SAVE;

  private Window parent; // can be Dialog or Frame
  private String title;
  private int mode; // LOAD or SAVE
  private String initialDir, resultDir;
  private boolean multiSelection;
  private ArrayList<FilenameFilter> filters = new ArrayList<>();
  private String initialFile, resultFile;
  private File[] resultFiles = new File[0];
  private boolean attemptWindowsJNI = true;

  // 0 = initializing, 1 = displaying, 2 = closed
  private int state;

  private static int traceLevel = 0; // for debugging

  private static Object lock = new Object();
  private static boolean initialized; // protected by lock during initialization
  private static boolean hasWindowsJNI; // protected by lock during initialization
  

  /**
   * Set debug tracing level. Use 0 to disable all debug printing. Higher
   * numbers enable more debug printing.
   */
  public static void setTraceLevel(int val) {
    traceLevel = val;
  }

  /**
   * Print debug message at the given level.
   */
  private static void trace(int level, String msg) {
    if (traceLevel >= level)
      System.out.println("XFileDialog: " + msg); 
  }

  /**
   * Internal initialization, protected by lock, includes internal guard to
   * protect against multiple initialization. This should be called before
   * using the hasWindowsJNI variable.
   */
  private static void initialize() {
    synchronized (lock) {
      if (initialized)
        return;
      initialized = true;
      String osname = System.getProperty("os.name", "generic");
      if (!osname.toLowerCase().contains("windows")) {
        trace(1, "Falling back to AWT FileDialog on non-windows platform " + osname);
        return;
      }
      String arch = System.getProperty("os.arch", "generic");
      String lib = arch.contains("64") ? "xfiledialog-x64" : "xfiledialog-x86";
      trace(1, "Attempting to load " + arch + " native library for " + osname + " platform");
      trace(2, "Searching java.library.path: " + System.getProperty("java.library.path", "(empty)"));

      try
      {
        System.loadLibrary(lib);
        int err = nativeWindowsInitialize(
            traceLevel,
            System.getProperty("java.home"));
        if (err != 0)
          throw new Exception("err " + err);
        hasWindowsJNI = true;
      } catch (UnsatisfiedLinkError e) {
        trace(1, "Could not load native library " + lib + ".dll");
        trace(1, "Falling back to AWT FileDialog due to library failure");
        if (traceLevel >= 2)
          e.printStackTrace();
      } catch (Exception e) {
        trace(1, "Could not initialize native library " + lib + ".dll: " + e.getMessage());
        trace(1, "Falling back to AWT FileDialog due to library failure");
        if (traceLevel >= 2)
          e.printStackTrace();
      }
    }
  }

  /**
   * Determine if native Windows support is available.
   */
  public static boolean hasNativeWindows() {
    initialize();
    return hasWindowsJNI;
  }

  /**
   * Construct an XFileDialog using the given parent.
   */
  public XFileDialog(Dialog parent)
  {
    this.parent = parent;
    this.mode = LOAD;
  }

  /**
   * Construct an XFileDialog using the given parent and title.
   */
  public XFileDialog(Dialog parent, String title)
  {
    this.parent = parent;
    this.title = title;
    this.mode = LOAD;
  }

  /**
   * Construct an XFileDialog using the given parent, title, and mode
   */
  public XFileDialog(Dialog parent, String title, int mode)
  {
    this.parent = parent;
    this.title = title;
    setMode(mode);
  }

  /**
   * Construct an XFileDialog using the given parent.
   */
  public XFileDialog(Frame parent)
  {
    this.parent = parent;
    this.mode = LOAD;
  }

  /**
   * Construct an XFileDialog using the given parent and title.
   */
  public XFileDialog(Frame parent, String title)
  {
    this.parent = parent;
    this.title = title;
    this.mode = LOAD;
  }

  /**
   * Construct an XFileDialog using the given parent, title, and mode
   */
  public XFileDialog(Frame parent, String title, int mode)
  {
    this.parent = parent;
    this.title = title;
    setMode(mode);
  }

  // Set the title for this load/save dialog.
  public void setTitle(String title) { this.title = title; }

  // Set the initial directory for this load/save dialog.
  public void setDirectory(String dir) { initialDir = dir; }

  // Set the initial file for this load/save dialog.
  public void setFile(String file) { initialFile = file; }

  // Enable or disable multi-file selection.
  public void setMultipleMode(boolean enable) { multiSelection = enable; }

  // Set the mode for this dialog.
  public void setMode(int mode) {
    if (mode != LOAD && mode != SAVE)
      throw new IllegalArgumentException("mode must be either LOAD or SAVE");
    this.mode = mode;
  }

  // Get a description of the current state of this load/save dialog.
  public String paramString() {
    if (state == 0) return "initializing";
    else if (state == 1) return "displaying";
    else return "closed";
  }

  // Get multi-file selection status.
  public boolean isMultipleMode() { return multiSelection; }

  // Get mode.
  public int getMode() { return mode; }

  // Get title.
  public String getTitle() { return title; }

  // Add to the list of filters that determine acceptable file names. For
  // Windows platforms, only ExtensionBasedFilter filters will be used, all
  // other filter objects will be ignored.
  public void addFilenameFilter(FilenameFilter filter) {
    if (filter == null)
      throw new NullPointerException("filter must not be null");
    filters.add(filter);
  }

  // Remove filter from list that determines acceptable file names.
  public boolean removeFilenameFilter(FilenameFilter filter) {
    return filters.remove(filter);
  }

  // Remove all filters from list that determines acceptable file names.
  public void resetFilenameFilters() {
    filters.clear();
  }

  // Get list that determines acceptable file names.
  public FilenameFilter[] getFilenameFilters() {
    FilenameFilter[] arr = new FilenameFilter[filters.size()];
    for (int i = 0; i < arr.length; i++)
      arr[i] = filters.get(i);
    return arr;
  }

  // Does nothing, but present for compatibility with java.awt.FileDialog.
  public void addNotify() { }

  // Enable or disable windows JNI.
  public void attemptNativeWindows(boolean enable) {
    attemptWindowsJNI = enable;
  }

  // setVisible(true) displays the dialog and blocks until the user closes the
  // dialog. setVisible(false) does nothing, but is present for compatibility
  // with java.awt.FileDialog.
  public void setVisible(boolean visible) {
    if (!visible)
      return;
    if (state != 0)
      throw new IllegalStateException("XFileDialog.setVisible(true) already invoked");

    if (attemptWindowsJNI)
      initialize();

    state = 1;

    if (hasWindowsJNI && attemptWindowsJNI) {
      // if (parent != null)
      //   parent.setIgnoreRepaint(false); // ???

      String defaultExtension = null;
      if (initialFile != null) {
        int i = initialFile.lastIndexOf('.');
        if (i > 0 && i < initialFile.length()-1)
          defaultExtension = initialFile.substring(i+1);
      }

      String[] ret = nativeWindowsFileDialog(
          traceLevel,
          parent,
          title,
          mode == LOAD,
          multiSelection,
          ExtensionBasedFilter.getWindowsDescription(getFilenameFilters()),
          defaultExtension,
          initialDir,
          initialFile);

      state = 2;
      if (ret != null && ret.length >= 2) {
        // dir, filename, filename2, ...
        resultDir = ret[0];
        resultFile = ret[1];
        resultFiles = new File[ret.length-1];
        for (int i = 1; i < ret.length; i++) {
          resultFiles[i-1] = new File(ret[0], ret[i]);
        }
      }

    } else {

      FileDialog dlg;
      if (parent instanceof Frame)
        dlg = new FileDialog((Frame)parent, title, mode);
      else
        dlg = new FileDialog((Dialog)parent, title, mode);

      dlg.setMultipleMode(multiSelection);
      if (initialDir != null)
        dlg.setDirectory(initialDir);
      if (initialFile != null)
        dlg.setFile(initialFile);
      if (filters.size() > 0)
        dlg.setFilenameFilter(new MultiFilter(getFilenameFilters()));

      dlg.setVisible(true);

      state = 2;
      // if (multiSelection) {
      resultFiles = dlg.getFiles();
      // } else {
      resultFile = dlg.getFile();
      // }
      resultDir = dlg.getDirectory();
    }

  }

  // Get result directory, if any, after setVisible(true), or original directory
  // if called before then.
  public String getDirectory() {
    return state < 2 ? initialDir : resultDir;
  }

  // Get result file, if any, after setVisible(true), or null if called before
  // then.
  public String getFile() {
    return resultFile;
  }

  // Get result array of zero or more result files, if any, after
  // setVisible(true), or empty array if called before then.
  public File[] getFiles() {
    if (resultFiles.length == 0)
      return resultFiles;
    File[] ret = new File[resultFiles.length];
    for (int i = 0; i < resultFiles.length; i++)
      ret[i] = resultFiles[i];
    return ret;
  }

  // Initialization for Windows native CFileDialog implementation.
  private static native int nativeWindowsInitialize(
      int traceLevel,
      String javaHome);

  // Entry point for Windows native CFileDialog implementation.
  private native String[] nativeWindowsFileDialog(
      int traceLevel,
      Window parent,
      String title,
      boolean isLoad,
      boolean isMulti,
      String filter,
      String extension,
      String initialDir,
      String initialFile);

  /**
   * ExtensionBasedFilter is a FilenameFilter that relies only on filename
   * extensions to determine acceptability. These can be used on Windows
   * platforms (using the native Windows load/save dialog) and on other
   * platforms (using java.awt.FileDialog). Other FilenameFilter implementations
   * are not supported with native Windows load/save dialogs.
   *
   * NOTE: It may be possible that Windows supports generic wildcard matching,
   * such as "filename.*" instead of only "*.ext", but currently only the latter
   * is implemented here.
   */
  public static interface ExtensionBasedFilter extends FilenameFilter {

    /**
     * Return a description of this filter suitable for use by the native
     * Windows file load/save dialog, for example,
     * "Image Files (*.png, *.jpg, *.jpeg)|*.png;*.jpg;*.jpeg"
     */
    public String getWindowsDescription();

    /**
     * Return a description of a list of filters suitable for use by the native
     * Windows file load/save dialog. This is a concatenation of each filter's
     * description, separated by "|", and ending with "||". For example:
     * "Image Files (*.jpg, *.jpeg)|*.jpg;*.jpeg|All Files (*.*)|*.*||"
     *
     * Only ExtensionBasedFilter objects in the list are used, all other
     * FilenameFilter objects in the list are ignored.
     *
     * If the list is empty or does not contain any ExtensionBasedFilter
     * objects, null is returned instead.
     */ 
    public static String getWindowsDescription(FilenameFilter... filters) 
    {
      String win = "";
      for (FilenameFilter f : filters) {
        if (!(f instanceof ExtensionBasedFilter))
          continue;
        win += ((ExtensionBasedFilter)f).getWindowsDescription() + "|";
      }
      return win.length() > 0 ? win + "|" : null;
    }

  } // end of ExtensionBasedFilter

  /**
   * Filter implements extension-based file name filtering.
   */
  public static class Filter implements ExtensionBasedFilter
  {
    protected String name;
    protected ArrayList<String> extensions;

    /**
     * Construct a Filter that accepts files with one of the given extensions,
     * or any directory.
     * @param name - a name for this filter, e.g. "Image Files".
     * @param extension - one or more extensions, e.g. "jpg", "png", "*". A
     * leading dot, if present, is removed.
     *
     * The extensions should not include wildcards or special characters, except
     * "*" may be used by itself to match all file extensions.
     *
     * If no extensions are given, or if "*" is given as an allowed extension,
     * then all files will be accepted.
     *
     * On Windows, typically file names are case insensitive and matching will
     * be done on a case insensitive basis by the native Windows load/save
     * dialog.
     *
     * On other platforms, this filter explicitly uses case insenstive matches
     * regardless of whether the underlying system uses case sensitive or case
     * insenstive file names.
     */
    public Filter(String name, String... extension)
    {
      this.name = name;

      extensions = new ArrayList<>();
      for (String ext : extension) {
        if (ext.startsWith("."))
          ext = ext.substring(1);
        extensions.add(ext);
      }
      if (extensions.size() == 0)
        extensions.add("*");
    }

    /**
     * Return the name of this filter, for example, "Image Files".
     */
    public String getName() {
      return name;
    }

    /**
     * Return a description of this filter, for example,
     * "Image Files (*.png, *.jpg, *.jpeg)".
     */
    public String getDescription() {
      String description = getName() + " (*." + extensions.get(0);
      for (int i = 1; i < extensions.size(); i++)
        description += ", *." + extensions.get(i);
      description += ")"; 
      return description;
    }

    /**
     * Return a description of this filter suitable for use by the native
     * Windows file load/save dialog, for example,
     * "Image Files (*.png, *.jpg, *.jpeg)|*.png;*.jpg;*.jpeg"
     */
    @Override
    public String getWindowsDescription() {
      String win = getDescription() + "|";
      win += "*."+extensions.get(0).toLowerCase();
      for (int i = 1; i < extensions.size(); i++)
        win += ";*." + extensions.get(i).toLowerCase();
      return win;
    }

    /**
     * Check if a given directory/file pair matches one of the allowed
     * extensions.
     * @param dir - The directory in which the file was found.
     * @param name - The name of the file.
     * @return true iff the name matches one fo the allowed extensions.
     */
    @Override
    public boolean accept(File dir, String name) {
      if (name == null || name.length() == 0)
        return true;
      File f = new File(dir, name);
      if (f.isDirectory())
        return true;
      String lname = name.toLowerCase();
      for (String ext : extensions) {
        if (ext.equals("*") || lname.endsWith("."+ext.toLowerCase()))
          return true;
      }
      return false;
    }

  } // end of Filter

  /**
   * MultiFilter implements filtering using a list of filters to do the actual
   * work. This is needed for platforms that only accept a single filter.
   */
  private static final class MultiFilter implements FilenameFilter
  {
    private FilenameFilter[] filters;
    private MultiFilter(FilenameFilter[] f) { filters = f; }
    @Override
    public boolean accept(File dir, String name) {
      for (FilenameFilter f : filters) {
        if (f.accept(dir, name))
          return true;
      }
      return false;
    }
  }

}

