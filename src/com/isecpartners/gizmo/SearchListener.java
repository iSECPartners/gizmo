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
import java.awt.event.ActionListener;


/**
 * This class handles events for the Gizmo search bar.
 *
 * @author Rachel Engel rachel@isecpartners.com
 */
public class SearchListener implements ActionListener {

    /**
     * @param event The event.
     */
    public void actionPerformed(ActionEvent event) {
        String s = event.getActionCommand();
        GizmoView v = GizmoView.getView();

        v.setStatus("Searching for " + s);
        GizmoView.getSummaryScrollers().clearPhraseFromScreen(s);
        if (! s.isEmpty()) {
            v.setStatus("Finished searching for " + s);
        }
        v.grabRoot();
    }
}

