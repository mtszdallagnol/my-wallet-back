package News;

import Anotations.*;

public class NewsDTO {

    @Table("noticias")
    public static class postRequirementModel {
        @Required
        @MaxLength(200)
        public String titulo;

        @Required
        public String categoria;

        @Required
        public String conteudo;

        @Required
        public Integer analista_id;

        public String status;
    }


    @Table("noticias")
    public static class updateRequirementModel {
        @Required
        @Exists
        public Integer id;

        @MaxLength(200)
        public String titulo;

        public String categoria;

        public String conteudo;

        public String status;
    }
}