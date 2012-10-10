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



public class AddRequest extends UpdateRequest {
    private HTTPMessage msg;
    private int end = -1;
    private Object lock = new Object();
    private String addendum;
    private boolean shouldMoveTo = false;
    private TextBlob blob = null;
    private String print_string;

    /**
     * @param msg
     * @param addendum
     */
    public AddRequest(HTTPMessage msg, String addendum,boolean shouldMoveTo) {
        super();
        this.msg = msg;
        this.addendum = addendum;
        this.shouldMoveTo = shouldMoveTo;
    }

    public AddRequest(String str) {
        this.print_string = str;
    }

    public HTTPMessage getMessage() {
        return msg;
    }

    public String addendum() {
        return addendum;
    }

   public void setEnd(int end) {
        this.end = end;

        synchronized(lock) {
            lock.notifyAll();
        }
    }

    public int getEnd() {
        if (end == -1) {
        synchronized(lock) {
            try {
            lock.wait();
            } catch (Exception e) {System.out.println(e);}
        }
        }
        return end;
    }

    public TextBlob getBlob() {
        return blob;
    }

    public String toString() {
        return "ADD(" + requestNumber + ")" + msg.header().length();
    }

    public boolean moveTo() {
        return shouldMoveTo;
    }

    void setBlob(TextBlob blob) {
        this.blob = blob;
    }

    public String getPrefix() {
        return print_string;
    }

    void setPrefix(String prefix) {
        this.print_string = prefix;
    }

}
