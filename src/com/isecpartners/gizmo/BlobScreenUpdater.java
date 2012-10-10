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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.StyledDocument;

public class BlobScreenUpdater implements Runnable {

    private static int numScreen = 0;
    private int thisScreen = numScreen++;
    private JTextPane pane;
    private LinkedBlockingQueue<UpdateRequest> queue = new LinkedBlockingQueue<UpdateRequest>();
    private BlobScroller scroller;
    private Highlighter highLight;
    private Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(new Color(134, 255, 134));
    private Highlighter.HighlightPainter editHighlightPainter = new MyHighlightPainter(new Color(255, 255, 255));
    private ArrayList<TextBlob> blobs = new ArrayList<TextBlob>();
    private boolean cleared = false;
    private String savedScreen;
    private boolean search = false;
    private int editting_pos = -1;
    private TextBlob default_edit_highlight = null;
    private List<Prefix> prefixes = new LinkedList<Prefix>();
    private List<UpdateRequest> saved_requests = new LinkedList<UpdateRequest>();

    public BlobScreenUpdater(JTextPane pane, BlobScroller scroller) {
        this.pane = pane;
        this.scroller = scroller;
        highLight = new DefaultHighlighter();
        pane.setHighlighter(highLight);
    }

    public void addUpdate(UpdateRequest update) {
        queue.add(update);
    }

    void clearBlobs() {
        this.blobs.clear();
        this.pane.setText("");
    }

