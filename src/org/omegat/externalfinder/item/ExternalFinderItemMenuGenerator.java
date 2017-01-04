/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool 
          with fuzzy matching, translation memory, keyword search, 
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2016 Chihiro Hio
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.externalfinder.item;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import org.omegat.core.Core;
import org.omegat.externalfinder.ExternalFinder;
import org.openide.awt.Mnemonics;

public class ExternalFinderItemMenuGenerator implements IExternalFinderItemMenuGenerator {

    private final ExternalFinderItem.TARGET target;
    private final boolean popup;

    public ExternalFinderItemMenuGenerator(ExternalFinderItem.TARGET target, boolean popup) {
        this.target = target;
        this.popup = popup;
    }

    @Override
    public List<Component> generate() {
        List<Component> menuItems = new ArrayList<Component>();
        List<ExternalFinderItem> finderItems = ExternalFinder.getItems();
        if (finderItems.isEmpty()) {
            return menuItems;
        }

        // generate menu
        if (popup) {
            menuItems.add(new JPopupMenu.Separator());
        } else {
            menuItems.add(new JToolBar.Separator());
        }
        for (int i = 0, n = finderItems.size(); i < n; i++) {
            ExternalFinderItem finderItem = finderItems.get(i);
            if (popup && finderItem.isNopopup()) {
                continue;
            }
            if (target == ExternalFinderItem.TARGET.ASCII_ONLY
                    && finderItem.isNonAsciiOnly()) {
                continue;
            } else if (target == ExternalFinderItem.TARGET.NON_ASCII_ONLY
                    && finderItem.isAsciiOnly()) {
                continue;
            }

            JMenuItem item = new JMenuItem();
            Mnemonics.setLocalizedText(item, finderItem.getName());

            // set keyboard shortcut
            if (!popup) {
                item.setAccelerator(finderItem.getKeystroke());
            }
            item.addActionListener(new ExternalFinderItemActionListener(finderItem));

            menuItems.add(item);
        }
        if (popup) {
            menuItems.add(new JPopupMenu.Separator());
        }
        return menuItems;
    }

    private static class ExternalFinderItemActionListener implements ActionListener {

        private final List<ExternalFinderItemURL> URLs;
        private final List<ExternalFinderItemCommand> commands;

        public ExternalFinderItemActionListener(ExternalFinderItem finderItem) {
            this.URLs = finderItem.getURLs();
            this.commands = finderItem.getCommands();
        }

        public void actionPerformed(ActionEvent e) {
            final String selection = Core.getEditor().getSelectedText();
            if (selection == null) {
                return;
            }

            final String targetWords = selection; // selection.trim();
            final boolean isASCII = ExternalFinderItem.isASCII(targetWords);

            new Thread(new Runnable() {

                public void run() {
                    for (ExternalFinderItemURL url : URLs) {
                        if ((isASCII && (url.getTarget() == ExternalFinderItem.TARGET.NON_ASCII_ONLY))
                                || (!isASCII && (url.getTarget() == ExternalFinderItem.TARGET.ASCII_ONLY))) {
                            continue;
                        }

                        try {
                            Desktop.getDesktop().browse(ExternalFinderItem.generateURL(url, targetWords));
                        } catch (IOException | URISyntaxException ex) {
                            Logger.getLogger(ExternalFinderItemMenuGenerator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }).start();

            new Thread(new Runnable() {

                public void run() {
                    for (ExternalFinderItemCommand command : commands) {
                        if ((isASCII && (command.getTarget() == ExternalFinderItem.TARGET.NON_ASCII_ONLY))
                                || (!isASCII && (command.getTarget() == ExternalFinderItem.TARGET.ASCII_ONLY))) {
                            continue;
                        }

                        try {
                            Runtime.getRuntime().exec(ExternalFinderItem.generateCommand(command, targetWords));
                        } catch (IOException ex) {
                            Logger.getLogger(ExternalFinderItemMenuGenerator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }).start();
        }
    }
}
