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
public class UserDTO {
    @Required
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

    private Instant data_criacao;

    private LocalDateTime ultimo_login;

    @Required
    private userType perfil;

    private userInvestimentStyle estilo_investidor;
    
    public UserDTO() { }
}