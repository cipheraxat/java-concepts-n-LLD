# In-Memory File System

**Difficulty:** Medium | **Companies:** Amazon, Google, Dropbox

---

## Requirements

> "Design an in-memory file system that supports creating files and directories, reading/writing file content, listing directory contents, and navigating paths."

### Clarifying Questions

> **You:** "In-memory only, no persistence?"
>
> **Interviewer:** "Correct. Everything lives in memory. Focus on the tree structure and operations."

> **You:** "Do we need to support absolute and relative paths?"
>
> **Interviewer:** "Absolute paths only for simplicity. Paths like `/home/user/file.txt`."

> **You:** "Any concurrency requirements?"
>
> **Interviewer:** "Yes — support concurrent reads and exclusive writes using read-write locks per directory."

> **You:** "Should we track metadata like size and timestamps?"
>
> **Interviewer:** "Yes — name, size, creation time, modification time."

### Final Requirements

```
Requirements:
1. Composite pattern: File and Directory share a common base
2. Create files and directories at a given path
3. Read/write file content
4. List directory contents
5. Delete files and directories
6. Path resolution from root
7. Read-write lock per directory for concurrency

Out of scope:
- Permissions / access control
- Symbolic links / hard links
- Disk quotas
- File search / indexing
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **FileSystem** | Facade. Resolves paths and delegates operations. Owns the root directory. |
| **FileSystemEntry** | Abstract base for File and Directory (Composite pattern). Holds name, timestamps. |
| **File** | Leaf node. Stores content as a byte array or String. Tracks size. |
| **Directory** | Composite node. Contains children (LinkedHashMap for insertion-order listing). Owns a ReentrantReadWriteLock. |

---

## Class Design

### FileSystemEntry (Composite base)

```
abstract class FileSystemEntry:
    - name: String
    - createdAt: Instant
    - modifiedAt: Instant
    - parent: Directory?

    + getName() → String
    + getPath() → String  // walk up parents to build full path
    + isDirectory() → boolean
```

### File

```
class File extends FileSystemEntry:
    - content: String
    - size: int

    + read() → String
    + write(content) → void
    + getSize() → int
```

### Directory

```
class Directory extends FileSystemEntry:
    - children: LinkedHashMap<String, FileSystemEntry>
    - lock: ReentrantReadWriteLock

    + addChild(entry) → void
    + removeChild(name) → FileSystemEntry
    + getChild(name) → FileSystemEntry?
    + listChildren() → List<FileSystemEntry>
    + isDirectory() → true
```

### FileSystem

```
class FileSystem:
    - root: Directory

    + createFile(path) → File
    + createDirectory(path) → Directory
    + readFile(path) → String
    + writeFile(path, content) → void
    + list(path) → List<String>
    + delete(path) → boolean
    - resolve(path) → FileSystemEntry
    - resolveParent(path) → Directory
```

---

## Implementation

### FileSystem.resolve (path resolution)

**Core logic:** Split path by `/`, walk from root through each directory.

```
resolve(path)
    if path == "/" → return root
    parts = path.split("/")  // skip empty first element
    current = root
    for each part in parts (skip empty):
        if current is not Directory → throw not found
        child = current.getChild(part)
        if child is null → throw not found
        current = child
    return current
```

### FileSystem.createFile

```
createFile(path)
    parentPath = path up to last "/"
    fileName = last segment of path
    parent = resolve(parentPath) as Directory

    parent.lock.writeLock().lock()
    try:
        if parent.getChild(fileName) exists → throw already exists
        file = new File(fileName, parent)
        parent.addChild(file)
        return file
    finally:
        parent.lock.writeLock().unlock()
```

### Directory.listChildren (with read lock)

```
listChildren()
    lock.readLock().lock()
    try:
        return new ArrayList<>(children.values())
    finally:
        lock.readLock().unlock()
```

### Complete Code Implementation

```java
import java.time.Instant;

