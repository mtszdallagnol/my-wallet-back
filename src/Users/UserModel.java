package Users;

import Anotations.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Table(TableName = "usuarios")
public class UserModel {
    //
    // =========== Seção de membros privados ===========
    //
    private Integer id;

    @Required
    @MaxLength(100)
    private String nome;

    @Required
    @MaxLength(254)
    @Email
    @Unique
    private String email;

    @Required
    @Password
    @MinLength(8)
    @MaxLength(255)
    private String senha;

    private byte[] salt;

    private Instant data_criacao;

    @Required
    private UserDTO.userType perfil;

    private UserDTO.userInvestimentStyle estilo_investidor;

    //
    // =========== Getters and Setters ===========
    //
    
    public UserModel() { }

    public Integer getId() { return id; }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public UserDTO.userType getPerfil() { return perfil; }

    public byte[] getSalt() { return salt; }

    public void setId(int id) { this.id = id; }

}