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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.isecpartners.gizmo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;
import org.apache.commons.collections.primitives.ArrayByteList;

/**
 *
 * @author I entirely agree that the below manual parse of http is complicated.  It does work.  I recommend you dont modify it.  I tried it in every scenario in every browser i could think of against a ton of
 * different servers, and it more or less works.  If you see some good small refactoring, go for it.  If you see a line where you think "i wonder what this does?  well i'll delete it", please dont.
 * You're going to have to try it against a whole battery of clients and servers before you'll know if you've done the right thing, and it'll all end in tears.
 */
class HttpResponse extends HTTPMessage {

    public static final String ENDL = "\r\n";
    ArrayByteList blist;
    private static final String CONTENT_LENGTH = "CONTENT-LENGTH";
    private static final String TRANSFER_ENCODING = "TRANSFER-ENCODING";
    private static final String CONTENT_ENCODING = "CONTENT-ENCODING";
    private HttpRequest req;
    private boolean gzip = false;
    private boolean deflate = false;

    private HttpResponse() {
    }

     /**
     *
     * @param s Value to print into the response.
     * @return HttpResponse based on the input parameter s.
     */
    static HttpResponse create(String s){
        byte[] contents = s.getBytes();
        ArrayByteList tempArray = new ArrayByteList();
        for (int ii = 0; ii < contents.length; ii++) {
            tempArray.add(contents[ii]);
        }
        HttpResponse resp = new HttpResponse();
        resp.setBlist(tempArray);
        resp.setHeader("HTTP/1.0 200 OK\r\n");
        return resp;
    }

    public static HttpResponse create(InputStream in) throws FailedRequestException {
        HttpResponse resp = new HttpResponse();

            resp.processResponse(in);
        return resp;
    }

    public void setRequest(HttpRequest req) {
        this.req = req;
    }

    public boolean isGzipped() {
        return gzip;
    }

    public boolean isInflated() {
        return deflate;
    }

    public HttpRequest getRequest() {
        return req;
    }

    private void append(ArrayByteList blist, StringBuffer string) {
        byte[] contents = string.toString().getBytes();
        for (int ii = 0; ii < contents.length; ii++) {
            blist.add(contents[ii]);
        }
    }

    public String second_auth_header() {
        String val = "";
        String WWW_AUTHENTICATE = "WWW-AUTHENTICATE";
        int endlendl = contents().indexOf("\r\n\r\n");
        String upper_contents = contents().substring(0, endlendl).toUpperCase();
        int first = upper_contents.indexOf(WWW_AUTHENTICATE);
        int first_eol = upper_contents.indexOf("\r\n", first);
        if (first == -1) {
            return "";
        }
        String first_header = upper_contents.substring(first, first_eol).split(":")[1].trim();
        String second_header = "";
        if (first != -1) {
            int second = upper_contents.indexOf(WWW_AUTHENTICATE, first + WWW_AUTHENTICATE.length());
            if (second == -1) {
                return "";
            }
            int eol = upper_contents.indexOf("\r\n", second);
            String line = upper_contents.substring(second, eol);
            String bits[] = line.split(":");
            second_header = bits[1].trim();
        }
        if (first_header.equalsIgnoreCase("NEGOTIATE")) {
            val = second_header;
        } else {
            val = first_header;
        }
        return val;
    }

    public String contents() {
        if (gzip)
            return unzipData(blist.toArray()) + "\n";
        else if (deflate)
            return deflateData(blist.toArray()) + "\n";
        return new String(blist.toArray()) + "\n";
    }

