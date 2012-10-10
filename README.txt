Gizmo -- A Graphical HTTP(S) Proxy
==================================

Proxy Configuration (IMPORTANT, READ THIS BIT)
----------------------------------------------

In IE, you have to configure the HTTP and HTTPS proxy settings and nothing
else. In IE 7 (and possibly IE 6, i havent looked yet), there's a
coarse-grained "proxy all connections through the following proxy" checkbox
that causes local proxies to go completely haywire. What happens is this:
IE sends HTTP requests to the proxy. The proxy (via Java) makes an outbound
socket connection. Java proxies socket connections through the proxy
settings (configured via IE), so the proxy makes a connection to itself.
This tailchasing behavior for local proxy settings makes the proxy hang and
appear not to work. In IE, the answer to this is to make sure that *only*
HTTP and HTTPS proxy settings are set. In Firefox, you should make sure that
IE proxy settings *aren't* configured, and that Socks and FTP proxying
aren't configured.

Gizmo source can be found in subversion in java/apps/gizmo_trunk. Building
requires ant and Java 6.0 (Java versions earlier than will not work). To
build, you just type 'ant' (no targets) in the top-level directory. This
will build the current version of the source, and put a jar containing the
application in the right place. To run on Windows you run 'gizmo.bat'. All
this does is run the jar and set the classpath. 

You may also need to set %JAVA_HOME%. You might have to do it like this:

      set JAVA_HOME=c:\progra~1\java\jdk1.6.0_02

You can run Gizmo on Unix by setting the same environment variables and porting the 
little gizmo.bat to a Unix shell script (it's just a one line batch file).

