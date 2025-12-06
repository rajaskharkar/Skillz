package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.SkillDao
import com.kingkharnivore.skillz.data.model.SkillEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SkillRepository @Inject constructor(private val skillDao: SkillDao) {

    fun getAllSkills(): Flow<List<SkillEntity>> {
        return skillDao.getAllSkills()
    }

    fun getSkillById(id: Long): Flow<SkillEntity?> {
        return skillDao.getSkillById(id)
    }

    suspend fun addSkill(name: String, description: String): Long {
        val skill = SkillEntity(
            name = name,
            description = description,
            totalTimeMs = 0L
        )
        return skillDao.insertSkill(skill)
    }

    suspend fun updateSkill(skill: SkillEntity) {
        skillDao.updateSkill(skill)
    }
}