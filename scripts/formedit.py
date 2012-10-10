#!/usr/bin/env python
"""
Generate a page for performing manual retesting of post parameters.
"""

import os, sys
from urllib import unquote

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

def genPage(url, meth, ps) :
    ls = []
    ls.append("""
<html>
<head>
  <title>Manual Post Request</title>
</head>
<body>
<h1>Manual Post Request</h1>
""")

    ls.append("<form action='%s' method='%s'>" % (url, meth))
    ls.append(' <input type=submit>')
    ls.append(' <table>')
    for n,v in ps : 
        ls.append('  <tr><td>%s:</td><td><input type=text name="%s" value="%s" size=80></td>' % (n,n,v))
    ls.append(' </table>')
    ls.append(' <input type=submit>')
    ls.append("</form>")
    ls.append("</html>")
    return '\n'.join(ls) + '\n'

def main() :
    if len(sys.argv) <= 1 :
        reqfn = os.environ.get('BUF')
    else :
        if len(sys.argv) != 2 :
            print "usage: %s request" % (sys.argv[0],)
            return
        reqfn = sys.argv[1]

    url,meth,path,proto,hdr,body = getReq(readfile(reqfn))
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
    pg = genPage(url, meth, ps)
    writefile("manual.html", pg)
    print "opening browser..."
    os.system("browser.py manual.html")

if __name__ == '__main__' :
    try :
        main()
    except Error,e :
        print e
    except Exception,e :
        print 'unhandled', e

