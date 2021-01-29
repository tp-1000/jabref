package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the updating of the local and remote git repository that is located at the repository path
 * This provides an easy to use interface to manage the git repository
 */
public class GitHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    private final Path repositoryPath;
    private final File repositoryPathAsFile;
    private final CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(System.getenv("GIT_EMAIL"), System.getenv("GIT_PW"));
    // For now we assume that the git repository is preconfigured until a configuration dialog for this is implemented
    private final String REMOTE_NAME = "origin";

    /**
     * Initialize the handler for the given repository
     *
     * @param repositoryPath The root of the intialized git repository
     */
    public GitHandler(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
        this.repositoryPathAsFile = this.repositoryPath.toFile();
    }

    /**
     * Fetch all changes from the specified remote repository
     *
     * @param remoteName Name of the remote repository Example: "origin" or "upstream"
     */
    public void fetchFromRemote(String remoteName) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            if (!git.getRepository().getRemoteNames().contains(remoteName)) {
                return;
            }
            git.fetch()
               .setRemote(remoteName)
               .call();
        }
    }

    /**
     * Checkout the branch with the specified name, if it does not exist create it
     * @param branchToCheckout Name of the branch to checkout
     */
    public void checkoutBranch(String branchToCheckout) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<Ref> branch = git.branchList()
                                      .call()
                                      .stream()
                                      .filter(ref -> ref.getName().equals(branchToCheckout))
                                      .findAny();
            git.checkout()
               // If the branch does not exist, create it
               .setCreateBranch(branch.isEmpty())
               .setName(branchToCheckout)
               .call();
        }
    }

    /**
     * Merges the source branch into the target branch
     * @param targetBranch the name of the branch that is merged into
     * @param sourceBranch the name of the branch that gets merged
     */
    public void mergeBranches(String targetBranch, String sourceBranch) throws IOException, GitAPIException {
        this.checkoutBranch(targetBranch);
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<Ref> searchBranch = git.branchList()
                                            .call()
                                            .stream()
                                            .filter(ref -> ref.getName().equals(sourceBranch))
                                            .findAny();
            if (searchBranch.isEmpty()) {
                // Do nothing
                return;
            }
            git.merge()
               .include(searchBranch.get())
               .setMessage("Merge search branch into working branch.")
               .call();
        }
    }

    public void createCommitOnCurrentBranch(String commitMessage) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Status status = git.status().call();
            if (!status.isClean()) {
                // Add new and changed files to index
                git.add()
                   .addFilepattern(".")
                   .call();
                // Add all removed files to index
                if (!status.getMissing().isEmpty()) {
                    RmCommand removeCommand = git.rm()
                                                 .setCached(true);
                    status.getMissing().forEach(removeCommand::addFilepattern);
                    removeCommand.call();
                }
                git.commit()
                   .setAllowEmpty(false)
                   .setMessage(commitMessage)
                   .call();
            }
        }
    }

    /**
     * Pushes all commits made to the branch that is tracked by the currently checked out branch.
     * If pushing to remote fails it fails silently.
     */
    public void pushCommitsToRemoteRepository() throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            try {
                git.push()
                   .setCredentialsProvider(credentialsProvider)
                   .call();
            } catch (GitAPIException e) {
                LOGGER.info("Failed to push");
            }
        }
    }
}
