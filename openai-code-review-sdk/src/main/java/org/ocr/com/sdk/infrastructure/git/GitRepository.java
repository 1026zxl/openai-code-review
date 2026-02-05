package org.ocr.com.sdk.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.ocr.com.sdk.domain.model.CodeInfo;
import org.ocr.com.sdk.domain.port.CodeChangeSource;
import org.ocr.com.sdk.exception.ErrorCode;
import org.ocr.com.sdk.exception.GitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Git 仓库操作基础设施（实现 CodeChangeSource 端口）
 *
 * @author SDK Team
 * @since 1.0
 */
public class GitRepository implements CodeChangeSource {
    
    private static final Logger logger = LoggerFactory.getLogger(GitRepository.class);
    
    private final String repositoryPath;
    
    public GitRepository() {
        this(null);
    }
    
    public GitRepository(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }
    
    @Override
    public CodeInfo getLatestDiff() {
        try {
            Repository repository = openRepository();
            
            try (Git git = new Git(repository)) {
                // 获取最近两次提交
                Iterable<RevCommit> commits = git.log().setMaxCount(2).call();
                List<RevCommit> commitList = new ArrayList<>();
                for (RevCommit commit : commits) {
                    commitList.add(commit);
                }
                
                if (commitList.size() < 2) {
                    throw new GitException(ErrorCode.GIT_COMMIT_HISTORY_INSUFFICIENT);
                }
                
                RevCommit newCommit = commitList.get(0);
                RevCommit oldCommit = commitList.get(1);
                
                // 获取提交信息
                String commitMessage = newCommit.getFullMessage().trim();
                String authorName = newCommit.getAuthorIdent().getName();
                String commitTime = String.valueOf(newCommit.getCommitTime());
                String commitHash = newCommit.getName();
                
                // 获取代码差异
                String diffContent = getDiffContent(git, repository, oldCommit, newCommit);
                
                logger.info("提交信息: {}", commitMessage);
                logger.info("提交人: {}", authorName);
                logger.info("提交时间: {}", commitTime);
                logger.info("代码差异行数: {}", diffContent.split("\n").length);
                
                return new CodeInfo(commitMessage, authorName, commitTime, commitHash, diffContent);
            }
        } catch (GitAPIException | IOException e) {
            throw new GitException(ErrorCode.GIT_OPERATION_FAILED, e);
        }
    }
    
    /**
     * 打开Git仓库
     */
    private Repository openRepository() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        
        if (repositoryPath != null && !repositoryPath.isEmpty()) {
            builder.setGitDir(new File(repositoryPath, ".git"));
        } else {
            builder.setGitDir(new File(".git"));
        }
        
        try {
            Repository repository = builder
                    .readEnvironment()
                    .findGitDir()
                    .build();
            
            if (repository.getDirectory() == null) {
                throw new GitException(ErrorCode.GIT_REPOSITORY_NOT_FOUND);
            }
            
            return repository;
        } catch (IOException e) {
            throw new GitException(ErrorCode.GIT_REPOSITORY_NOT_FOUND, e);
        }
    }
    
    /**
     * 获取代码差异内容
     */
    private String getDiffContent(Git git, Repository repository, RevCommit oldCommit, RevCommit newCommit) 
            throws IOException, GitAPIException {
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            oldTreeParser.reset(reader, oldCommit.getTree());
            
            CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
            newTreeParser.reset(reader, newCommit.getTree());
            
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTreeParser)
                    .setNewTree(newTreeParser)
                    .call();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
                diffFormatter.setRepository(repository);
                diffFormatter.format(diffs);
                return outputStream.toString();
            }
        }
    }
}

