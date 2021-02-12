package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeStrategy;
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
    void mergeBranches() throws IOException, GitAPIException {
        gitHandler.checkoutBranch("branch1");
        Files.createFile(Paths.get(repositoryPath.toString(), "Test1.txt"));
        gitHandler.createCommitOnCurrentBranch("Commit on branch1");
        gitHandler.checkoutBranch("branch2");
        Files.createFile(Paths.get(repositoryPath.toString(), "Test2.txt"));
        gitHandler.mergeBranches("branch2", "branch1", MergeStrategy.RECURSIVE);
        assertTrue(Files.exists(Paths.get(repositoryPath.toString(), "Test1.txt")) &&
                Files.exists(Paths.get(repositoryPath.toString(), "Test2.txt"))
        );
    }

    @Test
    void createCommitOnCurrentBranch() throws IOException, GitAPIException {
        try (Git git = Git.open(repositoryPath.toFile())) {
            // Create commit
            Files.createFile(Paths.get(repositoryPath.toString(), "Test.txt"));
            gitHandler.createCommitOnCurrentBranch("TestCommit");

            AnyObjectId head = git.getRepository().resolve(Constants.HEAD);
            Iterator<RevCommit> log = git.log()
                                         .add(head)
                                         .call().iterator();
            assertEquals("TestCommit", log.next().getFullMessage());
            assertEquals("Initial commit", log.next().getFullMessage());
            git.log()
               .add(head)
               .call().forEach(ref -> System.out.println(ref.getFullMessage()));
        }
    }

    @Test
    void getCurrentlyCheckedOutBranch() throws IOException {
        assertEquals("master", gitHandler.getCurrentlyCheckedOutBranch());
    }
}