    private String readline(InputStream in) {
        StringBuffer strbuf = new StringBuffer();

        try {

            int ch_n = in.read();
            char ch = (char) ch_n;
            while (ch != '\n' && ch_n != -1) {
                strbuf.append(ch);
                ch_n = in.read();
                ch = (char) ch_n;
            }
            if (ch_n != -1) {
                strbuf.append(ch);
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return strbuf.toString();

    }

    private void setBlist(ArrayByteList list) {
        this.blist = list;
    }

    public void processResponse(InputStream in) throws FailedRequestException {
        StringBuffer content = new StringBuffer();
        DataInputStream inputStream = new DataInputStream(in);
        ArrayByteList blist = new ArrayByteList();
        String header = null;
        int contentLength = 0;
        boolean isChunked = false;
        String line;
        try {
            line = readline(inputStream);
            while (line != null && !line.equals(ENDL)) {
                content.append(line);
                if (line.toUpperCase().contains(CONTENT_LENGTH) && line.toUpperCase().indexOf(CONTENT_LENGTH) == 0) {
                    String value = line.substring(line.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length() + 2, line.indexOf('\r'));
                    contentLength = Integer.parseInt(value.trim());
                } else if (line.toUpperCase().contains(TRANSFER_ENCODING)) {
                    if (line.toUpperCase().substring(line.toUpperCase().indexOf(TRANSFER_ENCODING) + "Transfer-Encoding:".length()).contains("CHUNKED")) {
                        isChunked = true;
                    }
                } else if (line.toUpperCase().contains(CONTENT_ENCODING)) {
                    String value = line.substring(line.indexOf(CONTENT_ENCODING) + CONTENT_ENCODING.length() + 2, line.indexOf('\r'));
                    value = value.trim();
                    if (value.toUpperCase().equals("GZIP")) {
                        this.gzip = true;
                    } else if (value.toUpperCase().equals("DEFLATE")) {
                        this.deflate = true;
                    }
                }
                line = readline(inputStream);
            }
            if (line == null) {
                GizmoView.log(content.toString());
                throw new FailedRequestException();
            }

            content.append("\r\n");
            header = content.substring(0, content.indexOf("\r\n"));
            append(blist, content);

            if (contentLength != 0) {
                for (int ii = 0; ii < contentLength; ii++) {
                    blist.add(inputStream.readByte());
                }
            }

            if (isChunked) {
                boolean isDone = false;
                while (!isDone) {
                    byte current = inputStream.readByte();
                    blist.add(current);

                    int size = 0;
                    while (current != '\n') {
                        if (current != '\r') {
                            size *= 16;
                            if (Character.isLetter((char) current)) {
                                current = (byte) Character.toLowerCase((char) current);
                            }
                            if ((current >= '0') && (current <= '9')) {
                                size += (current - 48);
                            } else if ((current >= 'a') && (current <= 'f')) {
                                size += (10 + current - 97);
                            }
                        }
                        current = inputStream.readByte();

                        while ((char) current == ' ') {
                            current = inputStream.readByte();
                        }
                        blist.add(current);
                    }

                    if (size != 0) {
                        for (int ii = 0; ii < size; ii++) {
                            int byte1 = inputStream.readByte();
                            byte blah = (byte) byte1;
                            blist.add(blah);
                        }
                        blist.add(inputStream.readByte());
                        blist.add(inputStream.readByte());
                    } else {
                        byte ch = (byte) inputStream.read();
                        StringBuffer endstuff = new StringBuffer();
                        blist.add(ch);
                        endstuff.append((char) ch);
                        while (ch != '\n') {
                            ch = inputStream.readByte();
                            endstuff.append((char) ch);
                            blist.add(ch);
                        }

                        isDone = true;
                    }

                }
            }

            if (inputStream.available() > 0) {
                try {
                    while (true) {
                        blist.add(inputStream.readByte());
                    }
                } catch (EOFException e) {
                    System.out.println(e);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
        } 


        setBlist(blist);
        setHeader(header);
        if (this.gzip) {
            addContents(unzipData(blist.toArray()));
        } else if (this.deflate) {
            addContents(deflateData(blist.toArray()));
        } else {
            addContents(content.toString());
        }
    }

    public String deflateData(byte[] blist) {
        return uncompressData(blist, new InflaterInputStream(new ByteArrayInputStream(reduceToBodyBlock(blist))));
    }

    public String unzipData(byte[] blist) {
        try {
            String str = uncompressData(blist, new GZIPInputStream(new ByteArrayInputStream(reduceToBodyBlock(blist))));
            return str;
        } catch (IOException ex) {
            Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new String(blist); // shouldnt happen, but at least not a terrible backup action
    }

    public String uncompressData(byte[] blist, InputStream in) {
        StringBuffer sb = new StringBuffer();
        sb.append(extractHeaderBlock(blist));

        try {

            byte[] buf = new byte[1024];


            while(in.available() > 0) {
                int nRead = in.read(buf);
                if (nRead < 0) break;
                byte[] buf2 = new byte[nRead];
                System.arraycopy(buf, 0, buf2, 0, nRead);
                sb.append(new String(buf2));
            }
        } catch (IOException ex) {
            Logger.getLogger(HttpResponse.class.getName()).log(Level.SEVERE, null, ex);
        }

        return sb.toString();
    }

    public byte[] byteContents() {
        return blist.toArray();
    }

    private void setHeader(String header) {
        this.header = header;
    }


    private String extractHeaderBlock(byte[] blist) {
        int pos = 0;
        for (int ii =0; ii<blist.length - 4; ii++) {
            if (blist[ii] == 13 && blist[ii + 1] == 10 && blist[ii+2] == 13 && blist[ii+3] == 10)  {
                pos = ii;
                break;
            }
        }
        byte[] buf = new byte[pos+4];
        System.arraycopy(blist, 0, buf, 0, pos+4);
        return new String(buf);
    }

    private byte[] reduceToBodyBlock(byte[] blist) {
        int pos = 0;
        for (int ii =0; ii<blist.length - 4; ii++) {
            if (blist[ii] == 13 && blist[ii + 1] == 10 && blist[ii+2] == 13 && blist[ii+3] == 10)  {
                pos = ii;
                break;
            }
        }
        byte[] buf = new byte[blist.length - pos-3 - 1];
        System.arraycopy(blist, pos+4, buf, 0, buf.length);
        return buf;
    }

    class FailedRequestException extends Exception {
    }
}
