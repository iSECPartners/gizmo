@rem run the binary visual editor, http://bvi.sourceforge.net/, using cygwin
@set CYG=c:\cygwin
@set PATH=%PATH%;%CYG%\bin
@start /wait %CYG%\usr\local\bin\bvi '%BUF%'
