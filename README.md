# xfiledialog
Native windows file load/save dialog for Java/Swing.

`net.tomahawk.XFileDialog` is a light-weight Java binding for native file
load/save dialog on Windows platforms (using JNI), with a fallback to AWT
dialogs on other platforms. This is is meant to provide a workaround for a
longstanding weakness in Java's load/save dialog support.

This code is inspired by the XFileDialog project by stevpan@gmail.com, which can
be found at
[https://code.google.com/archive/p/xfiledialog/](https://code.google.com/archive/p/xfiledialog/). 
That code has not been updated in many years, it uses a different API, and it
has a different fallback mechanism for non-Windows platforms.

## Usage

Put the `xfiledialog.jar` file in your `CLASSPATH`, and put the two `dll` files
where they can be found by Java's `System.loadLibrary()`. Then use
`net.tomahawk.XFileDialog` in just about the same way as `java.awt.FileDialog`.
For most cases, `XFileDialog` should be a near drop-in replacement for
`FileDialog`.

    JFrame parent = ...;

    // Configure and show a file save dialog
    XFileDialog dlg = new XFileDialog(parent, "Choose Destination");
    dlg.setMode(XFileDialog.SAVE);
    dlg.setDirectory(...); // optional: initial directory for dialog
    dlg.setFile("MyNewThing.jpg"); // optional: suggest a file name
    dlg.setVisible(true);

    // Get the user's chosen directory and filename. The
    // file name will be null if the user cancel's the dialog.
    String dir = dlg.getDirectory();
    String file = dlg.getFile();

    // Alternatively, get an array containing a list of full
    // paths. This will have length zero or one, depending on
    // whether the user cancels the dialog.
    File[] files = dlg.getFiles();

When using `LOAD` instead of `SAVE`, you can optionally allow the user to select
multiple files:

    dlg.setMode(XFileDialog.LOAD);
    dlg.setMultipleMode(true);

You can use filename filters and, crucially, for `net.tomahawk.XFileDialog`
these work on all platforms including Windows. This is in contrast to
`java.awt.FileDialog` where filename filters are not implemented for Windows
platforms.

    // The first added filter will be selected by default on Windows,
    // and the user can select among them using a dropdown menu.
    dlg.addFilenameFilter(new XFileDialog.FilterByExtension("PNG Images", "png"));
    dlg.addFilenameFilter(new XFileDialog.FilterByExtension("JPG Images", "jpg", "jpeg"));
    dlg.addFilenameFilter(new XFileDialog.FilterByExtension("All Files", "*"));

See the API below or `Example.java` for more details.

## Requirements

`XFileDialog` should work on Windows 7 or above (and possibly earlier) using the
native Windows dialog, and it should work on any other platform using the
fallback AWT dialog. It was designed for Java 17 or above, though it could be
easily compiled for earlier versions with only minor adjustments.

## Compiling

To compile the project on Windows using JDK 17 and Visual Studio Community 2022
or similar, first edit the paths at the top of `compile.bat`, then run this
batch file. Check the output manually for possible compile errors.

The `compile.bat` script should produce the three release binaries:

* `xfiledialog.jar` - contains the class files for `XFileDialog`
* `xfiledialog-x86.dll` - native library for 32-bit Windows platforms
* `xfiledialog-x64.dll` - native library for 64-bit Windows platforms

## API

```java
    public class XFileDialog {

      static final int LOAD; // Same as java.awt.FileDialog.LOAD;
      static final int SAVE; // Same as java.awt.FileDialog.SAVE;
 
      /**
       * Set debug tracing level. Use 0 to disable all debug printing. Higher
       * numbers enable more debug printing.
       */
      public static void setTraceLevel(int val);
    
      /**
       * Determine if native Windows support is available.
       */
      public static boolean hasNativeWindows();
    
      /**
       * Construct an XFileDialog using the given parent.
       */
      public XFileDialog(Dialog parent);
    
      /**
       * Construct an XFileDialog using the given parent and title.
       */
      public XFileDialog(Dialog parent, String title);
    
      /**
       * Construct an XFileDialog using the given parent, title, and mode
       */
      public XFileDialog(Dialog parent, String title, int mode);
    
      /**
       * Construct an XFileDialog using the given parent.
       */
      public XFileDialog(Frame parent);
    
      /**
       * Construct an XFileDialog using the given parent and title.
       */
      public XFileDialog(Frame parent, String title);
    
      /**
       * Construct an XFileDialog using the given parent, title, and mode
       */
      public XFileDialog(Frame parent, String title, int mode);
    
      // Set the title for this load/save dialog.
      public void setTitle(String title);
    
      // Set the initial directory for this load/save dialog.
      public void setDirectory(String dir);
    
      // Set the initial file for this load/save dialog.
      public void setFile(String file);
    
      // Enable or disable multi-file selection.
      public void setMultipleMode(boolean enable);
    
      // Set the mode for this dialog.
      public void setMode(int mode);
    
      // Get a description of the current state of this load/save dialog.
      public String paramString();
    
      // Get multi-file selection status.
      public boolean isMultipleMode();
    
      // Get mode.
      public int getMode();
    
      // Get title.
      public String getTitle();
    
      // Add to the list of filters that determine acceptable file names. For
      // Windows platforms, only FilterByExtension filters will be used, all other
      // filter objects will be ignored.
      public void addFilenameFilter(FilenameFilter filter);
    
      // Remove filter from list that determines acceptable file names.
      public boolean removeFilenameFilter(FilenameFilter filter);
    
      // Remove all filters from list that determines acceptable file names.
      public void resetFilenameFilters();
    
      // Get list that determines acceptable file names.
      public FilenameFilter[] getFilenameFilters();
    
      // Does nothing, but present for compatibility with java.awt.FileDialog.
      public void addNotify();
    
      // Enable or disable windows JNI.
      public void attemptNativeWindows(boolean enable);
    
      // setVisible(true) displays the dialog and blocks until the user closes the
      // dialog. setVisible(false) does nothing, but is present for compatibility
      // with java.awt.FileDialog.
      public void setVisible(boolean visible);
    
      // Get result directory, if any, after setVisible(true), or original directory
      // if called before then.
      public String getDirectory();
    
      // Get result file, if any, after setVisible(true), or null if called before
      // then.
      public String getFile();
    
      // Get result array of zero or more result files, if any, after
      // setVisible(true), or empty array if called before then.
      public File[] getFiles();
    
      /**
       * FilterByExtension implements extension-based file name filtering. This
       * should work on Windows platforms (using the native Windows load/save
       * dialog) and on other platforms (using java.awt.FileDialog). Other
       * FilenameFilter implementations will not work on Windows platforms, as they
       * support only extension-based filtering.
       *
       * NOTE: It may be possible that Windows supports generic wildcard matching,
       * such as "filename.*" instead of only "*.ext", but currently only the latter
       * is implemented here.
       */
      public static final class FilterByExtension implements FilenameFilter
      {
    
        /**
         * Construct a FilenameFilter that accepts files with one of the given
         * extensions.
         * Any directory is also accepted.
         * @param name - a name for this filter, e.g. "Image Files".
         * @param extension - one or more extensions, e.g. "jpg", "png", "*".
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
        public FilterByExtension(String name, String... extension);
    
        /**
         * Return the name of this filter, for example, "Image Files".
         */
        public String getName()
    
        /**
         * Return a description of this filter, for example,
         * "Image Files (*.png, *.jpg, *.jpeg)".
         */
        public String getDescription();
    
        /**
         * Return a description of this filter suitable for use by the native
         * Windows file load/save dialog, for example,
         * "Image Files (*.png, *.jpg, *.jpeg)|*.png;*.jpg;*.jpeg"
         */
        public String getWindowsDescription();
    
        /**
         * Check if a given directory/file pair matches one of the allowed
         * extensions.
         * @param dir - The directory in which the file was found.
         * @param name - The name of the file.
         * @return true iff the name matches one fo the allowed extensions.
         */
        @Override
        public boolean accept(File dir, String name);
    
        /**
         * Return a description of a list of filters suitable for use by the native
         * Windows file load/save dialog. This is a concatenation of each filter's
         * description, separated by "|", and ending with "||". For example:
         * "Image Files (*.jpg, *.jpeg)|*.jpg;*.jpeg|All Files (*.*)|*.*||"
         *
         * Only FilterByExtension objects in the list are used, all other
         * FilenameFilter objects in the list are ignored.
         *
         * If the list is empty or does not contain any FilterByExtension objects,
         * null is returned instead.
         */ 
        public static String getWindowsDescription(FilenameFilter... filters);
    
      }
    
    }
```

## Alternatives to XFileDialog

* Java's basic `java.awt.FileDialog` works well enough on MacOS and Linux
  platforms, providing access to native load/save dialogs. But on Windows
  platforms, `java.awt.FileDialog` does not support filename filters
  (`getFilenameFilter()`, `setFilenameFilter()`). Additionally, in 2023, the AWT
  ecosystem appears to have been essentially abandoned, so it is unlikely this
  approach will be improved in future.

* Java's Swing support for load/save dialogs, using `javax.swing.JFileChooser`,
  is more flexible and complete on all three platforms, but provides a sub-par
  interface far removed from the native load/save dialogs. Keyboard
  shortcuts/auto-complete barely work, keyboard focus does not follow platform
  conventions, etc. Swing has not been meaningfully improved in many years.

* JavaFX or SWT (Eclipse Foundation's Standard Widget Toolkit) may provide
  better load/save dialogs, but introduce large dependencies for projects that
  don't otherwise rely on those projects, requiring more configuration,
  packaging, and deployment effort.

## Credits and License

This project is maintained by Kevin Walsh <kwalsh@holycross.edu> and is released
under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

This project is a fork of the XFileDialog project by stevpan@gmail.com, which
can be found at
[https://code.google.com/archive/p/xfiledialog/](https://code.google.com/archive/p/xfiledialog/). 

Note: The original project page has somewhat confusing licensing. The readme.txt
file explicitly releases the software into the public domain, and no other
licensing restrictions are mentioned elsewhere in the repository itself. But the
[project page](https://code.google.com/archive/p/xfiledialog/) also lists Apache
License 2.0 in the sidebar. In light of this ambiguity, I have chosen to retain
the Apache License 2.0.

