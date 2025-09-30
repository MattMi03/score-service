package edu.qhjy.score_service.aop;

import lombok.Data;

@Data
public class UserContext {
    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    public static void set(UserInfo user) {
        USER_HOLDER.set(user);
    }

    public static UserInfo get() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }

    @Data
    public static class UserInfo {
        private String userId;
        private String username;
        private String realName;
        private String userType;
        private String js;
        private String dm;

        // 构造、getter、setter
        public UserInfo(String userId, String username, String realName,
                        String userType, String js, String dm) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.userType = userType;
            this.js = js;
            this.dm = dm;
        }
    }
}