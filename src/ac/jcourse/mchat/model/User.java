package ac.jcourse.mchat.model;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

/**
 * Data class that wraps the user information.
 * 
 * @author Andy Cheung
 * @date 2020-4-27
 */
public final class User {
    private String uuid;
    private AsynchronousSocketChannel channel;
    private String name;

    public User(String uuid, AsynchronousSocketChannel channel, String name) {
        super();
        this.uuid = uuid;
        this.channel = channel;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        User other = (User) obj;
        return Objects.equals(uuid, other.uuid);
    }
}
