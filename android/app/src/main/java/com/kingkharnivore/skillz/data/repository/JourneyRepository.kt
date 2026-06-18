package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(
    private val tagDao: TagDao
){
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun getOrCreateTagId(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) error("Tag name cannot be empty")

        val existing = tagDao.getTagByName(trimmed)
        if (existing != null) return existing.id

        return tagDao.insertTag(TagEntity(name = trimmed))
    }

    suspend fun insertTag(tag: TagEntity) {
        tagDao.insertTag(tag)
    }
}