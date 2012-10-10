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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.awt.event.KeyListener;
import java.util.LinkedList;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 *
 * @author rachel
 */
public class UIEventHandling implements ClipboardOwner {

    private BlobScroller intercept_scroller = null;
    private BlobScroller summary_scroller = null;
    private boolean command_mode = true;
    private LinkedList<JComponent> components = new LinkedList<JComponent>();
    private JComponent summary_component;
    private JComponent intercept_component;
    private JComponent main_component;
    private static UIEventHandling uiv = null;


    public static UIEventHandling getHandler() {
        if (uiv == null) {
            uiv = new UIEventHandling();
        }
        return uiv;
    }

    private UIEventHandling() {
    }

    public void clearInputmaps() {
        summary_component.getInputMap().clear();
        intercept_component.getInputMap().clear();
        main_component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).clear();
    }

    public void setInputMaps() {
        setSummaryInput(summary_component);
        setInterceptInput(intercept_component);
        setupMainInput(main_component);
    }

    public void setupMainInput(JComponent component) {
        this.main_component = component;
        InputMap input = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap action = component.getActionMap();



        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0), "search");
        action.put("search", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                GizmoView.getView().getSearchBox().requestFocusInWindow();
            }
        });
    }

    public void setSummaryInput(JComponent component) {
        this.summary_component = component;
        component.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    summary_scroller.down();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    summary_scroller.up();
                } else if (e.getKeyCode() == KeyEvent.VK_HOME) {
                    summary_scroller.moveToBeginning();
                } else if (e.getKeyCode() == KeyEvent.VK_END) {
                    summary_scroller.moveToEnd();
                }
            }

            public void keyReleased(KeyEvent e) {
            }

        });
        setInputs(component, summary_scroller);
    }

    private void endCommandMode() {
        this.command_mode = false;
    }

    private void setInputs(JComponent component, final BlobScroller scroller) {
        InputMap input = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap action = component.getActionMap();

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "up");
        action.put("up", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                scroller.up();
            }
        });

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "intercept");
        action.put("intercept", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                GizmoView.getView().intercept();
                GizmoView.getView().toggleInterceptRadioButton();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "clear");
        action.put("clear", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                scroller.clear();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "down");
        action.put("down", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                scroller.down();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.SHIFT_DOWN_MASK), "end");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "end");
        action.put("end", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                scroller.moveToEnd();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "edit");
        action.put("edit", new AbstractAction() {

            private boolean startEditMode;

            public void actionPerformed(ActionEvent e) {
                if (!scroller.isSearching()) {
                this.startEditMode = true;
                scroller.edit();
                } else {
                    this.startEditMode = true;
                    scroller.edit();
                }
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.SHIFT_DOWN_MASK), "begin");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "begin");
        action.put("begin", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                scroller.moveToBeginning();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "send");
        action.put("send", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
//                if (!scroller.isSearching()) {
                scroller.sendAction();
//                }
            }
        });

        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "expand");
        action.put("expand", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                TextBlob blob = scroller.getCurrent();
                if (blob.isExpanded()) {
                    scroller.contract(blob);
                } else {
                    scroller.expand(blob);
                }
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "clipboard");
        action.put("clipboard", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(GizmoView.getView().getSelectedText()), UIEventHandling.this);
            }
        });
    }

    public void setInterceptInput(JComponent component) {
        component.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    intercept_scroller.down();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    intercept_scroller.up();
                } else if (e.getKeyCode() == KeyEvent.VK_HOME) {
                    intercept_scroller.moveToBeginning();
                } else if (e.getKeyCode() == KeyEvent.VK_END) {
                    intercept_scroller.moveToEnd();
                }
            }

            public void keyReleased(KeyEvent e) {
            }

        });
        InputMap input = component.getInputMap();
        ActionMap action = component.getActionMap();
        this.intercept_component = component;
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "drop");
        action.put("drop", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                intercept_scroller.dropAction();
            }
        });



        setInputs(component, intercept_scroller);
    }

    /**
     * @param intercept_scroller the intercept_scroller to set
     */
    public void setIntercept_scroller(BlobScroller intercept_scroller) {
        this.intercept_scroller = intercept_scroller;
    }

    /**
     * @param summary_scroller the summary_scroller to set
     */
    public void setSummary_scroller(BlobScroller summary_scroller) {
        this.summary_scroller = summary_scroller;
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }


}
