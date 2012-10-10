#! /usr/bin/env python
'''
Runs gizmo with various java options.

This is intended to be platform independent and path independent.

Thus, you should be able to launch via its icon or run it from the
commandline on windows or any unixlike.
'''

import os, glob

EnableDebug = False
StackSize = '256M'
HeapSize = '1024M'


def main():

    # Memory options:
    args = ['java',
            '-Xms' + StackSize,
            '-Xmx' + HeapSize]

    # Debug options, if enabled:
    if EnableDebug:
        args.extend(
            ['-Xdebug',
             '-Xrunjdwp:transport=dt_shmem,address=jdbconn,server=y,suspend=n'])

    # Classpath and main class name:
    args.extend(['-cp', get_classpath(),
                 'gizmo.Gizmo'])
            
    # Run it:
    print 'Running:', ' '.join(args)
    os.execvp(args[0], args)


def get_classpath():
    '''
    Find the lib directory based on the path to this script, then find
    all jars inside, finally, append the build directory, then return
    the list as an appropriate classpath string for the current platform.
    '''
    opj = os.path.join # An alias.
    
    # Find the root directory:
    thismod = __import__(__name__)
    rootdir = os.path.dirname(thismod.__file__)

    # The lib and build subdirs:
    libdir = opj(rootdir, 'lib')
    builddir = opj(rootdir, 'build')

    # Create the classpath as a list:
    
    # Add the build directory jars:
    classpath = [opj(libdir, name) for name in os.listdir(libdir) if name.endswith('.jar')]

    # Add the build directory:
    classpath.append(builddir)

    # Join the list with the platforms path separator:
    return os.pathsep.join(classpath)


if __name__ == '__main__':
    main()
