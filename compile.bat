@rem Tested using Adoptium's JDK 17 and Visual Studio Community 2022.

@rem Before running this batch file, you need to change the file paths below to
@rem match your java and Visual Studio install directories. Additionally, you
@rem must configure your windows command shell for compiling for x86 using
@rem cl.exe, e.g. by running:
@rem "C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\Tools\VsDevCmd.bat"

@rem set JAVA_HOME=C:\Program Files\AdoptOpenJDK\temurin-17.0.7_7-hotspot\
@rem set VS_VARS=C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\Tools\VsDevCmd.bat
@set VS_VARS=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\
@set VS_MSVC=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.36.32532\atlmfc\

@echo *
@echo ==========================
@echo XFileDialog Compile Script
@echo ==========================
@echo JAVA_HOME: %JAVA_HOME%
@echo VS_VARS : %VS_VARS%
@echo VS_MSVC : %VS_MSVC%
@echo ==========================

@echo *
@echo Clean build artifacts and editor temp files
@del *.dll 2>nul
@del *.exp 2>nul
@del *.lib 2>nul
@del *.obj 2>nul
@del *.class  2>nul
@del src_cpp\net_tomahawk_XFileDialog.h 2>nul
@del src_java\net\tomahawk\*.class 2>nul

@del *~ 2>nul
@del src_cpp\*~  2>nul
@del src_java\net\tomahawk\*~ 2>nul
@del *.swp 2>nul
@del src_cpp\*.swp  2>nul
@del src_java\net\tomahawk\*.swp 2>nul

@echo *
@echo Compile java library
@javac src_java\net\tomahawk\*.java
@javac -h src_cpp src_java\net\tomahawk\*.java

@echo *
@echo Package jar
@cd src_java
@jar cvf ..\xfiledialog.jar net
@cd ..

@echo *
@echo Compile java example
@javac -cp xfiledialog.jar;. Example.java

@echo *
@echo Compile x86 DLL
@call "%VS_VARS%\vcvars32.bat"
@cl /D UNICODE /D _UNICODE /D OS_ARCH_X86 /D _WIN32_WINNT=0x0601 -I "%JAVA_HOME%\include" -I "%JAVA_HOME%\include\win32" -I "%VS_MSVC%\include" /W3 /O1 /LD /EHsc -Fexfiledialog-x86.dll src_cpp\*.cpp /link /LIBPATH:"%VS_MSVC%\lib\x86"

@echo *
@echo Compile x64 DLL
@call "%VS_VARS%\vcvars64.bat"
@cl /D UNICODE /D _UNICODE /D OS_ARCH_X64 /D _WIN32_WINNT=0x0601 -I "%JAVA_HOME%\include" -I "%JAVA_HOME%\include\win32" -I "%VS_MSVC%\include" /W3 /O1 /LD /EHsc -Fexfiledialog-x64.dll src_cpp\*.cpp /link /LIBPATH:"%VS_MSVC%\lib\x64"

@echo *
@echo Clean temp build artifacts
@del *.exp 2>nul
@del *.lib 2>nul
@del *.obj 2>nul

