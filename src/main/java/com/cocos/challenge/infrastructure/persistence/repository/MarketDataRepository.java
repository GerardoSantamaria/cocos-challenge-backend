package com.cocos.challenge.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cocos.challenge.infrastructure.persistence.entity.MarketDataEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketDataRepository extends JpaRepository<MarketDataEntity, Integer> {

	Optional<MarketDataEntity> findFirstByInstrumentIdAndClosePriceIsNotNullOrderByMarketDateDescIdDesc(
			Integer instrumentId);

	@Query("""
			select quote from MarketDataEntity quote
			where quote.instrumentId in :ids
			  and quote.marketDate = (
			    select max(other.marketDate) from MarketDataEntity other
			    where other.instrumentId = quote.instrumentId
			  )
			""")
	List<MarketDataEntity> findLatest(@Param("ids") Collection<Integer> ids);

}
