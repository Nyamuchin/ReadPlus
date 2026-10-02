package com.readplus.data.repository

import com.readplus.data.local.dao.CategoryDao
import com.readplus.data.local.entity.CategoryEntity
import com.readplus.domain.model.Category
import com.readplus.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    private fun toModel(e: CategoryEntity) = Category(e.id, e.name, e.type)

    override fun observeByType(type: String): Flow<List<Category>> =
        dao.observeByType(type).map { it.map(::toModel) }

    override suspend fun listByType(type: String): List<Category> =
        dao.listByType(type).map(::toModel)

    override suspend fun create(name: String, type: String): Long =
        dao.insert(CategoryEntity(name = name, type = type))

    override suspend fun rename(id: Long, newName: String) {
        dao.findById(id)?.let { dao.update(it.copy(name = newName)) }
    }

    override suspend fun delete(id: Long) {
        dao.findById(id)?.let { dao.delete(it) }
    }

    override suspend fun ensureManualImportCategory(): Long {
        dao.listByType("VIDEO").firstOrNull { it.name == "手动导入" }?.let { return it.id }
        return dao.insert(CategoryEntity(name = "手动导入", type = "VIDEO", sortOrder = 0))
    }
}