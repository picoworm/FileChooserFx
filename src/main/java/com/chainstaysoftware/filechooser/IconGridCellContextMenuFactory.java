package com.chainstaysoftware.filechooser;

import javafx.scene.control.ContextMenu;

/**
 * Build {@link ContextMenu} instance for a single {@link IconGridCell}.
 */
public interface IconGridCellContextMenuFactory {
   ContextMenu create(final DirectoryListItem item);
}
