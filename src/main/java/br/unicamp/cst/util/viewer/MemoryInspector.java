/**********************************************************************************************
 * Copyright (c) 2012  DCA-FEEC-UNICAMP
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * K. Raizer, A. L. O. Paraense, E. M. Froes, R. R. Gudwin - initial API and implementation
 ***********************************************************************************************/
package br.unicamp.cst.util.viewer;

import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.util.TimeStamp;
import br.unicamp.cst.util.TreeElement;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author rgudwin
 */
public class MemoryInspector extends javax.swing.JFrame {
    
    MemoryObject m;
    String lastobjectclass;
    DefaultMutableTreeNode ev_tn;
    DefaultMutableTreeNode ts_tn;
    DefaultMutableTreeNode info_tn;
    DefaultMutableTreeNode info_null;
    ObjectTreeNode obj;
    boolean lastwasnull = false;
    ImageIcon pause_icon = new ImageIcon(getClass().getResource("/pause-icon.png")); 
    ImageIcon play_icon = new ImageIcon(getClass().getResource("/play-icon.png"));
    MemoryInspector.MITimerTask tt;
    boolean capture=false;
    ArrayList<String> listtoavoidloops = new ArrayList<>();
    Logger log = Logger.getLogger(MemoryInspector.class.getCanonicalName());

    /**
     * Creates new form MemoryInspector
     *  @param mo MemoryObject to be inspected
     */ 
    public MemoryInspector(MemoryObject mo) {
        initComponents();
        setTitle(mo.getName());
        m = mo;
        Object ob = mo.getI();
        if (m.getI() != null) lastobjectclass = ob.getClass().getCanonicalName();
        obj = new ObjectTreeNode(mo.getName(),TreeElement.ICON_OBJECT);
        float eval = mo.getEvaluation().floatValue();
        ev_tn = obj.addObject(eval,"Eval");
        obj.add(ev_tn);
        long timestamp = mo.getTimestamp();
        String ts = TimeStamp.getStringTimeStamp(timestamp,"dd/MM/yyyy HH:mm:ss.SSS");
        ts_tn = obj.addString(ts,"timeStamp");
        obj.add(ts_tn);
        DefaultTreeModel objTreeModel = new DefaultTreeModel(obj);
        objTree.setModel(objTreeModel);
        objTree.setCellRenderer(new MindRenderer(false));
        StartTimer();
        MouseListener ml;
        ml = new ObjectMouseAdapter(objTree);
        objTree.addMouseListener(ml);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                set_enable(false);
                tt.cancel();
            }
        });
    }
    
    
    public void StartTimer() {
        Timer t = new Timer();
        tt = new MemoryInspector.MITimerTask(this);
        t.scheduleAtFixedRate(tt, 0, 100);
    }
    
    public void updateString(String s, String name) {
        DefaultMutableTreeNode treeNode = obj.updateMap.get(name);
        if (treeNode == null) {
            String[] mc = name.split("\\[");
            if (mc.length > 0) {
               String parent = mc[0];
               treeNode = obj.updateMap.get(parent);
               if (treeNode == null) {
                   // if I am here it means I failed in finding the parent of the unknown node   
                   log.warning("Unable to find the parent of "+name+": "+parent);
                   log.warning("Current List to Avoid Loops");
                    for (String ss : listtoavoidloops) {
                        log.warning(s+"->"+ss);
                    }
                    return;
               }
               // if I am here it means I found the parent of the unknown node   
               DefaultMutableTreeNode parentnode = obj.updateMap.get(parent);
               DefaultMutableTreeNode newnode = obj.addObject(null,name);
               parentnode.add(newnode);
               updateString(s,name);
            }
            else {
               // If I am here this means I failed in finding the variable parent
               log.warning("Trying to update something which does not exist: "+name+": "+s);
               for (String ss : listtoavoidloops) {
                  log.warning(s+"->"+ss);
               }
            }   
            return;
        }
        TreeElement element = (TreeElement) treeNode.getUserObject();
        String nodeName = element.getName();
        if (!name.equalsIgnoreCase(nodeName)) log.warning("Why the node name is different ? "+name+","+nodeName);
        element.setValue(s);
    }
    
    public void updateList(Object o, String name) {
        List ll = (List) o;
        int i=0;
        for (Object ob : ll) {
            updateObject(ob,ToString.el(name, i));
            i++;
        }
    }
    
    public void updateArray(Object o, String name) {
        int l = Array.getLength(o);
        for (int i=0;i<l;i++) {
            Object oo = Array.get(o,i);
            updateObject(oo,ToString.el(name, i));
        }
    }
    
    public void updateObject(Object o, String name) {
        String s = ToString.from(o);
        if (s != null) {
            updateString(s,name);
        }
        else if (o instanceof List) {
            updateList(o,name);
        }
        else if (o.getClass().isArray()) {
            updateArray(o,name);
        }
        else {
            // if the object is not primitive, first update the object element
            updateString(o.toString(),name);
            // then update each of its fields
            if (!listtoavoidloops.contains(name)) {
                listtoavoidloops.add(name);
                Field[] fields = o.getClass().getDeclaredFields();
                for (Field field : fields) {
                    String fname = field.getName();
                    if (!field.isAccessible()) field.setAccessible(true);
                        Object fo=null;
                    try {
                        fo = field.get(o);
                    } catch (Exception e) {e.printStackTrace();}
                    updateObject(fo,fname);
                }
            }
        }
    }
    
    public void updateTree(MemoryObject m) {
        String value;
        // update eval
        TreeElement teval = (TreeElement) ev_tn.getUserObject();
        value = String.format("%4.2f", m.getEvaluation());
        teval.setValue(value);
        // update timestamp
        long timestamp = m.getTimestamp();
        String ts = TimeStamp.getStringTimeStamp(timestamp,"dd/MM/yyyy HH:mm:ss.SSS");
        TreeElement tts = (TreeElement) ts_tn.getUserObject();
        tts.setValue(ts);
        // update info
        Object ii = m.getI();
        if (ii != null) {
            if (capture == true) {
                    set_enable(false);
                    capture = false;
                }
            if (info_tn == null) {
                info_tn = obj.addObject(ii,"Info");
                obj.add(info_tn);
                DefaultTreeModel tm = (DefaultTreeModel) objTree.getModel();
                tm.reload(obj);
                lastwasnull = false;
                return;
            }
            else {// this is not null and last was null
                if (lastwasnull) {
                    obj.remove(info_null);
                    obj.add(info_tn);
                    DefaultTreeModel tm = (DefaultTreeModel) objTree.getModel();
                    tm.reload(obj);
                    lastwasnull = false;
                    return;
                }
                else {// this is not null and last was not null
                    updateObject(ii,"Info");
                    DefaultTreeModel tm = (DefaultTreeModel) objTree.getModel();
                    tm.nodeChanged((TreeNode)tm.getRoot());
                    lastwasnull = false;
                    return;
                }
            } 
        }
        else {
            if (info_null == null) {
                info_null = obj.addObject(null,"Info");
                obj.add(info_null);
                DefaultTreeModel tm = (DefaultTreeModel) objTree.getModel();
                tm.reload(obj);
                lastwasnull = true;
                return;
            } // this is null and last was null
            else if (lastwasnull) {
                DefaultTreeModel tm = (DefaultTreeModel) objTree.getModel();
                tm.nodeChanged((TreeNode)tm.getRoot());
                return;
            }
            else {// this is null and last was not null
                obj.remove(info_tn);
                obj.add(info_null);
                DefaultTreeModel tm = (DefaultTreeModel) objTree.getModel();
                tm.reload(obj);
                lastwasnull = true;
                return;
            }
        }
    }
    
    public void tick() {
        listtoavoidloops.clear();
        if (m != null) {
            updateTree(m);
        } else {
            log.warning("Mind is null");
        }
    }
    
    class MITimerTask extends TimerTask {

        MemoryInspector wov;
        boolean enabled = true;

        public MITimerTask(MemoryInspector wovi) {
            wov = wovi;
        }

        public void run() {
            if (enabled) {
                wov.tick();
            }
        }

        public void setEnabled(boolean value) {
            enabled = value;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        objTree = new javax.swing.JTree();
        bPause = new javax.swing.JButton();
        bStop = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jScrollPane1.setViewportView(objTree);

        bPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pause-icon.png"))); // NOI18N
        bPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bPauseActionPerformed(evt);
            }
        });

        bStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stop-playing-icon.png"))); // NOI18N
        bStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(bPause, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bStop, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(bPause, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bStop, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bPauseActionPerformed
        if (tt.enabled == true) set_enable(false);
        else set_enable(true);
    }//GEN-LAST:event_bPauseActionPerformed

    private void bStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bStopActionPerformed
        capture = true;
    }//GEN-LAST:event_bStopActionPerformed

    public void set_enable(boolean state) {
        if (state == true) bPause.setIcon(pause_icon); 
        else bPause.setIcon(play_icon); 
        tt.enabled = state; 
    } 
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bPause;
    private javax.swing.JButton bStop;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree objTree;
    // End of variables declaration//GEN-END:variables
}

