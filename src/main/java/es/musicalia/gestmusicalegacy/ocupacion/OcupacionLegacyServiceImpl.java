package es.musicalia.gestmusicalegacy.ocupacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service("ocupacionLegacyService")
@Transactional(transactionManager = "mariadbTransactionManager", readOnly = true)
public class OcupacionLegacyServiceImpl implements OcupacionLegacyService{

    private final OcupacionLegacyRepository ocupacionLegacyRepository;

    @Autowired
    public OcupacionLegacyServiceImpl(OcupacionLegacyRepository ocupacionLegacyRepository) {
        this.ocupacionLegacyRepository = ocupacionLegacyRepository;
    }


    @Override
    public List<OcupacionLegacy> findOcupacionLegacyFromGestmusicaLegacy(LocalDate localDate){

        Optional<List<OcupacionLegacy>> optionalList = this.ocupacionLegacyRepository.findOcupacionesModificadasFromDate(localDate, LocalDateTime.now().minusHours(12));
        return optionalList.orElseGet(ArrayList::new);

    }

    @Override
    public Optional<Set<Integer>> findIdsOcupacionesFromDate(LocalDate localDate){
        return this.ocupacionLegacyRepository.findIdsOcupacionesFromDate(localDate);
    }



}
