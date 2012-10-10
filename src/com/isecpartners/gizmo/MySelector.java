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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rachel
 */
class MySelector extends ProxySelector{

    public MySelector() {
    }

    @Override
    public List<Proxy> select(URI uri) {
        ArrayList<Proxy> al = new ArrayList<Proxy>();
        if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
            al.add(new Proxy(Type.HTTP, new InetSocketAddress("localhost", 8080)));
        } else {

            al.add(Proxy.NO_PROXY);
        }
        return al;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
