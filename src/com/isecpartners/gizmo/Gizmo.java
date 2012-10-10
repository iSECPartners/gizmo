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

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class Gizmo extends SingleFrameApplication {
    private static int port = -1;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        GizmoView view = null;
        if (port == -1) {
            view = GizmoView.create(this);
        } else {
            view = GizmoView.create(this, port);
        }
        show(view);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of DesktopApplication1
     */
    public static Gizmo getApplication() {
        return Application.getInstance(Gizmo.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        try {
            JSAP jsap = new JSAP();

            GizmoView.setupLogging();
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");


//            GizmoView.log(UIManager.getLookAndFeel().getName());
            // create a flagged option we'll access using the id "count".
            // it's going to be an integer, with a default value of 1.
            // it's required (which has no effect since there's a default value)
            // its short flag is "n", so a command line containing "-n 5"
            //    will print our message five times.
            // it has no long flag.
            FlaggedOption opt1 = new FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setShortFlag('p');
            jsap.registerParameter(opt1);
            JSAPResult config = jsap.parse(args);
            if (config.contains("port"))
            port = config.getInt("port");
            launch(Gizmo.class, args);
                } catch (JSAPException ex) {
            GizmoView.log(ex.toString());
        }
    }
}