class ObjectMouseAdapter extends MouseAdapter {
    
    public ObjectMouseAdapter(JTree ttree)  {
        tree = ttree;
    }
    
    private JTree tree;
    
    @Override
    public void mousePressed(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    if(e.getClickCount() == 1 && e.getButton() == 3) {
                        DefaultMutableTreeNode tn = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                        TreeElement te = (TreeElement)tn.getUserObject();
                        DefaultMutableTreeNode parentnode = (DefaultMutableTreeNode)tn.getParent();
                        final Object element=te.getElement();
                        if(element instanceof Inspectable ) {
                            JPopupMenu popup = new JPopupMenu();
                            JMenuItem jm1 = new JMenuItem("Inspect");
                            ActionListener al = new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    ((Inspectable) element).inspect();
                                }
                            };
                            jm1.addActionListener(al);
                            popup.add(jm1);
                            popup.show(tree, e.getX(), e.getY());
                        }
                        else if (element instanceof MemoryObject || element instanceof MemoryContainer) {
                            MemoryObject mo = (MemoryObject) element;
                            JPopupMenu popup = new JPopupMenu();
                            JMenuItem jm1 = new JMenuItem("Inspect");
                            ActionListener al = new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String className = "Empty";
                                    Object o = mo.getI();
                                    if (o != null) className = o.getClass().getCanonicalName();
                                    MemoryViewer mv = new MemoryViewer(mo);
                                    mv.setVisible(true);
                                }
                            };
                            jm1.addActionListener(al);
                            popup.add(jm1);
                            popup.show(tree, e.getX(), e.getY());
                        }
                    }
                }
            }
}    
