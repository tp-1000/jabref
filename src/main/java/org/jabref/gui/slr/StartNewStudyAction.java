package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.crawler.StudyYamlParser;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.study.Study;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class StartNewStudyAction extends ExistingStudySearchAction {
    ImportFormatPreferences importFormatPreferences;
    Study newStudy;

    public StartNewStudyAction(JabRefFrame frame, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor, PreferencesService prefs) {
        super(frame, fileUpdateMonitor, taskExecutor, prefs);
        this.importFormatPreferences = prefs.getImportFormatPreferences();
    }

    @Override
    protected void setupRepository(Path studyRepositoryRoot) throws IOException, GitAPIException {
        StudyYamlParser studyYAMLParser = new StudyYamlParser();
        studyYAMLParser.writeStudyYamlFile(newStudy, studyRepositoryRoot.resolve("study.yml"));
        Git.init()
           .setDirectory(studyRepositoryRoot.toFile())
           .call();
    }

    @Override
    public void execute() {
        Optional<Study> createdStudy = dialogService.showCustomDialogAndWait(new ManageStudyDefinitionView(null, importFormatPreferences));
        if (createdStudy.isEmpty()) {
            return;
        }
        newStudy = createdStudy.get();
        crawl();
    }
}
