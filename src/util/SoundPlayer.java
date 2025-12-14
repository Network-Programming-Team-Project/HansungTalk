package util;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.net.URL;

/**
 * 사운드 재생 유틸리티 클래스
 * 카카오톡 알림음 등 WAV 파일 재생을 담당
 */
public class SoundPlayer {
  private static boolean soundEnabled = true;

  /**
   * 사운드 활성화/비활성화 설정
   */
  public static void setSoundEnabled(boolean enabled) {
    soundEnabled = enabled;
  }

  /**
   * "카톡!" 알림 사운드 재생
   */
  public static void playKakao() {
    if (!soundEnabled)
      return;
    playSound("/Assets/sounds/kakao.wav");
  }

  /**
   * 지정된 경로의 WAV 파일 재생
   * 
   * @param resourcePath 리소스 경로 (클래스패스 기준)
   */
  public static void playSound(String resourcePath) {
    if (!soundEnabled)
      return;

    new Thread(() -> {
      try {
        URL soundUrl = SoundPlayer.class.getResource(resourcePath);
        if (soundUrl == null) {
          // 파일이 없으면 시스템 beep 사용
          java.awt.Toolkit.getDefaultToolkit().beep();
          ClientLogger.network("Sound file not found: " + resourcePath + ", using system beep");
          return;
        }

        AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundUrl);
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        clip.start();

        // 재생 완료 후 리소스 해제
        clip.addLineListener(event -> {
          if (event.getType() == LineEvent.Type.STOP) {
            clip.close();
            try {
              audioIn.close();
            } catch (Exception e) {
              // ignore
            }
          }
        });
      } catch (Exception e) {
        // 사운드 재생 실패 시 시스템 beep 사용
        java.awt.Toolkit.getDefaultToolkit().beep();
        ClientLogger.network("Sound playback failed: " + e.getMessage());
      }
    }).start();
  }
}
