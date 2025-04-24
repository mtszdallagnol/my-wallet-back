package Users;

import Anotations.MaxLength;
import Anotations.Required;

public class UserDTO {
    public enum userType {
        ADMIN, USUARIO, ANALISTA
    }
    public enum userInvestimentStyle {
        CONSERVADOR, MODERADO, ARROJADO
    }

    public class userUpdate {
        @Required
        private Integer id;

        @Required
        @MaxLength(100)
        private String nome;

        private userInvestimentStyle estilo_investidor;

        public Integer getId() { return this.id; }

        public String getNome() { return this.nome; }

        public userInvestimentStyle getEstilo_investidor() { return estilo_investidor; }
    }
}
