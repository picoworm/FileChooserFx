package com.chainstaysoftware.filechooser;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.stream.Stream;

/**
 * Callbacks from the {@link FilesView} implementations back into the
 * {@link FileChooserFx}.
 */
public interface FilesViewCallback {
   /**
    * Request that the directory is changed, and update the
    * view to include the directory contents.
    */
   void requestChangeDirectory(File directory);

   /**
    * Get a Stream of File for the the passed in directory.
    */
   Stream<File> getFileStream(File directory);

   /**
    * Update the currently selected file.
    */
   void setCurrentSelection(File file);

   File getCurrentSelection();

   void fireDoneButton();

   void disableAddFavoriteButton(boolean disable);

   void disableRemoveFavoritieButton(boolean disable);

   /**
    * Update all the files in the view.
    */
   void updateFiles();

   ObjectProperty<OrderBy> orderByProperty();

   ObjectProperty<OrderDirection> orderDirectionProperty();

   ObservableList<File> favoriteDirsProperty();

   BooleanProperty showMountPointsProperty();
}
