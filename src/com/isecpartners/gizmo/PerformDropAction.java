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

/**
 *
 * @author av
 */
  class PerformDropAction implements Runnable {
      private BlobScroller blob_scroller;
      private boolean intercept;

    public PerformDropAction(BlobScroller blob_scroller, boolean intercept) {
        this.intercept = intercept;
        this.blob_scroller = blob_scroller;
    }

    public void run() {
        try {
            if (intercept) {
                TextBlob current = null;
                synchronized (blob_scroller.getUpdater()) {
                    current = blob_scroller.getCurrent();

                    if (current != null) {
                        GizmoView.getInterceptScroller().remove(current);
                    } else {
                        System.out.println("Trying to drop non-existing request.");
                    }
                 } // end syncrhonized (updater)
                if (current != null) {
                    HttpRequest req = (HttpRequest) current.getMsg();
                    req.setInterrimContents(new StringBuffer(current.content().contents()));
                    req.setDummyResponse("This Response Was Dropped by Gizmo User.");
                    GizmoView.getSummaryScrollers().add(req, " ** This Request was Manually dropped by Gizmo User **\r\n");
                            //(new AddRequest(req," ** This Request was Manually dropped by Gizmo User **\r\n"));
                }
            }// end if (intercept)
        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
           }
        }
  }