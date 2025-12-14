package domain;

/**
 * 채팅방 정보를 담는 도메인 클래스
 * 채팅방의 참여자, 메시지 등을 관리
 */
public class ChatRoom {
    private String id; // 채팅방 고유 ID
    private String name; // 채팅방 이름
    private User[] participants; // 참여자 목록
    private Message[] messages; // 메시지 목록
    private Emoji profileEmoji; // 채팅방 프로필 이모지

    /** 기본 생성자: ID와 이름만 설정 */
    public ChatRoom(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public ChatRoom(String id, String name, User[] participants, Emoji profileEmoji, Message[] messages) {
        this.id = id;
        this.name = name;
        this.participants = participants;
        this.profileEmoji = profileEmoji;
        this.messages = messages;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Message[] getMessages() {
        return messages;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void appendParticipant(User participant) {
        int length = participants.length;
        User[] newParticipants = new User[length + 1];
        System.arraycopy(participants, 0, newParticipants, 0, length);
        newParticipants[length] = participant;
        this.participants = newParticipants;
    }

    public void deleteParticipant(String participantId) {
        int length = participants.length;
        User[] newParticipants = new User[length - 1];
        int index = 0;
        for (User participant : participants) {
            if (!participant.getId().equals(participantId)) {
                newParticipants[index++] = participant;
            }
        }
        this.participants = newParticipants;
    }

    public User[] getParticipants() {
        return participants;
    }

    public Emoji getProfileEmoji() {
        return profileEmoji;
    }

    public void setProfileEmoji(Emoji profileEmoji) {
        this.profileEmoji = profileEmoji;
    }
}
