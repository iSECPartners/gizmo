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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configuration {

    private Properties config = new Properties();
    final String LOCALLY_BOUND_ADDRESS = "locallyboundaddress";
    final String TERMINATE_SSL = "terminate_ssl";

    public Configuration() {
        setupConfig();
    }

    public void setupConfig() {
        if (new File("config").exists()) {
            try {
                config.load(new FileInputStream("config"));

            } catch (IOException ex) {
                GizmoView.log(ex.toString());
            }
        } else {
            setPort(8080);
        }
    }

    public String getCommandShell() {
        return config.getProperty("command");
    }

    public void setCommandShell(String value) {
        config.setProperty("command", value);
    }

    public String getEditorShell() {
        return config.getProperty("editor");
    }

    public void setEditor(String editor) {
        config.setProperty("editor", editor);
    }

    public void setLocallyBoundAddress(String locallyBoundAddress) {
        config.setProperty(LOCALLY_BOUND_ADDRESS, locallyBoundAddress);
    }

    public InetAddress getLocallyBoundAddress() {
        try {
            if (config.getProperty(LOCALLY_BOUND_ADDRESS) == null) {

                return InetAddress.getByName("localhost");
            } else {
                return InetAddress.getByName(config.getProperty(LOCALLY_BOUND_ADDRESS));
            }

        } catch (UnknownHostException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null; // aware of the perils of saying "this will never happen", but when will there be no localhost.  honestly.

    }

    public int getPort() {
        return Integer.parseInt(config.getProperty("port"));
    }

    void setPort(int port) {
        config.setProperty("port", "" + port);
    }

    public boolean terminateSSL() {
        if (config.getProperty(TERMINATE_SSL) == null) {
           return true;
        } else {
            return Boolean.parseBoolean(config.getProperty(TERMINATE_SSL));
        }
    }


    void save() {
        try {
            config.store(new FileOutputStream("config"), "configuration");
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void setSSLTermination(boolean selected) {
        config.setProperty(TERMINATE_SSL, Boolean.toString(selected));
    }
}
