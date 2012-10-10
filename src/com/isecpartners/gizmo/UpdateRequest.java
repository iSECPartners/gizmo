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

/**
 * TODO: Explain this class.
 *
 * @author Rachel Engel rachel@isecpartners.com
 */
abstract class UpdateRequest {
    /** This class is very useful when debugging ui problems.  BlobScreenUpdater does most of the direct
     * updates to the screen.  There are often multiple threads that want to update the screen at the same
     * time, and some synchronousness is needed.  The BlobScreenUpdater is a queue that takes UpdateRequests
     * and processes them all at once.  If you have an update that happens in multiple phases, but all of them
     * need to happen without being interrupted by other display requests, you need to bundle them together.
     * You can use an AggregateRequest for this.  If a string needs to be removed from the display, and another
     * needs to be inserted, you can package them both up in an AggregateRequest, and this will ensure that they
     * get processed together.  Which brings us to the point of the request fields.  All classes that extend this
     * class will get this request number field.  It turns out to be almost essential in debugging some ui update features
     * to know the order of the requests.
     */
    private static int globalRequestNumber = 0;

    /** TODO: Explain this field. */
    protected int requestNumber;

    protected UpdateRequest() {
        requestNumber = globalRequestNumber++;
    }
}

