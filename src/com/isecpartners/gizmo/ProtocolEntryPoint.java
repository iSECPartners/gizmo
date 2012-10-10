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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.logging.Level;

class ProtocolEntryPoint implements Runnable {

    GizmoView screen;
    ServerSocket in;
    GizmoView outer;
    Queue<HttpRequest> queue;
    MessageForwarder handlerhandler;
    int new_requests = 0;
    Object lock = new Object();
    private boolean intercept = false;
    private boolean start = false;

    public ProtocolEntryPoint(GizmoView v) {
        super();
        this.screen = v;
        handlerhandler = new MessageForwarder(lock, screen);
    }

    public boolean intercepting() {
        return intercept;
    }

    public void run() {
        Thread t1 = new Thread(handlerhandler);
        t1.setName("handlerhandler");
        t1.start();
        try {
            GizmoView.log(Level.INFO, "binding to " + InetAddress.getByName("localhost").toString());
            if (GizmoView.getView() == null) GizmoView.log("one");
            InetSocketAddress localhost = new InetSocketAddress(GizmoView.getView().getIP(), GizmoView.getView().getPort());
            in = new ServerSocket();
            in.bind(localhost);
            GizmoView.log(Level.INFO, "port " + GizmoView.getView().getPort() + "successfully bound");
            while (true) {
                GizmoView.log(Level.INFO, "waiting for connection");
                Socket inbound = in.accept();
                GizmoView.log(Level.INFO, "received connection");
                HTTPHandler ch = new HTTPHandler(inbound, lock, handlerhandler);
                Thread t = new Thread(ch);
                t.setName("ClientHandler");
                t.start();

                handlerhandler.addHandler(ch);
            }
        } catch (Exception e) {
            GizmoView.log(Level.SEVERE, "homohabilus efloobious" + e.toString());
        }
    }

    void intercept() {
        if (this.intercept) {
            this.intercept = false;
        } else {
            this.intercept = true;
        }
    }

    boolean started() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}

