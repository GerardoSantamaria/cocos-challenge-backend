package com.cocos.challenge.infrastructure.persistence.repository;

import java.util.List;

import com.cocos.challenge.infrastructure.persistence.entity.InstrumentEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Integer> {

	@Query(value = """
			select *
			from instruments
			where "type" = 'ACCIONES'
			  and (
			    ticker ilike :pattern escape E'\\\\'
			    or "name" ilike :pattern escape E'\\\\'
			  )
			order by ticker
			limit :limit
			""", nativeQuery = true)
	List<InstrumentEntity> search(@Param("pattern") String pattern, @Param("limit") int limit);

}
