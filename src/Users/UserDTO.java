package Users;

import java.time.Instant;
import java.time.LocalDateTime;

enum userType {
    ADMIN, USUARIO, ANALISTA
}
enum userInvestimentStyle {
    CONSERVADOR, MODERADO, ARROJADO
}
public class UserDTO {
    private Integer id;
    private String nome;
    private String email;
    private String senha;
    private Instant data_criacao;
    private LocalDateTime ultimo_login;
    private userType perfil;
    private userInvestimentStyle estilo_investidor;
    
    public UserDTO() { }
}