    void restore() {
        try {
            StyledDocument doc = pane.getStyledDocument();
            AbstractDocument aDoc = (AbstractDocument) doc;
            aDoc.insertString(0, this.savedScreen, null);
            highLight = new DefaultHighlighter();
            pane.setHighlighter(highLight);

            while(saved_requests.size() > 0) {
                disassembleAndDispatch(saved_requests.remove(0));
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(BlobScreenUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void disassembleAndDispatch(UpdateRequest ur) {
        if (ur instanceof AggregateRequest) {
            AggregateRequest ar = (AggregateRequest) ur;
            for (UpdateRequest req : ar.getRequests()) {
                dispatch(req);
            }
        } else {
            dispatch(ur);
        }
    }

    private void upOneLine(int pos) {
        int delta = 0;

        String text = pane.getText();
        int prev_endl = text.substring(0, pos).lastIndexOf("\n");

        if (prev_endl == -1) {
            return;
        }

        delta = text.substring(prev_endl, pos).length();

        int prev_prev_endl = text.substring(0, prev_endl).lastIndexOf("\n");

        if (prev_prev_endl == -1) {
            return;
        }

        if (text.substring(prev_prev_endl, prev_endl).length() >= delta) {
            delta = (text.substring(prev_prev_endl, prev_endl).length() - delta) + delta;
        } else {
            editting_pos = prev_endl - 1;
        }

        moveEditHighlight(pos - delta);
        editting_pos -= delta;
    }

    private int downOneLine(int pos) {
        return -1;
    }

    private void move(MoveRequest moveRequest) {
        if (moveRequest.isUp()) {
            upOneLine(editting_pos);
        } else if (moveRequest.isDown()) {
        } else if (moveRequest.isLeft()) {
            if (editting_pos - 1 >= default_edit_highlight.getBegin()) {
                moveEditHighlight(--editting_pos);
            }

        } else if (moveRequest.isRight()) {
            if (editting_pos + 1 <= default_edit_highlight.getEnd()) {
                moveEditHighlight(++editting_pos);
            }
        }
    }

    private void moveCaret(int where) {
        try {
            if (where == MoveCaretRequest.GET_END) {
                pane.setCaretPosition(length());
            } else {
                pane.setCaretPosition(where);
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    public int numBlobs() {
        return blobs.size();
    }

    private int length() {
        final int len = pane.getStyledDocument().getLength();
        return len;
    }

    public void add(AddRequest req) {
        Prefix prefix = null;
        if (req.getPrefix() != null) {
            try {
                prefix = new Prefix(req.getPrefix().length());
                pane.getStyledDocument().insertString(length(), req.getPrefix(), null);
            } catch (BadLocationException ex) {
                Logger.getLogger(BlobScreenUpdater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        HTTPMessage msg = req.getMessage();
        int end;

        StyledDocument doc = pane.getStyledDocument();

        if (req.getBlob() != null) {
            end = req.getBlob().getEnd() + 1;
        } else {
            end = length();
        }
        TextBlob blob = new TextBlob(end, msg);
        blob.setPrefix(prefix);

        if (req.getBlob() != null) {
            int indx = blobs.indexOf(req.getBlob());
            blobs.add(indx + 1, blob);
        } else {
            blobs.add(blob);
        }

        int where = blobs.indexOf(blob);

        try {
            doc.insertString(end, blob.getMsg().header() + req.addendum(), null);
        } catch (Exception ex) {
            System.out.println(ex);
        }

        if (req.getBlob() != null) {
            for (Iterator<TextBlob> ii = iterator(where + 1); ii.hasNext();) {
                (ii.next()).increment(blob.getEnd() - blob.getBegin() + 2);
            }
        }

        req.setEnd(blob.getEnd());
        if (req.moveTo()) {
            scroller.setCurrent(where);
//            expand(blob);
        }
        if (blobs.size() == 1 || req.moveTo()) {
            highlightBlob(blob, true);
        }

    }

    private void moveEditHighlight(int pos) {
        try {
            highlightBlob(default_edit_highlight, true);
            highLight.removeAllHighlights();
            highLight.addHighlight(default_edit_highlight.getBegin(), pos, myHighlightPainter);
            highLight.addHighlight(pos + 1, default_edit_highlight.getEnd(), myHighlightPainter);
        } catch (BadLocationException ex) {
            Logger.getLogger(BlobScreenUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public TextBlob get(int index) {
        return blobs.get(index);
    }

    public Iterator<TextBlob> iterator(int ii) {
        return blobs.listIterator(ii);
    }

    public Iterator<TextBlob> iterator() {
        return blobs.listIterator();
    }

    private void print(PrintRequest req) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(length(), req.getWhat(), null);
            req.setEnd(length());
            if (blobs.size() == 1) {
                highlightBlob(blobs.get(0), true);
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private void highlightBlob(TextBlob blob, boolean down) {
        try {
            if (blob.getEnd() > length()) {
                return;
            }

            int begin = blob.getBegin();
            int end = blob.getEnd();

            if (highLight.getHighlights().length > 0) {
                highLight.removeAllHighlights();
            }
            highLight.addHighlight(begin, end, myHighlightPainter);
            if (down) {
                pane.setCaretPosition(end);
            } else {
                pane.setCaretPosition(begin);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void remove(RemoveRequest req) {
        StyledDocument doc = pane.getStyledDocument();
        AbstractDocument aDoc = (AbstractDocument) doc;
        int blob_ndx = Integer.MAX_VALUE;
        try {

            int difference = 0;

            for (int ii = 0; ii < blobs.size(); ii++) {
                if (ii > blob_ndx) {
                    blobs.get(ii).increment(-difference);
                }

                if (blobs.get(ii).getBlobIndex() == req.getBlob().getBlobIndex()) {
                    TextBlob blob = blobs.get(ii);
                    if (blob.getPrefix() != null) {
                        difference = blob.getEnd() - blob.getBegin() + 2 + blob.getPrefix().getLength();
                    } else {
                        difference = blob.getEnd() - blob.getBegin() + 2;
                    }

                    /*                    if (length() < (blob.getBegin() + difference)) {
                    System.out.println("argh");
                    } else {*/
                    int begin = blob.getBegin();
                    if (blob.getPrefix() != null) {
                        begin -= blob.getPrefix().getLength();
                    }
                    aDoc.remove(begin, difference);
//                    }

                    blob_ndx = ii;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(BlobScroller.class.getName()).log(Level.SEVERE, null, ex);
        }
        req.removeBlob();

        blobs.remove(req.getBlob());
        if (blob_ndx == Integer.MAX_VALUE) {
            highlightBlob(scroller.getCurrent(), true);
        } else {
            if (blob_ndx > 0) {
                highlightBlob(blobs.get(blob_ndx - 1), true);
            } else {
                highlightBlob(blobs.get(0), true);
            }
        }

    }

    public void clear() {

        try {
            StyledDocument doc = pane.getStyledDocument();
            AbstractDocument aDoc = (AbstractDocument) doc;

            this.savedScreen = aDoc.getText(0, aDoc.getLength());
            aDoc.remove(0, aDoc.getLength());
            cleared = true;
        } catch (Exception e) {
        }
    }

    public void toggleSearch() {
        if (search == true) {
            search = false;
        } else {
            search = true;
        }
    }

    public boolean isSearch() {
        return search;
    }

    public void contract(TextBlob blob) {
        try {
            if (!blob.isExpanded()) {
                return;
            }

            int before_size = blob.text().length();

            blob.toggleExpansion();

            int after_size = blob.text().length();

            AbstractDocument aDoc = (AbstractDocument) pane.getStyledDocument();

            int sizeToRemove = before_size - after_size;

            aDoc.remove(blob.getBegin() + after_size, sizeToRemove);

            for (Iterator<TextBlob> ii = iterator(scroller.getBlobIndex() + 1); ii.hasNext();) {
                (ii.next()).increment(-sizeToRemove);
            }

            highlightBlob(scroller.getCurrent(), true);
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public void expand(TextBlob blob) {
        try {
            if (blob.isExpanded()) {
                return;
            }

            int before_size = blob.text().length();
            int before_end = blob.getEnd();

            blob.toggleExpansion();

            int after_size = blob.text().length();

            AbstractDocument aDoc = (AbstractDocument) pane.getStyledDocument();

            String contents = blob.text();

            contents = contents.substring(contents.indexOf("\r\n") + 2);

            aDoc.insertString(before_end, "\r\n" + contents, null);

            // need to get the index of the blob here
            for (Iterator<TextBlob> ii = iterator(scroller.getBlobIndex() + 1); ii.hasNext();) {
                (ii.next()).increment(after_size - before_size);
            }

            highlightBlob(scroller.getCurrent(), true);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void dispatch(UpdateRequest ur) {
        if (ur instanceof PrintRequest) {
            print((PrintRequest) ur);
        } else if (ur instanceof RemoveRequest) {
            remove(((RemoveRequest) ur));
        } else if (ur instanceof HighlightRequest) {
            HighlightRequest hr = (HighlightRequest) ur;
            highlightBlob(hr.getBlob(), hr.isBool());
        } else if (ur instanceof ContractRequest) {
            ContractRequest cr = (ContractRequest) ur;
            contract(cr.getBlob());
        } else if (ur instanceof ExpandRequest) {
            ExpandRequest er = (ExpandRequest) ur;
            expand(er.getBlob());
        } else if (ur instanceof MoveCaretRequest) {
            moveCaret(((MoveCaretRequest) ur).getWhere());
        } else if (ur instanceof AddRequest) {
            add((AddRequest) ur);
        } else if (ur instanceof MoveRequest) {
            move((MoveRequest) ur);
        }
    }

    public void run() {
        while (true) {
            try {

                UpdateRequest ur = queue.take();

                if (!this.search) {
                    disassembleAndDispatch(ur);
                } else {
                    saved_requests.add(ur);
                }


            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}