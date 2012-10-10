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

/*
 * ThirdIdea.java
 *
 * Created on May 14, 2009, 3:30:45 PM
 */
package com.isecpartners.gizmo;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.apache.commons.codec.binary.Base64;
import org.jdesktop.application.Action;

/**
 *
 * @author rachel
 */
public class ThirdIdea extends javax.swing.JFrame implements CaretListener {
    private Properties command_translation;
    private BlobScroller scroller;
    private String reqText;
    private List<String> temp_files_to_clean_up = new LinkedList<String>();
    private File buffer_file;
    private static final String command_map_file_name = "command_map.properties";

    /*
     * I think the way to keep text selected after the user's clicked on the command area is to
     * re-highlight any text when the command line gets focus.  If we can't detect when the command
     * line gets focus, we re-highlight it when we execute the command.  Once the command is finished,
     * we re-highlight the new text and start waiting for a click or a keypress.  Either a click or a
     * keypress de-selects the text.  I think. */
    public ThirdIdea(final BlobScroller scroller, final HTTPMessage msg, final boolean request) {
        try {
            this.buffer_file = File.createTempFile("tmp", "end");
        } catch (IOException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        }
        initComponents();

        init(scroller, msg.contents(), new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (request) {
                    scroller.setCommand(jTextPane1.getText(), request, (HttpRequest) msg);
                }
                ThirdIdea.this.dispose();
            }
        });
    }

    public ThirdIdea(final BlobScroller scroller, String str) {
        initComponents();
        init(scroller, str, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ThirdIdea.this.dispose();
            }
        });
    }

    private String apply_macros(String command_string) {
         if (command_translation == null)
             return command_string;

         for (Object obj : command_translation.keySet()) {
             String key = (String)obj;
             String value = (String) command_translation.get(key);
                String regex = "^([\\s]*\\" + key + ").*";
                // so the goal here is to substitute the pattern *only* if it shows up first
                // and ignore trailing spaces.  i want to take the first group, remove that
                // and then prepend the replacement string to what's left.
                 Matcher match = Pattern.compile(regex).matcher(command_string);
                 if (match.matches()) {
                     if (match.groupCount() != 1)
                         break;

                     String found_str = match.group(1);
                     int end_of_match = command_string.indexOf(found_str) + found_str.length();
                     command_string = value + " " + command_string.substring(end_of_match + 1);
                 }
            command_string = Pattern.compile(key).matcher(command_string).replaceAll(value);
         }

         return command_string;
    }

    private void init(final BlobScroller scroller, String str, final AbstractAction action) {
        this.scroller = scroller;
        this.reqText = str;
        if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
            defaultShellField.setText("cmd.exe /c");
        } else {
            defaultShellField.setText("sh -c");
        }
        this.addWindowListener(
            new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e) {action.actionPerformed(null);
            }
        });

        command_translation = new Properties();
            try {
                command_translation.load(new FileInputStream(command_map_file_name));
            } catch (IOException ex) {
				try {
                	FileOutputStream fout = new FileOutputStream(command_map_file_name);
	                fout.write("".getBytes());
	                fout.close();
					} catch (IOException ex1) {}

                GizmoView.log("couldn't find " + command_map_file_name + ".. turning off macro translation");
            }
        jTextPane1.setText(str);
        jTextPane1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        jTextPane1.getActionMap().put("esc", action);

    }

    // Turn an environment map into an environment vector.
    // Get an environment into a vector.  Save the first n slots for use.
    private String[] getenv(int n) {
        Map<String, String> m = System.getenv();

        String ret[] = new String[n + m.size()];
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (e.getKey().toUpperCase().trim().equals("PATH")) {
                ret[n++] = e.getKey() + "=" + e.getValue() + System.getProperty("path.separator") + "scripts";
            } else {
            ret[n++] = e.getKey() + "=" + e.getValue();
            }
        }
        return ret;
    }

    private void cleanup_temp_files() {
        for (String str : temp_files_to_clean_up) {
            new File(str).delete();
        }
    }

    private void executeCommand(final String text) {
        final InputStream procInput;
        new Thread(new Runnable() {

            public void run() {
                try {
                    temp_files_to_clean_up.add(buffer_file.getCanonicalPath());
                    PrintWriter out = new PrintWriter(new FileOutputStream(buffer_file));
                    if (jTextPane1.getSelectedText() != null &&
                            jTextPane1.getSelectedText().length() > 0) {
                        out.write(jTextPane1.getSelectedText());
                    } else {
                    out.write(jTextPane1.getText());
                    }
                    out.close();
                    String env[] = getenv(1);
                    env[0] = "BUF=" + buffer_file.getCanonicalPath();

                    String output = exec(text, env);
                    ThirdIdea.this.shell_output_area.setText(shell_output_area.getText() + output);
                    ThirdIdea.this.shell_output_area.getCaret().setVisible(true);
                    ThirdIdea.this.shell_output_area.setCaretPosition(shell_output_area.getText().length() - 1);
                } catch (IOException ex) {
                    Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);

                } finally {
                    if (jTextPane1.getSelectedText() != null && jTextPane1.getSelectedText().length() > 0) {
                        jTextPane1.replaceSelection(readWholeFile(buffer_file));
                    } else {
                        ThirdIdea.this.jTextPane1.setText(readWholeFile(buffer_file));
                    }
                }

            }

            private String readWholeFile(File file) {
                InputStream in = null;
                String ret = "";
                try {
                    in = new FileInputStream(file);
                    int file_size = Integer.MAX_VALUE;
                    if (file.length() < Integer.MAX_VALUE) {
                        file_size = (int) file.length();
                    }
                    byte[] buf = new byte[file_size];
                    // truncated because for some reason, a byte array is initialized with an int instead of a long
                    // so this's a bit awful, but if you're editing a request bigger than 2G in size, then don't
                    in.read(buf);
                    ret = new String(buf);
                } catch (IOException ex) {
                    Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return ret;
            }
        }).start();




    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        defaultShellField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jTextField1 = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        shell_output_area = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.isecpartners.gizmo.Gizmo.class).getContext().getResourceMap(ThirdIdea.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        defaultShellField.setText(resourceMap.getString("defaultShellField.text")); // NOI18N
        defaultShellField.setName("defaultShellField"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTextPane1.setName("jTextPane1"); // NOI18N
        jScrollPane1.setViewportView(jTextPane1);

        jTextField1.setText(resourceMap.getString("jTextField1.text")); // NOI18N
        jTextField1.setName("jTextField1"); // NOI18N
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jPanel1.setName("jPanel1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        shell_output_area.setBackground(resourceMap.getColor("shell_output_area.background")); // NOI18N
        shell_output_area.setColumns(20);
        shell_output_area.setEditable(false);
        shell_output_area.setRows(5);
        shell_output_area.setName("shell_output_area"); // NOI18N
        jScrollPane2.setViewportView(shell_output_area);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(defaultShellField, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(628, 628, 628))
            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
                    .addComponent(defaultShellField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        executeCommand(jTextField1.getText());
    }//GEN-LAST:event_jTextField1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField defaultShellField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextArea shell_output_area;
    // End of variables declaration//GEN-END:variables

    public String exec(String command_string, String[] env) {
        StringBuffer display_text = new StringBuffer();
        try {
            String shell = defaultShellField.getText();
            Process proc = null;

            command_string = apply_macros(command_string);

            if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
                String[] args = translateCommandline(shell + " " + command_string);
                proc = Runtime.getRuntime().exec(args, env);
            } else {
                String[] shell_pieces = shell.split("\\s+");
                String args[] = new String[shell_pieces.length + 1];
                System.arraycopy(shell_pieces, 0, args, 0, shell_pieces.length);
                args[args.length - 1] = command_string;
                proc = Runtime.getRuntime().exec(args, env);
            }
            String out = readOutput(proc);
            String err = readError(proc);
            proc.waitFor();
            proc.destroy();
            display_text.append("\n\n" + out + err);
        } catch (InterruptedException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            GizmoView.log(e.toString());
        }

        return display_text.toString();
    }

    private void pipe(String command_string) {
        command_string = command_string.trim().replaceFirst("|", "");

    }

    private String readError(Process proc) {
        return readStream(proc, proc.getErrorStream());
    }

    private String readOutput(Process proc) {
        return readStream(proc, proc.getInputStream());
    }

    private static String[] translateCommandline(final String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            // no command? no string
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        Vector v = new Vector();
        StringBuffer current = new StringBuffer();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() != 0) {
                            v.addElement(current.toString());
                            current = new StringBuffer();
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in " + toProcess);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    private String readStream(Process proc, InputStream in) {
        StringBuffer ret = new StringBuffer();
        try {


            int n = in.read();
            while (n != -1) {
                ret.append((char) n);
                n = in.read();
            }
        } catch (IOException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ret.toString();
    }

    private String readWholeFile(String filename) {
        InputStream in = null;
        String ret = "";
        try {
            File f = new File(filename);
            in = new FileInputStream(f);
            int file_size = Integer.MAX_VALUE;
            if (f.length() < Integer.MAX_VALUE) {
                file_size = (int) f.length();
            }
            byte[] buf = new byte[file_size];
            // truncated because for some reason, a byte array is initialized with an int instead of a long
            // so this's a bit awful, but if you're editing a request bigger than 2G in size, then don't
            in.read(buf);
            ret = new String(buf);
        } catch (IOException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }

    private String process_req_variable(String text) {
        PrintWriter out = null;
        String ret = text;
        try {
            File tmp2 = File.createTempFile("tmp", "end");
            temp_files_to_clean_up.add(tmp2.getCanonicalPath());
            out = new PrintWriter(new FileOutputStream(tmp2));
            out.write(reqText);
            out.close();
            ret = ret.replace("$REQ", "\"" + tmp2.getCanonicalPath() + "\"");
        } catch (IOException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
        return ret;
    }

    @Action
    public void b64encode() {
        String original_text = jTextPane1.getSelectedText();
        String new_text = new String(Base64.encodeBase64(original_text.getBytes()));
        jTextPane1.replaceSelection(new_text);
    }

    @Action
    public void b64decode() {
       String original_text = jTextPane1.getSelectedText();
        String new_text = new String(Base64.decodeBase64(original_text.getBytes()));
        jTextPane1.replaceSelection(new_text);
    }

    @Action
    public void urlencode() {
        try {
            String original_text = jTextPane1.getSelectedText();
            String new_text = java.net.URLEncoder.encode(original_text, "UTF-8");
            jTextPane1.replaceSelection(new_text);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void urldecode() {
        try {
            String original_text = jTextPane1.getSelectedText();
            String new_text = java.net.URLDecoder.decode(original_text, "UTF-8");
            jTextPane1.replaceSelection(new_text);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ThirdIdea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void caretUpdate(CaretEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
