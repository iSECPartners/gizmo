/*
Copyright (C) 2009 Rachel Engel

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.isecpartners.gizmo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import cybervillains.ca.KeyStoreManager;
import com.isecpartners.gizmo.HttpResponse.FailedRequestException;

class HttpRequest extends HTTPMessage {

    private static int global_request_num = 0;
    private static char[] pass = "lalalala".toCharArray();
    private boolean canWakeUp = false;
    private String interrimContents;
    private String host;
    private boolean override_host = false;
    private String version;
    private boolean cached;
    private boolean isSSL = false;
    private int request_num = global_request_num++;
    HttpResponse resp;
    static SSLSocketFactory sslFactory;
    static SSLServerSocketFactory sslServerFactory;
    private Socket sock;
    private Socket outboundSock;
    private boolean sent = false;
    private static Hashtable<String, SSLSocketFactory> _factories = new Hashtable<String, SSLSocketFactory>();
    private StringBuffer workingContents = new StringBuffer();
    private int port = 80;
    private boolean user_defined_port = false;
    private boolean connect_protocol_handled = false;
    private String url = "";
    private boolean passthroughssl = false;
    private final String ACCEPT_ENCODING = "ACCEPT-ENCODING";

    public void setClientSocket(Socket sock) {
        this.sock = sock;
    }

    public Socket getClientSocket() {
        return this.sock;
    }

    public boolean passthroughssl() {
        return passthroughssl;
    }

    public HttpRequest clone() {
        HttpRequest ret = createRequest();
        ret.interrimContents = this.interrimContents;
        ret.host = this.host;
        ret.version = this.version;
        ret.isSSL = this.isSSL;
        ret.workingContents = this.workingContents;
        ret.url = this.url;
        ret.addContents(this.workingContents.toString());
        ret.header = header;
        return ret;
    }

    public String getURL() {
        String tmp = this.makeURL(workingContents);
        if (GizmoView.getView().intercepting()) {
            if (tmp.contains("//")) {
                tmp = tmp.substring(tmp.indexOf("/", tmp.indexOf("//") + 2));
            }
        }
        if (url.contains("?")) {
            tmp = tmp.substring(0, tmp.indexOf("?"));
        } else {
            tmp = tmp.substring(0, tmp.lastIndexOf("/"));
        }
        return (isSSL ? "https" : "http") + "://" + host + tmp + "/";
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

    boolean isSSL() {
        return isSSL;
    }

    void setHost(String host) {
        this.host = host;
        if (host != null && host != "" && host.contains(":")) {
			this.host = host.substring(0, host.indexOf(":"));
			this.port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
		}
        override_host = true;
    }

    void setSSL(boolean ssl) {
        this.isSSL = ssl;
        if (ssl)
            this.port = 443;
    }


    private String makeURL(StringBuffer workingContents) {
        String ret = "";
        if (header.contains("http")) {
            ret = mk_header(workingContents).substring(header.indexOf("http"), header.indexOf("HTTP") - 1);
        } else {
            ret = mk_header(workingContents).trim().substring(header.indexOf("/"), header.indexOf("HTTP") - 1);
        }
        return ret;
    }

    private String getUrlPath(String header) {
        if (header == null) {
            return "";
        }
        String tmp = header;
        String path = tmp.substring(tmp.indexOf(" ") + 1, tmp.lastIndexOf("HTTP")).trim();
        return path;
    }

    private String mk_header(StringBuffer workingContents) {
        return workingContents.substring(0, workingContents.indexOf("\r\n"));
    }

    private String[] parse_request_line(String header) {
        String pieces[] = header.trim().split(" ");
        return pieces;
    }

    private void readerToStringBuffer(DataInputStream buffered, StringBuffer contents) {

        try {

            byte ch_n = buffered.readByte();

            while (ch_n != -1) {
                contents.append((char) ch_n);

                if ((contents.indexOf("\r\n\r\n") != -1) || (buffered.available() == 0)) {
                    break;
                }
                ch_n = buffered.readByte();
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private String rewriteMethodLine(StringBuffer workingContents) {
        int host_index = workingContents.toString().toUpperCase().indexOf("HOST:");

        if (host_index != -1) {
            int host_line_end_index = workingContents.toString().indexOf("\r\n", host_index);

            host = workingContents.toString().substring(host_index + 6, host_line_end_index).trim();
        } else {
            this.url = makeURL(workingContents);

            host = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
        }
        if (host.contains(":")) {
            port = Integer.parseInt(host.substring(host.indexOf(":") + 1));
            user_defined_port = true;
            host = host.substring(0, host.indexOf(":"));
        }

        final String GET_HTTP = "^[a-zA-Z]+\\s+[hH][tT][tT][pP].*";

        if (header.matches(GET_HTTP)) {
            String tmp = workingContents.toString().substring(0, workingContents.indexOf("\r\n")).trim() + "\r\n";

            int start = tmp.indexOf("http://") + 8;
            String prefix = tmp.substring(0, start - 8);
            int end = tmp.indexOf("/", start);
            String postfix = tmp.substring(end, tmp.length());
            String whole = prefix + postfix;
            workingContents.replace(0, tmp.length(), whole);
        }

        return host;
    }

    public boolean isSent() {
        return this.sent;
    }

    void setInterrimContents(StringBuffer stringBuffer) {
        this.interrimContents = stringBuffer.toString();
        workingContents = new StringBuffer(this.interrimContents.toString());
        this.header = workingContents.substring(0, workingContents.indexOf("\r\n"));
        this.addContents(interrimContents);
    }

    void wakeupAndSend() {
        canWakeUp = true;
        synchronized (this) {
            this.notifyAll();
        }
    }

    public boolean awaitWakeup() throws InterruptedException {
        if (!canWakeUp) {
            synchronized (this) {
                this.wait();
            }
        }

        return canWakeUp;
    }

    private KeyManagerFactory createKeyManagerFactory(String cname) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, InvalidKeyException, SignatureException, NoSuchProviderException, NoCertException {
        X509Certificate cert = KeyStoreManager.getCertificateByHostname(cname);
        cybervillains.ca.KeyStoreManager.getCertificateByHostname(cname);

        if (cert == null) {
            throw new NoCertException();
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, pass);

        ks.setCertificateEntry(cname, cert);
        ks.setKeyEntry(cname, KeyStoreManager.getPrivateKeyForLocalCert(cert), pass, new X509Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, pass);

        return kmf;
    }

    private String getVersion(String header) {
        int begin = header.indexOf("HTTP/") + 5;
        return header.substring(begin).trim();
    }

    private SSLSocketFactory sloppySSL() {
        SSLUtilities.trustAllHostnames();
        return SSLUtilities.trustAllHttpsCertificates();
    }

    private synchronized SSLSocketFactory initSSL(String hostname) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, IOException, KeyManagementException, InvalidKeyException, SignatureException, NoSuchProviderException, NoCertException {

        KeyManagerFactory kmf = null;
        SSLContext sslcontext = null;

        kmf = createKeyManagerFactory(hostname);
        sslcontext = SSLContext.getInstance("SSLv3", Security.getProvider("BC"));
        sslcontext.init(kmf.getKeyManagers(), null, null);
        SSLSocketFactory factory = sslcontext.getSocketFactory();

        _factories.put(hostname, factory);
        return factory;

    }

    private SSLSocket negotiateSSL(Socket sock, String hostname) throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, IOException, InvalidKeyException, SignatureException, NoSuchProviderException, NoCertException {
        synchronized (_factories) {
            SSLSocketFactory factory = _factories.get(hostname);

            if (factory == null) {
                factory = initSSL(hostname);
            }
            String inbound_hostname = sock.getInetAddress().getHostName();
            int inbound_port = sock.getPort();
            SSLSocket sslsock = (SSLSocket) factory.createSocket(sock, inbound_hostname, inbound_port, true);
            sslsock.setUseClientMode(false);
            if (!sslsock.isConnected()) {
            	Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, "Couldn't negotiate SSL");
            	System.exit(-1);
            }
            return sslsock;
        }
    }

    public void closeClientConnection() {
        try {
            sock.close();
        } catch (IOException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private HttpRequest() {
    }

    public HttpResponse getResponse() {
        return resp;
    }

    public boolean setDummyResponse(String s) {

        try {
            removeLine("PROXY-CONNECTION", workingContents);  // removes line proxy-connection from the setInterimContents string
            if (mk_header(workingContents).contains("CONNECT") && !this.connect_protocol_handled) {
                handle_connect_protocol();
            }
        } catch (Exception e) {
            GizmoView.getView().setStatus("Unhandled protocol drop exception.  Probably shouldn't have excepted...");
        }

        host = rewriteMethodLine(workingContents);

        try {
            outboundSock = new Socket(host, port);
        } catch (Exception e) {
            GizmoView.getView().setStatus("Error creating outbound sock AV");
            return false;
        }

        this.resp = HttpResponse.create(s + "\r\n");
        resp.contentID = this.contentID;
        this.version = "1.0";

        try {
            this.sendDataToClient();
        } catch (Exception e) {
            GizmoView.getView().setStatus("Exception in closing connection.");
            return false;
        }
        this.wakeupAndSend();
        return true;
    }

    private int getPortFromHeader(String header) {
        int begin = header.indexOf("CONNECT") + "CONNECT".length();
        int end = header.indexOf(":", begin);
        String portString = header.substring(end + 1, header.indexOf(" ", end + 1));
        return Integer.parseInt(portString);
    }

    private String getHostFromHeader(String header) {
        int begin = header.indexOf("CONNECT") + "CONNECT".length();
        int end = header.indexOf(":", begin);
        return header.substring(begin, end).trim();
    }

    public static HttpRequest createRequest() {
        return new HttpRequest();
    }

    public boolean readRequest(Socket clientSock) {
        StringBuffer contents = new StringBuffer();

        try {
            InputStream input = clientSock.getInputStream();

            contents = readMessage(input);

            if (contents == null) {
                return false;
            }

            setContentEncodings(contents);
            this.interrimContents = contents.toString();
            this.sock = clientSock;

            this.header = contents.substring(0, contents.indexOf("\r\n"));


            workingContents = new StringBuffer(this.interrimContents.toString());

            if (header.contains("CONNECT") && GizmoView.getView().intercepting()) {
                handle_connect_protocol();
                if (!GizmoView.getView().config().terminateSSL()) {
                    return false;
                }
                this.header = workingContents.substring(0, workingContents.indexOf("\r\n"));
            }
            if (GizmoView.getView().intercepting()) {
                if (header.contains("http")) {
                    url = header.substring(header.indexOf("http"), header.indexOf("HTTP") - 1);
                } else {
                    url = header.substring(header.indexOf("/"), header.indexOf("HTTP") - 1);
                }
                if (url.contains("//")) {
                    host = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
                } else {
                    String upper = workingContents.toString().toUpperCase();
                    int host_start = upper.indexOf("HOST:") + 5;
                    host = workingContents.substring(host_start, upper.indexOf("\n", host_start)).trim();
                }
            }


            this.addContents(workingContents.toString());

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean fetchResponse(boolean cached) {
        this.cached = cached;

        OutputStream out = null;
        BufferedReader strBr = null;

        try {
            if (cached) {
                strBr = new BufferedReader(new StringReader(this.interrimContents.toString()));
            }

            removeLine("PROXY-CONNECTION", workingContents);
            updateContentLength();

            if (mk_header(workingContents).contains("CONNECT") && !this.connect_protocol_handled) {
                handle_connect_protocol();
                if (!GizmoView.getView().config().terminateSSL()) {
                    this.passthroughssl = true;
                    return false;
                }
            }

            if (isSSL || this.sock instanceof SSLSocket) {
                SSLSocket sslSock = (SSLSocket) this.sock;
                SSLSocket sslOut = null;
                if (workingContents == null) {
                    return false;
                }

                if (workingContents.indexOf("\r\n") == -1) {
                    return false;
                }

                if (!this.override_host)
                    host = rewriteMethodLine(workingContents);

                if (!user_defined_port) {
                    port = 443;
                }

                if (outboundSock == null ||
                        (!(outboundSock instanceof SSLSocket))) {

                    SSLSocketFactory sslsocketfactory = sloppySSL();
                    sslOut = (SSLSocket) sslsocketfactory.createSocket(host, port);
                } else {
                    sslOut = (SSLSocket) outboundSock;
                }

                sslOut.getOutputStream().write(workingContents.toString().getBytes());
                this.resp = HttpResponse.create(sslOut.getInputStream());
                if (resp == null) {
                    return false;
                }

            } else {
                //if (!this.override_host)
                    host = rewriteMethodLine(workingContents);

                outboundSock = new Socket(host, port);

                outboundSock.getOutputStream().write(workingContents.toString().getBytes());
                this.resp = HttpResponse.create(outboundSock.getInputStream());

                if (resp == null) {
                    return false;
                } 
            }

            this.addContents(workingContents.toString());

            this.header = workingContents.substring(0, this.workingContents.indexOf("\r\n"));
            this.url = getUrlPath(header);

            this.version = getVersion(this.header);

        } catch (SocketException e) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, e);
            return false;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            try {
                GizmoView.getView().setStatus("couldn't connect with ssl.. cert issues?");
                sock.close();
            } catch (IOException ex) {
                Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
        } catch (IOException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (FailedRequestException e) {
            GizmoView.getView().setStatus("malformed server response");
        } catch (Exception e) {
            try {
            	Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, e);
                GizmoView.getView().setStatus("couldn't connect");
                this.sock.close();
                return false;
            } catch (IOException ex) {
                Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.wakeupAndSend();

        resp.setRequest(this);
        return true;
    }

    public Socket getOutboundSock() {
        return outboundSock;
    }

    private boolean handle_connect_protocol() {
        try {
            isSSL = true;

            /*                if (Configuration.getConfiguration().useProxy()) {
            outboundSock = new Socket(Configuration.getConfiguration().proxyHost(), Configuration.getConfiguration().proxyPort());
            out = outboundSock.getOutputStream();
            out.write(workingContents.toString().getBytes());
            BufferedReader in = new BufferedReader(new InputStreamReader(outboundSock.getInputStream()));
            in.readLine();
            while (in.ready()) {
            in.readLine();
            }
            }*/

            host = getHostFromHeader(mk_header(workingContents));
            port = getPortFromHeader(mk_header(workingContents));

            if (this.sock.isClosed()) {
                return false;
            }
            this.sock.getOutputStream().write("HTTP/1.0 200 Connection established\r\n\r\n".getBytes());

            if (!GizmoView.getView().config().terminateSSL()) {
                return false;
            }

            SSLSocket sslSock = negotiateSSL(this.sock, host);

            if (!cached) {
                workingContents = readMessage(sslSock.getInputStream());
                this.header = mk_header(workingContents);
                removeLine("accept-encoding", workingContents);
            }
            this.sock = sslSock;
        } catch (NoCertException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (KeyManagementException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (KeyStoreException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (CertificateException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (InvalidKeyException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (SignatureException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        this.connect_protocol_handled = true;
        return true;
    }

    private StringBuffer readMessage(InputStream input) {
        StringBuffer contents = new StringBuffer();
        String CONTENT_LENGTH = "CONTENT-LENGTH";
        DataInputStream dinput = new DataInputStream(input);
        readerToStringBuffer(dinput, contents);
        int ii = 0;


        String uContents = contents.toString().toUpperCase();
        if (uContents.contains(CONTENT_LENGTH)) {
            int contentLengthIndex = uContents.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length() + 2;
            String value = uContents.substring(uContents.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length() + 2, uContents.indexOf('\r', contentLengthIndex));
            int contentLength = Integer.parseInt(value.trim());
            StringBuffer body = new StringBuffer();

            try {
                for (; body.length() < contentLength; ii++) {
                    byte ch_n = dinput.readByte();
                    while (ch_n == -1) {
                        Thread.sleep(20);
                        ch_n = dinput.readByte();
                    }
                    body.append((char) ch_n);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
            contents.append(body);
        }


        return contents;
    }

    public String toString() {
        return contents().toString();
    }

    void respond() {
        try {
            this.sock.getOutputStream().write(resp.byteContents());
            this.sock.close();

        } catch (IOException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void removeLine(String header, StringBuffer contents) {
        header = header.toUpperCase();

        String proxyUpper = contents.toString().toUpperCase();
        if (proxyUpper.contains(header)) {
            int begin = proxyUpper.indexOf(header);
            int lineEnd = proxyUpper.indexOf("\r\n", begin) + 2;
            contents.delete(begin, lineEnd);
        }
    }

    public void sendDataToClient() throws IOException {
        if (sock instanceof SSLSocket) {
            SSLSocket sslSock = (SSLSocket) sock;
            if (sslSock == null || resp == null) {
                return;
            }
            sslSock.getOutputStream().write(resp.byteContents());
        } else {
            this.sock.getOutputStream().write(resp.byteContents());
            this.sock.getOutputStream().flush();
        }
        if (version.equals("1.0") && !cached) {
            this.sock.close();
        }
        this.sent = true;
    }

    private void updateContentLength() {
        String proxyUpper = workingContents.toString().toUpperCase();
        final String CONTENTLENGTH = "CONTENT-LENGTH:";
        if (!proxyUpper.contains(CONTENTLENGTH)) {
            return;
        }

        int start = proxyUpper.indexOf(CONTENTLENGTH) + CONTENTLENGTH.length();
        int end = proxyUpper.indexOf("\r\n", start);

        int new_length = proxyUpper.substring(proxyUpper.indexOf("\r\n\r\n") + 4).length();

        workingContents.replace(start, end, " " + new_length);
    }

    void passThroughAllBits() {
        try {
            Socket destinationHost = new Socket(host, port);
            byte[] buf = new byte[1024];
            boolean didNothing = true;
            while(!this.sock.isClosed() && !destinationHost.isClosed()) {
                didNothing = true;
                if (sock.getInputStream().available() > 0) {
                    int b = sock.getInputStream().read(buf);
                    destinationHost.getOutputStream().write(buf, 0, b);
                    didNothing = false;
                }
                if (destinationHost.getInputStream().available() > 0) {
                    int b = destinationHost.getInputStream().read(buf);
                    sock.getOutputStream().write(buf, 0, b);
                    didNothing = false;
                }
                if (didNothing) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            this.sock.close();
            destinationHost.close();
        } catch (UnknownHostException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setContentEncodings() {

    }

    private void setContentEncodings(StringBuffer contents) {
        if (contents.toString().toUpperCase().indexOf(ACCEPT_ENCODING) != -1) {
            int valStart = contents.toString().toUpperCase().indexOf(ACCEPT_ENCODING)+ACCEPT_ENCODING.length() + 2;
            int valEnd = contents.indexOf("\r\n", valStart);
            contents.replace(valStart, valEnd, "gzip, deflate");
        }
    }

    class NoCertException extends Exception {
    }
}
