// Original Author: Stevpan@gmail.com
// Current Author: Kevin Walsh <kwalsh@holycross.edu>
//
// This code should be compiled with /UNICODE and /_UNICODE to enable UTF-16
// support in windows.

// JNI
#include <jni.h>

// Windows
#include <stdio.h>
#include <afxdlgs.h> 
#include <comdef.h>
#include <afxpriv.h>

// XFileDialog
#include "net_tomahawk_XFileDialog.h"

// Java AWT
#include <jawt.h>
#include <jawt_md.h>

// Multi-file selection requires a fixed buffer
#define MAX_MULTIFILE_SELECTION (100)


// Debugging
#define trace(lvl, ...) do {            \
  if (traceLevel >= (lvl)) {            \
    wprintf_s(_T("xfiledialog dll: ")); \
    wprintf_s(##__VA_ARGS__);           \
  }                                     \
} while (0)

HMODULE _hJAWT = NULL; // Handle to loaded jawt.dll module
JAWT awt; // Hook to Java AWT methods for obtaining drawing surfaces

// Convert java string object into plain unicode string
LPWSTR getString(JNIEnv *env, jstring jstr)
{
  // fixme, need to add null terminator unfortunately, b/c GetStringChars does not guarantee it
  if (jstr == NULL)
    return NULL;
  jsize len = env->GetStringLength(jstr);
  LPWSTR cstr = new WCHAR[len + 1]; // 1 for null termination
  const jchar *jbuf = env->GetStringChars(jstr, NULL);
  memcpy(cstr, jbuf, 2 * len);
  cstr[len] = 0x0000;
  env->ReleaseStringChars(jstr, jbuf);
  return cstr;
}

#define ERR_ALREADY_INITIALIZED (-8)
#define ERR_CANT_LOAD_JAWT_DLL (-7)
#define ERR_CANT_GET_JAWT_PROC (-6)
#define ERR_CANT_GET_AWT (-5)

JNIEXPORT jint JNICALL Java_net_tomahawk_XFileDialog_nativeWindowsInitialize
  (JNIEnv *env,
   jclass xfiledialog,
   jint traceLevel,
   jstring j_javaHome)
{
  if (_hJAWT)
    return ERR_ALREADY_INITIALIZED;

  LPCWSTR javaHome = getString(env, j_javaHome);
  CString jrePath(javaHome);
  jrePath += _T("\\bin");
  delete[] javaHome;

  trace(2, _T("Using JRE path: %s\n"), (LPCWSTR)jrePath);
  trace(1, _T("Attempting to load jawt.dll\n"));

  _hJAWT = LoadLibrary(jrePath + _T("\\jawt.dll")); // Java 1.4 and above

  if (_hJAWT == 0) {
    trace(1, _T("Attempting to load awt.dll as fallback\n"));
    _hJAWT = LoadLibrary(jrePath + _T("\\awt.dll")); // Java 1.3 and earlier
  }

  if (!_hJAWT) {
    trace(1, _T("Neither jawt.dll nor awt.dll could be loaded\n"));
    return ERR_CANT_LOAD_JAWT_DLL;
  }

  typedef jboolean (JNICALL *PJAWT_GETAWT)(JNIEnv*, JAWT*);
#ifdef OS_ARCH_X86
  PJAWT_GETAWT JAWT_GetAWT = (PJAWT_GETAWT)GetProcAddress(_hJAWT, "_JAWT_GetAWT@8");
#endif 
#ifdef OS_ARCH_X64
  PJAWT_GETAWT JAWT_GetAWT = (PJAWT_GETAWT)GetProcAddress(_hJAWT, "JAWT_GetAWT");
#endif 

  if (!JAWT_GetAWT) {
    trace(1, _T("Could not get proc address for JAWT_GetAWT() function\n"));
    FreeLibrary(_hJAWT);
    _hJAWT = NULL;
    return ERR_CANT_GET_JAWT_PROC;
  }

  awt.version = JAWT_VERSION_1_4;
  if (JAWT_GetAWT(env, &awt) == JNI_FALSE) {
    trace(1, _T("Could not get AWT info\n"));
    FreeLibrary(_hJAWT);
    _hJAWT = NULL;
    return ERR_CANT_GET_AWT;
  }

  trace(2, _T("native library is initialized\n"));
  return 0;
}

