rem Tested using Adoptium's JDK 17 and Visual Studio Community 2022.

rem Before running this batch file, you need to change the file paths below to
rem match your java and Visual Studio install directories. Additionally, you
rem must configure your windows command shell for compiling for x64 using
rem cl.exe, e.g. by running:
rem "C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\Tools\VsDevCmd.bat" -arch=arm64

rem Clean build artifacts and editor temp files
del xfiledialog64.dll xfiledialog64.exp xfiledialog64.lib xfiledialog64.obj
del *.class 
del src_java\net\tomahawk\*.class
del *~
del src_cpp\*~ 
del src_java\net\tomahawk\*~

rem Compile java code
javac src_java\net\tomahawk\*.java
javac -cp .\src_java helloworld.java
javac -cp .\src_java helloapplet.java

rem Package jar
cd src_java
jar cvf ..\xfiledialog.jar net
cd ..

rem Please change the include directory according to your JDK home and Visual
rem Studio home in the following command:

cl /D UNICODE /D _UNICODE /D OS_ARCH_X64 /D _WIN32_WINNT=0x0601 -I "C:\Program Files\AdoptOpenJDK\temurin-17.0.7_7-hotspot\include" -I "C:\Program Files\AdoptOpenJDK\temurin-17.0.7_7-hotspot\include\win32" -I "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.36.32532\atlmfc\include" /W3 /O1 /LD /EHsc -Fexfiledialog64.dll src_cpp\*.cpp /link /LIBPATH:"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.36.32532\atlmfc\lib\x64"

rem Clean temp build artifacts
del xfiledialog64.exp xfiledialog64.lib xfiledialog64.obj

