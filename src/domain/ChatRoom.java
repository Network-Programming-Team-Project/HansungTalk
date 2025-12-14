package domain;

public class ChatRoom {
    private String id;
    private String name;
    private User[] participants;
    private Message[] messages;
    private Emoji profileEmoji;

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
