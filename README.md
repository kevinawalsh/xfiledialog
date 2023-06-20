# xfiledialog
Native windows file load/save dialog for Java/Swing

XFileDialog is a simple and light-weight Java binding for native file load/save
dialog on Windows platforms. This is is meant to provide a workaround for a
longstanding weakness in Java's load/save dialog support.

Alternatives to XFileDialog:

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

Credits and License:

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

