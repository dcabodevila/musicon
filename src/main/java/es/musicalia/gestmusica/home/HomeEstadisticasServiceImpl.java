package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.listado.ListadoRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HomeEstadisticasServiceImpl implements HomeEstadisticasService {

    static final long DIAS_ULTIMO_MES = 30;

    private final ArtistaRepository artistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ListadoRepository listadoRepository;
    private final OcupacionRepository ocupacionRepository;

    public HomeEstadisticasServiceImpl(ArtistaRepository artistaRepository,
                                       UsuarioRepository usuarioRepository,
                                       ListadoRepository listadoRepository,
                                       OcupacionRepository ocupacionRepository) {
        this.artistaRepository = artistaRepository;
        this.usuarioRepository = usuarioRepository;
        this.listadoRepository = listadoRepository;
        this.ocupacionRepository = ocupacionRepository;
    }

    @Override
    @Cacheable(cacheNames = "homeEstadisticasPlataforma")
    public HomeEstadisticasDto obtenerEstadisticas() {
        final LocalDateTime fechaCorte = LocalDateTime.now().minusDays(DIAS_ULTIMO_MES);
        return new HomeEstadisticasDto(
                artistaRepository.countByActivoTrue(),
                usuarioRepository.countByActivoTrueAndValidadoTrue(),
                listadoRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(fechaCorte),
                ocupacionRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(fechaCorte)
        );
    }
}
