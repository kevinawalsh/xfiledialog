rem Author: stevpan@gmail.com 
rem JDK 6 and Visual Studio 2008 (or higher) are required.


rem Before running this batch file, 
rem you need to setup vs2008 enviroment at first
rem c:\vs2008\VC\bin\vcvars32.bat


rem clean old build
del xfiledialog.dll
del *.class 

javac src_java\net\tomahawk\*.java
javac -cp .\src_java helloworld.java
javac -cp .\src_java helloapplet.java

cd src_java
jar cvf ..\xfiledialog.jar net
cd ..


rem clean vim editor's temp files 
rem
del src_cpp\*~ 
del src_java\net\tomahawk\*~

rem please change the include directory according to your 
rem JDK home and Visual studio home in the following command

cl  /D UNICODE /D _UNICODE /D _CRT_NON_CONFORMING_SWPRINTFS /D OS_ARCH_X86 -I "d:\Program Files\Java\jdk1.6.0_16\include" -I "d:\Program Files\Java\jdk1.6.0_16\include\win32" -I "c:\vs2008\VC\atlmfc\include" /W3 /O1 /LD /EHsc -Fexfiledialog.dll src_cpp\*.cpp 

rem clean temp files in compilation 
del *.exp
del *.lib
del *.obj

