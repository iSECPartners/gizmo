#!/usr/bin/env python
"""
Encode base64.
http://en.wikipedia.org/wiki/Base64

see also: de64.py
"""

import os, sys
from optparse import OptionParser

def readfile(fn) :
    f = file(fn, 'r')
    d = f.read()
    f.close()
    return d
    
def fix(op, x) :
    if op.repl :
        for (frm,to) in zip(op.repl, '+/=') :
            x = x.replace(frm, to)
    if not op.multi :
        x = x.replace('\n', '')
        x = x.replace('\r', '')
        x = x.replace(' ', '')
    if op.url :
        x = x.rstrip('=')
    return x

def decode(op, x) :
    return fix(op, x.encode('base64'))
    
def getopts() :
    p = OptionParser(usage='%prg [options] vals...')
    p.add_option('-u', dest='url', action='store_true', default=False,
            help='Modified URL encoding')
    p.add_option('-r', dest='regx', action='store_true', default=False,
            help='Modified regexp encoding')
    p.add_option('-f', dest='fname', action='store_true', default=False,
            help='Modified filename encoding')
    p.add_option('-m', dest='repl', action='store', default=None,
            help='Specify modification characters.')
    p.add_option('-M', dest='multi', action='store_true', default=False,
            help='Return as multi-line text.')
    op,args = p.parse_args()

    if op.repl and len(op.repl) != 3 :
        p.error("specify three replacement characters for +/= with m flag")
    if int(op.repl is not None) + int(op.url) + int(op.regx) + int(op.fname) > 1 :
        p.error("specify only one of fmru flags.")
    if op.url :
        os.repl = '-_='
    elif op.regx :
        os.repl = '!-='
    elif op.fname :
        os.repl = '+-='

    op.args = args
    r = os.environ.get('BUF')
    if not op.args and r :
        op.args = [r]
    return op

def main() :
    op = getopts()
    for a in op.args :
        print decode(op, readfile(a))

if __name__ == '__main__' :
    main()
