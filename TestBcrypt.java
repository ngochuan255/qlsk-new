import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$wT3WfI9vJ0o3q6k8t4WdK.5qE5D1DvhE/Vq5Zf8aMsh8WvJbUu0uK";
        boolean match = encoder.matches("123456", hash);
        System.out.println("Match: " + match);
    }
}