public abstract class FileSystemEntry {
    private final String name;
    private final Instant createdAt;
    private Instant modifiedAt;
    private Directory parent;

    protected FileSystemEntry(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.createdAt = Instant.now();
        this.modifiedAt = this.createdAt;
    }

    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getModifiedAt() { return modifiedAt; }
    protected void touch() { this.modifiedAt = Instant.now(); }
    public Directory getParent() { return parent; }

    public String getPath() {
        if (parent == null) return "/";
        String parentPath = parent.getPath();
        return parentPath.equals("/") ? "/" + name : parentPath + "/" + name;
    }

    public abstract boolean isDirectory();
}
```

```java
public class File extends FileSystemEntry {
    private String content;

    public File(String name, Directory parent) {
        super(name, parent);
        this.content = "";
    }

    public String read() { return content; }

    public void write(String content) {
        this.content = content;
        touch();
    }

    public int getSize() { return content.length(); }

    @Override
    public boolean isDirectory() { return false; }
}
```

```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Directory extends FileSystemEntry {
    private final Map<String, FileSystemEntry> children = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Directory(String name, Directory parent) {
        super(name, parent);
    }

    public void addChild(FileSystemEntry entry) {
        children.put(entry.getName(), entry);
        touch();
    }

    public FileSystemEntry removeChild(String name) {
        FileSystemEntry removed = children.remove(name);
        if (removed != null) touch();
        return removed;
    }

    public FileSystemEntry getChild(String name) {
        return children.get(name);
    }

    public List<FileSystemEntry> listChildren() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(children.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public ReentrantReadWriteLock getLock() { return lock; }

    @Override
    public boolean isDirectory() { return true; }
}
```

```java
import java.util.List;
import java.util.stream.Collectors;

public class FileSystem {
    private final Directory root;

    public FileSystem() {
        this.root = new Directory("", null);
    }

    public File createFile(String path) {
        String[] parts = splitPath(path);
        String fileName = parts[parts.length - 1];
        Directory parent = resolveParentDir(parts);

        parent.getLock().writeLock().lock();
        try {
            if (parent.getChild(fileName) != null)
                throw new IllegalArgumentException("Already exists: " + path);
            File file = new File(fileName, parent);
            parent.addChild(file);
            return file;
        } finally {
            parent.getLock().writeLock().unlock();
        }
    }

    public Directory createDirectory(String path) {
        String[] parts = splitPath(path);
        String dirName = parts[parts.length - 1];
        Directory parent = resolveParentDir(parts);

        parent.getLock().writeLock().lock();
        try {
            if (parent.getChild(dirName) != null)
                throw new IllegalArgumentException("Already exists: " + path);
            Directory dir = new Directory(dirName, parent);
            parent.addChild(dir);
            return dir;
        } finally {
            parent.getLock().writeLock().unlock();
        }
    }

    public String readFile(String path) {
        FileSystemEntry entry = resolve(path);
        if (entry.isDirectory())
            throw new IllegalArgumentException("Not a file: " + path);
        return ((File) entry).read();
    }

    public void writeFile(String path, String content) {
        FileSystemEntry entry = resolve(path);
        if (entry.isDirectory())
            throw new IllegalArgumentException("Not a file: " + path);
        ((File) entry).write(content);
    }

    public List<String> list(String path) {
        FileSystemEntry entry = resolve(path);
        if (!entry.isDirectory())
            throw new IllegalArgumentException("Not a directory: " + path);
        return ((Directory) entry).listChildren().stream()
            .map(e -> e.getName() + (e.isDirectory() ? "/" : ""))
            .collect(Collectors.toList());
    }

    public boolean delete(String path) {
        if (path.equals("/")) throw new IllegalArgumentException("Cannot delete root");
        FileSystemEntry entry = resolve(path);
        Directory parent = entry.getParent();

        parent.getLock().writeLock().lock();
        try {
            return parent.removeChild(entry.getName()) != null;
        } finally {
            parent.getLock().writeLock().unlock();
        }
    }

    private FileSystemEntry resolve(String path) {
        if (path.equals("/")) return root;
        String[] parts = splitPath(path);
        FileSystemEntry current = root;
        for (String part : parts) {
            if (!current.isDirectory())
                throw new IllegalArgumentException("Not a directory: " + current.getPath());
            current = ((Directory) current).getChild(part);
            if (current == null)
                throw new IllegalArgumentException("Not found: " + path);
        }
        return current;
    }

    private Directory resolveParentDir(String[] parts) {
        FileSystemEntry current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.isDirectory())
                throw new IllegalArgumentException(current.getPath() + " is not a directory");
            current = ((Directory) current).getChild(parts[i]);
            if (current == null)
                throw new IllegalArgumentException("Parent not found");
        }
        if (!current.isDirectory())
            throw new IllegalArgumentException("Parent is not a directory");
        return (Directory) current;
    }

    private String[] splitPath(String path) {
        if (!path.startsWith("/"))
            throw new IllegalArgumentException("Path must be absolute");
        String trimmed = path.substring(1);
        if (trimmed.isEmpty())
            throw new IllegalArgumentException("Invalid path");
        return trimmed.split("/");
    }
}
```

### Verification

```
Setup: FileSystem with root "/".

