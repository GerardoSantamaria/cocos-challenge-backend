package com.cocos.challenge.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;

import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketDataRepository extends JpaRepository<MarketDataEntity, Integer> {

	@Query("""
			select quote from MarketDataEntity quote
			where quote.instrumentId in :ids
			  and quote.closePrice is not null and quote.closePrice > 0
			  and quote.marketDate = (
			    select max(other.marketDate) from MarketDataEntity other
			    where other.instrumentId = quote.instrumentId
			      and other.closePrice is not null and other.closePrice > 0
			  )
			""")
	List<MarketDataEntity> findLatest(@Param("ids") Collection<Integer> ids);

}
