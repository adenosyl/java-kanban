import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

public class FileBackedTaskManagerContractTest extends TaskManagerBaseTest<FileBackedTaskManager> {

    @TempDir
    Path tempDir;

    @Override
    protected FileBackedTaskManager createManager() {

        File file = tempDir.resolve("store.csv").toFile();
        return new FileBackedTaskManager(file);
    }
}