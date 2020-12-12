package org.jabref.gui.slr;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * This class controls the user interface of the study definition management dialog. The UI elements and their layout are
 * defined in the FXML file.
 */
public class ManageStudyDefinitionView extends BaseDialog<Study> {
    private final ManageStudyDefinitionViewModel viewModel;

    @FXML private TextField studyTitle;
    @FXML private TextField newAuthor;
    @FXML private TextField newResearchQuestion;
    @FXML private TextField newQuery;
    @FXML private ComboBox<StudyDatabase> databaseSelectorComboBox;

    @FXML private Label disabledSave;
    private final Button saveButton;

    @FXML private Button addAuthorButton;
    @FXML private Button addResearchQuestionButton;
    @FXML private Button addQueryButton;
    @FXML private Button addDatabaseButton;
    @FXML private ButtonType saveButtonType;

    @FXML private ListView<String> authorListView;
    @FXML private ListView<String> questionListView;
    @FXML private ListView<String> queryListView;
    @FXML private TableView<StudyDatabase> databaseTableView;

    @FXML private TableColumn<StudyDatabase, String> databaseTableEntry;
    @FXML private TableColumn<StudyDatabase, StudyDatabase> enabledTableEntry;
    @FXML private TableColumn<StudyDatabase, StudyDatabase> removeTableEntry;

    public ManageStudyDefinitionView(Study study, ImportFormatPreferences importFormatPreferences) {
        this.setTitle(Localization.lang("Manage study definition"));
        viewModel = new ManageStudyDefinitionViewModel(study, importFormatPreferences);
        ViewLoader.view(this).load().setAsDialogPane(this);

        saveButton = ((Button) this.getDialogPane().lookupButton(saveButtonType));
        if (Objects.isNull(study)) {
            // If this is a new study the next step after this dialog is to select the root of the new study repository
            saveButton.setText(Localization.lang("Select study repository root"));
        }

        setResultConverter(button -> {
            if (button == saveButtonType) {
                return viewModel.saveStudy();
            }
            return null;
        });
        registerListenersAndBindings();
    }

    @FXML
    private void initialize() {
        setButtonIcons();
        setButtonToolTips();
        setDataForListViews();
        setCellFactories();
        configureLayout();
    }

    /**
     * Configures all the buttons used directly in the study management GUI
     */
    private void setButtonIcons() {
        addAuthorButton.setGraphic(IconTheme.JabRefIcons.ADD_ARTICLE.getGraphicNode());
        addResearchQuestionButton.setGraphic(IconTheme.JabRefIcons.ADD_ARTICLE.getGraphicNode());
        addQueryButton.setGraphic(IconTheme.JabRefIcons.ADD_ARTICLE.getGraphicNode());
        addDatabaseButton.setGraphic(IconTheme.JabRefIcons.ADD_ARTICLE.getGraphicNode());
        addQueryButton.setGraphic(IconTheme.JabRefIcons.ADD_ARTICLE.getGraphicNode());
    }

    private void setButtonToolTips() {
        addAuthorButton.setTooltip(new Tooltip("Add"));
        addResearchQuestionButton.setTooltip(new Tooltip("Add"));
        addQueryButton.setTooltip(new Tooltip("Add"));
        addDatabaseButton.setTooltip(new Tooltip("Add"));
        addQueryButton.setTooltip(new Tooltip("Add"));
    }

    private void setDataForListViews() {
        authorListView.setItems(viewModel.getAuthors());
        questionListView.setItems(viewModel.getResearchQuestions());
        queryListView.setItems(viewModel.getQueries());

        databaseTableView.setItems(viewModel.getDatabases());

        databaseSelectorComboBox.setItems(viewModel.getNonSelectedDatabases());
    }

