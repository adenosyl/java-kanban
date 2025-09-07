import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerIoExceptionsTest {

    @TempDir
    Path tempDir;

    @Test
    void saveDoesNotThrow_onValidFile() {
        // Валидный путь — сохранение проходит без исключений
        File f = tempDir.resolve("ok.csv").toFile();
        FileBackedTaskManager m = new FileBackedTaskManager(f);
        assertDoesNotThrow(m::save, "Сохранение в валидный файл не должно кидать исключений");
    }

    @Test
    void saveThrows_onDirectoryPath() {
        // Если путь — это директория, save() должен выбросить ManagerSaveException
        File dir = tempDir.resolve("as_dir").toFile();
        assertTrue(dir.mkdir(), "Не удалось создать директорию для негативного теста");

        FileBackedTaskManager m = new FileBackedTaskManager(dir);
        assertThrows(ManagerSaveException.class, m::save,
                "Сохранение по пути-директории должно приводить к ManagerSaveException");
    }

    @Test
    void loadThrows_onDirectoryPath() {
        // Если загрузка пытается читать из директории, должен лететь ManagerSaveException
        File dir = tempDir.resolve("load_dir").toFile();
        assertTrue(dir.mkdir(), "Не удалось создать директорию для негативного теста");

        assertThrows(ManagerSaveException.class,
                () -> FileBackedTaskManager.loadFromFile(dir),
                "Загрузка из директории должна приводить к ManagerSaveException");
    }
}