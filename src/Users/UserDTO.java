package Users;

import Anotations.*;

public class UserDTO {
    public enum userType {
        ADMIN, USUARIO, ANALISTA
    }
    public enum userInvestimentStyle {
        CONSERVADOR, MODERADO, ARROJADO
    }

    public static class returnedUser {
        private final String nome;

        private final String email;

        private final userType perfil;

        private final userInvestimentStyle estilo_investidor;

        public returnedUser(UserModel user) {
            this.nome = user.getNome();
            this.email = user.getEmail();
            this.perfil = user.getPerfil();
            this.estilo_investidor = user.getEstilo_investidor();
        }
    }

    @Table(TableName = "usuarios")
    public static class postRequirementModel {
        @Required
        @MaxLength(100)
        public String nome;

        @Required
        @MaxLength(254)
        @Email
        @Unique(message = "Email já existente")
        public String email;

        @Required
        @Password
        @MinLength(8)
        @MaxLength(255)
        public String senha;

        @Required
        public userType perfil;
    }

    @Table(TableName = "usuarios")
    public static class updateRequirementModel
    {
        @Required
        public Integer id;

        @MaxLength(100)
        public String nome;

        public userInvestimentStyle estilo_investidor;
    }
}
