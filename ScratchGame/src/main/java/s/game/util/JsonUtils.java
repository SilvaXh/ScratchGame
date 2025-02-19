package s.game.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

public class JsonUtils {

    public static JSONObject parseJsonFile(String filePath) throws IOException {
        JSONObject jsonConfig = null;
        try {
            // Obtain the ClassLoader for the current class
            ClassLoader classLoader = JsonUtils.class.getClassLoader();

            // Use the ClassLoader to load the resource as InputStream
            InputStream inputStream = classLoader.getResourceAsStream(filePath);

            if (inputStream != null) {
                // Read contents from InputStream
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();

                // Parse the JSON content
                jsonConfig = new JSONObject(content.toString());
            } else {
                System.err.println("Resource not found: " + filePath);
            }

            return jsonConfig;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}