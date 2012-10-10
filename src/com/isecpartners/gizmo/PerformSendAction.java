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
import java.util.logging.Level;
import java.util.logging.Logger;

public class PerformSendAction implements Runnable {

    private BlobScroller blob_scroller;
    private boolean intercept;

    public PerformSendAction(BlobScroller blob_scroller, boolean intercept) {
        this.intercept = intercept;
        this.blob_scroller = blob_scroller;
    }

    public void run() {
        try {
            TextBlob current = null;
            GizmoView.log("in " + (intercept ? "intercept " : "non-intercept ") + "mode");
            synchronized (blob_scroller.getUpdater()) {
                current = blob_scroller.getCurrent();

                if (current != null && intercept) {
                    GizmoView.getInterceptScroller().remove(current);
                    GizmoView.log("removing current request from display");
                }
            }
            if (current != null) {
                HttpRequest req = (HttpRequest) current.getMsg();

                req.setInterrimContents(new StringBuffer(current.content().contents()));
                GizmoView.log("setting contents");
                req.fetchResponse(false);
                if (intercept) {
                    GizmoView.log("appending request and response to summary window");
                    GizmoView.getView().append(req, "\n", req.getURL() + "\n");
                    GizmoView.getView().append(req.getResponse(), "\n\n");
                    GizmoView.log("sending data back to client");
                    req.sendDataToClient();
                    GizmoView.log("closing client connection");
                    req.closeClientConnection();
                } else {
                    GizmoView.getView().appendAfter(req.getResponse(), "\n\n", req.getURL() + "\n\n", current);
                }
            }


        } catch (IOException ex) {
            Logger.getLogger(PerformSendAction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
        }

    }
}
