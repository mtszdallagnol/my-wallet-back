package General;

import Users.UserController;

public class Utils {
    public enum queryType {
        INVALID, SINGLE, MULTIPLE
    }
    public static queryType getParamIdType(String query) {
        if (query == null) {
            return queryType.MULTIPLE;
        } else {
            String[] params = query.split("&");
            if (params.length == 1 && params[0].contains("=")) {
                String[] keyValue = params[0].split("=");
                return (keyValue.length == 2 && keyValue[0].equals("id")) ? queryType.SINGLE : UserController.queryType.INVALID;
            } else {
                return queryType.INVALID;
            }
        }
    }
}