HWND GetWindowHandleFromAWT(JNIEnv *env, jint traceLevel, jobject j_parent)
{
  if (!_hJAWT) {
    trace(1, _T("jawt.dll was not loaded yet, or failed to load\n"));
    return NULL;
  }
  
  trace(2, _T("Obtaining native window handle\n"));
  
  HWND hWnd = NULL;

  JAWT_DrawingSurface* ds = NULL;
  jint lock = 0;
  JAWT_DrawingSurfaceInfo* dsi = NULL;
  JAWT_Win32DrawingSurfaceInfo* dsi_win = NULL;
    
  trace(3, _T("Get Drawing Surface\n"));
  ds = awt.GetDrawingSurface(env, j_parent);
  if (!ds) {
    trace(1, _T("could not obtain drawing surface for parent component\n"));
    goto out0;
  }
  
  trace(3, _T("Lock Drawing Surface\n"));
  lock = ds->Lock(ds);
  if ((lock & JAWT_LOCK_ERROR) != 0) {
    trace(1, _T("could not lock drawing surface for parent component\n"));
    goto out1;
  }

  trace(3, _T("Get Drawing Surface Info\n"));
  dsi = ds->GetDrawingSurfaceInfo(ds);
  if (!dsi) {
    trace(1, _T("could not obtain drawing surface info for parent component\n"));
    goto out2;
  }

  trace(3, _T("Get Window Handle\n"));
  dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
  if (!dsi_win) {
    trace(1, _T("could not obtain window info for parent component\n"));
    goto out3;
  }

  hWnd = dsi_win->hwnd;
  trace(2, _T("Obtained window handle %p for parent component\n"), hWnd);

out3:
  ds->FreeDrawingSurfaceInfo(dsi);
out2:
  ds->Unlock(ds);
out1:
  awt.FreeDrawingSurface(ds);
out0:
  return hWnd;
}

jstring toJavaString(JNIEnv *env, jint traceLevel, CString cstr)
{
  // DWORD len = cstr.GetLength();
  // const jchar *jbuf = new jchar[len+2]; // 1 for BOM, 1 for null terminator
  // memcpy(&jbuf[1], (LPCWSTR)cstr, 2*len); // 2 bytes per jchar and WCHAR
  // jbuf[0] = 0xfeff;
  // jbuf[len+1] = 0x0000;
  LPCWSTR buf = (LPCWSTR)cstr;
  return env->NewString((const jchar*)buf, cstr.GetLength());
}

