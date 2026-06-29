package es.musicalia.gestmusica.eventopublico;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

final class MetadataPreservingPage<T> extends PageImpl<T> {

    private final long preservedTotalElements;

    MetadataPreservingPage(List<T> content, Pageable pageable, long preservedTotalElements) {
        super(content, pageable, preservedTotalElements);
        this.preservedTotalElements = preservedTotalElements;
    }

    @Override
    public long getTotalElements() {
        return preservedTotalElements;
    }

    @Override
    public int getTotalPages() {
        return getSize() == 0 ? 1 : (int) Math.ceil((double) preservedTotalElements / (double) getSize());
    }
}
