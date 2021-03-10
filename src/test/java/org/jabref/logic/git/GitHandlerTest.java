package org.jabref.logic.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHandlerTest {
    @TempDir
    Path repositoryPath;
    private GitHandler gitHandler;

    @BeforeEach
    public void setUpGitHandler() {
        gitHandler = new GitHandler(repositoryPath);
    }

    @Test
    void checkoutNewBranch() throws IOException, GitAPIException {
        gitHandler.checkoutBranch("testBranch");

        try (Git git = Git.open(repositoryPath.toFile())) {
            assertEquals("testBranch", git.getRepository().getBranch());
        }
    }

    @Test
    void createCommitOnCurrentBranch() throws IOException, GitAPIException {
        try (Git git = Git.open(repositoryPath.toFile())) {
            // Create commit
            Files.createFile(Path.of(repositoryPath.toString(), "Test.txt"));
            gitHandler.createCommitOnCurrentBranch("TestCommit");

            AnyObjectId head = git.getRepository().resolve(Constants.HEAD);
            Iterator<RevCommit> log = git.log()
                                         .add(head)
                                         .call().iterator();
            assertEquals("TestCommit", log.next().getFullMessage());
            assertEquals("Initial commit", log.next().getFullMessage());
        }
    }

    @Test
    void calculateDiffOnBranch() throws IOException, GitAPIException {
        String expectedPatch = "diff --git a/Test2.txt b/Test2.txt\n" +
                "new file mode 100644\n" +
                "index 0000000..e69de29\n" +
                "--- /dev/null\n" +
                "+++ b/Test2.txt\n" +
                "diff --git a/TestFolder/Test1.txt b/TestFolder/Test1.txt\n" +
                "index 74809e3..2ae1945 100644\n" +
                "--- a/TestFolder/Test1.txt\n" +
                "+++ b/TestFolder/Test1.txt\n" +
                "@@ -1 +1,2 @@\n" +
                "+This is a new line of text 2\n" +
                " This is a new line of text\n";

        gitHandler.checkoutBranch("branch1");
        Files.createDirectory(Path.of(repositoryPath.toString(), "TestFolder"));
        Files.createFile(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"));
        Files.writeString(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"), "This is a new line of text\n");
        gitHandler.createCommitOnCurrentBranch("Commit 1 on branch1");

        Files.createFile(Path.of(repositoryPath.toString(), "Test2.txt"));
        Files.writeString(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt"), "This is a new line of text 2\n" + Files.readString(Path.of(repositoryPath.toString(), "TestFolder", "Test1.txt")));
        gitHandler.createCommitOnCurrentBranch("Commit 2 on branch1");

        assertEquals(expectedPatch, gitHandler.calculateDiffOfBranchHeadAndLastCommitWithChanges("branch1"));
    }

    @Test
    void applyPatch() throws IOException, GitAPIException {
        gitHandler.checkoutBranch("branch1");
        Files.createFile(Path.of(repositoryPath.toString(), "Test1.txt"));
        gitHandler.createCommitOnCurrentBranch("Commit on branch1");
        gitHandler.checkoutBranch("branch2");
        Files.createFile(Path.of(repositoryPath.toString(), "Test2.txt"));
        Files.writeString(Path.of(repositoryPath.toString(), "Test1.txt"), "This is a new line of text\n");
        gitHandler.createCommitOnCurrentBranch("Commit on branch2.");

        String patch = gitHandler.calculateDiffOfBranchHeadAndLastCommitWithChanges("branch2");
        gitHandler.checkoutBranch("branch1");
        gitHandler.applyPatchOnCurrentBranch(patch, "TestCommitMessage");

        assertEquals("This is a new line of text\n", Files.readString(Path.of(repositoryPath.toString(), "Test1.txt")));
    }

    @Test
    void getCurrentlyCheckedOutBranch() throws IOException {
        assertEquals("master", gitHandler.getCurrentlyCheckedOutBranch());
    }
}