    /**
     * Configures which cells are used for the different List-/TableViews
     */
    private void setCellFactories() {
        authorListView.setCellFactory(param -> new StringCellWithDelete());
        questionListView.setCellFactory(param -> new StringCellWithDelete());
        queryListView.setCellFactory(param -> new StringCellWithDelete());

        databaseSelectorComboBox.setCellFactory(param -> new StudyDatabaseComboBoxCell());
        databaseSelectorComboBox.setButtonCell(new StudyDatabaseComboBoxCell());
        databaseTableEntry.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getName()));
        enabledTableEntry.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        removeTableEntry.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));

        enabledTableEntry.setCellFactory(param -> new TableCell<>() {
            final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(StudyDatabase database, boolean empty) {
                super.updateItem(database, empty);

                if (database == null) {
                    setGraphic(null);
                    return;
                }
                checkBox.selectedProperty().setValue(database.isEnabled());
                checkBox.setPrefWidth(20);
                setGraphic(checkBox);
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> database.setEnabled(newValue));
            }
        });
        removeTableEntry.setCellFactory(param -> new TableCell<>() {
            final Button deleteButton = new Button();

            @Override
            protected void updateItem(StudyDatabase database, boolean empty) {
                super.updateItem(database, empty);

                if (database == null) {
                    setGraphic(null);
                    return;
                }
                deleteButton.setGraphic(IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode());
                deleteButton.setPrefWidth(20);
                setGraphic(deleteButton);
                deleteButton.setOnAction(
                        event -> getTableView().getItems().remove(database)
                );
            }
        });
    }

    private void configureLayout() {
        databaseTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void registerListenersAndBindings() {
        // Listen whether any databases are removed from selection -> Add back to the database selector
        databaseTableView.getItems().addListener(viewModel::handleChangeToDatabases);
        studyTitle.textProperty().bindBidirectional(viewModel.titleProperty());
        saveButton.disableProperty().bind(Bindings.or(Bindings.isEmpty(viewModel.getQueries()), Bindings.isEmpty(viewModel.getDatabases())));
        disabledSave.visibleProperty().bind(saveButton.disabledProperty());
    }

    private void saveStudyDefinition() {
        viewModel.saveStudy();
    }

    @FXML
    private void addAuthor() {
        viewModel.addAuthor(newAuthor.getText());
        newAuthor.setText("");
    }

    @FXML
    private void addResearchQuestion() {
        viewModel.addResearchQuestion(newResearchQuestion.getText());
        newResearchQuestion.setText("");
    }

    @FXML
    private void addQuery() {
        viewModel.addQuery(newQuery.getText());
        newQuery.setText("");
    }

    /**
     * Add seleted entry from combobox, push onto database pop from nonselecteddatabase (combobox)
     */
    @FXML
    private void addDatabase() {
        viewModel.addDatabase(databaseSelectorComboBox.getSelectionModel().getSelectedItem());
    }

    private static class StringCellWithDelete extends ListCell<String> {
        HBox hbox = new HBox();
        Label text = new Label();
        Button deleteButton = new Button();

        public StringCellWithDelete() {
            super();
            hbox.setSpacing(20);
            Region spacer = new Region();
            deleteButton.setGraphic(IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode());
            deleteButton.setOnAction(event -> getListView().getItems().remove(getItem()));
            hbox.getChildren().addAll(text, spacer, deleteButton);
            HBox.setHgrow(spacer, Priority.ALWAYS);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);

            if (item != null && !empty) {
                text.setText(item);
                setGraphic(hbox);
            }
        }
    }

    private static class StudyDatabaseComboBoxCell extends ListCell<StudyDatabase> {
        HBox hbox = new HBox();
        Label database = new Label();

        public StudyDatabaseComboBoxCell() {
            super();
            hbox.setSpacing(20);
            hbox.getChildren().add(database);
        }

        @Override
        protected void updateItem(StudyDatabase item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);

            if (item != null && !empty) {
                database.setText(item.getName());
                setGraphic(hbox);
            }
        }
    }
}
