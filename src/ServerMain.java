import network.SocketServer;
import java.io.File;

/**
 * 서버 애플리케이션의 진입점 클래스
 * 미니게임 서버들과 메인 카카오톡 서버를 시작
 */
public class ServerMain {
    /** 메인 메서드: 서버 시작 */
    public static void main(String[] args) {
        try {
            // 1. 미니게임 서버들 시작
            launchGameServer("MINIGAMES/TypingGame", "server.TypingGameServer", "8000"); // 타이핑 게임
            launchGameServer("MINIGAMES/SwipeBreakoutGame", "server.GameServer", "9000"); // 벽돌깨기
            launchGameServer("MINIGAMES/VolleyGame", "server.VolleyServer", "9001"); // 배구 게임

            // 2. 메인 카카오 서버 시작
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
                String classpath = "../" + projectPath + "/bin";

                String userDir = System.getProperty("user.dir");
                String rootDir = new java.io.File(userDir).getParent();
                String gameBinPath = rootDir + "/" + projectPath + "/bin";

                System.out.println(mainClass + "를 " + gameBinPath + "에서 실행합니다.");

                File gameDir = new File(rootDir + "/" + projectPath); // 작업 디렉토리를 프로젝트 루트(예: MINIGAMES/TypingGame)로 설정
                System.out.println(
                        "게임 디렉토리 설정: " + gameDir.getAbsolutePath() + " (존재 여부: " + gameDir.exists() + ")");

                ProcessBuilder pb = new ProcessBuilder("java", "-cp", gameBinPath, mainClass, portArg);
                pb.directory(gameDir);
                pb.inheritIO();
                Process p = pb.start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (p.isAlive()) {
                        p.destroy();
                        System.out.println(mainClass + " 중지됨");
                    }
                }));

                p.waitFor();
            } catch (Exception e) {
                System.err.println(mainClass + " 실행 실패");
                e.printStackTrace();
            }
        }).start();
    }
}
