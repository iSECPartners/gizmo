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

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class BlobScroller {

    private BlobScreenUpdater updater;
    private BlobScreenUpdater savedUpdater;
    private int blobIndex = 0;
    private ExpandAction expandAction;
    private GizmoView view;
    private JTextPane pane;
    private ArrayList<TextBlob> sentBlobs = new ArrayList<TextBlob>();
    private boolean intercept;

    BlobScroller(GizmoView aThis, JPanel jPanel1, boolean b) {
    }

    public BlobScreenUpdater getUpdater() {
        return updater;
    }

    public BlobScroller(GizmoView view, JTextPane pane) {
        this.intercept = false;
        this.view = view;
        this.pane = pane;



        expandAction = new ExpandAction(view, this);

        pane.setEditable(false);

        updater = new BlobScreenUpdater(pane, this);

        new Thread(updater).start();

        GizmoView.getUIEventHandler().setSummary_scroller(this);
    }

    public BlobScroller(GizmoView view, JTextPane pane, boolean intercept) {
        this.intercept = intercept;
        this.view = view;
        this.pane = pane;

        expandAction = new ExpandAction(view, this);

        pane.setEditable(false);

        updater = new BlobScreenUpdater(pane, this);

        new Thread(updater).start();

        if (intercept) {
            GizmoView.getUIEventHandler().setIntercept_scroller(this);
        }
    }

    public void contract(TextBlob blob) {
        AggregateRequest agg = new AggregateRequest();
        agg.addRequest(new ContractRequest(blob));
        agg.addRequest(new HighlightRequest(intercept, blob));
        addUpdate(agg);
    }

    public void expand(final TextBlob blob) {
        AggregateRequest agg = new AggregateRequest();
        agg.addRequest(new ExpandRequest(blob));
        agg.addRequest(new HighlightRequest(intercept, blob));
        addUpdate(agg);
    }

    public void down() {
        if (updater.numBlobs() == 0) {
            return;
        }
        moveDown();
        highlightBlob(getCurrent(), true);
    }

    public void edit() {
        Runnable r = new Runnable() {

            public void run() {
                try {
                    TextBlob current = getCurrent();
                    if (BlobScroller.this.intercept) {
                        GizmoView.getInterceptScroller().remove(current);
                    }
                    GizmoView.log("opening edit pane");
                    String selected_text = BlobScroller.this.pane.getSelectedText();
                    FourthIdea frame;
                    if (selected_text != null) {
                        GizmoView.log("with selected text: " + selected_text);
                        frame = new FourthIdea(BlobScroller.this, selected_text);
                    } else {
                        GizmoView.log("with the selected blob");
                        frame = new FourthIdea(BlobScroller.this, current.getMsg(), current.getMsg() instanceof HttpRequest);
                    }
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setVisible(true);
                } catch (Exception e) {
                    GizmoView.log(e.toString());
                }

            }
        };
        new Thread(r).start();
    }

    public void setCurrent(int current) {
        this.blobIndex = current;
    }



    void clear() {
        this.blobIndex = 0;
        updater.clearBlobs();
    }

    void moveEdit(int direction) {
        addUpdate(new MoveRequest(direction));
    }


    void setCommand(String text, boolean request, HttpRequest req2) {
//        HTTPMessage msg = false;
        HttpRequest req = HttpRequest.createRequest();
        req.setClientSocket(req2.getClientSocket());
        req.setInterrimContents(new StringBuffer(text));
        req.setSSL(req2.isSSL());
        req.setHost(req2.getHost());
        req.setPort(req2.getPort());

        AggregateRequest agg = new AggregateRequest();
        AddRequest addreq;
        if (intercept) {
            addreq = this.getAdd(req, "\n\n", true);
        } else {
            addreq = this.getAdd(req, "\n", true);
        }
        addreq.setPrefix(req2.getURL() + "\n");
        agg.addRequest(addreq);
        addUpdate(agg);
    }



    private HighlightRequest getHighlight(TextBlob blob, boolean bool) {
        return new HighlightRequest(bool, blob);
    }

    private HighlightRequest getHighlight() {
        TextBlob current = getCurrent();
        if (current == null) {
            return null;
        }
        return getHighlight(current, true);
    }

    private void highlightBlob() {
        if (getCurrent() != null) {
            addUpdate(getHighlight());
        }
    }

    private void highlightBlob(TextBlob blob, boolean bool) {
        addUpdate(getHighlight(blob, bool));
    }

    public void up() {
        if (updater.numBlobs() == 0) {
            return;
        }
        moveUp();
        highlightBlob(getCurrent(), false);
    }


    public void sendAction() {
        new Thread(new PerformSendAction(this, intercept)).start();
    }

    public void dropAction() {
        new Thread(new PerformDropAction(this, intercept)).start();
    }

    public ExpandAction expandAction() {
        return expandAction;
    }

    public TextBlob getBlob(int index) {
        if (updater.numBlobs() == 0) {
            return null;
        }

        return updater.get(index);
    }

    private AddRequest getAdd(HTTPMessage msg, String addendum, boolean shouldMoveTo) {
        return new AddRequest(msg, addendum, shouldMoveTo);
    }



    private boolean hasRemoved(TextBlob blob) {
        return sentBlobs.contains(blob);
    }

    private RemoveRequest getRemove(TextBlob blob) {
        if (hasRemoved(blob)) {
            return null;
        }
        sentBlobs.add(blob);
        RemoveRequest req = new RemoveRequest(blob, sentBlobs);
        this.blobIndex = 0;
        return req;
    }

    public synchronized void remove(TextBlob blob) {
        if (!hasRemoved(blob)) {
            addUpdate(getRemove(blob));
        }
    }

    private MoveCaretRequest getMoveCaret(int where) {
        return new MoveCaretRequest(where);
    }

    private void moveCaret(int where) {
        addUpdate(getMoveCaret(where));
    }

    public void clearPhraseFromScreen(String phrase) {
        if (savedUpdater != null) {
            updater.clear();
            updater = savedUpdater;
            savedUpdater.toggleSearch();
            updater.restore();
            savedUpdater = null;
            blobIndex = 0;
            this.highlightBlob();
        }

        if (phrase != null && !phrase.equals("")) {
            updater.clear();
            this.savedUpdater = updater;
            this.updater = new BlobScreenUpdater(pane, this);
            new Thread(updater).start();

            Iterator<TextBlob> it = savedUpdater.iterator();

            while (it.hasNext()) {
                TextBlob blob = it.next();
                String uContents = blob.getMsg().contents().toUpperCase();
                String uPhrase = phrase.toUpperCase();
                if (blob.getMsg().contents().toUpperCase().contains(phrase.toUpperCase())) {
                    if (blob.getMsg() instanceof HttpRequest) {
                        HttpRequest req = ((HttpRequest)blob.getMsg());
                        addMessage(req.getURL() + "\n", req, "\n");
                        add(((HttpRequest) blob.getMsg()).getResponse(), "\n\n");
                    } else {
                        HttpRequest req = ((HttpResponse)blob.getMsg()).getRequest();
                        String url = req.getURL();

                        addMessage(url + "\n", req, "\n");
                        add(blob.getMsg(), "\n\n");
                    }
                }
            }

            savedUpdater.toggleSearch();
            blobIndex = 0;
            highlightBlob();
        }
    }

    public boolean isSearching() {
        return (savedUpdater != null) && (savedUpdater.isSearch());
    }

    public AddRequest add(HTTPMessage msg, String addendum) {
        AddRequest req = getAdd(msg, addendum, false);
        addAddUpdate(req);
        return req;
    }

    public void addMessageAfter(HTTPMessage next, String str, TextBlob blob) {
        AggregateRequest agg = getAddMessage(next, str, false, blob);
        addAddUpdate(agg);
    }

    public void addMessageAfter(HTTPMessage next, String str, TextBlob blob, String url) {
        AggregateRequest agg = new AggregateRequest();
        agg.addRequest(new PrintRequest(url));
        AddRequest add = getAdd(next, str, false);
        add.setBlob(blob);
        agg.addRequest(add);
        HighlightRequest high = getHighlight();
        if (high != null) {
            agg.addRequest(high);

        }
        addAddUpdate(agg);
    }

    public void addMessage(String prefix, HTTPMessage next, String str) {
        AggregateRequest agg = new AggregateRequest();
        AddRequest add = getAdd(next, str, false);
        add.setBlob(null);
        add.setPrefix(prefix);
        agg.addRequest(add);
        HighlightRequest high = getHighlight();
        if (high != null) {
            agg.addRequest(high);

        }

        addAddUpdate(agg);
    }

    public synchronized int addMessage(HTTPMessage msg, String addendum) {
        addAddUpdate(getAddMessage(msg, addendum, false));
        return this.blobIndex;
    }

    private AggregateRequest getAddMessage(HTTPMessage msg, String addendum, boolean shouldMoveTo) {
        return getAddMessage(msg, addendum, shouldMoveTo, null);
    }

    private AggregateRequest getAddMessage(HTTPMessage msg, String addendum, boolean shouldMoveTo, TextBlob blob) {
        AggregateRequest agg = new AggregateRequest();
        AddRequest add = getAdd(msg, addendum, shouldMoveTo);
        add.setBlob(blob);
        agg.addRequest(add);
        HighlightRequest high = getHighlight();
        if (high != null) {
            agg.addRequest(high);

        }
        return agg;
    }

    private PrintRequest getPrint(String str) {
        return new PrintRequest(str);
    }

    public void print(String str) {
        updater.addUpdate(getPrint(str));
    }

    public TextBlob getCurrent() {
        if (updater.numBlobs() > 0 && blobIndex >= updater.numBlobs()) {
            blobIndex = updater.numBlobs() - 1;
        }
        return getBlob(blobIndex);
    }

    public int getBlobIndex() {
        return blobIndex;
    }

    public void moveUp() {
        if (blobIndex > 0) {
            blobIndex--;
        }
    }

    public void moveDown() {
        if (blobIndex < updater.numBlobs()) {
            blobIndex++;
        }
    }

    private void addAddUpdate(UpdateRequest update) {
        updater.addUpdate(update);
    }

    private void addUpdate(UpdateRequest req) {
        updater.addUpdate(req);
    }

    void moveToBeginning() {
        blobIndex = 0;
        BlobScroller.this.highlightBlob();
    }

    public void moveToEnd() {
        if (updater.numBlobs() > 0) {
            blobIndex = updater.numBlobs() - 1;
        } else {
            blobIndex = 0;
        }
        BlobScroller.this.highlightBlob();
    }
}
