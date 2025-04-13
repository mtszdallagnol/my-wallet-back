package Auth;

import Anotations.MaxLength;
import Anotations.Table;

import java.sql.Timestamp;

@Table(TableName = "refresh_tokens")
public class RefreshTokenModel {

    private Integer id;

    private String token;

    private Integer user_id;

    private Timestamp issued_at;

    private Timestamp expires_at;

    private Boolean revoked;


    public Timestamp getExpires_at() { return expires_at; }

    public Boolean getRevoked() { return revoked; }


}
