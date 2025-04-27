package Users;

import Anotations.Table;

import java.time.Instant;

@Table("usuarios")
public class UserModel {
    //                                                   //
    // =========== Seção de membros privados =========== //
    //                                                   //

    private Integer id;

    private String nome;

    private String email;

    private String senha;

    private byte[] salt;

    private Instant data_criacao;

    private UserDTO.userType perfil;

    private UserDTO.userInvestimentStyle estilo_investidor;

    //                                           //
    // =========== Getters e Setters =========== //
    //                                           //
    
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

    public UserDTO.userInvestimentStyle getEstilo_investidor() { return this.estilo_investidor; }

    public void setId(Integer id) { this.id = id; }

    public void setSenha(String senha) { this.senha = senha; }

    public void setSalt(byte[] salt) {this.salt = salt; }

    public void setData_criacao(Instant data_criacao) { this.data_criacao = data_criacao; }

}