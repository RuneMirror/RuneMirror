package net.runelite.wrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Launcher Wrapper for Custom RuneLite Build
 * 
 * This wrapper JAR replaces the official RuneLite JAR.
 * When Jagex Launcher launches it, this wrapper:
 * 1. Captures all command-line arguments and environment variables
 * 2. Saves them to a file
 * 3. Launches the actual custom RuneLite build with the same arguments
 * 
 * This allows us to use the authentication context from Jagex Launcher
 * while running our custom build.
 */
public class LauncherWrapper {
    
    private static final String CUSTOM_BUILD_JAR = "C:\\Users\\James\\Desktop\\Axiom\\pyosrs\\apps\\RuneLiteCustom\\runelite-client\\target\\client-1.12.7-SNAPSHOT.jar";
    private static final String CAPTURE_FILE = System.getProperty("user.home") + "\\.runelite\\jagex_launch_capture.txt";
    
    public static void main(String[] args) {
        System.out.println("[LauncherWrapper] Wrapper started");
        System.out.println("[LauncherWrapper] Capturing launch arguments...");
        
        // Capture all arguments
        StringBuilder capture = new StringBuilder();
        capture.append("=== JAGEX LAUNCHER ARGUMENTS CAPTURE ===\n\n");
        capture.append("Command Line Arguments:\n");
        for (int i = 0; i < args.length; i++) {
            capture.append(String.format("  [%d] %s\n", i, args[i]));
        }
        capture.append("\n");
        
        // Capture system properties (JVM args)
        capture.append("System Properties (JVM args):\n");
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            // Filter for relevant properties (Jagex/RuneLite related)
            if (key.toLowerCase().contains("jagex") || 
                key.toLowerCase().contains("runelite") ||
                key.toLowerCase().contains("session") ||
                key.toLowerCase().contains("token") ||
                key.toLowerCase().contains("auth") ||
                key.toLowerCase().contains("oauth")) {
                capture.append(String.format("  %s = %s\n", key, entry.getValue()));
            }
        }
        capture.append("\n");
        
        // Capture environment variables
        capture.append("Environment Variables (filtered):\n");
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            if (key.toUpperCase().contains("JAGEX") || 
                key.toUpperCase().contains("RUNELITE") ||
                key.toUpperCase().contains("SESSION") ||
                key.toUpperCase().contains("TOKEN") ||
                key.toUpperCase().contains("AUTH") ||
                key.toUpperCase().contains("OAUTH")) {
                capture.append(String.format("  %s = %s\n", key, entry.getValue()));
            }
        }
        capture.append("\n");
        
        // Save capture
        try {
            Path capturePath = Paths.get(CAPTURE_FILE);
            Files.createDirectories(capturePath.getParent());
            Files.write(capturePath, capture.toString().getBytes());
            System.out.println("[LauncherWrapper] Arguments saved to: " + CAPTURE_FILE);
        } catch (IOException e) {
            System.err.println("[LauncherWrapper] Failed to save capture: " + e.getMessage());
        }
        
        // Build command to launch custom build
        List<String> command = new ArrayList<>();
        command.add("java");
        
        // Add JVM arguments from system properties
        // Extract -D properties and other JVM args
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            
            // Important: pass Jagex auth properties
            if (key.toLowerCase().contains("jagex") || 
                key.toLowerCase().contains("runelite") ||
                key.toLowerCase().contains("session") ||
                key.toLowerCase().contains("token") ||
                key.toLowerCase().contains("oauth")) {
                command.add("-D" + key + "=" + value);
            }
        }
        
        // Add program arguments
        for (String arg : args) {
            command.add(arg);
        }
        
        // Add custom build JAR
        command.add("-jar");
        command.add(CUSTOM_BUILD_JAR);
        
        // Launch custom build
        System.out.println("[LauncherWrapper] Launching custom RuneLite build...");
        System.out.println("[LauncherWrapper] Command: " + String.join(" ", command));
        
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            
            // Inherit environment (important for auth tokens)
            Map<String, String> env = pb.environment();
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                env.put(entry.getKey(), entry.getValue());
            }
            
            pb.inheritIO();
            Process process = pb.start();
            
            System.out.println("[LauncherWrapper] Custom build launched (PID: " + process.pid() + ")");
            
            // Wait for process to exit
            int exitCode = process.waitFor();
            System.out.println("[LauncherWrapper] Custom build exited with code: " + exitCode);
            System.exit(exitCode);
            
        } catch (Exception e) {
            System.err.println("[LauncherWrapper] Failed to launch custom build: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

