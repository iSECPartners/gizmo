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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rachel
 */
class HTTPHandler extends ProtocolHandler {

    ArrayList<HttpRequest> queue = new ArrayList<HttpRequest>();
    private boolean isDead = false;
    private OutputStream out;
    InputStream in;
    private boolean intercept = false;
    LinkedBlockingQueue<HttpRequest> interceptResponses = new LinkedBlockingQueue<HttpRequest>();
    Thread th;

    HTTPHandler(Socket accept, Object lock, MessageForwarder handlerhandler) {
        super(accept, lock, handlerhandler);
//        th = new Thread(new SendResponses());
//        th.setName("intercept queue");

        try {
            in = inbound.getInputStream();
            out = inbound.getOutputStream();
        } catch (Exception e) {
            GizmoView.log(Level.SEVERE, e.toString());
        }
    }

    public void setLock(Object obj) {
        this.lock = obj;
    }

    class ShortCircuitSSLProxy implements Runnable {

        HttpRequest hq;

        public ShortCircuitSSLProxy(HttpRequest hq) {
            this.hq = hq;
        }

        public void run() {
            hq.passThroughAllBits();
        }
    }

    public void run() {
        HttpRequest hq = null;

        try {
            boolean successfull = false;
            if (inbound == null || inbound.isClosed()) {
                isDead = true;
                return;
            }

            hq = HttpRequest.createRequest();
            successfull = hq.readRequest(inbound);

            if (hq.passthroughssl()) {
                new Thread(new ShortCircuitSSLProxy(hq)).start();
            }

            if (!GizmoView.getView().intercepting() || !GizmoView.getView().matchRequest(hq.contents())) {
                hq.fetchResponse(false);
                if (hq.passthroughssl()) {
                    new Thread(new ShortCircuitSSLProxy(hq)).start();
                }
                GizmoView.getView().setStatus("");
                hq.sendDataToClient();
                hq.closeClientConnection();
            } else {
                /*                if (successfull) {
                th.start();
                interceptResponses.put(hq);
                }*/
            }

            if (successfull) {
                handlerhandler.addMessage(hq);

                synchronized (lock) {
                    lock.notifyAll();
                }
            } else {
                inbound.close();
            }

            return;
            /* } catch (InterruptedException ex) {
            Logger.getLogger(HTTPHandler.class.getName()).log(Level.SEVERE, null, ex);
             */        } catch (IOException e) {
            System.out.println(e);
        }


    }

    public boolean isDead() {
        return isDead;
    }

    public boolean hasNext() {
        return queue.size() > 0;
    }

    public HttpRequest getNext() {
        return queue.remove(0);
    }

    class SendResponses implements Runnable {

        public void run() {
//            while (true) {
            try {
                HttpRequest req = interceptResponses.take();
                req.awaitWakeup();
                req.sendDataToClient();
                req.closeClientConnection();
                GizmoView.getSummaryScrollers().addMessage(req, "\n");
                GizmoView.getSummaryScrollers().addMessage(req.getResponse(), "\n\n");
            } catch (InterruptedException ex) {
                Logger.getLogger(HTTPHandler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(HTTPHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
//            }
        }
    }
}
