package org.srs.datacat.vfs;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.Objects;
import org.srs.datacat.model.DatacatNode;
import org.srs.datacat.model.security.CallContext;
import org.srs.datacat.vfs.DcFile.GroupType;
import org.srs.vfs.PathMatchers;

/**
 * A Special walker mainly used for searching. 
 * @author bvan
 */
public class DirectoryWalker {

    private final DcFileSystemProvider provider;
    private final ContainerVisitor visitor;
    private final int maxDepth;

    public DirectoryWalker(DcFileSystemProvider provider, ContainerVisitor visitor, int maxDepth){
        this.visitor = visitor;
        this.maxDepth = maxDepth;
        this.provider = provider;
    }

    public DirectoryWalker(DcFileSystemProvider provider, String syntaxAndPattern, int maxDepth){
        this(provider, new ContainerVisitor(syntaxAndPattern), maxDepth);
    }

    public void walk(Path start, CallContext auth) throws IOException{
        FileVisitResult result = walk(start, auth, 0);
        Objects.requireNonNull(result, "FileVisitor returned null");
    }

    private FileVisitResult walk(Path file, CallContext context, int depth) throws IOException{

        DcFile target = null;
        try {
            target = provider.getFile(file, context);
        } catch(AccessDeniedException ex) {
            // Fail if this was the first directory, otherwise skip.
            if(depth == 0){
                throw ex;
            }
            return FileVisitResult.CONTINUE;
        } catch(IOException ex) {
            return visitor.visitFileFailed(file, ex);
        }

        // at maximum depth
        if(depth >= maxDepth){
            return FileVisitResult.CONTINUE;
        }

        // the exception notified to the postVisitDirectory method
        IOException ioe = null;
        FileVisitResult result;

        // invoke preVisitDirectory and then visit each entry
        result = visitor.preVisitDirectory(file, target);
        if(result != FileVisitResult.CONTINUE){
            return result;
        }

        try {
            for(Path dir: target.getAttributeView(SubdirectoryView.class).getChildrenPaths()){
                result = walk(dir, context, depth + 1);

                // returning null will cause NPE to be thrown
                if(result == null || result == FileVisitResult.TERMINATE) {
                    return result;
                }

                // skip remaining siblings in this directory
                if(result == FileVisitResult.SKIP_SIBLINGS) {
                    break;
                }
            }
        } catch(DirectoryIteratorException e) {
            // IOException will be notified to postVisitDirectory
            ioe = e.getCause();
        }

        // invoke postVisitDirectory last
        return visitor.postVisitDirectory(file, ioe);
    }

    /**
     * A Visitor which only visits containers.
     */
    public static class ContainerVisitor extends SimpleFileVisitor<Path> {
        protected final ContainerFilter filter;
        private final LinkedList<DcFile> folderStack = new LinkedList<>();
        public LinkedList<DatacatNode> files = new LinkedList<>();

        public ContainerVisitor(String syntaxAndPattern){
            boolean searchGroups = true;
            boolean searchFolders = true;

            // TODO: This should do some checks to make sure $ is escaped for a regex
            if(syntaxAndPattern.endsWith("$")){
                searchGroups = false;
            } else if(syntaxAndPattern.endsWith("^")){
                searchFolders = false;
            }
            if(!searchFolders || !searchGroups){
                syntaxAndPattern = syntaxAndPattern.substring(0, syntaxAndPattern.length() - 1);
            }
            filter = new ContainerFilter(PathMatchers.getPathMatcher(syntaxAndPattern, "/"), 
                    searchGroups, searchFolders);
        }

        /**
         * Defaults to glob.
         *
         * @param path A Globbular path
         * @param searchGroups Force search in Groups
         * @param searchFolders Force search in folders
         */
        public ContainerVisitor(String path, Boolean searchGroups, Boolean searchFolders){
            // TODO: This should do some checks to make sure $ is escaped for a regex
            if(path.endsWith("$")){
                searchGroups = false;
                path = path.substring(0, path.length() - 1);
            } else if(path.endsWith("^")){
                searchFolders = false;
                path = path.substring(0, path.length() - 1);
            }
            searchGroups = searchGroups == null ? true : searchGroups;
            searchFolders = searchFolders == null ? true : searchFolders;
            // TODO: Glob check?
            String globPath = "glob:" + path;
            filter = new ContainerFilter(PathMatchers.getPathMatcher(globPath, "/"), searchGroups, searchFolders);
        }

        public void accept(DcFile file){
            files.add(file.getObject());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException{
            DcFile file = (DcFile) attrs;
            // Groups can only contain other groups. If we are searching groups, accept the group,
            // otherwise, continue
            if(file.getType() instanceof GroupType){
                if(filter.searchGroups() && filter.matcher.matches(dir)){
                    accept(file);
                }
                return FileVisitResult.SKIP_SUBTREE;
            }
            folderStack.add(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException{
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException{
            DcFile file = folderStack.removeLast();
            if(filter.searchFolders()){
                if(filter.matcher.matches(dir)){
                    accept(file);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public String toString(){
            return "ContainerVisitor{" + "filter=" + filter + '}';
        }
    }

    static class ContainerFilter implements DirectoryStream.Filter<Path> {

        private PathMatcher matcher;
        private final boolean searchGroups;
        private final boolean searchFolders;

        ContainerFilter(PathMatcher matcher, boolean searchGroups, boolean searchFolders){
            this.matcher = matcher;
            this.searchGroups = searchGroups;
            this.searchFolders = searchFolders;
        }

        @Override
        public boolean accept(Path entry) throws IOException{
            return matcher.matches(entry);
        }

        public boolean searchGroups(){
            return this.searchGroups;
        }

        public boolean searchFolders(){
            return this.searchFolders;
        }

        @Override
        public String toString(){
            return "ContainerFilter{" + "matcher=" + matcher + ", searchGroups=" + searchGroups + 
                    ", searchFolders=" + searchFolders + '}';
        }

    }

}