JNIEXPORT jobjectArray JNICALL Java_net_tomahawk_XFileDialog_nativeWindowsFileDialog
  (JNIEnv *env,
   jobject obj,
   jint traceLevel,
   jobject j_parent,
   jstring j_title,
   jboolean isLoad,
   jboolean isMulti,
   jstring j_filter,
   jstring j_extension,
   jstring j_initialDir,
   jstring j_initialFile)
{
  trace(1, _T("Preparing CFileDialog\n"));
  
  CWnd *pParentWnd = NULL;
  if (j_parent != NULL) {
    HWND hParentWnd = GetWindowHandleFromAWT(env, traceLevel, j_parent);
    if (!hParentWnd) {
      trace(1, _T("Parent window not found. Ignoring error.\n"));
      //  return NULL;
    } else {
      pParentWnd = CWnd::FromHandle(hParentWnd); // temporary, freed automatically?
    }
  }

  LPCWSTR title = getString(env, j_title);
  LPCWSTR extension = getString(env, j_extension);
  LPCWSTR initialDir = getString(env, j_initialDir);
  LPCWSTR initialFile = getString(env, j_initialFile);
  LPCWSTR filter = getString(env, j_filter);

  trace(3, _T("Title [%s]\n"), title ? title : _T("(null)"));
  trace(3, _T("Extension [%s]\n"), extension ? extension : _T("(null)"));
  trace(3, _T("Initial Dir [%s]\n"), initialDir ? initialDir : _T("(null)"));
  trace(3, _T("Initial File [%s]\n"), initialFile ? initialFile : _T("(null)"));
  trace(3, _T("Filter [%s]\n"), filter ? filter : _T("(null)"));
  trace(3, _T("isLoad [%s]\n"), isLoad ? _T("TRUE") : _T("FALSE"));
  trace(3, _T("isMulti [%s]\n"), isMulti ? _T("TRUE") : _T("FALSE"));

  DWORD multiFlag = isMulti ? OFN_ALLOWMULTISELECT : 0;

  DWORD resultlen = isMulti ? MAX_PATH * MAX_MULTIFILE_SELECTION : MAX_PATH;
  LPWSTR result = new WCHAR[resultlen];
  if (initialFile)
    wcscpy_s(result, resultlen, initialFile);
  else
    result[0] = 0x0000;

  CFileDialog dlg(isLoad, extension, NULL /*initialFile*/,
      multiFlag | OFN_EXPLORER | OFN_HIDEREADONLY | OFN_ENABLESIZING | OFN_FILEMUSTEXIST | OFN_OVERWRITEPROMPT,
      filter, pParentWnd);

  dlg.m_ofn.lpstrFile = result;
  dlg.m_ofn.nMaxFile = resultlen;
  dlg.m_ofn.lpstrTitle = title;
  dlg.m_ofn.lpstrInitialDir = initialDir; 

  jobjectArray ret = NULL;

  if (dlg.DoModal() == IDOK) {

    CString *filenames;
    int count = 0;

    if (!isMulti) {

      count = 1;
      filenames = new CString[2];
      CString path = dlg.GetPathName();
      trace(3, _T("Path: %s\n"), (LPCWSTR)path);

      int sep = path.ReverseFind('\\');
      filenames[0] = path.Left(sep + 1);
      filenames[1] = path.Mid(sep+1);
      trace(3, _T("Result: %s\n"), (LPCWSTR)filenames[0]);
      trace(3, _T("Directory: %s\n"), (LPCWSTR)filenames[1]);

    } else {

      // With multi-selection, GetPathName() nromally returns a sequence of
      // null-terminated strings, the first of which is a directory, and the
      // rest are filenames that can be iterated over using GetStartPosition()
      // and GetNextPathName().

      filenames = new CString[MAX_MULTIFILE_SELECTION+1]; // 1 for dir
      POSITION pos = dlg.GetStartPosition();
      while (pos != NULL && count < MAX_MULTIFILE_SELECTION)
      {
        count++;
        filenames[count] = dlg.GetNextPathName(pos);
        trace(3, _T("Result[%d]: %s\n"), count, (LPCWSTR)filenames[count]);
      }
      CString dir = dlg.GetPathName(); // first of sequence is the directory
      trace(3, _T("Directory: %s\n"), (LPCWSTR)dir);

      if (count == 1) {
	// Selecting only one file with multi-selection, the first of the
	// sequence is the file path instead of the directory.  Correct this
	// here by removing the filename.
        int sep = dir.ReverseFind('\\');
        dir = dir.Left(sep + 1);
        trace(3, _T("Corrected directory: %s\n"), (LPCWSTR)dir);
      } else if (count > 1) {
        // Add separator to dir
        dir += _T("\\");
      }

      filenames[0] = dir;

    }

    if (count > 0) {
      jstring jdir = toJavaString(env, traceLevel, filenames[0]); // dir
      jclass strClass = env->GetObjectClass(jdir);
      ret = env->NewObjectArray(count+1, strClass, NULL);
      env->SetObjectArrayElement(ret, 0, jdir);
      for (int i = 0; i < count; i++) {
        jstring jname = toJavaString(env, traceLevel, filenames[i+1]); // filename
        env->SetObjectArrayElement(ret, i+1, jname);
      }
    }

    delete[] filenames;

  } else {
    // canceled
    trace(2, _T("Canceled by user\n"));
  }

  dlg.m_ofn.lpstrFile = NULL;
  dlg.m_ofn.lpstrTitle = NULL;
  dlg.m_ofn.lpstrInitialDir = NULL; 
  delete[] result;

  delete[] title;
  delete[] extension;
  delete[] initialDir;
  delete[] initialFile;
  delete[] filter;

  return ret;
}
