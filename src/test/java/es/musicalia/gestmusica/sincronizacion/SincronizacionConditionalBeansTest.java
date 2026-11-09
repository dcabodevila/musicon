package es.musicalia.gestmusica.sincronizacion;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusicalegacy.ocupacion.OcupacionLegacyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pins the mariadb.datasource.enabled guard: the legacy sync service, job and API exist only when it is "true".
 */
class SincronizacionConditionalBeansTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(OcupacionService.class, () -> mock(OcupacionService.class))
            .withBean("ocupacionLegacyService", OcupacionLegacyService.class, () -> mock(OcupacionLegacyService.class))
            .withBean(ArtistaRepository.class, () -> mock(ArtistaRepository.class))
            .withBean(SincronizacionRepository.class, () -> mock(SincronizacionRepository.class))
            .withBean(ProvinciaRepository.class, () -> mock(ProvinciaRepository.class))
            .withBean(MunicipioRepository.class, () -> mock(MunicipioRepository.class))
            .withBean(OcupacionRepository.class, () -> mock(OcupacionRepository.class))
            .withBean(TarifaRepository.class, () -> mock(TarifaRepository.class))
            .withUserConfiguration(SincronizacionService.class, SincronizacionJob.class, SincronizacionController.class);

    @Test
    void beansAreRegisteredWhenMariadbIsEnabled() {
        runner.withPropertyValues("mariadb.datasource.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SincronizacionService.class);
            assertThat(context).hasSingleBean(SincronizacionJob.class);
            assertThat(context).hasSingleBean(SincronizacionController.class);
        });
    }

    @Test
    void beansAreAbsentWhenPropertyIsMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SincronizacionService.class);
            assertThat(context).doesNotHaveBean(SincronizacionJob.class);
            assertThat(context).doesNotHaveBean(SincronizacionController.class);
        });
    }

    @Test
    void beansAreAbsentWhenMariadbIsDisabled() {
        runner.withPropertyValues("mariadb.datasource.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SincronizacionService.class);
            assertThat(context).doesNotHaveBean(SincronizacionJob.class);
            assertThat(context).doesNotHaveBean(SincronizacionController.class);
        });
    }
}
