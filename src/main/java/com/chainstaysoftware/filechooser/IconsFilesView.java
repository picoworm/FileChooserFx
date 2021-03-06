package com.chainstaysoftware.filechooser;

import com.chainstaysoftware.filechooser.icons.Icons;
import com.chainstaysoftware.filechooser.preview.PreviewPane;
import com.chainstaysoftware.filechooser.preview.PreviewPaneQuery;
import impl.org.controlsfx.skin.GridViewSkin;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.controlsfx.control.GridView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class IconsFilesView extends AbstractFilesView {
   private static final int NOT_SELECTED = -1;

   // Width and Height of Icon and Filename Cell.
   private static final int CELL_HEIGHT = 90;
   private static final int CELL_WIDTH = 90;

   private final GridView<DirectoryListItem> gridView = new GridView<>();
   private final Map<String, Class<? extends PreviewPane>> previewHandlers;
   private final Icons icons;
   private final IntegerProperty selectedCellIndex = new SimpleIntegerProperty(NOT_SELECTED);
   private final ResourceBundle resourceBundle = ResourceBundle.getBundle("filechooser");
   private final FilesViewCallback callback;

   private EventHandler<? super KeyEvent> keyEventHandler;
   private boolean disableListeners;

   public IconsFilesView(final Stage parent,
                         final Map<String, Class<? extends PreviewPane>> previewHandlers,
                         final Icons icons,
                         final FilesViewCallback callback) {
      super(parent);

      this.previewHandlers = previewHandlers;
      this.icons = icons;
      this.callback = callback;

      gridView.setCellFactory(gridView1 -> {
         final IconGridCell cell = new IconGridCell(true, new IconGridCellContextMenuFactImpl());
         cell.indexProperty().addListener((observable, oldValue, newValue) -> {
            cell.updateSelected(selectedCellIndex.intValue() == newValue.intValue());
         });
         selectedCellIndex.addListener((observable, oldValue, newValue) ->
               cell.updateSelected(newValue.intValue() == cell.getIndex()));
         return cell;
      });
      gridView.setCellHeight(CELL_HEIGHT);
      gridView.setCellWidth(CELL_WIDTH);
      gridView.setOnMouseClicked(new MouseClickHandler());
      gridView.setOnKeyPressed(new KeyClickHandler());
      gridView.setContextMenu(createContextMenu());
   }

   private ContextMenu createContextMenu() {
      final OrderBy initialOrderBy = callback.orderByProperty().get();
      final OrderDirection initialDirection = callback.orderDirectionProperty().get();

      final ToggleGroup toggleGroup = new ToggleGroup();

      final RadioMenuItem nameItem = new RadioMenuItem();
      nameItem.setId("nameMenuItem");
      nameItem.setText(resourceBundle.getString("iconsview.context.name"));
      nameItem.setToggleGroup(toggleGroup);
      nameItem.onActionProperty().setValue(event -> sort(OrderBy.Name));
      if (OrderBy.Name.equals(initialOrderBy)) {
         nameItem.setSelected(true);
      }

      final RadioMenuItem sizeItem = new RadioMenuItem();
      sizeItem.setId("sizeMenuItem");
      sizeItem.setText(resourceBundle.getString("iconsview.context.size"));
      sizeItem.setToggleGroup(toggleGroup);
      sizeItem.onActionProperty().setValue(event -> sort(OrderBy.Size));
      if (OrderBy.Size.equals(initialOrderBy)) {
         sizeItem.setSelected(true);
      }

      final RadioMenuItem typeItem = new RadioMenuItem();
      typeItem.setId("typeMenuItem");
      typeItem.setText(resourceBundle.getString("iconsview.context.type"));
      typeItem.setToggleGroup(toggleGroup);
      typeItem.onActionProperty().setValue(event -> sort(OrderBy.Type));
      if (OrderBy.Type.equals(initialOrderBy)) {
         typeItem.setSelected(true);
      }

      final RadioMenuItem dateItem = new RadioMenuItem();
      dateItem.setId("dateMenuItem");
      dateItem.setText(resourceBundle.getString("iconsview.context.modificationdate"));
      dateItem.setToggleGroup(toggleGroup);
      dateItem.onActionProperty().setValue(event -> sort(OrderBy.ModificationDate));
      if (OrderBy.ModificationDate.equals(initialOrderBy)) {
         dateItem.setSelected(true);
      }

      final CheckMenuItem reverseOrder = new CheckMenuItem();
      reverseOrder.setId("reverserOrderItem");
      reverseOrder.setText(resourceBundle.getString("iconsview.context.reverseorder"));
      reverseOrder.selectedProperty().addListener((observable, oldValue, newValue) -> {
         if (disableListeners) {
            return;
         }

         callback.orderDirectionProperty().setValue(newValue ? OrderDirection.Descending : OrderDirection.Ascending);
         sort(callback.orderByProperty().get());
      });

      disableListeners = true;
      reverseOrder.setSelected(OrderDirection.Descending.equals(initialDirection));
      disableListeners = false;

      final Menu sortOrderMenu = new Menu();
      sortOrderMenu.setId("sortOrderMenu");
      sortOrderMenu.setText(resourceBundle.getString("iconsview.context.arrangeby"));
      sortOrderMenu.getItems().addAll(nameItem, sizeItem, typeItem, dateItem,
            new SeparatorMenuItem(), reverseOrder);

      final ContextMenu contextMenu = new ContextMenu();
      contextMenu.getItems().addAll(sortOrderMenu);

      return contextMenu;
   }

   @Override
   public Node getNode() {
      return gridView;
   }

   @Override
   public void setFiles(final Stream<File> fileStream) {
      selectedCellIndex.setValue(NOT_SELECTED);

      // Disable event listeners in gridView while being updated programmatically
      disableListeners = true;
      gridView.getItems().clear();
      gridView.getItems().setAll(getIcons(sort(fileStream)));
      disableListeners = false;

      selectCurrent();
   }

   /**
    * Sort the existing view contents.
    */
   private void sort(final OrderBy orderBy) {
      callback.orderByProperty().set(orderBy);

      sort();
   }

   /**
    * Sort the existing view contents.
    */
   private void sort() {
      setFiles(getFileStreamFromView());
   }

   /**
    * Sort the passed in Stream<File>
    */
   private Stream<File> sort(final Stream<File> fileStream) {
      return fileStream.sorted(new FileMetaDataComparator(callback.orderByProperty().get(),
           callback.orderDirectionProperty().get()));
   }

   /**
    * Retrieve the files currently shown in view.
    */
   private Stream<File> getFileStreamFromView() {
      // Copy the values out of the UI and into a temp list, so that the UI
      // can be updated without stomping on the grabbed values.
      final List<DirectoryListItem> items = new ArrayList<>(gridView.getItems());

      return items.stream().map(DirectoryListItem::getFile);
   }

   /**
    * If there is a currently selected file, then update the GridView with
    * the selection.
    */
   private void selectCurrent() {
      final File currentSelection = callback.getCurrentSelection();
      final List<DirectoryListItem> items = gridView.getItems();
      IntStream.range(0, items.size())
            .filter(i -> compareFilePaths(items.get(i).getFile(), currentSelection))
            .findFirst()
            .ifPresent(selectedCellIndex::setValue);
   }

   public void setOnKeyPressed(final EventHandler<? super KeyEvent> eventHandler) {
      this.keyEventHandler = eventHandler;
   }

   private List<DirectoryListItem> getIcons(final Stream<File> fileStream) {
      return fileStream.map(this::getDirListItem).collect(Collectors.toList());
   }

   private DirectoryListItem getDirListItem(final File file) {
      return new DirectoryListItem(file, icons.getIconForFile(file));
   }

   private class IconGridCellContextMenuFactImpl implements IconGridCellContextMenuFactory {
      @Override
      public ContextMenu create(final DirectoryListItem item) {
         final ContextMenu contextMenu = createContextMenu();

         final File file = item.getFile();
         if (file.isDirectory()) {
            return null;
         }

         final Class<? extends PreviewPane> previewPaneClass = PreviewPaneQuery.query(previewHandlers, file);
         if (previewPaneClass == null) {
            return null;
         }

         final MenuItem imagePreviewItem = new MenuItem("Preview");
         imagePreviewItem.setOnAction(v -> showPreview(previewPaneClass, file));

         contextMenu.getItems().addAll(new SeparatorMenuItem(), imagePreviewItem);
         return contextMenu;
      }
   }

   private class MouseClickHandler implements EventHandler<MouseEvent> {
      @Override
      public void handle(MouseEvent event) {
         if (!(event.getTarget() instanceof IconGridCell)) {
            return;
         }

         final IconGridCell target = (IconGridCell)event.getTarget();
         if (isDoubleClick(event)) {
            final File file = (File)target.getUserData();
            if (file.isDirectory()) {
               callback.requestChangeDirectory(file);
            } else {
               callback.fireDoneButton();
            }
         } else {
            selectedCellIndex.setValue(target.getIndex());

            final File file = gridView.getItems().get(selectedCellIndex.get()).getFile();
            callback.setCurrentSelection(file);
         }
      }

      private boolean isDoubleClick(MouseEvent event) {
         return event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2;
      }
   }

   private class KeyClickHandler implements EventHandler<KeyEvent> {
      @Override
      public void handle(KeyEvent event) {
         if (keyEventHandler != null) {
            keyEventHandler.handle(event);

            if (event.isConsumed()) {
               return;
            }
         }

         final KeyCode keyCode = event.getCode();
         if (keyCode == KeyCode.LEFT) {
            selectedCellIndex.setValue(Math.max(0, selectedCellIndex.get() - 1));
            event.consume();
         } else if (keyCode == KeyCode.RIGHT) {
            selectedCellIndex.setValue(Math.min(selectedCellIndex.get() + 1, gridView.getItems().size() - 1));
            event.consume();
         } else if (keyCode == KeyCode.UP) {
            selectedCellIndex.setValue(Math.max(0, selectedCellIndex.get() - ((GridViewSkin)gridView.getSkin()).computeMaxCellsInRow()));
            event.consume();
         }  else if (keyCode == KeyCode.DOWN) {
            selectedCellIndex.setValue(Math.min(gridView.getItems().size() - 1, selectedCellIndex.get() + ((GridViewSkin)gridView.getSkin()).computeMaxCellsInRow()));
            event.consume();
         } else if (keyCode == KeyCode.ENTER) {
            if (selectedCellIndex.get() == NOT_SELECTED) {
               return;
            }

            final DirectoryListItem item = gridView.getItems().get(selectedCellIndex.get());
            final File file = item.getFile();
            if (!file.isDirectory()) {
               return;
            }

            callback.requestChangeDirectory(file);
            event.consume();
         }

         if (selectedCellIndex.get() == NOT_SELECTED) {
            return;
         }

         final File file = gridView.getItems().get(selectedCellIndex.get()).getFile();
         callback.setCurrentSelection(file);
      }
   }
}
