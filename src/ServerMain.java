import network.SocketServer;
import java.io.File;

public class ServerMain {
    public static void main(String[] args) {
        try {
            // 1. Launch MiniGame Servers
            launchGameServer("MINIGAMES/TypingGame", "server.TypingGameServer", "8000"); // Internal port 8000
            launchGameServer("MINIGAMES/SwipeBreakoutGame", "server.GameServer", "9000"); // Internal port 9000
            launchGameServer("MINIGAMES/VolleyGame", "server.VolleyServer", "9001"); // Internal port 9001 (Changed from
                                                                                     // 9000)

            // 2. Start Main Kakao Server
            SocketServer server = new SocketServer(12345);
            server.start();

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void launchGameServer(String projectPath, String mainClass, String portArg) {
        new Thread(() -> {
            try {
                String classpath = "../" + projectPath + "/bin"; // Assuming running from NPTP-KakaoTalk/bin or similar,
                                                                 // adj as needed
                // Actually, let's use absolute paths or relative to project root if we run from
                // IDE
                // Better to use "java -cp .../bin mainClass"

                String userDir = System.getProperty("user.dir");
                // Parent dir of NPTP-KakaoTalk is '네프'
                String rootDir = new java.io.File(userDir).getParent();
                String gameBinPath = rootDir + "/" + projectPath + "/bin";

                System.out.println("Launching " + mainClass + " from " + gameBinPath);

                File gameDir = new File(rootDir + "/" + projectPath); // Set CWD to the project root (e.g.,
                                                                      // MINIGAMES/TypingGame)
                System.out.println(
                        "Setting gameDir: " + gameDir.getAbsolutePath() + " (Exists: " + gameDir.exists() + ")");

                ProcessBuilder pb = new ProcessBuilder("java", "-cp", gameBinPath, mainClass, portArg);
                pb.directory(gameDir); // Critical: Set working directory so it finds local assets (words.txt, etc.)
                pb.inheritIO(); // Show output in main console
                Process p = pb.start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (p.isAlive()) {
                        p.destroy();
                        System.out.println("Stopped " + mainClass);
                    }
                }));

                p.waitFor();
            } catch (Exception e) {
                System.err.println("Failed to launch " + mainClass);
                e.printStackTrace();
            }
        }).start();
    }
}
