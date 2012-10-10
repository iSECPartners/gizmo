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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class ExpandAction extends AbstractAction {
    private BlobScroller blobScroller;

    public ExpandAction(GizmoView outer, BlobScroller blobScroller) {
        super();
        this.blobScroller = blobScroller;
    }

    public void actionPerformed(ActionEvent e) {
     /*   TextBlob blob = blobScroller.getCurrent();
        if (blob.isExpanded()) {
            blobScroller.contract(blob);
            blob.setExpanded(false);
        } else {
            blobScroller.expand(blob);
            blob.setExpanded(true);
        }*/
    }

}
