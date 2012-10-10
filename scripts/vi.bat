@rem run cygwin's vim editor on the request
@set CYG=c:\cygwin
@set PATH=%PATH%;%CYG%\bin
@start /wait %CYG%\bin\vim '%BUF%'
