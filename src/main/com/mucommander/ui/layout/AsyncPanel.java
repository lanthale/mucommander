/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.layout;

import com.mucommander.commons.runtime.OsFamily;
import com.mucommander.utils.text.Translator;
import com.mucommander.ui.icon.SpinningDial;
import ru.trolsoft.ui.ZxSpectrumLoadPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;

/**
 * <code>AsyncPanel</code> is a <code>JPanel</code> aimed at components that potentially take a long time to
 * initialize. It allows to deport their initialization in a separate thread as to not lock the event thread.
 * It works as follows:
 * <ol>
 *  <li>Initially, AsyncPanel displays a 'please wait component' that symbolizes the fact that the contents of the
 *      panel is being loaded.</li>
 *  <li>Then the initialization of the component will be executed in background thread by the method {@link #initTargetComponent()}</li>
 *  <li>When AsyncPanel becomes visible on screen, the {@link #getTargetComponent(Exception)} method is called to trigger the
 *      initialization of the real component to display.</li>
 *  <li>As soon as the method returns, the wait component is removed and the target component added to AsyncPanel.
 *      If AsyncPanel is the child of a <code>java.awt.Window</code>, the window is repacked to take into account the
 *      new size of this panel.</li>
 * </ol>
 *
 * <p>This panel tries to be as 'transparent' as possible for the target component: the borders of this panel are empty
 * and its layout is a <code>BorderLayout</code> where the target component is added to the center.
 *
 * @author Maxence Bernard
 */
public abstract class AsyncPanel extends ZxSpectrumLoadPane {

    private class InitWorker extends SwingWorker<Void, Void> {
        private Exception exception;

        @Override
        protected Void doInBackground() {
            AsyncPanel.this.start();
            try {
                initTargetComponent();
            } catch (Exception e) {
                exception = e;
            }
            return null;
        }

        @Override
        protected void done() {
            JComponent targetComponent = getTargetComponent(exception);
            remove(waitComponent);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            add(targetComponent, BorderLayout.CENTER);
            updateLayout();
            // Force update viewer/editor window on Windows 8.1
            if (OsFamily.WINDOWS.isCurrent()) {
                setSize(getSize());
            }
            AsyncPanel.this.stop();
        }
    }

    /** The component displayed while the target component is being loaded */
    private final JComponent waitComponent;

    /** This field becomes true when this panel has become visible on screen. */
    private boolean visibleOnScreen;

    private InitWorker worker;

    /**
     * Creates a new <code>AsyncPanel</code> with the default wait component.
     */
    protected AsyncPanel() {
        this(getDefaultWaitComponent());
    }

    /**
     * Creates a new <code>AsyncPanel</code> that displays the given component while the target component is being
     * loaded.
     *
     * @param waitComponent the component to display while the target component is being loaded
     */
    private AsyncPanel(JComponent waitComponent) {
        super(new BorderLayout());

        this.waitComponent = waitComponent;
        add(waitComponent, BorderLayout.CENTER);

        // Starts loading the component when this panel has become visible on screen
        addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent e) {
                if (visibleOnScreen) {
                    return;
                }

                visibleOnScreen = true;
                removeAncestorListener(this);

//                loadTargetComponent();
                worker = new InitWorker();
                worker.execute();
            }

            public void ancestorRemoved(AncestorEvent event) {}

            public void ancestorMoved(AncestorEvent event) {}
        });

    }


    protected void cancel() {
        if (worker != null) {
            worker.cancel(true);
        }
    }

    /*
     * Loads the target component by calling {@link #getTargetComponent()} and replace the wait component by it.
     */

//    private void loadTargetComponent() {
//        new Thread() {
//            @Override
//            public void init() {
//                JComponent targetComponent = getTargetComponent();
//
//                remove(waitComponent);
//                setBorder(new EmptyBorder(0, 0, 0, 0));
//
//                add(targetComponent, BorderLayout.CENTER);
//
//                updateLayout();
//            }
//        }.start();
//    }

    /**
     * Returns the default component to be displayed while the target component is being loaded.
     *
     * @return the default component to be displayed while the target component is being loaded
     */
    private static JComponent getDefaultWaitComponent() {
        JLabel label = new JLabel(Translator.get("loading"));
        label.setIcon(new SpinningDial(24, 24, true));

        // Center the label both horizontally and vertically
        JPanel tempPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;

        tempPanel.add(label, gbc);

        return tempPanel;
    }

    
    /**
     * Packs the parent Window that contains this component, if any. This method is called once the target component
     * has been made initialized and added to this panel. This method can be overridden by subclasses if additional work
     * needs to be done to update the layout.
     */
    protected void updateLayout() {
//        Container tla = getTopLevelAncestor();
//        if(tla instanceof Window)
//            ((Window)tla).pack();
    }


    public abstract JComponent getTargetComponent(Exception e);

    public abstract void initTargetComponent() throws Exception;
}