Step 1: createDirectory("/home")
  parent = root. root.getChild("home") → null.
  Create Directory("home", root). root.children = {"home": dir}

Step 2: createDirectory("/home/user")
  resolve parent: root → "home" → Directory.
  Create Directory("user", homeDir). home.children = {"user": dir}

Step 3: createFile("/home/user/notes.txt")
  resolve parent: root → home → user → Directory.
  Create File("notes.txt", userDir).

Step 4: writeFile("/home/user/notes.txt", "Hello World")
  resolve → File. file.write("Hello World"). size=11.

Step 5: readFile("/home/user/notes.txt")
  resolve → File. file.read() → "Hello World" ✓

Step 6: list("/home/user")
  resolve → Directory. listChildren → [notes.txt]
  Output: ["notes.txt"] ✓

Step 7: createFile("/home/user/notes.txt") → throws "Already exists" ✓

Step 8: delete("/home/user/notes.txt")
  parent = userDir. removeChild("notes.txt") → File removed.
  list("/home/user") → [] ✓

Step 9: getPath() on userDir → "/home/user"
  parent.getPath() = "/" + "home" → "/home" + "/" + "user" → "/home/user" ✓
```

---

## Extensibility

### 1. "How would you add file search?"

> "I'd add a `search(rootPath, predicate)` method that recursively walks the tree from the given directory. The predicate can match by name (glob or regex), extension, size range, or modified date. For large file systems, I'd build an inverted index (name → set of paths) updated on create/delete."

### 2. "How would you add permissions?"

> "Each FileSystemEntry gets an `owner` and a permission set (READ, WRITE, EXECUTE for owner/group/others). The FileSystem checks permissions before every operation based on the current user context. This mirrors Unix-style permissions."

### 3. "How would you support symbolic links?"

> "I'd add a `SymLink extends FileSystemEntry` that holds a target path (string). The `resolve` method detects SymLinks and re-resolves the target path, with a depth limit to prevent circular links. The symlink itself doesn't store content — it delegates to whatever the target path resolves to."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should implement the Composite pattern correctly — File and Directory sharing a base class. Creating files and directories, listing contents, and basic path resolution from root are the main goals. Concurrency and metadata are not required.

### Mid-level

Mid-level candidates should use LinkedHashMap for ordered directory children, implement proper path resolution with error handling (not found, wrong type), and add ReentrantReadWriteLock per directory. The delete operation should work for both files and directories. File read/write should be clean.

### Senior

Senior candidates would discuss recursive locking for nested operations (e.g., deleting a non-empty directory), the performance of path resolution in deep trees, and caching strategies for frequent lookups. Symbolic link handling with cycle detection and the trade-offs of per-directory vs. global locking should be addressed.