package com.kutluoglu.prayer.data.mapper

/**
 * Created by F.K. on 30.11.2025.
 *
 */

/**
 * Interface for model mappers. It provides helper methods that facilitate
 * retrieving and putting of models from data source layers to domain layer
 *
 * @param <E> the data(entity) model input type
 * @param <D> the domain model return type
 */

interface Mapper<E, D> {
    fun mapToDomain(type: E) : D
    fun mapFromDomain(type: D) : E
}