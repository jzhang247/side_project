package notreallyagroup.backend.users;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class UserFileStorageManager {
    public void add(String path, byte[] content) throws IOException {
        Path p = Path.of("user_storage/" + path);
        Files.createDirectories(p.getParent());
        Files.write(p, content);
    }

    public byte[] get(String path) {
        try {
            return Files.readAllBytes(Path.of("user_storage/" + path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteAll() {
        try {
            FileUtils.cleanDirectory(new File("user_storage"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
