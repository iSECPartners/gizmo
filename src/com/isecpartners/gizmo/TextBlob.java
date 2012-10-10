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


/**
 * Text displayed on the screen need to be managed.  The screen is functionally a one dimensional character array whose contents
 * get displayed on the screen by a java component.  To display requests on the screen, we insert strings at the end of this array.
 * We keep track of the begin and end of the request contents in this class.
 */
class TextBlob {
    private static int globalBlobIndex;

    /** TODO: Explain this field. */
    int blob_index = globalBlobIndex++;

    private boolean expanded = false;
    private int begin = 0;
    private int end = 0;
    private HTTPMessage message;
    private Prefix prefix;


    /**
     * @param begin TODO explain this parameter.
     * @param message The message constituting this TextBlob. TODO Explain
     * this better.
     */
    public TextBlob(int begin, HTTPMessage message) {
        this.begin = begin;
        this.end = begin + message.header().length();
        this.message = message;
    }


    /**
     * @return The index of this TextBlob.
     */
    // TODO: Consider getting rid of these accessor methods, and just making
    // the member fields public. Accessor methods are appropriate in public
    // library APIs, but this code is pretty Gizmo-specific. The performance
    // improvement might (or might not) be noticeable.
    public int getBlobIndex() {
        return blob_index;
    }


    /**
     * @return The beginning of this TextBlob.
     */
    public int getBegin() {
        return begin;
    }


    /**
     * @return The end of this TextBlob.
     */
    public int getEnd() {
        return end;
    }


    // TODO: Change this to follow standard Java naming guidelines
    // (getMessage).
    /**
     * @return The HTTPMessage for this TextBlob.
     */
    public HTTPMessage getMsg() {
        return message;
    }


    /**
     * Shifts the beginning and end of this TextBlob.
     *
     * @param delta The amount by which to shift the beginning and end.
     */
    public void increment(int delta) {
        begin += delta;
        end += delta;
    }


    /**
     * XXX: This is an alias for getMsg and should go away.
     */
    public HTTPMessage content() {
        return getMsg();
    }


    /**
     * @param begin The new begin for this TextBlob.
     */
    public void setBegin(int begin) {
        this.begin = begin;
    }


    /**
     * @param end The new end for this TextBlob.
     */
    public void setEnd(int end) {
        this.end = end;
    }


    /**
     * @return Whether or not this TextBlob is expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }


    /**
     * @return If this TextBlob is expanded, the contents of the message are
     * returned; otherwise, only the header is returned.
     */
    public String text() {
        if (expanded) {
            return message.contents();
        } else {
            return message.header();
        }
    }

    /**
     * Expands or unexpands this TextBlob.
     */
    public void toggleExpansion() {
        HTTPMessage m = message;
        int d = m.contents().length() - m.header().length();
        if (expanded) {
            end -= d;
        } else {
            end += d;
        }
        expanded = ! expanded;
    }

    void setPrefix(Prefix prefix) {
        this.prefix = prefix;
    }

    public Prefix getPrefix() {
        return prefix;
    }
}

