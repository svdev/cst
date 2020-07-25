/**
 * ********************************************************************************************
 * Copyright (c) 2012  DCA-FEEC-UNICAMP
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * K. Raizer, A. L. O. Paraense, E. M. Froes, R. R. Gudwin - initial API and implementation
 * *********************************************************************************************
 */
package br.unicamp.cst.util.viewer;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.entities.Mind;
import br.unicamp.cst.util.TreeElement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author rgudwin
 */
public class MindTreeNode extends DefaultMutableTreeNode {
    
    HashMap<String,DefaultMutableTreeNode> maps = new HashMap<>();
    
    public MindTreeNode() {
        super(new TreeElement("Mind", TreeElement.NODE_NORMAL, "Mind", TreeElement.ICON_MIND));
    }
    
    public MindTreeNode(String name, int icon_type) {
        super(new TreeElement(name, TreeElement.NODE_NORMAL, name, icon_type));
    }
    
    public MindTreeNode(Mind m) {
        super();
        DefaultMutableTreeNode mm = addMind(m);
        maps.put((String)mm.getUserObject(),mm);
        this.add(mm);
        //this.addMemory(m.getRawMemory().getAllMemoryObjects());
    }
    
    public DefaultMutableTreeNode addRootNode(String rootNodeName) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new TreeElement(rootNodeName, TreeElement.NODE_NORMAL, null, TreeElement.ICON_CONFIGURATION));
        return (root);
    }
    
    public DefaultMutableTreeNode addMind(Mind m) {

        //System.out.println("addMind");
        DefaultMutableTreeNode mindNode = addItem("Mind"," "," ",TreeElement.ICON_MIND);
        DefaultMutableTreeNode codeletsNode = addItem("Codelets"," "," ", TreeElement.ICON_CONFIGURATION);
        mindNode.add(codeletsNode);
        CopyOnWriteArrayList<Codelet> codelets = new CopyOnWriteArrayList(m.getCodeRack().getAllCodelets());
        for (Codelet oo : codelets) {
            DefaultMutableTreeNode newcodeletNode = addCodelet(oo);
            codeletsNode.add(newcodeletNode);
        }
        DefaultMutableTreeNode memoriesNode = addItem("Memories"," "," ", TreeElement.ICON_MEMORIES);
        mindNode.add(memoriesNode);
        CopyOnWriteArrayList<Memory> memories = new CopyOnWriteArrayList(m.getRawMemory().getAllMemoryObjects());
        for (Memory mo : memories) {
            DefaultMutableTreeNode memoryNode = addMemory(mo);
            memoriesNode.add(memoryNode);
        }
        //this.add(mindNode);
        return (mindNode);
    }
    
    public void addCodelets(Mind m) {
        CopyOnWriteArrayList<Codelet> codelets;
        if (m.getGroupsNumber() > 0) {
           ConcurrentHashMap<String,ArrayList> groups = m.getGroups();
           Enumeration<String> keys = groups.keys();
           while (keys.hasMoreElements()) {
               String s = keys.nextElement();
               codelets = new CopyOnWriteArrayList(groups.get(s));
               DefaultMutableTreeNode groupNode = addItem(s," "," ", TreeElement.ICON_CODELETS);
               for (Codelet oo : codelets) {
                    DefaultMutableTreeNode newcodeletNode = addCodelet(oo);
                    maps.put(((TreeElement)newcodeletNode.getUserObject()).getName(), newcodeletNode);
                    groupNode.add(newcodeletNode);
               }
               this.add(groupNode);
           }
           
        }
        else {    
           codelets = new CopyOnWriteArrayList(m.getCodeRack().getAllCodelets());
           for (Codelet oo : codelets) {
               DefaultMutableTreeNode newcodeletNode = addCodelet(oo);
               maps.put(((TreeElement)newcodeletNode.getUserObject()).getName(), newcodeletNode);
               this.add(newcodeletNode);
           }
        }   
    }
    
    public void addCodelets(Mind m, String groupName) {
        CopyOnWriteArrayList<Codelet> codelets;
        if (m.getGroupsNumber() > 0) {
           ConcurrentHashMap<String,ArrayList> groups = m.getGroups();
           codelets = new CopyOnWriteArrayList(groups.get(groupName));
           //DefaultMutableTreeNode groupNode = addItem(groupName,"","", TreeElement.ICON_CODELETS);
           for (Codelet oo : codelets) {
                    DefaultMutableTreeNode newcodeletNode = addCodelet(oo);
                    maps.put(((TreeElement)newcodeletNode.getUserObject()).getName(), newcodeletNode);
                    this.add(newcodeletNode);
           }
           //this.add(groupNode);
        }
    }
    
    public void addMemories(Mind m) {
        CopyOnWriteArrayList<Memory> memories = new CopyOnWriteArrayList(m.getRawMemory().getAllMemoryObjects());
        for (Memory mo : memories) {
            DefaultMutableTreeNode memoryNode = addMemory(mo);
            maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
            this.add(memoryNode);
        }
    }
    
    public DefaultMutableTreeNode addIO(Memory m, int icon) {
        //String value = m.getName() + " : ";
        String value = "";
        Object mval = m.getI();
        if (mval != null) {
            value += mval.toString();
        } else {
            value += null;
        }
        DefaultMutableTreeNode memoryNode = addItem(m.getName(),value,m, icon);
        maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
        return (memoryNode);
    }
    
    public DefaultMutableTreeNode addCodelet(Codelet p) {
        DefaultMutableTreeNode codeletNode = addItem(p.getName()," ",p, TreeElement.ICON_CODELET);
        maps.put(((TreeElement)codeletNode.getUserObject()).getName(), codeletNode);
        CopyOnWriteArrayList<Memory> inputs = new CopyOnWriteArrayList(p.getInputs());
        CopyOnWriteArrayList<Memory> outputs = new CopyOnWriteArrayList(p.getOutputs());
        CopyOnWriteArrayList<Memory> broadcasts = new CopyOnWriteArrayList(p.getBroadcast());
        for (Memory i : inputs) {
            DefaultMutableTreeNode memoryNode = addIO(i, TreeElement.ICON_INPUT);
            maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
            codeletNode.add(memoryNode);
        }
        for (Memory o : outputs) {
            DefaultMutableTreeNode memoryNode = addIO(o, TreeElement.ICON_OUTPUT);
            maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
            codeletNode.add(memoryNode);
        }
        for (Memory b : broadcasts) {
            DefaultMutableTreeNode memoryNode = addIO(b, TreeElement.ICON_BROADCAST);
            maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
            codeletNode.add(memoryNode);
        }
        return (codeletNode);
    }
    
    public DefaultMutableTreeNode addItem(String name, String value, Object ob, int icon_type) {
        Object o = new TreeElement(name, value, TreeElement.NODE_NORMAL, ob, icon_type);
        //Object o = new TreeElement(name, "                                                                           ", TreeElement.NODE_NORMAL, value, icon_type);
        DefaultMutableTreeNode memoryNode = new DefaultMutableTreeNode(o);
        maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
        return (memoryNode);
    }

    public DefaultMutableTreeNode addMemory(Memory p) {
        String name = p.getName();
        //System.out.println(name);
        Object obj = p.getI();
        String value;
        if (obj != null) value = p.getI().toString();
        else value = "";
        DefaultMutableTreeNode memoryNode=null; // = addItem(name,value,p,TreeElement.ICON_MEMORIES);
        //maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
        if (p.getClass().getCanonicalName().equals("br.unicamp.cst.core.entities.MemoryObject")) {
            value = "";
            Object pval = p.getI();
            if (pval != null) {
                value += pval.toString();
            } else {
                value += "MemoryObject";
            }
            memoryNode = addItem(name,value,p,TreeElement.ICON_MO);
            maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
        } else if (p.getClass().getCanonicalName().equals("br.unicamp.cst.core.entities.MemoryContainer")) {
            value = "";
            Object pval = p.getI();
            if (pval != null) {
                value += pval.toString();
            } else {
                value += "MemoryContainer";
            }
            memoryNode = addItem(name,value,p,TreeElement.ICON_CONTAINER);
            maps.put(((TreeElement)memoryNode.getUserObject()).getName(), memoryNode);
            MemoryContainer mc = (MemoryContainer) p;
            MemoryContainer mmc;
            MemoryObject mmo;
            int k=0;
            CopyOnWriteArrayList<Memory> allMemories = new CopyOnWriteArrayList(mc.getAllMemories());
            //System.out.println("List of Memories from "+name+" it has "+allMemories.size());
            for (Memory mo : allMemories) {
                if (mo instanceof MemoryObject) {
                    mmo = (MemoryObject)mo;
                    if (mmo.getName().equalsIgnoreCase("")) mmo.setType(name+"("+k+")");
                }
                else if (mo instanceof MemoryContainer) {
                    mmc = (MemoryContainer) mo;
                    if (mmc.getName().equalsIgnoreCase("")) mmc.setType(name+"("+k+")");
                }
                DefaultMutableTreeNode newmemo = addMemory(mo);
                //System.out.println("SubMemory["+k+"]: "+((TreeElement)newmemo.getUserObject()).getName());
                maps.put(((TreeElement)newmemo.getUserObject()).getName(), newmemo);
                memoryNode.add(newmemo);
                k++;
            }
        }
        return (memoryNode);
    }
    
    
    
}