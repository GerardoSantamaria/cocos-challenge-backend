package com.cocos.challenge.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, Integer> {

	@Query(value = """
			select *
			from instruments
			where type = 'ACCIONES'
			  and (
			    ticker ilike :pattern escape '\\'
			    or name ilike :pattern escape '\\'
			  )
			order by ticker
			limit :limit
			""", nativeQuery = true)
	List<InstrumentEntity> search(@Param("pattern") String pattern, @Param("limit") int limit);
}
