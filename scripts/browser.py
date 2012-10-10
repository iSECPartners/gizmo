#!/usr/bin/env python
"""
Invoke the browser on a local file.

Ugh, this kinda sucks.  It should be as simple as
"cmd /c fn" but fn has to have exec perms first and I cant
do that easily from python!

If this isn't suitable for your system, copy this file into
the gizmo root and edit it.
"""

import os, sys

browserPath = """c:\\Progra~1\\Intern~1\\IEXPLORE.EXE"""
#browserPath = """c:\\Progra~1\\Mozill~1\\firefox.exe"""
#browserPath = """firefox3"""

def fixpath(fn) :
    afn = fn
    if ':' in fn :
        scheme = fn.split(':', 1)[0].lower()
        if scheme not in ('file','http','https') :
            return 'file://' + os.path.abspath(fn)
    else :
        return 'file://' + os.path.abspath(fn)
    return fn

def run(cmd) :
    if os.system(cmd) :
        print "command failed: %s" % cmd

def browser(fn) :
    run('%s "%s"' % (browserPath, fixpath(fn)))

if __name__ == "__main__" :
    map(browser, sys.argv[1:])

