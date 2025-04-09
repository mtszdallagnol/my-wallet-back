package Users;

import Anotations.*;

import java.time.Instant;
import java.time.LocalDateTime;

enum userType {
    ADMIN, USUARIO, ANALISTA
}
enum userInvestimentStyle {
    CONSERVADOR, MODERADO, ARROJADO
}

@Table(TableName = "usuarios")
public class UserDTO {
    //
    // =========== Seção de membros privados ===========
    //
    private Integer id;

    @Required
    @MaxLength(100)
    private String nome;

    @Required
    @MaxLength(100)
    @Email
    @Unique
    private String email;

    @Required
    @Password
    @MinLength(8)
    @MaxLength(255)
    private String senha;

    @MaxLength(16)
    private byte[] salt;

    private Instant data_criacao;

    private LocalDateTime ultimo_login;

    @Required
    private userType perfil;

    private userInvestimentStyle estilo_investidor;

    //
    // =========== Getters and Setters ===========
    //
    
    public UserDTO() { }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public byte[] getSalt() {
        return salt;
    }

    public Instant getData_criacao() {
        return data_criacao;
    }

    public LocalDateTime getUltimo_login() {
        return ultimo_login;
    }

    public String getPerfil() {
        return perfil.name();
    }

    public String getEstilo_investidor() {
        return estilo_investidor.name();
    }
}