package org.tanzu.materialstarter;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/diagnostics")
@CrossOrigin(origins = "*")
public class DiagnosticsController {

    @GetMapping("/env")
    public Map<String, String> getEnvironment() {
        Map<String, String> filtered = new TreeMap<>();
        Map<String, String> env = System.getenv();
        
        // Only show Claude and Anthropic related variables
        env.forEach((key, value) -> {
            if (key.contains("CLAUDE") || key.contains("ANTHROPIC") || 
                key.equals("PATH") || key.equals("HOME")) {
                // Mask the API key for security
                if (key.contains("API_KEY") && value != null && value.length() > 10) {
                    filtered.put(key, value.substring(0, 10) + "..." + value.substring(value.length() - 4));
                } else {
                    filtered.put(key, value);
                }
            }
        });
        
        return filtered;
    }
}

