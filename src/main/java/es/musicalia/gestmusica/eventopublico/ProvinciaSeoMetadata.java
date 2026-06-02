package es.musicalia.gestmusica.eventopublico;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ProvinciaSeoMetadata {
    CORUNA(
        "Coruña",
        "Consulta las próximas fiestas y verbenas en A Coruña. Encuentra orquestas gallegas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia.",
        "Orquestas, verbenas y fiestas en A Coruña | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en A Coruña. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en A Coruña",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de A Coruña. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en A Coruña con información organizada por artista, localidad y día. Si buscas fiestas en A Coruña, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de A Coruña incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en A Coruña para conocer el lugar, el municipio y la programación prevista."
    ),
    LUGO("Lugo", "Descubre las actuaciones musicales programadas en Lugo. Orquestas gallegas, bandas de verbena y artistas en fiestas populares de los municipios de la provincia."),
    OURENSE(
        "Ourense",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de Ourense. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Orquestas, verbenas y fiestas en Ourense | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en Ourense. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en Ourense",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de Ourense. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en Ourense con información organizada por artista, localidad y día. Si buscas fiestas en Ourense, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de Ourense incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en Ourense para conocer el lugar, el municipio y la programación prevista."
    ),
    PONTEVEDRA(
        "Pontevedra",
        "Consulta las próximas fiestas y verbenas en Pontevedra. Encuentra orquestas gallegas, discotecas móviles y grupos musicales con fechas confirmadas  en los municipios de la provincia.",
        "Orquestas, verbenas y fiestas en Pontevedra | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en Pontevedra. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en Pontevedra",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de Pontevedra. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en Pontevedra con información organizada por artista, localidad y día. Si buscas fiestas en Pontevedra, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de Pontevedra incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en Pontevedra para conocer el lugar, el municipio y la programación prevista."
    ),
    ASTURIAS(
        "Asturias",
        "Programación de orquestas y grupos musicales en Asturias. Fiestas populares, verbenas y eventos culturales con artistas confirmados en los concejos del principado.",
        "Orquestas, verbenas y fiestas en Asturias | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en Asturias. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en Asturias",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en Asturias. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en Asturias con información organizada por artista, localidad y día. Si buscas fiestas en Asturias, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de Asturias incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios del territorio para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en Asturias para conocer el lugar, el municipio y la programación prevista."
    ),
    CANTABRIA("Cantabria", "Actuaciones musicales en Cantabria. Consulta la agenda de orquestas, grupos y artistas para fiestas populares y verbenas en los municipios de la comunidad."),
    ALAVA("Álava", "Fiestas y verbenas en Álava con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia."),
    BIZKAIA(
        "Bizkaia",
        "Agenda musical de Bizkaia. Orquestas, grupos y artistas en las fiestas populares y verbenas de los municipios de la provincia.",
        "Orquestas, verbenas y fiestas en Bizkaia | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en Bizkaia. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en Bizkaia",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en Bizkaia. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en Bizkaia con información organizada por artista, localidad y día. Si buscas fiestas en Bizkaia, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de Bizkaia incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en Bizkaia para conocer el lugar, el municipio y la programación prevista."
    ),
    GIPUZKOA("Gipuzkoa", "Programación de fiestas en Gipuzkoa. Descubre las orquestas, bandas y grupos musicales en los municipios de la provincia para las celebraciones."),
    NAVARRA("Navarra", "Verbenas y fiestas populares en Navarra. Consulta las actuaciones de orquestas y grupos musicales programadas en los municipios de la comunidad."),
    LA_RIOJA("La Rioja", "Agenda de conciertos y verbenas en La Rioja. Orquestas, artistas y grupos musicales en las fiestas populares de los municipios de la comunidad."),
    LEON(
        "León",
        "Fiestas y verbenas en León con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia.",
        "Orquestas, verbenas y fiestas en León | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en León. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en León",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de León. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en León con información organizada por artista, localidad y día. Si buscas fiestas en León, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de León incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en León para conocer el lugar, el municipio y la programación prevista."
    ),
    ZAMORA(
        "Zamora",
        "Agenda musical de Zamora. Orquestas, grupos folklóricos y artistas en las fiestas populares y verbenas de los municipios de la provincia.",
        "Orquestas, verbenas y fiestas en Zamora | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en Zamora. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en Zamora",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de Zamora. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en Zamora con información organizada por artista, localidad y día. Si buscas fiestas en Zamora, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de Zamora incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en Zamora para conocer el lugar, el municipio y la programación prevista."
    ),
    SALAMANCA("Salamanca", "Conciertos y actuaciones en Salamanca. Descubre las orquestas, bandas y grupos musicales programados en las fiestas de los municipios de la provincia."),
    BURGOS(
        "Burgos",
        "Programación de fiestas en Burgos. Orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia.",
        "Orquestas, verbenas y fiestas en Burgos | Festia",
        "Consulta actuaciones musicales, orquestas, verbenas y conciertos en Burgos. Descubre eventos por municipio, artista y fecha con información actualizada en Festia.",
        "Orquestas, verbenas y actuaciones musicales en Burgos",
        "Consulta la agenda de orquestas, verbenas, conciertos y actuaciones musicales previstas en la provincia de Burgos. Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
        "Festia reúne actuaciones musicales confirmadas en Burgos con información organizada por artista, localidad y día. Si buscas fiestas en Burgos, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
        "La programación musical de Burgos incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
        "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical en Burgos para conocer el lugar, el municipio y la programación prevista."
    ),
    PALENCIA("Palencia", "Verbenas y fiestas populares en Palencia. Consulta las actuaciones de orquestas y grupos musicales programadas en los municipios de la provincia."),
    VALLADOLID("Valladolid", "Agenda de conciertos y verbenas en Valladolid. Orquestas, artistas y grupos musicales en las fiestas populares de los municipios de la provincia."),
    SORIA("Soria", "Fiestas y actuaciones musicales en Soria. Descubre las orquestas, grupos y artistas programados en los municipios de la provincia."),
    SEGOVIA("Segovia", "Programación musical en Segovia. Orquestas, bandas y grupos en las fiestas populares y verbenas de los municipios de la provincia."),
    AVILA("Ávila", "Consulta las verbenas y fiestas de Ávila. Actuaciones de orquestas, grupos musicales y artistas en los municipios de la provincia."),
    MADRID("Madrid", "Agenda de eventos musicales en Madrid. Conciertos, fiestas populares y verbenas con orquestas y artistas en los municipios de la comunidad."),
    TOLEDO("Toledo", "Fiestas y actuaciones en Toledo. Programación de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia."),
    CIUDAD_REAL("Ciudad Real", "Verbenas populares en Ciudad Real. Consulta las orquestas y grupos musicales programados en las fiestas de los municipios de la provincia."),
    ALBACETE("Albacete", "Actuaciones musicales en Albacete. Orquestas, grupos y artistas en las fiestas populares de los municipios de la provincia."),
    CUENCA("Cuenca", "Programación de fiestas en Cuenca. Descubre las orquestas, bandas y grupos musicales en los municipios de la provincia para las celebraciones."),
    GUADALAJARA("Guadalajara", "Fiestas y verbenas en Guadalajara. Agenda de orquestas y grupos musicales programados en los municipios de la provincia."),
    BADAJOZ("Badajoz", "Consulta las próximas fiestas y verbenas en Badajoz. Encuentra orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia."),
    CACERES("Cáceres", "Descubre las actuaciones musicales programadas en Cáceres. Orquestas, bandas y artistas en verbenas y fiestas populares de los municipios de la provincia."),
    BARCELONA("Barcelona", "Agenda de conciertos y actuaciones en Barcelona. Consulta las fechas de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia."),
    GIRONA("Girona", "Programación de fiestas en Girona. Orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia."),
    LLEIDA("Lleida", "Verbenas y fiestas populares en Lleida. Consulta las actuaciones de orquestas y grupos musicales programadas en los municipios de la provincia."),
    TARRAGONA("Tarragona", "Actuaciones musicales en Tarragona. Orquestas, grupos y artistas en las fiestas populares de los municipios de la provincia."),
    VALENCIA("Valencia", "Fiestas y verbenas en Valencia con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia."),
    ALICANTE("Alicante", "Agenda musical de Alicante. Orquestas, grupos y artistas en las fiestas populares y verbenas de la Costa Blanca."),
    CASTELLON("Castellón", "Conciertos y actuaciones en Castellón. Descubre las orquestas, bandas y grupos musicales programados en las fiestas de los municipios de la provincia."),
    BALEARES("Baleares", "Programación de orquestas y grupos musicales en Baleares. Fiestas populares, verbenas y eventos en Mallorca, Menorca, Ibiza y Formentera."),
    LAS_PALMAS("Las Palmas", "Consulta las próximas fiestas y verbenas en Las Palmas. Encuentra orquestas, grupos y artistas con fechas confirmadas en Gran Canaria, Lanzarote y Fuerteventura."),
    TENERIFE("Tenerife", "Agenda de eventos musicales en Santa Cruz de Tenerife. Conciertos, fiestas populares y verbenas en Tenerife, La Gomera, La Palma y El Hierro."),
    SEVILLA("Sevilla", "Fiestas y actuaciones en Sevilla. Programación de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia."),
    MALAGA("Málaga", "Verbenas populares en Málaga. Consulta las orquestas y grupos musicales programados en las fiestas de la Costa del Sol."),
    CADIZ("Cádiz", "Actuaciones musicales en Cádiz. Orquestas, grupos y artistas en las fiestas populares de los municipios de la provincia."),
    CORDOBA("Córdoba", "Programación de fiestas en Córdoba. Descubre las orquestas, bandas y grupos musicales en los municipios de la provincia para las celebraciones."),
    GRANADA("Granada", "Agenda de conciertos y verbenas en Granada. Orquestas, artistas y grupos musicales en las fiestas populares de los municipios de la provincia."),
    JAEN("Jaén", "Fiestas y actuaciones musicales en Jaén. Descubre las orquestas, grupos y artistas programados en los municipios de la provincia."),
    ALMERIA("Almería", "Consulta las verbenas y fiestas de Almería. Actuaciones de orquestas, grupos musicales y artistas en los municipios de la provincia."),
    HUELVA("Huelva", "Agenda de eventos musicales en Huelva. Conciertos, fiestas populares y verbenas con orquestas y artistas en los municipios de la provincia."),
    ZARAGOZA("Zaragoza", "Fiestas y verbenas en Zaragoza con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia."),
    HUESCA("Huesca", "Agenda musical de Huesca. Orquestas, grupos y artistas en las fiestas populares y verbenas de los municipios de la provincia."),
    TERUEL("Teruel", "Conciertos y actuaciones en Teruel. Descubre las orquestas, bandas y grupos musicales programados en las fiestas de los municipios de la provincia."),
    MURCIA("Murcia", "Programación de fiestas en Murcia. Orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la región."),
    CEUTA("Ceuta", "Actuaciones musicales en Ceuta. Consulta la agenda de orquestas, grupos y artistas para fiestas populares y verbenas en la ciudad autónoma."),
    MELILLA("Melilla", "Fiestas y actuaciones musicales en Melilla. Descubre las orquestas, grupos y artistas programados en la ciudad autónoma.");

    private static final Map<String, ProvinciaSeoMetadata> BY_PROVINCIA = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(ProvinciaSeoMetadata::provincia, Function.identity()));

    private final String provincia;
    private final String textoProvincia;
    private final SeoCopy seoOverride;

    ProvinciaSeoMetadata(String provincia, String textoProvincia) {
        this(provincia, textoProvincia, null, null, null, null, null, null, null);
    }

    ProvinciaSeoMetadata(
        String provincia,
        String textoProvincia,
        String titulo,
        String descripcion,
        String bloqueTitulo,
        String parrafo1,
        String parrafo2,
        String parrafo3,
        String parrafo4
    ) {
        this.provincia = provincia;
        this.textoProvincia = textoProvincia;
        this.seoOverride = titulo == null
            ? null
            : new SeoCopy(titulo, descripcion, bloqueTitulo, parrafo1, parrafo2, parrafo3, parrafo4);
    }

    public String provincia() {
        return provincia;
    }

    public static String textoProvinciaPara(String provincia) {
        ProvinciaSeoMetadata metadata = BY_PROVINCIA.get(provincia);
        if (metadata != null) {
            return metadata.textoProvincia;
        }
        return "Consulta las próximas fiestas y verbenas en " + provincia + ". Encuentra orquestas, discotecas móviles y grupos musicales con fechas confirmadas.";
    }

    public static SeoCopy seoCopyPara(String provincia, String year) {
        ProvinciaSeoMetadata metadata = BY_PROVINCIA.get(provincia);
        if (metadata != null && metadata.seoOverride != null) {
            return metadata.seoOverride;
        }
        return new SeoCopy(
            "Fiestas y Orquestas en " + provincia + " " + year + " | Festia",
            "Descubre las fiestas populares y verbenas de " + provincia + ". Orquestas, grupos musicales y discotecas móviles con fechas y horarios confirmados.",
            "Fiestas, verbenas y actuaciones musicales en " + provincia,
            "Consulta la agenda de orquestas, verbenas y actuaciones musicales previstas en " + provincia + ". Encuentra eventos por municipio, revisa qué artistas actúan en cada fecha y descubre fiestas populares con música en directo.",
            "Festia reúne actuaciones musicales confirmadas en " + provincia + " con información organizada por artista, localidad y día. Si buscas fiestas, verbenas este fin de semana u orquestas cerca de ti, aquí puedes localizar los próximos eventos de forma rápida.",
            "La programación musical de " + provincia + " incluye orquestas, grupos de verbena, discotecas móviles y otros espectáculos habituales en fiestas populares y celebraciones locales. Esta página agrupa los eventos disponibles en distintos municipios de la provincia para facilitar su consulta.",
            "También puedes explorar artistas concretos, comparar fechas y acceder al detalle de cada actuación musical para conocer el lugar, el municipio y la programación prevista."
        );
    }

    public record SeoCopy(
        String titulo,
        String descripcion,
        String bloqueTitulo,
        String parrafo1,
        String parrafo2,
        String parrafo3,
        String parrafo4
    ) {}
}
