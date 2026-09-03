package haitai.ht_ax_hackathon.dto;

/** 심사 화면에만 노출하는 과제별 시연 접속 안내입니다. */
public record DemoAccessInfo(
        Type type,
        String username,
        String password,
        String adminUsername,
        String adminPassword,
        String message
) {

    public enum Type {
        CREDENTIALS,
        NO_LOGIN_REQUIRED,
        NO_DEMO
    }

    public static DemoAccessInfo credentials(String username, String password) {
        return new DemoAccessInfo(
                Type.CREDENTIALS, username, password, null, null, "시연 페이지 접속 정보"
        );
    }

    public static DemoAccessInfo credentialsWithAdmin(
            String username,
            String password,
            String adminUsername,
            String adminPassword
    ) {
        return new DemoAccessInfo(
                Type.CREDENTIALS,
                username,
                password,
                adminUsername,
                adminPassword,
                "시연 페이지 접속 정보"
        );
    }

    public static DemoAccessInfo noLoginRequired() {
        return new DemoAccessInfo(
                Type.NO_LOGIN_REQUIRED, null, null, null, null, "로그인 없이 시연 가능합니다."
        );
    }

    public static DemoAccessInfo noDemo(String message) {
        return new DemoAccessInfo(Type.NO_DEMO, null, null, null, null, message);
    }

    public boolean isCredentials() {
        return type == Type.CREDENTIALS;
    }

    public boolean isNoLoginRequired() {
        return type == Type.NO_LOGIN_REQUIRED;
    }

    public boolean hasUsername() {
        return username != null && !username.isBlank();
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    public boolean hasAdminAccount() {
        return adminUsername != null && !adminUsername.isBlank()
                && adminPassword != null && !adminPassword.isBlank();
    }
}
