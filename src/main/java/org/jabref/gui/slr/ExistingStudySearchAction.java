package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.crawler.git.GitHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExistingStudySearchAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStudySearchAction.class);

    protected final DialogService dialogService;

    private final JabRefFrame frame;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final Path workingDirectory;
    private final TaskExecutor taskExecutor;

    public ExistingStudySearchAction(JabRefFrame frame, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.workingDirectory = getInitialDirectory(frame.prefs().getWorkingDir());
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        crawl();
    }

    public void crawl() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        Optional<Path> studyRepositoryRoot = dialogService.showDirectorySelectionDialog(directoryDialogConfiguration);
        if (studyRepositoryRoot.isEmpty()) {
            // Do nothing if selection was canceled
            return;
        }

        try {
            setupRepository(studyRepositoryRoot.get());
        } catch (IOException | GitAPIException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Study repository could not be created"), e);
            return;
        }
        final Crawler crawler;
        try {
            crawler = new Crawler(studyRepositoryRoot.get(), new GitHandler(studyRepositoryRoot.get()), fileUpdateMonitor, JabRefPreferences.getInstance().getImportFormatPreferences(), JabRefPreferences.getInstance().getSavePreferences(), new BibEntryTypesManager());
        } catch (IOException e) {
            LOGGER.error("Error during reading of study definition file.", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Error during reading of study definition file."), e);
            return;
        }
        BackgroundTask.wrap(() -> {
            crawler.performCrawl();
            return 0; // Return any value to make this a callable instead of a runnable. This allows throwing exceptions.
        })
                      .onFailure(e -> {
                          LOGGER.error("Error during persistence of crawling results.");
                          dialogService.showErrorDialogAndWait(Localization.lang("Error during persistence of crawling results."), e);
                      })
                      .onSuccess(unused -> new OpenDatabaseAction(frame).openFile(Path.of(studyRepositoryRoot.get().toString(), "studyResult.bib"), true))
                      .executeWith(taskExecutor);
    }

    /**
     * Hook for setting up the repository
     */
    protected void setupRepository(Path studyRepositoryRoot) throws IOException, GitAPIException {
        // Do nothing as repository is already setup
    }

    /**
     * @return Path of current panel database directory or the standard working directory
     */
    private Path getInitialDirectory(Path standardWorkingDirectory) {
        if (frame.getBasePanelCount() == 0) {
            return standardWorkingDirectory;
        } else {
            Optional<Path> databasePath = frame.getCurrentLibraryTab().getBibDatabaseContext().getDatabasePath();
            return databasePath.map(Path::getParent).orElse(standardWorkingDirectory);
        }
    }
}
