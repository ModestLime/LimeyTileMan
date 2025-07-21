/*
 * Copyright (c) 2025, ModestLime <ModestLime@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.modestlime.limeytileman;

import com.modestlime.limeytileman.events.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
public class LimeyTileManPanel extends PluginPanel {

    private final LimeyTileManPlugin plugin;

    private final LimeyTileManImport limeyTileManImport;

    JButton copyTilesToClipboardButton;
    JButton importFromClipboardButton;
    JButton importTilesLeckeyButton;

    private final String deleteAllTilesString = "Delete ALL Tile Data";
    private final Timer deleteButtonTimer;
    JButton deleteAllTilesButton;
    private static final int clicksToDelete = 5;
    int deleteClicks;

    @Inject
    private ClientThread clientThread;

    @Inject
    private LimeyTileManPanel(LimeyTileManPlugin plugin, LimeyTileManImport limeyTileManImport){
        super();
        this.plugin = plugin;
        this.limeyTileManImport = limeyTileManImport;

        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea backUpWarning = createStyledTextArea("Please back up your tiles before importing from clipboard or Conor Leckey's tileMan!");
        backUpWarning.setForeground(Color.red);
        add(backUpWarning);
        add(Box.createRigidArea(new Dimension(0, 30)));


        JTextArea howToBackup = createStyledTextArea("To backup copy tiles to clipboard and paste somewhere safe.");
        add(howToBackup);


        copyTilesToClipboardButton = new JButton("Copy Tiles To ClipBoard");
        Insets currentMargins = copyTilesToClipboardButton.getMargin(); //so the success text fits cause its like 1 character to long
        copyTilesToClipboardButton.setMargin(new Insets(
                currentMargins.top,
                0,
                currentMargins.bottom,
                0
        ));
        add(copyTilesToClipboardButton);
        copyTilesToClipboardButton.addActionListener(e -> copyTilesToClipboard());
        add(Box.createRigidArea(new Dimension(0, 30)));


        JTextArea willMergeText = createStyledTextArea("Import will merge with current tiles.");
        add(willMergeText);


        importFromClipboardButton = new JButton("Import From ClipBoard");
        add(importFromClipboardButton);
        importFromClipboardButton.addActionListener(e -> importFromClipboard());
        add(Box.createRigidArea(new Dimension(0, 30)));


        JTextArea willMergeText2 = createStyledTextArea("Import will merge with current tiles.");
        add(willMergeText2);


        importTilesLeckeyButton = new JButton("Import From Leckey TileMan");
        add(importTilesLeckeyButton);
        importTilesLeckeyButton.addActionListener(e -> importTilesLeckey());
        add(Box.createRigidArea(new Dimension(0, 30)));


        JTextArea howToDeleteMode = createStyledTextArea("Hold delete and right click to toggle easy delete mode!\n\nWhile in this mode you can hold ctrl and drag your mouse to add tiles under you cursor or hold alt to remove tiles");
        howToDeleteMode.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(howToDeleteMode);


        add(Box.createRigidArea(new Dimension(0, 470)));
        JTextArea deleteTilesWarning = createStyledTextArea("Press 5 times in a row to delete ALL your tiles");
        add(deleteTilesWarning);


        deleteClicks = clicksToDelete;
        deleteButtonTimer = new Timer(5000, e -> {
            deleteClicks = clicksToDelete;
            deleteAllTilesButton.setText(deleteAllTilesString);
            deleteAllTilesButton.setForeground(Color.white);
        });
        deleteButtonTimer.setRepeats(false);
        deleteAllTilesButton = new JButton(deleteAllTilesString);
        deleteAllTilesButton.setForeground(Color.red);
        add(deleteAllTilesButton);
        deleteAllTilesButton.addActionListener(e -> deleteAllTiles());
    }

    private JTextArea createStyledTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        textArea.setOpaque(true);
        return textArea;
    }

    private void deleteAllTiles(){
        if(deleteClicks == 0){return;}
        deleteButtonTimer.stop();
        deleteButtonTimer.start();
        deleteClicks--;
        deleteAllTilesButton.setText(deleteAllTilesString + " " + deleteClicks);
        if (deleteClicks == 0){
            deleteButtonTimer.stop();
            deleteAllTilesButton.setText("Deleting All Tiles!");
            deleteAllTilesButton.setForeground(Color.red);
            clientThread.invokeLater(plugin::deleteAllTiles);
        }
    }


    public void tilesDeleteSuccess(){
        deleteAllTilesButton.setText("Tiles Deleted Successfully!");
        deleteAllTilesButton.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        deleteButtonTimer.start();
    }


    private void importFromClipboard(){
        importFromClipboardButton.setText("Importing Tiles....");
        clientThread.invokeLater(limeyTileManImport::importFromClipBoard);
    }


    @Subscribe
    public void onClipboardImported(ClipboardImported event){
        importFromClipboardButton.setText("Successfully Imported!");
        importFromClipboardButton.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        Timer timer = new Timer(5000, e -> {
            importFromClipboardButton.setText("Import From ClipBoard");
            importFromClipboardButton.setForeground(Color.white);
        });
        timer.setRepeats(false);
        timer.start();
    }


    @Subscribe
    public void onClipboardImportFailed(ClipboardImportFailed event){
        importFromClipboardButton.setText("Failed to Import.");
        importFromClipboardButton.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        Timer timer = new Timer(10000, e -> {
            importFromClipboardButton.setText("Import From ClipBoard");
            importFromClipboardButton.setForeground(Color.white);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void copyTilesToClipboard(){
        copyTilesToClipboardButton.setText("Copying...");
        clientThread.invokeLater(limeyTileManImport::copyTilesToClipBoard);
    }


    @Subscribe
    public void onTilesCopied(TilesCopied event){
        copyTilesToClipboardButton.setText("Successfully Copied To ClipBoard!");
        copyTilesToClipboardButton.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        Timer timer = new Timer(5000, e -> {
            copyTilesToClipboardButton.setText("Copy Tiles To ClipBoard");
            copyTilesToClipboardButton.setForeground(Color.white);
        });
        timer.setRepeats(false);
        timer.start();
    }


    private void importTilesLeckey(){
        importTilesLeckeyButton.setText("Importing Tiles...");
        clientThread.invokeLater(limeyTileManImport::importTilesFromLeckey);
    }


    @Subscribe
    public void onLeckeyTilesImported(LeckeyTilesImported event){
        importTilesLeckeyButton.setText("Imported Tiles Successfully!");
        importTilesLeckeyButton.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        Timer timer = new Timer(5000, e -> {
            importTilesLeckeyButton.setText("Import From Leckey TileMan");
            importTilesLeckeyButton.setForeground(Color.white);
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Subscribe
    public void onLeckeyTilesImportFailed(LeckeyTilesImportFailed event) {
        importTilesLeckeyButton.setText("Imported Tiles Failed");
        importTilesLeckeyButton.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        Timer timer = new Timer(5000, e -> {
            importTilesLeckeyButton.setText("Import From Leckey TileMan");
            importTilesLeckeyButton.setForeground(Color.white);
        });
        timer.setRepeats(false);
        timer.start();
    }

}
