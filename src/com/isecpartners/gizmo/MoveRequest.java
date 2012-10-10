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
 * @author rachel
 */
public class MoveRequest extends UpdateRequest{
    private int direction = -1;

    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int RIGHT = 3;
    public static final int LEFT = 4;

    public MoveRequest(int direction) {
        this.direction = direction;
    }

    public boolean isUp() { return direction == UP; }
        public boolean isDown() { return direction == DOWN; }
            public boolean isLeft() { return direction == LEFT; }
                public boolean isRight() { return direction == RIGHT; }
}
