#!/usr/bin/env python
"""
Generate a page for performing manual retesting of post parameters.
"""

import os, sys
from urllib import unquote, quote
import Tkinter

class Error(Exception) : pass

def readfile(fn) :
    f = file(fn, 'rb')
    d = f.read()
    f.close()
    return d

def writefile(fn, d) :
    f = file(fn, 'wb')
    f.write(d)
    f.close()

def writefile(fn, d) :
    f = file(fn, 'wb')
    f.write(d)
    f.close()

def decParam(pstr) :
    if '=' in pstr :
        k,v = pstr.split('=', 1)
        return k, unquote(v)
    return [pstr,""]

def decHeader(hstr) :
    if ':' in hstr :
        n,v = hstr.split(':', 1)
        return n.lower(),v
    return [hstr.lower(), ""]

def decHeaders(hstr) :
    return [decHeader(l) for l in hstr.split('\r\n')]

def encParam(n, v) :
    if v :
        # quote is rather tame here, consider something heavier.
        return '%s=%s' % (n, quote(v))
    return n
    
def encParams(ps) :
    return '&'.join(encParam(n,v) for n,v in ps)

def alookup(key, assoc) :
    for k,v in assoc :
        if k == key :
            return v

def decParams(pstr) :
    """Decode a string into an assoc list of parameters."""
    return [decParam(p) for p in pstr.split('&')]

def getReq(r) :
    if '\r\n' not in r :
        raise Error("No lines in request!")
    l0,rest = r.split('\r\n', 1)
    l0 = filter(None, l0.split(' '))
    if len(l0) != 3 :
        raise Error("Bad request line")
    meth,path,proto = l0
    if '\r\n\r\n' not in r :
        raise Error("No end of header")
    hdr,body = r.split('\r\n\r\n', 1)
    hdr = decHeaders(hdr)
    host = (alookup('host', hdr) or 'unknownhost').strip()
    # XXX  we dont know http/https based on req!
    #url = 'https://%s%s' % (host, path)
    url = 'http://%s%s' % (host, path)
    return url,meth,path,proto,hdr,body


class ParamGUI(object) :
    def __init__(self, ps) :
        self.params = ps
        self.accept = False
        self.initGui()
        self.root.mainloop()

    def _accept(self, *args) :
        ps = [(n,e.get()) for (n,v),e in zip(self.params, self.entries)]
        self.params = ps
        self.accept = True
        self.root.quit()

    def initGui(self) :
        self.root = Tkinter.Tk()
        es = []
        f = Tkinter.Frame(self.root)
        for cnt,(n,v) in enumerate(self.params) :
            lab = Tkinter.Label(f, text=n)
            lab.grid(row=cnt, column=0)
            e = Tkinter.Entry(f)
            e.insert(0, v)
            es.append(e)
            e.grid(row=cnt, column=1)
        f.pack()

        but = Tkinter.Button(self.root, text="Accept Changes", command=self._accept)
        but.pack()
        self.entries = es

def main() :
    if len(sys.argv) <= 1 :
        reqfn = os.environ.get('BUF')
    else :
        if len(sys.argv) != 2 :
            print "usage: %s request" % (sys.argv[0],)
            return
        reqfn = sys.argv[1]

    d = readfile(reqfn)
    url,meth,path,proto,hdr,body = getReq(d)
    if meth.upper() != 'POST' or not body :
        if '?' not in path :
            raise Error("No parameters found")
        pstr = path.split('?',1)[1]
        if '#' in path :
            pstr = path.split('#',1)[0]
    else :
        pstr = body
    #print pstr

    ps = decParams(pstr)

    g = ParamGUI(ps)
    if g.accept :
        pstr2 = encParams(g.params)
        d2 = d.replace(pstr, pstr2)
        writefile(reqfn, d2)
        #print "wrote"
    else :
        #print "cancel"
        pass

if __name__ == '__main__' :
    try :
        main()
    except Error,e :
        print e
    except Exception,e :
        print 'unhandled', e

