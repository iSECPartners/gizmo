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
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This class forwards messages from the various message handlers to the gui.  There are usually
 * any number of client handlers running in the background that need to forward their responses
 * to the gui.  To put some order around this, they post the messages they handle to a synchronized
 * queue in this class, which in turn forwards the requests on to the display.
 */
public class MessageForwarder implements Runnable {

    /** The protocol handlers for various protocol types are kept here (httphandler is one instance)
     * These handlers are responsible for keeping track of request/response pairs that are currently
     * being processed.
     */
    ArrayList<HTTPHandler> handlers = new ArrayList<HTTPHandler>();

    /** When a protocol handler is created, it's passed into the message forwarder for processing
     * through use of this queue.
     */
    LinkedBlockingQueue<HTTPHandler> handlerQueue = new LinkedBlockingQueue<HTTPHandler>();

    /** When request/response pairs have been processed and are ready to be displayed in the gui,
     * they get put on this queue, and shuffled off to the gui by the loop below
     */
    LinkedBlockingQueue<HTTPMessage> request_queue = new LinkedBlockingQueue<HTTPMessage>();

    private Object lock;
    private GizmoView screen;


    /**
     * @param lock The lock to use.
     * @param screen The screen to write to.
     */
    MessageForwarder(Object lock, GizmoView screen) {
        this.lock = lock;
        this.screen = screen;
    }


    /**
     * @param handler The handler to add to the queue.
     */
    public void addHandler(HTTPHandler handler) {
        handlerQueue.add(handler);
        handler.setLock(lock);
    }


    /**
     * TODO: Explain this method.
     */
    public void run() {
        // XXX: Is this not the same as screen?
        while (true) {
            try {
                while (! request_queue.isEmpty()) {
                    // XXX: request_queue is a queue of HTTPMessages, not
                    // HttpRequests.
                    HttpRequest next = (HttpRequest) request_queue.take();
                    if (next == null) {
                        continue;
                    }
                    if (GizmoView.getView().intercepting() && GizmoView.getView().matchRequest(next.contents())) {
                        screen.addIntercept(next, next.getURL() + "\n");
                    } else {
                        if (! next.isSent()) {
                            synchronized (next) {
                                next.notifyAll();
                            }
                            next.fetchResponse(true);
                        } else {
                            screen.append(next, "\n", next.getURL() + "\n");
                            screen.append(next.getResponse(), "\n\n");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            while (request_queue.isEmpty()) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }


    /**
     * @param request The request to add to the queue.
     */
    void addMessage(HttpRequest request) {
        request_queue.add(request);
    }
}

