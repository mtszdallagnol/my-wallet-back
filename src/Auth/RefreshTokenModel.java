package Auth;

import Anotations.MaxLength;
import Anotations.Table;

import java.sql.Time;
import java.sql.Timestamp;

@Table(TableName = "refresh_tokens")
public class RefreshTokenModel {

    private Integer id;

    private String token;

    private Timestamp expires_at;

    private Timestamp created_at;

    private Integer id_usuario;

    public String getToken() {
        return token;
    }

    public Integer getUser_id() { return id_usuario; }

    public Timestamp getExpires_at() {
        return expires_at;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setExpires_at(Timestamp expires_at) {
        this.expires_at = expires_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

}
